package io.github.mcardozo.escala.engine.generation;

import io.github.mcardozo.escala.domain.Cell;
import io.github.mcardozo.escala.domain.CellTag;
import io.github.mcardozo.escala.domain.Day;
import io.github.mcardozo.escala.domain.DayComposition;
import io.github.mcardozo.escala.domain.Employee;
import io.github.mcardozo.escala.domain.EmployeeId;
import io.github.mcardozo.escala.domain.Schedule;
import io.github.mcardozo.escala.domain.State;
import io.github.mcardozo.escala.domain.enums.DayType;
import io.github.mcardozo.escala.domain.enums.TrainingMode;
import io.github.mcardozo.escala.engine.generation.step.AllocateWeekendsStep;
import io.github.mcardozo.escala.engine.optimize.Optimizer;
import io.github.mcardozo.escala.engine.policy.RegimeBlockPolicy;
import io.github.mcardozo.escala.engine.policy.WeekendAssignmentPolicy;
import io.github.mcardozo.escala.engine.problem.ScheduleProblem;
import io.github.mcardozo.escala.engine.result.GenerationResult;
import io.github.mcardozo.escala.engine.result.Score;
import io.github.mcardozo.escala.engine.validation.ScheduleValidator;
import io.github.mcardozo.escala.engine.validation.Violation;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/* Implementação padrão do gerador (pipeline).
 *
 * Pipeline (conforme blueprint):
 * 1) criar schedule base (grid completo)
 * 2) applyRequestsAsLocks
 * 3) allocateWeekends via WeekendAssignmentPolicy
 * 4) applyCompensations (locks gerados)
 * 5) fillWeekdaysByComposition
 * 6) enforceRegimeBlocks
 * 7) validar hard
 * 8) se ok, otimizar e calcular score
 *
 * Evolução arquitetural:
 * - A fase de alocação de fins de semana foi extraída para AllocateWeekendsStep.
 * - Isso reduz responsabilidade do generator e prepara o pipeline para futuras
 *   extrações (compensações, training, preenchimento de dias úteis etc.).
 *
 * Regra nova relevante:
 * - TRAINING_1 não pode participar da escala de fim de semana.
 * - A validação dessa elegibilidade acontece no AllocateWeekendsStep.
 */
public final class DefaultScheduleGenerator implements ScheduleGenerator {

    private final WeekendAssignmentPolicy weekendPolicy;
    private final ScheduleValidator validator;
    private final RegimeBlockPolicy regimeBlockPolicy;
    private final Optimizer optimizer;
    private final AllocateWeekendsStep allocateWeekendsStep;

    public DefaultScheduleGenerator(WeekendAssignmentPolicy weekendPolicy,
                                    ScheduleValidator validator,
                                    RegimeBlockPolicy regimeBlockPolicy,
                                    Optimizer optimizer) {

        this.weekendPolicy = Objects.requireNonNull(weekendPolicy, "weekendPolicy cannot be null");
        this.validator = Objects.requireNonNull(validator, "validator cannot be null");
        this.regimeBlockPolicy = Objects.requireNonNull(regimeBlockPolicy, "regimeBlockPolicy cannot be null");
        this.optimizer = Objects.requireNonNull(optimizer, "optimizer cannot be null");
        this.allocateWeekendsStep = new AllocateWeekendsStep(this.weekendPolicy);
    }

    @Override
    public GenerationResult generate(ScheduleProblem problem) {
        Objects.requireNonNull(problem, "problem cannot be null");

        // 1) cria um schedule com grid completo (inicialmente tudo F)
        Schedule schedule = createEmptySchedule(problem);

        // 2) aplica requests como locks iniciais
        applyRequestsAsLocks(schedule, problem);

        // 3) aloca fins de semana em step separado
        allocateWeekendsStep.execute(schedule, problem);

        // 4) aplica compensações obrigatórias
        applyCompensations(schedule, problem);

        // 5) preenche dias úteis conforme composição alvo
        fillWeekdaysByComposition(schedule, problem);

        // 6) aplica regime blocks (heurística/policy separada)
        enforceRegimeBlocks(schedule, problem);

        // 7) valida hard antes de otimizar
        List<Violation> allValidationBefore = validator.validateHard(schedule, problem);
        List<Violation> hardViolationsBefore = validator.onlyHard(allValidationBefore);
        List<Violation> warningsBefore = validator.onlyWarnings(allValidationBefore);

        Score scoreBefore = validator.score(schedule, problem);

        if (!hardViolationsBefore.isEmpty()) {
            return GenerationResult.failed(schedule, hardViolationsBefore, warningsBefore, scoreBefore);
        }

        // 8) otimiza a solução
        Schedule optimized = optimizer.optimize(schedule, problem);

        // 9) revalida após otimização (proteção arquitetural)
        List<Violation> allValidationAfter = validator.validateHard(optimized, problem);
        List<Violation> hardViolationsAfter = validator.onlyHard(allValidationAfter);
        List<Violation> warningsAfter = validator.onlyWarnings(allValidationAfter);

        Score finalScore = validator.score(optimized, problem);

        if (!hardViolationsAfter.isEmpty()) {
            // O optimizer nunca deveria devolver hard violations,
            // mas essa proteção evita retorno inconsistente.
            return GenerationResult.failed(optimized, hardViolationsAfter, warningsAfter, finalScore);
        }

        return GenerationResult.success(optimized, warningsAfter, finalScore);
    }

    /**
     * Cria um Schedule com grid completo (todas as datas e funcionários).
     *
     * Estratégia inicial:
     * - preencher tudo como F (folga) sem lock
     *
     * Depois, as demais etapas do pipeline vão atribuir P/H e gerar locks.
     */
    private Schedule createEmptySchedule(ScheduleProblem problem) {
        YearMonth month = problem.month();
        List<Employee> employees = problem.employees();
        List<Day> days = problem.days();

        Schedule schedule = Schedule.createEmpty(month, employees, State.F);

        for (Day day : days) {
            LocalDate date = day.date();

            for (Employee employee : employees) {
                Cell cell = schedule.getCell(date, employee.id());

                // Se treinamento estiver habilitado, marcamos TRAINING em seg-sex
                // para funcionários que estejam em TRAINING_1 ou TRAINING_2.
                cell = applyTrainingTagIfNeeded(cell, employee, date, problem);

                schedule.setCell(date, employee.id(), cell);
            }
        }

        return schedule;
    }

    /**
     * Marca a célula com tag TRAINING quando:
     * - a policy do mês tiver training habilitado
     * - o funcionário estiver em algum modo de treinamento
     * - a data for dia útil (seg-sex)
     *
     * Observação:
     * - a tag TRAINING não muda o estado automaticamente aqui
     * - ela serve como sinal para etapas posteriores (ex.: enforcement de P)
     */
    private Cell applyTrainingTagIfNeeded(Cell cell, Employee employee, LocalDate date, ScheduleProblem problem) {
        if (!problem.policy().trainingEnabled()) {
            return cell;
        }

        if (employee.trainingMode() == TrainingMode.NONE) {
            return cell;
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        boolean weekday = dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;

        if (!weekday) {
            return cell;
        }

        EnumSet<CellTag> tagsCopy = cell.tags().isEmpty()
                ? EnumSet.noneOf(CellTag.class)
                : EnumSet.copyOf(cell.tags());

        tagsCopy.add(CellTag.TRAINING);

        return Cell.of(
                cell.state(),
                cell.manualLock(),
                tagsCopy,
                cell.note()
        );
    }

    /**
     * Transforma requests do problema em locks iniciais do schedule.
     *
     * Isso garante que:
     * - a escala respeita entradas manuais/importadas
     * - a validação de locks funcione corretamente depois
     */
    private void applyRequestsAsLocks(Schedule schedule, ScheduleProblem problem) {
        for (var req : problem.requests()) {
            LocalDate date = req.date();
            EmployeeId employeeId = req.employeeId();

            String note = "LOCK(" + req.source() + "): " + (req.reason() == null ? "" : req.reason());

            Cell locked = Cell.of(
                    req.requiredState(),
                    true,
                    EnumSet.noneOf(CellTag.class),
                    note
            );

            schedule.setCell(date, employeeId, locked);
        }
    }

    /**
     * Aplica folgas compensatórias geradas pelo trabalho em fim de semana.
     *
     * Estratégia atual:
     * - identifica, para cada sábado, os 2 trabalhadores do fim de semana
     * - ordena por EmployeeId para definir compensação tipo A/B de forma determinística
     * - tipo A: quinta anterior + segunda posterior
     * - tipo B: sexta anterior + terça posterior
     *
     * Observação:
     * - datas fora do mês são ignoradas
     * - locks existentes são respeitados
     * - células TRAINING não recebem folga automática
     */
    private void applyCompensations(Schedule schedule, ScheduleProblem problem) {
        for (Day day : problem.days()) {
            if (day.type() != DayType.SAT) {
                continue;
            }

            LocalDate saturday = day.date();

            // 1) Descobrir quem trabalhou no sábado
            List<EmployeeId> workers = new ArrayList<>();
            for (Employee employee : problem.employees()) {
                Cell cell = schedule.getCell(saturday, employee.id());
                if (cell.state() == State.P) {
                    workers.add(employee.id());
                }
            }

            // Se não houver exatamente 2, não força compensação aqui.
            // O validator poderá acusar inconsistência depois.
            if (workers.size() != 2) {
                continue;
            }

            // 2) Determinístico: ordena por id e define A/B
            workers.sort(Comparator.comparing(EmployeeId::value));
            EmployeeId typeA = workers.get(0);
            EmployeeId typeB = workers.get(1);

            // 3) Datas de compensação
            LocalDate thuBefore = saturday.minusDays(2);
            LocalDate friBefore = saturday.minusDays(1);
            LocalDate monAfter = saturday.plusDays(2);
            LocalDate tueAfter = saturday.plusDays(3);

            // 4) Aplica locks gerados
            lockIfPossible(schedule, thuBefore, typeA, State.F, "AUTO: comp A (thu before weekend)");
            lockIfPossible(schedule, monAfter, typeA, State.F, "AUTO: comp A (mon after weekend)");

            lockIfPossible(schedule, friBefore, typeB, State.F, "AUTO: comp B (fri before weekend)");
            lockIfPossible(schedule, tueAfter, typeB, State.F, "AUTO: comp B (tue after weekend)");
        }
    }

    /**
     * Cria um lock gerado pelo sistema se:
     * - a célula existir no mês
     * - não houver lock manual
     * - não violar TRAINING (ex.: não transformar TRAINING em folga)
     *
     * Se a data estiver fora do mês, ignora silenciosamente.
     */
    private void lockIfPossible(Schedule schedule,
                                LocalDate date,
                                EmployeeId employeeId,
                                State requiredState,
                                String note) {
        try {
            Cell current = schedule.getCell(date, employeeId);

            if (current.manualLock()) {
                return;
            }

            // Se for TRAINING, não pode virar folga automaticamente
            if (current.hasTag(CellTag.TRAINING) && requiredState != State.P) {
                return;
            }

            Cell locked = Cell.of(requiredState, true, current.tags(), note);
            schedule.setCell(date, employeeId, locked);

        } catch (IllegalArgumentException ex) {
            // Data fora do mês -> não aplicamos compensação.
        }
    }

    /**
     * Preenche dias úteis conforme composição alvo da policy.
     *
     * Estratégia atual:
     * - primeiro garante TRAINING => P
     * - depois conta quantos P/H/F-like existem no dia
     * - escolhe composição alvo baseada em disponibilidade
     * - ajusta estados em células editáveis (não lockadas)
     *
     * Observação:
     * - fairness fina e refinamento vêm depois, via optimizer
     */
    private void fillWeekdaysByComposition(Schedule schedule, ScheduleProblem problem) {
        Random rng = new Random(problem.policy().randomSeed());

        for (Day day : problem.days()) {
            if (day.type() != DayType.NORMAL) {
                continue;
            }

            LocalDate date = day.date();

            // 1) Garante TRAINING => P antes de ajustar composição
            enforceTrainingPresential(schedule, problem, date);

            DayCounts counts = countDay(schedule, problem, date);
            int available = counts.countP + counts.countH;

            DayComposition target = null;
            if (available == 4) {
                target = problem.policy().compositionWhen4Available();
            } else if (available == 3) {
                target = problem.policy().compositionWhen3Available();
            } else {
                // Não inventamos regra para outras disponibilidades.
                continue;
            }

            // 2) Monta lista de células editáveis (não lockadas)
            List<Employee> editable = new ArrayList<>();
            for (Employee employee : problem.employees()) {
                Cell cell = schedule.getCell(date, employee.id());
                if (!cell.manualLock()) {
                    editable.add(employee);
                }
            }

            // 3) Embaralha de forma determinística para reduzir viés fixo
            shuffleDeterministic(editable, rng, date);

            // 4) Ajusta o dia para atingir a composição alvo
            adjustToTarget(
                    schedule,
                    problem,
                    date,
                    editable,
                    target.requiredP(),
                    target.requiredH(),
                    target.requiredF()
            );
        }
    }

    /**
     * Garante que células marcadas com tag TRAINING fiquem em estado P,
     * desde que não estejam lockadas manualmente.
     *
     * Se houver lock incompatível, deixamos a validação acusar.
     */
    private void enforceTrainingPresential(Schedule schedule, ScheduleProblem problem, LocalDate date) {
        if (!problem.policy().trainingEnabled()) {
            return;
        }

        for (Employee employee : problem.employees()) {
            Cell cell = schedule.getCell(date, employee.id());

            if (!cell.hasTag(CellTag.TRAINING)) {
                continue;
            }

            if (cell.manualLock()) {
                continue;
            }

            if (cell.state() != State.P) {
                Cell updated = Cell.of(
                        State.P,
                        false,
                        cell.tags(),
                        "AUTO: training requires P"
                );
                schedule.setCell(date, employee.id(), updated);
            }
        }
    }

    /**
     * Ajusta o dia para bater exatamente a composição alvo.
     *
     * Estratégia:
     * - aumenta/reduz P
     * - aumenta/reduz H
     * - ajusta F-like por último
     *
     * Observação:
     * - esta versão busca corretude e simplicidade
     * - justiça e refinamento vêm depois com optimizer/policies
     */
    private void adjustToTarget(Schedule schedule,
                                ScheduleProblem problem,
                                LocalDate date,
                                List<Employee> editable,
                                int wantP,
                                int wantH,
                                int wantFLike) {

        DayCounts counts = countDay(schedule, problem, date);

        // 1) Aumentar P
        while (counts.countP < wantP) {
            Employee chosen = pickEditableWithState(schedule, date, editable, State.H);
            if (chosen == null) {
                chosen = pickEditableTimeOff(schedule, date, editable);
            }
            if (chosen == null) {
                break;
            }

            setIfNotLocked(schedule, date, chosen.id(), State.P, "AUTO: fill weekdays (need P)");
            counts = countDay(schedule, problem, date);
        }

        // 2) Reduzir P
        while (counts.countP > wantP) {
            Employee chosen = pickEditableWithState(schedule, date, editable, State.P);
            if (chosen == null) {
                break;
            }

            State to = (counts.countH < wantH) ? State.H : State.F;
            setIfNotLocked(schedule, date, chosen.id(), to, "AUTO: fill weekdays (reduce P)");
            counts = countDay(schedule, problem, date);
        }

        // 3) Aumentar H
        while (counts.countH < wantH) {
            Employee chosen = (counts.countP > wantP)
                    ? pickEditableWithState(schedule, date, editable, State.P)
                    : pickEditableTimeOff(schedule, date, editable);

            if (chosen == null) {
                break;
            }

            setIfNotLocked(schedule, date, chosen.id(), State.H, "AUTO: fill weekdays (need H)");
            counts = countDay(schedule, problem, date);
        }

        // 4) Reduzir H
        while (counts.countH > wantH) {
            Employee chosen = pickEditableWithState(schedule, date, editable, State.H);
            if (chosen == null) {
                break;
            }

            State to = (counts.countP < wantP) ? State.P : State.F;
            setIfNotLocked(schedule, date, chosen.id(), to, "AUTO: fill weekdays (reduce H)");
            counts = countDay(schedule, problem, date);
        }

        // 5) Ajustar F-like
        counts = countDay(schedule, problem, date);
        while (counts.countFLike < wantFLike) {
            Employee chosen = pickEditableWithState(schedule, date, editable, State.H);
            if (chosen == null) {
                chosen = pickEditableWithState(schedule, date, editable, State.P);
            }
            if (chosen == null) {
                break;
            }

            setIfNotLocked(schedule, date, chosen.id(), State.F, "AUTO: fill weekdays (need F-like)");
            counts = countDay(schedule, problem, date);
        }

        while (counts.countFLike > wantFLike) {
            Employee chosen = pickEditableTimeOff(schedule, date, editable);
            if (chosen == null) {
                break;
            }

            State to;
            if (counts.countH < wantH) {
                to = State.H;
            } else if (counts.countP < wantP) {
                to = State.P;
            } else {
                to = State.H;
            }

            setIfNotLocked(schedule, date, chosen.id(), to, "AUTO: fill weekdays (reduce F-like)");
            counts = countDay(schedule, problem, date);
        }
    }

    /**
     * Ajusta célula se não houver lock manual.
     *
     * Regra adicional:
     * - se a célula tiver tag TRAINING, só permitimos estado P automaticamente
     */
    private void setIfNotLocked(Schedule schedule,
                                LocalDate date,
                                EmployeeId id,
                                State state,
                                String note) {
        Cell current = schedule.getCell(date, id);

        if (current.manualLock()) {
            return;
        }

        if (current.hasTag(CellTag.TRAINING) && state != State.P) {
            return;
        }

        Cell updated = Cell.of(state, false, current.tags(), note);
        schedule.setCell(date, id, updated);
    }

    /**
     * Conta o estado agregado do dia.
     *
     * F-like = F / FB / DO
     */
    private DayCounts countDay(Schedule schedule, ScheduleProblem problem, LocalDate date) {
        int p = 0;
        int h = 0;
        int fLike = 0;

        for (Employee employee : problem.employees()) {
            State state = schedule.getCell(date, employee.id()).state();

            if (state == State.P) {
                p++;
            } else if (state == State.H) {
                h++;
            } else if (state == State.F || state == State.FB || state == State.DO) {
                fLike++;
            } else {
                fLike++;
            }
        }

        return new DayCounts(p, h, fLike);
    }

    /**
     * Procura funcionário editável com determinado estado.
     *
     * Se tiver tag TRAINING, só aceita P como estado compatível.
     */
    private Employee pickEditableWithState(Schedule schedule,
                                           LocalDate date,
                                           List<Employee> editable,
                                           State state) {
        for (Employee employee : editable) {
            Cell cell = schedule.getCell(date, employee.id());

            if (!cell.manualLock() && cell.state() == state) {
                if (cell.hasTag(CellTag.TRAINING) && state != State.P) {
                    continue;
                }
                return employee;
            }
        }
        return null;
    }

    /**
     * Procura funcionário editável em estado de folga/ausência.
     *
     * Se a célula estiver marcada como TRAINING, não pode ser escolhida aqui.
     */
    private Employee pickEditableTimeOff(Schedule schedule,
                                         LocalDate date,
                                         List<Employee> editable) {
        for (Employee employee : editable) {
            Cell cell = schedule.getCell(date, employee.id());

            if (!cell.manualLock()) {
                State state = cell.state();

                if (state == State.F || state == State.FB || state == State.DO) {
                    if (cell.hasTag(CellTag.TRAINING)) {
                        continue;
                    }
                    return employee;
                }
            }
        }
        return null;
    }

    /**
     * Embaralha de forma determinística por data.
     * Isso reduz viés fixo na ordem dos funcionários.
     */
    private void shuffleDeterministic(List<Employee> list, Random baseRng, LocalDate date) {
        long mixed = baseRng.nextLong() ^ date.toEpochDay();
        Random rng = new Random(mixed);
        java.util.Collections.shuffle(list, rng);
    }

    /**
     * Heurística/policy separada para reduzir alternância de regime.
     */
    private void enforceRegimeBlocks(Schedule schedule, ScheduleProblem problem) {
        regimeBlockPolicy.enforce(schedule, problem);
    }

    /**
     * Record interno para carregar contagens do dia.
     */
    private record DayCounts(int countP, int countH, int countFLike) {
    }
}