package xyz.nardone.agenda_fps.servizio.controllers;

import org.springframework.web.bind.annotation.*;
import xyz.nardone.agenda_fps.servizio.AppointmentStatsUtils;
import xyz.nardone.agenda_fps.servizio.EsitoLabels;
import xyz.nardone.agenda_fps.servizio.entities.Appointment;
import xyz.nardone.agenda_fps.servizio.entities.Banker;
import xyz.nardone.agenda_fps.servizio.BankerStatistics;
import xyz.nardone.agenda_fps.servizio.repositories.AppointmentRepository;
import xyz.nardone.agenda_fps.servizio.repositories.BankerRepository;
import xyz.nardone.agenda_fps.servizio.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import xyz.nardone.agenda_fps.servizio.repositories.CallbackRepository;

import java.util.List;

/**
 * Endpoints for managing bankers and their statistics.
 */
@Controller
@RequestMapping(path="/bankers")
public class BankerController {
    @Autowired
    private BankerRepository bankerRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private CallbackRepository callbackRepository;
    
    private static final Logger logger = LoggerFactory.getLogger(BankerController.class);

    /**
     * Returns a banker by ID.
     */
    @PostMapping(path = "/get")
    private @ResponseBody Banker getBankers(@RequestParam Integer bankerId) {
        Banker banker = null;
        try {
            banker = bankerRepository.findById(bankerId)
                    .orElseThrow(() -> new RuntimeException("Banker con ID " + bankerId + " non trovato."));
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return banker;
    }

    /**
     * Returns all bankers.
     */
    @GetMapping(path="/all")
    public @ResponseBody Iterable<Banker> getAllBankers(){
        return bankerRepository.findAll();
    }

    /**
     * Returns the number of bankers.
     */
    @GetMapping(path = "/count")
    public @ResponseBody int getBankersCount(){
        return bankerRepository.countAllBankers();
    }

    /**
     * Returns aggregated statistics for a banker in a date range.
     */
    @PostMapping(path = "/statistics")
    public @ResponseBody BankerStatistics getBankerStatistics(
            @RequestParam(required = false) Integer bankerId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate){
        Banker banker = null;
        int premi = 0;
        int appuntamenti = 0;
        int sottoscrizioni = 0;
        int primiAppuntamenti = 0;
        BankerStatistics bankerStatistics = new BankerStatistics();
        try{
            if (bankerId != null) {
                banker = bankerRepository.findById(bankerId)
                        .orElseThrow(() -> new RuntimeException("Banker con ID " + bankerId + " non trovato."));
            }
            List<Appointment> appointments = appointmentRepository
                    .findAppointmentByBankerIdAndDataBetweenAndEsito(bankerId, startDate, endDate, null);
            premi = AppointmentStatsUtils.sumPremi(appointments);
            appuntamenti = appointments.size();
            sottoscrizioni = AppointmentStatsUtils.countByEsito(appointments, EsitoLabels.SOTTOSCRIZIONE);
            primiAppuntamenti = AppointmentStatsUtils.countByEsito(appointments, EsitoLabels.PRIMO_APPUNTAMENTO);
            if (banker != null){
                bankerStatistics.setBankerName(banker.getNome());
                bankerStatistics.setBankerCognome(banker.getCognome());
            }
            bankerStatistics.setPremi(premi);
            bankerStatistics.setAppuntamenti(appuntamenti);
            bankerStatistics.setSottoscrizioni(sottoscrizioni);
            bankerStatistics.setPrimiAppuntamenti(primiAppuntamenti);
        } catch (RuntimeException e){
            logger.error(e.getMessage());
        }
        return bankerStatistics;
    }

    /**
     * Adds a new banker if it does not already exist.
     */
    @PutMapping(path = "/add")
    public @ResponseBody Response addBanker(@RequestBody Banker banker){
        Response response;
        try {
            if (bankerRepository.countBankersByName(banker.getNome(), banker.getCognome())!= 0) {
                return new Response("Family Banker con nome " + banker.getCognome() + " " + banker.getNome() + " già esiste.");
            }
            bankerRepository.save(banker);
            response = new Response("Aggiunto nuovo Family Banker con ID " + banker.getId() + ".");
            logger.info("Aggiunto nuovo Family Banker con ID " + banker.getId() + ".");
        } catch (RuntimeException e) {
            response = new Response("ERRORE", "Errore nella aggiunta del nuovo Family Banker.");
            logger.error(e.getMessage());
        }
        return response;
    }

    /**
     * Updates a banker.
     */
    @PutMapping(path = "/update")
    public @ResponseBody Response updateBanker(@RequestBody Banker banker){
        Response response;
        try {
            int resp = bankerRepository.updateBanker(banker.getNome(), banker.getCognome(), banker.getId());
            if (resp == 1){
                response = new Response("Family Banker con ID " + banker.getId() + " è stato aggiornato.");
                logger.info("Family Banker con ID " + banker.getId() + " aggiornato.");
            } else {
                throw new RuntimeException("Family Banker non aggiornato.");
            }
        } catch (Exception e) {
            response = new Response("ERRORE","Family Banker non aggiornato. Riprovare");
            logger.error(e.getClass().getName() + ": " + e.getMessage());
        }
        return response;
    }
    
    /**
     * Deletes a banker and associated callbacks.
     */
    @DeleteMapping(path="/delete")
    public @ResponseBody Response deleteBankerById(@RequestParam Integer bankerId){
        Response response;
        try{
            callbackRepository.deleteCallbacksByBankerId(bankerId);
            bankerRepository.deleteById(bankerId);
            response = new Response("Rimosso Family Banker con ID " + bankerId + ".");
            logger.info("Rimosso banker con ID: " + bankerId + ".");

        } catch (IllegalArgumentException e){
            logger.error(e.getMessage());
            response = new Response("ERROR", "Errore nella rimozione del Family Banker.");
        }
        return response;
    }
    
}
