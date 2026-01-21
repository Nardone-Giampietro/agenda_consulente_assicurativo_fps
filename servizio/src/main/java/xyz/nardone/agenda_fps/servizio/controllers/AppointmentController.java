package xyz.nardone.agenda_fps.servizio.controllers;

import java.util.List;

import xyz.nardone.agenda_fps.servizio.AppointmentIds;
import xyz.nardone.agenda_fps.servizio.AppointmentStatsUtils;
import xyz.nardone.agenda_fps.servizio.AppointmentsStatistics;
import xyz.nardone.agenda_fps.servizio.BankerAppointmentsStatistics;
import xyz.nardone.agenda_fps.servizio.EsitoLabels;
import xyz.nardone.agenda_fps.servizio.Response;
import xyz.nardone.agenda_fps.servizio.entities.Appointment;
import xyz.nardone.agenda_fps.servizio.entities.Banker;
import xyz.nardone.agenda_fps.servizio.entities.Client;
import xyz.nardone.agenda_fps.servizio.repositories.AppointmentRepository;
import xyz.nardone.agenda_fps.servizio.repositories.BankerRepository;
import xyz.nardone.agenda_fps.servizio.repositories.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Endpoints for managing appointments and related statistics.
 */
@Controller
@RequestMapping(path = "/appointments")
public class AppointmentController {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private BankerRepository bankerRepository;

    @Autowired
    private ClientRepository clientRepository;

    private static final Logger logger = LoggerFactory.getLogger(AppointmentController.class);

    /**
     * Returns all appointments.
     */
    @GetMapping(path = "/all")
    public @ResponseBody Iterable<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    /**
     * Returns a single appointment as an ID-based DTO.
     */
    @PostMapping(path = "/get")
    public @ResponseBody AppointmentIds getAppointment(@RequestParam Integer appointmentId) {
        AppointmentIds appointmentIds = new AppointmentIds();
        try{
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new RuntimeException("Appuntamento con ID " + appointmentId + " non trovato."));
            appointmentIds.setId(appointment.getId());
            appointmentIds.setData(appointment.getData());
            appointmentIds.setEsito(appointment.getEsito());
            appointmentIds.setId_banker(appointment.getBanker().getId());
            appointmentIds.setId_cliente(appointment.getClient().getId());
            appointmentIds.setNote(appointment.getNote());
            appointmentIds.setPolizza(appointment.getPolizza());
            appointmentIds.setPremio(appointment.getPremio());
        } catch (RuntimeException e) {
            logger.error(e.getMessage());
        }
        return appointmentIds;
    }

    /**
     * Returns aggregated statistics for a single banker.
     */
    @PostMapping(path = "banker_statistics")
    public @ResponseBody BankerAppointmentsStatistics getBankerAppointmentsStatistics(@RequestParam Integer id_banker) {
        BankerAppointmentsStatistics appointmentsStatistics = null;
        int totPremi = 0;
        int totAppuntamenti = 0;
        int totSottoscrizioni = 0;
        try {
            Banker banker = bankerRepository.findById(id_banker)
                    .orElseThrow(() -> new RuntimeException("Banker con ID " + id_banker + " non trovato."));
            String bankerNome = banker.getNome();
            String bankerCognome = banker.getCognome();
            List<Appointment> appointments = banker.getAppointments();
            totPremi = AppointmentStatsUtils.sumPremi(appointments);
            totAppuntamenti = appointments.size();
            totSottoscrizioni = AppointmentStatsUtils.countByEsito(appointments, EsitoLabels.SOTTOSCRIZIONE);
            appointmentsStatistics = new BankerAppointmentsStatistics(bankerNome, bankerCognome,
                    totPremi, totSottoscrizioni, totAppuntamenti);
        } catch (RuntimeException e) {
            logger.error(e.getMessage());
        }
        return appointmentsStatistics;
    }


    /**
     * Deletes an appointment by ID.
     */
    @DeleteMapping(path = "/delete")
    public @ResponseBody Response deleteAppointmentById(@RequestParam Integer appointmentId) {
        Response response;
        try {
            appointmentRepository.deleteById(appointmentId);
            response = new Response("Rimosso appuntamento con ID " + appointmentId);
            logger.info("Rimosso appuntamento con ID " + appointmentId);
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage());
            response = new Response("ERROR", "Errore nella rimozione dell'appuntamento");
        }
        return response;
    }

    /**
     * Deletes all appointments.
     */
    @DeleteMapping(path = "/delete_all")
    public @ResponseBody Response deleteAllAppointments() {
        Response response;
        try {
            appointmentRepository.deleteAll();
            response = new Response("Rimossi tutti gli appuntamenti");
            logger.info("Rimossi tutti gli appuntamententi");
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage());
            response = new Response("ERROR", "Errore nella rimozione di tutti gli appuntamententi");
        }
        return response;
    }

    /**
     * Updates an existing appointment.
     */
    @PutMapping(path = "/update")
    public @ResponseBody Response updateAppointment(@RequestBody AppointmentIds appointmentIds) {
        Response response = null;
        try {
            int resp = appointmentRepository.updateAppointment(
                    appointmentIds.getId(),
                    appointmentIds.getId_banker(),
                    appointmentIds.getId_cliente(),
                    appointmentIds.getData(),
                    appointmentIds.getEsito(),
                    appointmentIds.getPolizza(),
                    appointmentIds.getPremio(),
                    appointmentIds.getNote()
            );
            if (resp == 1) {
                response = new Response("Appuntamento con ID " + appointmentIds.getId() + " aggiornato");
                logger.info("Appuntamento con ID " + appointmentIds.getId() + " aggiornato");
            } else {
                throw new RuntimeException("Appuntamento non aggiornato");
            }
        } catch (Exception e){
            logger.error(e.getMessage());
            response = new Response("ERROR", "Appuntamento non aggiornato. Riprovare");
        }
        return response;
    }

    /**
     * Creates a new appointment.
     */
    @PutMapping(path = "/add")
    public @ResponseBody Response addAppointment(@RequestBody AppointmentIds a) {
        Response response;
        try {
            Banker banker = bankerRepository.findById(a.getId_banker())
                    .orElseThrow(() -> new RuntimeException("Banker con ID " + a.getId_banker() + " non trovato."));
            Client client = clientRepository.findById(a.getId_cliente())
                    .orElseThrow(() -> new RuntimeException("Cliente con ID " + a.getId_cliente() + " non trovato."));
            Appointment newAppointment = new Appointment();
            newAppointment.setBanker(banker);
            newAppointment.setClient(client);
            newAppointment.setData(a.getData());
            newAppointment.setEsito(a.getEsito());
            newAppointment.setPolizza(a.getPolizza());
            newAppointment.setPremio(a.getPremio());
            newAppointment.setNote(a.getNote());
            appointmentRepository.save(newAppointment);
            response = new Response("Aggiunto nuovo appuntamento con ID " + newAppointment.getId());
            logger.info("Aggiunto nuovo appuntamento con ID " + newAppointment.getId());
        } catch (RuntimeException e) {
            logger.error(e.getMessage());
            response = new Response("ERROR", "Errore Server nella aggiunta dell'appuntamento");
        }
        return response;
    }

    /**
     * Returns aggregated statistics for optional filters (banker, date range, outcome).
     */
    @PostMapping(path = "/statistics")
    public @ResponseBody AppointmentsStatistics getStatistics(
            @RequestParam(required = false) Integer bankerId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String esito
    ) {
        AppointmentsStatistics appointmentsStatistics;
        List<Appointment> appointments = appointmentRepository
                .findAppointmentByBankerIdAndDataBetweenAndEsito(bankerId, startDate, endDate, esito);
        int appuntamenti = appointments.size();
        int sottoscrizioni = AppointmentStatsUtils.countByEsito(appointments, EsitoLabels.SOTTOSCRIZIONE);
        int premi = AppointmentStatsUtils.sumPremi(appointments);
        appointmentsStatistics = new AppointmentsStatistics(premi, sottoscrizioni, appuntamenti);
        return appointmentsStatistics;
    }

    /**
     * Returns appointments filtered by banker, date range, and outcome.
     */
    @PostMapping(path = "/filter")
    public @ResponseBody List<Appointment> filterAppointments(
            @RequestParam(required = false) Integer bankerId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String esito
    ) {
        List<Appointment> appointments = null;
        appointments = appointmentRepository.findAppointmentByBankerIdAndDataBetweenAndEsito(bankerId, startDate, endDate, esito);
        return appointments;
    }
}
