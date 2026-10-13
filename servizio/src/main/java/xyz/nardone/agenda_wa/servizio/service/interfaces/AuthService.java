package xyz.nardone.agenda_wa.servizio.service.interfaces;

import java.time.Duration;
import org.springframework.security.oauth2.jwt.Jwt;
import xyz.nardone.agenda_wa.servizio.DTO.auth.LoginRequestDTO;
import xyz.nardone.agenda_wa.servizio.DTO.auth.RegisterRequestDTO;
import xyz.nardone.agenda_wa.servizio.entity.User;

public interface AuthService {
  Duration LIFETIME = Duration.ofHours(2);

  User register(RegisterRequestDTO request);

  User login(LoginRequestDTO request);

  User current();

  String token(User user);

  Jwt issue(User user);

  void logout();
}
