package xyz.nardone.agenda_wa.servizio.DTO.activity;

import java.math.BigDecimal;
import java.time.LocalDate;
import xyz.nardone.agenda_wa.servizio.entity.ClientType;

public record ActivityDTO(
    String id,
    LocalDate data,
    LocalDate dataAppuntamento,
    String clientId,
    String client,
    ClientType tipoCliente,
    String esitoId,
    String esito,
    String polizzaId,
    String polizza,
    BigDecimal premio,
    String note) {}
