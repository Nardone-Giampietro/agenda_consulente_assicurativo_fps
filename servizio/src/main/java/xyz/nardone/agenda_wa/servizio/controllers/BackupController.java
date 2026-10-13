package xyz.nardone.agenda_wa.servizio.controllers;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import xyz.nardone.agenda_wa.servizio.DTO.backup.*;
import xyz.nardone.agenda_wa.servizio.service.interfaces.BackupService;

@RestController
@RequestMapping("/api/backup")
public class BackupController {
  private final BackupService backups;

  public BackupController(BackupService backups) {
    this.backups = backups;
  }

  @GetMapping
  public ResponseEntity<byte[]> export() {
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=agenda-wa-backup.json")
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(backups.exportBackup());
  }

  @GetMapping("/stato")
  public BackupStatusDTO status() throws Exception {
    return backups.status();
  }

  @PostMapping(value = "/ripristino", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public RestoreResultDTO restore(@RequestParam("file") MultipartFile file) throws Exception {
    try (var input = file.getInputStream()) {
      return backups.restore(input);
    }
  }
}
