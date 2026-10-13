package xyz.nardone.agenda_wa.servizio.DTO.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDTO(
    @NotBlank(message = "Inserisci la password.") @Size(max = 128) String password) {}
