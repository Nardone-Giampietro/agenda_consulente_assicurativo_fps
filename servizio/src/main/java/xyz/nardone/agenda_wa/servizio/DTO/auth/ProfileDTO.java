package xyz.nardone.agenda_wa.servizio.DTO.auth;

import java.time.Instant;

public record ProfileDTO(String id, String nome, String cognome, String email, Instant scadeAlle) {}
