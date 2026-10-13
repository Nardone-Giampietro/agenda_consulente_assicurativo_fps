package xyz.nardone.agenda_wa.servizio.repository;

import java.util.ArrayList;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import xyz.nardone.agenda_wa.servizio.repository.query.Queries;

abstract class OwnedQuerySupport {
  protected final JdbcTemplate jdbc;

  protected OwnedQuerySupport(JdbcTemplate jdbc, SqlSchemaRepository schema) {
    this.jdbc = jdbc;
  }

  protected long count(String table, Queries.Filter filter) {
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM " + table + " WHERE " + filter.sql(), Long.class, filter.args());
  }

  protected <T> Optional<T> one(String table, RowMapper<T> mapper, String id, String owner) {
    return jdbc
        .query("SELECT * FROM " + table + " WHERE id=? AND owner_id=?", mapper, id, owner)
        .stream()
        .findFirst();
  }

  protected <T> RepositoryPage<T> page(
      String table, RowMapper<T> mapper, Queries.Filter filter, String order, int page, int size) {
    Queries.paginate(page, size);
    var args = new ArrayList<>(filter.values());
    args.add(size);
    args.add((long) page * size);
    return new RepositoryPage<>(
        jdbc.query(
            "SELECT * FROM "
                + table
                + " WHERE "
                + filter.sql()
                + " ORDER BY "
                + order
                + " LIMIT ? OFFSET ?",
            mapper,
            args.toArray()),
        count(table, filter));
  }

  protected long delete(String table, Queries.Filter filter) {
    return jdbc.update("DELETE FROM " + table + " WHERE " + filter.sql(), filter.args());
  }
}
