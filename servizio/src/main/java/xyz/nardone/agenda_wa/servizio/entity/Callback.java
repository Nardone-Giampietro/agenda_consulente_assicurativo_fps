package xyz.nardone.agenda_wa.servizio.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;

public record Callback(
    String id,
    @JsonIgnore String ownerId,
    LocalDate data,
    LocalDate dataAppuntamento,
    String clientId,
    String note) {}
