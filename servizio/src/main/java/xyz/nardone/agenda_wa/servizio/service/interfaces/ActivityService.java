package xyz.nardone.agenda_wa.servizio.service.interfaces;

import java.time.LocalDate;
import java.util.List;
import xyz.nardone.agenda_wa.servizio.DTO.activity.ActivityDTO;
import xyz.nardone.agenda_wa.servizio.DTO.activity.AppointmentRequestDTO;
import xyz.nardone.agenda_wa.servizio.DTO.activity.AppointmentSelectionDTO;
import xyz.nardone.agenda_wa.servizio.DTO.activity.CallbackRequestDTO;
import xyz.nardone.agenda_wa.servizio.DTO.common.PageDTO;
import xyz.nardone.agenda_wa.servizio.entity.Appointment;
import xyz.nardone.agenda_wa.servizio.entity.Callback;

public interface ActivityService {
  PageDTO<ActivityDTO> appointments(
      LocalDate dal, LocalDate al, String esito, String search, int page, int size);

  PageDTO<ActivityDTO> callbacks(LocalDate dal, LocalDate al, int page, int size);

  ActivityDTO appointment(String id);

  ActivityDTO callback(String id);

  Appointment saveAppointment(String id, AppointmentRequestDTO request);

  Callback saveCallback(String id, CallbackRequestDTO request);

  void deleteAppointment(String id);

  void deleteCallback(String id);

  PageDTO<ActivityDTO> selectedAppointments(AppointmentSelectionDTO selection);

  long deleteSelectedAppointments(AppointmentSelectionDTO selection, long expected);

  List<ActivityDTO> appointmentViews(List<Appointment> items);

  List<ActivityDTO> callbackViews(List<Callback> items);
}
