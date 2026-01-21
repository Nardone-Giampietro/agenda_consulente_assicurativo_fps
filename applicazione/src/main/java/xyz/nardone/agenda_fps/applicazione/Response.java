package xyz.nardone.agenda_fps.applicazione;

import java.io.Serializable;
import java.util.List;

/**
 * Simple API response wrapper used by the JavaFX client.
 */
public class Response implements Serializable{
    public String status ;
    public String message;

    public Response(String status, String message, List<Object> data) {
        this.status = status;
        this.message = message;
    }
   
    
}
