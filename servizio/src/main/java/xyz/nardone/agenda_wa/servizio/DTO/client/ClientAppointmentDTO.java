package xyz.nardone.agenda_wa.servizio.DTO.client;

import java.time.LocalDate;

public record ClientAppointmentDTO(String id, LocalDate data, String esito) {}
