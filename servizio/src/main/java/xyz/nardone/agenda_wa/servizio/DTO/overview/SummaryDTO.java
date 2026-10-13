package xyz.nardone.agenda_wa.servizio.DTO.overview;

import java.math.BigDecimal;
import java.util.Map;

public record SummaryDTO(
    long appuntamenti, long sottoscrizioni, BigDecimal premi, Map<String, Long> esiti) {}
