# The MinIO client comes from its own image: the unversioned download URL now answers 410, and pinning a
# release by hand would rot the same way.
FROM minio/mc AS mc

# postgres:18 so pg_dump matches the server exactly; a newer server than client refuses to dump.
FROM postgres:18

RUN set -eux; \
    apt-get update; \
    apt-get install -y --no-install-recommends cron ca-certificates; \
    rm -rf /var/lib/apt/lists/*

COPY --from=mc /usr/bin/mc /usr/local/bin/mc
COPY backup.sh /usr/local/bin/backup.sh
RUN chmod +x /usr/local/bin/mc /usr/local/bin/backup.sh

# 03:00 in the clinic's timezone, when nobody is booking.
RUN echo '0 3 * * * . /etc/environment; /usr/local/bin/backup.sh >> /var/log/backup.log 2>&1' > /etc/cron.d/qvety-backup \
    && chmod 0644 /etc/cron.d/qvety-backup \
    && crontab /etc/cron.d/qvety-backup \
    && touch /var/log/backup.log

# cron does not inherit the container's environment, so it is written out where the job can source it.
CMD ["sh", "-c", "printenv | sed 's/^\\(.*\\)$/export \\1/' > /etc/environment && cron && tail -f /var/log/backup.log"]
