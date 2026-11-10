#!/bin/sh
set -eu
project_root="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
umask 077
mkdir -p "$project_root/.secrets"
chmod 700 "$project_root/.secrets"
for name in mysql_root_password app_db_password keycloak_db_password keycloak_admin_password; do
  path="$project_root/.secrets/$name"
  if [ ! -e "$path" ]; then
    # noclobber avoids replacing credentials in concurrent setup runs.
    (set -C; openssl rand -hex 32 > "$path")
  fi
  # The owner-only directory protects host access; readable mounts support non-root containers.
  chmod 644 "$path"
done
if [ ! -e "$project_root/.env" ]; then
  cp "$project_root/.env.example" "$project_root/.env"
fi
printf '%s\n' 'Entorno local preparado. Los secretos existentes no fueron modificados.'
