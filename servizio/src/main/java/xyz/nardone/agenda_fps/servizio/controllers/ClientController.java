package xyz.nardone.agenda_fps.servizio.controllers;

import org.springframework.web.bind.annotation.*;
import xyz.nardone.agenda_fps.servizio.ClientIds;
import xyz.nardone.agenda_fps.servizio.entities.Banker;
import xyz.nardone.agenda_fps.servizio.entities.Client;
import xyz.nardone.agenda_fps.servizio.repositories.BankerRepository;
import xyz.nardone.agenda_fps.servizio.repositories.CallbackRepository;
import xyz.nardone.agenda_fps.servizio.repositories.ClientRepository;
import xyz.nardone.agenda_fps.servizio.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Endpoints for creating, updating, and listing clients.
 */
@Controller
@RequestMapping(path="/clients")
public class ClientController {
    @Autowired
    private ClientRepository clientRepository;
    
    private static final Logger logger = LoggerFactory.getLogger(ClientController.class);
    @Autowired
    private BankerRepository bankerRepository;

    @Autowired
    private CallbackRepository callbackRepository;

    /**
     * Returns all clients.
     */
    @GetMapping(path="/all")
    public @ResponseBody Iterable<Client> getAllClients(){
        return clientRepository.findAll();
    }

    /**
     * Returns a client by ID.
     */
    @PostMapping(path = "/get")
    public @ResponseBody Client getClient(@RequestParam Integer clientId){
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Banker con ID " + clientId + " non trovato."));
    }

    /**
     * Creates a new client linked to a banker.
     */
    @PutMapping(path = "/add")
    public @ResponseBody Response addClient(@RequestBody ClientIds client){
        Response response;
        try {
            Banker clientBanker = bankerRepository.findById(client.getId_banker())
                    .orElseThrow(() -> new RuntimeException("Banker con ID " + client.getId_banker() + " non trovato."));
            if (clientRepository.countClientsByNameandBankerId(client.getNome(), client.getCognome(), client.getId_banker()) != 0) {
                return response = new Response("OK", "Il cliente con nome " + client.getCognome() + " " + client.getNome() + " associato a questo FB già esiste");
            }
            Client newClient = new Client();
            newClient.setCognome(client.getCognome());
            newClient.setNome(client.getNome());
            newClient.setBanker(clientBanker);
            clientRepository.save(newClient);
            response = new Response("Aggiunto nuovo Cliente con ID " + newClient.getId() + ".");
            logger.info("Aggiunto nuovo Cliente con ID " + newClient.getId() + ".");
        }
        catch (RuntimeException e) {
            response = new Response("ERRORE", "Errore comunicazione con il server");
            logger.error(e.getMessage());
        }
        return response;
    }

    /**
     * Updates client details and banker relationship.
     */
    @PutMapping(path = "/update")
    public @ResponseBody Response updateClient(@RequestBody Client client){
        Response response;
        try {
            int resp = clientRepository.updateClient(client.getNome(), client.getCognome(), client.getId(), client.getBanker().getId());
            if (resp == 1){
                response = new Response("Cliente con ID " + client.getId() + " è stato aggiornato.");
                logger.info("Cliente con ID " + client.getId() + " aggiornato.");
            } else {
                throw new RuntimeException("Cliente non aggiornato.");
            }
        } catch (Exception e) {
            response = new Response("ERRORE","Cliente non aggiornato. Riprovare");
            logger.error(e.getClass().getName() + ": " + e.getMessage());
        }
        return response;
    }

    /**
     * Deletes a client and its callbacks.
     */
    @DeleteMapping(path="/delete")
    public @ResponseBody Response deleteClientById(@RequestParam Integer clienteId){
        Response response;
        try{
            callbackRepository.deleteCallbacksByClientId(clienteId);
            clientRepository.deleteById(clienteId);
            response = new Response("Rimosso cliente con ID " + clienteId);
            logger.info("Rimosso cliente con ID: " + clienteId);
        } catch (IllegalArgumentException e){
            logger.error(e.getMessage());
            response = new Response("ERROR", "Errore nella rimozione del cliente");
        }
        return response;
    }

    /**
     * Deletes all clients and their callbacks.
     */
    @DeleteMapping(path = "/delete_all")
    public @ResponseBody Response deleteAllClients(){
        Response response;
        try {
            callbackRepository.deleteAll();
            clientRepository.deleteAll();
            response = new Response("Rimossi tutti i clienti");
            logger.info("Rimossi tutti i clienti");
        } catch (IllegalArgumentException e){
            logger.error(e.getMessage());
            response = new Response("ERROR", "Errore nella rimozione di tutti i clienti");
        }
        return response;
    }
    
    /**
     * Returns clients for a specific banker.
     */
    @PostMapping(path="banker_clients")
    public @ResponseBody Iterable<Client> getClientsByBankerId(@RequestParam(required = false) Integer id_banker){
        return clientRepository.findClientsByBankerId(id_banker);
    }
}
