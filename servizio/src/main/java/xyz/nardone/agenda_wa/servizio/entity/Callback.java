package xyz.nardone.agenda_wa.servizio.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@TypeAlias("xyz.nardone.agenda_wa.servizio.domain.Callback")
@Document("callback")
@CompoundIndexes({
  @CompoundIndex(name = "callback_data", def = "{'ownerId':1,'data':1}"),
  @CompoundIndex(name = "callback_cliente", def = "{'ownerId':1,'clientId':1}")
})
public record Callback(
    @Id String id,
    @JsonIgnore String ownerId,
    LocalDate data,
    LocalDate dataAppuntamento,
    String clientId,
    String note) {}
