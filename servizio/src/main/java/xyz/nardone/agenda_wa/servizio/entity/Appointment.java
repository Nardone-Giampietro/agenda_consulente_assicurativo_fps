package xyz.nardone.agenda_wa.servizio.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@TypeAlias("xyz.nardone.agenda_wa.servizio.domain.Appointment")
@Document("appuntamenti")
@CompoundIndexes({
  @CompoundIndex(name = "appuntamento_data", def = "{'ownerId':1,'data':-1}"),
  @CompoundIndex(name = "appuntamento_cliente", def = "{'ownerId':1,'clientId':1}"),
  @CompoundIndex(name = "appuntamento_esito_nome_data", def = "{'ownerId':1,'esito':1,'data':-1}"),
  @CompoundIndex(name = "appuntamento_polizza_nome", def = "{'ownerId':1,'polizza':1}")
})
public record Appointment(
    @Id String id,
    @JsonIgnore String ownerId,
    LocalDate data,
    String clientId,
    ClientDetails cliente,
    String esito,
    String polizza,
    @Field(targetType = FieldType.DECIMAL128) BigDecimal premio,
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
