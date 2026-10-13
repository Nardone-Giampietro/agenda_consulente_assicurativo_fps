package xyz.nardone.agenda_wa.servizio.repository;

import xyz.nardone.agenda_wa.servizio.entity.Client;

public interface ClientRepositoryCustom {
  long updateDetails(String ownerId, Client client);

  long rebuildAppointmentHistory(String ownerId, String clientId);

  RepositoryPage<Client.AppointmentSummary> history(String ownerId, String clientId, int page, int size);

  RepositoryPage<Client> search(String ownerId, String search, int page, int size);
}
