package xyz.nardone.agenda_wa.servizio.DTO.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record RegisterRequestDTO(
    @NotBlank(message = "Inserisci il nome.") @Size(max = 100) String nome,
    @NotBlank(message = "Inserisci il cognome.") @Size(max = 100) String cognome,
    @NotBlank(message = "Inserisci l’email.")
        @Email(message = "Inserisci un indirizzo email valido.")
        @Size(max = 254)
        String email,
    @NotBlank(message = "Inserisci la password.")
        @Size(min = 12, max = 128, message = "Usa una password da 12 a 128 caratteri.")
        String password) {
  public RegisterRequestDTO {
    email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
  }
}
