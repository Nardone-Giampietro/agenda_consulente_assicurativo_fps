package xyz.nardone.agenda_wa.servizio.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class ApplicationErrors {
  private ApplicationErrors() {}

  public static ResponseStatusException bad(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }

  public static ResponseStatusException missing() {
    return new ResponseStatusException(
        HttpStatus.NOT_FOUND, "Il record non esiste più. Aggiorna l’elenco.");
  }
}
