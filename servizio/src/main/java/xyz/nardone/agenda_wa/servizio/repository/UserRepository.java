package xyz.nardone.agenda_wa.servizio.repository;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import xyz.nardone.agenda_wa.servizio.entity.User;

@Repository
public class UserRepository {
  private final JdbcTemplate jdbc;

  public UserRepository(JdbcTemplate jdbc, SqlSchemaRepository schema) {
    this.jdbc = jdbc;
  }

  public Optional<User> findById(String id) {
    return jdbc.query("SELECT * FROM utenti WHERE id=?", PersistenceRows.USER, id).stream()
        .findFirst();
  }

  public User insert(User u) {
    jdbc.update(
        "INSERT INTO utenti(id,nome,cognome,email,token_version) VALUES(?,?,?,?,?)",
        u.id(),
        u.nome(),
        u.cognome(),
        u.email(),
        u.tokenVersion());
    return u;
  }

  public long revokeTokens(String id) {
    return jdbc.update("UPDATE utenti SET token_version=token_version+1 WHERE id=?", id);
  }
}
