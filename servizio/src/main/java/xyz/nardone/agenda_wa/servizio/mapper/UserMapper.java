package xyz.nardone.agenda_wa.servizio.mapper;

import java.time.Instant;
import org.springframework.stereotype.Component;
import xyz.nardone.agenda_wa.servizio.DTO.auth.ProfileDTO;
import xyz.nardone.agenda_wa.servizio.DTO.auth.RegisterRequestDTO;
import xyz.nardone.agenda_wa.servizio.entity.User;

@Component
public class UserMapper {
  public User toEntity(String id, RegisterRequestDTO request, String passwordHash) {
    return new User(
        id, request.nome().strip(), request.cognome().strip(), request.email(), passwordHash, 0);
  }

  public ProfileDTO profile(User user, Instant expiry) {
    return new ProfileDTO(user.id(), user.nome(), user.cognome(), user.email(), expiry);
  }
}
