package xyz.nardone.agenda_wa.servizio.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class UserScope {
  public String id() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!(authentication instanceof JwtAuthenticationToken jwt) || !jwt.isAuthenticated())
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Accedi per usare l’agenda.");
    return jwt.getToken().getSubject();
  }
}
