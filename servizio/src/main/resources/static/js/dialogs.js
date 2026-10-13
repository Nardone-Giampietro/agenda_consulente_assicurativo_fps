import { api, all, query } from "./api.js";
import { escape, field, select, decorate, date, euro, status, clientName, activityClientName, personName, empty, icon } from "./ui.js";
const editor = document.querySelector("#editor");
const form = document.querySelector("#editor-form");
const confirmation = document.querySelector("#confirmation");
const saveButton = document.querySelector("#save-button");
const cancelButton = document.querySelector("#editor-cancel");
const backButton = document.querySelector("#editor-back");
const editButton = document.querySelector("#enable-edit");
let editing, removing;
let saving = false;
let deleting = false;
const names = {
  clienti: "cliente",
  appuntamenti: "appuntamento",
  callback: "callback",
  esiti: "esito",
  polizze: "polizza",
};
const path = (kind) =>
  ["esiti", "polizze"].includes(kind) ? `cataloghi/${kind}` : kind;
function error(element, message) {
  element.textContent = message;
  element.hidden = !message;
}
function noteField(value) {
  return `<div class="field full"><label for="f-note">Note</label><textarea id="f-note" name="note" maxlength="5000" placeholder="Informazioni utili per il prossimo incontro">${escape(value)}</textarea></div>`;
}
export async function openAppointment(item, context, { history = null } = {}) {
  if (!context.active()) return;
  editing = { kind: "appuntamenti", id: item.id, item, context, mode: "view", history };
  const task = editing;
  backButton.hidden = !history;
  backButton.textContent = "Torna allo storico";
  const title = document.querySelector("#editor-title");
  title.textContent = "Dettagli appuntamento";
  error(document.querySelector("#editor-error"), "");
  const detail = (label, value, classes = "") =>
    `<div class="${classes}"><dt>${escape(label)}</dt><dd>${value}</dd></div>`;
  document.querySelector("#editor-fields").innerHTML =
    `<p class="detail-hint full">Per cambiare i dati, premi Modifica.</p><dl class="appointment-details full">${detail("Cliente", escape(activityClientName(item)))}${history ? "" : detail("Telefono", '<span id="appointment-phone">Caricamento…</span>')}${detail("Data appuntamento", escape(date(item.data)))}${detail("Esito", status(item.esito))}${detail("Polizza", escape(item.polizza || "Nessuna polizza"))}${detail("Premio", escape(euro(item.premio)))}${detail("Note", escape(item.note || "Nessuna nota"), "full detail-notes")}</dl>${history ? "" : '<button type="button" class="button secondary full" data-client-history>Appuntamenti del cliente</button>'}`;
  saveButton.hidden = true;
  saveButton.disabled = true;
  editButton.hidden = false;
  editButton.disabled = false;
  editButton.innerHTML = '<span data-icon="edit"></span>Modifica';
  cancelButton.textContent = "Chiudi";
  decorate(editor);
  if (!editor.open) editor.showModal();
  title.focus();
  if (!history) {
    try {
      const client = await api(`clienti/${encodeURIComponent(item.clientId)}`);
      if (editing !== task || !editor.open || !context.active()) return;
      document.querySelector("#appointment-phone").textContent = client.telefono || "Non inserito";
    } catch (e) {
      if (editing !== task || !editor.open || !context.active()) return;
      document.querySelector("#appointment-phone").textContent = "Non disponibile";
      error(document.querySelector("#editor-error"), e.message);
    }
  }
}

export async function openClientHistory(clientId, context, { origin = null, page = 0 } = {}) {
  if (!context.active()) return;
  const task = { mode: "history", clientId, context, origin, page };
  editing = task;
  const title = document.querySelector("#editor-title");
  title.textContent = "Appuntamenti del cliente";
  error(document.querySelector("#editor-error"), "");
  document.querySelector("#editor-fields").innerHTML = '<p class="full loading" role="status">Caricamento dello storico…</p>';
  saveButton.hidden = true;
  saveButton.disabled = true;
  editButton.hidden = true;
  backButton.hidden = !origin;
  backButton.textContent = "Torna all’appuntamento";
  cancelButton.textContent = "Chiudi";
  if (!editor.open) editor.showModal();
  title.focus();
  const active = () => editing === task && editor.open && context.active();
  try {
    const [client, result] = await Promise.all([
      api(`clienti/${encodeURIComponent(clientId)}`),
      api(`clienti/${encodeURIComponent(clientId)}/appuntamenti?${query({ page, size: 20 })}`),
    ]);
    if (!active()) return;
    if (page > 0 && !result.items.length && result.total)
      return openClientHistory(clientId, context, { origin, page: Math.ceil(result.total / 20) - 1 });
    const pages = Math.max(1, Math.ceil(result.total / result.size));
    document.querySelector("#editor-fields").innerHTML =
      `<div class="client-history-heading full"><h3>${escape(clientName(client))}</h3>${origin ? "" : `<p class="muted">Telefono: ${escape(client.telefono || "Non inserito")}</p>`}<p class="detail-hint">${result.total} ${result.total === 1 ? "appuntamento" : "appuntamenti"} · Dal più recente</p></div>` +
      (result.items.length ? `<ul class="client-history-list full">${result.items.map(a => `<li><button type="button" class="client-history-entry" data-history-appointment="${escape(a.id)}" aria-label="Apri appuntamento del ${escape(date(a.data))}, ${escape(a.esito)}"><span class="history-date">${escape(date(a.data))}</span>${status(a.esito)}${icon("chevron")}</button></li>`).join("")}</ul>` : `<div class="full">${empty("Nessun appuntamento", "Questo cliente non ha ancora appuntamenti registrati.")}</div>`) +
      (pages > 1 ? `<nav class="history-pagination full" aria-label="Pagine dello storico"><button type="button" class="button secondary small" data-history-page="${page - 1}" ${page === 0 ? "disabled" : ""}>Precedente</button><span>Pagina ${page + 1} di ${pages}</span><button type="button" class="button secondary small" data-history-page="${page + 1}" ${page + 1 >= pages ? "disabled" : ""}>Successiva</button></nav>` : "");
    title.focus();
  } catch (e) {
    if (!active()) return;
    document.querySelector("#editor-fields").innerHTML = '<p class="detail-hint full">Lo storico non è disponibile.</p><button type="button" class="button secondary full" data-history-retry>Riprova</button>';
    error(document.querySelector("#editor-error"), e.message);
  }
}
export async function openEditor(
  kind,
  item,
  context,
  { returnToDetails = false, history = null } = {},
) {
  const previousTask = editing;
  const isActivity = ["appuntamenti", "callback"].includes(kind);
  const [clients, catalogs] = await Promise.all([
    isActivity ? all("clienti") : [],
    kind === "appuntamenti" ? context.catalogs() : { esiti: [], polizze: [] },
  ]);
  if (!context.active()) return;
  if (returnToDetails && (!editor.open || editing !== previousTask)) return;
  editing = {
    kind,
    id: item?.id,
    item,
    context,
    mode: "edit",
    returnToDetails,
    history,
  };
  backButton.hidden = true;
  const labels = {
    clienti: "Nuovo cliente",
    appuntamenti: "Nuovo appuntamento",
    callback: "Nuovo callback",
    esiti: "Nuovo esito",
    polizze: "Nuova polizza",
  };
  document.querySelector("#editor-title").textContent = item
    ? `Modifica ${names[kind]}`
    : labels[kind];
  error(document.querySelector("#editor-error"), "");
  let fields;
  const personFields =
    field(
      "Nome",
      "nome",
      "text",
      personName(item?.nome),
      'required maxlength="100" autocomplete="given-name"',
    ) +
    field(
      "Cognome",
      "cognome",
      "text",
      personName(item?.cognome),
      'required maxlength="100" autocomplete="family-name"',
    );
  if (kind === "clienti") {
    const type = item?.tipo || "PERSONA_FISICA";
    fields = `<div class="field full"><label for="f-tipo">Tipo di cliente</label><select id="f-tipo" name="tipo" required ${item ? 'disabled aria-describedby="client-type-hint"' : ""}><option value="PERSONA_FISICA" ${type === "PERSONA_FISICA" ? "selected" : ""}>Persona fisica</option><option value="AZIENDA" ${type === "AZIENDA" ? "selected" : ""}>Azienda</option></select></div>`;
    if (item) fields += '<p id="client-type-hint" class="detail-hint full">Il tipo di cliente non può essere modificato.</p>';
    fields += personFields + field("Nome legale", "nomeLegale", "text", item?.nomeLegale,
      'required maxlength="150" autocomplete="organization"', true);
    fields += field("Telefono", "telefono", "tel", item?.telefono,
      'maxlength="40" autocomplete="tel"', true);
  }
  else if (["esiti", "polizze"].includes(kind))
    fields = field(
      "Nome",
      "nome",
      "text",
      item?.nome,
      'required maxlength="150"',
      true,
    );
  else {
    fields = select(
      "Cliente",
      "clientId",
      clients,
      item?.clientId,
      "Seleziona il cliente…",
    );
    if (kind === "appuntamenti") {
      fields +=
        field(
          "Data appuntamento",
          "data",
          "date",
          item?.data || context.today(),
          "required",
        ) +
        select("Esito", "esitoId", catalogs.esiti, item?.esitoId) +
        select(
          "Polizza",
          "polizzaId",
          catalogs.polizze,
          item?.polizzaId,
          "Nessuna",
          false,
        ) +
        field(
          "Premio (€)",
          "premio",
          "number",
          item?.premio ?? "0",
          'required min="0" max="9999999999.99" step="0.01" inputmode="decimal"',
        );
      fields += noteField(item?.note);
      if (!item)
        fields += `<label class="checkbox full"><input type="checkbox" name="creaCallback" id="f-creaCallback">Prevedi un callback</label><div class="field full" id="callback-date" hidden><label for="f-dataCallback">Data callback</label><input type="date" name="dataCallback" id="f-dataCallback"><small>Puoi lasciare la data vuota e pianificarla in seguito.</small></div>`;
    } else {
      fields +=
        field(
          "Data appuntamento",
          "dataAppuntamento",
          "date",
          item?.dataAppuntamento || context.today(),
          "required",
        ) +
        field("Data callback", "data", "date", item?.data, "") +
        `<p class="muted full">Lascia la data del callback vuota se devi ancora pianificarlo.</p>` +
        noteField(item?.note);
    }
  }
  const needsClient = isActivity && !clients.length;
  const needsCatalog = kind === "appuntamenti" && !catalogs.esiti.length;
  if (needsClient)
    fields = `<p class="full">Aggiungi un cliente per registrare ${kind === "appuntamenti" ? "un appuntamento" : "un callback"}.</p><button type="button" class="button secondary full" data-dependency="clienti">Nuovo cliente</button>`;
  else if (needsCatalog)
    fields = `<p class="full">Aggiungi almeno un esito nelle impostazioni.</p><button type="button" class="button secondary full" data-dependency="esiti">Nuovo esito</button>`;
  document.querySelector("#editor-fields").innerHTML = fields;
  const save = document.querySelector("#save-button");
  save.disabled = needsClient || needsCatalog;
  save.hidden = false;
  save.textContent = "Salva";
  editButton.hidden = true;
  cancelButton.textContent = "Annulla";
  if (kind === "clienti") {
    const typeSelect = form.querySelector("[name=tipo]");
    const updateIdentity = () => {
      const company = typeSelect.value === "AZIENDA";
      for (const name of ["nome", "cognome", "nomeLegale"]) {
        const input = form.querySelector(`[name=${name}]`);
        const visible = name === "nomeLegale" ? company : !company;
        input.closest(".field").hidden = !visible;
        input.disabled = !visible;
        input.required = visible;
      }
    };
    for (const name of ["nome", "cognome", "nomeLegale"]) {
      const input = form.querySelector(`[name=${name}]`);
      input.addEventListener("blur", () => {
        input.value = name === "nomeLegale" ? input.value.trim().toLocaleUpperCase("it-IT") : personName(input.value);
      });
    }
    typeSelect.addEventListener("change", updateIdentity);
    updateIdentity();
  }
  decorate(editor);
  if (!editor.open) editor.showModal();
  if (returnToDetails) form.querySelector("input, select, textarea")?.focus();
  form
    .querySelector("[name=creaCallback]")
    ?.addEventListener("change", (event) => {
      const enabled = event.target.checked;
      document.querySelector("#callback-date").hidden = !enabled;
      if (!enabled) form.querySelector("[name=dataCallback]").value = "";
    });
}
export function askDelete(kind, item, context, reset = false) {
  removing = { kind, id: item?.id, context, reset };
  const title = reset
    ? `Elimina tutti ${kind === "clienti" ? "i clienti" : "gli appuntamenti"}`
    : `Elimina ${names[kind]}`;
  const name =
    (item?.client ? activityClientName(item) : "") ||
    (kind === "clienti" ? clientName(item) : item?.nome) ||
    date(item?.data);
  let text = reset
    ? `Questa operazione elimina tutti ${kind === "clienti" ? "i clienti" : "gli appuntamenti"}.`
    : `Vuoi eliminare ${name}?`;
  if (kind === "clienti")
    text += " Verranno eliminati anche gli appuntamenti e i callback associati.";
  if (kind === "appuntamenti") text += " I callback vengono conservati.";
  showConfirmation(title, text, reset);
}

export function askAppointmentDeletion(selection, count, summary, context) {
  removing = { kind: "appuntamenti", selection, expected: count, context };
  showConfirmation(
    "Elimina appuntamenti selezionati",
    `Verranno eliminati ${count} ${count === 1 ? "appuntamento" : "appuntamenti"}. ${summary} I clienti e i callback vengono conservati.`,
    true,
  );
}

export function askBackupRestore(file, context) {
  removing = { kind: "backup", file, context };
  showConfirmation(
    "Ripristina backup",
    "I dati del file sostituiranno clienti, cataloghi, appuntamenti e callback presenti. Una copia dell’agenda attuale verrà conservata prima del ripristino. Al termine dovrai accedere nuovamente.",
    false,
  );
  document.querySelector("#confirmation-input").innerHTML = field(
    "Scrivi RIPRISTINA per confermare", "confirm", "text", "",
    'required autocomplete="off" pattern="RIPRISTINA"',
  );
}

function showConfirmation(title, text, reset) {
  document.querySelector("#confirmation-title").textContent = title;
  document.querySelector("#confirmation-text").textContent = text;
  document.querySelector("#confirmation-input").innerHTML = reset
    ? field(
        "Scrivi ELIMINA per confermare",
        "confirm",
        "text",
        "",
        'required autocomplete="off" pattern="ELIMINA"',
      )
    : "";
  error(document.querySelector("#confirmation-error"), "");
  confirmation.querySelector("[type=submit]").disabled = false;
  confirmation.querySelector("[type=submit]").textContent = removing?.kind === "backup" ? "Ripristina" : "Elimina";
  confirmation.showModal();
}
export function setupDialogs() {
  document.querySelectorAll("[data-close]").forEach((button) =>
    button.addEventListener("click", () => {
      const dialog = button.closest("dialog");
      if (dialog === editor && saving) return;
      if (dialog === confirmation && deleting) return;
      dialog.close();
    }),
  );
  editor.addEventListener("cancel", (event) => {
    if (saving) event.preventDefault();
  });
  confirmation.addEventListener("cancel", (event) => {
    if (deleting) event.preventDefault();
  });
  cancelButton.addEventListener("click", () => {
    if (saving) return;
    if (editing?.returnToDetails)
      openAppointment(editing.item, editing.context, { history: editing.history });
    else editor.close();
  });
  editButton.addEventListener("click", async () => {
    if (saving || editing?.mode !== "view") return;
    const task = editing;
    editButton.disabled = true;
    editButton.textContent = "Caricamento…";
    try {
      await openEditor("appuntamenti", task.item, task.context, {
        returnToDetails: true,
        history: task.history,
      });
    } catch (e) {
      error(document.querySelector("#editor-error"), e.message);
    } finally {
      editButton.disabled = false;
      editButton.innerHTML = '<span data-icon="edit"></span>Modifica';
      decorate(editButton);
    }
  });
  backButton.addEventListener("click", async () => {
    const task = editing;
    if (saving || !task) return;
    if (task.history) {
      await openClientHistory(task.history.clientId, task.context, task.history);
    } else if (task.origin) {
      backButton.disabled = true;
      try {
        const item = await api(`appuntamenti/${encodeURIComponent(task.origin.id)}`);
        if (editing === task && editor.open && task.context.active())
          await openAppointment(item, task.context);
      } catch (e) {
        if (editing === task && editor.open && task.context.active())
          error(document.querySelector("#editor-error"), e.message);
      } finally { backButton.disabled = false; }
    }
  });
  editor.addEventListener("click", async (event) => {
    const button = event.target.closest("button");
    const task = editing;
    if (!button || button.disabled || saving || !task || !task.context.active()) return;
    if (button.hasAttribute("data-client-history")) {
      await openClientHistory(task.item.clientId, task.context, { origin: task.item });
    } else if (button.hasAttribute("data-history-page") || button.hasAttribute("data-history-retry")) {
      await openClientHistory(task.clientId, task.context, { origin: task.origin,
        page: button.hasAttribute("data-history-page") ? Number(button.dataset.historyPage) : task.page });
    } else if (button.hasAttribute("data-history-appointment")) {
      button.disabled = true;
      button.setAttribute("aria-busy", "true");
      try {
        const item = await api(`appuntamenti/${encodeURIComponent(button.dataset.historyAppointment)}`);
        if (editing === task && editor.open && task.context.active())
          await openAppointment(item, task.context, { history: task });
      } catch (e) {
        if (editing === task && editor.open && task.context.active())
          error(document.querySelector("#editor-error"), e.message);
      } finally { button.disabled = false; button.removeAttribute("aria-busy"); }
    }
  });
  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (
      !editor.open ||
      editing?.mode !== "edit" ||
      saving ||
      saveButton.disabled
    )
      return;
    if (!form.reportValidity()) return;
    const task = editing;
    const button = document.querySelector("#save-button");
    button.disabled = true;
    saving = true;
    cancelButton.disabled = true;
    editor.querySelector("[data-close]").disabled = true;
    button.textContent = "Salvataggio…";
    error(document.querySelector("#editor-error"), "");
    const data = Object.fromEntries(new FormData(form));
    if (task.kind === "clienti" && task.id) data.tipo = task.item.tipo;
    if (["appuntamenti", "callback"].includes(task.kind)) {
      if (!data.data) data.data = null;
      if (task.kind === "appuntamenti") {
        data.premio = Number(data.premio);
        data.polizzaId ||= null;
        data.creaCallback = Boolean(data.creaCallback);
        data.dataCallback ||= null;
      }
    }
    try {
      await api(`${path(task.kind)}${task.id ? `/${task.id}` : ""}`, {
        method: task.id ? "PUT" : "POST",
        body: data,
      });
      if (editing !== task || !task.context.active()) return;
      task.context.notice("Dati salvati.");
      if (task.returnToDetails) {
        try {
          const updated = await api(`appuntamenti/${task.id}`);
          if (editing === task && task.context.active()) await openAppointment(updated, task.context, { history: task.history });
        } catch {
          editor.close();
          task.context.notice(
            "Appuntamento salvato. Riapri i dettagli per consultarli.",
          );
        }
      } else editor.close();
      await task.context.refresh();
    } catch (e) {
      error(document.querySelector("#editor-error"), e.message);
    } finally {
      saving = false;
      button.disabled = editing?.mode === "view";
      button.textContent = "Salva";
      cancelButton.disabled = false;
      editor.querySelector("[data-close]").disabled = false;
    }
  });
  document
    .querySelector("#confirmation-form")
    .addEventListener("submit", async (event) => {
      event.preventDefault();
      if (deleting || !event.currentTarget.reportValidity()) return;
      const task = removing;
      const button = confirmation.querySelector("[type=submit]");
      deleting = true;
      button.disabled = true;
      button.textContent = task.kind === "backup" ? "Ripristino…" : "Eliminazione…";
      confirmation.querySelectorAll("[data-close]").forEach((control) => {
        control.disabled = true;
      });
      error(document.querySelector("#confirmation-error"), "");
      try {
        const params = task.selection
          ? `?${query({ ...task.selection, attesi: task.expected })}`
          : "";
        let result;
        if (task.kind === "backup") {
          const body = new FormData();
          body.set("file", task.file);
          result = await api("backup/ripristino", { method: "POST", body });
        } else {
          result = await api(
            `${path(task.kind)}${task.id ? `/${task.id}` : ""}${params}`,
            { method: "DELETE" },
          );
        }
        if (removing !== task || !task.context.active()) return;
        confirmation.close();
        if (task.kind === "backup") {
          window.dispatchEvent(new Event("agenda-restored"));
          return;
        }
        const message = task.selection
          ? result.eliminati === 1
            ? "Eliminato 1 appuntamento."
            : `Eliminati ${result.eliminati} appuntamenti.`
          : "Dati eliminati.";
        task.context.notice(message);
        await task.context.refresh();
      } catch (e) {
        error(document.querySelector("#confirmation-error"), e.message);
      } finally {
        deleting = false;
        button.disabled = false;
        button.textContent = "Elimina";
        confirmation.querySelectorAll("[data-close]").forEach((control) => {
          control.disabled = false;
        });
      }
    });
}

export function clearDialogs() {
  editing = removing = null;
  backButton.hidden = true;
  if (editor.open) editor.close();
  if (confirmation.open) confirmation.close();
  form.reset();
  document.querySelector("#editor-fields").innerHTML = "";
  document.querySelector("#editor-title").textContent = "";
  document.querySelector("#confirmation-text").textContent = "";
  document.querySelector("#confirmation-input").innerHTML = "";
  document.querySelector("#confirmation-title").textContent = "Elimina record";
  error(document.querySelector("#editor-error"), "");
  error(document.querySelector("#confirmation-error"), "");
}
