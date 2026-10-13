package xyz.nardone.agenda_wa.servizio.service.interfaces;

import java.util.List;
import xyz.nardone.agenda_wa.servizio.DTO.catalog.CatalogRequestDTO;
import xyz.nardone.agenda_wa.servizio.entity.CatalogItem;

public interface CatalogService {
  List<CatalogItem> list(String type);

  CatalogItem item(String type, String id);

  void lock(String type, String id);

  CatalogItem save(String type, String id, CatalogRequestDTO request);

  void delete(String type, String id);

  void initialize(String ownerId);
}
