package xyz.nardone.agenda_wa.servizio.DTO.activity;

import java.time.LocalDate;

public record AppointmentSelectionDTO(
    AppointmentPeriod periodo, LocalDate dal, LocalDate al, LocalDate data, String esitoId) {}
