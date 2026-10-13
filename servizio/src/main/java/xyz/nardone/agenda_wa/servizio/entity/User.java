package xyz.nardone.agenda_wa.servizio.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record User(
    String id,
    String nome,
    String cognome,
    String email,
    @JsonIgnore String passwordHash,
    @JsonIgnore long tokenVersion) {}
