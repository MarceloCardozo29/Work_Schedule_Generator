package io.github.mcardozo.escala.engine.validation;

import io.github.mcardozo.escala.domain.Cell;
import io.github.mcardozo.escala.domain.CellTag;
import io.github.mcardozo.escala.domain.Day;
import io.github.mcardozo.escala.domain.Employee;
import io.github.mcardozo.escala.domain.EmployeeId;
import io.github.mcardozo.escala.domain.Schedule;
import io.github.mcardozo.escala.domain.State;
import io.github.mcardozo.escala.engine.problem.ScheduleProblem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/*Hard constraint:
 * - Se a célula tiver a tag TRAINING, o estado deve ser P (presencial).
 *
 * Observação:
 * - A tag TRAINING será aplicada pelo builder/gerador conforme a policy do mês.
 * - Esta classe apenas valida a regra.
 */
public final class TrainingRequiresPresentialConstraint implements HardConstraint {

    private static final String CODE = "TRAINING_REQUIRES_PRESENTIAL";

    @Override
    public List<Violation> validate(Schedule schedule, ScheduleProblem problem) {
        Objects.requireNonNull(schedule, "schedule cannot be null");
        Objects.requireNonNull(problem, "problem cannot be null");

        // Se training não estiver habilitado, podemos retornar vazio direto.
        // Isso evita custo de varredura desnecessário.
        if (!problem.policy().trainingEnabled()) {
            return List.of();
        }

        List<Violation> violations = new ArrayList<>();

        for (Day day : problem.days()) {
            LocalDate date = day.date();

            for (Employee e : problem.employees()) {
                EmployeeId id = e.id();

                Cell cell = schedule.getCell(date, id);

                // Se tem a tag TRAINING, deve ser P
                if (cell.hasTag(CellTag.TRAINING) && cell.state() != State.P) {
                    violations.add(new Violation(
                            CODE,
                            Severity.HARD,
                            date,
                            id,
                            "TRAINING day requires P (presential), but was: " + cell.state()
                    ));
                }
            }
        }

        return List.copyOf(violations);
    }
}
