package xyz.nardone.agenda_wa.servizio.service;

import static xyz.nardone.agenda_wa.servizio.util.CommonUtil.id;

import java.time.Clock;
import java.time.LocalDate;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import xyz.nardone.agenda_wa.servizio.DTO.overview.NavigationDTO;
import xyz.nardone.agenda_wa.servizio.DTO.overview.OverviewDTO;
import xyz.nardone.agenda_wa.servizio.DTO.overview.SummaryDTO;
import xyz.nardone.agenda_wa.servizio.mapper.OverviewMapper;
import xyz.nardone.agenda_wa.servizio.repository.AppointmentRepository;
import xyz.nardone.agenda_wa.servizio.repository.CallbackRepository;
import xyz.nardone.agenda_wa.servizio.repository.ClientRepository;
import xyz.nardone.agenda_wa.servizio.security.UserScope;
import xyz.nardone.agenda_wa.servizio.service.interfaces.ActivityService;
import xyz.nardone.agenda_wa.servizio.service.interfaces.CatalogService;
import xyz.nardone.agenda_wa.servizio.service.interfaces.OverviewService;

@Service
@org.springframework.transaction.annotation.Transactional(
    readOnly = true,
    isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
public class OverviewServiceImpl implements OverviewService {
  private final AppointmentRepository appointments;
  private final CallbackRepository callbacks;
  private final ClientRepository clients;
  private final CatalogService catalogs;
  private final ActivityService activities;
  private final OverviewMapper mapper;
  private final Clock clock;
  private final UserScope scope;

  public OverviewServiceImpl(
      AppointmentRepository appointments,
      CallbackRepository callbacks,
      ClientRepository clients,
      CatalogService catalogs,
      ActivityService activities,
      OverviewMapper mapper,
      Clock clock,
      UserScope scope) {
    this.appointments = appointments;
    this.callbacks = callbacks;
    this.clients = clients;
    this.catalogs = catalogs;
    this.activities = activities;
    this.mapper = mapper;
    this.clock = clock;
    this.scope = scope;
  }

  @Override
  public SummaryDTO summary(LocalDate dal, LocalDate al, String esito, String search) {
    String owner = scope.id();
    String outcome = id(esito) == null ? null : catalogs.item("esiti", esito).nome();
    return mapper.summary(appointments.totals(owner, dal, al, outcome, search));
  }

  @Override
  public NavigationDTO navigation() {
    LocalDate today = LocalDate.now(clock);
    return new NavigationDTO(today, callbacks.countByOwnerIdAndData(scope.id(), today));
  }

  @Override
  public OverviewDTO overview() {
    LocalDate today = LocalDate.now(clock);
    String owner = scope.id();
    var limit = PageRequest.of(0, 5);
    return new OverviewDTO(
        today,
        clients.countByOwnerId(owner),
        callbacks.countByOwnerIdAndData(owner, today),
        summary(null, null, null, null),
        activities.appointmentViews(
            appointments.findByOwnerIdAndDataGreaterThanEqualOrderByDataAscIdAsc(
                owner, today, limit)),
        activities.callbackViews(callbacks.findByOwnerIdAndDataOrderByIdAsc(owner, today, limit)));
  }
}
