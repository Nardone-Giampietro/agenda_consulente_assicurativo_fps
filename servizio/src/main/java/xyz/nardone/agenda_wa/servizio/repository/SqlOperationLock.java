package xyz.nardone.agenda_wa.servizio.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** All writers and snapshots acquire this row until Spring commits or rolls back. */
@Repository
public class SqlOperationLock {
  private final JdbcTemplate jdbc;

  public SqlOperationLock(JdbcTemplate jdbc, SqlSchemaRepository schema) {
    this.jdbc = jdbc;
  }

  public void acquire() {
    if (!TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("La modifica richiede una transazione.");
    jdbc.queryForObject("SELECT id FROM operation_lock WHERE id=1 FOR UPDATE", Integer.class);
  }
}
