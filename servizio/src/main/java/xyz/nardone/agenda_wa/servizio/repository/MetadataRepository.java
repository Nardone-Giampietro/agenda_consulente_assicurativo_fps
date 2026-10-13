package xyz.nardone.agenda_wa.servizio.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MetadataRepository {
  private final JdbcTemplate jdbc;

  public MetadataRepository(JdbcTemplate jdbc, SqlSchemaRepository schema) {
    this.jdbc = jdbc;
  }

  public boolean catalogsInitialized(String owner) {
    return jdbc.queryForObject(
            "SELECT COUNT(*) FROM metadati WHERE id=? AND owner_id=?",
            Long.class,
            "cataloghi-v1:" + owner,
            owner)
        > 0;
  }

  public void markCatalogsInitialized(String owner) {
    jdbc.update(
        "MERGE INTO metadati(id,owner_id) KEY(id) VALUES(?,?)", "cataloghi-v1:" + owner, owner);
  }
}
