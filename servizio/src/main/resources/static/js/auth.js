import { api, resetSession } from "./api.js";
export async function setupAuth(onLogin, onLogout) {
  const screen = document.querySelector("#auth-screen");
  const agenda = document.querySelector("#agenda");
  const form = document.querySelector("#auth-form");
  const submit = document.querySelector("#auth-submit");
  const toggle = document.querySelector("#auth-switch");
  const password = document.querySelector("#auth-password");
  const message = document.querySelector("#auth-error");
  const status = document.querySelector("#auth-status");
  const logout = document.querySelector("#logout");
  let registering = false, busy = false, authenticated = false, expiryTimer;
  function mode(register) {
    registering = register;
    document.title = `Agenda WA · ${register ? "Registrati" : "Accedi"}`;
    document.querySelector("#auth-title").textContent = register ? "Crea il tuo account" : "Accedi alla tua agenda";
    document.querySelector("#auth-description").textContent = register ? "Registra i tuoi dati per iniziare a usare l’agenda." : "Inserisci email e password per ritrovare clienti e attività.";
    document.querySelector("#registration-fields").hidden = !register;
    form.querySelectorAll("#registration-fields input").forEach((input) => {
      input.disabled = !register;
      input.required = register;
    });
    password.minLength = register ? 12 : 1;
    password.autocomplete = register ? "new-password" : "current-password";
    password.setAttribute("aria-describedby", register ? "auth-password-hint" : "");
    password.value = "";
    document.querySelector("#auth-password-hint").hidden = !register;
    document.querySelector("#auth-switch-text").textContent = register ? "Hai già un account?" : "Non hai un account?";
    toggle.textContent = register ? "Accedi" : "Registrati";
    submit.textContent = register ? "Crea account" : "Accedi";
    message.hidden = true;
    document.querySelector("#auth-title").focus();
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
    document.querySelector("#user-name").textContent = `${profile.cognome} ${profile.nome}`;
    document.querySelector(".profile").textContent = `${profile.cognome[0] || ""}${profile.nome[0] || ""}`.toUpperCase();
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
    mode(false);
    status.textContent = expired ? "La sessione è scaduta. Accedi di nuovo per continuare." : "";
    status.hidden = !expired;
  }
  toggle.addEventListener("click", () => { if (!busy) mode(!registering); });
  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (busy || !form.reportValidity()) return;
    busy = true;
    submit.disabled = toggle.disabled = true;
    submit.setAttribute("aria-busy", "true");
    submit.textContent = registering ? "Registrazione…" : "Accesso…";
    message.hidden = true;
    try {
      const profile = await api(`auth/${registering ? "register" : "login"}`, { method: "POST", body: Object.fromEntries(new FormData(form)) });
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
      submit.disabled = toggle.disabled = false;
      submit.removeAttribute("aria-busy");
      submit.textContent = registering ? "Crea account" : "Accedi";
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
  document.addEventListener("visibilitychange", async () => {
    if (document.hidden || !authenticated) return;
    try { await api("auth/me"); }
    catch (error) { if (error.status === 401) leave(true); }
  });
  window.addEventListener("pageshow", (event) => {
    if (event.persisted) location.reload();
  });
  // Restore only after the server confirms that the JWT is still valid.
  submit.disabled = toggle.disabled = true;
  try { enter(await api("auth/me")); }
  catch (error) {
    leave();
    if (error.status !== 401) { message.textContent = error.message; message.hidden = false; }
  } finally { submit.disabled = toggle.disabled = false; }
}
