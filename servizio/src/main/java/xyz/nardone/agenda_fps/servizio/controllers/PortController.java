package xyz.nardone.agenda_fps.servizio.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import xyz.nardone.agenda_fps.servizio.PortListener;

/**
 * Exposes the runtime HTTP port for the service.
 */
@RestController
public class PortController {

    @Autowired
    private PortListener portListener;

    /**
     * Returns the port selected during startup.
     */
    @GetMapping(path = "/port")
    public @ResponseBody int getPort() {
        return portListener.getPort();
    }
}
