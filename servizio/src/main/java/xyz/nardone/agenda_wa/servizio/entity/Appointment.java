package xyz.nardone.agenda_wa.servizio.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.LocalDate;

public record Appointment(
    String id,
    @JsonIgnore String ownerId,
    LocalDate data,
    String clientId,
    ClientDetails cliente,
    String esito,
    String polizza,
    BigDecimal premio,
    String note) {
  public record ClientDetails(ClientType tipo, String nome, String cognome, String nomeLegale) {
    public static ClientDetails from(Client client) {
      return new ClientDetails(client.tipo(), client.nome(), client.cognome(), client.nomeLegale());
    }

    public String displayName() {
      return tipo == ClientType.AZIENDA ? nomeLegale : cognome + " " + nome;
    }
  }
}
