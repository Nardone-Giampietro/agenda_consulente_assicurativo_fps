export const escape = (value) =>
  String(value ?? "").replace(
    /[&<>"']/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        c
      ],
  );
const paths = {
  home: '<path d="m3 10 9-7 9 7v10H3z"/><path d="M9 20v-7h6v7"/>',
  calendar:
    '<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M16 3v4M8 3v4M3 11h18M8 15h2M14 15h2M8 18h2"/>',
  phone: '<path d="M7 3H3v4c0 8 6 14 14 14h4v-4l-5-2-2 3c-4-1-7-4-8-8l3-2z"/>',
  users:
    '<circle cx="9" cy="7" r="3"/><path d="M3 21v-3a6 6 0 0 1 12 0v3M16 4a3 3 0 0 1 0 6M18 15a5 5 0 0 1 3 4v2"/>',
  settings:
    '<path d="M4 6h16M4 12h16M4 18h16"/><circle cx="8" cy="6" r="2" fill="currentColor"/><circle cx="16" cy="12" r="2" fill="currentColor"/><circle cx="10" cy="18" r="2" fill="currentColor"/>',
  arrow: '<path d="M4 12h16m-6-6 6 6-6 6"/>',
  chevron: '<path d="m9 5 7 7-7 7"/>',
  left: '<path d="m15 5-7 7 7 7"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  edit: '<path d="m15 4 5 5L8 21H3v-5zM13 6l5 5"/>',
  trash: '<path d="M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7M14 10v7"/>',
  close: '<path d="m6 6 12 12M6 18 18 6"/>',
  refresh:
    '<path d="M20 7v5h-5M4 17v-5h5"/><path d="M5 8a8 8 0 0 1 14-2l1 6M4 12l1 6a8 8 0 0 0 14-2"/>',
  euro: '<path d="M18 5a7 7 0 0 0-11 7 7 7 0 0 0 11 7M4 10h11M4 14h9"/>',
  check: '<path d="m5 12 4 4L19 6"/>',
};
export const icon = (name) =>
  `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.65" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${paths[name] || paths.calendar}</svg>`;
export function decorate(root = document) {
  root.querySelectorAll("[data-icon]").forEach((el) => {
    el.innerHTML = icon(el.dataset.icon);
  });
}
export const euro = (value) =>
  new Intl.NumberFormat("it-IT", { style: "currency", currency: "EUR" }).format(
    value ?? 0,
  );
export const date = (value) =>
  value
    ? new Intl.DateTimeFormat("it-IT").format(new Date(`${value}T12:00:00`))
    : "Da pianificare";
export const status = (text) =>
  `<span class="status ${text === "Sottoscrizione" ? "positive" : text === "Rifiuto" ? "neutral" : ""}">${escape(text)}</span>`;
export const personName = (value) => String(value ?? "").trim().toLocaleLowerCase("it-IT")
  .replace(/(^|[\s'’\-])(\p{L})/gu, (_, separator, letter) => separator + letter.toLocaleUpperCase("it-IT"));
export const clientName = (item) => item?.tipo === "AZIENDA"
  ? item.nomeLegale : [personName(item?.cognome), personName(item?.nome)].filter(Boolean).join(" ");
export const activityClientName = (item) => item?.tipoCliente === "AZIENDA"
  ? item.client : personName(item?.client);
export const clientTypeLabel = (item) => item.tipo === "AZIENDA" ? "Azienda" : "Persona fisica";
export const options = (items, selected = "", placeholder = "Seleziona…") =>
  `<option value="">${escape(placeholder)}</option>${items.map((item) => `<option value="${escape(item.id)}" ${item.id === selected ? "selected" : ""}>${escape(["PERSONA_FISICA", "AZIENDA"].includes(item.tipo) ? clientName(item) : item.nome)}</option>`).join("")}`;
export function empty(title, text, action = "") {
  return `<div class="empty">${icon("calendar")}<h3>${escape(title)}</h3><p>${escape(text)}</p>${action}</div>`;
}
export const addButton = (kind, label) =>
  `<button class="button" data-add="${kind}">${icon("plus")}${label}</button>`;
export const actions = (kind, item) =>
  `<div class="actions"><button class="icon-button" data-edit="${kind}" data-id="${escape(item.id)}" aria-label="Modifica ${escape((item.client ? activityClientName(item) : "") || (kind === "clienti" ? clientName(item) : item.nome) || date(item.data))}" title="Modifica">${icon("edit")}</button><button class="icon-button delete-action" data-delete="${kind}" data-id="${escape(item.id)}" aria-label="Elimina ${escape((item.client ? activityClientName(item) : "") || (kind === "clienti" ? clientName(item) : item.nome) || date(item.data))}" title="Elimina">${icon("trash")}</button></div>`;
export const appointmentButton = (item, text) =>
  `<button type="button" class="appointment-open" data-view-appointment="${escape(item.id)}" aria-label="Dettagli appuntamento con ${escape(activityClientName(item))} del ${escape(date(item.data))}">${escape(text)}</button>`;
export function pageHeading(title, description, buttons = "") {
  return `<div class="page-heading"><div><h1>${title}</h1><p>${description}</p></div><div class="heading-actions">${buttons}</div></div>`;
}
export function pagination(result) {
  const pages = Math.max(1, Math.ceil(result.total / result.size));
  return `<div class="table-footer"><span>${result.total ? `${result.page * result.size + 1}–${Math.min((result.page + 1) * result.size, result.total)} di ${result.total}` : "0 record"}</span><div class="pagination"><button class="pager-button" data-page="${result.page - 1}" ${result.page === 0 ? "disabled" : ""} aria-label="Pagina precedente">${icon("left")}</button><span>Pagina ${result.page + 1} di ${pages}</span><button class="pager-button" data-page="${result.page + 1}" ${result.page + 1 >= pages ? "disabled" : ""} aria-label="Pagina successiva">${icon("chevron")}</button></div></div>`;
}
export function field(
  label,
  name,
  type = "text",
  value = "",
  attrs = "",
  full = false,
) {
  return `<div class="field ${full ? "full" : ""}"><label for="f-${name}">${label}</label><input id="f-${name}" name="${name}" type="${type}" value="${escape(value)}" ${attrs}></div>`;
}
export function select(
  label,
  name,
  items,
  selected,
  placeholder = "Seleziona…",
  required = true,
) {
  return `<div class="field"><label for="f-${name}">${label}</label><select id="f-${name}" name="${name}" ${required ? "required" : ""}>${options(items, selected, placeholder)}</select></div>`;
}
