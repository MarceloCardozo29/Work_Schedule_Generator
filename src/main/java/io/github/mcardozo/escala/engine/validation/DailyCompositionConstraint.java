package io.github.mcardozo.escala.engine.validation;

import io.github.mcardozo.escala.domain.Cell;
import io.github.mcardozo.escala.domain.Day;
import io.github.mcardozo.escala.domain.enums.DayType;
import io.github.mcardozo.escala.domain.DayComposition;
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
 * - Em dias normais (não SAT/SUN), a composição depende de quantos estão "disponíveis".
 *
 * Definição de "disponível" (para este projeto):
 * - Se o estado é P ou H => disponível
 * - Se o estado é F / FB / DO => não disponível (folga/ausência)
 *
 * Regras do blueprint:
 * - 4 disponíveis: usar policy.compositionWhen4Available (ex.: 2P + 2H)
 * - 3 disponíveis: usar policy.compositionWhen3Available (ex.: 2P + 1H + 1F)
 *
 * Observações:
 * - Se a disponibilidade for diferente de 3 ou 4 (ex.: 2, ou 5), reportamos HARD
 *   porque a regra do sistema foi definida para o cenário de 4 funcionários.
 * - Sábado/domingo são tratados por outra constraint (WeekendCompositionConstraint).
 */
public final class DailyCompositionConstraint implements HardConstraint {

    private static final String CODE_UNSUPPORTED_AVAILABLE = "DAILY_COMPOSITION_UNSUPPORTED_AVAILABLE_COUNT";
    private static final String CODE_WRONG_TOTALS = "DAILY_COMPOSITION_WRONG_TOTALS";

    @Override
    public List<Violation> validate(Schedule schedule, ScheduleProblem problem) {
        Objects.requireNonNull(schedule, "schedule cannot be null");
        Objects.requireNonNull(problem, "problem cannot be null");

        List<Violation> violations = new ArrayList<>();

        for (Day day : problem.days()) {
            if (day.type() == DayType.SAT || day.type() == DayType.SUN) {
                continue; // fim de semana validado em outra constraint
            }

            LocalDate date = day.date();

            // Contadores do dia
            int countP = 0;
            int countH = 0;
            int countFLike = 0; // F/FB/DO

            // Disponíveis (P ou H)
            int available = 0;

            for (Employee e : problem.employees()) {
                EmployeeId id = e.id();
                Cell cell = schedule.getCell(date, id);
                State state = cell.state();

                if (state == State.P) {
                    countP++;
                    available++;
                } else if (state == State.H) {
                    countH++;
                    available++;
                } else if (isTimeOff(state)) {
                    countFLike++;
                } else {
                    // Hoje só temos esses estados, mas deixamos explícito para segurança
                    countFLike++;
                }
            }

            // Escolhe qual composição deveria ser aplicada
            DayComposition expected = expectedComposition(problem, available);

            if (expected == null) {
                violations.add(new Violation(
                        CODE_UNSUPPORTED_AVAILABLE,
                        Severity.HARD,
                        date,
                        null,
                        "Unsupported available count for NORMAL day. Expected 3 or 4 available, but was: " + available
                ));
                continue;
            }

            // Agora validamos se o número de P/H/F "bate" com a composição esperada
            // Para F, aqui consideramos F-like (F, FB, DO) como "não disponível".
            // Na composição, o requiredF representa "quantas pessoas não estão trabalhando".
            boolean ok =
                    countP == expected.requiredP()
                            && countH == expected.requiredH()
                            && countFLike == expected.requiredF();

            if (!ok) {
                violations.add(new Violation(
                        CODE_WRONG_TOTALS,
                        Severity.HARD,
                        date,
                        null,
                        "Wrong composition for NORMAL day. Expected "
                                + expected.requiredP() + "P + "
                                + expected.requiredH() + "H + "
                                + expected.requiredF() + "F-like, but was: "
                                + countP + "P + "
                                + countH + "H + "
                                + countFLike + "F-like."
                ));
            }
        }

        return List.copyOf(violations);
    }

    /**
     * Retorna a composição esperada conforme quantos estão disponíveis (P ou H).
     * - 4 disponíveis => policy.compositionWhen4Available()
     * - 3 disponíveis => policy.compositionWhen3Available()
     */
    private DayComposition expectedComposition(ScheduleProblem problem, int available) {
        if (available == 4) {
            return problem.policy().compositionWhen4Available();
        }
        if (available == 3) {
            return problem.policy().compositionWhen3Available();
        }
        return null;
    }

    /**
     * Para o motor, "folga/ausência" inclui:
     * - F (folga)
     * - FB (folga banco)
     * - DO (folga aniversário)
     */
    private boolean isTimeOff(State state) {
        return state == State.F || state == State.FB || state == State.DO;
    }
}
