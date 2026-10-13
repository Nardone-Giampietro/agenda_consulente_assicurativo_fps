package xyz.nardone.agenda_wa.servizio.service;

import static xyz.nardone.agenda_wa.servizio.exception.ApplicationErrors.bad;
import static xyz.nardone.agenda_wa.servizio.util.CommonUtil.key;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import xyz.nardone.agenda_wa.servizio.DTO.backup.*;
import xyz.nardone.agenda_wa.servizio.entity.*;
import xyz.nardone.agenda_wa.servizio.repository.*;
import xyz.nardone.agenda_wa.servizio.security.LocalIdentity;
import xyz.nardone.agenda_wa.servizio.security.UserScope;
import xyz.nardone.agenda_wa.servizio.service.interfaces.BackupService;

@Service
public class BackupServiceImpl implements BackupService {
  private final ClientRepository clients;
  private final CatalogItemRepository catalogs;
  private final AppointmentRepository appointments;
  private final CallbackRepository callbacks;
  private final UserRepository users;
  private final MetadataRepository metadata;
  private final SqlOperationLock lock;
  private final BackupFilesRepository files;
  private final BackupValidator validator;
  private final UserScope scope;
  private final Clock clock;
  private final int retention;
  private final JsonMapper json = JsonMapper.builder().build();
  private String lastAutomaticDigest;

  public BackupServiceImpl(
      ClientRepository clients,
      CatalogItemRepository catalogs,
      AppointmentRepository appointments,
      CallbackRepository callbacks,
      UserRepository users,
      MetadataRepository metadata,
      SqlOperationLock lock,
      BackupFilesRepository files,
      BackupValidator validator,
      UserScope scope,
      Clock clock,
      @Value("${agenda.backup.retention:14}") int retention) {
    this.clients = clients;
    this.catalogs = catalogs;
    this.appointments = appointments;
    this.callbacks = callbacks;
    this.users = users;
    this.metadata = metadata;
    this.lock = lock;
    this.files = files;
    this.validator = validator;
    this.scope = scope;
    this.clock = clock;
    this.retention = Math.max(1, retention);
  }

  private BackupDataDTO snapshot(String owner) {
    return new BackupDataDTO(
        clients.findByOwnerId(owner).stream()
            .map(
                c ->
                    new BackupDataDTO.ClientData(
                        c.id(), c.tipo(), c.nome(), c.cognome(), c.nomeLegale(), c.telefono()))
            .toList(),
        catalogs.findByOwnerId(owner).stream()
            .map(c -> new BackupDataDTO.CatalogData(c.id(), c.tipo(), c.nome()))
            .toList(),
        appointments.findByOwnerId(owner).stream()
            .map(
                a ->
                    new BackupDataDTO.AppointmentData(
                        a.id(),
                        a.data(),
                        a.clientId(),
                        a.esito(),
                        a.polizza(),
                        a.premio(),
                        a.note()))
            .toList(),
        callbacks.findByOwnerId(owner).stream()
            .map(
                c ->
                    new BackupDataDTO.CallbackData(
                        c.id(), c.data(), c.dataAppuntamento(), c.clientId(), c.note()))
            .toList());
  }

  private String digest(BackupDataDTO data) {
    try {
      return HexFormat.of()
          .formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(data)));
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private byte[] encode(BackupDataDTO data) {
    byte[] bytes =
        json.writeValueAsBytes(
            new BackupDocumentDTO("agenda-wa-backup", 1, clock.instant(), digest(data), data));
    return bytes;
  }

  @Override
  @Transactional
  public byte[] exportBackup() {
    lock.acquire();
    return encode(snapshot(scope.id()));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public RestoreResultDTO restore(byte[] file) throws Exception {
    return restore(new ByteArrayInputStream(file));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public RestoreResultDTO restore(InputStream file) throws Exception {
    BackupDocumentDTO document;
    try {
      document = json.readValue(file, BackupDocumentDTO.class);
    } catch (Exception e) {
      throw bad("Il file non è un backup valido dell’agenda.");
    }
    if (document == null
        || !"agenda-wa-backup".equals(document.formato())
        || document.versione() != 1
        || document.creatoAlle() == null
        || document.sha256() == null)
      throw bad("Il formato o la versione del backup non sono supportati.");
    validator.validate(document.dati());
    if (!MessageDigest.isEqual(
        digest(document.dati()).getBytes(java.nio.charset.StandardCharsets.US_ASCII),
        document.sha256().getBytes(java.nio.charset.StandardCharsets.US_ASCII)))
      throw bad("Il backup è danneggiato: i dati non corrispondono alla sua impronta.");
    lock.acquire();
    String owner = scope.id();
    files.write("prima-ripristino-", encode(snapshot(owner)), 5);
    var d = document.dati();
    appointments.deleteByOwnerId(owner);
    callbacks.deleteByOwnerId(owner);
    clients.deleteByOwnerId(owner);
    catalogs.deleteByOwnerId(owner);
    var clientMap = new HashMap<String, Client>();
    for (var c : d.clienti()) {
      Client saved =
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
      clientMap.put(saved.id(), saved);
    }
    for (var c : d.cataloghi())
      catalogs.insert(new CatalogItem(c.id(), owner, c.tipo(), c.nome(), key(c.nome())));
    for (var a : d.appuntamenti())
      appointments.insert(
          new Appointment(
              a.id(),
              owner,
              a.data(),
              a.clientId(),
              Appointment.ClientDetails.from(clientMap.get(a.clientId())),
              a.esito(),
              a.polizza(),
              a.premio(),
              a.note()));
    for (var c : d.callback())
      callbacks.insert(
          new Callback(c.id(), owner, c.data(), c.dataAppuntamento(), c.clientId(), c.note()));
    metadata.markCatalogsInitialized(owner);
    users.revokeTokens(owner);
    return new RestoreResultDTO(
        d.clienti().size(), d.cataloghi().size(), d.appuntamenti().size(), d.callback().size());
  }

  @Override
  public BackupStatusDTO status() throws Exception {
    var backups = files.list("automatico-");
    return new BackupStatusDTO(
        backups.isEmpty() ? null : files.modified(backups.getFirst()), backups.size());
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void automaticBackup() throws Exception {
    lock.acquire();
    var data = snapshot(LocalIdentity.ID);
    String hash = digest(data);
    if (hash.equals(lastAutomaticDigest)) return;
    files.write("automatico-", encode(data), retention);
    lastAutomaticDigest = hash;
  }
}
