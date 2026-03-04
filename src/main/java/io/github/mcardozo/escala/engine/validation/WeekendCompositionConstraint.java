package io.github.mcardozo.escala.engine.validation;

import io.github.mcardozo.escala.domain.Cell;
import io.github.mcardozo.escala.domain.Day;
import io.github.mcardozo.escala.domain.enums.DayType;
import io.github.mcardozo.escala.domain.Employee;
import io.github.mcardozo.escala.domain.EmployeeId;
import io.github.mcardozo.escala.domain.Schedule;
import io.github.mcardozo.escala.domain.State;
import io.github.mcardozo.escala.engine.problem.ScheduleProblem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/* Hard constraint:
 * - Sábado e Domingo: 2P + 2F (exatamente)
 * - Quem trabalha no fim de semana trabalha sempre presencial (P)
 *
 * Como é uma validação:
 * - Não impede o usuário de editar (manual edit não é bloqueado).
 * - Apenas retorna violações para o sistema alertar/registrar.
 */
public final class WeekendCompositionConstraint implements HardConstraint {

    // Códigos padronizados para facilitar debug e filtros em UI/logs
    private static final String CODE_WRONG_TOTALS = "WEEKEND_COMPOSITION_WRONG_TOTALS";
    private static final String CODE_INVALID_STATE = "WEEKEND_COMPOSITION_INVALID_STATE";

    @Override
    public List<Violation> validate(Schedule schedule, ScheduleProblem problem) {
        Objects.requireNonNull(schedule, "schedule cannot be null");
        Objects.requireNonNull(problem, "problem cannot be null");

        List<Violation> violations = new ArrayList<>();

        // Percorre todos os dias do problema e checa apenas SAT/SUN
        for (Day day : problem.days()) {
            if (!isWeekend(day)) {
                continue;
            }

            LocalDate date = day.date();

            int countP = 0;
            int countF = 0;

            // Valida o estado de cada funcionário no dia
            for (Employee e : problem.employees()) {
                EmployeeId id = e.id();

                Cell cell = schedule.getCell(date, id);
                if (cell == null) {
                    // Se faltar célula, é uma violação HARD (escala incompleta/inconsistente)
                    violations.add(new Violation(
                            CODE_INVALID_STATE,
                            Severity.HARD,
                            date,
                            id,
                            "Missing cell for employee on weekend day."
                    ));
                    continue;
                }

                State state = cell.state();

                // No fim de semana, pelo blueprint, só aceitamos P ou F
                if (state == State.P) {
                    countP++;
                } else if (state == State.F) {
                    countF++;
                } else {
                    // Qualquer outro estado no fim de semana viola regra dura 2P+2F
                    violations.add(new Violation(
                            CODE_INVALID_STATE,
                            Severity.HARD,
                            date,
                            id,
                            "Weekend day must be P or F, but was: " + state
                    ));
                }
            }

            // Regra dura: exatamente 2P e 2F
            if (countP != 2 || countF != 2) {
                violations.add(new Violation(
                        CODE_WRONG_TOTALS,
                        Severity.HARD,
                        date,
                        null, // violação do dia, não de um funcionário específico
                        "Weekend composition must be exactly 2P + 2F, but was: "
                                + countP + "P + " + countF + "F."
                ));
            }
        }

        return List.copyOf(violations);
    }

    private boolean isWeekend(Day day) {
        DayType type = day.type();
        return type == DayType.SAT || type == DayType.SUN;
    }
}

