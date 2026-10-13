package xyz.nardone.agenda_wa.servizio.service.interfaces;

import xyz.nardone.agenda_wa.servizio.DTO.backup.BackupStatusDTO;
import xyz.nardone.agenda_wa.servizio.DTO.backup.RestoreResultDTO;

public interface BackupService {
  byte[] exportBackup();

  RestoreResultDTO restore(byte[] file) throws Exception;

  RestoreResultDTO restore(java.io.InputStream file) throws Exception;

  BackupStatusDTO status() throws Exception;

  void automaticBackup() throws Exception;
}
