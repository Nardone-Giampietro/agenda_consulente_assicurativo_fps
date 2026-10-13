package xyz.nardone.agenda_wa.servizio.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class LoginAttempts {
  private record Attempt(int failures, Instant until) {}

  private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
  private final Clock clock;

  public LoginAttempts(Clock clock) {
    this.clock = clock;
  }

  public void check(String key) {
    Instant now = clock.instant();
    attempts.entrySet().removeIf(entry -> entry.getValue().until().isBefore(now));
    var attempt = attempts.get(key);
    if (attempt != null && attempt.failures() >= 10)
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS, "Troppi tentativi. Riprova tra 15 minuti.");
    if (attempts.size() >= 10000 && attempt == null)
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS, "Troppi tentativi. Riprova più tardi.");
  }

  public void failed(String key) {
    attempts.compute(
        key,
        (k, previous) ->
            previous == null
                ? new Attempt(1, clock.instant().plus(Duration.ofMinutes(15)))
                : new Attempt(previous.failures() + 1, previous.until()));
  }

  public void succeeded(String key) {
    attempts.remove(key);
  }
}
