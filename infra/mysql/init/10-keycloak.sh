#!/bin/bash
set -euo pipefail

# Only locally generated hexadecimal secrets are accepted before interpolation into SQL.
root_password="$(cat /run/secrets/mysql_root_password)"
keycloak_password="$(cat /run/secrets/keycloak_db_password)"
[[ "$root_password" =~ ^[a-f0-9]{64}$ ]] || { echo 'Invalid root secret format' >&2; exit 1; }
[[ "$keycloak_password" =~ ^[a-f0-9]{64}$ ]] || { echo 'Invalid Keycloak secret format' >&2; exit 1; }

MYSQL_PWD="$root_password" mysql --protocol=socket -uroot <<SQL
CREATE DATABASE IF NOT EXISTS keycloak CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'keycloak'@'%' IDENTIFIED BY '${keycloak_password}';
GRANT ALL PRIVILEGES ON keycloak.* TO 'keycloak'@'%';
SQL
unset root_password keycloak_password
