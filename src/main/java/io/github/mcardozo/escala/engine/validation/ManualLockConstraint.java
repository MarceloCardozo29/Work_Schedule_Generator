package io.github.mcardozo.escala.engine.validation;

import io.github.mcardozo.escala.domain.Schedule;
import io.github.mcardozo.escala.domain.State;
import io.github.mcardozo.escala.domain.TimeOffRequest;
import io.github.mcardozo.escala.engine.problem.ScheduleProblem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/* Hard constraint:
 * - Requests (especialmente MANUAL_EDIT) devem ser respeitados.
 *
 * Importante:
 * - O Schedule em si não guarda o "valor original" do lock.
 * - O "contrato" do lock está representado pelos TimeOffRequest do problema.
 *
 * Resultado:
 * - Se o Schedule final não estiver no estado requerido pelo request,
 *   retornamos violação HARD.
 */
public final class ManualLockConstraint implements HardConstraint {

    private static final String CODE = "REQUEST_LOCK_NOT_RESPECTED";

    @Override
    public List<Violation> validate(Schedule schedule, ScheduleProblem problem) {
        Objects.requireNonNull(schedule, "schedule cannot be null");
        Objects.requireNonNull(problem, "problem cannot be null");

        List<Violation> violations = new ArrayList<>();

        for (TimeOffRequest req : problem.requests()) {
            State expected = req.requiredState();
            State actual = schedule.getCell(req.date(), req.employeeId()).state();

            if (actual != expected) {
                violations.add(new Violation(
                        CODE,
                        Severity.HARD,
                        req.date(),
                        req.employeeId(),
                        "Request lock not respected. Expected: " + expected + ", but was: " + actual
                                + ". Source=" + req.source()
                                + (req.reason().isBlank() ? "" : (" Reason=" + req.reason()))
                ));
            }
        }

        return List.copyOf(violations);
    }
}
