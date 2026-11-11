# Diccionario físico MySQL — Fase 02

Generado desde MySQL real después de V14. UUID BINARY(16); fechas DATETIME(6) en UTC.

## act

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `id` | binary(16) | NO | uuid_to_bin(uuid(),1) | PRI DEFAULT_GENERATED  |
| `act_type` | varchar(20) | NO | — | MUL   |
| `act_state` | varchar(20) | NO | BORRADOR |    |
| `content_version` | int unsigned | NO | 1 |    |
| `transfer_id` | binary(16) | YES | — | MUL   |
| `reception_id` | binary(16) | YES | — | MUL   |
| `resolution_id` | binary(16) | YES | — | MUL   |
| `retirement_request_id` | binary(16) | YES | — | MUL   |
| `replaced_act_id` | binary(16) | YES | — | UNI   |
| `content_snapshot` | json | YES | — |    |
| `content_hash` | char(64) | YES | — |    |
| `file_id` | binary(16) | YES | — | MUL   |
| `issued_at` | datetime(6) | YES | — |    |
| `formalized_at` | datetime(6) | YES | — |    |
| `annulled_at` | datetime(6) | YES | — |    |
| `annul_reason` | varchar(500) | YES | — |    |
| `superseded_at` | datetime(6) | YES | — |    |
| `created_by_user_id` | binary(16) | NO | — | MUL   |
| `created_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED  |
| `updated_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6)  |
| `organization_id` | binary(16) | NO | — | MUL   |

Claves foráneas (columna → tabla.columna):

- `fk_act_creator`: `created_by_user_id` → `app_user.id`.
- `fk_act_file`: `file_id` → `stored_file.id`.
- `fk_act_reception`: `reception_id` → `reception.id`.
- `fk_act_replaced`: `replaced_act_id` → `act.id`.
- `fk_act_resolution`: `resolution_id` → `resolution.id`.
- `fk_act_retirement_request`: `retirement_request_id` → `retirement_request.id`.
- `fk_act_transfer`: `transfer_id` → `transfer.id`.
- `fk_tenant_62`: `organization_id` → `transfer.organization_id`.
- `fk_tenant_62`: `transfer_id` → `transfer.id`.
- `fk_tenant_63`: `organization_id` → `reception.organization_id`.
- `fk_tenant_63`: `reception_id` → `reception.id`.
- `fk_tenant_64`: `organization_id` → `resolution.organization_id`.
- `fk_tenant_64`: `resolution_id` → `resolution.id`.
- `fk_tenant_65`: `organization_id` → `retirement_request.organization_id`.
- `fk_tenant_65`: `retirement_request_id` → `retirement_request.id`.
- `fk_tenant_66`: `organization_id` → `act.organization_id`.
- `fk_tenant_66`: `replaced_act_id` → `act.id`.
- `fk_tenant_67`: `organization_id` → `stored_file.organization_id`.
- `fk_tenant_67`: `file_id` → `stored_file.id`.
- `fk_tenant_68`: `organization_id` → `app_user.organization_id`.
- `fk_tenant_68`: `created_by_user_id` → `app_user.id`.

## app_role

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `id` | binary(16) | NO | uuid_to_bin(uuid(),1) | PRI DEFAULT_GENERATED  |
| `organization_id` | binary(16) | NO | — | MUL   |
| `code` | varchar(50) | NO | — |    |
| `name` | varchar(200) | NO | — |    |
| `description` | varchar(500) | YES | — |    |
| `created_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED  |
| `updated_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6)  |

Claves foráneas (columna → tabla.columna):

- `fk_app_role_organization`: `organization_id` → `organization.id`.

## app_user

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `id` | binary(16) | NO | uuid_to_bin(uuid(),1) | PRI DEFAULT_GENERATED  |
| `organization_id` | binary(16) | NO | — | MUL   |
| `identity_issuer` | varchar(255) | NO | — |    |
| `identity_subject` | varchar(255) | NO | — |    |
| `display_name` | varchar(200) | NO | — |    |
| `email_address` | varchar(320) | YES | — |    |
| `user_state` | varchar(20) | NO | ACTIVO |    |
| `created_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED  |
| `updated_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6)  |

Claves foráneas (columna → tabla.columna):

- `fk_app_user_organization`: `organization_id` → `organization.id`.

## asset

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `id` | binary(16) | NO | uuid_to_bin(uuid(),1) | PRI DEFAULT_GENERATED  |
| `organization_id` | binary(16) | NO | — | MUL   |
| `registral_center_id` | binary(16) | NO | — | MUL   |
| `current_location_id` | binary(16) | YES | — |    |
| `category_id` | binary(16) | NO | — | MUL   |
| `permanent_code_seq` | bigint unsigned | NO | — |    |
| `permanent_code_check` | tinyint unsigned | NO | — |    |
| `brand` | varchar(100) | YES | — | MUL   |
| `model` | varchar(150) | YES | — |    |
| `serial_number` | varchar(100) | YES | — | MUL   |
| `description` | varchar(1000) | YES | — |    |
| `physical_condition` | varchar(20) | NO | — |    |
| `operational_state` | varchar(20) | NO | DISPONIBLE |    |
| `version` | bigint unsigned | NO | 1 |    |
| `retired_at` | datetime(6) | YES | — |    |
| `created_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED  |
| `updated_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6)  |
| `physical_center_id` | binary(16) | YES | — | MUL   |
| `permanent_code_prefix` | varchar(10) | NO | — |    |
| `permanent_code` | varchar(26) | YES | — |  STORED GENERATED concat(`permanent_code_prefix`,_utf8mb4\'-\',lpad(`permanent_code_seq`,12,_utf8mb4\'0\'),_utf8mb4\'-\',lpad(`permanent_code_check`,2,_utf8mb4\'0\')) |

Claves foráneas (columna → tabla.columna):

- `fk_asset_category`: `organization_id` → `asset_category.organization_id`.
- `fk_asset_category`: `category_id` → `asset_category.id`.
- `fk_asset_center`: `organization_id` → `center.organization_id`.
- `fk_asset_center`: `registral_center_id` → `center.id`.
- `fk_asset_physical_center`: `organization_id` → `center.organization_id`.
- `fk_asset_physical_center`: `physical_center_id` → `center.id`.
- `fk_asset_physical_location`: `physical_center_id` → `location.center_id`.
- `fk_asset_physical_location`: `current_location_id` → `location.id`.

## asset_attribute_value

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `asset_id` | binary(16) | NO | — | PRI   |
| `category_attribute_id` | binary(16) | NO | — | PRI   |
| `value_text` | varchar(1000) | YES | — |    |
| `value_number` | decimal(18,6) | YES | — |    |
| `value_datetime` | datetime(6) | YES | — |    |
| `value_boolean` | tinyint(1) | YES | — |    |
| `organization_id` | binary(16) | NO | — | MUL   |
| `category_id` | binary(16) | NO | — |    |
| `value_type` | varchar(20) | NO | — |    |

Claves foráneas (columna → tabla.columna):

- `fk_asset_attribute_value_asset`: `asset_id` → `asset.id`.
- `fk_asset_attribute_value_attribute`: `category_attribute_id` → `asset_category_attribute.id`.
- `fk_attribute_value_asset_category`: `asset_id` → `asset.id`.
- `fk_attribute_value_asset_category`: `category_id` → `asset.category_id`.
- `fk_attribute_value_definition`: `category_attribute_id` → `asset_category_attribute.id`.
- `fk_attribute_value_definition`: `category_id` → `asset_category_attribute.category_id`.
- `fk_attribute_value_definition`: `value_type` → `asset_category_attribute.value_type`.
- `fk_tenant_20`: `organization_id` → `asset.organization_id`.
- `fk_tenant_20`: `asset_id` → `asset.id`.
- `fk_tenant_21`: `organization_id` → `asset_category_attribute.organization_id`.
- `fk_tenant_21`: `category_attribute_id` → `asset_category_attribute.id`.

## asset_category

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `id` | binary(16) | NO | uuid_to_bin(uuid(),1) | PRI DEFAULT_GENERATED  |
| `organization_id` | binary(16) | NO | — | MUL   |
| `code` | varchar(50) | NO | — |    |
| `name` | varchar(200) | NO | — |    |
| `parent_category_id` | binary(16) | YES | — |    |
| `category_state` | varchar(20) | NO | ACTIVO |    |
| `created_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED  |
| `updated_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6)  |

Claves foráneas (columna → tabla.columna):

- `fk_asset_category_organization`: `organization_id` → `organization.id`.
- `fk_asset_category_parent`: `organization_id` → `asset_category.organization_id`.
- `fk_asset_category_parent`: `parent_category_id` → `asset_category.id`.

## asset_category_attribute

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `id` | binary(16) | NO | uuid_to_bin(uuid(),1) | PRI DEFAULT_GENERATED  |
| `category_id` | binary(16) | NO | — | MUL   |
| `attribute_code` | varchar(50) | NO | — |    |
| `attribute_name` | varchar(200) | NO | — |    |
| `value_type` | varchar(20) | NO | — |    |
| `required_flag` | tinyint(1) | NO | 0 |    |
| `allowed_values` | json | YES | — |    |
| `created_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED  |
| `updated_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6)  |
| `organization_id` | binary(16) | NO | — | MUL   |

Claves foráneas (columna → tabla.columna):

- `fk_asset_category_attribute_category`: `category_id` → `asset_category.id`.
- `fk_tenant_17`: `organization_id` → `asset_category.organization_id`.
- `fk_tenant_17`: `category_id` → `asset_category.id`.

## asset_custody_assignment

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `id` | binary(16) | NO | uuid_to_bin(uuid(),1) | PRI DEFAULT_GENERATED  |
| `asset_id` | binary(16) | NO | — | MUL   |
| `custodian_id` | binary(16) | NO | — | MUL   |
| `custody_cause` | varchar(20) | NO | — |    |
| `started_at` | datetime(6) | NO | — |    |
| `ended_at` | datetime(6) | YES | — |    |
| `end_cause` | varchar(500) | YES | — |    |
| `related_entity` | varchar(30) | YES | — |    |
| `related_id` | binary(16) | YES | — |    |
| `created_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED  |
| `updated_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6)  |
| `open_flag` | tinyint | YES | — |  STORED GENERATED if((`ended_at` is null),1,NULL) |
| `organization_id` | binary(16) | NO | — | MUL   |

Claves foráneas (columna → tabla.columna):

- `fk_asset_custody_assignment_asset`: `asset_id` → `asset.id`.
- `fk_asset_custody_assignment_custodian`: `custodian_id` → `custodian.id`.
- `fk_tenant_22`: `organization_id` → `asset.organization_id`.
- `fk_tenant_22`: `asset_id` → `asset.id`.
- `fk_tenant_23`: `organization_id` → `custodian.organization_id`.
- `fk_tenant_23`: `custodian_id` → `custodian.id`.

## asset_event

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `id` | binary(16) | NO | uuid_to_bin(uuid(),1) | PRI DEFAULT_GENERATED  |
| `asset_id` | binary(16) | NO | — | MUL   |
| `event_type` | varchar(40) | NO | — |    |
| `occurred_at` | datetime(6) | NO | — | MUL   |
| `actor_user_id` | binary(16) | YES | — | MUL   |
| `from_center_id` | binary(16) | YES | — | MUL   |
| `to_center_id` | binary(16) | YES | — | MUL   |
| `from_location_id` | binary(16) | YES | — | MUL   |
| `to_location_id` | binary(16) | YES | — | MUL   |
| `motive` | varchar(500) | YES | — |    |
| `related_entity` | varchar(30) | YES | — |    |
| `related_id` | binary(16) | YES | — |    |
| `created_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED  |
| `organization_id` | binary(16) | NO | — | MUL   |

Claves foráneas (columna → tabla.columna):

- `fk_asset_event_actor`: `actor_user_id` → `app_user.id`.
- `fk_asset_event_asset`: `asset_id` → `asset.id`.
- `fk_asset_event_from_center`: `from_center_id` → `center.id`.
- `fk_asset_event_from_location`: `from_location_id` → `location.id`.
- `fk_asset_event_to_center`: `to_center_id` → `center.id`.
- `fk_asset_event_to_location`: `to_location_id` → `location.id`.
- `fk_tenant_24`: `organization_id` → `asset.organization_id`.
- `fk_tenant_24`: `asset_id` → `asset.id`.
- `fk_tenant_25`: `organization_id` → `app_user.organization_id`.
- `fk_tenant_25`: `actor_user_id` → `app_user.id`.
- `fk_tenant_26`: `organization_id` → `center.organization_id`.
- `fk_tenant_26`: `from_center_id` → `center.id`.
- `fk_tenant_27`: `organization_id` → `center.organization_id`.
- `fk_tenant_27`: `to_center_id` → `center.id`.
- `fk_tenant_28`: `organization_id` → `location.organization_id`.
- `fk_tenant_28`: `from_location_id` → `location.id`.
- `fk_tenant_29`: `organization_id` → `location.organization_id`.
- `fk_tenant_29`: `to_location_id` → `location.id`.

## asset_qr_identifier

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `id` | binary(16) | NO | uuid_to_bin(uuid(),1) | PRI DEFAULT_GENERATED  |
| `asset_id` | binary(16) | NO | — | MUL   |
| `public_id` | binary(16) | NO | — | UNI   |
| `created_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED  |
| `updated_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6)  |
| `revoked_at` | datetime(6) | YES | — |    |
| `revoke_reason` | varchar(500) | YES | — |    |
| `active_flag` | tinyint | YES | — |  STORED GENERATED if((`revoked_at` is null),1,NULL) |
| `organization_id` | binary(16) | NO | — | MUL   |

Claves foráneas (columna → tabla.columna):

- `fk_asset_qr_identifier_asset`: `asset_id` → `asset.id`.
- `fk_tenant_19`: `organization_id` → `asset.organization_id`.
- `fk_tenant_19`: `asset_id` → `asset.id`.

## asset_reclassification

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `id` | binary(16) | NO | uuid_to_bin(uuid(),1) | PRI DEFAULT_GENERATED  |
| `asset_id` | binary(16) | NO | — | MUL   |
| `from_category_id` | binary(16) | NO | — | MUL   |
| `to_category_id` | binary(16) | NO | — | MUL   |
| `attribute_changes` | json | YES | — |    |
| `motive` | varchar(500) | NO | — |    |
| `performed_by_user_id` | binary(16) | NO | — | MUL   |
| `performed_at` | datetime(6) | NO | — |    |
| `created_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED  |
| `organization_id` | binary(16) | NO | — | MUL   |

Claves foráneas (columna → tabla.columna):

- `fk_asset_reclassification_actor`: `performed_by_user_id` → `app_user.id`.
- `fk_asset_reclassification_asset`: `asset_id` → `asset.id`.
- `fk_asset_reclassification_from`: `from_category_id` → `asset_category.id`.
- `fk_asset_reclassification_to`: `to_category_id` → `asset_category.id`.
- `fk_tenant_30`: `organization_id` → `asset.organization_id`.
- `fk_tenant_30`: `asset_id` → `asset.id`.
- `fk_tenant_31`: `organization_id` → `asset_category.organization_id`.
- `fk_tenant_31`: `from_category_id` → `asset_category.id`.
- `fk_tenant_32`: `organization_id` → `asset_category.organization_id`.
- `fk_tenant_32`: `to_category_id` → `asset_category.id`.
- `fk_tenant_33`: `organization_id` → `app_user.organization_id`.
- `fk_tenant_33`: `performed_by_user_id` → `app_user.id`.

## asset_reservation

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `id` | binary(16) | NO | uuid_to_bin(uuid(),1) | PRI DEFAULT_GENERATED  |
| `asset_id` | binary(16) | NO | — | MUL   |
| `transfer_id` | binary(16) | NO | — | MUL   |
| `transfer_item_id` | binary(16) | NO | — | UNI   |
| `activated_at` | datetime(6) | NO | — |    |
| `released_at` | datetime(6) | YES | — |    |
| `release_reason` | varchar(500) | YES | — |    |
| `created_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED  |
| `updated_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6)  |
| `active_flag` | tinyint | YES | — |  STORED GENERATED if((`released_at` is null),1,NULL) |
| `organization_id` | binary(16) | NO | — | MUL   |

Claves foráneas (columna → tabla.columna):

- `fk_asset_reservation_asset`: `asset_id` → `asset.id`.
- `fk_asset_reservation_item`: `transfer_item_id` → `transfer_item.id`.
- `fk_asset_reservation_transfer`: `transfer_id` → `transfer.id`.
- `fk_reservation_exact_item`: `transfer_id` → `transfer_item.transfer_id`.
- `fk_reservation_exact_item`: `asset_id` → `transfer_item.asset_id`.
- `fk_reservation_exact_item`: `transfer_item_id` → `transfer_item.id`.
- `fk_tenant_39`: `organization_id` → `asset.organization_id`.
- `fk_tenant_39`: `asset_id` → `asset.id`.
- `fk_tenant_40`: `organization_id` → `transfer.organization_id`.
- `fk_tenant_40`: `transfer_id` → `transfer.id`.
- `fk_tenant_41`: `organization_id` → `transfer_item.organization_id`.
- `fk_tenant_41`: `transfer_item_id` → `transfer_item.id`.

## audit_event

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `id` | binary(16) | NO | uuid_to_bin(uuid(),1) | PRI DEFAULT_GENERATED  |
| `organization_id` | binary(16) | NO | — | MUL   |
| `center_id` | binary(16) | YES | — |    |
| `effective_actor_user_id` | binary(16) | YES | — | MUL   |
| `original_actor_user_id` | binary(16) | YES | — | MUL   |
| `action` | varchar(80) | NO | — |    |
| `entity_type` | varchar(50) | NO | — |    |
| `entity_id` | binary(16) | YES | — |    |
| `outcome` | varchar(20) | NO | — |    |
| `occurred_at` | datetime(6) | NO | — | MUL   |
| `correlation_id` | varchar(64) | YES | — | MUL   |
| `motive` | varchar(500) | YES | — |    |
| `changed_fields` | json | YES | — |    |
| `created_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED  |

Claves foráneas (columna → tabla.columna):

- `fk_audit_event_center`: `organization_id` → `center.organization_id`.
- `fk_audit_event_center`: `center_id` → `center.id`.
- `fk_audit_event_effective_actor`: `effective_actor_user_id` → `app_user.id`.
- `fk_audit_event_organization`: `organization_id` → `organization.id`.
- `fk_audit_event_original_actor`: `original_actor_user_id` → `app_user.id`.
- `fk_tenant_5`: `organization_id` → `app_user.organization_id`.
- `fk_tenant_5`: `effective_actor_user_id` → `app_user.id`.
- `fk_tenant_6`: `organization_id` → `app_user.organization_id`.
- `fk_tenant_6`: `original_actor_user_id` → `app_user.id`.

## center

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `id` | binary(16) | NO | uuid_to_bin(uuid(),1) | PRI DEFAULT_GENERATED  |
| `organization_id` | binary(16) | NO | — | MUL   |
| `code` | varchar(20) | NO | — |    |
| `name` | varchar(200) | NO | — |    |
| `center_state` | varchar(20) | NO | ACTIVO |    |
| `created_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED  |
| `updated_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6)  |

Claves foráneas (columna → tabla.columna):

- `fk_center_organization`: `organization_id` → `organization.id`.

## code_sequence

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `id` | binary(16) | NO | uuid_to_bin(uuid(),1) | PRI DEFAULT_GENERATED  |
| `organization_id` | binary(16) | NO | — | MUL   |
| `sequence_name` | varchar(50) | NO | — |    |
| `current_value` | bigint unsigned | NO | 0 |    |
| `created_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED  |
| `updated_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6)  |

Claves foráneas (columna → tabla.columna):

- `fk_code_sequence_organization`: `organization_id` → `organization.id`.

## custodian

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `id` | binary(16) | NO | uuid_to_bin(uuid(),1) | PRI DEFAULT_GENERATED  |
| `organization_id` | binary(16) | NO | — | MUL   |
| `center_id` | binary(16) | YES | — |    |
| `display_name` | varchar(200) | NO | — |    |
| `document_number` | varchar(50) | YES | — |    |
| `linked_user_id` | binary(16) | YES | — | MUL   |
| `custodian_state` | varchar(20) | NO | ACTIVO |    |
| `created_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED  |
| `updated_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6)  |

Claves foráneas (columna → tabla.columna):

- `fk_custodian_center`: `organization_id` → `center.organization_id`.
- `fk_custodian_center`: `center_id` → `center.id`.
- `fk_custodian_linked_user`: `linked_user_id` → `app_user.id`.
- `fk_custodian_organization`: `organization_id` → `organization.id`.
- `fk_tenant_16`: `organization_id` → `app_user.organization_id`.
- `fk_tenant_16`: `linked_user_id` → `app_user.id`.

## discrepancy

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `id` | binary(16) | NO | uuid_to_bin(uuid(),1) | PRI DEFAULT_GENERATED  |
| `transfer_item_id` | binary(16) | NO | — | MUL   |
| `reception_item_id` | binary(16) | YES | — | MUL   |
| `discrepancy_type` | varchar(20) | NO | — |    |
| `motive` | varchar(500) | NO | — |    |
| `responsible_custodian_id` | binary(16) | YES | — | MUL   |
| `created_by_user_id` | binary(16) | NO | — | MUL   |
| `resolved_at` | datetime(6) | YES | — |    |
| `created_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED  |
| `updated_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6)  |
| `open_flag` | tinyint | YES | — |  STORED GENERATED if((`resolved_at` is null),1,NULL) |
| `organization_id` | binary(16) | NO | — | MUL   |

Claves foráneas (columna → tabla.columna):

- `fk_discrepancy_creator`: `created_by_user_id` → `app_user.id`.
- `fk_discrepancy_exact_reception`: `transfer_item_id` → `reception_item.transfer_item_id`.
- `fk_discrepancy_exact_reception`: `reception_item_id` → `reception_item.id`.
- `fk_discrepancy_reception_item`: `reception_item_id` → `reception_item.id`.
- `fk_discrepancy_responsible_custodian`: `responsible_custodian_id` → `custodian.id`.
- `fk_discrepancy_transfer_item`: `transfer_item_id` → `transfer_item.id`.
- `fk_tenant_48`: `organization_id` → `transfer_item.organization_id`.
- `fk_tenant_48`: `transfer_item_id` → `transfer_item.id`.
- `fk_tenant_49`: `organization_id` → `reception_item.organization_id`.
- `fk_tenant_49`: `reception_item_id` → `reception_item.id`.
- `fk_tenant_50`: `organization_id` → `custodian.organization_id`.
- `fk_tenant_50`: `responsible_custodian_id` → `custodian.id`.
- `fk_tenant_51`: `organization_id` → `app_user.organization_id`.
- `fk_tenant_51`: `created_by_user_id` → `app_user.id`.

## idempotency_key

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `id` | binary(16) | NO | uuid_to_bin(uuid(),1) | PRI DEFAULT_GENERATED  |
| `organization_id` | binary(16) | NO | — | MUL   |
| `actor_user_id` | binary(16) | NO | — | MUL   |
| `idem_key` | varchar(100) | NO | — |    |
| `operation` | varchar(100) | NO | — |    |
| `request_fingerprint` | char(64) | YES | — |    |
| `response_snapshot` | json | YES | — |    |
| `expires_at` | datetime(6) | YES | — | MUL   |
| `created_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED  |
| `updated_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6)  |

Claves foráneas (columna → tabla.columna):

- `fk_idempotency_key_actor`: `actor_user_id` → `app_user.id`.
- `fk_idempotency_key_organization`: `organization_id` → `organization.id`.
- `fk_tenant_7`: `organization_id` → `app_user.organization_id`.
- `fk_tenant_7`: `actor_user_id` → `app_user.id`.

## job

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `id` | binary(16) | NO | uuid_to_bin(uuid(),1) | PRI DEFAULT_GENERATED  |
| `organization_id` | binary(16) | NO | — | MUL   |
| `job_type` | varchar(80) | NO | — |    |
| `job_state` | varchar(20) | NO | PENDIENTE | MUL   |
| `priority` | tinyint unsigned | NO | 5 |    |
| `payload` | json | YES | — |    |
| `related_entity` | varchar(30) | YES | — | MUL   |
| `related_id` | binary(16) | YES | — |    |
| `attempts` | smallint unsigned | NO | 0 |    |
| `next_attempt_at` | datetime(6) | YES | — |    |
| `completed_at` | datetime(6) | YES | — |    |
| `result_snapshot` | json | YES | — |    |
| `last_error` | varchar(1000) | YES | — |    |
| `created_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED  |
| `updated_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6)  |
| `lease_token` | binary(16) | YES | — |    |
| `leased_until` | datetime(6) | YES | — |    |
| `deduplication_key` | varchar(200) | YES | — |    |

Claves foráneas (columna → tabla.columna):

- `fk_job_organization`: `organization_id` → `organization.id`.

## location

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `id` | binary(16) | NO | uuid_to_bin(uuid(),1) | PRI DEFAULT_GENERATED  |
| `center_id` | binary(16) | NO | — | MUL   |
| `name` | varchar(200) | NO | — |    |
| `location_type` | varchar(50) | NO | — |    |
| `parent_location_id` | binary(16) | YES | — |    |
| `created_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED  |
| `updated_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6)  |
| `organization_id` | binary(16) | NO | — | MUL   |

Claves foráneas (columna → tabla.columna):

- `fk_location_center`: `center_id` → `center.id`.
- `fk_location_parent`: `center_id` → `location.center_id`.
- `fk_location_parent`: `parent_location_id` → `location.id`.
- `fk_tenant_9`: `organization_id` → `center.organization_id`.
- `fk_tenant_9`: `center_id` → `center.id`.

## maintenance_work

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `id` | binary(16) | NO | uuid_to_bin(uuid(),1) | PRI DEFAULT_GENERATED  |
| `asset_id` | binary(16) | NO | — | MUL   |
| `maintenance_kind` | varchar(20) | NO | — |    |
| `maintenance_state` | varchar(20) | NO | PLANIFICADO |    |
| `description` | varchar(1000) | NO | — |    |
| `scheduled_start_at` | datetime(6) | YES | — |    |
| `scheduled_end_at` | datetime(6) | YES | — |    |
| `performed_start_at` | datetime(6) | YES | — |    |
| `performed_end_at` | datetime(6) | YES | — |    |
| `responsible_custodian_id` | binary(16) | YES | — | MUL   |
| `performed_by_user_id` | binary(16) | YES | — | MUL   |
| `result_notes` | varchar(1000) | YES | — |    |
| `created_by_user_id` | binary(16) | NO | — | MUL   |
| `created_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED  |
| `updated_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6)  |
| `organization_id` | binary(16) | NO | — | MUL   |

Claves foráneas (columna → tabla.columna):

- `fk_maintenance_work_asset`: `asset_id` → `asset.id`.
- `fk_maintenance_work_creator`: `created_by_user_id` → `app_user.id`.
- `fk_maintenance_work_performed_by`: `performed_by_user_id` → `app_user.id`.
- `fk_maintenance_work_responsible`: `responsible_custodian_id` → `custodian.id`.
- `fk_tenant_1`: `organization_id` → `asset.organization_id`.
- `fk_tenant_1`: `asset_id` → `asset.id`.
- `fk_tenant_2`: `organization_id` → `custodian.organization_id`.
- `fk_tenant_2`: `responsible_custodian_id` → `custodian.id`.
- `fk_tenant_3`: `organization_id` → `app_user.organization_id`.
- `fk_tenant_3`: `performed_by_user_id` → `app_user.id`.
- `fk_tenant_4`: `organization_id` → `app_user.organization_id`.
- `fk_tenant_4`: `created_by_user_id` → `app_user.id`.

## notification_event

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `id` | binary(16) | NO | uuid_to_bin(uuid(),1) | PRI DEFAULT_GENERATED  |
| `organization_id` | binary(16) | NO | — | MUL   |
| `center_id` | binary(16) | YES | — |    |
| `recipient_user_id` | binary(16) | YES | — | MUL   |
| `target_center_id` | binary(16) | YES | — | MUL   |
| `notification_kind` | varchar(50) | NO | — |    |
| `payload` | json | NO | — |    |
| `delivery_state` | varchar(20) | NO | PENDIENTE | MUL   |
| `delivered_at` | datetime(6) | YES | — |    |
| `created_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED  |
| `updated_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6)  |
| `source_event_id` | binary(16) | YES | — | MUL   |
| `channel` | varchar(20) | NO | IN_APP |    |
| `attempts` | int unsigned | NO | 0 |    |
| `next_attempt_at` | datetime(6) | YES | — |    |
| `last_error_code` | varchar(100) | YES | — |    |

Claves foráneas (columna → tabla.columna):

- `fk_notification_event_center`: `organization_id` → `center.organization_id`.
- `fk_notification_event_center`: `center_id` → `center.id`.
- `fk_notification_event_organization`: `organization_id` → `organization.id`.
- `fk_notification_event_recipient`: `recipient_user_id` → `app_user.id`.
- `fk_notification_event_target_center`: `organization_id` → `center.organization_id`.
- `fk_notification_event_target_center`: `target_center_id` → `center.id`.
- `fk_notification_source`: `source_event_id` → `outbox_event.id`.
- `fk_tenant_8`: `organization_id` → `app_user.organization_id`.
- `fk_tenant_8`: `recipient_user_id` → `app_user.id`.
- `fk_tenant_notification_source`: `organization_id` → `outbox_event.organization_id`.
- `fk_tenant_notification_source`: `source_event_id` → `outbox_event.id`.

## organization

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `id` | binary(16) | NO | uuid_to_bin(uuid(),1) | PRI DEFAULT_GENERATED  |
| `name` | varchar(200) | NO | — | UNI   |
| `asset_code_prefix` | varchar(10) | NO | ATI |    |
| `presentation_timezone` | varchar(64) | NO | America/Guayaquil |    |
| `created_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED  |
| `updated_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6)  |

Claves foráneas (columna → tabla.columna):


## outbox_event

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `id` | binary(16) | NO | uuid_to_bin(uuid(),1) | PRI DEFAULT_GENERATED  |
| `organization_id` | binary(16) | NO | — | MUL   |
| `event_type` | varchar(100) | NO | — |    |
| `aggregate_entity` | varchar(50) | NO | — | MUL   |
| `aggregate_id` | binary(16) | NO | — |    |
| `event_key` | varchar(200) | YES | — |    |
| `payload` | json | NO | — |    |
| `delivery_state` | varchar(20) | NO | PENDIENTE | MUL   |
| `attempts` | smallint unsigned | NO | 0 |    |
| `next_attempt_at` | datetime(6) | YES | — |    |
| `delivered_at` | datetime(6) | YES | — |    |
| `created_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED  |
| `updated_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6)  |
| `lease_token` | binary(16) | YES | — |    |
| `leased_until` | datetime(6) | YES | — |    |

Claves foráneas (columna → tabla.columna):

- `fk_outbox_event_organization`: `organization_id` → `organization.id`.

## permission

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `id` | binary(16) | NO | uuid_to_bin(uuid(),1) | PRI DEFAULT_GENERATED  |
| `code` | varchar(80) | NO | — | UNI   |
| `name` | varchar(200) | NO | — |    |
| `description` | varchar(500) | YES | — |    |
| `created_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED  |
| `updated_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6)  |

Claves foráneas (columna → tabla.columna):


## reception

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `id` | binary(16) | NO | uuid_to_bin(uuid(),1) | PRI DEFAULT_GENERATED  |
| `transfer_id` | binary(16) | NO | — | MUL   |
| `receiver_user_id` | binary(16) | NO | — | MUL   |
| `reception_state` | varchar(20) | NO | BORRADOR |    |
| `observed_at` | datetime(6) | YES | — |    |
| `confirmed_at` | datetime(6) | YES | — |    |
| `discarded_at` | datetime(6) | YES | — |    |
| `discard_reason` | varchar(500) | YES | — |    |
| `version` | bigint unsigned | NO | 1 |    |
| `created_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED  |
| `updated_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6)  |
| `organization_id` | binary(16) | NO | — | MUL   |

Claves foráneas (columna → tabla.columna):

- `fk_reception_receiver`: `receiver_user_id` → `app_user.id`.
- `fk_reception_transfer`: `transfer_id` → `transfer.id`.
- `fk_tenant_42`: `organization_id` → `transfer.organization_id`.
- `fk_tenant_42`: `transfer_id` → `transfer.id`.
- `fk_tenant_43`: `organization_id` → `app_user.organization_id`.
- `fk_tenant_43`: `receiver_user_id` → `app_user.id`.

## reception_item

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `id` | binary(16) | NO | uuid_to_bin(uuid(),1) | PRI DEFAULT_GENERATED  |
| `reception_id` | binary(16) | NO | — | MUL   |
| `transfer_item_id` | binary(16) | NO | — | MUL   |
| `outcome` | varchar(20) | YES | — |    |
| `observed_condition` | varchar(20) | YES | — |    |
| `destination_location_id` | binary(16) | YES | — | MUL   |
| `destination_custodian_id` | binary(16) | YES | — | MUL   |
| `rejection_reason` | varchar(500) | YES | — |    |
| `created_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED  |
| `updated_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6)  |
| `voided_at` | datetime(6) | YES | — |    |
| `claimed_flag` | tinyint | YES | — |  STORED GENERATED if((`voided_at` is null),1,NULL) |
| `organization_id` | binary(16) | NO | — | MUL   |
| `transfer_id` | binary(16) | NO | — | MUL   |

Claves foráneas (columna → tabla.columna):

- `fk_reception_item_custodian`: `destination_custodian_id` → `custodian.id`.
- `fk_reception_item_exact_reception`: `transfer_id` → `reception.transfer_id`.
- `fk_reception_item_exact_reception`: `reception_id` → `reception.id`.
- `fk_reception_item_exact_transfer`: `transfer_id` → `transfer_item.transfer_id`.
- `fk_reception_item_exact_transfer`: `transfer_item_id` → `transfer_item.id`.
- `fk_reception_item_location`: `destination_location_id` → `location.id`.
- `fk_reception_item_reception`: `reception_id` → `reception.id`.
- `fk_reception_item_transfer_item`: `transfer_item_id` → `transfer_item.id`.
- `fk_tenant_44`: `organization_id` → `reception.organization_id`.
- `fk_tenant_44`: `reception_id` → `reception.id`.
- `fk_tenant_45`: `organization_id` → `transfer_item.organization_id`.
- `fk_tenant_45`: `transfer_item_id` → `transfer_item.id`.
- `fk_tenant_46`: `organization_id` → `location.organization_id`.
- `fk_tenant_46`: `destination_location_id` → `location.id`.
- `fk_tenant_47`: `organization_id` → `custodian.organization_id`.
- `fk_tenant_47`: `destination_custodian_id` → `custodian.id`.

## resolution

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `id` | binary(16) | NO | uuid_to_bin(uuid(),1) | PRI DEFAULT_GENERATED  |
| `discrepancy_id` | binary(16) | NO | — | MUL   |
| `resolution_type` | varchar(30) | NO | — |    |
| `resolution_state` | varchar(20) | NO | PROPUESTA |    |
| `motive` | varchar(500) | NO | — |    |
| `proposed_by_user_id` | binary(16) | NO | — | MUL   |
| `approver_user_id` | binary(16) | YES | — | MUL   |
| `decided_at` | datetime(6) | YES | — |    |
| `executed_by_user_id` | binary(16) | YES | — | MUL   |
| `executed_at` | datetime(6) | YES | — |    |
| `destination_custodian_id` | binary(16) | YES | — | MUL   |
| `created_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED  |
| `updated_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6)  |
| `pending_flag` | tinyint | YES | — |  STORED GENERATED if((`resolution_state` in (_utf8mb4\'PROPUESTA\',_utf8mb4\'APROBADA\')),1,NULL) |
| `executed_flag` | tinyint | YES | — |  STORED GENERATED if((`resolution_state` = _utf8mb4\'EJECUTADA\'),1,NULL) |
| `organization_id` | binary(16) | NO | — | MUL   |

Claves foráneas (columna → tabla.columna):

- `fk_resolution_approver`: `approver_user_id` → `app_user.id`.
- `fk_resolution_destination_custodian`: `destination_custodian_id` → `custodian.id`.
- `fk_resolution_discrepancy`: `discrepancy_id` → `discrepancy.id`.
- `fk_resolution_executor`: `executed_by_user_id` → `app_user.id`.
- `fk_resolution_proposer`: `proposed_by_user_id` → `app_user.id`.
- `fk_tenant_52`: `organization_id` → `discrepancy.organization_id`.
- `fk_tenant_52`: `discrepancy_id` → `discrepancy.id`.
- `fk_tenant_53`: `organization_id` → `app_user.organization_id`.
- `fk_tenant_53`: `proposed_by_user_id` → `app_user.id`.
- `fk_tenant_54`: `organization_id` → `app_user.organization_id`.
- `fk_tenant_54`: `approver_user_id` → `app_user.id`.
- `fk_tenant_55`: `organization_id` → `app_user.organization_id`.
- `fk_tenant_55`: `executed_by_user_id` → `app_user.id`.
- `fk_tenant_56`: `organization_id` → `custodian.organization_id`.
- `fk_tenant_56`: `destination_custodian_id` → `custodian.id`.

## retirement_request

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `id` | binary(16) | NO | uuid_to_bin(uuid(),1) | PRI DEFAULT_GENERATED  |
| `asset_id` | binary(16) | NO | — | MUL   |
| `retirement_kind` | varchar(20) | NO | — |    |
| `request_state` | varchar(20) | NO | PROPUESTA |    |
| `motive` | varchar(500) | NO | — |    |
| `requested_by_user_id` | binary(16) | NO | — | MUL   |
| `approver_user_id` | binary(16) | YES | — | MUL   |
| `decided_at` | datetime(6) | YES | — |    |
| `executed_by_user_id` | binary(16) | YES | — | MUL   |
| `executed_at` | datetime(6) | YES | — |    |
| `resolution_id` | binary(16) | YES | — | MUL   |
| `formal_act_id` | binary(16) | YES | — | MUL   |
| `created_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED  |
| `updated_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6)  |
| `open_flag` | tinyint | YES | — |  STORED GENERATED if((`request_state` in (_utf8mb4\'PROPUESTA\',_utf8mb4\'APROBADA\')),1,NULL) |
| `organization_id` | binary(16) | NO | — | MUL   |

Claves foráneas (columna → tabla.columna):

- `fk_retirement_request_approver`: `approver_user_id` → `app_user.id`.
- `fk_retirement_request_asset`: `asset_id` → `asset.id`.
- `fk_retirement_request_executor`: `executed_by_user_id` → `app_user.id`.
- `fk_retirement_request_formal_act`: `formal_act_id` → `act.id`.
- `fk_retirement_request_requester`: `requested_by_user_id` → `app_user.id`.
- `fk_retirement_request_resolution`: `resolution_id` → `resolution.id`.
- `fk_tenant_57`: `organization_id` → `asset.organization_id`.
- `fk_tenant_57`: `asset_id` → `asset.id`.
- `fk_tenant_58`: `organization_id` → `app_user.organization_id`.
- `fk_tenant_58`: `requested_by_user_id` → `app_user.id`.
- `fk_tenant_59`: `organization_id` → `app_user.organization_id`.
- `fk_tenant_59`: `approver_user_id` → `app_user.id`.
- `fk_tenant_60`: `organization_id` → `app_user.organization_id`.
- `fk_tenant_60`: `executed_by_user_id` → `app_user.id`.
- `fk_tenant_61`: `organization_id` → `resolution.organization_id`.
- `fk_tenant_61`: `resolution_id` → `resolution.id`.
- `fk_tenant_retirement_act`: `organization_id` → `act.organization_id`.
- `fk_tenant_retirement_act`: `formal_act_id` → `act.id`.

## role_permission

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `role_id` | binary(16) | NO | — | PRI   |
| `permission_id` | binary(16) | NO | — | PRI   |
| `organization_id` | binary(16) | NO | — | MUL   |

Claves foráneas (columna → tabla.columna):

- `fk_role_permission_permission`: `permission_id` → `permission.id`.
- `fk_role_permission_role`: `role_id` → `app_role.id`.
- `fk_tenant_10`: `organization_id` → `app_role.organization_id`.
- `fk_tenant_10`: `role_id` → `app_role.id`.

## signature

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `id` | binary(16) | NO | uuid_to_bin(uuid(),1) | PRI DEFAULT_GENERATED  |
| `request_id` | binary(16) | NO | — | MUL   |
| `request_signer_id` | binary(16) | NO | — |    |
| `signature_state` | varchar(20) | NO | PENDIENTE |    |
| `signature_method` | varchar(20) | YES | — |    |
| `actual_user_id` | binary(16) | YES | — | MUL   |
| `signed_at` | datetime(6) | YES | — |    |
| `signed_content_hash` | char(64) | YES | — |    |
| `rejection_reason` | varchar(500) | YES | — |    |
| `evidence_file_id` | binary(16) | YES | — | MUL   |
| `external_reference` | varchar(200) | YES | — |    |
| `created_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED  |
| `updated_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6)  |
| `organization_id` | binary(16) | NO | — | MUL   |
| `identity_evidence` | json | YES | — |    |

Claves foráneas (columna → tabla.columna):

- `fk_signature_actual_user`: `actual_user_id` → `app_user.id`.
- `fk_signature_evidence_file`: `evidence_file_id` → `stored_file.id`.
- `fk_signature_exact_hash`: `request_id` → `signature_request.id`.
- `fk_signature_exact_hash`: `signed_content_hash` → `signature_request.content_hash`.
- `fk_signature_expected_identity`: `request_id` → `signature_request_signer.request_id`.
- `fk_signature_expected_identity`: `request_signer_id` → `signature_request_signer.id`.
- `fk_signature_expected_identity`: `actual_user_id` → `signature_request_signer.expected_user_id`.
- `fk_signature_request`: `request_id` → `signature_request.id`.
- `fk_signature_signer`: `request_id` → `signature_request_signer.request_id`.
- `fk_signature_signer`: `request_signer_id` → `signature_request_signer.id`.
- `fk_tenant_73`: `organization_id` → `signature_request.organization_id`.
- `fk_tenant_73`: `request_id` → `signature_request.id`.
- `fk_tenant_74`: `organization_id` → `app_user.organization_id`.
- `fk_tenant_74`: `actual_user_id` → `app_user.id`.
- `fk_tenant_75`: `organization_id` → `stored_file.organization_id`.
- `fk_tenant_75`: `evidence_file_id` → `stored_file.id`.

## signature_request

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `id` | binary(16) | NO | uuid_to_bin(uuid(),1) | PRI DEFAULT_GENERATED  |
| `act_id` | binary(16) | NO | — | MUL   |
| `content_hash` | char(64) | NO | — |    |
| `signature_policy` | varchar(50) | NO | — |    |
| `provider` | varchar(100) | YES | — |    |
| `request_state` | varchar(20) | NO | CREADA |    |
| `expires_at` | datetime(6) | YES | — |    |
| `created_by_user_id` | binary(16) | NO | — | MUL   |
| `created_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED  |
| `updated_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6)  |
| `active_flag` | tinyint | YES | — |  STORED GENERATED if((`request_state` in (_utf8mb4\'CREADA\',_utf8mb4\'PENDIENTE\')),1,NULL) |
| `organization_id` | binary(16) | NO | — | MUL   |

Claves foráneas (columna → tabla.columna):

- `fk_request_exact_act_hash`: `act_id` → `act.id`.
- `fk_request_exact_act_hash`: `content_hash` → `act.content_hash`.
- `fk_signature_request_act`: `act_id` → `act.id`.
- `fk_signature_request_creator`: `created_by_user_id` → `app_user.id`.
- `fk_tenant_69`: `organization_id` → `act.organization_id`.
- `fk_tenant_69`: `act_id` → `act.id`.
- `fk_tenant_70`: `organization_id` → `app_user.organization_id`.
- `fk_tenant_70`: `created_by_user_id` → `app_user.id`.

## signature_request_signer

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `id` | binary(16) | NO | uuid_to_bin(uuid(),1) | PRI DEFAULT_GENERATED  |
| `request_id` | binary(16) | NO | — | MUL   |
| `signer_position` | smallint unsigned | NO | — |    |
| `expected_user_id` | binary(16) | NO | — | MUL   |
| `display_name` | varchar(200) | NO | — |    |
| `created_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED  |
| `organization_id` | binary(16) | NO | — | MUL   |

Claves foráneas (columna → tabla.columna):

- `fk_signature_request_signer_request`: `request_id` → `signature_request.id`.
- `fk_signature_request_signer_user`: `expected_user_id` → `app_user.id`.
- `fk_tenant_71`: `organization_id` → `signature_request.organization_id`.
- `fk_tenant_71`: `request_id` → `signature_request.id`.
- `fk_tenant_72`: `organization_id` → `app_user.organization_id`.
- `fk_tenant_72`: `expected_user_id` → `app_user.id`.

## stored_file

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `id` | binary(16) | NO | uuid_to_bin(uuid(),1) | PRI DEFAULT_GENERATED  |
| `organization_id` | binary(16) | NO | — | MUL   |
| `object_key` | binary(16) | NO | — | UNI   |
| `owner_entity` | varchar(30) | YES | — |    |
| `owner_id` | binary(16) | YES | — |    |
| `original_filename` | varchar(255) | YES | — |    |
| `content_type` | varchar(100) | YES | — |    |
| `size_bytes` | bigint unsigned | NO | — |    |
| `sha256_hash` | char(64) | YES | — |    |
| `file_state` | varchar(20) | NO | SUBIDA_PENDIENTE |    |
| `analysis_result` | varchar(200) | YES | — |    |
| `analyzed_at` | datetime(6) | YES | — |    |
| `classification` | varchar(20) | NO | — |    |
| `retention_class` | varchar(50) | YES | — |    |
| `retention_until` | datetime(6) | YES | — |    |
| `created_by_user_id` | binary(16) | YES | — | MUL   |
| `created_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED  |
| `updated_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6)  |
| `legal_hold` | tinyint(1) | NO | 0 |    |
| `object_version` | varchar(255) | YES | — |    |

Claves foráneas (columna → tabla.columna):

- `fk_stored_file_creator`: `created_by_user_id` → `app_user.id`.
- `fk_stored_file_organization`: `organization_id` → `organization.id`.
- `fk_tenant_18`: `organization_id` → `app_user.organization_id`.
- `fk_tenant_18`: `created_by_user_id` → `app_user.id`.

## transfer

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `id` | binary(16) | NO | uuid_to_bin(uuid(),1) | PRI DEFAULT_GENERATED  |
| `organization_id` | binary(16) | NO | — | MUL   |
| `transfer_number` | bigint unsigned | NO | — |    |
| `source_center_id` | binary(16) | NO | — | MUL   |
| `destination_center_id` | binary(16) | NO | — | MUL   |
| `requester_user_id` | binary(16) | NO | — | MUL   |
| `authorizer_user_id` | binary(16) | YES | — | MUL   |
| `transport_custodian_id` | binary(16) | YES | — | MUL   |
| `dispatch_act_id` | binary(16) | YES | — | MUL   |
| `final_act_id` | binary(16) | YES | — | MUL   |
| `motive` | varchar(500) | NO | — |    |
| `transfer_state` | varchar(30) | NO | BORRADOR |    |
| `version` | bigint unsigned | NO | 1 |    |
| `authorized_at` | datetime(6) | YES | — |    |
| `rejected_at` | datetime(6) | YES | — |    |
| `dispatched_at` | datetime(6) | YES | — |    |
| `closed_at` | datetime(6) | YES | — |    |
| `created_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED  |
| `updated_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6)  |

Claves foráneas (columna → tabla.columna):

- `fk_tenant_34`: `organization_id` → `app_user.organization_id`.
- `fk_tenant_34`: `requester_user_id` → `app_user.id`.
- `fk_tenant_35`: `organization_id` → `app_user.organization_id`.
- `fk_tenant_35`: `authorizer_user_id` → `app_user.id`.
- `fk_tenant_36`: `organization_id` → `custodian.organization_id`.
- `fk_tenant_36`: `transport_custodian_id` → `custodian.id`.
- `fk_tenant_dispatch_act`: `organization_id` → `act.organization_id`.
- `fk_tenant_dispatch_act`: `dispatch_act_id` → `act.id`.
- `fk_tenant_final_act`: `organization_id` → `act.organization_id`.
- `fk_tenant_final_act`: `final_act_id` → `act.id`.
- `fk_transfer_authorizer`: `authorizer_user_id` → `app_user.id`.
- `fk_transfer_destination_center`: `organization_id` → `center.organization_id`.
- `fk_transfer_destination_center`: `destination_center_id` → `center.id`.
- `fk_transfer_dispatch_act`: `dispatch_act_id` → `act.id`.
- `fk_transfer_final_act`: `final_act_id` → `act.id`.
- `fk_transfer_requester`: `requester_user_id` → `app_user.id`.
- `fk_transfer_source_center`: `organization_id` → `center.organization_id`.
- `fk_transfer_source_center`: `source_center_id` → `center.id`.
- `fk_transfer_transport_custodian`: `transport_custodian_id` → `custodian.id`.

## transfer_item

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `id` | binary(16) | NO | uuid_to_bin(uuid(),1) | PRI DEFAULT_GENERATED  |
| `transfer_id` | binary(16) | NO | — | MUL   |
| `asset_id` | binary(16) | NO | — | MUL   |
| `dispatch_snapshot` | json | YES | — |    |
| `reception_outcome` | varchar(30) | NO | PENDIENTE |    |
| `resolution_id` | binary(16) | YES | — | MUL   |
| `created_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED  |
| `updated_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6)  |
| `organization_id` | binary(16) | NO | — | MUL   |

Claves foráneas (columna → tabla.columna):

- `fk_tenant_37`: `organization_id` → `transfer.organization_id`.
- `fk_tenant_37`: `transfer_id` → `transfer.id`.
- `fk_tenant_38`: `organization_id` → `asset.organization_id`.
- `fk_tenant_38`: `asset_id` → `asset.id`.
- `fk_tenant_item_resolution`: `organization_id` → `resolution.organization_id`.
- `fk_tenant_item_resolution`: `resolution_id` → `resolution.id`.
- `fk_transfer_item_asset`: `asset_id` → `asset.id`.
- `fk_transfer_item_resolution`: `resolution_id` → `resolution.id`.
- `fk_transfer_item_transfer`: `transfer_id` → `transfer.id`.

## user_role_assignment

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `id` | binary(16) | NO | uuid_to_bin(uuid(),1) | PRI DEFAULT_GENERATED  |
| `user_id` | binary(16) | NO | — | MUL   |
| `role_id` | binary(16) | NO | — | MUL   |
| `scope_kind` | varchar(20) | NO | — |    |
| `valid_from` | datetime(6) | NO | — |    |
| `valid_until` | datetime(6) | YES | — |    |
| `granted_by_user_id` | binary(16) | NO | — | MUL   |
| `revoked_at` | datetime(6) | YES | — |    |
| `revoke_reason` | varchar(500) | YES | — |    |
| `created_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED  |
| `updated_at` | datetime(6) | NO | CURRENT_TIMESTAMP(6) |  DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6)  |
| `active_flag` | tinyint | YES | — |  STORED GENERATED if((`revoked_at` is null),1,NULL) |
| `organization_id` | binary(16) | NO | — | MUL   |

Claves foráneas (columna → tabla.columna):

- `fk_tenant_11`: `organization_id` → `app_user.organization_id`.
- `fk_tenant_11`: `user_id` → `app_user.id`.
- `fk_tenant_12`: `organization_id` → `app_role.organization_id`.
- `fk_tenant_12`: `role_id` → `app_role.id`.
- `fk_tenant_13`: `organization_id` → `app_user.organization_id`.
- `fk_tenant_13`: `granted_by_user_id` → `app_user.id`.
- `fk_user_role_assignment_grantor`: `granted_by_user_id` → `app_user.id`.
- `fk_user_role_assignment_role`: `role_id` → `app_role.id`.
- `fk_user_role_assignment_user`: `user_id` → `app_user.id`.

## user_role_assignment_center

| Columna | Tipo | NULL | Predeterminado | Clave / generación |
|---|---|---|---|---|
| `role_assignment_id` | binary(16) | NO | — | PRI   |
| `center_id` | binary(16) | NO | — | PRI   |
| `organization_id` | binary(16) | NO | — | MUL   |

Claves foráneas (columna → tabla.columna):

- `fk_tenant_14`: `organization_id` → `user_role_assignment.organization_id`.
- `fk_tenant_14`: `role_assignment_id` → `user_role_assignment.id`.
- `fk_tenant_15`: `organization_id` → `center.organization_id`.
- `fk_tenant_15`: `center_id` → `center.id`.
- `fk_urac_assignment`: `role_assignment_id` → `user_role_assignment.id`.
- `fk_urac_center`: `center_id` → `center.id`.

