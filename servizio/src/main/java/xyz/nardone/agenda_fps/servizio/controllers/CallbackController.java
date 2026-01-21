package xyz.nardone.agenda_fps.servizio.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import xyz.nardone.agenda_fps.servizio.CallbackIds;
import xyz.nardone.agenda_fps.servizio.Response;
import xyz.nardone.agenda_fps.servizio.entities.Banker;
import xyz.nardone.agenda_fps.servizio.entities.Callback;
import xyz.nardone.agenda_fps.servizio.entities.Client;
import xyz.nardone.agenda_fps.servizio.repositories.BankerRepository;
import xyz.nardone.agenda_fps.servizio.repositories.CallbackRepository;
import xyz.nardone.agenda_fps.servizio.repositories.ClientRepository;

/**
 * Endpoints for creating, updating, and listing callbacks.
 */
@Controller
@RequestMapping(path = "/callbacks")
public class CallbackController {
    @Autowired
    private CallbackRepository callbackRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private BankerRepository bankerRepository;

    private static final Logger logger = LoggerFactory.getLogger(CallbackController.class);

    /**
     * Returns all callbacks.
     */
    @GetMapping(path = "/all")
    public @ResponseBody Iterable<Callback> getAll(){
        return callbackRepository.findAll();
    }

    /**
     * Returns a callback by ID.
     */
    @PostMapping(path = "/get")
    public @ResponseBody Callback getCallback(@RequestParam int callbackId){
        Callback callback = null;
        try {
            callback = callbackRepository.findById(callbackId)
                    .orElseThrow(() -> new RuntimeException("Callback con ID " + callbackId + " non trovato."));
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return callback;
    }

    /**
     * Deletes a callback by ID.
     */
    @DeleteMapping(path = "/delete")
    public @ResponseBody Response deleteCallbackById(@RequestParam Integer callbackId) {
        Response response;
        try {
            callbackRepository.deleteById(callbackId);
            response = new Response("Rimosso callback con ID " + callbackId);
            logger.info("Rimosso callback con ID " + callbackId);
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage());
            response = new Response("ERROR", "Errore nella rimozione del callback.");
        }
        return response;
    }

    /**
     * Updates an existing callback.
     */
    @PutMapping(path = "/update")
    public @ResponseBody Response updateCallback(@RequestBody CallbackIds callbackIds) {
        Response response = null;
        try {
            int resp = callbackRepository.updateCallback(
                    callbackIds.getId(),
                    callbackIds.getId_banker(),
                    callbackIds.getId_cliente(),
                    callbackIds.getData(),
                    callbackIds.getData_app(),
                    callbackIds.getNota()
            );
            if (resp == 1) {
                response = new Response("Callback con ID " + callbackIds.getId() + " aggiornato");
                logger.info("Callback con ID " + callbackIds.getId() + " aggiornato");
            } else {
                throw new RuntimeException("Callback non aggiornato");
            }
        } catch (Exception e){
            logger.error(e.getMessage());
            response = new Response("ERROR", "Callback non aggiornato. Riprovare");
        }
        return response;
    }

    /**
     * Creates a new callback.
     */
    @PutMapping(path = "/add")
    public @ResponseBody Response addCallback(@RequestBody CallbackIds c) {
        Response response;
        try {
            Banker banker = bankerRepository.findById(c.getId_banker())
                    .orElseThrow(() -> new RuntimeException("Banker con ID " + c.getId_banker() + " non trovato."));
            Client client = clientRepository.findById(c.getId_cliente())
                    .orElseThrow(() -> new RuntimeException("Cliente con ID " + c.getId_cliente() + " non trovato."));
            Callback callback = new Callback();
            callback.setBanker(banker);
            callback.setClient(client);
            callback.setData(c.getData());
            callback.setData_app(c.getData_app());
            callback.setNote(c.getNota());
            callbackRepository.save(callback);
            response = new Response("Aggiunto nuovo callback con ID " + callback.getId());
            logger.info("Aggiunto nuovo callback con ID " + callback.getId());
        } catch (RuntimeException e) {
            logger.error(e.getMessage());
            response = new Response("ERROR", "Errore Server nella aggiunta del callback");
        }
        return response;
    }

    /**
     * Returns the total number of callbacks.
     */
    @GetMapping(path = "/count")
    public @ResponseBody int getCallbacksCount(){
        return callbackRepository.countAllCallbacks();
    }

    /**
     * Returns the number of callbacks scheduled for a specific date.
     */
    @PostMapping(path="/today")
    public @ResponseBody int getTodayCallbacksCount(@RequestParam String data){
        return callbackRepository.countAllByDataEquals(data);
    }
}
