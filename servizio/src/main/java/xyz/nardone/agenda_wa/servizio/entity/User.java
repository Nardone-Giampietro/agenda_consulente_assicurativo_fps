package xyz.nardone.agenda_wa.servizio.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@TypeAlias("xyz.nardone.agenda_wa.servizio.domain.User")
@Document("utenti")
public record User(
    @Id String id,
    String nome,
    String cognome,
    @Indexed(unique = true) String email,
    @JsonIgnore String passwordHash,
    @JsonIgnore long tokenVersion) {}
