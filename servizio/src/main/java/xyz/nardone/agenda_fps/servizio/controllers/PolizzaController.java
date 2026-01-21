package xyz.nardone.agenda_fps.servizio.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import xyz.nardone.agenda_fps.servizio.Response;
import xyz.nardone.agenda_fps.servizio.entities.Polizza;
import xyz.nardone.agenda_fps.servizio.repositories.PolizzaRepository;

/**
 * Endpoints for managing polizze.
 */
@Controller
@RequestMapping(path="/polizze")
public class PolizzaController {
    @Autowired
    private PolizzaRepository polizzaRepository;

    private static final Logger logger = LoggerFactory.getLogger(PolizzaController.class);


    /**
     * Returns all polizze.
     */
    @GetMapping(path = "/all")
    public @ResponseBody Iterable<Polizza> getAll() {
        return polizzaRepository.findAll();
    }

    /**
     * Adds a new polizza if it does not already exist.
     */
    @PutMapping(path = "/add")
    public @ResponseBody Response add(@RequestParam String nomePolizza) {
        Response response;
        Polizza polizza = new Polizza();
        polizza.setPolizza(nomePolizza);
        if( polizzaRepository.findPolizzaByPolizzaLike(nomePolizza) != null ) {
            response = new Response("ERRORE", "Polizza già esistente");
        } else {
            polizzaRepository.save(polizza);
            response = new Response("Nuova Polizza aggiunta correttamente");
        };
        return response;
    }

    /**
     * Returns a polizza by ID.
     */
    @PostMapping(path = "/get")
    public @ResponseBody Polizza get(@RequestParam Integer polizzaId) {
        return polizzaRepository.findById(polizzaId)
                .orElseThrow(() -> new RuntimeException("Polizza con ID " + polizzaId + " non trovato."));
    }

    /**
     * Deletes a polizza by ID.
     */
    @DeleteMapping(path = "/delete")
    public @ResponseBody Response delete(@RequestParam Integer id) {
        Response response;
        try {
            Polizza polizza = polizzaRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Polizza non trovata"));
            polizzaRepository.deleteById(id);
            logger.info("Rimosso polizza con ID: " + id + ".");
            response = new Response("Polizza tolta correttamente");
        } catch (Exception e) {
            response = new Response("ERRORE", e.getMessage());
        }
        return response;
    }

    /**
     * Updates a polizza label.
     */
    @PutMapping(path = "/update")
    public @ResponseBody Response update(@RequestBody Polizza polizza) {
        Response response;
        if(polizzaRepository.findPolizzaByPolizzaLike(polizza.getPolizza()) != null) {
            response = new Response("ERRORE", "Polizza già esistente");
        } else {
            polizzaRepository.updatePolizza(polizza.getId(), polizza.getPolizza());
            response = new Response("Polizza aggiornata correttamente");
        }
        return response;
    }
}
