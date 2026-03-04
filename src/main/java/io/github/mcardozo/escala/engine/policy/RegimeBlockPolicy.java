package io.github.mcardozo.escala.engine.policy;

import io.github.mcardozo.escala.domain.Schedule;
import io.github.mcardozo.escala.engine.problem.ScheduleProblem;

/**
 * ✅ INTERFACE (Strategy)
 *
 * Política plugável para "regime blocks":
 * - evitar alternância P/H dia a dia
 * - manter blocos contínuos de P ou H
 * - quebra permitida por folga ou lock ou restrições do sistema
 *
 * A implementação pode aplicar heurísticas simples inicialmente e evoluir depois.
 */
public interface RegimeBlockPolicy {

    void enforce(Schedule schedule, ScheduleProblem problem);
}
