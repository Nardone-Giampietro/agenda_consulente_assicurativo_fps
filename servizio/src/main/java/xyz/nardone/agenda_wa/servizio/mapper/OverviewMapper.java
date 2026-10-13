package xyz.nardone.agenda_wa.servizio.mapper;

import static xyz.nardone.agenda_wa.servizio.util.CommonUtil.key;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Component;
import xyz.nardone.agenda_wa.servizio.DTO.overview.SummaryDTO;
import xyz.nardone.agenda_wa.servizio.repository.OutcomeTotal;

@Component
public class OverviewMapper {
  public SummaryDTO summary(List<OutcomeTotal> totals) {
    long total = 0, subscriptions = 0;
    BigDecimal premiums = BigDecimal.ZERO;
    Map<String, Long> outcomes = new TreeMap<>();
    for (OutcomeTotal row : totals) {
      String name = row.esito();
      outcomes.merge(name, row.numero(), Long::sum);
      total += row.numero();
      if (key(name).equals("sottoscrizione")) subscriptions += row.numero();
      premiums = premiums.add(row.premi());
    }
    return new SummaryDTO(total, subscriptions, premiums, outcomes);
  }
}
