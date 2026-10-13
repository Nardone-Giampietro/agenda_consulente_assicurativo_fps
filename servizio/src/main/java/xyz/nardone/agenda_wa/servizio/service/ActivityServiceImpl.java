package xyz.nardone.agenda_wa.servizio.service;

import static xyz.nardone.agenda_wa.servizio.exception.ApplicationErrors.bad;
import static xyz.nardone.agenda_wa.servizio.exception.ApplicationErrors.missing;
import static xyz.nardone.agenda_wa.servizio.util.CommonUtil.id;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import xyz.nardone.agenda_wa.servizio.DTO.activity.ActivityDTO;
import xyz.nardone.agenda_wa.servizio.DTO.activity.AppointmentRequestDTO;
import xyz.nardone.agenda_wa.servizio.DTO.activity.AppointmentSelectionDTO;
import xyz.nardone.agenda_wa.servizio.DTO.activity.CallbackRequestDTO;
import xyz.nardone.agenda_wa.servizio.DTO.common.PageDTO;
import xyz.nardone.agenda_wa.servizio.entity.Appointment;
import xyz.nardone.agenda_wa.servizio.entity.Callback;
import xyz.nardone.agenda_wa.servizio.entity.CatalogItem;
import xyz.nardone.agenda_wa.servizio.entity.Client;
import xyz.nardone.agenda_wa.servizio.mapper.ActivityMapper;
import xyz.nardone.agenda_wa.servizio.repository.AppointmentRepository;
import xyz.nardone.agenda_wa.servizio.repository.CallbackRepository;
import xyz.nardone.agenda_wa.servizio.repository.CatalogItemRepository;
import xyz.nardone.agenda_wa.servizio.repository.ClientRepository;
import xyz.nardone.agenda_wa.servizio.security.UserScope;
import xyz.nardone.agenda_wa.servizio.service.interfaces.ActivityService;
import xyz.nardone.agenda_wa.servizio.service.interfaces.CatalogService;
import xyz.nardone.agenda_wa.servizio.service.interfaces.ClientService;

@Service
public class ActivityServiceImpl implements ActivityService {
  private final AppointmentRepository appointments;
  private final CallbackRepository callbacks;
  private final ClientRepository clients;
  private final CatalogItemRepository labels;
  private final ClientService clientService;
  private final CatalogService catalogs;
  private final ActivityMapper mapper;
  private final UserScope scope;
  private final Clock clock;

  public ActivityServiceImpl(
      AppointmentRepository appointments,
      CallbackRepository callbacks,
      ClientRepository clients,
      CatalogItemRepository labels,
      ClientService clientService,
      CatalogService catalogs,
      ActivityMapper mapper,
      UserScope scope,
      Clock clock) {
    this.appointments = appointments;
    this.callbacks = callbacks;
    this.clients = clients;
    this.labels = labels;
    this.clientService = clientService;
    this.catalogs = catalogs;
    this.mapper = mapper;
    this.scope = scope;
    this.clock = clock;
  }

  @Override
  public PageDTO<ActivityDTO> appointments(
      LocalDate dal, LocalDate al, String esito, String search, int page, int size) {
    var result = appointments.search(scope.id(), dal, al, outcomeName(esito), search, page, size);
    return new PageDTO<>(appointmentViews(result.items()), result.total(), page, size);
  }

  @Override
  public PageDTO<ActivityDTO> callbacks(LocalDate dal, LocalDate al, int page, int size) {
    var result = callbacks.search(scope.id(), dal, al, LocalDate.now(clock), page, size);
    return new PageDTO<>(callbackViews(result.items()), result.total(), page, size);
  }

  private Appointment requireAppointment(String id) {
    return appointments.findByIdAndOwnerId(id, scope.id()).orElseThrow(() -> missing());
  }

  private Callback requireCallback(String id) {
    return callbacks.findByIdAndOwnerId(id, scope.id()).orElseThrow(() -> missing());
  }

  @Override
  public ActivityDTO appointment(String id) {
    return appointmentViews(List.of(requireAppointment(id))).getFirst();
  }

  @Override
  public ActivityDTO callback(String id) {
    return callbackViews(List.of(requireCallback(id))).getFirst();
  }

  @Override
  @Transactional
  public Appointment saveAppointment(String id, AppointmentRequestDTO request) {
    Appointment previous = id == null ? null : requireAppointment(id);
    if (previous != null && !previous.clientId().equals(request.clientId()))
      clientService.lockClient(previous.clientId());
    clientService.lockClient(request.clientId());
    Client client = clientService.client(request.clientId());
    catalogs.lock("esiti", request.esitoId());
    CatalogItem outcome = catalogs.item("esiti", request.esitoId());
    String policy = id(request.polizzaId());
    if (policy != null) catalogs.lock("polizze", policy);
    if (!request.creaCallback() && request.dataCallback() != null)
      throw bad("Attiva il callback per impostarne la data.");
    if (id != null && request.creaCallback())
      throw bad("Per un appuntamento esistente, aggiungi il callback dalla sezione Callback.");
    validateCallbackDate(request.dataCallback(), request.data());
    Appointment saved =
        appointments.save(
            mapper.toEntity(
                id == null ? UUID.randomUUID().toString() : id,
                scope.id(),
                request,
                client,
                outcome,
                policy == null ? null : catalogs.item("polizze", policy)));
    if (request.creaCallback())
      callbacks.save(
          mapper.toCallback(UUID.randomUUID().toString(), saved, request.dataCallback()));
    clients.rebuildAppointmentHistory(scope.id(), saved.clientId());
    if (previous != null && !previous.clientId().equals(saved.clientId()))
      clients.rebuildAppointmentHistory(scope.id(), previous.clientId());
    return saved;
  }

  @Override
  @Transactional
  public Callback saveCallback(String id, CallbackRequestDTO request) {
    if (id != null) requireCallback(id);
    clientService.lockClient(request.clientId());
    validateCallbackDate(request.data(), request.dataAppuntamento());
    return callbacks.save(
        mapper.toEntity(id == null ? UUID.randomUUID().toString() : id, scope.id(), request));
  }

  private void validateCallbackDate(LocalDate callback, LocalDate appointment) {
    if (callback != null && callback.isBefore(appointment))
      throw bad(
          "La data del callback deve essere successiva o uguale alla data dell’appuntamento.");
  }

  @Override
  @Transactional
  public void deleteAppointment(String id) {
    Appointment previous = requireAppointment(id);
    clientService.lockClient(previous.clientId());
    appointments.deleteByIdAndOwnerId(id, scope.id());
    clients.rebuildAppointmentHistory(scope.id(), previous.clientId());
  }

  @Override
  @Transactional
  public void deleteCallback(String id) {
    requireCallback(id);
    callbacks.deleteByIdAndOwnerId(id, scope.id());
  }

  private String outcomeName(String outcomeId) {
    return id(outcomeId) == null ? null : catalogs.item("esiti", outcomeId).nome();
  }

  @Override
  public PageDTO<ActivityDTO> selectedAppointments(AppointmentSelectionDTO selection) {
    String esito = selection == null ? null : outcomeName(selection.esitoId());
    var result = appointments.selection(scope.id(), selection, esito);
    return new PageDTO<>(appointmentViews(result.items()), result.total(), 0, 5);
  }

  @Override
  @Transactional
  public long deleteSelectedAppointments(AppointmentSelectionDTO selection, long expected) {
    if (expected < 0) throw bad("Il numero di appuntamenti attesi non è valido.");
    String esito = selection == null ? null : outcomeName(selection.esitoId());
    String owner = scope.id();
    var affected = appointments.selectionClientIds(owner, selection, esito).stream().sorted().toList();
    affected.forEach(clientService::lockClient);
    if (appointments.countSelection(owner, selection, esito) != expected)
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Gli appuntamenti sono cambiati. Verifica di nuovo la selezione prima di eliminare.");
    long deleted = appointments.deleteSelection(owner, selection, esito);
    affected.forEach(clientId -> clients.rebuildAppointmentHistory(owner, clientId));
    return deleted;
  }

  @Override
  public List<ActivityDTO> appointmentViews(List<Appointment> items) {
    var refs = references(List.of(), !items.isEmpty());
    return items.stream().map(a -> mapper.detail(a, refs)).toList();
  }

  @Override
  public List<ActivityDTO> callbackViews(List<Callback> items) {
    var refs = references(items.stream().map(Callback::clientId).toList(), false);
    return items.stream().map(c -> mapper.detail(c, refs)).toList();
  }

  private ActivityMapper.References references(List<String> clientIds, boolean includeLabels) {
    String owner = scope.id();
    Map<String, Client> clientMap = new HashMap<>();
    Map<String, CatalogItem> labelMap = new HashMap<>();
    if (!clientIds.isEmpty())
      clients
          .findByOwnerIdAndIdIn(owner, new HashSet<>(clientIds))
          .forEach(c -> clientMap.put(c.id(), c));
    if (includeLabels)
      labels.findByOwnerId(owner).forEach(c -> labelMap.put(c.tipo() + ":" + c.nome(), c));
    return new ActivityMapper.References(clientMap, labelMap);
  }
}
