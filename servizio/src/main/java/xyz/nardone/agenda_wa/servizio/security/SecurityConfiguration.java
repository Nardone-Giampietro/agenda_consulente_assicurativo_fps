package xyz.nardone.agenda_wa.servizio.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.file.Path;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import xyz.nardone.agenda_wa.servizio.entity.User;
import xyz.nardone.agenda_wa.servizio.repository.UserRepository;

@Configuration
public class SecurityConfiguration {
  public static final String ISSUER = "agenda-wa";
  public static final String AUDIENCE = "agenda-wa-web";
  public static final String COOKIE = "agenda_session";

  @Bean
  SecretKey jwtKey(@Value("${agenda.auth.key-file}") String path) throws Exception {
    return JwtKeys.load(Path.of(path));
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new Pbkdf2PasswordEncoder(
        "", 16, 600000, Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256);
  }

  @Bean
  JwtEncoder jwtEncoder(SecretKey key) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(key));
  }

  @Bean
  JwtDecoder jwtDecoder(SecretKey key) {
    var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    OAuth2TokenValidator<Jwt> audience =
        token ->
            token.getAudience() != null
                    && token.getAudience().contains(AUDIENCE)
                    && token.getSubject() != null
                    && !token.getSubject().isBlank()
                    && token.getExpiresAt() != null
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(java.time.Duration.ZERO),
            new JwtIssuerValidator(ISSUER),
            audience));
    return decoder;
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http, UserRepository users) throws Exception {
    var csrf = new CookieCsrfTokenRepository();
    csrf.setCookieCustomizer(cookie -> cookie.httpOnly(true).sameSite("Strict").path("/api"));
    var headerResolver = new DefaultBearerTokenResolver();
    // Resource Server normally exempts bearer requests from CSRF. JWT cookies are
    // sent automatically by browsers, so use Spring's filter for every mutation.
    var csrfFilter = new CsrfFilter(csrf);
    csrfFilter.setRequestHandler(new CsrfTokenRequestAttributeHandler());
    csrfFilter.setAccessDeniedHandler(
        (request, response, e) ->
            problem(response, 403, "Richiesta non autorizzata. Ricarica la pagina e riprova."));
    http.sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .requestCache(cache -> cache.disable())
        .formLogin(login -> login.disable())
        .httpBasic(basic -> basic.disable())
        .logout(logout -> logout.disable())
        .csrf(config -> config.disable())
        .addFilterAt(csrfFilter, CsrfFilter.class)
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        HttpMethod.GET,
                        "/",
                        "/index.html",
                        "/styles.css",
                        "/favicon.svg",
                        "/fonts/**",
                        "/js/**",
                        "/api/auth/csrf")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/login")
                    .permitAll()
                    .requestMatchers("/error")
                    .permitAll()
                    .requestMatchers("/api/**")
                    .authenticated()
                    .anyRequest()
                    .denyAll())
        .exceptionHandling(
            errors ->
                errors
                    .authenticationEntryPoint(
                        (request, response, e) ->
                            problem(response, 401, "Accedi per usare l’agenda."))
                    .accessDeniedHandler(
                        (request, response, e) ->
                            problem(
                                response,
                                403,
                                "Richiesta non autorizzata. Ricarica la pagina e riprova.")))
        .oauth2ResourceServer(
            resource ->
                resource
                    .bearerTokenResolver(
                        request -> {
                          if (List.of("/api/auth/login", "/api/auth/csrf")
                              .contains(request.getServletPath())) return null;
                          String header = headerResolver.resolve(request);
                          if (header != null) return header;
                          if (request.getCookies() != null)
                            for (Cookie cookie : request.getCookies())
                              if (COOKIE.equals(cookie.getName())) return cookie.getValue();
                          return null;
                        })
                    .authenticationEntryPoint(
                        (request, response, e) ->
                            problem(response, 401, "La sessione è scaduta. Accedi di nuovo."))
                    .jwt(
                        jwt ->
                            jwt.jwtAuthenticationConverter(
                                token -> {
                                  User user = users.findById(token.getSubject()).orElse(null);
                                  Object claim = token.getClaims().get("version");
                                  if (!LocalIdentity.ID.equals(token.getSubject())
                                      || user == null
                                      || !(claim instanceof Number version)
                                      || user.tokenVersion() != version.longValue())
                                    throw new BadCredentialsException("Sessione non valida.");
                                  return new JwtAuthenticationToken(token, List.of(), user.id());
                                })))
        .headers(
            headers ->
                headers
                    .cacheControl(cache -> {})
                    .contentSecurityPolicy(
                        policy ->
                            policy.policyDirectives(
                                "default-src 'self'; script-src 'self'; style-src 'self'"
                                    + " 'unsafe-inline'; img-src 'self' data:; font-src 'self';"
                                    + " connect-src 'self'; frame-ancestors 'none'")));
    return http.build();
  }

  private static void problem(HttpServletResponse response, int status, String detail)
      throws java.io.IOException {
    response.setStatus(status);
    response.setContentType("application/problem+json;charset=UTF-8");
    response.getWriter().write("{\"status\":" + status + ",\"detail\":\"" + detail + "\"}");
  }
}
