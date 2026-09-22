package com.qvety.reference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.sql.Types;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

/**
 * Something the clinic sells: consultation, vaccine, surgery (glossary: Service). The Java class carries
 * the Clinic prefix only because {@code Service} is Spring's own annotation; the table, the API path, and
 * the JSON schema are all plain "service".
 *
 * The currency is the practice currency, copied on create and never taken from a request. An invoice line
 * (part 14) snapshots the name and price, so editing either never moves an old invoice.
 */
@Entity
@Table(name = "services")
@Getter
@Setter
@NoArgsConstructor
public class ClinicService extends ReferenceEntity {

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @JdbcTypeCode(Types.CHAR)   // Postgres char(n) is bpchar; validate needs the JDBC CHAR type, as on practices
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
}
