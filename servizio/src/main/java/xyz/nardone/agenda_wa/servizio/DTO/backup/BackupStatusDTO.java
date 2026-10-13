package xyz.nardone.agenda_wa.servizio.DTO.backup;

import java.time.Instant;

public record BackupStatusDTO(Instant ultimoBackup, int copieAutomatiche) {}
