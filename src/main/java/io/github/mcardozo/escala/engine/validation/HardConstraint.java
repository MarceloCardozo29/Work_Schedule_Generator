package io.github.mcardozo.escala.engine.validation;

import io.github.mcardozo.escala.domain.Schedule;
import io.github.mcardozo.escala.engine.problem.ScheduleProblem;

import java.util.List;

/* Uma HardConstraint valida regras duras (obrigatórias).
 * Se houver violação HARD, a escala não é considerada válida.
 *
 * Exemplo de hard rules:
 * - composição do fim de semana: 2P + 2F
 * - TRAINING exige P
 * - respeitar manual locks
 */
public interface HardConstraint {

    /**
     * Valida a escala inteira e retorna a lista de violações encontradas.
     *
     * Observação:
     * - Retorne lista vazia se estiver OK.
     * - Violações devem ter Severity.HARD (ou WARN, se essa constraint também reportar alertas).
     */
    List<Violation> validate(Schedule schedule, ScheduleProblem problem);
}
