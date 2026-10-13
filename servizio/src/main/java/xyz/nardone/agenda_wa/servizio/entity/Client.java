package xyz.nardone.agenda_wa.servizio.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import xyz.nardone.agenda_wa.servizio.util.ClientNames;

@TypeAlias("xyz.nardone.agenda_wa.servizio.domain.Client")
@Document("clienti")
@CompoundIndexes({
  @CompoundIndex(name = "cliente_cognome", def = "{'ownerId':1,'cognome':1,'_id':1}"),
  @CompoundIndex(name = "cliente_nome_legale", def = "{'ownerId':1,'nomeLegale':1,'_id':1}")
})
public record Client(
    @Id String id, @JsonIgnore String ownerId, ClientType tipo, String nome,
    String cognome, String nomeLegale, String telefono, List<AppointmentSummary> appuntamenti) {
  public record AppointmentSummary(@Field("id") String id, LocalDate data, String esito) {}
  public Client {
    telefono = telefono == null ? "" : telefono.strip();
    appuntamenti = appuntamenti == null ? List.of() : List.copyOf(appuntamenti);
    // Compatibility with documents persisted before client types were introduced.
    if (tipo == null) tipo = ClientType.PERSONA_FISICA;
    if (tipo == ClientType.AZIENDA) {
      nomeLegale = ClientNames.company(nomeLegale);
      nome = null;
      cognome = null;
    } else {
      nome = ClientNames.person(nome);
      cognome = ClientNames.person(cognome);
      nomeLegale = null;
    }
  }

  public Client(String id, String ownerId, ClientType tipo, String nome,
      String cognome, String nomeLegale) {
    this(id, ownerId, tipo, nome, cognome, nomeLegale, "", List.of());
  }

  public Client(String id, String ownerId, String nome, String cognome) {
    this(id, ownerId, ClientType.PERSONA_FISICA, nome, cognome, null);
  }

  public String displayName() {
    return tipo == ClientType.AZIENDA ? nomeLegale : cognome + " " + nome;
  }
}
