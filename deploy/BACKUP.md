# Backup and restore

The whole database is dumped every night and a restore is rehearsed every quarter. A backup nobody has
restored is not a backup, so the drill log at the bottom of this page is the part that matters.

## What runs

The `backup` service in `docker/docker-compose.yml` is a `postgres:18` image with `cron` and the MinIO
client. At 03:00 Africa/Cairo it runs `docker/backup/backup.sh`, which:

1. `pg_dump -Fc` of the whole database (custom format: compressed, and `pg_restore` can rebuild selectively).
2. Uploads it to the `qvety-backups` bucket as `qvety-<timestamp>.dump`.
3. Removes objects older than `RETENTION_DAYS`, which is 30.

`pg_dump` comes from the same major version as the server on purpose: an older client refuses to dump a
newer server. The MinIO client is copied from the `minio/mc` image rather than downloaded, because the
unversioned download URL now answers `410 Gone`.

Run it by hand at any time:

```bash
docker exec qvety-backup /usr/local/bin/backup.sh
docker exec qvety-backup mc ls store/qvety-backups/
```

## How to restore

```bash
# 1. take the dump you want
DUMP=$(docker exec qvety-backup mc ls store/qvety-backups/ | tail -1 | awk '{print $NF}')
docker exec qvety-backup mc cp "store/qvety-backups/$DUMP" /tmp/drill.dump

# 2. rebuild it into a scratch database, never over the live one
docker exec qvety-backup psql -h postgres -U qvety_owner -d postgres \
  -c "DROP DATABASE IF EXISTS qvety_drill;" -c "CREATE DATABASE qvety_drill OWNER qvety_owner;"
docker exec qvety-backup pg_restore -h postgres -U qvety_owner -d qvety_drill --no-owner /tmp/drill.dump

# 3. point the application at it and log in
./mvnw spring-boot:run -Dspring-boot.run.profiles=local \
  -Dspring-boot.run.arguments=--spring.datasource.url=jdbc:postgresql://localhost:5433/qvety_drill
```

A restore counts only when someone has logged in against it and seen real rows. Check that the row-level
security policies came back too (`SELECT count(*) FROM pg_policies WHERE schemaname = 'public'`): a dump
that restores tables without policies would be a silent tenant leak.

## Drill log

Before the first paying clinic, and at least once a quarter after.

| Date | Who | Dump | Result |
| --- | --- | --- | --- |
| 2026-09-23 | hazem | `qvety-2026-09-23T19-04-12Z.dump` | Restored into `qvety_drill`: 12 RLS policies, 2 practices, 6 users, 5 clients, 10 appointments, 1 platform user. Application started against it; clinic and platform logins both answered 200 and the client list loaded. |
