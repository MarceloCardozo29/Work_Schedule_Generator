package io.github.mcardozo.escala.engine.generation;

import io.github.mcardozo.escala.engine.problem.ScheduleProblem;
import io.github.mcardozo.escala.engine.result.GenerationResult;

/* Contrato do gerador de escala.
 *
 * Entrada: ScheduleProblem (mês, funcionários, policy, requests, constraints, histórico)
 * Saída: GenerationResult (SUCCESS/FAILED, schedule, violações, score, warnings)
 */
public interface ScheduleGenerator {

    GenerationResult generate(ScheduleProblem problem);
}
