package xyz.nardone.agenda_wa.servizio.repository;

import static xyz.nardone.agenda_wa.servizio.exception.ApplicationErrors.missing;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import xyz.nardone.agenda_wa.servizio.entity.CatalogItem;
import xyz.nardone.agenda_wa.servizio.repository.query.Queries;

@Repository
public class CatalogItemRepository extends OwnedQuerySupport {
  public CatalogItemRepository(JdbcTemplate jdbc, SqlSchemaRepository schema) {
    super(jdbc, schema);
  }

  public Optional<CatalogItem> findByIdAndOwnerId(String id, String owner) {
    return one("cataloghi", PersistenceRows.CATALOG, id, owner);
  }

  public Optional<CatalogItem> findByIdAndOwnerIdAndTipo(String id, String owner, String tipo) {
    return findByIdAndOwnerId(id, owner).filter(c -> c.tipo().equals(tipo));
  }

  public List<CatalogItem> findByOwnerId(String owner) {
    return jdbc.query(
        "SELECT * FROM cataloghi WHERE owner_id=? ORDER BY id", PersistenceRows.CATALOG, owner);
  }

  public List<CatalogItem> findByOwnerIdAndTipoOrderByNomeAsc(String owner, String tipo) {
    return jdbc.query(
        "SELECT * FROM cataloghi WHERE owner_id=? AND tipo=? ORDER BY nome,id",
        PersistenceRows.CATALOG,
        owner,
        tipo);
  }

  public long countByOwnerId(String owner) {
    return count("cataloghi", Queries.owned(owner));
  }

  public long deleteByIdAndOwnerId(String id, String owner) {
    return delete("cataloghi", Queries.owned(owner).and("id=?", id));
  }

  public long deleteByOwnerId(String owner) {
    return delete("cataloghi", Queries.owned(owner));
  }

  public long lock(String owner, String id) {
    return jdbc.update(
        "UPDATE cataloghi SET revision=revision+1 WHERE owner_id=? AND id=?", owner, id);
  }

  public CatalogItem insert(CatalogItem c) {
    jdbc.update(
        "INSERT INTO cataloghi(id,owner_id,tipo,nome,chiave) VALUES(?,?,?,?,?)",
        c.id(),
        c.ownerId(),
        c.tipo(),
        c.nome(),
        c.chiave());
    return c;
  }

  public void insert(List<CatalogItem> items) {
    items.forEach(this::insert);
  }

  public CatalogItem update(CatalogItem c) {
    if (jdbc.update(
            "UPDATE cataloghi SET nome=?,chiave=? WHERE id=? AND owner_id=? AND tipo=?",
            c.nome(),
            c.chiave(),
            c.id(),
            c.ownerId(),
            c.tipo())
        == 0) throw missing();
    return c;
  }
}
