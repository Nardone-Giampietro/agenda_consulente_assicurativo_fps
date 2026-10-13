package xyz.nardone.agenda_wa.servizio.DTO.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record LoginRequestDTO(
    @NotBlank(message = "Inserisci l’email.")
        @Email(message = "Inserisci un indirizzo email valido.")
        @Size(max = 254)
        String email,
    @NotBlank(message = "Inserisci la password.") @Size(max = 128) String password) {
  public LoginRequestDTO {
    email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
  }
}
