package com.qvety.tenant;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Session;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionDefinition;

/**
 * Every Spring-managed transaction (services, repositories, the JWT filter's lookup) begins here.
 * Right after the transaction opens, the practice and user ids are written into the Postgres session
 * with set_config(..., true) — transaction-local, so a pooled connection returns clean. Row-level
 * security reads them from current_practice_id() / current_user_id().
 *
 * No tenant and no SystemContext: the transaction is rolled back before any statement runs.
 */
public class TenantTransactionManager extends JpaTransactionManager {

    public TenantTransactionManager(EntityManagerFactory emf) {
        super(emf);
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        super.doBegin(transaction, definition);
        var scope = TenantContext.current();
        if (scope.isEmpty()) {
            if (SystemContext.isActive()) {
                return;   // deliberately no tenant: RLS hides every tenant row
            }
            doCleanupAfterCompletion(transaction);
            throw new NoTenantException();
        }
        var em = EntityManagerFactoryUtils.getTransactionalEntityManager(getEntityManagerFactory());
        if (em == null) {
            throw new IllegalStateException("no transactional EntityManager after doBegin");
        }
        var practiceId = scope.get().practiceId().toString();
        var userId = scope.get().userId() == null ? "" : scope.get().userId().toString();
        em.unwrap(Session.class).doWork(connection -> {
            try (var ps = connection.prepareStatement("SELECT set_config('app.practice_id', ?, true), set_config('app.user_id', ?, true)")) {
                ps.setString(1, practiceId);
                ps.setString(2, userId);
                ps.execute();
            }
        });
    }
}
