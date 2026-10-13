package xyz.nardone.agenda_wa.servizio.mapper;

import static xyz.nardone.agenda_wa.servizio.util.CommonUtil.id;

import java.time.LocalDate;
import java.util.Map;
import org.springframework.stereotype.Component;
import xyz.nardone.agenda_wa.servizio.DTO.activity.ActivityDTO;
import xyz.nardone.agenda_wa.servizio.DTO.activity.AppointmentDTO;
import xyz.nardone.agenda_wa.servizio.DTO.activity.AppointmentRequestDTO;
import xyz.nardone.agenda_wa.servizio.DTO.activity.CallbackDTO;
import xyz.nardone.agenda_wa.servizio.DTO.activity.CallbackRequestDTO;
import xyz.nardone.agenda_wa.servizio.entity.Appointment;
import xyz.nardone.agenda_wa.servizio.entity.Callback;
import xyz.nardone.agenda_wa.servizio.entity.CatalogItem;
import xyz.nardone.agenda_wa.servizio.entity.Client;

@Component
public class ActivityMapper {
  public Appointment toEntity(
      String id,
      String ownerId,
      AppointmentRequestDTO request,
      Client client,
      CatalogItem outcome,
      CatalogItem policy) {
    return new Appointment(
        id,
        ownerId,
        request.data(),
        request.clientId(),
        Appointment.ClientDetails.from(client),
        outcome.nome(),
        policy == null ? null : policy.nome(),
        request.premio(),
        note(request.note()));
  }

  public Callback toEntity(String id, String ownerId, CallbackRequestDTO request) {
    return new Callback(
        id,
        ownerId,
        request.data(),
        request.dataAppuntamento(),
        request.clientId(),
        note(request.note()));
  }

  public Callback toCallback(String id, Appointment appointment, LocalDate date) {
    return new Callback(
        id,
        appointment.ownerId(),
        date,
        appointment.data(),
        appointment.clientId(),
        appointment.note());
  }

  public AppointmentDTO toDTO(Appointment a, AppointmentRequestDTO request) {
    return new AppointmentDTO(
        a.id(), a.data(), a.clientId(), request.esitoId(), id(request.polizzaId()), a.premio(), a.note());
  }

  public CallbackDTO toDTO(Callback c) {
    return new CallbackDTO(c.id(), c.data(), c.dataAppuntamento(), c.clientId(), c.note());
  }

  public ActivityDTO detail(Appointment a, References refs) {
    return new ActivityDTO(
        a.id(),
        a.data(),
        null,
        a.clientId(),
        a.cliente().displayName(),
        a.cliente().tipo(),
        refs.catalogId("esiti", a.esito()),
        a.esito(),
        refs.catalogId("polizze", a.polizza()),
        a.polizza(),
        a.premio(),
        a.note());
  }

  public ActivityDTO detail(Callback c, References refs) {
    return new ActivityDTO(
        c.id(),
        c.data(),
        c.dataAppuntamento(),
        c.clientId(),
        refs.client(c.clientId()),
        refs.clientType(c.clientId()),
        null,
        null,
        null,
        null,
        null,
        c.note());
  }

  private String note(String value) {
    return value == null ? "" : value.strip();
  }

  /** References are loaded once per page by the service; mapping never queries the database. */
  public record References(Map<String, Client> clients, Map<String, CatalogItem> labels) {
    private String client(String id) {
      Client c = clients.get(id);
      return c == null ? "Cliente rimosso" : c.displayName();
    }

    private xyz.nardone.agenda_wa.servizio.entity.ClientType clientType(String id) {
      Client c = clients.get(id);
      return c == null ? null : c.tipo();
    }

    private String catalogId(String type, String name) {
      CatalogItem item = labels.get(type + ":" + name);
      return item == null ? null : item.id();
    }
  }
}
