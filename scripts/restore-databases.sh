#!/bin/bash
set -euo pipefail

if [ -z "${1:-}" ]; then
  echo "Usage: $0 <backup_directory>"
  echo "Example: $0 ./backups/20261031_220000"
  exit 1
fi

BACKUP_DIR="$1"
POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-digitalofficeos-postgres-1}"
POSTGRES_USER="${POSTGRES_USER:-digitaloffice}"

if [ ! -d "${BACKUP_DIR}" ]; then
  echo "Error: Directory ${BACKUP_DIR} does not exist."
  exit 1
fi

DATABASES=(
  "identity_db"
  "organization_db"
  "client_db"
  "project_db"
  "work_db"
  "document_db"
  "notification_db"
  "communication_db"
  "finance_db"
)

echo "Restoring databases from ${BACKUP_DIR}..."

for DB in "${DATABASES[@]}"; do
  DUMP_FILE="${BACKUP_DIR}/${DB}.dump"
  if [ -f "${DUMP_FILE}" ]; then
    echo "Restoring database ${DB} from ${DUMP_FILE}..."
    docker exec -i "${POSTGRES_CONTAINER}" pg_restore -U "${POSTGRES_USER}" -d "${DB}" --clean --if-exists -v < "${DUMP_FILE}" || true
    echo "Restored ${DB}."
  else
    echo "Warning: ${DUMP_FILE} not found, skipping."
  fi
done

echo "Database restoration completed."

