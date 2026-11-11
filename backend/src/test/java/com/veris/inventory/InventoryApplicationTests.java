package com.veris.inventory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

// H2 no cubre las migraciones de Fase 02, escritas en dialecto MySQL: este
// arranque solo comprueba la estructura técnica de Fase 01. Las migraciones
// se prueban contra MySQL real en com.veris.inventory.schema.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:bootstrap;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.flyway.enabled=false",
                "app.security.client-secret=test-only-not-a-credential"
        })
class InventoryApplicationTests {
    private static final AtomicBoolean identityAvailable = new AtomicBoolean(true);
    private static final HttpServer identity = createIdentityServer();

    @LocalServerPort
    private int port;

    private static HttpServer createIdentityServer() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            var issuer = "http://127.0.0.1:" + server.getAddress().getPort() + "/realms/test";
            server.createContext("/realms/test/.well-known/openid-configuration", exchange -> {
                var body = ("{\"issuer\":\"" + issuer + "\",\"jwks_uri\":\"" + issuer + "/keys\"}")
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(identityAvailable.get() ? 200 : 503, body.length);
                try (var output = exchange.getResponseBody()) {
                    output.write(body);
                }
            });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @DynamicPropertySource
    static void identityProperties(DynamicPropertyRegistry registry) {
        registry.add("app.identity.issuer-uri",
                () -> "http://127.0.0.1:" + identity.getAddress().getPort() + "/realms/test");
    }

    @AfterAll
    static void stopIdentity() {
        identity.stop(0);
    }

    @Test
    void readinessTracksDependencyFailureWhileLivenessStaysUp() throws Exception {
        var client = HttpClient.newHttpClient();
        assertThat(get(client, "/actuator/health/readiness").statusCode()).isEqualTo(200);
        assertThat(get(client, "/actuator/health/readiness").body()).contains("\"status\":\"UP\"");
        identityAvailable.set(false);
        try {
            assertThat(get(client, "/actuator/health/readiness").statusCode()).isEqualTo(503);
            assertThat(get(client, "/actuator/health/liveness").statusCode()).isEqualTo(200);
            assertThat(get(client, "/actuator/env").statusCode()).isEqualTo(401);
            assertThat(get(client, "/api/v1/assets").statusCode()).isEqualTo(401);
        } finally {
            identityAvailable.set(true);
        }
    }

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
