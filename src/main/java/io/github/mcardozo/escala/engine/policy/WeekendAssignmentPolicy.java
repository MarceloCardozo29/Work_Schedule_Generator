package io.github.mcardozo.escala.engine.policy;

import io.github.mcardozo.escala.domain.EmployeeId;
import io.github.mcardozo.escala.domain.Schedule;
import io.github.mcardozo.escala.engine.problem.ScheduleProblem;

import java.time.LocalDate;
import java.util.List;

/* Política plugável para escolher quem trabalha no fim de semana.
 *
 * Contrato:
 * - Recebe uma data de referência do fim de semana (normalmente o sábado)
 * - Recebe a escala parcial (algumas células já podem estar preenchidas)
 * - Recebe o problema completo (policy, history, etc.)
 *
 * Retorno:
 * - Uma lista com exatamente 2 EmployeeId (os 2 que irão trabalhar P no sábado e domingo)
 */
public interface WeekendAssignmentPolicy {

    List<EmployeeId> pickWeekendWorkers(LocalDate weekendDate, Schedule partial, ScheduleProblem problem);
}
