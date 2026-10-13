package xyz.nardone.agenda_wa.servizio.controllers;

import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import xyz.nardone.agenda_wa.servizio.DTO.activity.ActivityDTO;
import xyz.nardone.agenda_wa.servizio.DTO.activity.AppointmentDTO;
import xyz.nardone.agenda_wa.servizio.DTO.activity.AppointmentPeriod;
import xyz.nardone.agenda_wa.servizio.DTO.activity.AppointmentRequestDTO;
import xyz.nardone.agenda_wa.servizio.DTO.activity.AppointmentSelectionDTO;
import xyz.nardone.agenda_wa.servizio.DTO.activity.CallbackDTO;
import xyz.nardone.agenda_wa.servizio.DTO.activity.CallbackRequestDTO;
import xyz.nardone.agenda_wa.servizio.DTO.common.DeletionDTO;
import xyz.nardone.agenda_wa.servizio.DTO.common.PageDTO;
import xyz.nardone.agenda_wa.servizio.DTO.overview.NavigationDTO;
import xyz.nardone.agenda_wa.servizio.DTO.overview.OverviewDTO;
import xyz.nardone.agenda_wa.servizio.DTO.overview.SummaryDTO;
import xyz.nardone.agenda_wa.servizio.mapper.ActivityMapper;
import xyz.nardone.agenda_wa.servizio.service.interfaces.ActivityService;
import xyz.nardone.agenda_wa.servizio.service.interfaces.OverviewService;

@RestController
@RequestMapping("/api")
public class ActivityController {
  private final ActivityMapper mapper;
  private final ActivityService activities;
  private final OverviewService overview;

  public ActivityController(
      ActivityService activities, OverviewService overview, ActivityMapper mapper) {
    this.mapper = mapper;
    this.activities = activities;
    this.overview = overview;
  }

  @GetMapping("/appuntamenti")
  public PageDTO<ActivityDTO> appointments(
      @RequestParam(required = false) LocalDate dal,
      @RequestParam(required = false) LocalDate al,
      @RequestParam(required = false) String esitoId,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return activities.appointments(dal, al, esitoId, q, page, size);
  }

  @GetMapping("/appuntamenti/riepilogo")
  public SummaryDTO summary(
      @RequestParam(required = false) LocalDate dal,
      @RequestParam(required = false) LocalDate al,
      @RequestParam(required = false) String esitoId,
      @RequestParam(required = false) String q) {
    return overview.summary(dal, al, esitoId, q);
  }

  @GetMapping("/appuntamenti/{id}")
  public ActivityDTO appointment(@PathVariable String id) {
    return activities.appointment(id);
  }

  @PostMapping("/appuntamenti")
  @ResponseStatus(HttpStatus.CREATED)
  public AppointmentDTO createAppointment(@Valid @RequestBody AppointmentRequestDTO p) {
    return mapper.toDTO(activities.saveAppointment(null, p), p);
  }

  @PutMapping("/appuntamenti/{id}")
  public AppointmentDTO updateAppointment(
      @PathVariable String id, @Valid @RequestBody AppointmentRequestDTO p) {
    return mapper.toDTO(activities.saveAppointment(id, p), p);
  }

  @DeleteMapping("/appuntamenti/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteAppointment(@PathVariable String id) {
    activities.deleteAppointment(id);
  }

  @DeleteMapping("/appuntamenti")
  public DeletionDTO deleteSelectedAppointments(
      @RequestParam AppointmentPeriod periodo,
      @RequestParam(required = false) LocalDate dal,
      @RequestParam(required = false) LocalDate al,
      @RequestParam(required = false) LocalDate data,
      @RequestParam(required = false) String esitoId,
      @RequestParam long attesi) {
    return new DeletionDTO(
        activities.deleteSelectedAppointments(
            new AppointmentSelectionDTO(periodo, dal, al, data, esitoId), attesi));
  }

  @GetMapping("/appuntamenti/selezione")
  public PageDTO<ActivityDTO> selectedAppointments(
      @RequestParam AppointmentPeriod periodo,
      @RequestParam(required = false) LocalDate dal,
      @RequestParam(required = false) LocalDate al,
      @RequestParam(required = false) LocalDate data,
      @RequestParam(required = false) String esitoId) {
    return activities.selectedAppointments(
        new AppointmentSelectionDTO(periodo, dal, al, data, esitoId));
  }

  @GetMapping("/callback")
  public PageDTO<ActivityDTO> callbacks(
      @RequestParam(required = false) LocalDate dal,
      @RequestParam(required = false) LocalDate al,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return activities.callbacks(dal, al, page, size);
  }

  @GetMapping("/callback/{id}")
  public ActivityDTO callback(@PathVariable String id) {
    return activities.callback(id);
  }

  @PostMapping("/callback")
  @ResponseStatus(HttpStatus.CREATED)
  public CallbackDTO createCallback(@Valid @RequestBody CallbackRequestDTO p) {
    return mapper.toDTO(activities.saveCallback(null, p));
  }

  @PutMapping("/callback/{id}")
  public CallbackDTO updateCallback(
      @PathVariable String id, @Valid @RequestBody CallbackRequestDTO p) {
    return mapper.toDTO(activities.saveCallback(id, p));
  }

  @DeleteMapping("/callback/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteCallback(@PathVariable String id) {
    activities.deleteCallback(id);
  }

  @GetMapping("/navigazione")
  public NavigationDTO navigation() {
    return overview.navigation();
  }

  @GetMapping("/panoramica")
  public OverviewDTO overview() {
    return overview.overview();
  }
}
