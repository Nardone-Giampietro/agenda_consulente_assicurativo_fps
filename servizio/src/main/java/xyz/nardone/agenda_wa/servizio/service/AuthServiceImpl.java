package xyz.nardone.agenda_wa.servizio.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import xyz.nardone.agenda_wa.servizio.DTO.auth.LoginRequestDTO;
import xyz.nardone.agenda_wa.servizio.entity.User;
import xyz.nardone.agenda_wa.servizio.repository.UserRepository;
import xyz.nardone.agenda_wa.servizio.security.SecurityConfiguration;
import xyz.nardone.agenda_wa.servizio.security.UserScope;
import xyz.nardone.agenda_wa.servizio.service.interfaces.AuthService;

@Service
public class AuthServiceImpl implements AuthService {
  private final UserRepository users;
  private final xyz.nardone.agenda_wa.servizio.security.FixedPassword passwords;
  private final JwtEncoder tokens;
  private final UserScope scope;
  private final Clock clock;
  private final xyz.nardone.agenda_wa.servizio.repository.SqlOperationLock operationLock;

  public AuthServiceImpl(
      UserRepository users,
      xyz.nardone.agenda_wa.servizio.security.FixedPassword passwords,
      JwtEncoder tokens,
      UserScope scope,
      xyz.nardone.agenda_wa.servizio.repository.SqlOperationLock operationLock,
      Clock clock) {
    this.users = users;
    this.passwords = passwords;
    this.tokens = tokens;
    this.scope = scope;
    this.clock = clock;
    this.operationLock = operationLock;
  }

  @Override
  public User login(LoginRequestDTO request) {
    if (!passwords.matches(request.password()))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Password non corretta.");
    return users
        .findById(xyz.nardone.agenda_wa.servizio.security.LocalIdentity.ID)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "L’agenda non è ancora pronta."));
  }

  @Override
  public User current() {
    return users
        .findById(scope.id())
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Accedi di nuovo."));
  }

  @Override
  public String token(User user) {
    return issue(user).getTokenValue();
  }

  @Override
  public Jwt issue(User user) {
    Instant now = clock.instant().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
    var claims =
        JwtClaimsSet.builder()
            .issuer(SecurityConfiguration.ISSUER)
            .audience(List.of(SecurityConfiguration.AUDIENCE))
            .subject(user.id())
            .issuedAt(now)
            .expiresAt(now.plus(LIFETIME))
            .id(UUID.randomUUID().toString())
            .claim("version", user.tokenVersion())
            .build();
    return tokens.encode(
        JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims));
  }

  @Override
  @Transactional
  public void logout() {
    operationLock.acquire();
    users.revokeTokens(scope.id());
  }
}
