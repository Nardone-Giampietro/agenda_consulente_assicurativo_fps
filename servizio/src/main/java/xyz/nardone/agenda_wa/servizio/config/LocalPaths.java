package xyz.nardone.agenda_wa.servizio.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalPaths {
  private final Path directory;

  public LocalPaths(@Value("${agenda.data-dir}") String directory) throws IOException {
    this.directory = Path.of(directory).toAbsolutePath().normalize();
    Files.createDirectories(this.directory);
    if (Files.getFileStore(this.directory).supportsFileAttributeView("posix"))
      Files.setPosixFilePermissions(this.directory, PosixFilePermissions.fromString("rwx------"));
  }

  public Path directory() {
    return directory;
  }

  public Path backups() throws IOException {
    Path backups = directory.resolve("backups");
    Files.createDirectories(backups);
    if (Files.getFileStore(backups).supportsFileAttributeView("posix"))
      Files.setPosixFilePermissions(backups, PosixFilePermissions.fromString("rwx------"));
    return backups;
  }
}
