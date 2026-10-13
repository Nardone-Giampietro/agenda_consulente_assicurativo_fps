package xyz.nardone.agenda_wa.servizio.DTO.activity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CallbackRequestDTO(
    LocalDate data,
    @NotNull LocalDate dataAppuntamento,
    @NotBlank String clientId,
    @Size(max = 5000) String note) {}
