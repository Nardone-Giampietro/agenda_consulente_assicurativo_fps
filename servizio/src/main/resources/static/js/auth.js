import { api, resetSession } from "./api.js";
export async function setupAuth(onLogin, onLogout) {
  const screen = document.querySelector("#auth-screen");
  const agenda = document.querySelector("#agenda");
  const form = document.querySelector("#auth-form");
  const submit = document.querySelector("#auth-submit");
  const password = document.querySelector("#auth-password");
  const message = document.querySelector("#auth-error");
  const status = document.querySelector("#auth-status");
  const logout = document.querySelector("#logout");
  let busy = false, authenticated = false, expiryTimer;
  function mode() {
    document.title = "Agenda WA · Accedi";
    password.value = "";
    message.hidden = true;
    password.focus();
  }
  function enter(profile) {
    resetSession();
    authenticated = true;
    clearTimeout(expiryTimer);
    expiryTimer = setTimeout(() => leave(true), Math.max(0, Date.parse(profile.scadeAlle) - Date.now()));
    form.reset();
    message.hidden = status.hidden = true;
    screen.hidden = true;
    agenda.hidden = false;
    document.querySelector("#user-name").textContent = "Agenda personale";
    document.querySelector(".profile").textContent = "AP";
    onLogin(profile);
  }
  function leave(expired = false) {
    authenticated = false;
    clearTimeout(expiryTimer);
    resetSession();
    onLogout();
    agenda.hidden = true;
    screen.hidden = false;
    form.reset();
    document.querySelector("#user-name").textContent = document.querySelector(".profile").textContent = "";
    mode();
    status.textContent = expired ? "La sessione è scaduta. Accedi di nuovo per continuare." : "";
    status.hidden = !expired;
  }
  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (busy || !form.reportValidity()) return;
    busy = true;
    submit.disabled = true;
    submit.setAttribute("aria-busy", "true");
    submit.textContent = "Accesso…";
    message.hidden = true;
    try {
      const profile = await api("auth/login", { method: "POST", body: Object.fromEntries(new FormData(form)) });
      enter(profile);
    } catch (error) {
      if (error.name !== "AbortError") {
        message.textContent = error.message;
        message.hidden = false;
        password.value = "";
        password.focus();
      }
    } finally {
      busy = false;
      submit.disabled = false;
      submit.removeAttribute("aria-busy");
      submit.textContent = "Accedi";
    }
  });
  logout.addEventListener("click", async () => {
    logout.disabled = true;
    try { await api("auth/logout", { method: "POST" }); leave(); }
    catch (error) {
      if (error.status === 401) leave(true);
      else {
        const notice = document.querySelector("#notice");
        notice.textContent = error.message;
        notice.hidden = false;
      }
    } finally { logout.disabled = false; }
  });
  window.addEventListener("auth-expired", () => leave(true));
  window.addEventListener("agenda-restored", () => {
    leave();
    status.textContent = "Backup ripristinato. Accedi per continuare.";
    status.hidden = false;
  });
  document.addEventListener("visibilitychange", async () => {
    if (document.hidden || !authenticated) return;
    try { await api("auth/me"); }
    catch (error) { if (error.status === 401) leave(true); }
  });
  window.addEventListener("pageshow", (event) => {
    if (event.persisted) location.reload();
  });
  // Restore only after the server confirms that the JWT is still valid.
  submit.disabled = true;
  try { enter(await api("auth/me")); }
  catch (error) {
    leave();
    if (error.status !== 401) { message.textContent = error.message; message.hidden = false; }
  } finally { submit.disabled = false; }
}
