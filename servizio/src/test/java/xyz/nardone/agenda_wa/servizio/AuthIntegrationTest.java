package xyz.nardone.agenda_wa.servizio;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import org.bson.Document;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.*;
import xyz.nardone.agenda_wa.servizio.entity.*;
import xyz.nardone.agenda_wa.servizio.security.SecurityConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "AGENDA_TEST_MONGO_URI", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthIntegrationTest {
  static final String PASSWORD = "Solo una password di test 2026!";

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    String uri = System.getenv("AGENDA_TEST_MONGO_URI");
    if (!uri.matches("mongodb://[^/]+/agenda_wa_test_[a-zA-Z0-9_]+\\?.*"))
      throw new IllegalArgumentException("Usa un database isolato agenda_wa_test_*. ");
    registry.add("spring.mongodb.uri", () -> uri.replace("?", "_security?"));
    registry.add(
        "agenda.auth.key-file",
        () -> System.getProperty("java.io.tmpdir") + "/agenda-wa-test-security/auth.key");
  }

  @Value("${local.server.port}")
  int port;

  @Autowired MongoTemplate mongo;
  @Autowired JwtEncoder encoder;
  @Autowired PasswordEncoder passwords;

  @BeforeEach
  void clear() {
    for (String name : mongo.getCollectionNames()) mongo.remove(new Query(), name);
  }

  @AfterAll
  void cleanup() throws Exception {
    assertTrue(mongo.getDb().getName().startsWith("agenda_wa_test_"));
    mongo.getDb().drop();
    java.nio.file.Files.deleteIfExists(
        java.nio.file.Path.of(
            System.getProperty("java.io.tmpdir"), "agenda-wa-test-security/auth.key"));
  }

  class Browser {
    final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    final HttpClient http = HttpClient.newBuilder().cookieHandler(cookies).build();
    String csrf;

    Browser() throws Exception {
      csrf = json(send("GET", "auth/csrf", null, false)).getString("token");
    }

    HttpResponse<String> send(String method, String path, String body) throws Exception {
      return send(method, path, body, true);
    }

    HttpResponse<String> send(String method, String path, String body, boolean includeCsrf)
        throws Exception {
      var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/" + path));
      if (includeCsrf && csrf != null) request.header("X-XSRF-TOKEN", csrf);
      if (body != null) request.header("Content-Type", "application/json");
      return http.send(
          request
              .method(
                  method,
                  body == null
                      ? HttpRequest.BodyPublishers.noBody()
                      : HttpRequest.BodyPublishers.ofString(body))
              .build(),
          HttpResponse.BodyHandlers.ofString());
    }

    Document register(String email) throws Exception {
      var response =
          send(
              "POST",
              "auth/register",
              new Document("nome", "Anna")
                  .append("cognome", "Test")
                  .append("email", email)
                  .append("password", PASSWORD)
                  .toJson());
      assertEquals(201, response.statusCode(), response.body());
      return json(response);
    }

    HttpResponse<String> login(String email, String password) throws Exception {
      return send(
          "POST", "auth/login", new Document("email", email).append("password", password).toJson());
    }

    String token() {
      return cookies.getCookieStore().getCookies().stream()
          .filter(c -> c.getName().equals("agenda_session"))
          .findFirst()
          .orElseThrow()
          .getValue();
    }

    Document create(String path, String body) throws Exception {
      var response = send("POST", path, body);
      assertEquals(201, response.statusCode(), response.body());
      return json(response);
    }

    Document get(String path) throws Exception {
      var response = send("GET", path, null);
      assertEquals(200, response.statusCode(), response.body());
      return json(response);
    }

    String outcome() throws Exception {
      var response = send("GET", "cataloghi/esiti", null);
      assertEquals(200, response.statusCode());
      return Document.parse("{\"items\":" + response.body() + "}")
          .getList("items", Document.class)
          .getFirst()
          .getString("id");
    }

    Document client() throws Exception {
      return create(
          "clienti", "{\"tipo\":\"PERSONA_FISICA\",\"nome\":\"Giulia\",\"cognome\":\"Rossi\"}");
    }

    String appointment(String client, String outcome, String policy) {
      String today = LocalDate.now(ZoneId.of("Europe/Rome")).toString();
      return new Document("clientId", client)
          .append("data", today)
          .append("esitoId", outcome)
          .append("polizzaId", policy)
          .append("premio", 12.50)
          .append("creaCallback", true)
          .append("dataCallback", today)
          .toJson();
    }
  }

  Document json(HttpResponse<String> response) {
    return Document.parse(response.body());
  }

  long total(Document page) {
    return ((Number) page.get("total")).longValue();
  }

  long number(Document value, String name) {
    return ((Number) value.get(name)).longValue();
  }

  Browser user(String email) throws Exception {
    Browser b = new Browser();
    b.register(email);
    return b;
  }

  @Test
  void unauthenticatedClientsCannotReadOrWriteBusinessData() throws Exception {
    Browser anonymous = new Browser();
    for (String path :
        List.of(
            "panoramica",
            "navigazione",
            "clienti",
            "appuntamenti",
            "appuntamenti/riepilogo",
            "appuntamenti/selezione?periodo=TUTTE",
            "callback",
            "cataloghi/esiti",
            "cataloghi/polizze",
            "appuntamenti/missing",
            "auth/me")) assertEquals(401, anonymous.send("GET", path, null).statusCode(), path);
    for (String path : List.of("clienti", "appuntamenti", "callback", "cataloghi/esiti")) {
      assertEquals(401, anonymous.send("POST", path, "{}").statusCode(), path);
      assertEquals(401, anonymous.send("PUT", path + "/missing", "{}").statusCode(), path);
      assertEquals(401, anonymous.send("DELETE", path + "/missing", null).statusCode(), path);
    }
    assertEquals(401, anonymous.send("DELETE", "clienti", null).statusCode());
    assertEquals(
        401, anonymous.send("DELETE", "appuntamenti?periodo=TUTTE&attesi=0", null).statusCode());
    assertEquals(401, anonymous.send("POST", "auth/logout", null).statusCode());
    assertEquals(0, mongo.count(new Query(), Client.class));
  }

  @Test
  void registrationNormalizesEmailHashesPasswordAndDoesNotExposeCredentials() throws Exception {
    Browser b = new Browser();
    var response =
        b.send(
            "POST",
            "auth/register",
            "{\"nome\":\" Anna \",\"cognome\":\" Test \",\"email\":\" ANNA@agenda.example"
                + " \",\"password\":\""
                + PASSWORD
                + "\"}");
    assertEquals(201, response.statusCode(), response.body());
    assertEquals("anna@agenda.example", json(response).getString("email"));
    assertEquals("Anna", json(response).getString("nome"));
    Instant expiry = Instant.parse(json(response).getString("scadeAlle"));
    assertTrue(expiry.isAfter(Instant.now().plusSeconds(7100)));
    assertTrue(expiry.isBefore(Instant.now().plusSeconds(7300)));
    assertEquals(json(response).getString("scadeAlle"), b.get("auth/me").getString("scadeAlle"));
    assertFalse(response.body().contains("password"));
    assertFalse(response.body().contains("token"));
    String cookie = response.headers().firstValue("set-cookie").orElseThrow();
    assertTrue(cookie.contains("HttpOnly"));
    assertTrue(cookie.contains("SameSite=Strict"));
    assertTrue(cookie.contains("Path=/api"));
    assertTrue(cookie.contains("Max-Age=7200"));
    User stored = mongo.findAll(User.class).getFirst();
    assertNotEquals(PASSWORD, stored.passwordHash());
    assertTrue(passwords.matches(PASSWORD, stored.passwordHash()));
    assertEquals(
        6,
        Document.parse("{\"items\":" + b.send("GET", "cataloghi/esiti", null).body() + "}")
            .getList("items", Document.class)
            .size());
    assertEquals(
        409,
        new Browser()
            .send(
                "POST",
                "auth/register",
                new Document("nome", "A")
                    .append("cognome", "B")
                    .append("email", "ANNA@agenda.example")
                    .append("password", PASSWORD)
                    .toJson())
            .statusCode());
    Browser login = new Browser();
    assertEquals(200, login.login("ANNA@agenda.example", PASSWORD).statusCode());
    assertEquals(stored.id(), login.get("auth/me").getString("id"));
    Browser unknown = new Browser();
    assertEquals(401, unknown.login("none@agenda.example", PASSWORD).statusCode());
    assertEquals(401, unknown.login("anna@agenda.example", "incorrect password").statusCode());
    assertEquals(1, mongo.count(new Query(), User.class));
  }

  @Test
  void registrationRejectsInvalidFieldsBeforeWriting() throws Exception {
    Browser b = new Browser();
    for (Document invalid :
        List.of(
            new Document("nome", "")
                .append("cognome", "Test")
                .append("email", "invalid@agenda.example")
                .append("password", PASSWORD),
            new Document("nome", "Anna")
                .append("cognome", "")
                .append("email", "invalid@agenda.example")
                .append("password", PASSWORD),
            new Document("nome", "Anna")
                .append("cognome", "Test")
                .append("email", "invalid")
                .append("password", PASSWORD),
            new Document("nome", "Anna")
                .append("cognome", "Test")
                .append("email", "invalid@agenda.example")
                .append("password", "short"),
            new Document("nome", "Anna")
                .append("cognome", "Test")
                .append("email", "invalid@agenda.example")
                .append("password", "            "))) {
      assertEquals(400, b.send("POST", "auth/register", invalid.toJson()).statusCode());
    }
    assertEquals(0, mongo.count(new Query(), User.class));
    assertEquals(0, mongo.count(new Query(), CatalogItem.class));
  }

  @Test
  void appointmentTextSearchCombinesFieldsFiltersPaginationSummariesAndOwnerScope()
      throws Exception {
    Browser a = user("search-a@agenda.example"), b = user("search-b@agenda.example");
    var person =
        a.create(
            "clienti", "{\"tipo\":\"PERSONA_FISICA\",\"nome\":\"André\",\"cognome\":\"Rossi\"}");
    var company = a.create("clienti", "{\"tipo\":\"AZIENDA\",\"nomeLegale\":\"Àlfa S.r.l.\"}");
    String outcome = a.outcome();
    Document body =
        Document.parse(a.appointment(person.getString("id"), outcome, null))
            .append("note", "Rinnóvo caffè [DEMO].*");
    String first = a.create("appuntamenti", body.toJson()).getString("id");
    String second = a.create("appuntamenti", body.toJson()).getString("id");
    a.create("appuntamenti", new Document(body).append("note", "Altro incontro").toJson());
    String companyAppointment =
        a.create(
                "appuntamenti",
                new Document(body).append("clientId", company.getString("id")).toJson())
            .getString("id");
    String query =
        "?q="
            + URLEncoder.encode(
                "  ANDRE\t rossi\n rinnovo CÀFFE  ", java.nio.charset.StandardCharsets.UTF_8);
    var page = a.get("appuntamenti" + query + "&size=1");
    assertEquals(2, total(page));
    var ids = new ArrayList<>(List.of(first, second));
    Collections.sort(ids);
    assertEquals(ids.getFirst(), page.getList("items", Document.class).getFirst().getString("id"));
    assertEquals(
        ids.getLast(),
        a.get("appuntamenti" + query + "&size=1&page=1")
            .getList("items", Document.class)
            .getFirst()
            .getString("id"));
    var summary = a.get("appuntamenti/riepilogo" + query);
    assertEquals(2, number(summary, "appuntamenti"));
    assertEquals(
        0,
        new java.math.BigDecimal("25.00")
            .compareTo(new java.math.BigDecimal(summary.get("premi").toString())));
    assertEquals(0, total(b.get("appuntamenti" + query)));
    assertEquals(0, number(b.get("appuntamenti/riepilogo" + query), "appuntamenti"));
    var foreign = b.client();
    b.create(
        "appuntamenti",
        Document.parse(b.appointment(foreign.getString("id"), b.outcome(), null))
            .append("note", "ANDRE rossi rinnovo caffe")
            .toJson());
    assertEquals(2, total(a.get("appuntamenti" + query)));
    assertEquals(2, number(a.get("appuntamenti/riepilogo" + query), "appuntamenti"));
    assertEquals(1, total(b.get("appuntamenti" + query)));
    String companyQuery =
        "?q=" + URLEncoder.encode("alfa s.r.l. rinnovo", java.nio.charset.StandardCharsets.UTF_8);
    assertEquals(
        companyAppointment,
        a.get("appuntamenti" + companyQuery)
            .getList("items", Document.class)
            .getFirst()
            .getString("id"));
    assertEquals(1, number(a.get("appuntamenti/riepilogo" + companyQuery), "appuntamenti"));
    assertEquals(
        0,
        total(
            a.get(
                "appuntamenti?q="
                    + URLEncoder.encode(".*", java.nio.charset.StandardCharsets.UTF_8))));
    assertEquals(
        0,
        total(
            a.get(
                "appuntamenti?q="
                    + URLEncoder.encode("[missing]", java.nio.charset.StandardCharsets.UTF_8))));
    for (String fragment : List.of("ross", "rinn", "rinnovi")) {
      assertEquals(0, total(a.get("appuntamenti?q=" + fragment)));
      assertEquals(0, number(a.get("appuntamenti/riepilogo?q=" + fragment), "appuntamenti"));
    }
    assertEquals(2, total(a.get("appuntamenti?q=caffe%20rinnovo%20rossi%20andre")));
    assertEquals(2, total(a.get("appuntamenti?q=%22rossi%22%20-rinnovo")));
    assertEquals(3, total(a.get("appuntamenti?q=%5BDEMO%5D")));
    assertEquals(0, number(a.get("appuntamenti/riepilogo?q=.%2A"), "appuntamenti"));
    assertEquals(0, total(a.get("appuntamenti?q=rossi%20missing")));
    assertEquals(4, total(a.get("appuntamenti?q=%20%20")));
    String date = body.getString("data");
    String combined = query + "&dal=" + date + "&al=" + date + "&esitoId=" + outcome;
    assertEquals(2, total(a.get("appuntamenti" + combined)));
    assertEquals(2, number(a.get("appuntamenti/riepilogo" + combined), "appuntamenti"));
    String after = LocalDate.parse(date).plusDays(1).toString();
    assertEquals(0, total(a.get("appuntamenti" + query + "&dal=" + after)));
    assertEquals(
        0, number(a.get("appuntamenti/riepilogo" + query + "&dal=" + after), "appuntamenti"));
    var otherOutcome = a.create("cataloghi/esiti", "{\"nome\":\"Esito ricerca\"}").getString("id");
    assertEquals(0, total(a.get("appuntamenti" + query + "&esitoId=" + otherOutcome)));
    assertEquals(
        0,
        number(
            a.get("appuntamenti/riepilogo" + query + "&esitoId=" + otherOutcome), "appuntamenti"));
    assertEquals(
        400, b.send("GET", "appuntamenti" + query + "&esitoId=" + outcome, null).statusCode());
    assertEquals(
        400,
        b.send("GET", "appuntamenti/riepilogo" + query + "&esitoId=" + outcome, null).statusCode());
    assertEquals(401, new Browser().send("GET", "appuntamenti" + query, null).statusCode());
    assertEquals(
        401, new Browser().send("GET", "appuntamenti/riepilogo" + query, null).statusCode());
    assertEquals(
        200,
        a.send(
                "PUT",
                "clienti/" + person.getString("id"),
                "{\"tipo\":\"PERSONA_FISICA\",\"nome\":\"André\",\"cognome\":\"Bianchi\"}")
            .statusCode());
    assertEquals(0, total(a.get("appuntamenti" + query)));
    assertEquals(2, total(a.get("appuntamenti?q=andre%20bianchi%20rinnovo")));
    assertEquals(
        2, number(a.get("appuntamenti/riepilogo?q=andre%20bianchi%20rinnovo"), "appuntamenti"));
    assertEquals(
        200,
        a.send(
                "PUT",
                "appuntamenti/" + first,
                new Document(body)
                    .append("note", "Nuova nota")
                    .append("creaCallback", false)
                    .append("dataCallback", null)
                    .toJson())
            .statusCode());
    assertEquals(1, total(a.get("appuntamenti?q=andre%20bianchi%20rinnovo")));
    assertEquals(
        1, number(a.get("appuntamenti/riepilogo?q=andre%20bianchi%20rinnovo"), "appuntamenti"));
    assertEquals(1, total(a.get("appuntamenti?q=bianchi%20nuova")));
  }

  @Test
  void listsSearchFiltersSummariesAndNavigationArePrivate() throws Exception {
    Browser a = user("a@agenda.example"), b = user("b@agenda.example");
    var c = a.client();
    String outcome = a.outcome();
    a.create("appuntamenti", a.appointment(c.getString("id"), outcome, null));
    assertEquals(1, total(a.get("clienti?q=rossi")));
    assertEquals(1, total(a.get("appuntamenti")));
    for (String path : List.of("clienti", "clienti?q=rossi", "appuntamenti", "callback"))
      assertEquals(0, total(b.get(path)), path);
    assertEquals(400, b.send("GET", "appuntamenti?esitoId=" + outcome, null).statusCode());
    var dashboard = b.get("panoramica");
    assertEquals(0, number(dashboard, "clienti"));
    assertEquals(0, number(dashboard, "callbackOggi"));
    assertEquals(0, number(dashboard.get("riepilogo", Document.class), "appuntamenti"));
    assertEquals(0, number(b.get("appuntamenti/riepilogo"), "appuntamenti"));
    assertEquals(0, number(b.get("navigazione"), "callbackOggi"));
    assertEquals(1, number(a.get("navigazione"), "callbackOggi"));
    assertFalse(a.send("GET", "cataloghi/esiti", null).body().contains("ownerId"));
  }

  @Test
  void companyClientsAndTheirActivitiesRemainOwnerScoped() throws Exception {
    Browser a = user("company-a@agenda.example"), b = user("company-b@agenda.example");
    String body = "{\"tipo\":\"AZIENDA\",\"nomeLegale\":\"Alfa S.r.l.\"}";
    var company = a.create("clienti", body);
    var app = a.create("appuntamenti", a.appointment(company.getString("id"), a.outcome(), null));
    String callback = a.get("callback").getList("items", Document.class).getFirst().getString("id");
    assertEquals("ALFA S.R.L.", a.get("appuntamenti/" + app.getString("id")).getString("client"));
    assertEquals(0, total(b.get("clienti?q=Alfa")));
    assertEquals(404, b.send("PUT", "clienti/" + company.getString("id"), body).statusCode());
    assertEquals(
        404,
        b.send("POST", "appuntamenti", b.appointment(company.getString("id"), b.outcome(), null))
            .statusCode());
    assertEquals(404, b.send("GET", "appuntamenti/" + app.getString("id"), null).statusCode());
    assertEquals(404, b.send("GET", "callback/" + callback, null).statusCode());
    assertEquals(204, b.send("DELETE", "clienti", null).statusCode());
    assertEquals(1, total(a.get("clienti")));
    assertEquals(1, total(a.get("appuntamenti")));
    assertEquals(1, total(a.get("callback")));
  }

  @Test
  void foreignIdsCannotBeReadChangedDeletedOrBorrowedAsReferences() throws Exception {
    Browser a = user("a@agenda.example"), b = user("b@agenda.example");
    var c = a.client();
    String outcome = a.outcome();
    var policy = a.create("cataloghi/polizze", "{\"nome\":\"Polizza privata\"}");
    var app =
        a.create("appuntamenti", a.appointment(c.getString("id"), outcome, policy.getString("id")));
    String callback = a.get("callback").getList("items", Document.class).getFirst().getString("id");
    for (String path : List.of("appuntamenti/" + app.getString("id"), "callback/" + callback))
      assertEquals(404, b.send("GET", path, null).statusCode());
    assertEquals(
        404,
        b.send(
                "PUT",
                "clienti/" + c.getString("id"),
                "{\"tipo\":\"PERSONA_FISICA\",\"nome\":\"Cambio\",\"cognome\":\"Test\"}")
            .statusCode());
    String mine = b.client().getString("id");
    String myOutcome = b.outcome();
    assertEquals(
        404,
        b.send("PUT", "appuntamenti/" + app.getString("id"), b.appointment(mine, myOutcome, null))
            .statusCode());
    for (String path :
        List.of(
            "clienti/" + c.getString("id"),
            "appuntamenti/" + app.getString("id"),
            "callback/" + callback))
      assertEquals(404, b.send("DELETE", path, null).statusCode(), path);
    for (String path :
        List.of("cataloghi/esiti/" + outcome, "cataloghi/polizze/" + policy.getString("id"))) {
      assertEquals(400, b.send("PUT", path, "{\"nome\":\"Cambio\"}").statusCode());
      assertEquals(400, b.send("DELETE", path, null).statusCode());
    }
    assertEquals(
        404,
        b.send("POST", "appuntamenti", b.appointment(c.getString("id"), myOutcome, null))
            .statusCode());
    assertEquals(
        400, b.send("POST", "appuntamenti", b.appointment(mine, outcome, null)).statusCode());
    assertEquals(
        400,
        b.send("POST", "appuntamenti", b.appointment(mine, myOutcome, policy.getString("id")))
            .statusCode());
    assertEquals(0, total(b.get("appuntamenti")));
    assertEquals(0, total(b.get("callback")));
    assertEquals(1, total(a.get("appuntamenti")));
    assertEquals(1, total(a.get("callback")));
    assertEquals("rossi giulia", a.get("appuntamenti/" + app.getString("id")).getString("client"));
    assertFalse(
        mongo
            .getCollection("clienti")
            .find(new Document("_id", mine))
            .first()
            .containsKey("revision"));
  }

  @Test
  void bulkDeletionAndCatalogEditsAffectOnlyTheCurrentUser() throws Exception {
    Browser a = user("a@agenda.example"), b = user("b@agenda.example");
    var c = a.client();
    a.create("appuntamenti", a.appointment(c.getString("id"), a.outcome(), null));
    String bc = b.client().getString("id");
    b.create("appuntamenti", b.appointment(bc, b.outcome(), null));
    assertEquals(1, total(b.get("appuntamenti/selezione?periodo=TUTTE")));
    assertEquals(409, b.send("DELETE", "appuntamenti?periodo=TUTTE&attesi=2", null).statusCode());
    assertEquals(
        1,
        json(b.send("DELETE", "appuntamenti?periodo=TUTTE&attesi=1", null))
            .getInteger("eliminati"));
    assertEquals(1, total(a.get("appuntamenti")));
    assertEquals(1, total(b.get("callback")));
    assertEquals(204, b.send("DELETE", "clienti", null).statusCode());
    assertEquals(0, total(b.get("clienti")));
    assertEquals(0, total(b.get("callback")));
    assertEquals(1, total(a.get("clienti")));
    assertEquals(1, total(a.get("callback")));
    String shared = "Un valore uguale";
    var pa = a.create("cataloghi/polizze", "{\"nome\":\"" + shared + "\"}");
    var pb = b.create("cataloghi/polizze", "{\"nome\":\"" + shared + "\"}");
    assertNotEquals(pa.getString("id"), pb.getString("id"));
    assertEquals(
        200,
        b.send("PUT", "cataloghi/polizze/" + pb.getString("id"), "{\"nome\":\"Nome nuovo\"}")
            .statusCode());
    assertTrue(a.send("GET", "cataloghi/polizze", null).body().contains(shared));
    assertEquals(
        204, b.send("DELETE", "cataloghi/polizze/" + pb.getString("id"), null).statusCode());
    assertTrue(a.send("GET", "cataloghi/polizze", null).body().contains(shared));
  }

  @Test
  void denormalizedRenamesRemainPrivateWithIdenticalNamesAcrossUsers() throws Exception {
    Browser a = user("a@agenda.example"), b = user("b@agenda.example");
    String ca = a.client().getString("id"), cb = b.client().getString("id");
    String oa = a.outcome(), ob = b.outcome();
    String pa = a.create("cataloghi/polizze", "{\"nome\":\"Stesso nome\"}").getString("id");
    String pb = b.create("cataloghi/polizze", "{\"nome\":\"Stesso nome\"}").getString("id");
    String aa = a.create("appuntamenti", a.appointment(ca, oa, pa)).getString("id");
    String ab = b.create("appuntamenti", b.appointment(cb, ob, pb)).getString("id");
    Document before = a.get("appuntamenti/" + aa);
    assertEquals(
        200,
        b.send(
                "PUT",
                "clienti/" + cb,
                "{\"tipo\":\"PERSONA_FISICA\",\"nome\":\"Luca\",\"cognome\":\"Verdi\"}")
            .statusCode());
    assertEquals(
        200,
        b.send("PUT", "cataloghi/esiti/" + ob, "{\"nome\":\"Esito rinominato\"}").statusCode());
    assertEquals(
        200,
        b.send("PUT", "cataloghi/polizze/" + pb, "{\"nome\":\"Polizza rinominata\"}").statusCode());
    assertEquals(before, a.get("appuntamenti/" + aa));
    Document changed = b.get("appuntamenti/" + ab);
    assertEquals("verdi luca", changed.getString("client"));
    assertEquals("Esito rinominato", changed.getString("esito"));
    assertEquals("Polizza rinominata", changed.getString("polizza"));
    assertEquals(ob, changed.getString("esitoId"));
    assertEquals(pb, changed.getString("polizzaId"));
    assertEquals(1, total(a.get("appuntamenti?esitoId=" + oa)));
    assertEquals(1, total(b.get("appuntamenti?esitoId=" + ob)));
    assertEquals(1, number(b.get("appuntamenti/riepilogo?esitoId=" + ob), "appuntamenti"));
  }

  @Test
  void csrfIsRequiredForRegistrationAndForAuthenticatedMutations() throws Exception {
    Browser b = new Browser();
    assertEquals(
        403,
        b.send(
                "POST",
                "auth/register",
                new Document("nome", "Anna")
                    .append("cognome", "Test")
                    .append("email", "a@agenda.example")
                    .append("password", PASSWORD)
                    .toJson(),
                false)
            .statusCode());
    b.register("a@agenda.example");
    assertEquals(
        403,
        b.send(
                "POST",
                "clienti",
                "{\"tipo\":\"PERSONA_FISICA\",\"nome\":\"A\",\"cognome\":\"B\"}",
                false)
            .statusCode());
    assertEquals(0, total(b.get("clienti")));
    b.client();
    assertEquals(1, total(b.get("clienti")));
  }

  @Test
  void logoutRevokesTokensAndLoginCanRecoverEvenWithAnInvalidCookie() throws Exception {
    Browser a = user("a@agenda.example");
    String token = a.token();
    Browser other = new Browser();
    assertEquals(200, other.login("a@agenda.example", PASSWORD).statusCode());
    assertEquals(204, a.send("POST", "auth/logout", null).statusCode());
    assertEquals(401, a.send("GET", "auth/me", null).statusCode());
    assertEquals(401, other.send("GET", "auth/me", null).statusCode());
    var request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/clienti"))
            .header("Authorization", "Bearer " + token)
            .GET()
            .build();
    assertEquals(
        401,
        HttpClient.newHttpClient()
            .send(request, HttpResponse.BodyHandlers.ofString())
            .statusCode());
    assertEquals(200, other.login("a@agenda.example", PASSWORD).statusCode());
    assertEquals(200, other.send("GET", "auth/me", null).statusCode());
  }

  String signed(String subject, String issuer, List<String> audience, Instant expires) {
    var claims =
        JwtClaimsSet.builder()
            .subject(subject)
            .issuer(issuer)
            .audience(audience)
            .issuedAt(Instant.now().minusSeconds(300))
            .claim("version", 0);
    if (expires != null) claims.expiresAt(expires);
    return encoder
        .encode(
            JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build()))
        .getTokenValue();
  }

  @Test
  void jwtSignatureExpiryIssuerAudienceAndUserExistenceAreChecked() throws Exception {
    Browser a = user("a@agenda.example");
    String id = a.get("auth/me").getString("id");
    String normal = a.token();
    String[] parts = normal.split("\\.");
    String forged =
        parts[0]
            + "."
            + parts[1]
            + "."
            + Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]);
    for (String token :
        List.of(
            forged,
            "broken.token",
            signed(
                id,
                SecurityConfiguration.ISSUER,
                List.of(SecurityConfiguration.AUDIENCE),
                Instant.now().minusSeconds(1)),
            signed(
                id,
                "other",
                List.of(SecurityConfiguration.AUDIENCE),
                Instant.now().plusSeconds(300)),
            signed(
                id, SecurityConfiguration.ISSUER, List.of("other"), Instant.now().plusSeconds(300)),
            signed(id, SecurityConfiguration.ISSUER, List.of(SecurityConfiguration.AUDIENCE), null),
            signed(
                "missing",
                SecurityConfiguration.ISSUER,
                List.of(SecurityConfiguration.AUDIENCE),
                Instant.now().plusSeconds(300)))) {
      var request =
          HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/clienti"))
              .header("Authorization", "Bearer " + token)
              .GET()
              .build();
      assertEquals(
          401,
          HttpClient.newHttpClient()
              .send(request, HttpResponse.BodyHandlers.ofString())
              .statusCode());
    }
    var request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/clienti"))
            .header("Authorization", "Bearer " + normal)
            .GET()
            .build();
    assertEquals(
        200,
        HttpClient.newHttpClient()
            .send(request, HttpResponse.BodyHandlers.ofString())
            .statusCode());
  }

  @Test
  void repeatedWrongPasswordsAreRateLimited() throws Exception {
    Browser b = new Browser();
    String email = UUID.randomUUID() + "@agenda.example";
    for (int i = 0; i < 10; i++) assertEquals(401, b.login(email, "wrong password").statusCode());
    assertEquals(429, b.login(email, PASSWORD).statusCode());
  }

  @Test
  void existingDemoDataCanBeClaimedOnlyByTheEmailChosenByTheUser() throws Exception {
    String reserved = UUID.randomUUID().toString();
    mongo.insert(
        new Document("_id", "initial-owner")
            .append("email", "nashj760@gmail.com")
            .append("userId", reserved),
        "metadati");
    mongo.insert(new Client("demo", reserved, "Luca", "Rossi"));
    mongo.insert(new Client("unassigned", null, "Anonimo", "Storico"));
    Browser other = user("other@agenda.example");
    assertEquals(0, total(other.get("clienti")));
    Browser owner = user("NASHJ760@gmail.com");
    assertEquals(reserved, owner.get("auth/me").getString("id"));
    assertEquals(1, total(owner.get("clienti")));
    assertEquals(
        "demo", owner.get("clienti").getList("items", Document.class).getFirst().getString("id"));
    assertNull(mongo.findById("initial-owner", Document.class, "metadati"));
  }

  @Test
  void phoneAndEmbeddedHistoryAreScopedAndFullDetailsRequireAnAuthorizedSelection() throws Exception {
    Browser a = user("history-a@agenda.example"), b = user("history-b@agenda.example");
    String ca = a.client().getString("id"), cb = b.client().getString("id");
    String first = a.create("appuntamenti", a.appointment(ca, a.outcome(), null)).getString("id");
    String second = b.create("appuntamenti", b.appointment(cb, b.outcome(), null)).getString("id");
    assertEquals("", a.get("clienti/" + ca).getString("telefono"));
    assertEquals(200, a.send("PUT", "clienti/" + ca,
        "{\"tipo\":\"PERSONA_FISICA\",\"nome\":\"Giulia\",\"cognome\":\"Rossi\",\"telefono\":\"+39 0123\"}").statusCode());
    assertEquals("+39 0123", a.get("clienti/" + ca).getString("telefono"));
    assertEquals("+39 0123", a.get("clienti").getList("items", Document.class).getFirst().getString("telefono"));
    var summary = a.get("clienti/" + ca + "/appuntamenti").getList("items", Document.class).getFirst();
    assertEquals(Set.of("id", "data", "esito"), summary.keySet());
    assertEquals(first, summary.getString("id"));
    assertTrue(a.get("appuntamenti/" + first).containsKey("premio"));
    for (String path : List.of("clienti/" + ca, "clienti/" + ca + "/appuntamenti", "appuntamenti/" + first)) {
      assertEquals(404, b.send("GET", path, null).statusCode());
      assertEquals(401, new Browser().send("GET", path, null).statusCode());
    }
    assertEquals(404, b.send("PUT", "clienti/" + ca,
        "{\"tipo\":\"PERSONA_FISICA\",\"nome\":\"Giulia\",\"cognome\":\"Rossi\",\"telefono\":\"123\"}").statusCode());
    assertEquals(400, a.send("GET", "clienti/" + ca + "/appuntamenti?size=101", null).statusCode());
    assertEquals(400, a.send("PUT", "clienti/" + ca, new Document("tipo", "PERSONA_FISICA")
        .append("nome", "Giulia").append("cognome", "Rossi").append("telefono", "1".repeat(41)).toJson()).statusCode());
    assertEquals(200, a.send("DELETE", "appuntamenti?periodo=TUTTE&attesi=1", null).statusCode());
    assertEquals(0, total(a.get("clienti/" + ca + "/appuntamenti")));
    assertEquals(second, b.get("clienti/" + cb + "/appuntamenti")
        .getList("items", Document.class).getFirst().getString("id"));
    assertEquals("+39 0123", a.get("clienti/" + ca).getString("telefono"));
    assertEquals(204, a.send("DELETE", "clienti/" + ca, null).statusCode());
    assertEquals(404, a.send("GET", "clienti/" + ca + "/appuntamenti", null).statusCode());
    assertEquals(1, total(b.get("clienti/" + cb + "/appuntamenti")));
  }
}
