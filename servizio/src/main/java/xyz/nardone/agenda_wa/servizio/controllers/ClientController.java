package xyz.nardone.agenda_wa.servizio.controllers;

import xyz.nardone.agenda_wa.servizio.DTO.client.ClientAppointmentDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import xyz.nardone.agenda_wa.servizio.DTO.client.ClientDTO;
import xyz.nardone.agenda_wa.servizio.DTO.client.ClientRequestDTO;
import xyz.nardone.agenda_wa.servizio.DTO.client.ClientSummaryDTO;
import xyz.nardone.agenda_wa.servizio.DTO.common.PageDTO;
import xyz.nardone.agenda_wa.servizio.mapper.ClientMapper;
import xyz.nardone.agenda_wa.servizio.service.interfaces.ClientService;

@RestController
@RequestMapping("/api")
public class ClientController {
  private final ClientMapper mapper;
  private final ClientService clients;

  public ClientController(ClientService clients, ClientMapper mapper) {
    this.mapper = mapper;
    this.clients = clients;
  }

  @GetMapping("/clienti")
  public PageDTO<ClientSummaryDTO> clients(
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return clients.clients(q, page, size);
  }

  @GetMapping("/clienti/{id}")
  public ClientDTO client(@PathVariable String id) {
    return mapper.toDTO(clients.client(id));
  }

  @GetMapping("/clienti/{id}/appuntamenti")
  public PageDTO<ClientAppointmentDTO> history(
      @PathVariable String id, @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return clients.history(id, page, size);
  }

  @PostMapping("/clienti")
  @ResponseStatus(HttpStatus.CREATED)
  public ClientDTO createClient(@Valid @RequestBody ClientRequestDTO p) {
    return mapper.toDTO(clients.saveClient(null, p));
  }

  @PutMapping("/clienti/{id}")
  public ClientDTO updateClient(@PathVariable String id, @Valid @RequestBody ClientRequestDTO p) {
    return mapper.toDTO(clients.saveClient(id, p));
  }

  @DeleteMapping("/clienti/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteClient(@PathVariable String id) {
    clients.deleteClient(id);
  }

  @DeleteMapping("/clienti")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void clearClients() {
    clients.clearClients();
  }
}
