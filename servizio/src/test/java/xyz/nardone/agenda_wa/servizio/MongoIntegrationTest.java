package xyz.nardone.agenda_wa.servizio;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.*;
import xyz.nardone.agenda_wa.servizio.DTO.activity.*;
import xyz.nardone.agenda_wa.servizio.DTO.auth.*;
import xyz.nardone.agenda_wa.servizio.DTO.catalog.*;
import xyz.nardone.agenda_wa.servizio.DTO.client.*;
import xyz.nardone.agenda_wa.servizio.entity.*;
import xyz.nardone.agenda_wa.servizio.service.interfaces.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "AGENDA_TEST_MONGO_URI", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MongoIntegrationTest {
  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    String uri = System.getenv("AGENDA_TEST_MONGO_URI");
    if (!uri.matches("mongodb://[^/]+/agenda_wa_test_[a-zA-Z0-9_]+\\?.*"))
      throw new IllegalArgumentException("Usa un database isolato agenda_wa_test_*. ");
    registry.add("spring.mongodb.uri", () -> uri);
    registry.add(
        "agenda.auth.key-file",
        () -> System.getProperty("java.io.tmpdir") + "/agenda-wa-test-security/integration.key");
  }

  @org.springframework.beans.factory.annotation.Value("${local.server.port}")
  int port;

  @Autowired ClientService clientService;
  @Autowired ActivityService activities;
  @Autowired CatalogService catalogs;
  @Autowired OverviewService overview;
  @Autowired MongoTemplate mongo;
  @Autowired AuthService auth;
  @Autowired JwtDecoder decoder;
  @Autowired xyz.nardone.agenda_wa.servizio.repository.CallbackRepository callbacksRepository;
  @Autowired xyz.nardone.agenda_wa.servizio.repository.AppointmentSchemaRepository schema;
  @Autowired xyz.nardone.agenda_wa.servizio.repository.ClientSchemaRepository clientSchema;
  java.net.http.HttpClient http;
  String csrf;
  String ownerId;

  private java.net.http.HttpRequest.Builder request(java.net.URI uri) {
    return java.net.http.HttpRequest.newBuilder(uri).header("X-XSRF-TOKEN", csrf);
  }

  @AfterEach
  void clearScope() {
    SecurityContextHolder.clearContext();
  }

  @AfterAll
  void removeTestDatabase() throws Exception {
    var database = mongo.getDb();
    if (!database.getName().matches("agenda_wa_test_[a-zA-Z0-9_]+"))
      throw new IllegalStateException("La pulizia è consentita solo sul database isolato di test.");
    database.drop();
    java.nio.file.Files.deleteIfExists(
        java.nio.file.Path.of(
            System.getProperty("java.io.tmpdir"), "agenda-wa-test-security/integration.key"));
  }

  @BeforeEach
  void clearTestData() throws Exception {
    mongo.remove(new Query(), Appointment.class);
    mongo.remove(new Query(), Callback.class);
    mongo.remove(new Query(), Client.class);
    mongo.remove(new Query(), User.class);
    mongo.remove(new Query(), CatalogItem.class);
    mongo.remove(new Query(), "metadati");
    User user =
        auth.register(
            new RegisterRequestDTO(
                "Anna", "Test", "integration@agenda.example", "Password di prova 2026"));
    ownerId = user.id();
    String token = auth.token(user);
    SecurityContextHolder.getContext()
        .setAuthentication(new JwtAuthenticationToken(decoder.decode(token), java.util.List.of()));
    var cookies = new java.net.CookieManager(null, java.net.CookiePolicy.ACCEPT_ALL);
    http = java.net.http.HttpClient.newBuilder().cookieHandler(cookies).build();
    var base = java.net.URI.create("http://127.0.0.1:" + port + "/api/");
    var response =
        http.send(
            java.net.http.HttpRequest.newBuilder(base.resolve("auth/csrf")).GET().build(),
            java.net.http.HttpResponse.BodyHandlers.ofString());
    csrf = org.bson.Document.parse(response.body()).getString("token");
    var session = new java.net.HttpCookie("agenda_session", token);
    session.setPath("/api");
    session.setVersion(0);
    cookies.getCookieStore().add(base, session);
  }

  private Client client() {
    return clientService.saveClient(null, new ClientRequestDTO("Giulia", "Rossi"));
  }

  private AppointmentRequestDTO appointment(Client c, String policy, boolean createCallback) {
    return new AppointmentRequestDTO(
        LocalDate.now(ZoneId.of("Europe/Rome")),
        c.id(),
        catalogs.list("esiti").stream()
            .filter(e -> e.nome().equals("Sottoscrizione"))
            .findFirst()
            .orElseThrow()
            .id(),
        policy,
        new BigDecimal("123.45"),
        "Note di prova",
        createCallback,
        null);
  }

  @Test
  void appointmentTextIndexIsIdempotentOwnerPrefixedAndUsedWithoutStemmingOrStopWords() {
    schema.ensureTextIndex();
    schema.ensureTextIndex();
    var indexes =
        mongo.getCollection("appuntamenti").listIndexes().into(new java.util.ArrayList<>());
    var textIndexes =
        indexes.stream().filter(index -> index.containsKey("textIndexVersion")).toList();
    assertEquals(1, textIndexes.size());
    var index = textIndexes.getFirst();
    assertEquals("appuntamento_testo", index.getString("name"));
    assertEquals("none", index.getString("default_language"));
    assertEquals(3, index.getInteger("textIndexVersion"));
    assertEquals("ownerId", index.get("key", org.bson.Document.class).keySet().iterator().next());
    assertEquals(1, index.get("key", org.bson.Document.class).getInteger("ownerId"));
    assertEquals(
        java.util.Set.of("cliente.nome", "cliente.cognome", "cliente.nomeLegale", "note"),
        index.get("weights", org.bson.Document.class).keySet());

    Client c = clientService.saveClient(null, new ClientRequestDTO("Marco", "Di Pietro"));
    var request = appointment(c, null, false);
    Appointment app =
        activities.saveAppointment(
            null,
            new AppointmentRequestDTO(
                request.data(),
                request.clientId(),
                request.esitoId(),
                null,
                request.premio(),
                "Rinnóvo caffè",
                false,
                null));
    assertEquals(
        1, activities.appointments(null, null, null, "DI pietro CAFFÈ rinnovo", 0, 20).total());
    assertEquals(0, activities.appointments(null, null, null, "di rinnovi", 0, 20).total());
    assertEquals(0, activities.appointments(null, null, null, "di rinn", 0, 20).total());
    assertEquals(1, activities.appointments(null, null, null, "di", 0, 20).total());

    var query =
        xyz.nardone.agenda_wa.servizio.repository.query.Queries.appointments(
                null, null, null, "di rinnovo")
            .addCriteria(
                org.springframework.data.mongodb.core.query.Criteria.where("ownerId").is(ownerId));
    var explain =
        mongo
            .getDb()
            .runCommand(
                new org.bson.Document(
                        "explain",
                        new org.bson.Document("find", "appuntamenti")
                            .append("filter", query.getQueryObject())
                            .append("sort", new org.bson.Document("data", -1).append("_id", 1)))
                    .append("verbosity", "executionStats"));
    var plan = explain.get("queryPlanner", org.bson.Document.class).toJson();
    assertTrue(plan.contains("TEXT_MATCH"), plan);
    assertTrue(plan.contains("appuntamento_testo"), plan);
    assertEquals(1, explain.get("executionStats", org.bson.Document.class).getInteger("nReturned"));
    assertThrows(
        com.mongodb.MongoCommandException.class,
        () ->
            mongo
                .getDb()
                .runCommand(
                    new org.bson.Document("find", "appuntamenti")
                        .append(
                            "filter",
                            new org.bson.Document(
                                "$text", new org.bson.Document("$search", "di")))));
  }

  @Test
  void prioritizesTodaysCallbacksBeforePaginationAndPreservesFiltersAndOtherOrdering() {
    Client c = client();
    LocalDate today = LocalDate.now(ZoneId.of("Europe/Rome"));
    LocalDate previous = today.minusDays(2), next = today.plusDays(2);
    for (int i = 0; i < 24; i++)
      mongo.insert(new Callback("undated-%02d".formatted(i), ownerId, null, previous, c.id(), ""));
    mongo.insert(new Callback("today-b", ownerId, today, previous, c.id(), ""));
    mongo.insert(new Callback("today-a", ownerId, today, previous, c.id(), ""));
    mongo.insert(new Callback("past", ownerId, previous, previous, c.id(), ""));
    mongo.insert(new Callback("future", ownerId, next, previous, c.id(), ""));
    User other =
        auth.register(
            new RegisterRequestDTO(
                "Altro", "Test", "callback-other@agenda.example", "Password di prova 2026"));
    mongo.insert(new Callback("foreign-today", other.id(), today, previous, "foreign-client", ""));

    var first = activities.callbacks(null, null, 0, 2);
    assertEquals(28, first.total());
    assertEquals(
        java.util.List.of("today-a", "today-b"),
        first.items().stream().map(ActivityDTO::id).toList());
    assertEquals(
        java.util.List.of("undated-00", "undated-01"),
        activities.callbacks(null, null, 1, 2).items().stream().map(ActivityDTO::id).toList());
    var expected = new java.util.ArrayList<String>(java.util.List.of("today-a", "today-b"));
    for (int i = 0; i < 24; i++) expected.add("undated-%02d".formatted(i));
    expected.addAll(java.util.List.of("past", "future"));
    assertEquals(
        expected,
        activities.callbacks(null, null, 0, 100).items().stream().map(ActivityDTO::id).toList());
    assertEquals(
        java.util.List.of("today-a", "today-b", "future"),
        activities.callbacks(today, next, 0, 20).items().stream().map(ActivityDTO::id).toList());
    assertEquals(
        java.util.List.of("past"),
        activities.callbacks(previous, previous, 0, 20).items().stream()
            .map(ActivityDTO::id)
            .toList());
    assertEquals(2, activities.callbacks(today, today, 0, 20).total());
    var withoutToday = new java.util.ArrayList<String>();
    for (int i = 0; i < 24; i++) withoutToday.add("undated-%02d".formatted(i));
    withoutToday.addAll(java.util.List.of("past", "today-a", "today-b", "future"));
    assertEquals(
        withoutToday,
        callbacksRepository.search(ownerId, null, null, today.plusDays(10), 0, 100).items().stream()
            .map(Callback::id)
            .toList());
    assertEquals(
        0,
        mongo
            .getCollection("callback")
            .countDocuments(
                new org.bson.Document(
                    "_callbackPriority", new org.bson.Document("$exists", true))));
  }

  @Test
  void normalizesCompoundNamesAndOrdersOnlyBySurnameWithoutDerivedFields() {
    Client compound =
        clientService.saveClient(
            null, new ClientRequestDTO("  MARCO   Giovanni ", " De   PIETRO "));
    assertEquals("marco giovanni", compound.nome());
    assertEquals("de pietro", compound.cognome());
    assertEquals(1, clientService.clients("Marco De Pietro", 0, 20).total());
    mongo.insert(new Client("a-tie", ownerId, "Zeno", "Bianchi"));
    mongo.insert(new Client("b-tie", ownerId, "Anna", "Bianchi"));
    Client company =
        clientService.saveClient(
            null, new ClientRequestDTO(ClientType.AZIENDA, null, null, "Costruzioni"));
    assertEquals("COSTRUZIONI", company.nomeLegale());
    assertEquals(
        java.util.List.of("a-tie", "b-tie", company.id(), compound.id()),
        clientService.clients(null, 0, 20).items().stream().map(ClientSummaryDTO::id).toList());
    assertEquals("b-tie", clientService.clients(null, 1, 1).items().getFirst().id());
    assertEquals(1, clientService.clients("costruZIONI", 0, 20).total());
    assertEquals(0, clientService.clients(".*", 0, 20).total());
    for (var doc : mongo.getCollection("clienti").find()) {
      assertFalse(doc.containsKey("chiave"));
      assertFalse(doc.containsKey("ordine"));
      assertFalse(doc.containsKey("_clientSort"));
    }
    Appointment app = activities.saveAppointment(null, appointment(compound, null, false));
    assertEquals("marco giovanni", app.cliente().nome());
    assertEquals("de pietro", app.cliente().cognome());
    assertEquals("de pietro marco giovanni", activities.appointment(app.id()).client());
  }

  @Test
  void normalizesLegacyCompaniesAndSnapshotsWithoutChangingOtherData() {
    var company =
        new org.bson.Document("_id", "legacy-company")
            .append("ownerId", ownerId)
            .append("tipo", "AZIENDA")
            .append("nomeLegale", "Costruzioni Àlfa")
            .append("chiave", "costruzioni alfa")
            .append("ordine", "costruzioni alfa")
            .append("extra", "conservato");
    mongo.getCollection("clienti").insertOne(company);
    var app =
        new org.bson.Document("_id", "legacy-company-appointment")
            .append("ownerId", ownerId)
            .append("clientId", "legacy-company")
            .append(
                "cliente",
                new org.bson.Document("tipo", "AZIENDA")
                    .append("nomeLegale", "Costruzioni storiche"))
            .append("esito", "Conservato")
            .append("polizza", "Conservata")
            .append("note", "Conservate");
    mongo.getCollection("appuntamenti").insertOne(app);
    assertEquals(1, clientSchema.migrate());
    assertEquals(1, schema.migrate());
    var expectedCompany = new org.bson.Document(company).append("nomeLegale", "COSTRUZIONI ÀLFA");
    expectedCompany.remove("chiave");
    expectedCompany.remove("ordine");
    assertEquals(
        expectedCompany,
        mongo
            .getCollection("clienti")
            .find(new org.bson.Document("_id", "legacy-company"))
            .first());
    assertEquals(
        new org.bson.Document(app)
            .append(
                "cliente",
                new org.bson.Document("tipo", "AZIENDA")
                    .append("nomeLegale", "COSTRUZIONI STORICHE")),
        mongo
            .getCollection("appuntamenti")
            .find(new org.bson.Document("_id", "legacy-company-appointment"))
            .first());
    assertEquals(0, clientSchema.migrate());
    assertEquals(0, schema.migrate());
  }

  @Test
  void companiesAreDenormalizedRenamedAndMixedAlphabeticallyWithPeople() {
    Client person = client();
    Client company =
        clientService.saveClient(
            null, new ClientRequestDTO(ClientType.AZIENDA, null, null, "  Àlfa Servizi S.r.l.  "));
    assertEquals("ÀLFA SERVIZI S.R.L.", company.displayName());
    assertNull(company.nome());
    assertNull(company.cognome());
    assertEquals(company.id(), clientService.clients(null, 0, 1).items().getFirst().id());
    assertEquals(person.id(), clientService.clients(null, 1, 1).items().getFirst().id());
    assertEquals(1, clientService.clients("alfa servizi", 0, 20).total());
    Appointment app = activities.saveAppointment(null, appointment(company, null, true));
    var before =
        mongo.getCollection("appuntamenti").find(new org.bson.Document("_id", app.id())).first();
    assertEquals(
        new org.bson.Document("tipo", "AZIENDA").append("nomeLegale", company.nomeLegale()),
        before.get("cliente"));
    assertEquals(company.nomeLegale(), activities.appointment(app.id()).client());
    assertEquals(
        company.nomeLegale(), activities.callbacks(null, null, 0, 20).items().getFirst().client());
    clientService.saveClient(
        company.id(), new ClientRequestDTO(ClientType.AZIENDA, null, null, "ZETA S.R.L."));
    assertEquals("ZETA S.R.L.", activities.appointment(app.id()).client());
    assertEquals(
        "ZETA S.R.L.", activities.callbacks(null, null, 0, 20).items().getFirst().client());
    assertEquals(person.id(), clientService.clients(null, 0, 20).items().getFirst().id());
    assertThrows(
        RuntimeException.class,
        () -> clientService.saveClient(company.id(), new ClientRequestDTO("Anna", "Test")));
    assertEquals(ClientType.AZIENDA, clientService.client(company.id()).tipo());
    mongo.remove(new Query(), Client.class);
    assertEquals("ZETA S.R.L.", activities.appointment(app.id()).client());
    assertEquals(0, schema.migrate());
  }

  @Test
  void clientTypeMigrationPreservesLegacyFieldsSnapshotsAndIsIdempotent() {
    var legacy =
        new org.bson.Document("_id", "legacy-person")
            .append("ownerId", ownerId)
            .append("nome", "Èva")
            .append("cognome", "Bianchi")
            .append("chiave", "eva bianchi")
            .append("extra", "conservato")
            .append("_class", "xyz.nardone.agenda_wa.servizio.domain.Client");
    mongo.getCollection("clienti").insertOne(legacy);
    var details =
        new org.bson.Document("nome", "Nome storico").append("cognome", "Cognome storico");
    var app =
        new org.bson.Document("_id", "legacy-snapshot")
            .append("ownerId", ownerId)
            .append("clientId", "legacy-person")
            .append("cliente", details)
            .append("esito", "Storico")
            .append("data", "2026-10-03")
            .append("polizza", "Conservata")
            .append("note", "Storico");
    mongo.getCollection("appuntamenti").insertOne(app);
    assertEquals(1, clientSchema.migrate());
    assertEquals(1, schema.migrate());
    var expectedClient =
        new org.bson.Document(legacy)
            .append("tipo", "PERSONA_FISICA")
            .append("nome", "èva")
            .append("cognome", "bianchi");
    expectedClient.remove("chiave");
    assertEquals(
        expectedClient,
        mongo.getCollection("clienti").find(new org.bson.Document("_id", "legacy-person")).first());
    var expected =
        new org.bson.Document(app)
            .append(
                "cliente",
                new org.bson.Document(details)
                    .append("tipo", "PERSONA_FISICA")
                    .append("nome", "nome storico")
                    .append("cognome", "cognome storico"));
    assertEquals(
        expected,
        mongo
            .getCollection("appuntamenti")
            .find(new org.bson.Document("_id", "legacy-snapshot"))
            .first());
    assertEquals(0, clientSchema.migrate());
    assertEquals(0, schema.migrate());
  }

  @Test
  void httpValidatesIdentityAndRejectsBothDirectionsOfTypeChange() throws Exception {
    for (String body :
        java.util.List.of(
            "{\"nome\":\"Anna\",\"cognome\":\"Test\"}",
            "{\"tipo\":\"ALTRO\"}",
            "{\"tipo\":\"AZIENDA\",\"nomeLegale\":\" \"}",
            "{\"tipo\":\"AZIENDA\",\"nomeLegale\":\"Alfa\",\"nome\":\"Anna\"}",
            "{\"tipo\":\"PERSONA_FISICA\",\"nome\":\"Anna\",\"cognome\":\"Test\",\"nomeLegale\":\"Alfa\"}",
            "{\"tipo\":\"PERSONA_FISICA\",\"nome\":\"Anna\"}",
            new org.bson.Document("tipo", "AZIENDA")
                .append("nomeLegale", "A".repeat(151))
                .toJson())) {
      var response =
          http.send(
              request(java.net.URI.create("http://127.0.0.1:" + port + "/api/clienti"))
                  .header("Content-Type", "application/json")
                  .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                  .build(),
              java.net.http.HttpResponse.BodyHandlers.ofString());
      assertEquals(400, response.statusCode(), body);
    }
    assertEquals(0, clientService.clients(null, 0, 20).total());
    var companyBody = "{\"tipo\":\"AZIENDA\",\"nomeLegale\":\"Alfa S.r.l.\"}";
    var company = createHttp("clienti", companyBody);
    Client person = client();
    for (var entry :
        java.util.Map.of(
                company.getString("id"),
                "{\"tipo\":\"PERSONA_FISICA\",\"nome\":\"Anna\",\"cognome\":\"Test\"}",
                person.id(),
                companyBody)
            .entrySet()) {
      var response =
          http.send(
              request(
                      java.net.URI.create(
                          "http://127.0.0.1:" + port + "/api/clienti/" + entry.getKey()))
                  .header("Content-Type", "application/json")
                  .PUT(java.net.http.HttpRequest.BodyPublishers.ofString(entry.getValue()))
                  .build(),
              java.net.http.HttpResponse.BodyHandlers.ofString());
      assertEquals(400, response.statusCode(), response.body());
    }
    assertEquals(2, clientService.clients(null, 0, 20).total());
    assertEquals(ClientType.PERSONA_FISICA, clientService.client(person.id()).tipo());
  }

  @Test
  void appointmentStoresClientDetailsAndCatalogNamesWithoutCatalogIds() {
    Client c = client();
    CatalogItem policy = catalogs.save("polizze", null, new CatalogRequestDTO("Polizza test"));
    Appointment app = activities.saveAppointment(null, appointment(c, policy.id(), false));
    var document =
        mongo.getCollection("appuntamenti").find(new org.bson.Document("_id", app.id())).first();
    assertEquals(
        new org.bson.Document("tipo", "PERSONA_FISICA")
            .append("nome", "giulia")
            .append("cognome", "rossi"),
        document.get("cliente"));
    assertEquals(c.id(), document.getString("clientId"));
    assertEquals("Sottoscrizione", document.getString("esito"));
    assertEquals("Polizza test", document.getString("polizza"));
    assertFalse(document.containsKey("esitoId"));
    assertFalse(document.containsKey("polizzaId"));
    // Display and unfiltered totals are independent of the source collections.
    mongo.remove(new Query(), Client.class);
    mongo.remove(new Query(), CatalogItem.class);
    var view = activities.appointment(app.id());
    assertEquals("rossi giulia", view.client());
    assertEquals("Sottoscrizione", view.esito());
    assertEquals("Polizza test", view.polizza());
    assertNull(view.esitoId());
    assertEquals(1, overview.summary(null, null, null, null).sottoscrizioni());
  }

  @Test
  void outcomeRenameUpdatesPersistenceFiltersAndTotals() {
    Client c = client();
    var request = appointment(c, null, false);
    Appointment app = activities.saveAppointment(null, request);
    catalogs.save("esiti", request.esitoId(), new CatalogRequestDTO("Esito aggiornato"));
    assertEquals(
        "Esito aggiornato",
        mongo
            .getCollection("appuntamenti")
            .find(new org.bson.Document("_id", app.id()))
            .first()
            .getString("esito"));
    assertEquals(request.esitoId(), activities.appointment(app.id()).esitoId());
    assertEquals(1, activities.appointments(null, null, request.esitoId(), null, 0, 20).total());
    assertEquals(
        1, overview.summary(null, null, request.esitoId(), null).esiti().get("Esito aggiornato"));
    assertThrows(RuntimeException.class, () -> catalogs.delete("esiti", request.esitoId()));
  }

  @Test
  void migrationPreservesOtherFieldsAndRejectsForeignReferencesAtomically() {
    Client c = client();
    CatalogItem policy = catalogs.save("polizze", null, new CatalogRequestDTO("Polizza migrata"));
    var legacy =
        new org.bson.Document("_id", "legacy")
            .append("ownerId", ownerId)
            .append("clientId", c.id())
            .append("data", "2026-10-03")
            .append("esitoId", appointment(c, null, false).esitoId())
            .append("polizzaId", policy.id())
            .append("premio", new org.bson.types.Decimal128(new BigDecimal("321.09")))
            .append("note", "Storico")
            .append("extra", "conservato")
            .append("_class", "xyz.nardone.agenda_wa.servizio.domain.Appointment");
    mongo.getCollection("appuntamenti").insertOne(legacy);
    var foreign =
        new org.bson.Document(legacy).append("_id", "foreign").append("ownerId", "another-owner");
    mongo.getCollection("appuntamenti").insertOne(foreign);
    assertThrows(RuntimeException.class, () -> schema.migrate());
    assertEquals(
        legacy,
        mongo.getCollection("appuntamenti").find(new org.bson.Document("_id", "legacy")).first());
    mongo.getCollection("appuntamenti").deleteOne(new org.bson.Document("_id", "foreign"));
    assertEquals(1, schema.migrate());
    var expected = new org.bson.Document(legacy);
    expected.remove("esitoId");
    expected.remove("polizzaId");
    expected
        .append(
            "cliente",
            new org.bson.Document("tipo", "PERSONA_FISICA")
                .append("nome", c.nome())
                .append("cognome", c.cognome()))
        .append("esito", "Sottoscrizione")
        .append("polizza", "Polizza migrata");
    assertEquals(
        expected,
        mongo.getCollection("appuntamenti").find(new org.bson.Document("_id", "legacy")).first());
    assertEquals(0, schema.migrate());
  }

  @Test
  void storesDecimalsDatesAndAnUndatedCallbackAtomically() {
    Client c = client();
    Appointment app = activities.saveAppointment(null, appointment(c, null, true));
    var saved = activities.appointment(app.id());
    assertEquals(new BigDecimal("123.45"), saved.premio());
    assertEquals("rossi giulia", saved.client());
    assertEquals(1, mongo.count(new Query(), Callback.class));
    assertNull(activities.callbacks(null, null, 0, 20).items().getFirst().data());
    var summary = overview.summary(app.data(), app.data(), null, null);
    assertEquals(1, summary.appuntamenti());
    assertEquals(1, summary.sottoscrizioni());
    assertEquals(new BigDecimal("123.45"), summary.premi());
    assertEquals(
        0, activities.appointments(app.data().plusDays(1), null, null, null, 0, 20).total());
    assertEquals(
        app.data().toString(),
        mongo.getCollection("appuntamenti").find().first().getString("data"));
  }

  @Test
  void invalidCatalogRollsBackAllRelatedWrites() {
    Client c = client();
    Object revisionBefore = mongo.getCollection("clienti").find().first().get("revision");
    assertThrows(
        RuntimeException.class,
        () -> activities.saveAppointment(null, appointment(c, "missing", true)));
    assertEquals(0, mongo.count(new Query(), Appointment.class));
    assertEquals(0, mongo.count(new Query(), Callback.class));
    assertEquals(revisionBefore, mongo.getCollection("clienti").find().first().get("revision"));
  }

  @Test
  void clientsAreIndependentAndHomonymsHaveSeparateIds() {
    Client first = client();
    Client second = clientService.saveClient(null, new ClientRequestDTO("Giulia", "Rossi"));
    assertNotEquals(first.id(), second.id());
    assertEquals(2, clientService.clients("rossi", 0, 20).total());
    var document = mongo.getCollection("clienti").find().first();
    assertFalse(document.containsKey("subagentId"));
    assertEquals("giulia", document.getString("nome"));
    assertEquals("rossi", document.getString("cognome"));
    assertFalse(document.containsKey("chiave"));
    assertFalse(document.containsKey("ordine"));
  }

  @Test
  void appointmentsAndCallbacksRequireOnlyAClient() {
    Client c = client();
    Appointment app = activities.saveAppointment(null, appointment(c, null, true));
    assertEquals(c.id(), activities.appointment(app.id()).clientId());
    Callback callback = mongo.findAll(Callback.class).getFirst();
    assertEquals(c.id(), activities.callback(callback.id()).clientId());
    Callback manual =
        activities.saveCallback(
            null, new CallbackRequestDTO(null, app.data(), c.id(), "Da pianificare"));
    assertEquals(c.id(), manual.clientId());
    assertEquals(2, mongo.count(new Query(), Callback.class));
  }

  @Test
  void renamingAClientPreservesActivitiesAndUpdatesTheirLabels() {
    Client c = client();
    Appointment first = activities.saveAppointment(null, appointment(c, null, true));
    Appointment second = activities.saveAppointment(null, appointment(c, null, false));
    clientService.saveClient(c.id(), new ClientRequestDTO("Giulia", "Verdi"));
    assertEquals(c.id(), activities.appointment(first.id()).clientId());
    assertEquals(c.id(), activities.appointment(second.id()).clientId());
    assertEquals("verdi giulia", activities.appointment(first.id()).client());
    assertEquals(
        "verdi giulia", activities.callbacks(null, null, 0, 20).items().getFirst().client());
    assertEquals(new BigDecimal("246.90"), overview.summary(null, null, null, null).premi());
  }

  @Test
  void editingAnAppointmentChangesItsClientAndPreservesExistingCallbacks() {
    Client first = client();
    Client second = clientService.saveClient(null, new ClientRequestDTO("Luca", "Bianchi"));
    Appointment app = activities.saveAppointment(null, appointment(first, null, true));
    activities.saveAppointment(app.id(), appointment(second, null, false));
    assertEquals(second.id(), activities.appointment(app.id()).clientId());
    assertEquals(first.id(), mongo.findAll(Callback.class).getFirst().clientId());
    assertEquals(1, mongo.count(new Query(), Appointment.class));
    assertEquals(1, mongo.count(new Query(), Callback.class));
  }

  @Test
  void missingClientRollsBackAllWrites() {
    Client missing = new Client("missing", ownerId, "Giulia", "Rossi");
    assertThrows(
        RuntimeException.class,
        () -> activities.saveAppointment(null, appointment(missing, null, true)));
    assertEquals(0, mongo.count(new Query(), Appointment.class));
    assertEquals(0, mongo.count(new Query(), Callback.class));
  }

  @Test
  void deletingAppointmentsPreservesCallbacksButDeletingClientsRemovesThem() {
    Client c = client();
    Appointment app = activities.saveAppointment(null, appointment(c, null, true));
    activities.deleteAppointment(app.id());
    assertEquals(1, mongo.count(new Query(), Callback.class));
    activities.saveAppointment(null, appointment(c, null, false));
    clientService.deleteClient(c.id());
    assertEquals(0, mongo.count(new Query(), Appointment.class));
    assertEquals(0, mongo.count(new Query(), Callback.class));
  }

  @Test
  void catalogRenameUpdatesDisplayedHistoryAndUsedValuesCannotBeDeleted() {
    Client c = client();
    CatalogItem policy = catalogs.save("polizze", null, new CatalogRequestDTO("Polizza test"));
    Appointment app = activities.saveAppointment(null, appointment(c, policy.id(), false));
    catalogs.save("polizze", policy.id(), new CatalogRequestDTO("Polizza aggiornata"));
    assertEquals("Polizza aggiornata", activities.appointment(app.id()).polizza());
    assertThrows(RuntimeException.class, () -> catalogs.delete("polizze", policy.id()));
    activities.deleteSelectedAppointments(
        new AppointmentSelectionDTO(AppointmentPeriod.TUTTE, null, null, null, null), 1);
    catalogs.delete("polizze", policy.id());
  }

  @Test
  void resetClientsRemovesClientsAndActivityAndKeepsCatalogs() {
    Client c = client();
    activities.saveAppointment(null, appointment(c, null, true));
    clientService.clearClients();
    assertEquals(0, mongo.count(new Query(), Client.class));
    assertEquals(0, mongo.count(new Query(), Appointment.class));
    assertEquals(0, mongo.count(new Query(), Callback.class));
    assertEquals(28, mongo.count(new Query(), CatalogItem.class));
    assertEquals(1, mongo.count(new Query(), User.class));
  }

  private java.net.http.HttpResponse<String> selectionHttp(String query, boolean delete)
      throws Exception {
    var request =
        request(
            java.net.URI.create(
                "http://127.0.0.1:"
                    + port
                    + "/api/appuntamenti"
                    + (delete ? "?" : "/selezione?")
                    + query));
    return http.send(
        (delete ? request.DELETE() : request.GET()).build(),
        java.net.http.HttpResponse.BodyHandlers.ofString());
  }

  private Appointment datedAppointment(Client c, String date, String outcome) {
    return activities.saveAppointment(
        null,
        new AppointmentRequestDTO(
            LocalDate.parse(date),
            c.id(),
            outcome,
            null,
            BigDecimal.ZERO,
            "Selezione di prova",
            true,
            null));
  }

  @Test
  void filteredDeletionUsesDateBoundariesAndOutcomeAndPreservesReferences() throws Exception {
    Client c = client();
    String firstOutcome = catalogs.list("esiti").get(0).id();
    String secondOutcome = catalogs.list("esiti").get(1).id();
    for (String day : java.util.List.of("2026-10-01", "2026-10-03", "2026-10-05")) {
      datedAppointment(c, day, firstOutcome);
      datedAppointment(c, day, secondOutcome);
    }
    var cases =
        java.util.Map.of(
            "periodo=TUTTE",
            6,
            "periodo=INTERVALLO&dal=2026-10-03&al=2026-10-05",
            4,
            "periodo=PRIMA&data=2026-10-03",
            2,
            "periodo=DOPO&data=2026-10-03",
            2,
            "periodo=TUTTE&esitoId=" + firstOutcome,
            3,
            "periodo=INTERVALLO&dal=2026-10-01&al=2026-10-03&esitoId=" + firstOutcome,
            2);
    for (var entry : cases.entrySet()) {
      var response = selectionHttp(entry.getKey(), false);
      assertEquals(200, response.statusCode(), response.body());
      var body = org.bson.Document.parse(response.body());
      assertEquals(entry.getValue().longValue(), ((Number) body.get("total")).longValue());
      assertEquals(
          Math.min(5, entry.getValue()), body.getList("items", org.bson.Document.class).size());
    }
    for (String invalid :
        java.util.List.of(
            "",
            "periodo=WRONG",
            "periodo=PRIMA",
            "periodo=DOPO&data=bad",
            "periodo=INTERVALLO&dal=2026-10-05&al=2026-10-01",
            "periodo=INTERVALLO&dal=2026-10-01",
            "periodo=TUTTE&data=2026-10-03",
            "periodo=TUTTE&esitoId=missing")) {
      assertEquals(400, selectionHttp(invalid, false).statusCode(), invalid);
      assertEquals(400, selectionHttp(invalid + "&attesi=6", true).statusCode(), invalid);
      assertEquals(6, mongo.count(new Query(), Appointment.class));
    }
    assertEquals(400, selectionHttp("periodo=TUTTE", true).statusCode());
    assertEquals(400, selectionHttp("periodo=TUTTE&attesi=-1", true).statusCode());
    var deleted =
        selectionHttp("periodo=PRIMA&data=2026-10-03&esitoId=" + firstOutcome + "&attesi=1", true);
    assertEquals(200, deleted.statusCode(), deleted.body());
    assertEquals(1, org.bson.Document.parse(deleted.body()).getInteger("eliminati"));
    assertEquals(5, mongo.count(new Query(), Appointment.class));
    assertEquals(
        1,
        activities
            .appointments(
                LocalDate.parse("2026-10-01"),
                LocalDate.parse("2026-10-01"),
                secondOutcome,
                null,
                0,
                20)
            .total());
    assertEquals(
        2,
        activities
            .appointments(
                LocalDate.parse("2026-10-03"), LocalDate.parse("2026-10-03"), null, null, 0, 20)
            .total());
    assertEquals(6, mongo.count(new Query(), Callback.class));
    assertEquals(1, mongo.count(new Query(), Client.class));
  }

  @Test
  void deletionRejectsStalePreviewAndDeletesEveryMatchBeyondThePreview() throws Exception {
    Client c = client();
    String outcome = catalogs.list("esiti").getFirst().id();
    for (int i = 0; i < 25; i++) datedAppointment(c, "2026-10-01", outcome);
    datedAppointment(c, "2026-10-03", outcome);
    String filter = "periodo=PRIMA&data=2026-10-03&esitoId=" + outcome;
    var preview = org.bson.Document.parse(selectionHttp(filter, false).body());
    assertEquals(25, preview.getInteger("total"));
    assertEquals(5, preview.getList("items", org.bson.Document.class).size());
    datedAppointment(c, "2026-10-01", outcome);
    var stale = selectionHttp(filter + "&attesi=25", true);
    assertEquals(409, stale.statusCode(), stale.body());
    assertEquals(27, mongo.count(new Query(), Appointment.class));
    var deleted = selectionHttp(filter + "&attesi=26", true);
    assertEquals(200, deleted.statusCode(), deleted.body());
    assertEquals(26, org.bson.Document.parse(deleted.body()).getInteger("eliminati"));
    assertEquals(1, mongo.count(new Query(), Appointment.class));
    assertEquals(LocalDate.parse("2026-10-03"), mongo.findAll(Appointment.class).getFirst().data());
    assertEquals(27, mongo.count(new Query(), Callback.class));
    var noMatches = selectionHttp(filter + "&attesi=0", true);
    assertEquals(200, noMatches.statusCode(), noMatches.body());
    assertEquals(0, org.bson.Document.parse(noMatches.body()).getInteger("eliminati"));
  }

  @Test
  void httpValidationErrorsAndForeignOriginAllowed() throws Exception {
    java.net.URI base = java.net.URI.create("http://127.0.0.1:" + port);
    var invalid =
        request(base.resolve("/api/clienti"))
            .header("Content-Type", "application/json")
            .POST(
                java.net.http.HttpRequest.BodyPublishers.ofString(
                    "{\"tipo\":\"PERSONA_FISICA\",\"nome\":\"\",\"cognome\":\"Rossi\"}"))
            .build();
    assertEquals(
        400, http.send(invalid, java.net.http.HttpResponse.BodyHandlers.ofString()).statusCode());
    var foreign =
        request(base.resolve("/api/clienti")).header("Origin", "https://example.org").GET().build();
    assertEquals(
        200, http.send(foreign, java.net.http.HttpResponse.BodyHandlers.ofString()).statusCode());
    var removed = request(base.resolve("/api/appuntamenti/missing")).GET().build();
    assertEquals(
        404, http.send(removed, java.net.http.HttpResponse.BodyHandlers.ofString()).statusCode());
    var malformed = request(base.resolve("/api/appuntamenti?dal=not-a-date")).GET().build();
    assertEquals(
        400, http.send(malformed, java.net.http.HttpResponse.BodyHandlers.ofString()).statusCode());
    var index =
        http.send(
            request(base.resolve("/")).GET().build(),
            java.net.http.HttpResponse.BodyHandlers.ofString());
    assertEquals(200, index.statusCode());
    assertFalse(index.body().contains("Subagenti"));
    assertTrue(index.body().contains("Agenda WA"));
    assertFalse(index.body().contains("javafx"));
  }

  @Test
  void httpCallbackLifecycleUsesTheRenamedApiAndOverviewFields() throws Exception {
    Client client = client();
    String outcome = catalogs.list("esiti").getFirst().id();
    String today = LocalDate.now(ZoneId.of("Europe/Rome")).toString();
    var base = java.net.URI.create("http://127.0.0.1:" + port + "/api/");
    String appointmentBody =
        """
        {"data":"%s","clientId":"%s","esitoId":"%s","premio":0,
         "creaCallback":true,"dataCallback":"%s"}
        """
            .formatted(today, client.id(), outcome, today);
    var created =
        http.send(
            request(base.resolve("appuntamenti"))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(appointmentBody))
                .build(),
            java.net.http.HttpResponse.BodyHandlers.ofString());
    assertEquals(201, created.statusCode(), created.body());
    assertEquals(1, mongo.getCollection("callback").countDocuments());
    Callback callback = mongo.findAll(Callback.class).getFirst();
    assertEquals(LocalDate.parse(today), callback.data());

    var listing =
        http.send(
            request(base.resolve("callback")).GET().build(),
            java.net.http.HttpResponse.BodyHandlers.ofString());
    assertEquals(200, listing.statusCode(), listing.body());
    assertEquals(1, ((Number) org.bson.Document.parse(listing.body()).get("total")).intValue());
    var dashboard =
        http.send(
            request(base.resolve("panoramica")).GET().build(),
            java.net.http.HttpResponse.BodyHandlers.ofString());
    var overviewJson = org.bson.Document.parse(dashboard.body());
    assertEquals(1, ((Number) overviewJson.get("callbackOggi")).intValue());
    assertEquals(1, overviewJson.getList("callback", org.bson.Document.class).size());
    var navigation =
        http.send(
            request(base.resolve("navigazione")).GET().build(),
            java.net.http.HttpResponse.BodyHandlers.ofString());
    assertEquals(
        1, ((Number) org.bson.Document.parse(navigation.body()).get("callbackOggi")).intValue());

    String updatedBody =
        """
        {"data":null,"dataAppuntamento":"%s","clientId":"%s","note":"Da pianificare"}
        """
            .formatted(today, client.id());
    var updated =
        http.send(
            request(base.resolve("callback/" + callback.id()))
                .header("Content-Type", "application/json")
                .PUT(java.net.http.HttpRequest.BodyPublishers.ofString(updatedBody))
                .build(),
            java.net.http.HttpResponse.BodyHandlers.ofString());
    assertEquals(200, updated.statusCode(), updated.body());
    assertNull(activities.callback(callback.id()).data());
    assertEquals("Da pianificare", activities.callback(callback.id()).note());
    var deleted =
        http.send(
            request(base.resolve("callback/" + callback.id())).DELETE().build(),
            java.net.http.HttpResponse.BodyHandlers.ofString());
    assertEquals(204, deleted.statusCode());
    assertEquals(0, mongo.getCollection("callback").countDocuments());
    assertEquals(1, mongo.count(new Query(), Appointment.class));
  }

  @Test
  void httpCreatesClientsAndAppointmentsWithCallbacks() throws Exception {
    var base = java.net.URI.create("http://127.0.0.1:" + port);
    var createClient =
        request(base.resolve("/api/clienti"))
            .header("Content-Type", "application/json")
            .POST(
                java.net.http.HttpRequest.BodyPublishers.ofString(
                    "{\"tipo\":\"PERSONA_FISICA\",\"nome\":\"Luca\",\"cognome\":\"Bianchi\"}"))
            .build();
    var response = http.send(createClient, java.net.http.HttpResponse.BodyHandlers.ofString());
    assertEquals(201, response.statusCode());
    assertFalse(response.body().contains("subagent"));
    String clientId = clientService.clients("bianchi", 0, 20).items().getFirst().id();
    String outcome = catalogs.list("esiti").getFirst().id();
    String body =
        """
        {"data":"2026-10-03","clientId":"%s","esitoId":"%s","premio":10.25,
         "creaCallback":true,"dataCallback":null}
        """
            .formatted(clientId, outcome);
    var request =
        request(base.resolve("/api/appuntamenti"))
            .header("Content-Type", "application/json")
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
            .build();
    var created = http.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
    assertEquals(201, created.statusCode(), created.body());
    assertEquals(1, mongo.count(new Query(), Appointment.class));
  }

  @Test
  void readsExistingMongoDiscriminatorsAndPreservesThemWhenSaving() {
    String prefix = "xyz.nardone.agenda_wa.servizio.domain.";
    String outcome = catalogs.list("esiti").getFirst().id();
    mongo
        .getCollection("clienti")
        .insertOne(
            new org.bson.Document("_id", "existing-client")
                .append("ownerId", ownerId)
                .append("nome", "Giulia")
                .append("cognome", "Storico")
                .append("chiave", "giulia storico")
                .append("_class", prefix + "Client"));
    mongo
        .getCollection("appuntamenti")
        .insertOne(
            new org.bson.Document("_id", "existing-appointment")
                .append("ownerId", ownerId)
                .append("data", "2026-10-03")
                .append("clientId", "existing-client")
                .append("esitoId", outcome)
                .append("premio", new org.bson.types.Decimal128(new BigDecimal("321.09")))
                .append("note", "Storico")
                .append("_class", prefix + "Appointment"));
    mongo
        .getCollection("callback")
        .insertOne(
            new org.bson.Document("_id", "existing-callback")
                .append("ownerId", ownerId)
                .append("dataAppuntamento", "2026-10-03")
                .append("clientId", "existing-client")
                .append("note", "Da pianificare")
                .append("_class", prefix + "Callback"));
    mongo
        .getCollection("cataloghi")
        .insertOne(
            new org.bson.Document("_id", "existing-policy")
                .append("ownerId", ownerId)
                .append("tipo", "polizze")
                .append("nome", "Polizza storica")
                .append("chiave", "polizza storica")
                .append("_class", prefix + "CatalogItem"));
    assertEquals(1, clientSchema.migrate());
    assertEquals(0, clientSchema.migrate());
    assertEquals(1, schema.migrate());
    assertEquals(0, schema.migrate());
    assertEquals("storico giulia", activities.appointment("existing-appointment").client());
    assertEquals(new BigDecimal("321.09"), activities.appointment("existing-appointment").premio());
    assertEquals("Polizza storica", catalogs.item("polizze", "existing-policy").nome());
    assertEquals(
        ownerId,
        auth.login(new LoginRequestDTO("integration@agenda.example", "Password di prova 2026"))
            .id());
    clientService.saveClient("existing-client", new ClientRequestDTO("Giulia", "Aggiornato"));
    activities.saveAppointment(
        "existing-appointment",
        new AppointmentRequestDTO(
            LocalDate.parse("2026-10-03"),
            "existing-client",
            outcome,
            "existing-policy",
            new BigDecimal("321.09"),
            "Aggiornato",
            false,
            null));
    activities.saveCallback(
        "existing-callback",
        new CallbackRequestDTO(
            null, LocalDate.parse("2026-10-03"), "existing-client", "Aggiornato"));
    catalogs.save("polizze", "existing-policy", new CatalogRequestDTO("Polizza aggiornata"));
    var types =
        java.util.Map.of(
            "clienti",
            "Client",
            "appuntamenti",
            "Appointment",
            "callback",
            "Callback",
            "cataloghi",
            "CatalogItem",
            "utenti",
            "User");
    types.forEach(
        (collection, type) ->
            mongo
                .getCollection(collection)
                .find()
                .forEach(
                    document ->
                        assertEquals(prefix + type, document.getString("_class"), collection)));
  }

  private org.bson.Document createHttp(String path, String json) throws Exception {
    var response =
        http.send(
            request(java.net.URI.create("http://127.0.0.1:" + port + "/api/" + path))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json))
                .build(),
            java.net.http.HttpResponse.BodyHandlers.ofString());
    assertEquals(201, response.statusCode(), response.body());
    return org.bson.Document.parse(response.body());
  }

  @Test
  void removedResourceIsUnavailableAndActivitiesHaveNoObsoleteReferences() throws Exception {
    Client c = client();
    activities.saveAppointment(null, appointment(c, null, true));
    var base = java.net.URI.create("http://127.0.0.1:" + port + "/api/");
    for (String method : java.util.List.of("GET", "POST", "PUT", "DELETE")) {
      String path =
          method.equals("GET") || method.equals("POST") ? "subagenti" : "subagenti/missing";
      var response =
          http.send(
              request(base.resolve(path))
                  .header("Content-Type", "application/json")
                  .method(
                      method,
                      java.net.http.HttpRequest.BodyPublishers.ofString(
                          "{\"tipo\":\"PERSONA_FISICA\",\"nome\":\"Eliminato\",\"cognome\":\"Test\"}"))
                  .build(),
              java.net.http.HttpResponse.BodyHandlers.ofString());
      assertEquals(404, response.statusCode(), method + " " + response.body());
    }
    assertFalse(mongo.getCollectionNames().contains("subagenti"));
    for (String collection : java.util.List.of("clienti", "appuntamenti", "callback")) {
      mongo
          .getCollection(collection)
          .find()
          .forEach(
              document -> {
                assertFalse(document.containsKey("subagentId"), collection);
                assertFalse(document.containsKey("subagent"), collection);
              });
    }
    for (String path : java.util.List.of("panoramica", "appuntamenti", "callback", "clienti")) {
      var response =
          http.send(
              request(base.resolve(path)).GET().build(),
              java.net.http.HttpResponse.BodyHandlers.ofString());
      assertEquals(200, response.statusCode(), path);
      assertFalse(response.body().toLowerCase(java.util.Locale.ROOT).contains("subagent"), path);
    }
  }

  @Test
  void dtoResponsesExposeClientTypeAndPreserveActivityFieldSets() throws Exception {
    var client =
        createHttp(
            "clienti", "{\"tipo\":\"PERSONA_FISICA\",\"nome\":\"Giulia\",\"cognome\":\"Rossi\"}");
    var catalog = createHttp("cataloghi/polizze", "{\"nome\":\"Polizza API\"}");
    assertEquals(java.util.Set.of("id", "tipo", "nome", "cognome", "nomeLegale", "telefono"), client.keySet());
    assertEquals(java.util.Set.of("id", "tipo", "nome", "chiave"), catalog.keySet());
    var appointment =
        createHttp(
            "appuntamenti",
            new org.bson.Document("data", "2026-10-03")
                .append("clientId", client.getString("id"))
                .append("esitoId", catalogs.list("esiti").getFirst().id())
                .append("premio", 12.50)
                .append("creaCallback", false)
                .toJson());
    assertEquals(
        java.util.Set.of("id", "data", "clientId", "esitoId", "polizzaId", "premio", "note"),
        appointment.keySet());
    var callback =
        createHttp(
            "callback",
            new org.bson.Document("dataAppuntamento", "2026-10-03")
                .append("clientId", client.getString("id"))
                .toJson());
    assertEquals(
        java.util.Set.of("id", "data", "dataAppuntamento", "clientId", "note"), callback.keySet());
    var listResponse =
        http.send(
            request(java.net.URI.create("http://127.0.0.1:" + port + "/api/clienti")).GET().build(),
            java.net.http.HttpResponse.BodyHandlers.ofString());
    var page = org.bson.Document.parse(listResponse.body());
    assertEquals(java.util.Set.of("items", "total", "page", "size"), page.keySet());
    assertEquals(
        java.util.Set.of("id", "tipo", "nome", "cognome", "nomeLegale", "telefono"),
        page.getList("items", org.bson.Document.class).getFirst().keySet());
    var detailResponse =
        http.send(
            request(
                    java.net.URI.create(
                        "http://127.0.0.1:"
                            + port
                            + "/api/appuntamenti/"
                            + appointment.getString("id")))
                .GET()
                .build(),
            java.net.http.HttpResponse.BodyHandlers.ofString());
    assertEquals(
        java.util.Set.of(
            "id",
            "data",
            "dataAppuntamento",
            "clientId",
            "client",
            "tipoCliente",
            "esitoId",
            "esito",
            "polizzaId",
            "polizza",
            "premio",
            "note"),
        org.bson.Document.parse(detailResponse.body()).keySet());
  }

  @Autowired xyz.nardone.agenda_wa.servizio.repository.ClientHistorySchemaRepository historySchema;

  private java.util.List<org.bson.Document> storedHistory(String clientId) {
    return mongo.getCollection("clienti").find(new org.bson.Document("_id", clientId)).first()
        .getList("appuntamenti", org.bson.Document.class);
  }

  @Test
  void embeddedHistoryIsPartialSortedPaginatedAndUpdatedAcrossEveryWrite() {
    Client c = client();
    assertEquals("", clientService.client(c.id()).telefono());
    assertTrue(storedHistory(c.id()).isEmpty());
    String outcome = catalogs.list("esiti").getFirst().id();
    var first = datedAppointment(c, "2026-10-01", outcome);
    var last = datedAppointment(c, "2026-10-03", outcome);
    var sameDay = datedAppointment(c, "2026-10-03", outcome);
    var ids = java.util.stream.Stream.of(last.id(), sameDay.id()).sorted().toList();
    assertEquals(java.util.List.of(ids.getFirst(), ids.getLast(), first.id()),
        clientService.history(c.id(), 0, 20).items().stream().map(ClientAppointmentDTO::id).toList());
    for (var embedded : storedHistory(c.id()))
      assertEquals(java.util.Set.of("id", "data", "esito"), embedded.keySet());
    var page = clientService.history(c.id(), 1, 1);
    assertEquals(3, page.total());
    assertEquals(ids.getLast(), page.items().getFirst().id());
    assertThrows(RuntimeException.class, () -> clientService.history(c.id(), -1, 20));
    assertThrows(RuntimeException.class, () -> clientService.history(c.id(), 0, 101));
    clientService.saveClient(c.id(), new ClientRequestDTO(ClientType.PERSONA_FISICA,
        "Giulia", "Verdi", null, "  +39 0123 456789  "));
    assertEquals("+39 0123 456789", clientService.client(c.id()).telefono());
    assertEquals(3, clientService.history(c.id(), 0, 20).total());
    catalogs.save("esiti", outcome, new CatalogRequestDTO("Esito rinominato"));
    assertTrue(clientService.history(c.id(), 0, 20).items().stream()
        .allMatch(a -> a.esito().equals("Esito rinominato")));
    var edited = activities.saveAppointment(first.id(), new AppointmentRequestDTO(
        LocalDate.parse("2026-10-05"), c.id(), outcome, null, BigDecimal.ONE,
        "Nota completa", false, null));
    assertEquals(edited.id(), clientService.history(c.id(), 0, 20).items().getFirst().id());
    assertEquals("Nota completa", activities.appointment(edited.id()).note());
    Client second = clientService.saveClient(null, new ClientRequestDTO("Luca", "Bianchi"));
    activities.saveAppointment(first.id(), appointment(second, null, false));
    assertEquals(2, clientService.history(c.id(), 0, 20).total());
    assertEquals(first.id(), clientService.history(second.id(), 0, 20).items().getFirst().id());
    activities.deleteAppointment(first.id());
    assertEquals(0, clientService.history(second.id(), 0, 20).total());
    assertThrows(RuntimeException.class, () -> activities.deleteSelectedAppointments(
        new AppointmentSelectionDTO(AppointmentPeriod.TUTTE, null, null, null, null), 99));
    assertEquals(2, clientService.history(c.id(), 0, 20).total());
    activities.deleteSelectedAppointments(
        new AppointmentSelectionDTO(AppointmentPeriod.INTERVALLO,
            LocalDate.parse("2026-10-03"), LocalDate.parse("2026-10-03"), null, null), 2);
    assertTrue(storedHistory(c.id()).isEmpty());
    assertEquals(3, mongo.count(new Query(), Callback.class));
  }

  @Test
  void embeddedHistoryBackfillIsIdempotentPreservesPhoneAndRejectsForeignReferencesAtomically() {
    Client c = client();
    var app = activities.saveAppointment(null, appointment(c, null, false));
    mongo.getCollection("clienti").updateOne(new org.bson.Document("_id", c.id()),
        new org.bson.Document("$unset", new org.bson.Document("telefono", "").append("appuntamenti", "")));
    assertTrue(historySchema.migrate() > 0);
    assertEquals("", clientService.client(c.id()).telefono());
    assertEquals(app.id(), clientService.history(c.id(), 0, 20).items().getFirst().id());
    assertEquals(0, historySchema.migrate());
    clientService.saveClient(c.id(), new ClientRequestDTO(ClientType.PERSONA_FISICA,
        "Giulia", "Rossi", null, "+39 0123"));
    assertEquals(0, historySchema.migrate());
    assertEquals("+39 0123", clientService.client(c.id()).telefono());
    var before = storedHistory(c.id());
    var foreign = new org.bson.Document(mongo.getCollection("appuntamenti").find().first())
        .append("_id", "foreign-history").append("ownerId", "other-owner");
    mongo.getCollection("appuntamenti").insertOne(foreign);
    assertThrows(RuntimeException.class, () -> historySchema.migrate());
    assertEquals(before, storedHistory(c.id()));
    assertEquals(2, mongo.count(new Query(), Appointment.class));
  }
}
