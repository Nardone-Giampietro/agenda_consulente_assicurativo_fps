package xyz.nardone.agenda_wa.servizio.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import xyz.nardone.agenda_wa.servizio.controllers.AuthController;

@RestControllerAdvice
public class ApiErrors {
  private static final Logger log = LoggerFactory.getLogger(ApiErrors.class);

  @ExceptionHandler(ResponseStatusException.class)
  ProblemDetail response(ResponseStatusException e) {
    return ProblemDetail.forStatusAndDetail(
        e.getStatusCode(), e.getReason() == null ? "Richiesta non valida." : e.getReason());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail invalid(MethodArgumentNotValidException e) {
    if (e.getParameter().getDeclaringClass().equals(AuthController.class))
      return ProblemDetail.forStatusAndDetail(
          HttpStatus.BAD_REQUEST,
          e.getBindingResult().getFieldErrors().stream()
              .map(error -> error.getDefaultMessage())
              .findFirst()
              .orElse("Controlla i dati inseriti."));
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        "Controlla i campi obbligatori, la lunghezza dei testi e il premio (massimo due"
            + " decimali).");
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class
  })
  ProblemDetail malformed(Exception e) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST, "Controlla il formato dei valori e delle date.");
  }

  @ExceptionHandler(DuplicateKeyException.class)
  ProblemDetail duplicate(Exception e) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT, "Esiste già un record con questi dati.");
  }

  @ExceptionHandler(DataAccessException.class)
  ProblemDetail database(Exception e) {
    log.error("Database operation failed: {}", e.getClass().getSimpleName());
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.SERVICE_UNAVAILABLE,
        "Il database non è disponibile oppure i dati sono stati modificati nel frattempo. Controlla"
            + " lo spazio disponibile sul computer e riprova.");
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail unexpected(Exception e) {
    if (e instanceof org.springframework.web.ErrorResponse response) {
      return ProblemDetail.forStatusAndDetail(
          response.getStatusCode(), "Risorsa o operazione non disponibile.");
    }
    log.error("Unexpected application error", e);
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Non è stato possibile completare l’operazione. Riprova.");
  }
}
