package xyz.nardone.agenda_wa.servizio.DTO.activity;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AppointmentDTO(
    String id,
    LocalDate data,
    String clientId,
    String esitoId,
    String polizzaId,
    BigDecimal premio,
    String note) {}
