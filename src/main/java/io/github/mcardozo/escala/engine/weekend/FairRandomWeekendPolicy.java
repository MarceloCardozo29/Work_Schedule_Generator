package io.github.mcardozo.escala.engine.weekend;

import io.github.mcardozo.escala.domain.Employee;
import io.github.mcardozo.escala.domain.EmployeeId;
import io.github.mcardozo.escala.domain.Schedule;
import io.github.mcardozo.escala.domain.enums.TrainingMode;
import io.github.mcardozo.escala.engine.history.EmployeeWeekendHistory;
import io.github.mcardozo.escala.engine.policy.WeekendAssignmentPolicy;
import io.github.mcardozo.escala.engine.problem.ScheduleProblem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * Política de escolha de trabalhadores do fim de semana (2 pessoas).
 *
 * Heurística atual:
 * - considera apenas funcionários elegíveis para fim de semana
 * - prioriza quem trabalhou menos (rolling history)
 * - evita quem trabalhou mais recentemente
 * - empate: random determinístico com seed do mês + data do fim de semana
 *
 * Regra de elegibilidade:
 * - TRAINING_1: não pode participar
 * - TRAINING_2: pode participar
 * - NONE: pode participar
 */
public final class FairRandomWeekendPolicy implements WeekendAssignmentPolicy {

    @Override
    public List<EmployeeId> pickWeekendWorkers(LocalDate weekendDate, Schedule partial, ScheduleProblem problem) {
        Objects.requireNonNull(weekendDate, "weekendDate cannot be null");
        Objects.requireNonNull(partial, "partial cannot be null");
        Objects.requireNonNull(problem, "problem cannot be null");

        // Seed determinística: mês + data do fim de semana
        long seed = problem.policy().randomSeed() ^ weekendDate.toEpochDay();
        Random rng = new Random(seed);

        // 1) Filtra apenas funcionários elegíveis
        List<Employee> eligibleEmployees = problem.employees().stream()
                .filter(this::isEligibleForWeekend)
                .toList();

        if (eligibleEmployees.size() < 2) {
            throw new IllegalStateException(
                    "Need at least 2 eligible employees to pick weekend workers for weekend starting at " + weekendDate
            );
        }

        // 2) Monta ranking
        List<EmployeeScore> scores = new ArrayList<>();
        for (Employee employee : eligibleEmployees) {
            Optional<EmployeeWeekendHistory> historyOpt =
                    problem.history().getWeekendHistory(employee.id(), problem.month());

            int rolling = historyOpt.map(EmployeeWeekendHistory::totalWorkedWeekendsRolling).orElse(0);
            LocalDate lastWorkedWeekend = historyOpt.map(EmployeeWeekendHistory::lastWorkedWeekend).orElse(null);

            boolean workedRecently =
                    lastWorkedWeekend != null && !lastWorkedWeekend.isBefore(weekendDate.minusDays(7));

            scores.add(new EmployeeScore(employee.id(), rolling, workedRecently));
        }

        // 3) Ordena:
        // - menor rolling primeiro
        // - quem NÃO trabalhou recentemente vem antes
        scores.sort(Comparator
                .comparingInt(EmployeeScore::rolling)
                .thenComparing(EmployeeScore::workedRecently)
        );

        // 4) Pega um pool dos melhores e sorteia 2 sem repetir
        int poolSize = Math.min(scores.size(), 4);
        List<EmployeeId> pool = new ArrayList<>();

        for (int i = 0; i < poolSize; i++) {
            pool.add(scores.get(i).id());
        }

        java.util.Collections.shuffle(pool, rng);

        return List.of(pool.get(0), pool.get(1));
    }

    /**
     * Define elegibilidade para escala de fim de semana.
     */
    private boolean isEligibleForWeekend(Employee employee) {
        return employee.trainingMode() != TrainingMode.TRAINING_1;
    }

    /**
     * Score auxiliar da policy.
     *
     * rolling:
     * - total de fins de semana trabalhados no histórico rolling
     *
     * workedRecently:
     * - true se trabalhou no fim de semana imediatamente anterior
     */
    private record EmployeeScore(EmployeeId id, int rolling, boolean workedRecently) {
    }
}