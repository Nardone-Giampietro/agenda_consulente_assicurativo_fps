package xyz.nardone.agenda_wa.servizio.service.interfaces;

import java.time.LocalDate;
import xyz.nardone.agenda_wa.servizio.DTO.overview.NavigationDTO;
import xyz.nardone.agenda_wa.servizio.DTO.overview.OverviewDTO;
import xyz.nardone.agenda_wa.servizio.DTO.overview.SummaryDTO;

public interface OverviewService {
  SummaryDTO summary(LocalDate dal, LocalDate al, String esito, String search);

  NavigationDTO navigation();

  OverviewDTO overview();
}
