package io.github.mcardozo.escala.engine.optimize;

import io.github.mcardozo.escala.domain.Schedule;
import io.github.mcardozo.escala.engine.problem.ScheduleProblem;

/**
 * ✅ INTERFACE (contrato)
 *
 * Um Optimizer recebe uma escala válida (hard ok) e tenta reduzir penalidades soft
 * sem violar regras duras.
 */
public interface Optimizer {

    /**
     * Retorna uma nova versão (ou a mesma) escala, idealmente com menor penalidade soft.
     * O optimizer NUNCA deve aceitar uma mudança que gere hard violations.
     */
    Schedule optimize(Schedule schedule, ScheduleProblem problem);
}
