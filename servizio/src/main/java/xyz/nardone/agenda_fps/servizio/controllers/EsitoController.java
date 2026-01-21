package xyz.nardone.agenda_fps.servizio.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import xyz.nardone.agenda_fps.servizio.EsitoLabels;
import xyz.nardone.agenda_fps.servizio.Response;
import xyz.nardone.agenda_fps.servizio.entities.Esito;
import xyz.nardone.agenda_fps.servizio.repositories.EsitoRepository;

/**
 * Endpoints for managing esito values.
 */
@Controller
@RequestMapping(path="/esiti")
public class EsitoController {
    @Autowired
    private EsitoRepository esitoRepository;

    /**
     * Returns all esiti.
     */
    @GetMapping(path = "/all")
    public @ResponseBody Iterable<Esito> getAll() {
        return esitoRepository.findAll();
    }

    private static final Logger logger = LoggerFactory.getLogger(EsitoController.class);


    /**
     * Adds a new esito if it does not already exist.
     */
    @PutMapping(path = "/add")
    public @ResponseBody Response add(@RequestParam String nomeEsito) {
        Response response;
        Esito esito = new Esito();
        esito.setEsito(nomeEsito);
         if( esitoRepository.findEsitoByEsitoLike(nomeEsito) != null ) {
             response = new Response("ERRORE", "Esito già esistente");
         } else {
             esitoRepository.save(esito);
             response = new Response("Nuovo esito aggiunto correttamente");
         };
        return response;
    }

    /**
     * Returns an esito by ID.
     */
    @PostMapping(path = "/get")
    public @ResponseBody Esito get(@RequestParam Integer esitoId) {
        return esitoRepository.findById(esitoId)
                .orElseThrow(() -> new RuntimeException("Esito con ID " + esitoId + " non trovato."));
    }

    /**
     * Deletes a non-reserved esito by ID.
     */
    @DeleteMapping(path = "/delete")
    public @ResponseBody Response delete(@RequestParam Integer id) {
        Response response;
        try{
            Esito esito = esitoRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Esito non trovato"));
            if (EsitoLabels.RESERVED.contains(esito.getEsito())){
                throw new IllegalArgumentException("Non è possibile rimuovere questo esito");
            }
            esitoRepository.deleteById(id);
            logger.info("Rimosso esito con ID: " + id + ".");
            response = new Response("Esito tolto correttamente");
        } catch (Exception e) {
            response = new Response("ERRORE", e.getMessage());
        }
        return response;
    }

    /**
     * Updates an esito label.
     */
    @PutMapping(path = "/update")
    public @ResponseBody Response update(@RequestBody Esito esito) {
        Response response;
        if(esitoRepository.findEsitoByEsitoLike(esito.getEsito()) != null) {
            response = new Response("ERRORE", "Esito già esistente");
        } else {
            esitoRepository.updateEsito(esito.getId(), esito.getEsito());
            response = new Response("Esito aggiornato correttamente");
        }
        return response;
    }
}
