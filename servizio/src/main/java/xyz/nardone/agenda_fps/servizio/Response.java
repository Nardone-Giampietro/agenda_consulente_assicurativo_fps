package xyz.nardone.agenda_fps.servizio;

import java.io.Serializable;

/**
 * Simple API response payload with status and message.
 */
public class Response implements Serializable{
    private String status = "OK";
    private String message;

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Response(String status, String message) {
        this.status = status;
        this.message = message;
    }
    
    
    public Response(String message){
        this.message = message;
    }
}
