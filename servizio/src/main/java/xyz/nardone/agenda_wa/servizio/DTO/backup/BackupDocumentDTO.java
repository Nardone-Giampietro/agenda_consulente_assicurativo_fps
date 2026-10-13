package xyz.nardone.agenda_wa.servizio.DTO.backup;

import java.time.Instant;

public record BackupDocumentDTO(
    String formato, int versione, Instant creatoAlle, String sha256, BackupDataDTO dati) {}
