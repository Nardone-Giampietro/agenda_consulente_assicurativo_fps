import {
  escape,
  clientName,
  activityClientName,
  clientTypeLabel,
  icon,
  euro,
  date,
  status,
  options,
  empty,
  addButton,
  actions,
  appointmentButton,
  pageHeading,
  pagination,
} from "./ui.js";
const colors = [
  "#00008f",
  "#7777d6",
  "#a6cfbd",
  "#b2b2ec",
  "#6c7184",
  "#cbabd2",
];
function chart(summary) {
  const entries = Object.entries(summary.esiti);
  if (!summary.appuntamenti)
    return empty(
      "Nessun esito da mostrare",
      "Il riepilogo si aggiornerà con i tuoi appuntamenti.",
    );
  let offset = 0;
  const circles = entries
    .map(([name, count], i) => {
      const percent = (count / summary.appuntamenti) * 100;
      const result = `<circle cx="50" cy="50" r="38" pathLength="100" fill="none" stroke="${colors[i % colors.length]}" stroke-width="12" stroke-dasharray="${percent} ${100 - percent}" stroke-dashoffset="${-offset}" transform="rotate(-90 50 50)"/>`;
      offset += percent;
      return result;
    })
    .join("");
  return `<div class="chart-layout"><svg class="donut" viewBox="0 0 100 100" role="img" aria-label="Distribuzione di ${summary.appuntamenti} appuntamenti per esito">${circles}<text x="50" y="49" text-anchor="middle" fill="#00008f" font-size="17" font-weight="600">${summary.appuntamenti}</text><text x="50" y="61" text-anchor="middle" fill="#62657d" font-size="6.5">appuntamenti</text></svg><ul class="legend">${entries.map(([name, count], i) => `<li><span class="swatch tone-${i % colors.length}"></span>${escape(name)}<strong>${count}</strong></li>`).join("")}</ul></div>`;
}
function dateTile(value) {
  if (!value)
    return `<div class="date-tile"><strong>—</strong><span>data</span></div>`;
  const d = new Date(`${value}T12:00:00`);
  return `<div class="date-tile"><strong>${d.getDate()}</strong><span>${d.toLocaleDateString("it-IT", { month: "short" })}</span></div>`;
}
function activityRows(items, kind) {
  return items
    .map(
      (item) =>
        `<div class="list-row ${kind === "appuntamenti" ? "appointment-row" : ""}" ${kind === "appuntamenti" ? `data-view-appointment="${escape(item.id)}"` : ""}>${dateTile(item.data)}<div class="list-info">${kind === "appuntamenti" ? appointmentButton(item, activityClientName(item)) : `<strong>${escape(activityClientName(item))}</strong>`}</div>${kind === "appuntamenti" ? status(item.esito) : `<button class="icon-button" data-edit="callback" data-id="${escape(item.id)}" aria-label="Modifica callback per ${escape(activityClientName(item))}">${icon("chevron")}</button>`}</div>`,
    )
    .join("");
}
export function overview(data) {
  const metric = (name, value, symbol, link, label) =>
    `<div class="metric"><div class="metric-icon">${icon(symbol)}</div><div><div class="metric-value">${value}</div><div class="metric-label">${name}</div><a class="text-link" href="#${link}">${label}${icon("arrow")}</a></div></div>`;
  return (
    pageHeading(
      "Panoramica",
      "Clienti, appuntamenti e callback della tua attività.",
      addButton("appuntamenti", "Nuovo appuntamento"),
    ) +
    `<div class="metrics">${metric("Appuntamenti", data.riepilogo.appuntamenti, "calendar", "appuntamenti", "Consulta l’agenda")}${metric("Premi totali", euro(data.riepilogo.premi), "euro", "appuntamenti", "Vedi gli appuntamenti")}</div>
    <div class="grid-dashboard"><div class="stack-main"><section class="panel"><div class="panel-head"><h2>Prossimi appuntamenti</h2><a class="text-link" href="#appuntamenti">Vedi tutti ${icon("arrow")}</a></div>${data.prossimiAppuntamenti.length ? activityRows(data.prossimiAppuntamenti, "appuntamenti") : empty("Nessun appuntamento in programma", "Aggiungi un appuntamento per organizzare i prossimi incontri.", `<button class="button secondary small" data-add="appuntamenti">Nuovo appuntamento</button>`)}</section><section class="panel reset-panel"><div class="panel-head"><h2>Clienti</h2><span class="muted">${data.clienti} ${data.clienti === 1 ? "cliente" : "clienti"}</span></div><div class="panel-body"><p class="muted">Tieni aggiornata l’anagrafica dei clienti.</p><div class="quick-actions"><button class="button secondary small" data-add="clienti">${icon("plus")}Nuovo cliente</button></div></div></section></div>
    <div class="stack"><section class="panel callback-panel"><div class="panel-head"><h2>Callback di oggi</h2><span class="count-label">${data.callbackOggi}</span></div>${data.callback.length ? activityRows(data.callback, "callback") : empty("Nessun callback per oggi", "Puoi consultare quelli già pianificati.")}<div class="panel-body"><a class="text-link" href="#callback">Tutti i callback ${icon("arrow")}</a></div></section><section class="panel"><div class="panel-head"><h2>Esiti degli appuntamenti</h2></div><div class="panel-body">${chart(data.riepilogo)}</div></section></div></div>`
  );
}
function filterField(label, name, body) {
  return `<div class="field"><label for="filter-${name}">${label}</label>${body}</div>`;
}
export function filters(route, catalogs, state) {
  const f = state.filters;
  const searchLabel =
    route === "appuntamenti"
      ? "Cerca per cliente o note"
      : "Cerca per nome, cognome o nome legale";
  const searchPlaceholder =
    route === "appuntamenti" ? "Cliente o note" : "Nome, cognome o nome legale";
  const search = `<div class="field search"><label for="filter-q">${searchLabel}</label><input id="filter-q" name="q" type="search" placeholder="${searchPlaceholder}" value="${escape(f.q)}"></div>`;
  let fields =
    route === "clienti"
      ? search
      : (route === "appuntamenti" ? search : "") +
        filterField(
          "Dal",
          "dal",
          `<input type="date" name="dal" id="filter-dal" value="${escape(f.dal)}">`,
        ) +
        filterField(
          "Al",
          "al",
          `<input type="date" name="al" id="filter-al" value="${escape(f.al)}">`,
        ) +
        (route === "appuntamenti"
          ? filterField(
              "Esito",
              "esitoId",
              `<select name="esitoId" id="filter-esitoId">${options(catalogs.esiti, f.esitoId, "Tutti gli esiti")}</select>`,
            )
          : "");
  return `<form class="filters" id="filters">${fields}<button class="button small" type="submit">Filtra</button><button class="button secondary small" type="button" data-reset-filters>Azzera</button></form>`;
}
export function listing(route, result, catalogs, state, summary) {
  const config = {
    appuntamenti: [
      "Appuntamenti",
      "Consulta e aggiorna gli incontri con i clienti.",
      "Nuovo appuntamento",
    ],
    callback: [
      "Callback",
      "Organizza i prossimi contatti con i clienti.",
      "Nuovo callback",
    ],
    clienti: [
      "Clienti",
      "Consulta e aggiorna l’anagrafica dei clienti.",
      "Nuovo cliente",
    ],
  }[route];
  const columns = {
    appuntamenti: [
      "Data",
      "Cliente",
      "Esito",
      "Polizza",
      "Premio",
      "Note",
      "Azioni",
    ],
    callback: ["Data callback", "Cliente", "Appuntamento", "Note", "Azioni"],
    clienti: ["Cliente", "Tipo", "Telefono", "Azioni"],
  }[route];
  const rows = result.items
    .map((item) => {
      let cells;
      const isToday =
        route === "callback" && state.today && item.data === state.today;
      if (route === "appuntamenti" || route === "callback") {
        cells = `<td>${route === "appuntamenti" ? appointmentButton(item, date(item.data)) : `<span class="callback-date">${date(item.data)}${isToday ? '<span class="status callback-today-label">Oggi</span>' : ""}</span>`}</td><td><strong>${escape(activityClientName(item))}</strong></td>`;
        cells +=
          route === "appuntamenti"
            ? `<td>${status(item.esito)}</td><td>${escape(item.polizza || "—")}</td><td class="money">${euro(item.premio)}</td>`
            : `<td>${date(item.dataAppuntamento)}</td>`;
        cells += `<td class="note-cell" title="${escape(item.note)}">${escape(item.note || "—")}</td>`;
      } else {
        cells = `<td><button type="button" class="appointment-open client-open" data-view-client="${escape(item.id)}" aria-label="Appuntamenti di ${escape(clientName(item))}">${escape(clientName(item))}</button><span class="client-phone-mobile">${escape(item.telefono || "—")}</span></td><td><span class="status neutral">${clientTypeLabel(item)}</span></td><td class="client-phone-cell">${escape(item.telefono || "—")}</td>`;
      }
      return `<tr ${route === "appuntamenti" ? `class="appointment-row" data-view-appointment="${escape(item.id)}"` : route === "clienti" ? `class="client-row" data-view-client="${escape(item.id)}"` : isToday ? 'class="callback-today"' : ""}>${cells}<td>${actions(route, item)}</td></tr>`;
    })
    .join("");
  const hasFilters = Object.values(state.filters).some(Boolean);
  const content = result.items.length
    ? `<div class="table-scroll"><table><caption class="sr-only">${config[0]}</caption><thead><tr>${columns.map((c) => `<th scope="col" ${route === "clienti" && c === "Telefono" ? 'class="client-phone-cell"' : ""}>${c}</th>`).join("")}</tr></thead><tbody>${rows}</tbody></table></div>`
    : empty(
        hasFilters ? "Nessun risultato" : `L’elenco è vuoto`,
        hasFilters
          ? "Modifica o azzera i filtri per vedere altri record."
          : `Usa “${config[2]}” per aggiungere il primo record.`,
        hasFilters
          ? ""
          : `<button class="button secondary small" data-add="${route}">${config[2]}</button>`,
      );
  return (
    pageHeading(...config.slice(0, 2), addButton(route, config[2])) +
    `<section class="panel">${filters(route, catalogs, state)}${content}${pagination(result)}</section>` +
    (summary
      ? `<div class="summary-strip"><span><strong>${summary.appuntamenti}</strong> appuntamenti</span><span><strong>${summary.sottoscrizioni}</strong> sottoscrizioni</span><span><strong>${euro(summary.premi)}</strong> premi nel periodo</span></div>`
      : "")
  );
}
export function settings(catalogs, openCatalogs = new Set()) {
  const panel = (kind, label, cta) => {
    const expanded = openCatalogs.has(kind);
    return `<section class="panel catalog-panel"><div class="panel-head catalog-heading"><h2><button type="button" class="catalog-toggle" data-catalog-toggle="${kind}" aria-expanded="${expanded}" aria-controls="catalog-${kind}"><span>${label}</span>${icon("chevron")}</button></h2><button class="button secondary small" data-add="${kind}">${icon("plus")}${cta}</button></div><div class="catalog-content" id="catalog-${kind}" aria-hidden="${!expanded}" ${expanded ? "" : "inert"}><div class="catalog-content-inner"><ul class="catalog-list">${catalogs[kind].map((item) => `<li><span>${escape(item.nome)}</span>${actions(kind, item)}</li>`).join("")}</ul>${catalogs[kind].length ? "" : empty("L’elenco è vuoto", "Aggiungi un valore per usarlo negli appuntamenti.")}</div></div></section>`;
  };
  return (
    pageHeading(
      "Impostazioni",
      "Gestisci cataloghi e copie dei dati della tua agenda.",
    ) +
    `<div class="settings-grid">${panel("esiti", "Esiti degli appuntamenti", "Nuovo esito")}${panel("polizze", "Polizze", "Nuova polizza")}</div>
    <section class="panel backup-panel"><div class="panel-head"><h2>Backup dell’agenda</h2></div><div class="panel-body backup-body">
      <p class="muted">Le copie automatiche vengono salvate su questo computer ogni 15 minuti quando i dati cambiano. Esporta un backup per conservarlo anche altrove.</p>
      <p id="backup-status" class="backup-status" role="status">Verifica delle copie automatiche…</p>
      <div class="backup-controls"><div><h3>Esporta i dati</h3><p class="muted">Salva clienti, cataloghi, appuntamenti e callback in un unico file.</p><button class="button secondary" id="backup-export" type="button">Esporta backup</button></div>
        <form id="backup-restore-form"><h3>Ripristina i dati</h3><div class="field"><label for="backup-file">File di backup</label><input id="backup-file" name="file" type="file" accept=".json,application/json" required aria-describedby="backup-file-hint"></div><p id="backup-file-hint" class="muted">Il ripristino sostituisce l’agenda attuale. Prima viene salvata una copia dei dati presenti.</p><button class="button secondary outline-danger" type="submit">Ripristina backup</button></form></div>
      <p class="form-error" id="backup-error" role="alert" hidden></p></div>
    </section>
    <section class="panel reset-panel"><div class="panel-head"><h2>Gestione dei dati</h2></div>
      <div class="deletion-panel"><h3>Elimina appuntamenti</h3><p class="muted">Seleziona il periodo. I clienti e i callback vengono conservati.</p>
        <form id="appointment-deletion-filters">
          <div class="deletion-controls">
            <div class="field"><label for="deletion-period">Periodo</label><select id="deletion-period" name="periodo" aria-describedby="deletion-period-hint"><option value="TUTTE">Tutte le date</option><option value="INTERVALLO">Intervallo di date</option><option value="PRIMA">Prima di una data</option><option value="DOPO">Dopo una data</option></select></div>
          </div>
          <div class="deletion-dates" id="deletion-range" hidden><div class="field"><label for="deletion-from">Dal</label><input id="deletion-from" name="dal" type="date" disabled></div><div class="field"><label for="deletion-to">Al</label><input id="deletion-to" name="al" type="date" disabled></div></div>
          <div class="field deletion-date" id="deletion-single" hidden><label for="deletion-date">Data di riferimento</label><input id="deletion-date" name="data" type="date" disabled></div>
          <p class="deletion-hint" id="deletion-period-hint">Considera gli appuntamenti di qualsiasi data.</p>
          <p class="form-error" id="deletion-error" role="alert" hidden></p>
          <button type="submit" class="button secondary" id="deletion-preview-button">Verifica appuntamenti</button>
        </form>
        <div class="deletion-preview" id="deletion-preview" hidden></div>
      </div>
      <div class="reset-row"><div><h3>Elimina tutti i clienti</h3><p>Elimina anche gli appuntamenti e i callback dei clienti.</p></div><button class="button secondary small outline-danger" data-reset="clienti">Elimina clienti</button></div></section>`
  );
}
