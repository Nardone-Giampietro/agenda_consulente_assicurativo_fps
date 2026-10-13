package xyz.nardone.agenda_wa.servizio.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class FixedPassword {
  private final String hash;
  private final PasswordEncoder encoder;

  public FixedPassword(
      @Value("${agenda.auth.fixed-password-hash:}") String hash, PasswordEncoder encoder) {
    if (!hash.matches("[a-f0-9]{96}"))
      throw new IllegalStateException(
          "La build non contiene la configurazione privata della password fissa.");
    this.hash = hash;
    this.encoder = encoder;
  }

  public boolean matches(String password) {
    return encoder.matches(password, hash);
  }
}
