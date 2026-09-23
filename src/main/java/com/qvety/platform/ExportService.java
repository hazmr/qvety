package com.qvety.platform;

import com.qvety.tenant.TenantContext;
import com.qvety.tenant.TenantScope;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ObjectMapper;

/**
 * Everything one practice owns, as one JSON document, so a clinic can leave with its data.
 *
 * Two rules make it trustworthy. It runs under the practice's own tenant context, never system context,
 * so every row passes the same row-level security as a normal read and no other practice can leak in.
 * And the table list is read from the database catalog rather than written down: every table carrying a
 * practice_id is in the export, so a table added in a later part joins by existing instead of by someone
 * remembering. That is also why the isolation test can compare the export set with the RLS set.
 *
 * The rows are streamed. A clinic with years of history should not have to fit in memory twice.
 */
@Service
public class ExportService {

    /** Every tenant table, in a stable order. Same question the isolation test asks of RLS. */
    private static final String TENANT_TABLES = """
        SELECT table_name FROM information_schema.columns
        WHERE table_schema = 'public' AND column_name = 'practice_id'
        ORDER BY table_name
        """;

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final TransactionTemplate tx;

    public ExportService(JdbcTemplate jdbc, ObjectMapper json, PlatformTransactionManager txManager) {
        this.jdbc = jdbc;
        this.json = json;
        this.tx = new TransactionTemplate(txManager);
    }

    /** The table names that will appear in an export. Read from the catalog, never maintained by hand. */
    public List<String> tables() {
        return jdbc.queryForList(TENANT_TABLES, String.class);
    }

    /**
     * Writes the whole document. Called from the servlet's streaming body, which may be a different
     * thread from the request, so the tenant scope is set here rather than inherited.
     */
    public void writeTo(UUID practiceId, OutputStream out) {
        TenantContext.runAs(new TenantScope(practiceId, null), () -> tx.execute(status -> {
            try (var generator = json.createGenerator(out)) {
                generator.writeStartObject();
                generator.writeStringProperty("exported_at", OffsetDateTime.now().toString());
                writePractice(generator, practiceId);
                generator.writeName("tables");
                generator.writeStartObject();
                for (var table : tables()) {
                    generator.writeName(table);
                    generator.writeStartArray();
                    writeRows(generator, table);
                    generator.writeEndArray();
                }
                generator.writeEndObject();
                // Files live in object storage from part 13; until then the manifest is honestly empty.
                generator.writeName("attachments");
                generator.writeStartArray();
                generator.writeEndArray();
                generator.writeEndObject();
                generator.flush();
            }
            return null;
        }));
    }

    /** The practice row itself is not a tenant table (it has no practice_id), so it is written separately. */
    private void writePractice(JsonGenerator generator, UUID practiceId) {
        generator.writeName("practice");
        generator.writeStartObject();
        jdbc.query("SELECT * FROM practices WHERE id = ?", rs -> {
            writeColumns(generator, rs);
        }, practiceId);
        generator.writeEndObject();
    }

    private void writeRows(JsonGenerator generator, String table) {
        // The table name comes from the catalog, never from a caller, so it cannot be injected.
        jdbc.query("SELECT * FROM " + table, (ResultSet rs) -> {
            generator.writeStartObject();
            writeColumns(generator, rs);
            generator.writeEndObject();
        });
    }

    private static void writeColumns(JsonGenerator generator, ResultSet rs) throws SQLException {
        var meta = rs.getMetaData();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            var name = meta.getColumnLabel(i);
            var value = rs.getObject(i);
            if (value == null) {
                generator.writeNullProperty(name);
            } else {
                generator.writeStringProperty(name, String.valueOf(value));
            }
        }
    }
}
