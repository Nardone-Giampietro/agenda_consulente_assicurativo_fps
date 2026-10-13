package xyz.nardone.agenda_wa.servizio.service;

import static xyz.nardone.agenda_wa.servizio.exception.ApplicationErrors.bad;
import static xyz.nardone.agenda_wa.servizio.util.CommonUtil.key;

import jakarta.validation.Validator;
import java.util.*;
import org.springframework.stereotype.Component;
import xyz.nardone.agenda_wa.servizio.DTO.backup.BackupDataDTO;
import xyz.nardone.agenda_wa.servizio.entity.ClientType;

@Component
public class BackupValidator {
  private final Validator validator;

  public BackupValidator(Validator validator) {
    this.validator = validator;
  }

  public void validate(BackupDataDTO d) {
    if (d == null || !validator.validate(d).isEmpty())
      throw bad("Il file contiene dati non validi o troppo lunghi.");
    var clients = new HashSet<String>();
    for (var c : d.clienti()) {
      if (c == null || !clients.add(c.id()))
        throw bad("Il file contiene clienti duplicati o non validi.");
      if (c.tipo() == ClientType.AZIENDA) {
        if (c.nomeLegale() == null
            || c.nomeLegale().isBlank()
            || c.nome() != null
            || c.cognome() != null) throw bad("Le generalità di un’azienda non sono valide.");
      } else if (c.nome() == null
          || c.nome().isBlank()
          || c.cognome() == null
          || c.cognome().isBlank()
          || c.nomeLegale() != null) throw bad("Le generalità di una persona non sono valide.");
    }
    var catalogIds = new HashSet<String>();
    var keys = new HashSet<String>();
    var names = new HashSet<String>();
    for (var c : d.cataloghi()) {
      if (c == null || !catalogIds.add(c.id()) || !keys.add(c.tipo() + ":" + key(c.nome())))
        throw bad("Il file contiene cataloghi duplicati o non validi.");
      names.add(c.tipo() + ":" + c.nome());
    }
    var appointmentIds = new HashSet<String>();
    for (var a : d.appuntamenti()) {
      if (a == null
          || !appointmentIds.add(a.id())
          || !clients.contains(a.clientId())
          || !names.contains("esiti:" + a.esito())
          || (a.polizza() != null && !names.contains("polizze:" + a.polizza())))
        throw bad("Il file contiene appuntamenti con riferimenti non validi.");
    }
    var callbackIds = new HashSet<String>();
    for (var c : d.callback()) {
      if (c == null
          || !callbackIds.add(c.id())
          || !clients.contains(c.clientId())
          || (c.data() != null && c.data().isBefore(c.dataAppuntamento())))
        throw bad("Il file contiene callback con riferimenti o date non validi.");
    }
  }
}
