package xyz.nardone.agenda_wa.servizio.service;

import static xyz.nardone.agenda_wa.servizio.util.CommonUtil.key;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import xyz.nardone.agenda_wa.servizio.DTO.backup.BackupDataDTO;
import xyz.nardone.agenda_wa.servizio.config.DefaultCatalogs;
import xyz.nardone.agenda_wa.servizio.entity.*;
import xyz.nardone.agenda_wa.servizio.repository.*;
import xyz.nardone.agenda_wa.servizio.security.LocalIdentity;

@Service
public class LocalInitializationService {
  private final UserRepository users;
  private final ClientRepository clients;
  private final CatalogItemRepository catalogs;
  private final MetadataRepository metadata;
  private final SqlOperationLock lock;
  private final BackupValidator validator;
  private final String initialFile;
  private final boolean initialEnabled;

  public record InitialData(
      List<BackupDataDTO.ClientData> clienti, List<BackupDataDTO.CatalogData> cataloghi) {}

  public LocalInitializationService(
      UserRepository users,
      ClientRepository clients,
      CatalogItemRepository catalogs,
      MetadataRepository metadata,
      SqlOperationLock lock,
      BackupValidator validator,
      @Value("${agenda.initial-data-file:}") String initialFile,
      @Value("${agenda.initial-data.enabled:true}") boolean enabled) {
    this.users = users;
    this.clients = clients;
    this.catalogs = catalogs;
    this.metadata = metadata;
    this.lock = lock;
    this.validator = validator;
    this.initialFile = initialFile;
    this.initialEnabled = enabled;
  }

  @Transactional(rollbackFor = Exception.class)
  public void initialize() throws Exception {
    lock.acquire();
    String owner = LocalIdentity.ID;
    if (users.findById(owner).isEmpty())
      users.insert(new User(owner, "Agenda", "personale", "locale@localhost", "", 0));
    if (metadata.catalogsInitialized(owner)) return;
    InitialData initial = null;
    if (initialEnabled) {
      var json = JsonMapper.builder().build();
      if (!initialFile.isBlank())
        initial = json.readValue(Files.readAllBytes(Path.of(initialFile)), InitialData.class);
      else {
        var resource = new ClassPathResource("initial-data.json");
        if (resource.exists())
          try (var stream = resource.getInputStream()) {
            initial = json.readValue(stream, InitialData.class);
          }
      }
    }
    if (initial != null) {
      validator.validate(
          new BackupDataDTO(initial.clienti(), initial.cataloghi(), List.of(), List.of()));
      if (initial.cataloghi().stream().anyMatch(c -> !c.tipo().equals("polizze")))
        throw new IllegalStateException(
            "I dati iniziali possono contenere solo clienti e polizze.");
    }
    DefaultCatalogs.OUTCOMES.forEach(
        name ->
            catalogs.insert(
                new CatalogItem(UUID.randomUUID().toString(), owner, "esiti", name, key(name))));
    if (initial == null)
      DefaultCatalogs.POLICIES.forEach(
          name ->
              catalogs.insert(
                  new CatalogItem(
                      UUID.randomUUID().toString(), owner, "polizze", name, key(name))));
    else {
      for (var c : initial.cataloghi())
        catalogs.insert(new CatalogItem(c.id(), owner, c.tipo(), c.nome(), key(c.nome())));
      for (var c : initial.clienti())
        clients.insert(
            new Client(
                c.id(),
                owner,
                c.tipo(),
                c.nome(),
                c.cognome(),
                c.nomeLegale(),
                c.telefono(),
                List.of()));
    }
    metadata.markCatalogsInitialized(owner);
  }
}
