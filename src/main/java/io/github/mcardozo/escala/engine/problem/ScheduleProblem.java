package io.github.mcardozo.escala.engine.problem;

import io.github.mcardozo.escala.domain.*;
import io.github.mcardozo.escala.engine.history.HistoryProvider;
import io.github.mcardozo.escala.engine.validation.HardConstraint;
import io.github.mcardozo.escala.engine.validation.SoftConstraint;

import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

/** Representa o “problema” completo que o motor vai resolver: mês, funcionários, dias, policy, requests, constraints e histórico.
 * Representa a "entrada completa" do motor:
 * - qual mês
 * - quais funcionários
 * - quais dias (com DayType e feriado apenas para relatório)
 * - qual policy (composições/pesos/seed)
 * - requests (edições manuais ou pedidos admin/import)
 * - constraints (hard e soft) ativas para o mês
 * - history (para saber último final de semana etc.)
 *
 * Observação:
 * - Esse record é o contrato principal entre Application e Engine.
 */
public record ScheduleProblem(
        YearMonth month,
        List<Employee> employees,
        List<Day> days,
        MonthlyPolicy policy,
        List<TimeOffRequest> requests,
        List<HardConstraint> hardConstraints,
        List<SoftConstraint> softConstraints,
        HistoryProvider history
) {
    public ScheduleProblem {
        Objects.requireNonNull(month, "ScheduleProblem.month cannot be null");
        Objects.requireNonNull(employees, "ScheduleProblem.employees cannot be null");
        Objects.requireNonNull(days, "ScheduleProblem.days cannot be null");
        Objects.requireNonNull(policy, "ScheduleProblem.policy cannot be null");
        Objects.requireNonNull(requests, "ScheduleProblem.requests cannot be null");
        Objects.requireNonNull(hardConstraints, "ScheduleProblem.hardConstraints cannot be null");
        Objects.requireNonNull(softConstraints, "ScheduleProblem.softConstraints cannot be null");
        Objects.requireNonNull(history, "ScheduleProblem.history cannot be null");

        // snapshots imutáveis para evitar mutações externas
        employees = List.copyOf(employees);
        days = List.copyOf(days);
        requests = List.copyOf(requests);
        hardConstraints = List.copyOf(hardConstraints);
        softConstraints = List.copyOf(softConstraints);
    }
}
