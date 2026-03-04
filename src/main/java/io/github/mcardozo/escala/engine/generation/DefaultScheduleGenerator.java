package io.github.mcardozo.escala.engine.generation;

import io.github.mcardozo.escala.domain.*;
import io.github.mcardozo.escala.domain.enums.DayType;
import io.github.mcardozo.escala.engine.policy.WeekendAssignmentPolicy;
import io.github.mcardozo.escala.engine.problem.ScheduleProblem;
import io.github.mcardozo.escala.engine.result.GenerationResult;
import io.github.mcardozo.escala.engine.result.Score;
import io.github.mcardozo.escala.engine.validation.ScheduleValidator;
import io.github.mcardozo.escala.engine.validation.Violation;
import io.github.mcardozo.escala.engine.policy.RegimeBlockPolicy;
import io.github.mcardozo.escala.engine.optimize.Optimizer;
import io.github.mcardozo.escala.domain.State;


import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/* Implementação padrão do gerador (pipeline).
 *
 * Pipeline (conforme blueprint):
 * 1) criar schedule base (grid completo)
 * 2) applyRequestsAsLocks
 * 3) allocateWeekends via WeekendAssignmentPolicy
 * 4) applyCompensations (locks gerados)
 * 5) fillWeekdaysByComposition
 * 6) enforceRegimeBlocks (se habilitado)
 * 7) validar hard
 * 8) se ok, otimizar e calcular score
 *
 * Neste passo, apenas estruturamos o pipeline de forma profissional.
 * Implementações serão preenchidas em passos seguintes.
 */
public final class DefaultScheduleGenerator implements ScheduleGenerator {

    private final WeekendAssignmentPolicy weekendPolicy;
    private final ScheduleValidator validator;
    private final RegimeBlockPolicy regimeBlockPolicy;
    private final Optimizer optimizer;


    public DefaultScheduleGenerator(WeekendAssignmentPolicy weekendPolicy,
                                    ScheduleValidator validator,
                                    RegimeBlockPolicy regimeBlockPolicy,
                                    Optimizer optimizer) {

        this.weekendPolicy = Objects.requireNonNull(weekendPolicy, "weekendPolicy cannot be null");
        this.validator = Objects.requireNonNull(validator, "validator cannot be null");
        this.regimeBlockPolicy = Objects.requireNonNull(regimeBlockPolicy, "regimeBlockPolicy cannot be null");
        this.optimizer = Objects.requireNonNull(optimizer, "optimizer cannot be null");
    }

    @Override
    public GenerationResult generate(ScheduleProblem problem) {
        Objects.requireNonNull(problem, "problem cannot be null");

        // 1) cria um schedule com grid completo (inicialmente tudo F, por exemplo)
        Schedule schedule = createEmptySchedule(problem);

        // 2) aplica requests como locks iniciais (aqui só stub; vamos implementar)
        applyRequestsAsLocks(schedule, problem);

        // 3) aloca fins de semana (aqui só stub; vamos implementar)
        allocateWeekends(schedule, problem);

        // 4) aplica compensações obrigatórias (stub; vamos implementar)
        applyCompensations(schedule, problem);

        // 5) preenche dias de semana por composição (stub; vamos implementar)
        fillWeekdaysByComposition(schedule, problem);

        // 6) aplica regime blocks (policy/heurística separada)
        enforceRegimeBlocks(schedule, problem);

        // 7) valida hard (antes de otimizar)
        List<Violation> allValidationBefore = validator.validateHard(schedule, problem);
        List<Violation> hardViolationsBefore = validator.onlyHard(allValidationBefore);
        List<Violation> warningsBefore = validator.onlyWarnings(allValidationBefore);

        Score scoreBefore = validator.score(schedule, problem);

        if (!hardViolationsBefore.isEmpty()) {
            return GenerationResult.failed(schedule, hardViolationsBefore, warningsBefore, scoreBefore);
        }

// 8) otimiza (SwapOptimizer)
        Schedule optimized = optimizer.optimize(schedule, problem);

// 9) revalida hard (depois da otimização)
        List<Violation> allValidationAfter = validator.validateHard(optimized, problem);
        List<Violation> hardViolationsAfter = validator.onlyHard(allValidationAfter);
        List<Violation> warningsAfter = validator.onlyWarnings(allValidationAfter);

        Score finalScore = validator.score(optimized, problem);

        if (!hardViolationsAfter.isEmpty()) {
            // Proteção: optimizer nunca deveria devolver hard>0, mas garantimos.
            return GenerationResult.failed(optimized, hardViolationsAfter, warningsAfter, finalScore);
        }

        return GenerationResult.success(optimized, warningsAfter, finalScore);

    }

    /**
     * Cria um Schedule com grid completo (todas as datas e funcionários).
     * <p>
     * Estratégia inicial:
     * - Preenche tudo como F (folga) sem lock.
     * <p>
     * Depois, as etapas do pipeline vão atribuir P/H e locks.
     */
    private Schedule createEmptySchedule(ScheduleProblem problem) {
        YearMonth month = problem.month();
        List<Employee> employees = problem.employees();
        List<Day> days = problem.days();

        Schedule schedule = Schedule.createEmpty(month, employees, State.F);

        for (Day day : days) {
            LocalDate date = day.date();

            for (Employee e : employees) {
                Cell cell = schedule.getCell(date, e.id());

                // Se treinamento estiver habilitado, marca TRAINING em seg-sex
                cell = applyTrainingTagIfNeeded(cell, date, problem);

                schedule.setCell(date, e.id(), cell);
            }
        }

        return schedule;
    }

    private Cell applyTrainingTagIfNeeded(Cell cell, LocalDate date, ScheduleProblem problem) {
        if (!problem.policy().trainingEnabled()) {
            return cell;
        }

        var dow = date.getDayOfWeek();
        boolean weekday = (dow != java.time.DayOfWeek.SATURDAY && dow != java.time.DayOfWeek.SUNDAY);

        if (!weekday) {
            return cell;
        }

        java.util.EnumSet<io.github.mcardozo.escala.domain.CellTag> tagsCopy =
                java.util.EnumSet.copyOf(cell.tags());

        tagsCopy.add(io.github.mcardozo.escala.domain.CellTag.TRAINING);

        return Cell.of(
                cell.state(),
                cell.manualLock(),
                tagsCopy,
                cell.note()
        );
    }


    /**
     * Stub (implementaremos):
     * - transforma requests do problema em locks iniciais no schedule.
     */
    private void applyRequestsAsLocks(Schedule schedule, ScheduleProblem problem) {
        // Para cada request, travamos a célula no estado exigido.
        // Isso garante que:
        // - a escala respeita manual edits/admin/import
        // - e a ManualLockConstraint consegue validar depois.
        for (var req : problem.requests()) {
            var date = req.date();
            var employeeId = req.employeeId();

            // A nota é útil para debug e também para futuras telas de auditoria.
            String note = "LOCK(" + req.source() + "): " + (req.reason() == null ? "" : req.reason());

            // IMPORTANT: usamos Cell.of(..., manualLock=true, ...) se existir.
            // Como seu Cell tem factories of(...), usamos a mais completa.
            // Se seu Cell.of(...) não tiver essa assinatura, me avise que eu adapto.
            Cell locked = Cell.of(req.requiredState(), true, java.util.EnumSet.noneOf(io.github.mcardozo.escala.domain.CellTag.class), note);

            // setCell valida integridade (se data/id existem no grid)
            schedule.setCell(date, employeeId, locked);
        }
    }

    /**
     * Stub (implementaremos):
     * - escolhe 2 trabalhadores por fim de semana via WeekendAssignmentPolicy
     * - seta sábado e domingo como P para esses 2
     * - seta os outros 2 como F
     * - respeita locks (se tiver conflito, deve falhar ou registrar violação)
     */
    private void allocateWeekends(Schedule schedule, ScheduleProblem problem) {
        // Percorre todos os dias e quando encontrar um sábado, trata o par sábado+domingo
        for (Day day : problem.days()) {
            if (day.type() != DayType.SAT) {
                continue;
            }

            LocalDate saturday = day.date();
            LocalDate sunday = saturday.plusDays(1);

            // Escolhe os 2 trabalhadores do fim de semana
            List<io.github.mcardozo.escala.domain.EmployeeId> workers =
                    weekendPolicy.pickWeekendWorkers(saturday, schedule, problem);

            if (workers == null || workers.size() != 2) {
                throw new IllegalStateException("WeekendAssignmentPolicy must return exactly 2 workers. Got: " + workers);
            }

            // Monta conjunto para consulta rápida
            java.util.Set<io.github.mcardozo.escala.domain.EmployeeId> workerSet = new java.util.HashSet<>(workers);

            // Para cada funcionário, seta P se é worker, senão seta F
            for (Employee e : problem.employees()) {
                var id = e.id();

                State satState = workerSet.contains(id) ? State.P : State.F;
                State sunState = workerSet.contains(id) ? State.P : State.F;

                // Respeita locks: só altera se a célula não estiver travada
                applyIfNotLocked(schedule, saturday, id, satState, "AUTO: weekend allocation");
                applyIfNotLocked(schedule, sunday, id, sunState, "AUTO: weekend allocation");
            }
        }
    }

    /**
     * Aplica um estado em uma célula apenas se ela não estiver travada (manualLock=false).
     * Se estiver travada, não altera (deixa o validator acusar violações depois).
     */
    private void applyIfNotLocked(Schedule schedule,
                                  LocalDate date,
                                  io.github.mcardozo.escala.domain.EmployeeId employeeId,
                                  State desiredState,
                                  String note) {

        Cell current = schedule.getCell(date, employeeId);

        if (current.manualLock()) {
            return; // respeita lock
        }

        // Mantém tags existentes, apenas troca estado e note
        Cell updated = Cell.of(
                desiredState,
                false,
                current.tags(),
                note
        );

        schedule.setCell(date, employeeId, updated);
    }

    /**
     * Stub (implementaremos):
     * - aplica folgas compensatórias tipo A e B (locks gerados)
     */
    private void applyCompensations(Schedule schedule, ScheduleProblem problem) {
        // Para cada sábado, identificamos os 2 trabalhadores (state=P) e aplicamos as folgas obrigatórias.
        for (Day day : problem.days()) {
            if (day.type() != DayType.SAT) {
                continue;
            }

            LocalDate saturday = day.date();

            // 1) Descobrir quem trabalhou no sábado (regra: trabalhadores ficam P)
            List<EmployeeId> workers = new ArrayList<>();
            for (Employee e : problem.employees()) {
                Cell cell = schedule.getCell(saturday, e.id());
                if (cell.state() == State.P) {
                    workers.add(e.id());
                }
            }

            // Se não tiver exatamente 2, não forçamos nada aqui (validator pega depois).
            if (workers.size() != 2) {
                continue;
            }

            // 2) Determinístico: ordena por id e define A/B
            workers.sort(Comparator.comparing(EmployeeId::value));
            EmployeeId typeA = workers.get(0);
            EmployeeId typeB = workers.get(1);

            // 3) Datas de compensação
            LocalDate thuBefore = saturday.minusDays(2); // quinta anterior
            LocalDate friBefore = saturday.minusDays(1); // sexta anterior
            LocalDate monAfter = saturday.plusDays(2);  // segunda posterior (sábado+2)
            LocalDate tueAfter = saturday.plusDays(3);  // terça posterior  (sábado+3)

            // 4) Aplica locks gerados
            lockIfPossible(schedule, thuBefore, typeA, State.F, "AUTO: comp A (thu before weekend)");
            lockIfPossible(schedule, monAfter, typeA, State.F, "AUTO: comp A (mon after weekend)");

            lockIfPossible(schedule, friBefore, typeB, State.F, "AUTO: comp B (fri before weekend)");
            lockIfPossible(schedule, tueAfter, typeB, State.F, "AUTO: comp B (tue after weekend)");
        }
    }

    /**
     * Cria um lock "gerado pelo sistema" (manualLock=true) se a célula existir e não estiver travada.
     * - Se a data não existir no grid (fora do mês), ignora.
     * - Se a célula já estiver travada (manual edit), não sobrescreve.
     */
    private void lockIfPossible(Schedule schedule, LocalDate date, EmployeeId employeeId, State requiredState, String note) {
        // fora do mês -> ignora
        try {
            Cell current = schedule.getCell(date, employeeId);

            if (current.manualLock()) {
                return; // respeita lock existente
            }

            // Se for TRAINING, não pode virar folga automaticamente
            if (current.hasTag(CellTag.TRAINING) && requiredState != State.P) {
                return;
            }

            Cell locked = Cell.of(requiredState, true, current.tags(), note);
            schedule.setCell(date, employeeId, locked);

        } catch (IllegalArgumentException ex) {
            // Schedule não tem essa data (ex: quinta anterior caiu no mês anterior)
            // não é erro: simplesmente não aplicamos
        }
    }

    private void fillWeekdaysByComposition(Schedule schedule, ScheduleProblem problem) {
        // RNG determinístico para preencher dias com repetibilidade
        java.util.Random rng = new java.util.Random(problem.policy().randomSeed());

        for (Day day : problem.days()) {
            if (day.type() != DayType.NORMAL) {
                continue; // só dias úteis/normais; fim de semana já foi alocado
            }

            LocalDate date = day.date();

            // 1) Primeiro passamos garantindo TRAINING => P (se não estiver lockado).
            //    Se estiver lockado com outro estado, deixamos como está e a constraint vai acusar.
            enforceTrainingPresential(schedule, problem, date);

            // 2) Agora calculamos contagens atuais do dia (respeitando locks e o que já foi setado)
            DayCounts counts = countDay(schedule, problem, date);

            // Disponíveis = P ou H
            int available = counts.countP + counts.countH;

            // Definimos qual composição queremos atingir
            io.github.mcardozo.escala.domain.DayComposition target = null;
            if (available == 4) {
                target = problem.policy().compositionWhen4Available();
            } else if (available == 3) {
                target = problem.policy().compositionWhen3Available();
            } else {
                // Se cair em algo diferente (2 disponíveis etc), deixamos a validação acusar.
                // Não forçamos nada aqui para não "inventar regra".
                continue;
            }

            // 3) Ajustamos o dia para atingir a composição alvo:
            //    - Não mexer em locks
            //    - Não violar TRAINING (já garantimos acima)
            //    Estratégia:
            //    a) construir lista de "células editáveis" (não lockadas)
            //    b) ajustar P/H/F-like conforme necessário

            List<Employee> employees = problem.employees();

            // candidatos editáveis (não travados)
            List<Employee> editable = new java.util.ArrayList<>();
            for (Employee e : employees) {
                Cell cell = schedule.getCell(date, e.id());
                if (!cell.manualLock()) {
                    editable.add(e);
                }
            }

            // Para reduzir viés, embaralhamos de forma determinística por dia
            shuffleDeterministic(editable, rng, date);

            // Recalcula counts após possíveis mudanças (training)
            counts = countDay(schedule, problem, date);

            // Queremos atingir: target.requiredP / requiredH / requiredF
            // Note: requiredF aqui é F-like (F/FB/DO). Porém nossa geração padrão usa F.
            int wantP = target.requiredP();
            int wantH = target.requiredH();
            int wantFLike = target.requiredF();

            // Ajuste P: se estiver faltando P, converte H->P primeiro (editáveis), depois F->P se necessário.
            adjustToTarget(schedule, problem, date, editable, rng, wantP, wantH, wantFLike);
        }
    }

    /**
     * Garante TRAINING => P, respeitando locks.
     * Se a célula estiver lockada (manualLock=true), não altera.
     */
    private void enforceTrainingPresential(Schedule schedule, ScheduleProblem problem, LocalDate date) {
        if (!problem.policy().trainingEnabled()) {
            return;
        }

        for (Employee e : problem.employees()) {
            Cell cell = schedule.getCell(date, e.id());

            if (!cell.hasTag(io.github.mcardozo.escala.domain.CellTag.TRAINING)) {
                continue;
            }

            if (cell.manualLock()) {
                continue; // respeita lock, mesmo se estiver errado (constraint vai acusar)
            }

            if (cell.state() != State.P) {
                // mantém tags e troca estado
                Cell updated = Cell.of(
                        State.P,
                        false,
                        cell.tags(),
                        "AUTO: training requires P"
                );
                schedule.setCell(date, e.id(), updated);
            }
        }
    }

    /**
     * Ajusta o dia para bater exatamente a composição alvo.
     *
     * Observação:
     * - Essa versão é simples e correta (boa base).
     * - Justiça/otimização vem depois com o SwapOptimizer.
     */
    private void adjustToTarget(Schedule schedule,
                                ScheduleProblem problem,
                                LocalDate date,
                                List<Employee> editable,
                                java.util.Random rng,
                                int wantP,
                                int wantH,
                                int wantFLike) {

        // Atualiza contagens atuais
        DayCounts counts = countDay(schedule, problem, date);

        // 1) Se precisar aumentar P:
        while (counts.countP < wantP) {
            // preferimos converter H->P primeiro
            Employee chosen = pickEditableWithState(schedule, date, editable, State.H);
            if (chosen == null) {
                // se não existe H editável, converte F->P
                chosen = pickEditableTimeOff(schedule, date, editable);
            }
            if (chosen == null) break;

            setIfNotLocked(schedule, date, chosen.id(), State.P, "AUTO: fill weekdays (need P)");
            counts = countDay(schedule, problem, date);
        }

        // 2) Se P está sobrando, reduz para H (preferencial) ou F se necessário
        while (counts.countP > wantP) {
            Employee chosen = pickEditableWithState(schedule, date, editable, State.P);
            if (chosen == null) break;

            // decidimos para onde: se falta H, vai para H; senão vai para F
            State to = (counts.countH < wantH) ? State.H : State.F;
            setIfNotLocked(schedule, date, chosen.id(), to, "AUTO: fill weekdays (reduce P)");
            counts = countDay(schedule, problem, date);
        }

        // 3) Ajustar H
        while (counts.countH < wantH) {
            // converte P->H se P estiver sobrando, senão converte F->H
            Employee chosen = (counts.countP > wantP)
                    ? pickEditableWithState(schedule, date, editable, State.P)
                    : pickEditableTimeOff(schedule, date, editable);

            if (chosen == null) break;

            setIfNotLocked(schedule, date, chosen.id(), State.H, "AUTO: fill weekdays (need H)");
            counts = countDay(schedule, problem, date);
        }

        while (counts.countH > wantH) {
            Employee chosen = pickEditableWithState(schedule, date, editable, State.H);
            if (chosen == null) break;

            // se falta P, vira P; senão vira F
            State to = (counts.countP < wantP) ? State.P : State.F;
            setIfNotLocked(schedule, date, chosen.id(), to, "AUTO: fill weekdays (reduce H)");
            counts = countDay(schedule, problem, date);
        }

        // 4) Ajustar F-like (aqui usamos F como padrão)
        counts = countDay(schedule, problem, date);
        while (counts.countFLike < wantFLike) {
            // precisamos de mais folgas/ausências: converte H->F primeiro, depois P->F
            Employee chosen = pickEditableWithState(schedule, date, editable, State.H);
            if (chosen == null) {
                chosen = pickEditableWithState(schedule, date, editable, State.P);
            }
            if (chosen == null) break;

            setIfNotLocked(schedule, date, chosen.id(), State.F, "AUTO: fill weekdays (need F-like)");
            counts = countDay(schedule, problem, date);
        }

        while (counts.countFLike > wantFLike) {
            // temos folga demais: converte F->H se falta H, senão F->P se falta P
            Employee chosen = pickEditableTimeOff(schedule, date, editable);
            if (chosen == null) break;

            State to;
            if (counts.countH < wantH) to = State.H;
            else if (counts.countP < wantP) to = State.P;
            else to = State.H; // default seguro

            setIfNotLocked(schedule, date, chosen.id(), to, "AUTO: fill weekdays (reduce F-like)");
            counts = countDay(schedule, problem, date);
        }
    }

    private void setIfNotLocked(Schedule schedule, LocalDate date, io.github.mcardozo.escala.domain.EmployeeId id, State state, String note) {
        Cell current = schedule.getCell(date, id);
        if (current.manualLock()) return;

        // Se a célula tem TRAINING, nunca colocamos H/F automaticamente.
        if (current.hasTag(io.github.mcardozo.escala.domain.CellTag.TRAINING) && state != State.P) {
            return;
        }

        Cell updated = Cell.of(state, false, current.tags(), note);
        schedule.setCell(date, id, updated);
    }

    /**
     * Conta P/H/F-like para o dia.
     * F-like = F/FB/DO.
     */
    private DayCounts countDay(Schedule schedule, ScheduleProblem problem, LocalDate date) {
        int p = 0, h = 0, fLike = 0;

        for (Employee e : problem.employees()) {
            State s = schedule.getCell(date, e.id()).state();
            if (s == State.P) p++;
            else if (s == State.H) h++;
            else if (s == State.F || s == State.FB || s == State.DO) fLike++;
            else fLike++;
        }

        return new DayCounts(p, h, fLike);
    }

    private Employee pickEditableWithState(Schedule schedule, LocalDate date, List<Employee> editable, State state) {
        for (Employee e : editable) {
            Cell cell = schedule.getCell(date, e.id());
            if (!cell.manualLock() && cell.state() == state) {
                // Se for TRAINING, só aceitamos P
                if (cell.hasTag(io.github.mcardozo.escala.domain.CellTag.TRAINING) && state != State.P) {
                    continue;
                }
                return e;
            }
        }
        return null;
    }

    private Employee pickEditableTimeOff(Schedule schedule, LocalDate date, List<Employee> editable) {
        for (Employee e : editable) {
            Cell cell = schedule.getCell(date, e.id());
            if (!cell.manualLock()) {
                State s = cell.state();
                if (s == State.F || s == State.FB || s == State.DO) {
                    // Se for TRAINING, não pode estar em folga automaticamente
                    if (cell.hasTag(io.github.mcardozo.escala.domain.CellTag.TRAINING)) {
                        continue;
                    }
                    return e;
                }
            }
        }
        return null;
    }

    /**
     * Embaralha de forma determinística por data.
     * Isso evita viés fixo na ordem dos funcionários.
     */
    private void shuffleDeterministic(List<Employee> list, java.util.Random baseRng, LocalDate date) {
        long mixed = baseRng.nextLong() ^ date.toEpochDay();
        java.util.Random rng = new java.util.Random(mixed);
        java.util.Collections.shuffle(list, rng);
    }

    /**
     * Record interno para carregar contagens do dia.
     */
    /**
     * Record interno para carregar contagens do dia.
     */
    private record DayCounts(int countP, int countH, int countFLike) { }

    /**
     * Stub (implementaremos):
     * - evita alternância P/H diária
     * - policy/heurística separada
     */
    private void enforceRegimeBlocks(Schedule schedule, ScheduleProblem problem) {
        // Policy plugável: heurística separada do gerador
        regimeBlockPolicy.enforce(schedule, problem);
    }

} // ✅ ÚNICO "}" final: fecha a classe DefaultScheduleGenerator







