package xyz.nardone.agenda_wa.servizio.repository;

import java.time.LocalDate;
import java.util.List;
import xyz.nardone.agenda_wa.servizio.DTO.activity.AppointmentSelectionDTO;
import xyz.nardone.agenda_wa.servizio.entity.Appointment;

public interface AppointmentRepositoryCustom {
  RepositoryPage<Appointment> search(
      String ownerId, LocalDate dal, LocalDate al, String esito, String search, int page, int size);

  RepositoryPage<Appointment> selection(
      String ownerId, AppointmentSelectionDTO selection, String esito);

  long countSelection(String ownerId, AppointmentSelectionDTO selection, String esito);

  List<String> selectionClientIds(String ownerId, AppointmentSelectionDTO selection, String esito);

  long deleteSelection(String ownerId, AppointmentSelectionDTO selection, String esito);

  List<OutcomeTotal> totals(
      String ownerId, LocalDate dal, LocalDate al, String esito, String search);
}
