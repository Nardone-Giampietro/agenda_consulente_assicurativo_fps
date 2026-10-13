import { api } from "./api.js";
import { askBackupRestore } from "./dialogs.js";

export function setupBackups(context) {
  const panel = document.querySelector(".backup-panel");
  if (!panel) return;
  const status = panel.querySelector("#backup-status");
  const error = panel.querySelector("#backup-error");
  const download = panel.querySelector("#backup-export");
  const form = panel.querySelector("#backup-restore-form");
  api("backup/stato").then((result) => {
    if (!panel.isConnected) return;
    status.textContent = result.ultimoBackup
      ? `Ultima copia automatica: ${new Date(result.ultimoBackup).toLocaleString("it-IT")}. Copie conservate: ${result.copieAutomatiche}.`
      : "La prima copia automatica verrà salvata dopo l’avvio dell’agenda.";
  }).catch((e) => {
    if (panel.isConnected && e.name !== "AbortError") status.textContent = "Non è stato possibile verificare le copie automatiche.";
  });
  download.addEventListener("click", async () => {
    if (download.disabled) return;
    download.disabled = true;
    download.textContent = "Esportazione…";
    download.setAttribute("aria-busy", "true");
    error.hidden = true;
    try {
      const blob = await api("backup", { responseType: "blob" });
      if (!panel.isConnected || !context.active()) return;
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `agenda-wa-backup-${context.today()}.json`;
      document.body.append(link);
      link.click();
      link.remove();
      setTimeout(() => URL.revokeObjectURL(url), 5000);
      context.notice("Backup esportato.");
    } catch (e) {
      if (panel.isConnected && e.name !== "AbortError") { error.textContent = e.message; error.hidden = false; }
    } finally {
      download.disabled = false;
      download.textContent = "Esporta backup";
      download.removeAttribute("aria-busy");
    }
  });
  form.addEventListener("submit", (event) => {
    event.preventDefault();
    if (!form.reportValidity()) return;
    error.hidden = true;
    const file = form.querySelector("#backup-file").files[0];
    if (!file || !file.size) {
      error.textContent = "Scegli un file di backup dell’agenda.";
      error.hidden = false;
      return;
    }
    askBackupRestore(file, context);
  });
}
