package xyz.nardone.agenda_wa.servizio.repository;

import static xyz.nardone.agenda_wa.servizio.exception.ApplicationErrors.missing;

import java.time.LocalDate;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import xyz.nardone.agenda_wa.servizio.entity.Callback;
import xyz.nardone.agenda_wa.servizio.repository.query.Queries;

@Repository
public class CallbackRepository extends OwnedQuerySupport implements CallbackRepositoryCustom {
  public CallbackRepository(JdbcTemplate jdbc, SqlSchemaRepository schema) {
    super(jdbc, schema);
  }

  public Optional<Callback> findByIdAndOwnerId(String id, String owner) {
    return one("callback", PersistenceRows.CALLBACK, id, owner);
  }

  public List<Callback> findByOwnerId(String owner) {
    return jdbc.query(
        "SELECT * FROM callback WHERE owner_id=? ORDER BY id", PersistenceRows.CALLBACK, owner);
  }

  public long countByOwnerId(String owner) {
    return count("callback", Queries.owned(owner));
  }

  public long deleteByIdAndOwnerId(String id, String owner) {
    return delete("callback", Queries.owned(owner).and("id=?", id));
  }

  public long deleteByOwnerId(String owner) {
    return delete("callback", Queries.owned(owner));
  }

  public long deleteByOwnerIdAndClientId(String owner, String client) {
    return delete("callback", Queries.owned(owner).and("client_id=?", client));
  }

  public long countByOwnerIdAndData(String owner, LocalDate date) {
    return count("callback", Queries.owned(owner).and("data=?", date));
  }

  public List<Callback> findByOwnerIdAndDataOrderByIdAsc(
      String owner, LocalDate date, Pageable limit) {
    return page(
            "callback",
            PersistenceRows.CALLBACK,
            Queries.owned(owner).and("data=?", date),
            "id ASC",
            limit.getPageNumber(),
            limit.getPageSize())
        .items();
  }

  public Callback insert(Callback c) {
    jdbc.update(
        "INSERT INTO callback(id,owner_id,data,data_appuntamento,client_id,note)"
            + " VALUES(?,?,?,?,?,?)",
        c.id(),
        c.ownerId(),
        c.data(),
        c.dataAppuntamento(),
        c.clientId(),
        c.note());
    return c;
  }

  public Callback update(Callback c) {
    if (jdbc.update(
            "UPDATE callback SET data=?,data_appuntamento=?,client_id=?,note=? WHERE id=? AND"
                + " owner_id=?",
            c.data(),
            c.dataAppuntamento(),
            c.clientId(),
            c.note(),
            c.id(),
            c.ownerId())
        == 0) throw missing();
    return c;
  }

  public RepositoryPage<Callback> search(
      String owner, LocalDate dal, LocalDate al, LocalDate today, int page, int size) {
    Queries.paginate(page, size);
    var f = Queries.activities(owner, dal, al, null);
    var args = new ArrayList<>(f.values());
    args.add(today);
    args.add(size);
    args.add((long) page * size);
    var rows =
        jdbc.query(
            "SELECT * FROM callback WHERE "
                + f.sql()
                + " ORDER BY CASE WHEN data=? THEN 0 ELSE 1 END,data ASC NULLS FIRST,id ASC LIMIT ?"
                + " OFFSET ?",
            PersistenceRows.CALLBACK,
            args.toArray());
    return new RepositoryPage<>(rows, count("callback", f));
  }
}
