const { test, before, after } = require("node:test");
const assert = require("node:assert/strict");
const { createServer } = require("node:http");
const { readFile, mkdir } = require("node:fs/promises");
const { resolve, extname } = require("node:path");
const { chromium } = require("playwright");

const root = resolve(__dirname, "../../main/resources/static");
const pictures = resolve(__dirname, "../../../../docs/web");
const profile = { id: "locale", nome: "Agenda", cognome: "personale", email: "locale@localhost", scadeAlle: new Date(Date.now() + 3600000).toISOString() };
const snapshot = JSON.stringify({ formato: "agenda-wa-backup", versione: 1, dati: { clienti: [], cataloghi: [], appuntamenti: [], callback: [] } });
let server, browser, base, restored = 0;
before(async () => {
  server = createServer(async (request, response) => {
    try {
      const path = new URL(request.url, "http://localhost").pathname;
      const output = (status, value) => { response.writeHead(status, { "Content-Type": "application/json" }); response.end(JSON.stringify(value)); };
      if (path.startsWith("/api/")) {
        const authenticated = request.headers.cookie?.includes("agenda_fixture=ok");
        if (path === "/api/auth/csrf") return output(200, { headerName: "X-CSRF-TOKEN", token: "fixture-csrf" });
        if (path === "/api/auth/login") {
          let body = ""; for await (const chunk of request) body += chunk;
          if (JSON.parse(body).password !== "password-fittizia") return output(401, { detail: "Password non corretta." });
          assert.equal(request.headers["x-csrf-token"], "fixture-csrf");
          response.setHeader("Set-Cookie", "agenda_fixture=ok; HttpOnly; SameSite=Strict; Path=/api");
          return output(200, profile);
        }
        if (!authenticated) return output(401, { detail: "Accedi per usare l’agenda." });
        if (path === "/api/auth/me") return output(200, profile);
        if (path === "/api/navigazione") return output(200, { oggi: "2026-10-13", callbackOggi: 0 });
        if (path === "/api/cataloghi/esiti") return output(200, [{ id: "outcome", nome: "Sottoscrizione" }]);
        if (path === "/api/cataloghi/polizze") return output(200, [{ id: "policy", nome: "Polizza di esempio" }]);
        if (path === "/api/backup/stato") return output(200, { ultimoBackup: "2026-10-13T10:00:00Z", copieAutomatiche: 3 });
        if (path === "/api/backup" && request.method === "GET") { response.writeHead(200, { "Content-Type": "application/json", "Content-Disposition": "attachment; filename=backup.json" }); response.end(snapshot); return; }
        if (path === "/api/backup/ripristino") {
          assert.equal(request.headers["x-csrf-token"], "fixture-csrf");
          assert.match(request.headers["content-type"], /^multipart\/form-data; boundary=/);
          let body = ""; for await (const chunk of request) body += chunk;
          assert.ok(body.includes("agenda-wa-backup"));
          restored++;
          response.setHeader("Set-Cookie", "agenda_fixture=; Max-Age=0; Path=/api");
          return output(200, { clienti: 0, cataloghi: 0, appuntamenti: 0, callback: 0 });
        }
        return output(404, { detail: "Risorsa non disponibile." });
      }
      const file = resolve(root, `.${path === "/" ? "/index.html" : path}`);
      if (!file.startsWith(root + "/")) throw Error("Path non valido");
      const types = { ".html": "text/html", ".js": "text/javascript", ".css": "text/css", ".otf": "font/otf", ".svg": "image/svg+xml" };
      response.writeHead(200, { "Content-Type": types[extname(file)] || "application/octet-stream" });
      response.end(await readFile(file));
    } catch (e) { response.writeHead(500); response.end("Fixture error"); }
  });
  await new Promise((done) => server.listen(0, "127.0.0.1", done));
  base = `http://127.0.0.1:${server.address().port}`;
  browser = await chromium.launch(process.env.CHROMIUM_EXECUTABLE_PATH ? { executablePath: process.env.CHROMIUM_EXECUTABLE_PATH } : {});
  await mkdir(pictures, { recursive: true });
});
after(async () => { await browser?.close(); await new Promise((done) => server?.close(done)); });
async function enter(page) {
  await page.goto(`${base}/#impostazioni`);
  await page.locator("#auth-password").fill("password-fittizia");
  await page.locator("#auth-submit").click();
  await page.locator(".backup-panel").waitFor();
  await page.waitForFunction(() => document.querySelector("#backup-status").textContent.includes("Copie conservate"));
}
for (const width of [1280, 390, 320]) {
  test(`local login and backup panel fit ${width}px`, async () => {
    const page = await browser.newPage({ viewport: { width, height: 900 } });
    const errors = []; page.on("pageerror", (e) => errors.push(e.message));
    await page.goto(`${base}/#impostazioni`);
    assert.equal(await page.locator("#auth-form input").count(), 1);
    assert.equal(await page.getByText("Registrati", { exact: true }).count(), 0);
    if (width !== 320) await page.screenshot({ path: resolve(pictures, `accesso-locale${width === 390 ? "-mobile" : ""}.jpg`), type: "jpeg", quality: 90 });
    await page.locator("#auth-password").fill("errata"); await page.locator("#auth-submit").click();
    await page.locator("#auth-error").waitFor({ state: "visible" });
    assert.match(await page.locator("#auth-error").innerText(), /Password non corretta/);
    await page.locator("#auth-password").fill("password-fittizia"); await page.locator("#auth-submit").click();
    await page.locator(".backup-panel").waitFor();
    await page.waitForFunction(() => document.querySelector("#backup-status").textContent.includes("Copie conservate"));
    assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
    if (width !== 320) await page.screenshot({ path: resolve(pictures, `backup-locale${width === 390 ? "-mobile" : ""}.jpg`), type: "jpeg", quality: 90, fullPage: true });
    const downloadPromise = page.waitForEvent("download");
    await page.locator("#backup-export").click();
    const download = await downloadPromise;
    assert.match(download.suggestedFilename(), /^agenda-wa-backup-/);
    await download.delete();
    assert.deepEqual(errors, []);
    await page.close();
  });
}
test("restore is cancelled without writes and confirms before uploading", async () => {
  const page = await browser.newPage({ viewport: { width: 390, height: 844 } });
  const errors = []; page.on("pageerror", (e) => errors.push(e.message));
  await enter(page);
  await page.locator("#backup-file").setInputFiles({ name: "backup.json", mimeType: "application/json", buffer: Buffer.from(snapshot) });
  await page.locator("#backup-restore-form button").click();
  await page.locator("#confirmation").waitFor({ state: "visible" });
  assert.match(await page.locator("#confirmation-title").innerText(), /Ripristina/);
  await page.waitForFunction(() => getComputedStyle(document.querySelector("#confirmation")).opacity === "1");
  await page.screenshot({ path: resolve(pictures, "backup-ripristino-mobile.jpg"), type: "jpeg", quality: 90 });
  await page.getByRole("button", { name: "Annulla", exact: true }).click();
  assert.equal(restored, 0);
  await page.locator("#backup-restore-form button").click();
  await page.locator('#confirmation-input input').fill("RIPRISTINA");
  await page.getByRole("button", { name: "Ripristina", exact: true }).click();
  await page.locator("#auth-status").waitFor({ state: "visible" });
  assert.match(await page.locator("#auth-status").innerText(), /Backup ripristinato/);
  assert.equal(restored, 1);
  assert.deepEqual(errors, []);
  await page.close();
});
