package xyz.nardone.agenda_wa.servizio.DTO.client;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import xyz.nardone.agenda_wa.servizio.entity.ClientType;

public record ClientRequestDTO(
    @NotNull ClientType tipo, @Size(max = 100) String nome,
    @Size(max = 100) String cognome, @Size(max = 150) String nomeLegale,
    @Size(max = 40) String telefono) {
  public ClientRequestDTO(ClientType tipo, String nome, String cognome, String nomeLegale) {
    this(tipo, nome, cognome, nomeLegale, "");
  }
  public ClientRequestDTO(String nome, String cognome) {
    this(ClientType.PERSONA_FISICA, nome, cognome, null);
  }

  @JsonIgnore
  @AssertTrue(message = "Per una persona fisica inserisci nome e cognome; per un’azienda soltanto il nome legale.")
  public boolean isIdentityValid() {
    return tipo == null || (tipo == ClientType.AZIENDA
        ? present(nomeLegale) && !present(nome) && !present(cognome)
        : present(nome) && present(cognome) && !present(nomeLegale));
  }

  private static boolean present(String value) {
    return value != null && !value.isBlank();
  }
}
