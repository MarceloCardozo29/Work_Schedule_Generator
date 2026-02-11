package io.github.mcardozo.escala.engine.history;

import io.github.mcardozo.escala.domain.EmployeeId;

import java.time.LocalDate;
import java.util.Objects;

/* Histórico mínimo por funcionário para ajudar na justiça do fim de semana.
 *
 * Campos:
 * - id: funcionário
 * - lastWorkedWeekend: data de referência do último fim de semana trabalhado (ex.: sábado daquele fim de semana)
 * - totalWorkedWeekendsRolling: contador acumulado/rolling (para fairness)
 */
public record EmployeeWeekendHistory(
        EmployeeId id,
        LocalDate lastWorkedWeekend,
        int totalWorkedWeekendsRolling
) {
    public EmployeeWeekendHistory {
        Objects.requireNonNull(id, "EmployeeWeekendHistory.id cannot be null");
        // lastWorkedWeekend pode ser null no início (sem histórico)
        if (totalWorkedWeekendsRolling < 0) {
            throw new IllegalArgumentException("totalWorkedWeekendsRolling cannot be negative");
        }
    }
}
