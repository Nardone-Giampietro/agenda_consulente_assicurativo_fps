package xyz.nardone.agenda_wa.servizio.controllers;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import xyz.nardone.agenda_wa.servizio.DTO.catalog.CatalogItemDTO;
import xyz.nardone.agenda_wa.servizio.DTO.catalog.CatalogRequestDTO;
import xyz.nardone.agenda_wa.servizio.mapper.CatalogMapper;
import xyz.nardone.agenda_wa.servizio.service.interfaces.CatalogService;

@RestController
@RequestMapping("/api/cataloghi/{tipo}")
public class CatalogController {
  private final CatalogMapper mapper;
  private final CatalogService catalogs;

  public CatalogController(CatalogService catalogs, CatalogMapper mapper) {
    this.mapper = mapper;
    this.catalogs = catalogs;
  }

  @GetMapping
  public List<CatalogItemDTO> list(@PathVariable String tipo) {
    return catalogs.list(tipo).stream().map(mapper::toDTO).toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CatalogItemDTO create(@PathVariable String tipo, @Valid @RequestBody CatalogRequestDTO p) {
    return mapper.toDTO(catalogs.save(tipo, null, p));
  }

  @PutMapping("/{id}")
  public CatalogItemDTO update(
      @PathVariable String tipo, @PathVariable String id, @Valid @RequestBody CatalogRequestDTO p) {
    return mapper.toDTO(catalogs.save(tipo, id, p));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String tipo, @PathVariable String id) {
    catalogs.delete(tipo, id);
  }
}
