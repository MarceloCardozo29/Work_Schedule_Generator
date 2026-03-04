package io.github.mcardozo.escala.engine.weekend;

import io.github.mcardozo.escala.domain.Employee;
import io.github.mcardozo.escala.domain.EmployeeId;
import io.github.mcardozo.escala.domain.Schedule;
import io.github.mcardozo.escala.engine.history.EmployeeWeekendHistory;
import io.github.mcardozo.escala.engine.problem.ScheduleProblem;
import io.github.mcardozo.escala.engine.policy.WeekendAssignmentPolicy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * ✅ CLASS final
 *
 * Política inicial de escolha de trabalhadores do fim de semana (2 pessoas).
 *
 * Heurística simples:
 * - Prioriza quem trabalhou menos (se houver histórico rolling)
 * - Evita quem trabalhou mais recentemente (lastWorkedWeekend)
 * - Empate: random determinístico (seed do mês)
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

        List<Employee> employees = problem.employees();

        // Ranking: menor totalWorkedWeekendsRolling primeiro; quem trabalhou no fim de semana mais recente vai pro fim.
        List<EmployeeScore> scores = new ArrayList<>();
        for (Employee e : employees) {
            Optional<EmployeeWeekendHistory> hOpt = problem.history().getWeekendHistory(e.id(), problem.month());

            int rolling = hOpt.map(EmployeeWeekendHistory::totalWorkedWeekendsRolling).orElse(0);
            LocalDate last = hOpt.map(EmployeeWeekendHistory::lastWorkedWeekend).orElse(null);

            boolean workedRecently = (last != null) && !last.isBefore(weekendDate.minusDays(7));

            scores.add(new EmployeeScore(e.id(), rolling, workedRecently));
        }

        scores.sort(Comparator
                .comparingInt(EmployeeScore::rolling)
                .thenComparing(EmployeeScore::workedRecently) // false antes de true
        );

        // Pega um “pool” dos melhores e sorteia 2 sem repetir
        int poolSize = Math.min(scores.size(), 4);
        List<EmployeeId> pool = new ArrayList<>();
        for (int i = 0; i < poolSize; i++) {
            pool.add(scores.get(i).id());
        }

        // embaralha determinístico
        java.util.Collections.shuffle(pool, rng);

        // retorna 2
        if (pool.size() < 2) {
            throw new IllegalStateException("Need at least 2 employees to pick weekend workers.");
        }

        return List.of(pool.get(0), pool.get(1));
    }

    private record EmployeeScore(EmployeeId id, int rolling, boolean workedRecently) { }
}
