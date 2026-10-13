package xyz.nardone.agenda_wa.servizio.mapper;

import xyz.nardone.agenda_wa.servizio.DTO.client.ClientAppointmentDTO;
import org.springframework.stereotype.Component;
import xyz.nardone.agenda_wa.servizio.DTO.client.ClientDTO;
import xyz.nardone.agenda_wa.servizio.DTO.client.ClientRequestDTO;
import xyz.nardone.agenda_wa.servizio.DTO.client.ClientSummaryDTO;
import xyz.nardone.agenda_wa.servizio.entity.Client;
import xyz.nardone.agenda_wa.servizio.entity.ClientType;

@Component
public class ClientMapper {
  public Client toEntity(String id, String ownerId, ClientRequestDTO request) {
    boolean company = request.tipo() == ClientType.AZIENDA;
    String nome = company ? null : request.nome().strip();
    String cognome = company ? null : request.cognome().strip();
    String legalName = company ? request.nomeLegale().strip() : null;
    return new Client(id, ownerId, request.tipo(), nome, cognome, legalName, request.telefono(), java.util.List.of());
  }

  public ClientDTO toDTO(Client entity) {
    return new ClientDTO(entity.id(), entity.tipo(), entity.nome(), entity.cognome(),
        entity.nomeLegale(), entity.telefono());
  }

  public ClientAppointmentDTO toAppointmentDTO(
      Client.AppointmentSummary summary) {
    return new ClientAppointmentDTO(
        summary.id(), summary.data(), summary.esito());
  }

  public ClientSummaryDTO toSummary(Client entity) {
    return new ClientSummaryDTO(entity.id(), entity.tipo(), entity.nome(), entity.cognome(),
        entity.nomeLegale(), entity.telefono());
  }
}
