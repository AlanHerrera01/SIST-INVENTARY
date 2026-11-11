package com.veris.inventory.schema;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

/** Real MySQL only. Creates its own random databases; never cleans a supplied database. */
class MySqlSchemaIT {
    private static String serverUrl;
    private static String user;
    private static String password;
    private static final String suffix = UUID.randomUUID().toString().replace("-", "");
    private static final String schema = "inventory_test_" + suffix;
    private static final String upgradeSchema = "inventory_test_upgrade_" + suffix;
    private static Flyway flyway;

    @BeforeAll
    static void migrate() throws Exception {
        serverUrl = System.getenv("TEST_MYSQL_URL");
        assertThat(serverUrl).as("Set TEST_MYSQL_URL to a disposable MySQL 8.4 server, e.g. jdbc:mysql://127.0.0.1:3307/mysql").isNotBlank();
        user = Objects.requireNonNullElse(System.getenv("TEST_MYSQL_USER"), "root");
        password = Objects.requireNonNullElse(System.getenv("TEST_MYSQL_PASSWORD"), "");
        try (var connection = admin(); var statement = connection.createStatement()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("MySQL");
            assertThat(connection.getMetaData().getDatabaseProductVersion()).startsWith("8.4.");
            statement.execute("CREATE DATABASE " + schema + " CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
            statement.execute("CREATE DATABASE " + upgradeSchema + " CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
        }
        flyway = migration(schema, null);
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(16);
        flyway.validate();
        try (var c = connect(schema)) {
            for (String table : tables(c)) {
                if (!table.equals("flyway_schema_history")) {
                    assertThat(number(c, "SELECT COUNT(*) FROM `" + table + "`")).as("No seeded data: " + table).isZero();
                }
            }
            exportDictionary(c);
        }
    }

    @AfterAll
    static void cleanup() throws Exception {
        if (serverUrl == null) return;
        // Only identifiers created above, with a validated random suffix, are eligible.
        assertThat(suffix).matches("[a-f0-9]{32}");
        if ("true".equals(System.getenv("KEEP_TEST_DATABASES"))) {
            Files.writeString(Path.of("target/mysql-test-databases.txt"), schema + "\n" + upgradeSchema + "\n");
            return;
        }
        try (var c = admin(); var s = c.createStatement()) {
            s.execute("DROP DATABASE IF EXISTS " + schema);
            s.execute("DROP DATABASE IF EXISTS " + upgradeSchema);
        }
    }

    @Test
    void migrationIsRepeatableAndUsesInnoDb() throws Exception {
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThatThrownBy(flyway::clean).hasMessageContaining("cleanDisabled");
        try (var c = connect(schema)) {
            assertThat(number(c, "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND engine <> 'InnoDB'")).isZero();
        }
    }

    @Test
    void upgradeFromV11PreservesExistingRowsAndCode() throws Exception {
        var old = migration(upgradeSchema, "11");
        assertThat(old.migrate().migrationsExecuted).isEqualTo(11);
        String org = id(), center = id(), category = id(), asset = id();
        try (var c = connect(upgradeSchema)) {
            execute(c, "INSERT INTO organization(id,name) VALUES("+b(org)+",'upgrade-test')");
            execute(c, "INSERT INTO center(id,organization_id,code,name) VALUES("+b(center)+","+b(org)+",'C','test')");
            execute(c, "INSERT INTO asset_category(id,organization_id,code,name) VALUES("+b(category)+","+b(org)+",'CAT','test')");
            execute(c, "INSERT INTO asset(id,organization_id,registral_center_id,category_id,permanent_code_seq,permanent_code_check,physical_condition) VALUES("+b(asset)+","+b(org)+","+b(center)+","+b(category)+",1,95,'BUENO')");
        }
        var latest = migration(upgradeSchema, null);
        assertThat(latest.migrate().migrationsExecuted).isEqualTo(5);
        latest.validate();
        try (var c = connect(upgradeSchema)) {
            assertThat(text(c,"SELECT permanent_code FROM asset WHERE id="+b(asset))).isEqualTo("ATI-000000000001-95");
            c.setAutoCommit(false);
            assertThat(allocate(c,org)).as("Counter advances past migrated identities").isEqualTo(2);
            c.commit();
            c.setAutoCommit(true);
            execute(c,"UPDATE organization SET asset_code_prefix='NEW' WHERE id="+b(org));
            assertThat(text(c,"SELECT permanent_code FROM asset WHERE id="+b(asset))).isEqualTo("ATI-000000000001-95");
            try (var fresh = connect(schema)) {
                assertThat(columnShape(c)).isEqualTo(columnShape(fresh));
            }
        }
    }

    @Test
    void foreignKeysRejectCrossOrganizationAndOrphans() throws Exception {
        try (var c = connect(schema)) {
            var a = fixture(c); var other = fixture(c);
            rejects(c, "UPDATE asset SET category_id="+b(other.category)+" WHERE id="+b(a.asset));
            rejects(c, "UPDATE asset SET registral_center_id="+b(other.center)+" WHERE id="+b(a.asset));
            rejects(c, "INSERT INTO user_role_assignment(organization_id,user_id,role_id,scope_kind,valid_from,granted_by_user_id) VALUES("+b(a.org)+","+b(a.user)+","+b(other.role)+",'GLOBAL',NOW(6),"+b(a.user)+")");
            rejects(c, "INSERT INTO asset_reservation(organization_id,asset_id,transfer_id,transfer_item_id,activated_at) VALUES("+b(a.org)+","+b(a.asset)+","+b(other.transfer)+","+b(other.item)+",NOW(6))");
            rejects(c, "INSERT INTO center(organization_id,code,name) VALUES("+b(id())+",'orphan','test')");
        }
    }

    @Test
    void reservationReleaseRetainsHistoryAndAllowsOnlyOneActive() throws Exception {
        try (var c = connect(schema)) {
            var f = fixture(c);
            execute(c, reservation(f,f.item));
            String second = item(c,f);
            rejects(c,reservation(f,second));
            execute(c,"UPDATE asset_reservation SET released_at=NOW(6),release_reason='test cancellation' WHERE asset_id="+b(f.asset));
            execute(c,reservation(f,second));
            assertThat(number(c,"SELECT COUNT(*) FROM asset_reservation WHERE asset_id="+b(f.asset))).isEqualTo(2);
            assertThat(number(c,"SELECT COUNT(*) FROM asset_reservation WHERE asset_id="+b(f.asset)+" AND active_flag=1")).isEqualTo(1);
        }
    }

    @Test
    void physicalLocationMayBeDestinationWhileRegistralCenterRemainsOrigin() throws Exception {
        try (var c = connect(schema)) {
            var f = fixture(c); String location=id();
            execute(c,"INSERT INTO location(id,organization_id,center_id,name,location_type) VALUES("+b(location)+","+b(f.org)+","+b(f.destination)+",'test location','ROOM')");
            execute(c,"UPDATE asset SET physical_center_id="+b(f.destination)+",current_location_id="+b(location)+",operational_state='EN_INCIDENCIA' WHERE id="+b(f.asset));
            assertThat(number(c,"SELECT COUNT(*) FROM asset WHERE id="+b(f.asset)+" AND registral_center_id="+b(f.center))).isEqualTo(1);
            rejects(c,"UPDATE asset SET physical_center_id="+b(f.center)+" WHERE id="+b(f.asset));
        }
    }

    @Test
    void permanentCodesAndHistoryCannotBeRewrittenOrReused() throws Exception {
        try (var c = connect(schema)) {
            var f = fixture(c);
            rejects(c,assetSql(f,id(),1,95));
            rejects(c,assetSql(f,id(),2,1));
            rejects(c,"UPDATE asset SET permanent_code_prefix='NEW' WHERE id="+b(f.asset));
            rejects(c,"DELETE FROM asset WHERE id="+b(f.asset));
            execute(c,"UPDATE asset SET operational_state='BAJA',retired_at=NOW(6) WHERE id="+b(f.asset));
            rejects(c,assetSql(f,id(),1,95));
            String event=id();
            execute(c,"INSERT INTO audit_event(id,organization_id,action,entity_type,outcome,occurred_at) VALUES("+b(event)+","+b(f.org)+",'TEST','test','EXITO',NOW(6))");
            rejects(c,"UPDATE audit_event SET action='ALTERED' WHERE id="+b(event));
            rejects(c,"DELETE FROM audit_event WHERE id="+b(event));
        }
    }

    @Test
    void concurrentFirstUseOfNumberingIsUniqueAndRollbackDoesNotPublishNumbers() throws Exception {
        String org=id();
        try (var c=connect(schema)) { execute(c,"INSERT INTO organization(id,name) VALUES("+b(org)+",'concurrency-"+org+"')"); }
        var values=ConcurrentHashMap.<Long>newKeySet();
        try (var pool=Executors.newFixedThreadPool(8)) {
            var tasks=new ArrayList<Callable<Void>>();
            for (int i=0;i<32;i++) tasks.add(()->{
                try(var c=connect(schema)) {
                    c.setAutoCommit(false);
                    long value=allocate(c,org);
                    c.commit();
                    assertThat(values.add(value)).isTrue();
                }
                return null;
            });
            for(var future:pool.invokeAll(tasks)) future.get(30,TimeUnit.SECONDS);
        }
        assertThat(values).hasSize(32).contains(1L,32L);
        try(var c=connect(schema)) {
            c.setAutoCommit(false);
            assertThat(allocate(c,org)).isEqualTo(33);
            c.rollback();
            assertThat(allocate(c,org)).isEqualTo(33);
            c.commit();
            rejects(c,"UPDATE code_sequence SET current_value=1 WHERE organization_id="+b(org));
            c.rollback();
            execute(c,"UPDATE code_sequence SET current_value=999999999999 WHERE organization_id="+b(org));
            c.commit();
            assertThatThrownBy(()->allocate(c,org)).isInstanceOf(SQLException.class);
            c.rollback();
        }
    }

    @Test
    void transactionRollsBackInventoryAuditAndOutboxTogether() throws Exception {
        try(var c=connect(schema)) {
            var f=fixture(c); String event=id(); c.setAutoCommit(false);
            execute(c,"UPDATE asset SET description='rollback-test' WHERE id="+b(f.asset));
            execute(c,"INSERT INTO audit_event(id,organization_id,action,entity_type,outcome,occurred_at) VALUES("+b(event)+","+b(f.org)+",'TEST','asset','EXITO',NOW(6))");
            execute(c,"INSERT INTO outbox_event(id,organization_id,event_type,aggregate_entity,aggregate_id,payload) VALUES("+b(event)+","+b(f.org)+",'TEST','asset',"+b(f.asset)+",JSON_OBJECT())");
            c.rollback();
            assertThat(number(c,"SELECT COUNT(*) FROM audit_event WHERE id="+b(event))).isZero();
            assertThat(number(c,"SELECT COUNT(*) FROM outbox_event WHERE id="+b(event))).isZero();
            assertThat(number(c,"SELECT COUNT(*) FROM asset WHERE id="+b(f.asset)+" AND description='rollback-test'")).isZero();
        }
    }

    @Test
    void receptionMustReferToAnItemOfItsOwnTransfer() throws Exception {
        try(var c=connect(schema)) {
            var f=fixture(c); String secondItem=item(c,f), receipt=id();
            execute(c,"INSERT INTO reception(id,organization_id,transfer_id,receiver_user_id) VALUES("+b(receipt)+","+b(f.org)+","+b(f.transfer)+","+b(f.user)+")");
            rejects(c,"INSERT INTO reception_item(organization_id,reception_id,transfer_id,transfer_item_id) SELECT "+b(f.org)+","+b(receipt)+",transfer_id,id FROM transfer_item WHERE id="+b(secondItem));
            execute(c,"INSERT INTO reception_item(organization_id,reception_id,transfer_id,transfer_item_id) VALUES("+b(f.org)+","+b(receipt)+","+b(f.transfer)+","+b(f.item)+")");
        }
    }

    @Test
    void attributesAreTypedAndBoundToTheAssetsCategory() throws Exception {
        try(var c=connect(schema)) {
            var f=fixture(c); String attr=id();
            execute(c,"INSERT INTO asset_category_attribute(id,organization_id,category_id,attribute_code,attribute_name,value_type) VALUES("+b(attr)+","+b(f.org)+","+b(f.category)+",'test','test','NUMERO')");
            rejects(c,"INSERT INTO asset_attribute_value(organization_id,asset_id,category_attribute_id,category_id,value_type,value_text) VALUES("+b(f.org)+","+b(f.asset)+","+b(attr)+","+b(f.category)+",'NUMERO','wrong')");
            execute(c,"INSERT INTO asset_attribute_value(organization_id,asset_id,category_attribute_id,category_id,value_type,value_number) VALUES("+b(f.org)+","+b(f.asset)+","+b(attr)+","+b(f.category)+",'NUMERO',16)");
        }
    }

    @Test
    void signaturesMustMatchIssuedHashAndAssignedSignerAndRemainImmutable() throws Exception {
        try(var c=connect(schema)) {
            var f=fixture(c); String file=id(),act=id(),request=id(),signer=id(),signature=id();
            String hash="a".repeat(64), wrong="b".repeat(64);
            execute(c,"INSERT INTO stored_file(id,organization_id,object_key,size_bytes,classification,file_state,sha256_hash,content_type,analyzed_at) VALUES("+b(file)+","+b(f.org)+","+b(id())+",1,'ACTA','LIMPIO','"+hash+"','application/pdf',NOW(6))");
            execute(c,"INSERT INTO act(id,organization_id,act_type,act_state,transfer_id,content_snapshot,content_hash,file_id,issued_at,created_by_user_id) VALUES("+b(act)+","+b(f.org)+",'DESPACHO','EMITIDA',"+b(f.transfer)+",JSON_OBJECT(),'"+hash+"',"+b(file)+",NOW(6),"+b(f.user)+")");
            rejects(c,"INSERT INTO signature_request(organization_id,act_id,content_hash,signature_policy,created_by_user_id) VALUES("+b(f.org)+","+b(act)+",'"+wrong+"','TEST',"+b(f.user)+")");
            execute(c,"INSERT INTO signature_request(id,organization_id,act_id,content_hash,signature_policy,created_by_user_id) VALUES("+b(request)+","+b(f.org)+","+b(act)+",'"+hash+"','TEST',"+b(f.user)+")");
            execute(c,"INSERT INTO signature_request_signer(id,organization_id,request_id,signer_position,expected_user_id,display_name) VALUES("+b(signer)+","+b(f.org)+","+b(request)+",1,"+b(f.user)+",'test')");
            execute(c,"INSERT INTO signature(id,organization_id,request_id,request_signer_id) VALUES("+b(signature)+","+b(f.org)+","+b(request)+","+b(signer)+")");
            String sign="UPDATE signature SET signature_state='FIRMADA',signature_method='INTERNA',signed_at=NOW(6),actual_user_id="+b(f.user)+",identity_evidence=JSON_OBJECT('test',true),signed_content_hash=";
            rejects(c,sign+"'"+wrong+"' WHERE id="+b(signature));
            execute(c,sign+"'"+hash+"' WHERE id="+b(signature));
            rejects(c,"UPDATE signature SET external_reference='rewrite' WHERE id="+b(signature));
            rejects(c,"UPDATE act SET content_snapshot=JSON_OBJECT('changed',true) WHERE id="+b(act));
        }
    }

    @Test
    void skipLockedAllowsIndependentWorkersAndLeaseTokenFencesStaleCompletion() throws Exception {
        try(var c=connect(schema)) {
            var f=fixture(c); String job1=id(),job2=id(),token=id();
            execute(c,"INSERT INTO job(id,organization_id,job_type) VALUES("+b(job1)+","+b(f.org)+",'TEST'),("+b(job2)+","+b(f.org)+",'TEST')");
            try(var a=connect(schema);var b=connect(schema)) {
                a.setAutoCommit(false);b.setAutoCommit(false);
                String query="SELECT HEX(id) FROM job WHERE organization_id="+b(f.org)+" AND job_state='PENDIENTE' ORDER BY id LIMIT 1 FOR UPDATE SKIP LOCKED";
                String first=text(a,query),second=text(b,query);
                assertThat(first).isNotEqualTo(second);
                execute(a,"UPDATE job SET job_state='EN_PROCESO',lease_token="+b(token)+",leased_until=DATE_ADD(NOW(6),INTERVAL 1 MINUTE) WHERE id="+b(first));
                a.commit();b.rollback();
                try(var s=c.createStatement()) {
                    assertThat(s.executeUpdate("UPDATE job SET job_state='COMPLETADO',completed_at=NOW(6),lease_token=NULL,leased_until=NULL WHERE id="+b(first)+" AND lease_token="+b(id()))).isZero();
                    assertThat(s.executeUpdate("UPDATE job SET job_state='COMPLETADO',completed_at=NOW(6),lease_token=NULL,leased_until=NULL WHERE id="+b(first)+" AND lease_token="+b(token))).isEqualTo(1);
                }
            }
        }
    }

    private record Fixture(String org,String center,String destination,String category,String user,String role,String asset,String transfer,String item) {}
    private static Fixture fixture(Connection c) throws Exception {
        var f=new Fixture(id(),id(),id(),id(),id(),id(),id(),id(),id());
        execute(c,"INSERT INTO organization(id,name) VALUES("+b(f.org)+",'test-"+f.org+"')");
        execute(c,"INSERT INTO center(id,organization_id,code,name) VALUES("+b(f.center)+","+b(f.org)+",'A','test'),("+b(f.destination)+","+b(f.org)+",'B','test')");
        execute(c,"INSERT INTO app_user(id,organization_id,identity_issuer,identity_subject,display_name) VALUES("+b(f.user)+","+b(f.org)+",'https://test.invalid','test','Test fixture')");
        execute(c,"INSERT INTO app_role(id,organization_id,code,name) VALUES("+b(f.role)+","+b(f.org)+",'TEST','test')");
        execute(c,"INSERT INTO asset_category(id,organization_id,code,name) VALUES("+b(f.category)+","+b(f.org)+",'TEST','test')");
        execute(c,assetSql(f,f.asset,1,95));
        insertTransfer(c,f,f.transfer,1);
        execute(c,"INSERT INTO transfer_item(id,organization_id,transfer_id,asset_id) VALUES("+b(f.item)+","+b(f.org)+","+b(f.transfer)+","+b(f.asset)+")");
        return f;
    }
    private static String item(Connection c,Fixture f) throws Exception {
        String transfer=id(),item=id(); insertTransfer(c,f,transfer,2);
        execute(c,"INSERT INTO transfer_item(id,organization_id,transfer_id,asset_id) VALUES("+b(item)+","+b(f.org)+","+b(transfer)+","+b(f.asset)+")");
        return item;
    }
    private static void insertTransfer(Connection c,Fixture f,String transfer,int number) throws Exception {
        execute(c,"INSERT INTO transfer(id,organization_id,transfer_number,source_center_id,destination_center_id,requester_user_id,motive) VALUES("+b(transfer)+","+b(f.org)+","+number+","+b(f.center)+","+b(f.destination)+","+b(f.user)+",'test')");
    }
    private static String assetSql(Fixture f,String asset,long seq,int check) {
        return "INSERT INTO asset(id,organization_id,registral_center_id,category_id,permanent_code_prefix,permanent_code_seq,permanent_code_check,physical_condition) VALUES("+b(asset)+","+b(f.org)+","+b(f.center)+","+b(f.category)+",'ATI',"+seq+","+check+",'BUENO')";
    }
    private static String reservation(Fixture f,String item) {
        return "INSERT INTO asset_reservation(organization_id,asset_id,transfer_id,transfer_item_id,activated_at) SELECT "+b(f.org)+","+b(f.asset)+",transfer_id,id,NOW(6) FROM transfer_item WHERE id="+b(item);
    }
    private static long allocate(Connection c,String org) throws SQLException {
        try(var call=c.prepareCall("{call allocate_code_number(?,?,?)}")) {
            call.setBytes(1,HexFormat.of().parseHex(org)); call.setString(2,"ASSET_PERMANENT_CODE"); call.registerOutParameter(3,Types.BIGINT); call.execute(); return call.getLong(3);
        }
    }
    private static String id() { return UUID.randomUUID().toString().replace("-", ""); }
    private static String b(String id) { return "UNHEX('"+id+"')"; }
    private static void rejects(Connection c,String sql) { assertThatThrownBy(()->execute(c,sql)).isInstanceOf(SQLException.class); }
    private static void execute(Connection c,String sql) throws SQLException { try(var s=c.createStatement()) { s.execute(sql); } }
    private static long number(Connection c,String sql) throws SQLException { try(var s=c.createStatement();var r=s.executeQuery(sql)) { r.next();return r.getLong(1); } }
    private static String text(Connection c,String sql) throws SQLException { try(var s=c.createStatement();var r=s.executeQuery(sql)) { r.next();return r.getString(1); } }
    private static Connection admin() throws SQLException { return DriverManager.getConnection(serverUrl,user,password); }
    private static String url(String name) { return serverUrl.replaceFirst("/[^/?]*(\\?.*)?$", "/"+name+"?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&allowPublicKeyRetrieval=true&sslMode=DISABLED"); }
    private static Connection connect(String name) throws SQLException { return DriverManager.getConnection(url(name),user,password); }
    private static Flyway migration(String name,String target) {
        var configuration=Flyway.configure().dataSource(url(name),user,password).locations("classpath:db/migration")
                .cleanDisabled(true).baselineOnMigrate(false).configuration(Map.of("flyway.communityDBSupportEnabled","true"));
        if(target!=null) configuration.target(target);
        return configuration.load();
    }
    private static List<String> tables(Connection c) throws SQLException {
        var names=new ArrayList<String>();
        try(var s=c.createStatement();var r=s.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE() ORDER BY table_name")) { while(r.next()) names.add(r.getString(1)); }
        return names;
    }
    private static List<String> columnShape(Connection c) throws SQLException {
        var result=new ArrayList<String>();
        try(var s=c.createStatement();var r=s.executeQuery("SELECT table_name,column_name,column_type,is_nullable,COALESCE(column_default,'NULL'),extra FROM information_schema.columns WHERE table_schema=DATABASE() ORDER BY table_name,ordinal_position")) {
            while(r.next()) result.add(r.getString(1)+":"+r.getString(2)+":"+r.getString(3)+":"+r.getString(4)+":"+r.getString(5)+":"+r.getString(6));
        } return result;
    }
    private static void exportDictionary(Connection c) throws Exception {
        var output=new StringBuilder("# Diccionario físico MySQL — Fase 02\n\nGenerado desde MySQL real después de V14. UUID BINARY(16); fechas DATETIME(6) en UTC.\n\n");
        for(String table:tables(c)) {
            if(table.equals("flyway_schema_history")) continue;
            output.append("## ").append(table).append("\n\n| Columna | Tipo | NULL | Predeterminado | Clave / generación |\n|---|---|---|---|---|\n");
            try(var s=c.prepareStatement("SELECT column_name,column_type,is_nullable,COALESCE(column_default,'—'),column_key,extra,generation_expression FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=? ORDER BY ordinal_position")) {
                s.setString(1,table);try(var r=s.executeQuery()) { while(r.next()) {
                    output.append("| `").append(r.getString(1)).append("` | ").append(r.getString(2)).append(" | ").append(r.getString(3)).append(" | ").append(r.getString(4)).append(" | ").append(r.getString(5)).append(" ").append(r.getString(6)).append(" ").append(r.getString(7)).append(" |\n");
                }}
            }
            output.append("\nClaves foráneas (columna → tabla.columna):\n\n");
            try(var s=c.prepareStatement("SELECT constraint_name,column_name,referenced_table_name,referenced_column_name FROM information_schema.key_column_usage WHERE table_schema=DATABASE() AND table_name=? AND referenced_table_name IS NOT NULL ORDER BY constraint_name,ordinal_position")) {
                s.setString(1,table);try(var r=s.executeQuery()) { while(r.next()) output.append("- `").append(r.getString(1)).append("`: `").append(r.getString(2)).append("` → `").append(r.getString(3)).append('.').append(r.getString(4)).append("`.\n"); }
            }
            output.append("\n");
        }
        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target/DICCIONARIO-DATOS-FASE-02.md"),output.toString());
    }
}
