package xyz.nardone.agenda_wa.servizio.DTO.client;

import xyz.nardone.agenda_wa.servizio.entity.ClientType;

public record ClientDTO(String id, ClientType tipo, String nome, String cognome,
    String nomeLegale, String telefono) {}
