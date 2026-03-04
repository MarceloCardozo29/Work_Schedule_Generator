package io.github.mcardozo.escala.engine.validation;

import io.github.mcardozo.escala.domain.Cell;
import io.github.mcardozo.escala.domain.Day;
import io.github.mcardozo.escala.domain.Employee;
import io.github.mcardozo.escala.domain.EmployeeId;
import io.github.mcardozo.escala.domain.Schedule;
import io.github.mcardozo.escala.engine.problem.ScheduleProblem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/*Hard constraint (safety check):
 * - Garante que existe exatamente "um estado por célula" (state não pode ser null)
 * - Garante que a célula existe para todo (dia, funcionário)
 *
 * Observação:
 * - Pelo modelo, uma célula possui um único State.
 * - Então essa constraint serve como validação defensiva e detecta inconsistências.
 */
public final class OneStatePerDayConstraint implements HardConstraint {

    private static final String CODE_MISSING_CELL = "CELL_MISSING";
    private static final String CODE_NULL_STATE = "CELL_STATE_NULL";

    @Override
    public List<Violation> validate(Schedule schedule, ScheduleProblem problem) {
        Objects.requireNonNull(schedule, "schedule cannot be null");
        Objects.requireNonNull(problem, "problem cannot be null");

        List<Violation> violations = new ArrayList<>();

        for (Day day : problem.days()) {
            LocalDate date = day.date();

            for (Employee e : problem.employees()) {
                EmployeeId id = e.id();

                Cell cell;
                try {
                    cell = schedule.getCell(date, id);
                } catch (RuntimeException ex) {
                    // Se o Schedule lançar exceção por falta de célula, reportamos violação HARD
                    violations.add(new Violation(
                            CODE_MISSING_CELL,
                            Severity.HARD,
                            date,
                            id,
                            "Missing cell for employee/date in schedule grid."
                    ));
                    continue;
                }

                if (cell == null) {
                    violations.add(new Violation(
                            CODE_MISSING_CELL,
                            Severity.HARD,
                            date,
                            id,
                            "Missing cell for employee/date in schedule grid."
                    ));
                    continue;
                }

                if (cell.state() == null) {
                    violations.add(new Violation(
                            CODE_NULL_STATE,
                            Severity.HARD,
                            date,
                            id,
                            "Cell.state cannot be null."
                    ));
                }
            }
        }

        return List.copyOf(violations);
    }
}
