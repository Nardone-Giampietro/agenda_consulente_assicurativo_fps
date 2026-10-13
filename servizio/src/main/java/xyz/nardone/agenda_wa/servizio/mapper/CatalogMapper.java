package xyz.nardone.agenda_wa.servizio.mapper;

import static xyz.nardone.agenda_wa.servizio.util.CommonUtil.key;

import org.springframework.stereotype.Component;
import xyz.nardone.agenda_wa.servizio.DTO.catalog.CatalogItemDTO;
import xyz.nardone.agenda_wa.servizio.entity.CatalogItem;

@Component
public class CatalogMapper {
  public CatalogItem toEntity(String id, String ownerId, String type, String name) {
    return new CatalogItem(id, ownerId, type, name.strip(), key(name));
  }

  public CatalogItemDTO toDTO(CatalogItem entity) {
    return new CatalogItemDTO(entity.id(), entity.tipo(), entity.nome(), entity.chiave());
  }
}
