package io.github.mcardozo.escala.engine.history;

import io.github.mcardozo.escala.domain.EmployeeId;
import java.time.YearMonth;
import java.util.Optional;

/* Implementação simples de HistoryProvider para fase inicial do projeto.
 * Sempre retorna vazio (sem histórico).
 *
 * Útil para:
 * - testar o motor sem banco/arquivo
 * - rodar exemplos e protótipos
 */
public final class EmptyHistoryProvider implements HistoryProvider {

    @Override
    public Optional<EmployeeWeekendHistory> getWeekendHistory(EmployeeId id, YearMonth month) {
        return Optional.empty();
    }
}
