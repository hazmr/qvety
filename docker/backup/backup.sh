#!/bin/sh
# Nightly dump of the whole database to object storage, and prune what is older than the retention.
# Custom format (-Fc), because pg_restore can then rebuild selectively and it compresses on the way out.
set -eu

: "${PGHOST:?}" "${PGUSER:?}" "${PGPASSWORD:?}" "${PGDATABASE:?}"
: "${BACKUP_BUCKET:?}" "${MINIO_URL:?}" "${MINIO_ACCESS_KEY:?}" "${MINIO_SECRET_KEY:?}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"

stamp="$(date -u +%Y-%m-%dT%H-%M-%SZ)"
dump="/tmp/qvety-${stamp}.dump"

echo "[backup] dumping ${PGDATABASE} to ${dump}"
pg_dump -Fc -f "${dump}" "${PGDATABASE}"

mc alias set store "${MINIO_URL}" "${MINIO_ACCESS_KEY}" "${MINIO_SECRET_KEY}" >/dev/null
mc mb --ignore-existing "store/${BACKUP_BUCKET}" >/dev/null

echo "[backup] uploading to ${BACKUP_BUCKET}"
mc cp "${dump}" "store/${BACKUP_BUCKET}/qvety-${stamp}.dump"
rm -f "${dump}"

echo "[backup] pruning dumps older than ${RETENTION_DAYS} days"
mc rm --recursive --force --older-than "${RETENTION_DAYS}d" "store/${BACKUP_BUCKET}/" || true

echo "[backup] done ${stamp}"
