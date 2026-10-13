package xyz.nardone.agenda_wa.servizio.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record CatalogItem(
    String id, @JsonIgnore String ownerId, String tipo, String nome, String chiave) {}
