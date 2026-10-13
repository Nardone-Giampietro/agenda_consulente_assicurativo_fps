package xyz.nardone.agenda_wa.servizio.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;
import xyz.nardone.agenda_wa.servizio.entity.*;

final class PersistenceRows {
  private PersistenceRows() {}

  static final RowMapper<Client> CLIENT =
      (r, n) ->
          new Client(
              r.getString("id"),
              r.getString("owner_id"),
              ClientType.valueOf(r.getString("tipo")),
              r.getString("nome"),
              r.getString("cognome"),
              r.getString("nome_legale"),
              r.getString("telefono"),
              List.of());
  static final RowMapper<CatalogItem> CATALOG =
      (r, n) ->
          new CatalogItem(
              r.getString("id"),
              r.getString("owner_id"),
              r.getString("tipo"),
              r.getString("nome"),
              r.getString("chiave"));
  static final RowMapper<Callback> CALLBACK =
      (r, n) ->
          new Callback(
              r.getString("id"),
              r.getString("owner_id"),
              r.getObject("data", LocalDate.class),
              r.getObject("data_appuntamento", LocalDate.class),
              r.getString("client_id"),
              r.getString("note"));
  static final RowMapper<Appointment> APPOINTMENT =
      (r, n) ->
          new Appointment(
              r.getString("id"),
              r.getString("owner_id"),
              r.getObject("data", LocalDate.class),
              r.getString("client_id"),
              new Appointment.ClientDetails(
                  ClientType.valueOf(r.getString("client_tipo")),
                  r.getString("client_nome"),
                  r.getString("client_cognome"),
                  r.getString("client_nome_legale")),
              r.getString("esito"),
              r.getString("polizza"),
              r.getBigDecimal("premio"),
              r.getString("note"));
  static final RowMapper<User> USER =
      (r, n) ->
          new User(
              r.getString("id"),
              r.getString("nome"),
              r.getString("cognome"),
              r.getString("email"),
              "",
              r.getLong("token_version"));
}
