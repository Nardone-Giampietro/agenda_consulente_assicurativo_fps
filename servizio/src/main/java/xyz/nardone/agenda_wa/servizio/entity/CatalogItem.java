package xyz.nardone.agenda_wa.servizio.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@TypeAlias("xyz.nardone.agenda_wa.servizio.domain.CatalogItem")
@Document("cataloghi")
@CompoundIndex(
    name = "catalogo_utente_unico",
    def = "{'ownerId':1,'tipo':1,'chiave':1}",
    unique = true)
public record CatalogItem(
    @Id String id, @JsonIgnore String ownerId, String tipo, String nome, String chiave) {}
