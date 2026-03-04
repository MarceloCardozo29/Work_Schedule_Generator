package io.github.mcardozo.escala.engine.optimize;

import io.github.mcardozo.escala.domain.*;
import io.github.mcardozo.escala.domain.enums.DayType;
import io.github.mcardozo.escala.engine.problem.ScheduleProblem;
import io.github.mcardozo.escala.engine.result.Score;
import io.github.mcardozo.escala.engine.validation.ScheduleValidator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * ✅ CLASS final
 *
 * Otimizador por swaps:
 * - escolhe dois funcionários no mesmo dia NORMAL
 * - tenta trocar os estados (P <-> H, etc) respeitando locks/training
 * - aceita a troca se:
 *   - NÃO cria violações hard
 *   - melhora (reduz) a penalidade soft
 *
 * Observação:
 * - Esta é uma versão inicial "real" e segura.
 * - Depois podemos evoluir para swaps mais inteligentes.
 */
public final class SwapOptimizer implements Optimizer {

    private final ScheduleValidator validator;
    private final int maxIterations;
    private final int maxAttemptsPerIteration;

    public SwapOptimizer(ScheduleValidator validator, int maxIterations, int maxAttemptsPerIteration) {
        this.validator = Objects.requireNonNull(validator, "validator cannot be null");
        this.maxIterations = maxIterations;
        this.maxAttemptsPerIteration = maxAttemptsPerIteration;
    }

    @Override
    public Schedule optimize(Schedule schedule, ScheduleProblem problem) {
        Objects.requireNonNull(schedule, "schedule cannot be null");
        Objects.requireNonNull(problem, "problem cannot be null");

        // Trabalhamos em uma cópia para não “surpreender” quem chamou
        Schedule best = schedule.deepCopy();
        Score bestScore = scoreOf(best, problem);

        // Se já tem hard, não otimizamos (otimização presume schedule válido)
        if (bestScore.hardViolations() > 0) {
            return best;
        }

        Random rng = new Random(problem.policy().randomSeed() ^ 0x5A77BEEF);

        List<LocalDate> candidateDates = normalDates(problem);

        for (int iter = 0; iter < maxIterations; iter++) {

            boolean improvedThisIter = false;

            for (int attempt = 0; attempt < maxAttemptsPerIteration; attempt++) {
                if (candidateDates.isEmpty()) break;

                LocalDate date = candidateDates.get(rng.nextInt(candidateDates.size()));

                // escolhe 2 funcionários distintos
                List<Employee> emps = problem.employees();
                if (emps.size() < 2) break;

                int i1 = rng.nextInt(emps.size());
                int i2 = rng.nextInt(emps.size());
                if (i1 == i2) continue;

                Employee e1 = emps.get(i1);
                Employee e2 = emps.get(i2);

                // tenta swap no "best"
                SwapCandidate cand = tryBuildSwap(best, date, e1, e2);
                if (cand == null) continue;

                // aplica numa cópia e avalia
                Schedule trial = best.deepCopy();
                applySwap(trial, cand);

                Score trialScore = scoreOf(trial, problem);

                // aceita se hard ok e soft melhora
                if (trialScore.hardViolations() == 0 && trialScore.softPenalty() < bestScore.softPenalty()) {
                    best = trial;
                    bestScore = trialScore;
                    improvedThisIter = true;
                }
            }

            // Se não melhorou nada nesta iteração, podemos parar cedo
            if (!improvedThisIter) {
                break;
            }
        }

        return best;
    }

    /**
     * Lista só dias NORMAL para swaps (não mexemos em SAT/SUN na versão inicial).
     */
    private List<LocalDate> normalDates(ScheduleProblem problem) {
        List<LocalDate> dates = new ArrayList<>();
        for (Day d : problem.days()) {
            if (d.type() == DayType.NORMAL) {
                dates.add(d.date());
            }
        }
        return dates;
    }

    /**
     * Monta a troca se for possível:
     * - mesma data
     * - ambos não travados
     * - não viola TRAINING
     * - estados diferentes (swap inútil se iguais)
     */
    private SwapCandidate tryBuildSwap(Schedule schedule, LocalDate date, Employee e1, Employee e2) {
        Cell c1 = schedule.getCell(date, e1.id());
        Cell c2 = schedule.getCell(date, e2.id());

        if (c1.manualLock() || c2.manualLock()) return null;

        // não mexe em TRAINING (porque tem constraint dura de P)
        if (c1.hasTag(io.github.mcardozo.escala.domain.CellTag.TRAINING)) return null;
        if (c2.hasTag(io.github.mcardozo.escala.domain.CellTag.TRAINING)) return null;

        if (c1.state() == c2.state()) return null;

        // troca só estados (mantém tags e lock=false)
        return new SwapCandidate(date, e1, e2, c1, c2);
    }

    private void applySwap(Schedule schedule, SwapCandidate cand) {
        // Troca os estados, preservando tags originais de cada um
        Cell newC1 = Cell.of(cand.c2().state(), false, cand.c1().tags(), "AUTO: swap optimizer");
        Cell newC2 = Cell.of(cand.c1().state(), false, cand.c2().tags(), "AUTO: swap optimizer");

        schedule.setCell(cand.date(), cand.e1().id(), newC1);
        schedule.setCell(cand.date(), cand.e2().id(), newC2);
    }

    /**
     * Calcula Score:
     * - hardViolations: via validator
     * - softPenalty: soma das SoftConstraints
     */
    private Score scoreOf(Schedule schedule, ScheduleProblem problem) {
        // Usa a implementação centralizada do ScheduleValidator
        return validator.score(schedule, problem);
    }


    private record SwapCandidate(LocalDate date, Employee e1, Employee e2, Cell c1, Cell c2) { }
}
