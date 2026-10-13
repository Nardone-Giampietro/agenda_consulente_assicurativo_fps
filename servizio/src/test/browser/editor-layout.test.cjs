const { test, before, after } = require("node:test");
const assert = require("node:assert/strict");
const { createServer } = require("node:http");
const { readFile } = require("node:fs/promises");
const { resolve, extname } = require("node:path");
const { chromium } = require("playwright");

// Render the real HTML, CSS and dialog module, with isolated API fixtures.
// No application service, account or MongoDB database is needed.
const root = resolve(__dirname, "../../main/resources/static");
const longName = "A".repeat(150);
const fixtures = {
  empty: [],
  short: [{ id: "person", tipo: "PERSONA_FISICA", nome: "marco", cognome: "rossi" }],
  long: [
    { id: "person", tipo: "PERSONA_FISICA", nome: "n".repeat(100), cognome: "c".repeat(100) },
    { id: "company", tipo: "AZIENDA", nomeLegale: longName },
  ],
};
const viewports = [
  [1920, 1080], [1440, 900], [1280, 720], [1024, 768],
  [781, 700], [780, 700], [693, 600], [390, 844], [320, 568],
  [844, 390], [640, 360],
];
let server, browser, base;

before(async () => {
  server = createServer(async (request, response) => {
    try {
      const pathname = new URL(request.url, "http://localhost").pathname;
      const file = resolve(root, `.${pathname === "/" ? "/index.html" : pathname}`);
      if (!file.startsWith(root + "/")) throw new Error("Invalid path");
      let body = await readFile(file);
      if (pathname === "/") body = body.toString().replace('<script type="module" src="/js/app.js"></script>', "");
      const types = { ".html": "text/html", ".js": "text/javascript", ".css": "text/css", ".otf": "font/otf" };
      response.writeHead(200, { "Content-Type": types[extname(file)] || "application/octet-stream" });
      response.end(body);
    } catch {
      response.writeHead(404);
      response.end();
    }
  });
  await new Promise((done) => server.listen(0, "127.0.0.1", done));
  base = `http://127.0.0.1:${server.address().port}`;
  browser = await chromium.launch({
    ...(process.env.CHROMIUM_EXECUTABLE_PATH ? { executablePath: process.env.CHROMIUM_EXECUTABLE_PATH } : {}),
  });
});

after(async () => {
  await browser?.close();
  if (server) await new Promise((done) => server.close(done));
});

async function editor(viewport, names = "long") {
  const page = await browser.newPage({ viewport, reducedMotion: "reduce" });
  const errors = [], writes = [];
  page.on("pageerror", (error) => errors.push(error.message));
  await page.route("**/api/**", (route) => {
    const request = route.request();
    if (request.method() !== "GET") writes.push({ method: request.method(), body: request.postDataJSON() });
    let body = {};
    if (request.url().includes("/auth/csrf")) body = { headerName: "X-XSRF-TOKEN", token: "fixture" };
    if (request.url().includes("/clienti?")) body = { items: fixtures[names], total: fixtures[names].length };
    return route.fulfill({ json: body });
  });
  await page.goto(base);
  await page.evaluate(async (longName) => {
    document.querySelector("#auth-screen").hidden = true;
    document.querySelector("#agenda").hidden = false;
    window.dialogs = await import("/js/dialogs.js");
    window.dialogs.setupDialogs();
    window.editorContext = {
      active: () => true,
      today: () => "2026-10-06",
      catalogs: () => ({ esiti: [{ id: "outcome", nome: longName }], polizze: [{ id: "policy", nome: longName }] }),
      notice: () => {}, refresh: async () => {},
    };
  }, names === "long" ? longName : "Assistenza");
  return { page, errors, writes };
}

async function open(page, kind = "appuntamenti", item = null) {
  await page.evaluate(async ({ kind, item }) => window.dialogs.openEditor(kind, item, window.editorContext), { kind, item });
}

async function fits(page) {
  const failures = await page.evaluate(() => {
    const dialog = document.querySelector("#editor");
    const grid = document.querySelector("#editor-fields");
    const bounds = grid.getBoundingClientRect();
    const problems = [];
    if (dialog.scrollWidth > dialog.clientWidth + 1) problems.push("horizontal dialog overflow");
    if (grid.scrollWidth > grid.clientWidth + 1) problems.push("horizontal grid overflow");
    if (document.documentElement.scrollWidth > innerWidth) problems.push("horizontal page overflow");
    const dialogBounds = dialog.getBoundingClientRect();
    if (dialogBounds.left < 0 || dialogBounds.right > innerWidth || dialogBounds.height > innerHeight * .9 + 1)
      problems.push("dialog outside viewport");
    for (const control of grid.querySelectorAll("input:not([type=checkbox]), select, textarea")) {
      if (!control.getClientRects().length) continue;
      const box = control.getBoundingClientRect();
      const field = control.closest(".field").getBoundingClientRect();
      if (box.left < bounds.left - 1 || box.right > bounds.right + 1 || box.width > field.width + 1)
        problems.push(`${control.id} clipped`);
    }
    const columns = getComputedStyle(grid).gridTemplateColumns.split(" ").map(parseFloat);
    if (columns.length !== (innerWidth <= 780 ? 1 : 2)) problems.push("incorrect column count");
    if (columns.length === 2 && Math.abs(columns[0] - columns[1]) > 1) problems.push("unequal columns");
    return problems;
  });
  assert.deepEqual(failures, []);
}

test("appointment form contains every control with short and maximum length names", async (t) => {
  for (const [width, height] of viewports) {
    for (const names of ["short", "long"]) {
      await t.test(`${width}x${height}, ${names} names, callback off/on`, async () => {
        const { page, errors } = await editor({ width, height }, names);
        try {
          await open(page);
          await fits(page);
          await page.locator("#f-creaCallback").check();
          await page.locator("#f-dataCallback").fill("2026-10-20");
          await fits(page);
          await page.locator("#save-button").scrollIntoViewIfNeeded();
          assert(await page.locator("#save-button").isVisible());
          const save = await page.locator("#save-button").boundingBox();
          assert(save.y >= 0 && save.y + save.height <= height);
          await page.locator("#f-creaCallback").uncheck();
          assert(await page.locator("#callback-date").isHidden());
          assert.equal(await page.locator("#f-dataCallback").inputValue(), "");
          assert.deepEqual(errors, []);
        } finally { await page.close(); }
      });
    }
  }
});

test("shared editor contains edit, callback, client and catalog forms", async (t) => {
  for (const width of [1280, 781, 390, 320]) {
    await t.test(`${width}px`, async () => {
      const { page, errors } = await editor({ width, height: 720 });
      try {
        const item = { id: "appointment", clientId: "company", data: "2026-10-06", esitoId: "outcome", polizzaId: "policy", premio: 120.5, note: "Note lunghe ".repeat(200) };
        await open(page, "appuntamenti", item);
        await fits(page);
        assert.equal(await page.locator("#f-creaCallback").count(), 0);
        for (const kind of ["callback", "clienti", "esiti", "polizze"]) {
          await open(page, kind);
          await fits(page);
          if (kind === "clienti") {
            await page.locator("#f-tipo").selectOption("AZIENDA");
            await page.locator("#f-nomeLegale").fill(longName);
            await fits(page);
          }
        }
        assert.deepEqual(errors, []);
      } finally { await page.close(); }
    });
  }
});

test("required fields, keyboard navigation and save payload remain functional", async () => {
  const { page, errors, writes } = await editor({ width: 320, height: 568 });
  try {
    await open(page);
    await page.locator("#save-button").click();
    assert.equal(writes.length, 0);
    assert.equal(await page.locator("#editor").evaluate((el) => el.open), true);
    await page.locator("#f-clientId").selectOption("company");
    await page.locator("#f-esitoId").selectOption("outcome");
    await page.locator("#f-polizzaId").selectOption("policy");
    await page.locator("#f-premio").fill("125.75");
    await page.locator("#f-note").fill("Note di verifica\nSeconda riga");
    await page.locator("#f-creaCallback").check();
    await page.locator("#f-dataCallback").fill("2026-10-20");
    await page.locator("#f-dataCallback").focus();
    // Native date controls can expose multiple keyboard segments.
    for (let i = 0; i < 6 && await page.evaluate(() => document.activeElement.id === "f-dataCallback"); i++)
      await page.keyboard.press("Tab");
    assert.equal(await page.evaluate(() => document.activeElement.id), "editor-cancel");
    await page.keyboard.press("Tab");
    assert.equal(await page.evaluate(() => document.activeElement.id), "save-button");
    assert.equal(await page.locator("#save-button").evaluate((el) => getComputedStyle(el).outlineStyle), "solid");
    await page.keyboard.press("Enter");
    await page.waitForFunction(() => !document.querySelector("#editor").open);
    assert.deepEqual(writes, [{ method: "POST", body: {
      clientId: "company", data: "2026-10-06", esitoId: "outcome", polizzaId: "policy",
      premio: 125.75, note: "Note di verifica\nSeconda riga", creaCallback: true, dataCallback: "2026-10-20",
    } }]);
    await open(page);
    await page.keyboard.press("Escape");
    assert.equal(await page.locator("#editor").evaluate((el) => el.open), false);
    assert.deepEqual(errors, []);
  } finally { await page.close(); }
});

test("missing clients show a contained prompt instead of an unusable form", async () => {
  const { page, errors } = await editor({ width: 320, height: 568 }, "empty");
  try {
    await open(page);
    await fits(page);
    assert(await page.locator("#save-button").isDisabled());
    assert(await page.locator('[data-dependency="clienti"]').isVisible());
    assert.deepEqual(errors, []);
  } finally { await page.close(); }
});

test("save errors keep the form and its actions accessible", async () => {
  const { page, errors } = await editor({ width: 390, height: 568 });
  try {
    await page.route("**/api/appuntamenti", (route) => route.fulfill({
      status: 422, json: { detail: "Non è stato possibile salvare l’appuntamento. Verifica i dati e riprova." },
    }));
    await open(page);
    await page.locator("#f-clientId").selectOption("company");
    await page.locator("#f-esitoId").selectOption("outcome");
    await page.locator("#save-button").click();
    await page.locator("#editor-error").waitFor({ state: "visible" });
    await fits(page);
    assert(await page.locator("#editor").evaluate((el) => el.open));
    assert(await page.locator("#save-button").isEnabled());
    await page.locator("#editor-cancel").click();
    assert.equal(await page.locator("#editor").evaluate((el) => el.open), false);
    assert.deepEqual(errors, []);
  } finally { await page.close(); }
});

async function historyPage(viewport, { failHistory = false, delayHistory = false, emptyClients = false } = {}) {
  const page = await browser.newPage({ viewport, reducedMotion: "reduce" });
  const errors = [], requests = [];
  const clients = [
    { id: "person", tipo: "PERSONA_FISICA", nome: "giulia", cognome: "rossi", telefono: "+39 0123 456789" },
    { id: "company", tipo: "AZIENDA", nomeLegale: "SOCIETÀ " + "A".repeat(140), telefono: "" },
    { id: "empty", tipo: "PERSONA_FISICA", nome: "luca", cognome: "bianchi", telefono: "" },
  ];
  const appointments = Array.from({ length: 21 }, (_, index) => ({
    id: `history-${index}`, data: `2026-10-${String(31 - index).padStart(2, "0")}`,
    clientId: "person", client: "rossi giulia", tipoCliente: "PERSONA_FISICA",
    esitoId: "outcome", esito: "Sottoscrizione", polizzaId: null, polizza: null,
    premio: 125.50, note: `Note complete dell’incontro ${index}`,
  }));
  appointments.push({ ...appointments[0], id: "company-app", clientId: "company", client: clients[1].nomeLegale,
    tipoCliente: "AZIENDA", esito: "Esito " + "L".repeat(145) });
  page.on("pageerror", error => errors.push(error.message));
  await page.route("**/api/**", async route => {
    const request = route.request(), url = new URL(request.url());
    requests.push(url.pathname + url.search);
    let body;
    const paginated = (items) => {
      const size = Number(url.searchParams.get("size") || 20), current = Number(url.searchParams.get("page") || 0);
      return { items: items.slice(current * size, (current + 1) * size), total: items.length, page: current, size };
    };
    if (url.pathname === "/api/auth/me") body = { nome: "Anna", cognome: "Verifica", scadeAlle: new Date(Date.now() + 3600000).toISOString() };
    else if (url.pathname === "/api/auth/csrf") body = { headerName: "X-XSRF-TOKEN", token: "fixture" };
    else if (url.pathname === "/api/auth/logout") return route.fulfill({ status: 204 });
    else if (url.pathname === "/api/navigazione") body = { oggi: "2026-10-06", callbackOggi: 0 };
    else if (url.pathname === "/api/cataloghi/esiti") body = [{ id: "outcome", nome: "Sottoscrizione" }];
    else if (url.pathname === "/api/cataloghi/polizze") body = [];
    else if (url.pathname === "/api/appuntamenti/riepilogo") body = { appuntamenti: 22, sottoscrizioni: 21, premi: 2761 };
    else if (url.pathname === "/api/clienti") body = paginated(emptyClients ? [] : clients);
    else if (url.pathname === "/api/appuntamenti") body = paginated(appointments);
    else if (/\/clienti\/[^/]+\/appuntamenti$/.test(url.pathname)) {
      if (delayHistory) await new Promise(resolve => setTimeout(resolve, 200));
      if (failHistory) { failHistory = false; return route.fulfill({ status: 503, json: { detail: "Riprova tra poco." } }); }
      const id = url.pathname.split("/")[3];
      body = paginated(appointments.filter(a => a.clientId === id).map(({ id, data, esito }) => ({ id, data, esito })));
    } else if (/\/clienti\/[^/]+$/.test(url.pathname)) {
      const client = clients.find(c => c.id === url.pathname.split("/")[3]);
      if (request.method() === "PUT") Object.assign(client, request.postDataJSON());
      body = client;
    } else if (/\/appuntamenti\/[^/]+$/.test(url.pathname)) {
      const item = appointments.find(a => a.id === url.pathname.split("/")[3]);
      if (request.method() === "PUT") Object.assign(item, request.postDataJSON());
      body = item;
    } else throw new Error(`Unexpected API request ${request.method()} ${url.pathname}`);
    return route.fulfill({ json: body });
  });
  await page.goto(base + "/#clienti");
  await page.evaluate(() => import("/js/app.js"));
  await page.locator(".heading-actions [data-add=clienti]").waitFor();
  return { page, errors, requests };
}

async function historyFits(page) {
  assert.deepEqual(await page.evaluate(() => {
    const dialog = document.querySelector("#editor");
    return [document.documentElement.scrollWidth <= innerWidth,
      dialog.scrollWidth <= dialog.clientWidth + 1,
      document.querySelector("#editor-fields").scrollWidth <= document.querySelector("#editor-fields").clientWidth + 1];
  }), [true, true, true]);
}
async function reference(page, name) {
  if (!process.env.AGENDA_HISTORY_SCREENSHOTS || ![1280, 390].includes(page.viewportSize().width)) return;
  const mobile = page.viewportSize().width === 390 ? "-mobile" : "";
  await page.screenshot({ path: resolve(root, `../../../../../docs/web/${name}${mobile}.jpg`), type: "jpeg", quality: 90 });
}

test("client history navigates lazily from clients and appointments, preserving return and edit context", async t => {
  for (const width of [1280, 781, 390, 320]) await t.test(`${width}px`, async () => {
    const { page, errors, requests } = await historyPage({ width, height: 900 });
    try {
      assert((await page.locator("#main").innerText()).includes("+39 0123 456789"));
      await reference(page, "clienti");
      await page.locator('button[data-view-client="person"]').focus();
      await page.keyboard.press("Enter");
      await page.locator(".client-history-entry").first().waitFor();
      assert.equal(await page.locator(".client-history-entry").count(), 20);
      assert((await page.locator(".client-history-entry").first().innerText()).includes("31/10/2026"));
      assert(!requests.some(r => /^\/api\/appuntamenti\//.test(r)));
      assert(!(await page.locator("#editor-fields").innerText()).includes("Note complete"));
      await historyFits(page);
      await reference(page, "cliente-storico");
      await page.locator('[data-history-page="1"]').click();
      await page.locator('[data-history-appointment="history-20"]').waitFor();
      await page.locator('[data-history-appointment="history-20"]').click();
      await page.locator(".detail-notes").waitFor();
      assert((await page.locator(".detail-notes").innerText()).includes("Note complete dell’incontro 20"));
      assert.equal(await page.locator("#appointment-phone").count(), 0);
      await historyFits(page);
      await page.locator("#enable-edit").click();
      await page.locator("#f-note").fill("Modifica dallo storico");
      await page.locator("#editor-cancel").click();
      assert((await page.locator(".detail-notes").innerText()).includes("Note complete dell’incontro 20"));
      await page.locator("#enable-edit").click();
      await page.locator("#f-note").fill("Salvato dallo storico");
      await page.locator("#save-button").click();
      await page.waitForFunction(() => document.querySelector(".detail-notes")?.textContent.includes("Salvato dallo storico"));
      await page.locator("#editor-back").click();
      await page.locator('[data-history-appointment="history-20"]').waitFor();
      assert((await page.locator(".history-pagination").innerText()).includes("Pagina 2 di 2"));
      await page.locator("#editor-cancel").click();
      await page.locator('[data-route="appuntamenti"]').click();
      await page.locator('.appointment-row [data-view-appointment="history-0"]').click();
      await page.waitForFunction(() => document.querySelector("#appointment-phone")?.textContent === "+39 0123 456789");
      await historyFits(page);
      await reference(page, "appuntamento-dettagli");
      await page.locator("[data-client-history]").click();
      await page.locator(".client-history-entry").first().waitFor();
      await page.locator('[data-history-appointment="history-1"]').click();
      await page.locator(".detail-notes").waitFor();
      assert.equal(await page.locator("#appointment-phone").count(), 0);
      await reference(page, "cliente-storico-dettagli");
      await page.locator("#editor-back").click();
      await page.locator(".client-history-entry").first().waitFor();
      await page.locator("#editor-back").click();
      await page.waitForFunction(() => document.querySelector("#appointment-phone")?.textContent === "+39 0123 456789");
      assert((await page.locator(".detail-notes").innerText()).includes("Note complete dell’incontro 0"));
      await page.locator("#editor-cancel").click();
      await page.locator('[data-route="clienti"]').click();
      await page.locator('[data-edit="clienti"][data-id="person"]').click();
      assert.equal(await page.locator("#f-telefono").inputValue(), "+39 0123 456789");
      await page.locator("#f-telefono").fill("+39 0987 654321");
      await page.locator("#save-button").click();
      await page.waitForFunction(() => !document.querySelector("#editor").open);
      await page.waitForFunction(() => document.querySelector("#main").textContent.includes("+39 0987 654321"));
      await page.locator('[data-view-client="company"]').first().click();
      await page.locator(".client-history-entry").first().waitFor();
      await historyFits(page);
      await page.locator("#editor-cancel").click();
      await page.locator('[data-view-client="empty"]').first().click();
      await page.getByText("Nessun appuntamento", { exact: true }).waitFor();
      assert((await page.locator("#editor-fields").innerText()).includes("Non inserito"));
      await historyFits(page);
      assert.deepEqual(errors, []);
    } finally { await page.close(); }
  });
});

test("history failures can be retried and late responses cannot reopen a closed or expired dialog", async () => {
  const { page, errors } = await historyPage({ width: 390, height: 844 }, { failHistory: true, delayHistory: true });
  try {
    await page.locator('[data-view-client="person"]').first().click();
    await page.locator("[data-history-retry]").waitFor();
    assert((await page.locator("#editor-error").innerText()).includes("Riprova"));
    await page.locator("[data-history-retry]").click();
    await page.locator(".client-history-entry").first().waitFor();
    await page.locator("#editor-cancel").click();
    await page.locator('[data-view-client="person"]').first().click();
    await page.keyboard.press("Escape");
    await page.waitForTimeout(300);
    assert.equal(await page.locator("#editor").evaluate(el => el.open), false);
    await page.locator('[data-view-client="person"]').first().click();
    await page.evaluate(() => window.dispatchEvent(new Event("auth-expired")));
    await page.waitForTimeout(300);
    assert.equal(await page.locator("#editor").evaluate(el => el.open), false);
    assert.equal(await page.locator("#editor-fields").innerText(), "");
    assert.deepEqual(errors, []);
  } finally { await page.close(); }
});


test("new appointment still opens client creation when no clients exist", async () => {
  const { page, errors } = await historyPage({ width: 390, height: 844 }, { emptyClients: true });
  try {
    await page.locator('[data-route="appuntamenti"]').click();
    await page.locator('.heading-actions [data-add="appuntamenti"]').click();
    await page.locator('[data-dependency="clienti"]').click();
    await page.locator('#f-telefono').waitFor();
    assert.equal(await page.locator('#editor-title').innerText(), 'Nuovo cliente');
    assert.equal(await page.locator('#f-telefono').inputValue(), '');
    assert.deepEqual(errors, []);
  } finally { await page.close(); }
});
