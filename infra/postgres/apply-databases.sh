#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
ENV_FILE="$REPO_ROOT/.env"
SQL_FILE="$SCRIPT_DIR/databases.sql"

if [ ! -f "$ENV_FILE" ]; then
  echo "ERROR: .env file not found at $ENV_FILE"
  exit 1
fi

if [ ! -f "$SQL_FILE" ]; then
  echo "ERROR: databases.sql not found at $SQL_FILE"
  exit 1
fi

# Load simple KEY=VALUE pairs from .env
while IFS='=' read -r key value; do
  case "$key" in
    ''|\#*)
      continue
      ;;
  esac
  export "$key"="$value"
done < "$ENV_FILE"

export PGPASSWORD="${DB_PASSWORD:-${POSTGRES_PASSWORD:-}}"
DB_USER="${DB_USER:-${POSTGRES_USER:-}}"

echo "Applying database provisioning record from $SQL_FILE"
output="$(cd "$REPO_ROOT" && docker compose exec -T postgres psql -U "$DB_USER" -d postgres -v ON_ERROR_STOP=0 < "$SQL_FILE" 2>&1)" || status=$?

printf '%s\n' "$output"

if [ "${status:-0}" -ne 0 ]; then
  if printf '%s\n' "$output" | grep -qE 'already exists'; then
    echo "Note: existing databases were skipped as expected."
    exit 0
  fi
  echo "ERROR: Database provisioning failed with status ${status:-1}."
  exit "${status:-1}"
fi

echo "Database provisioning completed."
