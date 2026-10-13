package xyz.nardone.agenda_wa.servizio.service;

import xyz.nardone.agenda_wa.servizio.entity.Appointment;
import xyz.nardone.agenda_wa.servizio.repository.ClientRepository;
import static xyz.nardone.agenda_wa.servizio.exception.ApplicationErrors.bad;
import static xyz.nardone.agenda_wa.servizio.exception.ApplicationErrors.missing;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import xyz.nardone.agenda_wa.servizio.DTO.catalog.CatalogRequestDTO;
import xyz.nardone.agenda_wa.servizio.config.DefaultCatalogs;
import xyz.nardone.agenda_wa.servizio.entity.CatalogItem;
import xyz.nardone.agenda_wa.servizio.mapper.CatalogMapper;
import xyz.nardone.agenda_wa.servizio.repository.AppointmentRepository;
import xyz.nardone.agenda_wa.servizio.repository.CatalogItemRepository;
import xyz.nardone.agenda_wa.servizio.repository.MetadataRepository;
import xyz.nardone.agenda_wa.servizio.security.UserScope;
import xyz.nardone.agenda_wa.servizio.service.interfaces.CatalogService;

@Service
public class CatalogServiceImpl implements CatalogService {
  private final CatalogItemRepository catalogs;
  private final AppointmentRepository appointments;
  private final MetadataRepository metadata;
  private final ClientRepository clients;
  private final CatalogMapper mapper;
  private final UserScope scope;

  public CatalogServiceImpl(
      CatalogItemRepository catalogs,
      AppointmentRepository appointments,
      MetadataRepository metadata,
      ClientRepository clients,
      CatalogMapper mapper,
      UserScope scope) {
    this.catalogs = catalogs;
    this.appointments = appointments;
    this.metadata = metadata;
    this.clients = clients;
    this.mapper = mapper;
    this.scope = scope;
  }

  private String type(String type) {
    if (!Set.of("esiti", "polizze").contains(type)) throw missing();
    return type;
  }

  @Override
  public List<CatalogItem> list(String type) {
    return catalogs.findByOwnerIdAndTipoOrderByNomeAsc(scope.id(), type(type));
  }

  @Override
  public CatalogItem item(String type, String id) {
    return catalogs
        .findByIdAndOwnerIdAndTipo(id, scope.id(), type(type))
        .orElseThrow(
            () ->
                bad(
                    "Seleziona un "
                        + (type.equals("esiti") ? "esito" : "tipo di polizza")
                        + " valido."));
  }

  @Override
  public void lock(String type, String id) {
    item(type, id);
    catalogs.lock(scope.id(), id);
  }

  @Override
  @Transactional
  public CatalogItem save(String type, String id, CatalogRequestDTO request) {
    type(type);
    CatalogItem previous = null;
    if (id != null) {
      lock(type, id);
      previous = item(type, id);
    }
    CatalogItem saved = catalogs.save(
        mapper.toEntity(
            id == null ? UUID.randomUUID().toString() : id, scope.id(), type, request.nome()));
    if (previous != null && !previous.nome().equals(saved.nome())) {
      if (type.equals("esiti")) {
        String owner = scope.id();
        var affected = appointments.clientsWithOutcome(owner, previous.nome()).stream()
            .map(Appointment::clientId).distinct().sorted().toList();
        affected.forEach(clientId -> {
          if (clients.lock(owner, clientId) == 0) throw missing();
        });
        appointments.renameOutcome(owner, previous.nome(), saved.nome());
        affected.forEach(clientId -> clients.rebuildAppointmentHistory(owner, clientId));
      }
      else appointments.renamePolicy(scope.id(), previous.nome(), saved.nome());
    }
    return saved;
  }

  @Override
  @Transactional
  public void delete(String type, String id) {
    lock(type, id);
    String name = item(type, id).nome();
    String owner = scope.id();
    boolean used =
        type.equals("esiti")
            ? appointments.existsByOwnerIdAndEsito(owner, name)
            : appointments.existsByOwnerIdAndPolizza(owner, name);
    if (used)
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Questo valore è usato da appuntamenti. Puoi rinominarlo oppure modificare prima gli"
              + " appuntamenti.");
    catalogs.deleteByIdAndOwnerId(id, owner);
  }

  /** The explicit owner is generated or claimed by registration, before a JWT exists. */
  @Override
  @Transactional
  public void initialize(String ownerId) {
    if (metadata.catalogsInitialized(ownerId)) return;
    if (catalogs.countByOwnerId(ownerId) == 0) {
      List<CatalogItem> defaults = new ArrayList<>();
      DefaultCatalogs.OUTCOMES.forEach(
          name ->
              defaults.add(mapper.toEntity(UUID.randomUUID().toString(), ownerId, "esiti", name)));
      DefaultCatalogs.POLICIES.forEach(
          name ->
              defaults.add(
                  mapper.toEntity(UUID.randomUUID().toString(), ownerId, "polizze", name)));
      catalogs.insert(defaults);
    }
    metadata.markCatalogsInitialized(ownerId);
  }
}
