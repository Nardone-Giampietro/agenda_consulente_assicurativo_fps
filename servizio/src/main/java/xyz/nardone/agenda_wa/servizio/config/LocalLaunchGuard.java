package xyz.nardone.agenda_wa.servizio.config;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;

/** Generated native launcher starts this Java guard; no custom startup scripts. */
public final class LocalLaunchGuard {
  private static FileChannel channel;
  private static FileLock lock;

  private LocalLaunchGuard() {}

  public static boolean acquire(Path directory, int port) throws Exception {
    Files.createDirectories(
        directory,
        PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
    Path path = directory.resolve("app.lock");
    channel =
        FileChannel.open(
            path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
    lock = channel.tryLock();
    if (lock == null) {
      channel.close();
      channel = null;
      try {
        if (!"false".equalsIgnoreCase(System.getenv("AGENDA_OPEN_BROWSER")))
          BrowserLaunch.open(Integer.parseInt(Files.readString(path).strip()));
      } catch (Exception e) {
        System.err.println("Agenda WA è già in esecuzione. Apri http://localhost:" + port);
      }
      return false;
    }
    channel.truncate(0);
    ByteBuffer content =
        ByteBuffer.wrap(Integer.toString(port).getBytes(StandardCharsets.US_ASCII));
    while (content.hasRemaining()) channel.write(content);
    channel.force(true);
    Runtime.getRuntime()
        .addShutdownHook(new Thread(LocalLaunchGuard::release, "agenda-wa-launcher-close"));
    return true;
  }

  public static void release() {
    try {
      if (lock != null && lock.isValid()) lock.release();
      if (channel != null) channel.close();
    } catch (java.io.IOException ignored) {
    }
  }
}
