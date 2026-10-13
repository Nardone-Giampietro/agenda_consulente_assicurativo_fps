import { setupAuth } from "./auth.js";
import { api, query } from "./api.js";
import { escape, decorate } from "./ui.js";
import { overview, listing, settings } from "./views.js";
import { setupAppointmentDeletion } from "./deletion.js";
import {
  openEditor,
  openAppointment,
  openClientHistory,
  askDelete,
  setupDialogs,
  clearDialogs,
} from "./dialogs.js";
const routes = {
  panoramica: "Panoramica",
  appuntamenti: "Appuntamenti",
  callback: "Callback",
  clienti: "Clienti",
  impostazioni: "Impostazioni",
};
const state = {
  user: null,
  route: "panoramica",
  filters: {},
  page: 0,
  items: [],
  catalogs: null,
  today: null,
  openCatalogs: new Set(),
};
let loadVersion = 0,
  toastTimer;
const main = document.querySelector("#main");
const context = {
  catalogs: async () => {
    const version = loadVersion;
    if (!state.user) throw new DOMException("Sessione cambiata", "AbortError");
    if (!state.catalogs) {
      const [esiti, polizze] = await Promise.all([
        api("cataloghi/esiti"),
        api("cataloghi/polizze"),
      ]);
      if (version !== loadVersion || !state.user) throw new DOMException("Sessione cambiata", "AbortError");
      state.catalogs = { esiti, polizze };
    }
    return state.catalogs;
  },
  today: () =>
    state.today ||
    new Intl.DateTimeFormat("sv-SE", { timeZone: "Europe/Rome" }).format(
      new Date(),
    ),
  refresh: async () => {
    state.catalogs = null;
    return await render();
  },
  active: () => Boolean(state.user),
  notice: (message) => {
    if (!state.user) return;
    clearTimeout(toastTimer);
    const el = document.querySelector("#notice");
    el.textContent = message;
    el.hidden = false;
    toastTimer = setTimeout(() => {
      el.hidden = true;
    }, 4500);
  },
};
async function render() {
  if (!state.user) return false;
  const version = ++loadVersion;
  const route = state.route;
  const refresh = document.querySelector("#refresh");
  refresh.disabled = true;
  refresh.setAttribute("aria-busy", "true");
  document.querySelector("#refresh-label").textContent = "Aggiornamento…";
  main.setAttribute("aria-busy", "true");
  try {
    const dashboard = await api(
      route === "panoramica" ? "panoramica" : "navigazione",
    );
    if (version !== loadVersion) return;
    state.today = dashboard.oggi;
    document.querySelector("#today-label").textContent = new Date(
      `${dashboard.oggi}T12:00:00`,
    ).toLocaleDateString("it-IT", {
      weekday: "long",
      day: "numeric",
      month: "long",
    });
    const badge = document.querySelector("#callback-badge");
    badge.textContent = dashboard.callbackOggi;
    badge.hidden = !dashboard.callbackOggi;
    let content;
    if (route === "panoramica") {
      state.items = [...dashboard.prossimiAppuntamenti, ...dashboard.callback];
      content = overview(dashboard);
    } else {
      const catalogs = await context.catalogs();
      if (route === "impostazioni") {
        state.items = [];
        content = settings(catalogs, state.openCatalogs);
      } else {
        const filter = query({ ...state.filters, page: state.page, size: 20 });
        const [result, summary] = await Promise.all([
          api(`${route}?${filter}`),
          route === "appuntamenti"
            ? api(`appuntamenti/riepilogo?${query(state.filters)}`)
            : null,
        ]);
        if (result.total && !result.items.length && state.page > 0) {
          state.page--;
          return await render();
        }
        state.items = result.items;
        content = listing(route, result, catalogs, state, summary);
      }
    }
    if (version !== loadVersion) return;
    main.innerHTML = content;
    decorate(main);
    if (route === "impostazioni") setupAppointmentDeletion(context);
    document.querySelector("#filters")?.addEventListener("submit", (event) => {
      event.preventDefault();
      state.filters = Object.fromEntries(new FormData(event.target));
      state.page = 0;
      render();
    });
    return true;
  } catch (e) {
    if (version === loadVersion)
      main.innerHTML = `<div class="error-panel"><h1>Agenda non disponibile</h1><p>${escape(e.message)}</p><button class="button" data-retry>Riprova</button></div>`;
    return false;
  } finally {
    if (version === loadVersion) {
      main.removeAttribute("aria-busy");
      refresh.disabled = false;
      refresh.removeAttribute("aria-busy");
      document.querySelector("#refresh-label").textContent = "Aggiorna dati";
    }
  }
}
function navigate() {
  if (!state.user) return;
  const route = location.hash.slice(1) || "panoramica";
  if (!routes[route]) {
    location.hash = "panoramica";
    return;
  }
  state.route = route;
  state.filters = {};
  state.page = 0;
  state.openCatalogs.clear();
  document.title = `Agenda WA · ${routes[route]}`;
  document.querySelectorAll("[data-route]").forEach((a) => {
    if (a.dataset.route === route) a.setAttribute("aria-current", "page");
    else a.removeAttribute("aria-current");
  });
  render();
}
document.addEventListener("click", async (event) => {
  if (!state.user) return;
  const button = event.target.closest("button");
  if (button?.disabled) return;
  const appointment = event.target.closest("[data-view-appointment]");
  const client = event.target.closest("[data-view-client]");
  try {
    if (
      appointment &&
      main.contains(appointment) &&
      (button?.hasAttribute("data-view-appointment") ||
        (!event.target.closest("button, a, input, select, textarea") &&
          window.getSelection()?.isCollapsed !== false))
    ) {
      const item = await api(
        `appuntamenti/${appointment.dataset.viewAppointment}`,
      );
      openAppointment(item, context);
      return;
    }
    if (client && main.contains(client) &&
      (button?.hasAttribute("data-view-client") ||
        (!event.target.closest("button, a, input, select, textarea") &&
          window.getSelection()?.isCollapsed !== false))) {
      await openClientHistory(client.dataset.viewClient, context);
      return;
    }
    if (!button || (!main.contains(button) && !button.dataset.dependency)) return;
    if (button.dataset.catalogToggle) {
      const kind = button.dataset.catalogToggle;
      const expanded = button.getAttribute("aria-expanded") !== "true";
      const content = document.getElementById(button.getAttribute("aria-controls"));
      if (!content || !main.contains(content)) return;
      if (expanded) state.openCatalogs.add(kind);
      else state.openCatalogs.delete(kind);
      button.setAttribute("aria-expanded", String(expanded));
      content.setAttribute("aria-hidden", String(!expanded));
      content.inert = !expanded;
    } else if (button.dataset.add) await openEditor(button.dataset.add, null, context);
    else if (button.dataset.dependency) {
      document.querySelector("#editor").close();
      await openEditor(button.dataset.dependency, null, context);
    } else if (button.dataset.edit || button.dataset.delete) {
      const kind = button.dataset.edit || button.dataset.delete;
      let item = ["esiti", "polizze"].includes(kind)
        ? state.catalogs[kind].find((i) => i.id === button.dataset.id)
        : state.items.find((i) => i.id === button.dataset.id);
      if (["appuntamenti", "callback"].includes(kind))
        item = await api(`${kind}/${button.dataset.id}`);
      if (button.dataset.edit) await openEditor(kind, item, context);
      else askDelete(kind, item, context);
    } else if (button.dataset.reset)
      askDelete(button.dataset.reset, null, context, true);
    else if (button.hasAttribute("data-reset-filters")) {
      state.filters = {};
      state.page = 0;
      render();
    } else if (button.hasAttribute("data-page")) {
      state.page = Number(button.dataset.page);
      render();
    } else if (button.hasAttribute("data-retry")) render();
  } catch (e) {
    context.notice(e.message);
  }
});
document.querySelector(".skip-link").addEventListener("click", (event) => {
  event.preventDefault();
  main.focus();
});
document.querySelector("#refresh").addEventListener("click", async () => {
  if (await context.refresh()) context.notice("Dati aggiornati.");
});
window.addEventListener("hashchange", navigate);
setupDialogs();
decorate();
setupAuth(
  (profile) => { state.user = profile; navigate(); },
  () => {
    loadVersion++;
    state.user = null;
    state.items = [];
    state.catalogs = state.today = null;
    state.filters = {};
    state.page = 0;
    state.openCatalogs.clear();
    main.innerHTML = "";
    main.removeAttribute("aria-busy");
    document.querySelector("#callback-badge").textContent = "";
    document.querySelector("#callback-badge").hidden = true;
    document.querySelector("#today-label").textContent = "";
    clearTimeout(toastTimer);
    document.querySelector("#notice").hidden = true;
    document.querySelector("#notice").textContent = "";
    clearDialogs();
  },
);
