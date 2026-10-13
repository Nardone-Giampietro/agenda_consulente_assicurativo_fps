package xyz.nardone.agenda_wa.servizio.service.interfaces;

import xyz.nardone.agenda_wa.servizio.DTO.client.ClientAppointmentDTO;
import xyz.nardone.agenda_wa.servizio.DTO.client.ClientRequestDTO;
import xyz.nardone.agenda_wa.servizio.DTO.client.ClientSummaryDTO;
import xyz.nardone.agenda_wa.servizio.DTO.common.PageDTO;
import xyz.nardone.agenda_wa.servizio.entity.Client;

public interface ClientService {
  PageDTO<ClientSummaryDTO> clients(String search, int page, int size);

  Client client(String id);

  PageDTO<ClientAppointmentDTO> history(
      String id, int page, int size);

  Client saveClient(String id, ClientRequestDTO request);

  void lockClient(String id);

  void deleteClient(String id);

  void clearClients();
}
