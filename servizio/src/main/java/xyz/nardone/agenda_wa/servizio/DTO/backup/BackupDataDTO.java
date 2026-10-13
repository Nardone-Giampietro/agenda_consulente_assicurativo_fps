package xyz.nardone.agenda_wa.servizio.DTO.backup;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import xyz.nardone.agenda_wa.servizio.entity.ClientType;

public record BackupDataDTO(
    @NotNull List<@Valid ClientData> clienti,
    @NotNull List<@Valid CatalogData> cataloghi,
    @NotNull List<@Valid AppointmentData> appuntamenti,
    @NotNull List<@Valid CallbackData> callback) {
  public record ClientData(
      @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,80}") String id,
      @NotNull ClientType tipo,
      @Size(max = 100) String nome,
      @Size(max = 100) String cognome,
      @Size(max = 150) String nomeLegale,
      @NotNull @Size(max = 40) String telefono) {}

  public record CatalogData(
      @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,80}") String id,
      @NotBlank @Pattern(regexp = "esiti|polizze") String tipo,
      @NotBlank @Size(max = 150) String nome) {}

  public record AppointmentData(
      @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,80}") String id,
      @NotNull LocalDate data,
      @NotBlank String clientId,
      @NotBlank @Size(max = 150) String esito,
      @Size(max = 150) String polizza,
      @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal premio,
      @NotNull @Size(max = 5000) String note) {}

  public record CallbackData(
      @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,80}") String id,
      LocalDate data,
      @NotNull LocalDate dataAppuntamento,
      @NotBlank String clientId,
      @NotNull @Size(max = 5000) String note) {}
}
