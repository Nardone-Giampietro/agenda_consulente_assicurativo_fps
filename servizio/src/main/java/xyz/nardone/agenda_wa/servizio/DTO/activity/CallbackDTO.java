package xyz.nardone.agenda_wa.servizio.DTO.activity;

import java.time.LocalDate;

public record CallbackDTO(
    String id, LocalDate data, LocalDate dataAppuntamento, String clientId, String note) {}
