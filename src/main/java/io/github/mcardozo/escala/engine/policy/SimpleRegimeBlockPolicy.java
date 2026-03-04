package io.github.mcardozo.escala.engine.policy;

import io.github.mcardozo.escala.domain.Cell;
import io.github.mcardozo.escala.domain.CellTag;
import io.github.mcardozo.escala.domain.Day;
import io.github.mcardozo.escala.domain.enums.DayType;
import io.github.mcardozo.escala.domain.Employee;
import io.github.mcardozo.escala.domain.Schedule;
import io.github.mcardozo.escala.domain.State;
import io.github.mcardozo.escala.engine.problem.ScheduleProblem;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/*Implementação inicial de regime blocks:
 * - tenta reduzir padrões P-H-P e H-P-H em dias NORMAL consecutivos.
 *
 * Regras:
 * - Não altera SAT/SUN
 * - Respeita manualLock
 * - Respeita TRAINING (sempre P)
 *
 * Observação:
 * - Essa heurística pode alterar a composição diária (2P+2H etc).
 * - O otimizador posterior (SwapOptimizer) pode corrigir/compensar isso.
 */
public final class SimpleRegimeBlockPolicy implements RegimeBlockPolicy {

    @Override
    public void enforce(Schedule schedule, ScheduleProblem problem) {
        Objects.requireNonNull(schedule, "schedule cannot be null");
        Objects.requireNonNull(problem, "problem cannot be null");

        // Lista de datas NORMAL ordenadas (evita mexer em fim de semana)
        List<LocalDate> normalDates = problem.days().stream()
                .filter(d -> d.type() == DayType.NORMAL)
                .map(Day::date)
                .sorted(Comparator.naturalOrder())
                .toList();

        if (normalDates.size() < 3) {
            return;
        }

        for (Employee e : problem.employees()) {
            for (int i = 1; i < normalDates.size() - 1; i++) {
                LocalDate prev = normalDates.get(i - 1);
                LocalDate mid  = normalDates.get(i);
                LocalDate next = normalDates.get(i + 1);

                Cell cPrev = schedule.getCell(prev, e.id());
                Cell cMid  = schedule.getCell(mid, e.id());
                Cell cNext = schedule.getCell(next, e.id());

                // Se houver folga/ausência no meio, já quebra bloco (não mexe)
                if (!isWorkLike(cPrev.state()) || !isWorkLike(cMid.state()) || !isWorkLike(cNext.state())) {
                    continue;
                }

                // Respeita lock e training no dia do meio
                if (cMid.manualLock()) {
                    continue;
                }
                if (cMid.hasTag(CellTag.TRAINING)) {
                    // Training exige P; não vamos mexer.
                    continue;
                }

                // P-H-P -> transformar meio em P
                if (cPrev.state() == State.P && cMid.state() == State.H && cNext.state() == State.P) {
                    schedule.setCell(mid, e.id(),
                            Cell.of(State.P, false, cMid.tags(), "AUTO: regime block smooth (P-H-P)"));
                    continue;
                }

                // H-P-H -> transformar meio em H (se não for training)
                if (cPrev.state() == State.H && cMid.state() == State.P && cNext.state() == State.H) {
                    schedule.setCell(mid, e.id(),
                            Cell.of(State.H, false, cMid.tags(), "AUTO: regime block smooth (H-P-H)"));
                }
            }
        }
    }

    private boolean isWorkLike(State s) {
        return s == State.P || s == State.H;
    }
}

