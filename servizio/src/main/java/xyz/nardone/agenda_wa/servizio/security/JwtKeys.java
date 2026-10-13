package xyz.nardone.agenda_wa.servizio.security;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

final class JwtKeys {
  private JwtKeys() {}

  static SecretKey load(Path path) throws IOException {
    Path directory = path.toAbsolutePath().getParent();
    Files.createDirectories(
        directory,
        PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
    if (!Files.exists(path)) {
      byte[] bytes = new byte[64];
      new SecureRandom().nextBytes(bytes);
      try {
        try (var file =
            FileChannel.open(
                path,
                java.util.Set.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW),
                PosixFilePermissions.asFileAttribute(
                    PosixFilePermissions.fromString("rw-------")))) {
          ByteBuffer content = ByteBuffer.wrap(Base64.getEncoder().encode(bytes));
          while (content.hasRemaining()) file.write(content);
        }
      } catch (FileAlreadyExistsException ignored) {
        // Another application instance completed the same atomic creation.
      }
    }
    if (Files.getFileStore(path).supportsFileAttributeView("posix")) {
      Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"));
    }
    byte[] bytes = Base64.getDecoder().decode(Files.readString(path).strip());
    if (bytes.length < 32) throw new IOException("La chiave JWT deve contenere almeno 32 byte.");
    return new SecretKeySpec(bytes, "HmacSHA256");
  }
}
