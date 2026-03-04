package io.github.mcardozo.escala.engine.validation;

import io.github.mcardozo.escala.domain.Cell;
import io.github.mcardozo.escala.domain.CellTag;
import io.github.mcardozo.escala.domain.Day;
import io.github.mcardozo.escala.domain.enums.DayType;
import io.github.mcardozo.escala.domain.Employee;
import io.github.mcardozo.escala.domain.EmployeeId;
import io.github.mcardozo.escala.domain.Schedule;
import io.github.mcardozo.escala.domain.State;
import io.github.mcardozo.escala.engine.problem.ScheduleProblem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/* Hard constraint opcional:
 * - Se alguém trabalhou no fim de semana, as compensações obrigatórias (A/B) devem existir.
 *
 * Definição A/B (versão 1):
 * - Ordena os 2 workers por EmployeeId.value()
 * - menor => Tipo A
 * - maior => Tipo B
 *
 * Datas:
 * - A: quinta antes (sat-2) e segunda depois (sat+2)
 * - B: sexta antes (sat-1) e terça depois (sat+3)
 *
 * Se a data cair fora do mês (não existir no grid), ignoramos.
 */
public final class CompensationMustExistConstraint implements HardConstraint {

    private static final String CODE = "COMPENSATION_MISSING";

    @Override
    public List<Violation> validate(Schedule schedule, ScheduleProblem problem) {
        Objects.requireNonNull(schedule, "schedule cannot be null");
        Objects.requireNonNull(problem, "problem cannot be null");

        List<Violation> violations = new ArrayList<>();

        for (Day day : problem.days()) {
            if (day.type() != DayType.SAT) {
                continue;
            }

            LocalDate saturday = day.date();

            // workers = quem está P no sábado
            List<EmployeeId> workers = new ArrayList<>();
            for (Employee e : problem.employees()) {
                if (schedule.getCell(saturday, e.id()).state() == State.P) {
                    workers.add(e.id());
                }
            }

            if (workers.size() != 2) {
                continue; // weekend constraint já acusa se estiver errado
            }

            workers.sort(Comparator.comparing(EmployeeId::value));
            EmployeeId typeA = workers.get(0);
            EmployeeId typeB = workers.get(1);

            LocalDate thuBefore = saturday.minusDays(2);
            LocalDate friBefore = saturday.minusDays(1);
            LocalDate monAfter  = saturday.plusDays(2);
            LocalDate tueAfter  = saturday.plusDays(3);

            // A
            checkFLikeIfDateExists(schedule, thuBefore, typeA, violations, "A requires thu-before");
            checkFLikeIfDateExists(schedule, monAfter,  typeA, violations, "A requires mon-after");

            // B
            checkFLikeIfDateExists(schedule, friBefore, typeB, violations, "B requires fri-before");
            checkFLikeIfDateExists(schedule, tueAfter,  typeB, violations, "B requires tue-after");
        }

        return List.copyOf(violations);
    }

    private void checkFLikeIfDateExists(Schedule schedule,
                                        LocalDate date,
                                        EmployeeId employeeId,
                                        List<Violation> out,
                                        String message) {

        Cell cell;
        try {
            cell = schedule.getCell(date, employeeId);
        } catch (IllegalArgumentException ex) {
            return; // fora do mês/grid -> ignora
        }

        // Se for TRAINING, não faz sentido exigir folga ali.
        // (Em tese, o gerador nem deveria colocar folga em TRAINING.)
        if (cell.hasTag(CellTag.TRAINING)) {
            return;
        }

        if (!isFLike(cell.state())) {
            out.add(new Violation(
                    CODE,
                    Severity.HARD,
                    date,
                    employeeId,
                    "Compensation missing: " + message + ". Expected F-like (F/FB/DO), but was: " + cell.state()
            ));
        }
    }

    private boolean isFLike(State s) {
        return s == State.F || s == State.FB || s == State.DO;
    }
}
