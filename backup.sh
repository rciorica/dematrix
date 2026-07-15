#!/bin/bash
# PostgreSQL backup script for enterprise document analyzer

BACKUP_DIR="/backups"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/docdb_backup_${TIMESTAMP}.sql"

# Ensure backup directory exists
mkdir -p ${BACKUP_DIR}

# Perform backup
echo "[$(date)] Starting backup to ${BACKUP_FILE}..."
pg_dump -U docuser -d docdb -h postgres -F p > ${BACKUP_FILE}

if [ $? -eq 0 ]; then
    echo "[$(date)] Backup completed successfully: ${BACKUP_FILE}"
    # Cleanup old backups (older than 7 days)
    find ${BACKUP_DIR} -name "docdb_backup_*.sql" -mtime +7 -delete
    echo "[$(date)] Removed backups older than 7 days"
else
    echo "[$(date)] Backup failed!"
    exit 1
fi
