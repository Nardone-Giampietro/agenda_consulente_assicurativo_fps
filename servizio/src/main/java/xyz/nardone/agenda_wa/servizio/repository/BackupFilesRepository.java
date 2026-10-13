package xyz.nardone.agenda_wa.servizio.repository;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Repository;
import xyz.nardone.agenda_wa.servizio.config.LocalPaths;

@Repository
public class BackupFilesRepository {
  private final LocalPaths paths;

  public BackupFilesRepository(LocalPaths paths) {
    this.paths = paths;
  }

  public void write(String prefix, byte[] data, int retention) throws IOException {
    Path directory = paths.backups();
    Path temp = Files.createTempFile(directory, ".backup-", ".tmp");
    try {
      if (Files.getFileStore(temp).supportsFileAttributeView("posix"))
        Files.setPosixFilePermissions(temp, PosixFilePermissions.fromString("rw-------"));
      try (var file =
          FileChannel.open(temp, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
        var bytes = ByteBuffer.wrap(data);
        while (bytes.hasRemaining()) file.write(bytes);
        file.force(true);
      }
      Path destination =
          directory.resolve(
              prefix + System.currentTimeMillis() + "-" + UUID.randomUUID() + ".json");
      try {
        Files.move(temp, destination, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(temp, destination);
      }
      var files = list(prefix);
      for (int i = retention; i < files.size(); i++) Files.delete(files.get(i));
    } finally {
      Files.deleteIfExists(temp);
    }
  }

  public List<Path> list(String prefix) throws IOException {
    try (var stream = Files.list(paths.backups())) {
      return stream
          .filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS))
          .filter(
              p ->
                  p.getFileName().toString().startsWith(prefix)
                      && p.getFileName().toString().endsWith(".json"))
          .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed())
          .toList();
    }
  }

  public Instant modified(Path file) throws IOException {
    return Files.getLastModifiedTime(file).toInstant();
  }
}
