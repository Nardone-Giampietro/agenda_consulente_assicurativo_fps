package xyz.nardone.agenda_wa.servizio.repository;

import static xyz.nardone.agenda_wa.servizio.exception.ApplicationErrors.missing;
import static xyz.nardone.agenda_wa.servizio.util.CommonUtil.key;

import java.text.Collator;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import xyz.nardone.agenda_wa.servizio.entity.Client;
import xyz.nardone.agenda_wa.servizio.entity.ClientType;
import xyz.nardone.agenda_wa.servizio.repository.query.Queries;

@Repository
public class ClientRepository extends OwnedQuerySupport implements ClientRepositoryCustom {
  public ClientRepository(JdbcTemplate jdbc, SqlSchemaRepository schema) {
    super(jdbc, schema);
  }

  public Optional<Client> findByIdAndOwnerId(String id, String owner) {
    return one("clienti", PersistenceRows.CLIENT, id, owner);
  }

  public List<Client> findByOwnerId(String owner) {
    return jdbc.query(
        "SELECT * FROM clienti WHERE owner_id=? ORDER BY id", PersistenceRows.CLIENT, owner);
  }

  public long countByOwnerId(String owner) {
    return count("clienti", Queries.owned(owner));
  }

  public long deleteByIdAndOwnerId(String id, String owner) {
    return delete("clienti", Queries.owned(owner).and("id=?", id));
  }

  public long deleteByOwnerId(String owner) {
    return delete("clienti", Queries.owned(owner));
  }

  public List<Client> findByOwnerIdAndIdIn(String owner, Collection<String> ids) {
    if (ids.isEmpty()) return List.of();
    var args = new ArrayList<Object>();
    args.add(owner);
    args.addAll(ids);
    return jdbc.query(
        "SELECT * FROM clienti WHERE owner_id=? AND id IN ("
            + String.join(",", Collections.nCopies(ids.size(), "?"))
            + ")",
        PersistenceRows.CLIENT,
        args.toArray());
  }

  public long lock(String owner, String id) {
    return jdbc.update(
        "UPDATE clienti SET revision=revision+1 WHERE owner_id=? AND id=?", owner, id);
  }

  public Client insert(Client c) {
    jdbc.update(
        "INSERT INTO"
            + " clienti(id,owner_id,tipo,nome,cognome,nome_legale,telefono,search_text,sort_key)"
            + " VALUES(?,?,?,?,?,?,?,?,?)",
        c.id(),
        c.ownerId(),
        c.tipo().name(),
        c.nome(),
        c.cognome(),
        c.nomeLegale(),
        c.telefono(),
        search(c),
        sort(c));
    return c;
  }

  public long updateDetails(String owner, Client c) {
    return jdbc.update(
        "UPDATE clienti SET"
            + " tipo=?,nome=?,cognome=?,nome_legale=?,telefono=?,search_text=?,sort_key=? WHERE"
            + " owner_id=? AND id=?",
        c.tipo().name(),
        c.nome(),
        c.cognome(),
        c.nomeLegale(),
        c.telefono(),
        search(c),
        sort(c),
        owner,
        c.id());
  }

  private String search(Client c) {
    return key(
        Objects.toString(c.nome(), "")
            + " "
            + Objects.toString(c.cognome(), "")
            + " "
            + Objects.toString(c.nomeLegale(), ""));
  }

  private byte[] sort(Client c) {
    Collator collation = Collator.getInstance(Locale.ITALIAN);
    collation.setStrength(Collator.SECONDARY);
    collation.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
    return collation
        .getCollationKey(c.tipo() == ClientType.AZIENDA ? c.nomeLegale() : c.cognome())
        .toByteArray();
  }

  public RepositoryPage<Client> search(String owner, String search, int page, int size) {
    return page(
        "clienti",
        PersistenceRows.CLIENT,
        Queries.clients(owner, search),
        "sort_key ASC,id ASC",
        page,
        size);
  }

  public RepositoryPage<Client.AppointmentSummary> history(
      String owner, String clientId, int page, int size) {
    Queries.paginate(page, size);
    findByIdAndOwnerId(clientId, owner).orElseThrow(() -> missing());
    var f = Queries.owned(owner).and("client_id=?", clientId);
    var args = new ArrayList<>(f.values());
    args.add(size);
    args.add((long) page * size);
    var rows =
        jdbc.query(
            "SELECT id,data,esito FROM appuntamenti WHERE "
                + f.sql()
                + " ORDER BY data DESC,id ASC LIMIT ? OFFSET ?",
            (r, n) ->
                new Client.AppointmentSummary(
                    r.getString("id"),
                    r.getObject("data", java.time.LocalDate.class),
                    r.getString("esito")),
            args.toArray());
    return new RepositoryPage<>(rows, count("appuntamenti", f));
  }
}
