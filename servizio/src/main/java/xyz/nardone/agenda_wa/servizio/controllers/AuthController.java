package xyz.nardone.agenda_wa.servizio.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.nardone.agenda_wa.servizio.DTO.auth.LoginRequestDTO;
import xyz.nardone.agenda_wa.servizio.DTO.auth.ProfileDTO;
import xyz.nardone.agenda_wa.servizio.DTO.auth.RegisterRequestDTO;
import xyz.nardone.agenda_wa.servizio.entity.User;
import xyz.nardone.agenda_wa.servizio.mapper.UserMapper;
import xyz.nardone.agenda_wa.servizio.security.LoginAttempts;
import xyz.nardone.agenda_wa.servizio.security.SecurityConfiguration;
import xyz.nardone.agenda_wa.servizio.service.interfaces.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService auth;
  private final UserMapper mapper;
  private final LoginAttempts attempts;
  private final boolean secureCookie;

  public AuthController(
      AuthService auth,
      LoginAttempts attempts,
      UserMapper mapper,
      @Value("${agenda.auth.secure-cookie}") boolean secureCookie) {
    this.auth = auth;
    this.mapper = mapper;
    this.attempts = attempts;
    this.secureCookie = secureCookie;
  }

  @GetMapping("/csrf")
  public CsrfToken csrf(CsrfToken token) {
    token.getToken();
    return token;
  }

  @PostMapping("/register")
  public ResponseEntity<ProfileDTO> register(
      @Valid @RequestBody RegisterRequestDTO request, HttpServletRequest http) {
    return session(auth.register(request), http, HttpStatus.CREATED);
  }

  @PostMapping("/login")
  public ResponseEntity<ProfileDTO> login(
      @Valid @RequestBody LoginRequestDTO request, HttpServletRequest http) {
    String key = http.getRemoteAddr() + "|" + request.email();
    attempts.check(key);
    User user;
    try {
      user = auth.login(request);
    } catch (org.springframework.web.server.ResponseStatusException e) {
      if (e.getStatusCode().value() == 401) attempts.failed(key);
      throw e;
    }
    attempts.succeeded(key);
    return session(user, http, HttpStatus.OK);
  }

  @GetMapping("/me")
  public ProfileDTO me(@AuthenticationPrincipal Jwt token) {
    return mapper.profile(auth.current(), token.getExpiresAt());
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest http) {
    auth.logout();
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO, http).toString())
        .build();
  }

  private ResponseEntity<ProfileDTO> session(
      User user, HttpServletRequest http, HttpStatus status) {
    Jwt token = auth.issue(user);
    return ResponseEntity.status(status)
        .header(
            HttpHeaders.SET_COOKIE,
            cookie(token.getTokenValue(), AuthService.LIFETIME, http).toString())
        .body(mapper.profile(user, token.getExpiresAt()));
  }

  private ResponseCookie cookie(String token, Duration age, HttpServletRequest http) {
    return ResponseCookie.from(SecurityConfiguration.COOKIE, token)
        .httpOnly(true)
        .secure(secureCookie || http.isSecure())
        .sameSite("Strict")
        .path("/api")
        .maxAge(age)
        .build();
  }
}
