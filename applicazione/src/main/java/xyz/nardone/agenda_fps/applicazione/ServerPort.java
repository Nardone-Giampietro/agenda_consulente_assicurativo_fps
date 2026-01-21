package xyz.nardone.agenda_fps.applicazione;

/**
 * Singleton holder for the service port discovered at runtime.
 */
public class ServerPort {
    public static final ServerPort PORT = new ServerPort();
    private int port;

    private ServerPort() {}

    public static ServerPort get() { return PORT; }

    public int getPort() { return port; }

    public void setPort(int port) { this.port = port; }
}
