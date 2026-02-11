package io.github.mcardozo.escala.engine.validation;

import io.github.mcardozo.escala.domain.Schedule;
import io.github.mcardozo.escala.engine.problem.ScheduleProblem;

/* SoftConstraints não "bloqueiam" a escala.
 * Elas geram um custo (penalty) para o otimizador tentar minimizar.
 *
 * Exemplo:
 * - anti-duplinhas
 * - balanceamento de totais
 * - meta semanal
 */
public interface SoftConstraint {

    /**
     * Calcula a penalidade (custo) da escala segundo esta constraint.
     * Deve ser determinístico para o mesmo input.
     *
     * Retorne 0 para "sem penalidade".
     */
    long penalty(Schedule schedule, ScheduleProblem problem);
}
