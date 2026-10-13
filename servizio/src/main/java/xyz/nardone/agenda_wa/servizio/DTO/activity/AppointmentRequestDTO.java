package xyz.nardone.agenda_wa.servizio.DTO.activity;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AppointmentRequestDTO(
    @NotNull LocalDate data,
    @NotBlank String clientId,
    @NotBlank String esitoId,
    String polizzaId,
    @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal premio,
    @Size(max = 5000) String note,
    boolean creaCallback,
    LocalDate dataCallback) {}
