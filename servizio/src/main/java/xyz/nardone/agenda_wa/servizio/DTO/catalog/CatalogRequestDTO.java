package xyz.nardone.agenda_wa.servizio.DTO.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CatalogRequestDTO(@NotBlank @Size(max = 150) String nome) {}
