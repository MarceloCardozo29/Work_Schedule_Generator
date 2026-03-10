package io.github.mcardozo.escala.engine.generation.step;

import io.github.mcardozo.escala.domain.Cell;
import io.github.mcardozo.escala.domain.Day;
import io.github.mcardozo.escala.domain.Employee;
import io.github.mcardozo.escala.domain.EmployeeId;
import io.github.mcardozo.escala.domain.Schedule;
import io.github.mcardozo.escala.domain.State;
import io.github.mcardozo.escala.domain.enums.DayType;
import io.github.mcardozo.escala.domain.enums.TrainingMode;
import io.github.mcardozo.escala.engine.policy.WeekendAssignmentPolicy;
import io.github.mcardozo.escala.engine.problem.ScheduleProblem;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Step responsável por alocar finais de semana.
 *
 * Responsabilidades:
 * - percorrer todos os sábados do mês
 * - delegar a escolha dos 2 workers para WeekendAssignmentPolicy
 * - aplicar P para workers e F para os demais em sábado e domingo
 * - proteger regras de elegibilidade de fim de semana
 *
 * Regra atual de treinamento:
 * - TRAINING_1 não pode participar da escala de final de semana
 * - TRAINING_2 pode participar
 * - NONE pode participar
 *
 * Observação arquitetural:
 * - este step não decide fairness
 * - fairness/heurística continuam sendo responsabilidade da WeekendAssignmentPolicy
 * - aqui validamos o resultado e aplicamos os estados no schedule
 */
public final class AllocateWeekendsStep {

    private final WeekendAssignmentPolicy weekendPolicy;

    public AllocateWeekendsStep(WeekendAssignmentPolicy weekendPolicy) {
        this.weekendPolicy = Objects.requireNonNull(weekendPolicy, "weekendPolicy cannot be null");
    }

    /**
     * Executa a fase de alocação de finais de semana.
     *
     * Para cada sábado do mês:
     * - obtém os workers via policy
     * - valida elegibilidade
     * - aplica P aos workers e F aos demais em sábado e domingo
     */
    public void execute(Schedule schedule, ScheduleProblem problem) {
        Objects.requireNonNull(schedule, "schedule cannot be null");
        Objects.requireNonNull(problem, "problem cannot be null");

        for (Day day : problem.days()) {
            if (day.type() != DayType.SAT) {
                continue;
            }

            LocalDate saturday = day.date();
            LocalDate sunday = saturday.plusDays(1);

            long eligibleCount = problem.employees().stream()
                    .filter(this::canWorkWeekend)
                    .count();

            if (eligibleCount < 2) {
                throw new IllegalStateException(
                        "Weekend allocation requires at least 2 eligible employees for weekend starting at " + saturday
                );
            }

            List<EmployeeId> workers = weekendPolicy.pickWeekendWorkers(saturday, schedule, problem);

            if (workers == null || workers.size() != 2) {
                throw new IllegalStateException(
                        "WeekendAssignmentPolicy must return exactly 2 workers. Got: " + workers
                );
            }

            validateWeekendWorkersOrThrow(workers, problem, saturday);

            Set<EmployeeId> workerSet = new HashSet<>(workers);

            for (Employee employee : problem.employees()) {
                EmployeeId id = employee.id();

                State satState = workerSet.contains(id) ? State.P : State.F;
                State sunState = workerSet.contains(id) ? State.P : State.F;

                applyIfNotLocked(schedule, saturday, id, satState, "AUTO: weekend allocation");
                applyIfNotLocked(schedule, sunday, id, sunState, "AUTO: weekend allocation");
            }
        }
    }

    /**
     * Define se o funcionário pode participar da escala de final de semana.
     */
    private boolean canWorkWeekend(Employee employee) {
        return employee.trainingMode() != TrainingMode.TRAINING_1;
    }

    /**
     * Garante que a policy não devolveu funcionário inelegível.
     *
     * Neste estágio do projeto, o step protege a regra de domínio com fail-fast.
     * Em uma evolução futura, a própria policy deverá filtrar apenas candidatos elegíveis.
     */
    private void validateWeekendWorkersOrThrow(List<EmployeeId> workers,
                                               ScheduleProblem problem,
                                               LocalDate saturday) {

        Set<EmployeeId> workerSet = new HashSet<>(workers);

        for (Employee employee : problem.employees()) {
            if (!workerSet.contains(employee.id())) {
                continue;
            }

            if (!canWorkWeekend(employee)) {
                throw new IllegalStateException(
                        "Weekend allocation violation: employee " + employee.id().value()
                                + " with trainingMode=" + employee.trainingMode()
                                + " cannot work weekend starting at " + saturday
                );
            }
        }
    }

    /**
     * Aplica estado na célula apenas se ela não estiver travada.
     *
     * Locks manuais sempre têm prioridade sobre a alocação automática.
     */
    private void applyIfNotLocked(Schedule schedule,
                                  LocalDate date,
                                  EmployeeId employeeId,
                                  State desiredState,
                                  String note) {

        Cell current = schedule.getCell(date, employeeId);

        if (current.manualLock()) {
            return;
        }

        Cell updated = Cell.of(
                desiredState,
                false,
                current.tags(),
                note
        );

        schedule.setCell(date, employeeId, updated);
    }
}