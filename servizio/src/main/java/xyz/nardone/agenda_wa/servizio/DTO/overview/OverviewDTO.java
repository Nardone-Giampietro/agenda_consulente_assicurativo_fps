package xyz.nardone.agenda_wa.servizio.DTO.overview;

import java.time.LocalDate;
import java.util.List;
import xyz.nardone.agenda_wa.servizio.DTO.activity.ActivityDTO;

public record OverviewDTO(
    LocalDate oggi,
    long clienti,
    long callbackOggi,
    SummaryDTO riepilogo,
    List<ActivityDTO> prossimiAppuntamenti,
    List<ActivityDTO> callback) {}
