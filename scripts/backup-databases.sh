#!/bin/bash
set -euo pipefail

BACKUP_DIR="${1:-./backups/$(date +%Y%m%d_%H%M%S)}"
POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-digitalofficeos-postgres-1}"
POSTGRES_USER="${POSTGRES_USER:-digitaloffice}"

mkdir -p "${BACKUP_DIR}"

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

echo "Starting automated database backups to ${BACKUP_DIR}..."

for DB in "${DATABASES[@]}"; do
  echo "Backing up database: ${DB}..."
  docker exec -t "${POSTGRES_CONTAINER}" pg_dump -U "${POSTGRES_USER}" -d "${DB}" -F c -b -v > "${BACKUP_DIR}/${DB}.dump"
  echo "Successfully backed up ${DB} to ${BACKUP_DIR}/${DB}.dump"
done

echo "All 9 databases successfully backed up."

