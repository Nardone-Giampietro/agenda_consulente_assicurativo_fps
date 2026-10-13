import { api, query } from "./api.js";
import { escape, date, status, appointmentButton, activityClientName } from "./ui.js";
import { askAppointmentDeletion } from "./dialogs.js";

export function setupAppointmentDeletion(context) {
  const form = document.querySelector("#appointment-deletion-filters");
  const period = form.querySelector("#deletion-period");
  const range = form.querySelector("#deletion-range");
  const single = form.querySelector("#deletion-single");
  const hint = form.querySelector("#deletion-period-hint");
  const error = form.querySelector("#deletion-error");
  const preview = document.querySelector("#deletion-preview");
  const button = form.querySelector("#deletion-preview-button");
  let version = 0;

  function invalidate() {
    version++;
    preview.hidden = true;
    preview.replaceChildren();
    error.hidden = true;
  }

  function updatePeriod() {
    const isRange = period.value === "INTERVALLO";
    const isSingle = ["PRIMA", "DOPO"].includes(period.value);
    range.hidden = !isRange;
    single.hidden = !isSingle;
    for (const input of range.querySelectorAll("input")) {
      input.disabled = !isRange;
      input.required = isRange;
    }
    const input = single.querySelector("input");
    input.disabled = !isSingle;
    input.required = isSingle;
    hint.textContent = {
      TUTTE: "Considera gli appuntamenti di qualsiasi data.",
      INTERVALLO: "La data iniziale e la data finale sono incluse.",
      PRIMA: "Considera le date precedenti. La data scelta è esclusa.",
      DOPO: "Considera le date successive. La data scelta è esclusa.",
    }[period.value];
  }

  function description(selection) {
    const dates = {
      TUTTE: "Tutte le date",
      INTERVALLO: `Dal ${date(selection.dal)} al ${date(selection.al)}, estremi inclusi`,
      PRIMA: `Prima del ${date(selection.data)}, data esclusa`,
      DOPO: `Dopo il ${date(selection.data)}, data esclusa`,
    }[selection.periodo];
    return `${dates}.`;
  }

  form.addEventListener("input", invalidate);
  form.addEventListener("change", () => {
    invalidate();
    updatePeriod();
  });
  updatePeriod();

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (button.disabled || !form.reportValidity()) return;
    invalidate();
    const requestVersion = version;
    const selection = Object.fromEntries(new FormData(form));
    const summary = description(selection);
    button.disabled = true;
    button.textContent = "Verifica in corso…";
    button.setAttribute("aria-busy", "true");
    try {
      const result = await api(`appuntamenti/selezione?${query(selection)}`);
      if (!form.isConnected || requestVersion !== version) return;
      const count = result.total;
      const label = count === 1 ? "1 appuntamento da eliminare" : `${count} appuntamenti da eliminare`;
      const rows = result.items.map((item) =>
        `<tr class="appointment-row" data-view-appointment="${escape(item.id)}"><td>${appointmentButton(item, date(item.data))}</td><td>${escape(activityClientName(item))}</td><td>${status(item.esito)}</td></tr>`,
      ).join("");
      preview.innerHTML = count
        ? `<div class="deletion-result-heading"><p role="status">${label}</p><button type="button" class="button secondary outline-danger" data-confirm-deletion>Elimina appuntamenti</button></div><div class="table-scroll"><table><caption class="sr-only">Appuntamenti da eliminare</caption><thead><tr><th scope="col">Data</th><th scope="col">Cliente</th><th scope="col">Esito</th></tr></thead><tbody>${rows}</tbody></table></div><p class="muted">${count > result.items.length ? `Anteprima dei primi ${result.items.length} di ${count} appuntamenti. ` : ""}${escape(summary)}</p>`
        : '<p role="status">Nessun appuntamento corrisponde ai filtri. Modifica il periodo.</p>';
      preview.hidden = false;
      preview.querySelector("[data-confirm-deletion]")?.addEventListener("click", () => {
        askAppointmentDeletion(selection, count, summary, context);
      });
    } catch (e) {
      if (form.isConnected && requestVersion === version) {
        error.textContent = e.message;
        error.hidden = false;
      }
    } finally {
      button.disabled = false;
      button.textContent = "Verifica appuntamenti";
      button.removeAttribute("aria-busy");
    }
  });
}
