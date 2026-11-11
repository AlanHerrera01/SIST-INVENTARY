#!/bin/bash
set -euo pipefail
export KC_DB_PASSWORD="$(cat /run/secrets/keycloak_db_password)"
export KC_BOOTSTRAP_ADMIN_PASSWORD="$(cat /run/secrets/keycloak_admin_password)"
export INVENTORY_CLIENT_SECRET="$(cat /run/secrets/oidc_client_secret)"
export OIDC_CLIENT_SECRET="$(cat /run/secrets/oidc_client_secret)"
export OIDC_ADMIN_CLIENT_SECRET="$(cat /run/secrets/oidc_admin_client_secret)"
export APP_PUBLIC_URL="${APP_PUBLIC_URL:-http://localhost:5173}"
# Habilita la sustitución de ${VARIABLE} en realm-inventory-dev.json.
export JAVA_OPTS_APPEND="${JAVA_OPTS_APPEND:-} -Dkeycloak.migration.replace-placeholders=true"
exec /opt/keycloak/bin/kc.sh start-dev --import-realm
