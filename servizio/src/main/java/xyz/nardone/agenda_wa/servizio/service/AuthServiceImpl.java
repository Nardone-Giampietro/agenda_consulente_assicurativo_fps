package xyz.nardone.agenda_wa.servizio.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import xyz.nardone.agenda_wa.servizio.DTO.auth.RegisterRequestDTO;
import xyz.nardone.agenda_wa.servizio.entity.User;
import xyz.nardone.agenda_wa.servizio.mapper.UserMapper;
import xyz.nardone.agenda_wa.servizio.repository.MetadataRepository;
import xyz.nardone.agenda_wa.servizio.repository.UserRepository;
import xyz.nardone.agenda_wa.servizio.security.SecurityConfiguration;
import xyz.nardone.agenda_wa.servizio.security.UserScope;
import xyz.nardone.agenda_wa.servizio.service.interfaces.AuthService;
import xyz.nardone.agenda_wa.servizio.service.interfaces.CatalogService;

@Service
public class AuthServiceImpl implements AuthService {
  private final UserRepository users;
  private final MetadataRepository metadata;
  private final PasswordEncoder passwords;
  private final JwtEncoder tokens;
  private final CatalogService catalogs;
  private final UserMapper mapper;
  private final UserScope scope;
  private final Clock clock;
  private final String dummyHash;

  public AuthServiceImpl(
      UserRepository users,
      MetadataRepository metadata,
      PasswordEncoder passwords,
      JwtEncoder tokens,
      CatalogService catalogs,
      UserMapper mapper,
      UserScope scope,
      Clock clock) {
    this.users = users;
    this.metadata = metadata;
    this.passwords = passwords;
    this.tokens = tokens;
    this.catalogs = catalogs;
    this.mapper = mapper;
    this.scope = scope;
    this.clock = clock;
    this.dummyHash = passwords.encode(UUID.randomUUID().toString());
  }

  @Override
  @Transactional
  public User register(RegisterRequestDTO request) {
    if (users.existsByEmail(request.email()))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Questa email è già registrata. Accedi al tuo account.");
    Optional<String> reserved = metadata.reservedOwner(request.email());
    String id = reserved.orElseGet(() -> UUID.randomUUID().toString());
    User user = users.insert(mapper.toEntity(id, request, passwords.encode(request.password())));
    catalogs.initialize(id);
    if (reserved.isPresent()) metadata.removeReservation();
    return user;
  }

  @Override
  public User login(LoginRequestDTO request) {
    User user = users.findByEmail(request.email()).orElse(null);
    boolean valid =
        passwords.matches(request.password(), user == null ? dummyHash : user.passwordHash());
    if (user == null || !valid)
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email o password non corrette.");
    return user;
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
  public void logout() {
    users.revokeTokens(scope.id());
  }
}
