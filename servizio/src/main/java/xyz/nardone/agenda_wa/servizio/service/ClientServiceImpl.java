package xyz.nardone.agenda_wa.servizio.service;

import xyz.nardone.agenda_wa.servizio.DTO.client.ClientAppointmentDTO;
import static xyz.nardone.agenda_wa.servizio.exception.ApplicationErrors.missing;
import static xyz.nardone.agenda_wa.servizio.exception.ApplicationErrors.bad;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.nardone.agenda_wa.servizio.DTO.client.ClientRequestDTO;
import xyz.nardone.agenda_wa.servizio.DTO.client.ClientSummaryDTO;
import xyz.nardone.agenda_wa.servizio.DTO.common.PageDTO;
import xyz.nardone.agenda_wa.servizio.entity.Client;
import xyz.nardone.agenda_wa.servizio.entity.Appointment;
import xyz.nardone.agenda_wa.servizio.mapper.ClientMapper;
import xyz.nardone.agenda_wa.servizio.repository.AppointmentRepository;
import xyz.nardone.agenda_wa.servizio.repository.CallbackRepository;
import xyz.nardone.agenda_wa.servizio.repository.ClientRepository;
import xyz.nardone.agenda_wa.servizio.security.UserScope;
import xyz.nardone.agenda_wa.servizio.service.interfaces.ClientService;

@Service
public class ClientServiceImpl implements ClientService {
  private final ClientRepository clients;
  private final AppointmentRepository appointments;
  private final CallbackRepository callbacks;
  private final ClientMapper mapper;
  private final UserScope scope;

  public ClientServiceImpl(
      ClientRepository clients,
      AppointmentRepository appointments,
      CallbackRepository callbacks,
      ClientMapper mapper,
      UserScope scope) {
    this.clients = clients;
    this.appointments = appointments;
    this.callbacks = callbacks;
    this.mapper = mapper;
    this.scope = scope;
  }

  @Override
  public PageDTO<ClientSummaryDTO> clients(String search, int page, int size) {
    var result = clients.search(scope.id(), search, page, size);
    return new PageDTO<>(
        result.items().stream().map(mapper::toSummary).toList(), result.total(), page, size);
  }

  @Override
  public Client client(String id) {
    return clients.findByIdAndOwnerId(id, scope.id()).orElseThrow(() -> missing());
  }

  @Override
  @Transactional
  public Client saveClient(String id, ClientRequestDTO request) {
    if (request.tipo() == null || !request.isIdentityValid())
      throw bad("Inserisci le generalità richieste per il tipo di cliente.");
    if (id != null) {
      lockClient(id);
      if (client(id).tipo() != request.tipo())
        throw bad("Il tipo di cliente non può essere modificato.");
    }
    Client saved = mapper.toEntity(id == null ? UUID.randomUUID().toString() : id, scope.id(), request);
    if (id == null) clients.insert(saved);
    else clients.updateDetails(scope.id(), saved);
    if (id != null)
      appointments.updateClientDetails(scope.id(), id, Appointment.ClientDetails.from(saved));
    return saved;
  }

  @Override
  public PageDTO<ClientAppointmentDTO> history(
      String id, int page, int size) {
    var result = clients.history(scope.id(), id, page, size);
    return new PageDTO<>(result.items().stream().map(mapper::toAppointmentDTO).toList(),
        result.total(), page, size);
  }

  /** Parent writes serialize dependent activity inserts with deletion of their client. */
  @Override
  public void lockClient(String id) {
    if (clients.lock(scope.id(), id) == 0) throw missing();
  }

  @Override
  @Transactional
  public void deleteClient(String id) {
    lockClient(id);
    String owner = scope.id();
    appointments.deleteByOwnerIdAndClientId(owner, id);
    callbacks.deleteByOwnerIdAndClientId(owner, id);
    clients.deleteByIdAndOwnerId(id, owner);
  }

  @Override
  @Transactional
  public void clearClients() {
    String owner = scope.id();
    clients.findByOwnerId(owner).forEach(c -> lockClient(c.id()));
    appointments.deleteByOwnerId(owner);
    callbacks.deleteByOwnerId(owner);
    clients.deleteByOwnerId(owner);
  }
}
