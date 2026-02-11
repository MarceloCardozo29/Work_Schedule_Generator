package io.github.mcardozo.escala.engine.result;

import io.github.mcardozo.escala.domain.Schedule;
import io.github.mcardozo.escala.engine.validation.Violation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/* Resultado completo da geração:
 * - status: SUCCESS/FAILED
 * - schedule: a escala gerada (pode existir mesmo no FAILED para debug)
 * - hardViolations: lista de violações duras
 * - warnings: lista de avisos
 * - score: score calculado (hard count + soft penalty)
 *
 * Design:
 * - guardamos listas como snapshots imutáveis (List.copyOf)
 */
public final class GenerationResult {

    private final Status status;
    private final Schedule schedule;
    private final List<Violation> hardViolations;
    private final List<Violation> warnings;
    private final Score score;

    private GenerationResult(Status status,
                             Schedule schedule,
                             List<Violation> hardViolations,
                             List<Violation> warnings,
                             Score score) {

        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.schedule = Objects.requireNonNull(schedule, "schedule cannot be null");
        this.hardViolations = List.copyOf(Objects.requireNonNull(hardViolations, "hardViolations cannot be null"));
        this.warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings cannot be null"));
        this.score = Objects.requireNonNull(score, "score cannot be null");
    }

    public static GenerationResult success(Schedule schedule, List<Violation> warnings, Score score) {
        return new GenerationResult(
                Status.SUCCESS,
                schedule,
                List.of(),
                (warnings == null) ? List.of() : warnings,
                score
        );
    }

    public static GenerationResult failed(Schedule schedule, List<Violation> hardViolations, List<Violation> warnings, Score score) {
        return new GenerationResult(
                Status.FAILED,
                schedule,
                (hardViolations == null) ? List.of() : hardViolations,
                (warnings == null) ? List.of() : warnings,
                score
        );
    }

    public Status status() {
        return status;
    }

    public Schedule schedule() {
        return schedule;
    }

    public List<Violation> hardViolations() {
        return hardViolations;
    }

    public List<Violation> warnings() {
        return warnings;
    }

    public Score score() {
        return score;
    }

    /**
     * Helper: junta hardViolations + warnings em uma lista única (para logs/prints).
     */
    public List<Violation> allIssues() {
        List<Violation> all = new ArrayList<>(hardViolations.size() + warnings.size());
        all.addAll(hardViolations);
        all.addAll(warnings);
        return List.copyOf(all);
    }
}
