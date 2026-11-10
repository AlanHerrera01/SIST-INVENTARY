#!/bin/sh
set -eu
MYSQL_PWD="$(cat /run/secrets/app_db_password)" \
  mysql --protocol=TCP -h127.0.0.1 -uinventory --database=inventory --silent --skip-column-names --execute='SELECT 1' >/dev/null
