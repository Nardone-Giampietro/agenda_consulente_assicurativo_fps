package xyz.nardone.agenda_wa.servizio.config;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import xyz.nardone.agenda_wa.servizio.service.interfaces.BackupService;

@Component
public class LocalBackupScheduler {
  private final BackupService backups;
  private final boolean enabled;

  public LocalBackupScheduler(
      BackupService backups, @Value("${agenda.backup.enabled:true}") boolean enabled) {
    this.backups = backups;
    this.enabled = enabled;
  }

  @Scheduled(
      fixedDelayString = "${agenda.backup.delay-ms:900000}",
      initialDelayString = "${agenda.backup.initial-delay-ms:60000}")
  public void backup() {
    if (!enabled) return;
    try {
      backups.automaticBackup();
    } catch (Exception e) {
      LoggerFactory.getLogger(getClass())
          .warn("Copia automatica non riuscita; controllare lo spazio nella cartella dati.");
    }
  }
}
