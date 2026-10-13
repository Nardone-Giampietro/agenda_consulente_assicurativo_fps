package xyz.nardone.agenda_wa.servizio;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import xyz.nardone.agenda_wa.servizio.DTO.activity.*;
import xyz.nardone.agenda_wa.servizio.DTO.catalog.CatalogRequestDTO;
import xyz.nardone.agenda_wa.servizio.DTO.client.ClientRequestDTO;
import xyz.nardone.agenda_wa.servizio.entity.*;
import xyz.nardone.agenda_wa.servizio.repository.*;
import xyz.nardone.agenda_wa.servizio.security.LoginAttempts;
import xyz.nardone.agenda_wa.servizio.service.interfaces.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LocalIntegrationTest {
  static final String PASSWORD = "password-fittizia-agenda";
  static final Path DIRECTORY = createDirectory();

  static Path createDirectory() {
    try {
      return Files.createTempDirectory("agenda_wa_test_sql_");
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @BeforeAll
  void start() {
    String hash =
        new Pbkdf2PasswordEncoder(
                "",
                16,
                600000,
                Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256)
            .encode(PASSWORD);
    context =
        new org.springframework.boot.builder.SpringApplicationBuilder(AgendaWaApplication.class)
            .run(
                "--server.port=0",
                "--agenda.data-dir=" + DIRECTORY,
                "--agenda.auth.key-file=" + DIRECTORY.resolve("jwt.key"),
                "--agenda.auth.fixed-password-hash=" + hash,
                "--agenda.initial-data.enabled=false",
                "--agenda.open-browser=false",
                "--agenda.backup.enabled=false");
    context.getAutowireCapableBeanFactory().autowireBean(this);
  }

  @Value("${local.server.port}")
  int port;

  @Autowired JdbcTemplate jdbc;
  @Autowired ConfigurableApplicationContext context;
  @Autowired PlatformTransactionManager transactionManager;
  @Autowired UserRepository users;
  @Autowired ClientRepository clients;
  @Autowired CatalogItemRepository catalogRows;
  @Autowired AppointmentRepository appointmentRows;
  @Autowired CallbackRepository callbackRows;
  @Autowired ClientService clientService;
  @Autowired ActivityService activities;
  @Autowired CatalogService catalogs;
  @Autowired OverviewService overview;
  @Autowired BackupService backups;
  @Autowired LoginAttempts attempts;
  final JsonMapper json = JsonMapper.builder().build();
  HttpClient http;
  CookieManager cookies;

  void as(String owner) {
    Jwt jwt =
        Jwt.withTokenValue("fictitious-test-token")
            .header("alg", "HS256")
            .subject(owner)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .build();
    SecurityContextHolder.getContext()
        .setAuthentication(new JwtAuthenticationToken(jwt, List.of(), owner));
  }

  @BeforeEach
  void fixtures() throws Exception {
    assertTrue(DIRECTORY.getFileName().toString().startsWith("agenda_wa_test_"));
    assertEquals(
        DIRECTORY.toRealPath(),
        Path.of(jdbc.queryForObject("SELECT DATABASE_PATH()", String.class))
            .getParent()
            .toRealPath());
    new TransactionTemplate(transactionManager)
        .executeWithoutResult(
            status -> {
              for (String table : List.of("appuntamenti", "callback", "clienti", "cataloghi"))
                jdbc.update(
                    "DELETE FROM " + table + " WHERE owner_id IN (?,?)", "locale", "secondo");
              if (users.findById("secondo").isEmpty())
                users.insert(
                    new User("secondo", "Utente", "Fittizio", "test@example.invalid", "", 0));
              jdbc.update(
                  "UPDATE utenti SET token_version=0 WHERE id IN (?,?)", "locale", "secondo");
              clients.insert(new Client("cli1", "locale", "andré", "di pietro"));
              clients.insert(
                  new Client("cli2", "locale", ClientType.AZIENDA, null, null, "AZIENDA FITTIZIA"));
              clients.insert(new Client("alieno", "secondo", "persona", "altrui"));
              catalogRows.insert(
                  new CatalogItem("esito", "locale", "esiti", "Sottoscrizione", "sottoscrizione"));
              catalogRows.insert(
                  new CatalogItem(
                      "polizza", "locale", "polizze", "Polizza fittizia", "polizza fittizia"));
              catalogRows.insert(
                  new CatalogItem(
                      "esito-altrui", "secondo", "esiti", "Sottoscrizione", "sottoscrizione"));
            });
    as("locale");
    attempts.succeeded("127.0.0.1|locale");
    cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    http = HttpClient.newBuilder().cookieHandler(cookies).build();
  }

  @AfterEach
  void clearSecurity() {
    SecurityContextHolder.clearContext();
  }

  @AfterAll
  void cleanup() throws Exception {
    if (context != null) context.close();
    try (var paths = Files.walk(DIRECTORY)) {
      for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
    }
    assertFalse(Files.exists(DIRECTORY));
    try (var socket = new java.net.Socket()) {
      assertThrows(
          java.io.IOException.class,
          () -> socket.connect(new InetSocketAddress("127.0.0.1", port), 200));
    }
  }

  URI uri(String path) {
    return URI.create("http://127.0.0.1:" + port + "/api/" + path);
  }

  HttpResponse<String> get(String path) throws Exception {
    return http.send(
        HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
  }

  HttpRequest.Builder mutation(String path) throws Exception {
    JsonNode csrf = json.readTree(get("auth/csrf").body());
    return HttpRequest.newBuilder(uri(path))
        .header(csrf.get("headerName").asString(), csrf.get("token").asString());
  }

  HttpResponse<String> send(String method, String path, String body) throws Exception {
    return http.send(
        mutation(path)
            .header("Content-Type", "application/json")
            .method(method, HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  void login() throws Exception {
    assertEquals(
        200, send("POST", "auth/login", "{\"password\":\"" + PASSWORD + "\"}").statusCode());
  }

  Appointment add(String id, String client, LocalDate date, String note, BigDecimal amount) {
    as("locale");
    return activities.saveAppointment(
        id, new AppointmentRequestDTO(date, client, "esito", "polizza", amount, note, false, null));
  }

  @Test
  void passwordOnlyLoginCookiesCsrfAndRevocation() throws Exception {
    assertEquals(401, get("clienti").statusCode());
    assertEquals(401, get("backup").statusCode());
    assertEquals(401, send("POST", "auth/login", "{\"password\":\"errata\"}").statusCode());
    var response = send("POST", "auth/login", "{\"password\":\"" + PASSWORD + "\"}");
    assertEquals(200, response.statusCode());
    String cookie = response.headers().firstValue("set-cookie").orElseThrow();
    assertTrue(cookie.contains("HttpOnly"));
    assertTrue(cookie.contains("SameSite=Strict"));
    assertFalse(response.body().contains("password"));
    assertFalse(response.body().contains("token"));
    assertEquals(200, get("clienti").statusCode());
    var forbidden =
        http.send(
            HttpRequest.newBuilder(uri("clienti/cli1")).DELETE().build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(403, forbidden.statusCode());
    assertEquals(404, send("POST", "auth/register", "{}").statusCode());
    assertEquals(204, send("POST", "auth/logout", "{}").statusCode());
    assertEquals(401, get("clienti").statusCode());
  }

  @Test
  void foreignIdsReferencesAndOwnerPayloadAreRejected() throws Exception {
    login();
    assertEquals(404, get("clienti/alieno").statusCode());
    var denied =
        send(
            "POST",
            "appuntamenti",
            "{\"data\":\"2026-10-13\",\"clientId\":\"alieno\",\"esitoId\":\"esito\",\"premio\":0,\"creaCallback\":false}");
    assertEquals(404, denied.statusCode(), denied.body());
    assertEquals(
        400,
        send(
                "POST",
                "appuntamenti",
                "{\"data\":\"2026-10-13\",\"clientId\":\"cli1\",\"esitoId\":\"esito-altrui\",\"premio\":0,\"creaCallback\":false}")
            .statusCode());
    assertEquals(0, appointmentRows.countByOwnerId("locale"));
    assertEquals(1, clients.countByOwnerId("secondo"));
    var created =
        send(
            "POST",
            "clienti",
            "{\"tipo\":\"PERSONA_FISICA\",\"nome\":\"Fittizio\",\"cognome\":\"Test\",\"ownerId\":\"secondo\"}");
    assertEquals(201, created.statusCode());
    String id = json.readTree(created.body()).get("id").asString();
    assertTrue(clients.findByIdAndOwnerId(id, "locale").isPresent());
    assertTrue(clients.findByIdAndOwnerId(id, "secondo").isEmpty());
  }

  @Test
  void searchesPreserveAccentsWholeWordsAndTotals() {
    LocalDate date = LocalDate.of(2026, 10, 13);
    add(null, "cli1", date, "Rinnovo già concordato", new BigDecimal("12.20"));
    add(null, "cli2", date.minusDays(1), "Rinnovi possibili", new BigDecimal("3.00"));
    assertEquals(1, activities.appointments(null, null, null, "ANDRE rinnovo", 0, 20).total());
    assertEquals(1, activities.appointments(null, null, null, "\"di\" -rinnovo", 0, 20).total());
    assertEquals(0, activities.appointments(null, null, null, "di rinn", 0, 20).total());
    assertEquals(0, activities.appointments(null, null, null, "andr", 0, 20).total());
    assertEquals(0, activities.appointments(null, null, null, ".*", 0, 20).total());
    assertEquals(1, activities.appointments(null, null, null, "rinnovi", 0, 20).total());
    assertEquals(1, clientService.clients("ANDR", 0, 20).total());
    assertEquals(
        new BigDecimal("12.20"), overview.summary(null, null, null, "andre rinnovo").premi());
    clientService.saveClient(
        "cli1",
        new ClientRequestDTO(ClientType.PERSONA_FISICA, "Émilie", "Di Pietro", null, "0123"));
    assertEquals(1, activities.appointments(null, null, null, "emilie rinnovo", 0, 20).total());
    assertEquals(0, activities.appointments(null, null, null, "andre", 0, 20).total());
  }

  @Test
  void historyIsDerivedAfterRenameMoveAndDeletion() {
    var a = add(null, "cli1", LocalDate.of(2026, 10, 13), "Annotazione", new BigDecimal("7.50"));
    assertEquals(1, clientService.history("cli1", 0, 20).total());
    catalogs.save("esiti", "esito", new CatalogRequestDTO("Esito rinominato"));
    assertEquals(
        "Esito rinominato", clientService.history("cli1", 0, 20).items().getFirst().esito());
    add(a.id(), "cli2", a.data(), a.note(), a.premio());
    assertEquals(0, clientService.history("cli1", 0, 20).total());
    assertEquals(1, clientService.history("cli2", 0, 20).total());
    activities.deleteAppointment(a.id());
    assertEquals(0, clientService.history("cli2", 0, 20).total());
    assertThrows(Exception.class, () -> clientService.history("alieno", 0, 20));
    assertThrows(
        Exception.class,
        () -> clientService.saveClient("cli2", new ClientRequestDTO("Nome", "Cognome")));
  }

  @Test
  void todayCallbacksPrecedeNullDatesAcrossPages() {
    LocalDate today = LocalDate.now(ZoneId.of("Europe/Rome"));
    for (int i = 0; i < 24; i++)
      activities.saveCallback(
          null, new CallbackRequestDTO(null, today.minusDays(2), "cli1", "Da pianificare"));
    activities.saveCallback(
        null, new CallbackRequestDTO(today, today.minusDays(2), "cli1", "Oggi"));
    var first = activities.callbacks(null, null, 0, 20);
    assertEquals(25, first.total());
    assertEquals(today, first.items().getFirst().data());
    assertNull(first.items().get(1).data());
    assertEquals(1, activities.callbacks(today, today, 0, 20).total());
    assertEquals(5, activities.callbacks(null, null, 1, 20).items().size());
  }

  @Test
  void appointmentWithCallbackIsAtomicAndClientDeletionCascades() {
    LocalDate date = LocalDate.of(2026, 10, 13);
    assertThrows(
        Exception.class,
        () ->
            activities.saveAppointment(
                null,
                new AppointmentRequestDTO(
                    date,
                    "cli1",
                    "esito",
                    "polizza",
                    BigDecimal.ZERO,
                    "",
                    true,
                    date.minusDays(1))));
    assertEquals(0, appointmentRows.countByOwnerId("locale"));
    assertEquals(0, callbackRows.countByOwnerId("locale"));
    var a =
        activities.saveAppointment(
            null,
            new AppointmentRequestDTO(
                date, "cli1", "esito", "polizza", new BigDecimal("2.35"), "", true, date));
    assertEquals(1, callbackRows.countByOwnerId("locale"));
    assertThrows(Exception.class, () -> catalogs.delete("polizze", "polizza"));
    activities.deleteAppointment(a.id());
    assertEquals(1, callbackRows.countByOwnerId("locale"));
    clientService.deleteClient("cli1");
    assertEquals(0, callbackRows.countByOwnerId("locale"));
    assertEquals(1, clients.countByOwnerId("secondo"));
  }

  @Test
  void filteredDeletionUsesExclusiveDatesAndExpectedCount() {
    LocalDate date = LocalDate.of(2026, 10, 13);
    for (int i = 0; i < 8; i++) add(null, "cli1", date.minusDays(i), "", BigDecimal.ZERO);
    var before = new AppointmentSelectionDTO(AppointmentPeriod.PRIMA, null, null, date, null);
    assertEquals(7, activities.selectedAppointments(before).total());
    assertEquals(5, activities.selectedAppointments(before).items().size());
    assertThrows(Exception.class, () -> activities.deleteSelectedAppointments(before, 6));
    assertEquals(8, appointmentRows.countByOwnerId("locale"));
    assertEquals(7, activities.deleteSelectedAppointments(before, 7));
    assertEquals(1, clientService.history("cli1", 0, 20).total());
  }

  @Test
  void backupRoundtripKeepsIdsDecimalsAndForeignData() throws Exception {
    var a =
        add(
            null,
            "cli1",
            LocalDate.of(2026, 10, 13),
            "Note con accenti: è",
            new BigDecimal("123.45"));
    byte[] exported = backups.exportBackup();
    String text = new String(exported, java.nio.charset.StandardCharsets.UTF_8);
    assertFalse(text.contains("ownerId"));
    assertFalse(text.contains("password"));
    assertFalse(text.contains("secondo"));
    activities.deleteAppointment(a.id());
    clientService.deleteClient("cli1");
    var restored = backups.restore(exported);
    assertEquals(2, restored.clienti());
    assertEquals(1, restored.appuntamenti());
    assertEquals(
        new BigDecimal("123.45"),
        appointmentRows.findByIdAndOwnerId(a.id(), "locale").orElseThrow().premio());
    assertEquals(1, clients.countByOwnerId("secondo"));
    assertEquals(1, clientService.history("cli1", 0, 20).total());
    try (var paths = Files.list(DIRECTORY.resolve("backups"))) {
      assertTrue(paths.anyMatch(p -> p.getFileName().toString().startsWith("prima-ripristino-")));
    }
  }

  @Test
  void corruptBackupsFailBeforeChangingDataAndAutomaticCopiesAreBounded() throws Exception {
    byte[] file = backups.exportBackup();
    String text = new String(file, java.nio.charset.StandardCharsets.UTF_8);
    byte[] corrupt =
        text.replace("andré", "modificato").getBytes(java.nio.charset.StandardCharsets.UTF_8);
    assertThrows(Exception.class, () -> backups.restore(corrupt));
    assertEquals("andré", clients.findByIdAndOwnerId("cli1", "locale").orElseThrow().nome());
    assertThrows(Exception.class, () -> backups.restore("DROP ALL OBJECTS".getBytes()));
    backups.automaticBackup();
    long count = backups.status().copieAutomatiche();
    backups.automaticBackup();
    assertEquals(count, backups.status().copieAutomatiche());
    for (int i = 0; i < 16; i++) {
      clientService.saveClient(
          "cli1",
          new ClientRequestDTO(ClientType.PERSONA_FISICA, "andré", "di pietro", null, "0" + i));
      backups.automaticBackup();
    }
    assertEquals(14, backups.status().copieAutomatiche());
  }

  @Test
  void sqlConstraintsAndUpdatesDoNotResurrectDeletedRows() {
    var a = add(null, "cli1", LocalDate.of(2026, 10, 13), "", BigDecimal.ZERO);
    activities.deleteAppointment(a.id());
    assertThrows(Exception.class, () -> appointmentRows.update(a));
    assertEquals(0, appointmentRows.countByOwnerId("locale"));
    assertThrows(
        Exception.class,
        () ->
            callbackRows.insert(
                new Callback("broken", "locale", null, LocalDate.now(), "alieno", "")));
    assertThrows(
        Exception.class,
        () ->
            catalogRows.insert(
                new CatalogItem(
                    "duplicate", "locale", "polizze", "POLIZZA FITTIZIA", "polizza fittizia")));
  }

  @Test
  void multipartRestoreRequiresCsrfAndInvalidatesSession() throws Exception {
    add(null, "cli1", LocalDate.of(2026, 10, 13), "Test del ripristino", new BigDecimal("17.25"));
    login();
    byte[] data = backups.exportBackup();
    String boundary = "agenda-fixture-boundary";
    byte[] prefix =
        ("--"
                + boundary
                + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"backup.json\"\r\n"
                + "Content-Type: application/json\r\n\r\n")
            .getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    byte[] suffix =
        ("\r\n--" + boundary + "--\r\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    var request =
        mutation("backup/ripristino")
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(HttpRequest.BodyPublishers.ofByteArrays(List.of(prefix, data, suffix)))
            .build();
    var response = http.send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode(), response.body());
    assertEquals(1, json.readTree(response.body()).get("appuntamenti").asInt());
    assertEquals(401, get("auth/me").statusCode());
    login();
    assertEquals(200, get("clienti").statusCode());
  }

  @Test
  void writerLockPreventsConcurrentPutFromResurrectingDeletion() throws Exception {
    var a = add(null, "cli1", LocalDate.of(2026, 10, 13), "", BigDecimal.ZERO);
    var deleted = new java.util.concurrent.CountDownLatch(1);
    var release = new java.util.concurrent.CountDownLatch(1);
    var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
    try {
      var first =
          executor.submit(
              () ->
                  new TransactionTemplate(transactionManager)
                      .executeWithoutResult(
                          status -> {
                            as("locale");
                            activities.deleteAppointment(a.id());
                            deleted.countDown();
                            try {
                              assertTrue(release.await(5, java.util.concurrent.TimeUnit.SECONDS));
                            } catch (InterruptedException e) {
                              throw new IllegalStateException(e);
                            } finally {
                              SecurityContextHolder.clearContext();
                            }
                          }));
      assertTrue(deleted.await(5, java.util.concurrent.TimeUnit.SECONDS));
      var second =
          executor.submit(
              () -> {
                as("locale");
                try {
                  assertThrows(
                      Exception.class,
                      () -> add(a.id(), "cli1", a.data(), "Aggiornato", BigDecimal.ZERO));
                } finally {
                  SecurityContextHolder.clearContext();
                }
              });
      release.countDown();
      first.get(5, java.util.concurrent.TimeUnit.SECONDS);
      second.get(5, java.util.concurrent.TimeUnit.SECONDS);
      assertEquals(0, appointmentRows.countByOwnerId("locale"));
    } finally {
      release.countDown();
      executor.shutdownNow();
    }
  }

  @Test
  void normalizationAndSearchRespectPersistenceAndBackupLimits() throws Exception {
    assertThrows(
        Exception.class,
        () ->
            clientService.saveClient(
                null, new ClientRequestDTO(ClientType.AZIENDA, null, null, "ß".repeat(100))));
    assertThrows(
        Exception.class,
        () -> clientService.saveClient(null, new ClientRequestDTO("İ".repeat(100), "cognome")));
    var company =
        clientService.saveClient(
            null, new ClientRequestDTO(ClientType.AZIENDA, null, null, "ß".repeat(75)));
    assertEquals(150, company.nomeLegale().length());
    add(null, company.id(), LocalDate.now(), "각".repeat(5000), BigDecimal.ZERO);
    byte[] backup = backups.exportBackup();
    backups.restore(backup);
    assertEquals(5000, appointmentRows.findByOwnerId("locale").getFirst().note().length());
    assertEquals(
        company.nomeLegale(),
        clients.findByIdAndOwnerId(company.id(), "locale").orElseThrow().nomeLegale());
  }

  @Test
  void largeBackupCanBeExportedAutomaticallySavedAndRestored() throws Exception {
    new TransactionTemplate(transactionManager)
        .executeWithoutResult(
            status -> {
              var details =
                  Appointment.ClientDetails.from(
                      clients.findByIdAndOwnerId("cli1", "locale").orElseThrow());
              String notes = "a".repeat(5000);
              for (int i = 0; i < 4200; i++)
                appointmentRows.insert(
                    new Appointment(
                        "large-" + i,
                        "locale",
                        LocalDate.now(),
                        "cli1",
                        details,
                        "Sottoscrizione",
                        "Polizza fittizia",
                        BigDecimal.ZERO,
                        notes));
            });
    byte[] backup = backups.exportBackup();
    assertTrue(backup.length > 20 * 1024 * 1024);
    backups.automaticBackup();
    assertTrue(backups.status().copieAutomatiche() > 0);
    backups.restore(new java.io.ByteArrayInputStream(backup));
    assertEquals(4200, appointmentRows.countByOwnerId("locale"));
  }
}
