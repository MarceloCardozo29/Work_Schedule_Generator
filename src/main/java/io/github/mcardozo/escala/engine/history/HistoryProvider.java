package io.github.mcardozo.escala.engine.history;

import io.github.mcardozo.escala.domain.EmployeeId;

import java.time.YearMonth;
import java.util.Optional;

/*Fornece histórico para o motor (ex.: último fim de semana trabalhado).
 *
 * A implementação real pode ler de arquivo, banco, etc.
 * No começo podemos ter uma implementação "vazia" que sempre retorna Optional.empty().
 */
public interface HistoryProvider {

    Optional<EmployeeWeekendHistory> getWeekendHistory(EmployeeId id, YearMonth month);
}
