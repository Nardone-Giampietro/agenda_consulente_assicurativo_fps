package xyz.nardone.agenda_wa.servizio.repository;

import static xyz.nardone.agenda_wa.servizio.exception.ApplicationErrors.missing;

import java.time.LocalDate;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import xyz.nardone.agenda_wa.servizio.DTO.activity.AppointmentSelectionDTO;
import xyz.nardone.agenda_wa.servizio.entity.Appointment;
import xyz.nardone.agenda_wa.servizio.repository.query.Queries;

@Repository
public class AppointmentRepository extends OwnedQuerySupport
    implements AppointmentRepositoryCustom {
  public AppointmentRepository(JdbcTemplate jdbc, SqlSchemaRepository schema) {
    super(jdbc, schema);
  }

  public Optional<Appointment> findByIdAndOwnerId(String id, String owner) {
    return one("appuntamenti", PersistenceRows.APPOINTMENT, id, owner);
  }

  public List<Appointment> findByOwnerId(String owner) {
    return jdbc.query(
        "SELECT * FROM appuntamenti WHERE owner_id=? ORDER BY id",
        PersistenceRows.APPOINTMENT,
        owner);
  }

  public List<Appointment> clientsWithOutcome(String owner, String esito) {
    return jdbc.query(
        "SELECT * FROM appuntamenti WHERE owner_id=? AND esito=?",
        PersistenceRows.APPOINTMENT,
        owner,
        esito);
  }

  public long countByOwnerId(String owner) {
    return count("appuntamenti", Queries.owned(owner));
  }

  public long deleteByIdAndOwnerId(String id, String owner) {
    return delete("appuntamenti", Queries.owned(owner).and("id=?", id));
  }

  public long deleteByOwnerId(String owner) {
    return delete("appuntamenti", Queries.owned(owner));
  }

  public long deleteByOwnerIdAndClientId(String owner, String client) {
    return delete("appuntamenti", Queries.owned(owner).and("client_id=?", client));
  }

  public boolean existsByOwnerIdAndEsito(String owner, String esito) {
    return count("appuntamenti", Queries.owned(owner).and("esito=?", esito)) > 0;
  }

  public boolean existsByOwnerIdAndPolizza(String owner, String polizza) {
    return count("appuntamenti", Queries.owned(owner).and("polizza=?", polizza)) > 0;
  }

  private String words(Appointment a) {
    var c = a.cliente();
    return Queries.words(
        Objects.toString(c.nome(), "")
            + " "
            + Objects.toString(c.cognome(), "")
            + " "
            + Objects.toString(c.nomeLegale(), "")
            + " "
            + a.note());
  }

  private Object[] values(Appointment a) {
    var c = a.cliente();
    return new Object[] {
      a.data(),
      a.clientId(),
      c.tipo().name(),
      c.nome(),
      c.cognome(),
      c.nomeLegale(),
      a.esito(),
      a.polizza(),
      a.premio(),
      a.note(),
      words(a),
      a.id(),
      a.ownerId()
    };
  }

  public Appointment insert(Appointment a) {
    jdbc.update(
        "INSERT INTO"
            + " appuntamenti(data,client_id,client_tipo,client_nome,client_cognome,client_nome_legale,esito,polizza,premio,note,search_words,id,owner_id)"
            + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",
        values(a));
    return a;
  }

  public Appointment update(Appointment a) {
    if (jdbc.update(
            "UPDATE appuntamenti SET"
                + " data=?,client_id=?,client_tipo=?,client_nome=?,client_cognome=?,client_nome_legale=?,esito=?,polizza=?,premio=?,note=?,search_words=?"
                + " WHERE id=? AND owner_id=?",
            values(a))
        == 0) throw missing();
    return a;
  }

  public long updateClientDetails(String owner, String client, Appointment.ClientDetails details) {
    var list =
        jdbc.query(
            "SELECT * FROM appuntamenti WHERE owner_id=? AND client_id=?",
            PersistenceRows.APPOINTMENT,
            owner,
            client);
    list.forEach(
        a ->
            update(
                new Appointment(
                    a.id(),
                    owner,
                    a.data(),
                    client,
                    details,
                    a.esito(),
                    a.polizza(),
                    a.premio(),
                    a.note())));
    return list.size();
  }

  public long renameOutcome(String owner, String previous, String name) {
    return jdbc.update(
        "UPDATE appuntamenti SET esito=? WHERE owner_id=? AND esito=?", name, owner, previous);
  }

  public long renamePolicy(String owner, String previous, String name) {
    return jdbc.update(
        "UPDATE appuntamenti SET polizza=? WHERE owner_id=? AND polizza=?", name, owner, previous);
  }

  public List<Appointment> findByOwnerIdAndDataGreaterThanEqualOrderByDataAscIdAsc(
      String owner, LocalDate date, Pageable limit) {
    return page(
            "appuntamenti",
            PersistenceRows.APPOINTMENT,
            Queries.activities(owner, date, null, null),
            "data ASC,id ASC",
            limit.getPageNumber(),
            limit.getPageSize())
        .items();
  }

  public RepositoryPage<Appointment> search(
      String owner, LocalDate dal, LocalDate al, String esito, String search, int page, int size) {
    return page(
        "appuntamenti",
        PersistenceRows.APPOINTMENT,
        Queries.appointments(owner, dal, al, esito, search),
        "data DESC,id ASC",
        page,
        size);
  }

  private Queries.Filter selectionFilter(String owner, AppointmentSelectionDTO s, String esito) {
    return Queries.appointmentSelection(owner, s, esito);
  }

  public RepositoryPage<Appointment> selection(
      String owner, AppointmentSelectionDTO s, String esito) {
    return page(
        "appuntamenti",
        PersistenceRows.APPOINTMENT,
        selectionFilter(owner, s, esito),
        "data DESC,id ASC",
        0,
        5);
  }

  public long countSelection(String owner, AppointmentSelectionDTO s, String esito) {
    return count("appuntamenti", selectionFilter(owner, s, esito));
  }

  public List<String> selectionClientIds(String owner, AppointmentSelectionDTO s, String esito) {
    var f = selectionFilter(owner, s, esito);
    return jdbc.queryForList(
        "SELECT DISTINCT client_id FROM appuntamenti WHERE " + f.sql(), String.class, f.args());
  }

  public long deleteSelection(String owner, AppointmentSelectionDTO s, String esito) {
    return delete("appuntamenti", selectionFilter(owner, s, esito));
  }

  public List<OutcomeTotal> totals(
      String owner, LocalDate dal, LocalDate al, String esito, String search) {
    var f = Queries.appointments(owner, dal, al, esito, search);
    return jdbc.query(
        "SELECT esito,COUNT(*) numero,SUM(premio) premi FROM appuntamenti WHERE "
            + f.sql()
            + " GROUP BY esito ORDER BY esito",
        (r, n) ->
            new OutcomeTotal(r.getString("esito"), r.getLong("numero"), r.getBigDecimal("premi")),
        f.args());
  }
}
