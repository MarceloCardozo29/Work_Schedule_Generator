package io.github.mcardozo.escala.engine.validation;

import io.github.mcardozo.escala.domain.Schedule;
import io.github.mcardozo.escala.engine.problem.ScheduleProblem;
import io.github.mcardozo.escala.engine.result.Score;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/*
 * Responsável por:
 * - Executar todas as HardConstraints
 * - Executar todas as SoftConstraints (penalty)
 * - Separar violações HARD vs WARN
 * - Produzir Score
 *
 * Importante:
 * - Isso NÃO gera escala (não é generator).
 * - Isso apenas valida e pontua.
 */
public final class ScheduleValidator {

        /**
         * Valida todas as HardConstraints e retorna todas as violações (HARD e WARN).
         */
        public List<Violation> validateHard(Schedule schedule, ScheduleProblem problem) {
            Objects.requireNonNull(schedule, "schedule cannot be null");
            Objects.requireNonNull(problem, "problem cannot be null");

            List<Violation> all = new ArrayList<>();

            for (HardConstraint c : problem.hardConstraints()) {
                List<Violation> violations = c.validate(schedule, problem);
                if (violations != null && !violations.isEmpty()) {
                    all.addAll(violations);
                }
            }

            return List.copyOf(all);
        }

        /**
         * Calcula a soma total das SoftConstraints.
         */
        public long softPenalty(Schedule schedule, ScheduleProblem problem) {
            Objects.requireNonNull(schedule, "schedule cannot be null");
            Objects.requireNonNull(problem, "problem cannot be null");

            long total = 0L;

            for (SoftConstraint s : problem.softConstraints()) {
                long p = s.penalty(schedule, problem);

                if (p < 0) {
                    // Penalty negativa não faz sentido; melhor falhar rápido.
                    throw new IllegalStateException("SoftConstraint returned negative penalty: " + s.getClass().getName());
                }

                total += p;
            }

            return total;
        }

        /**
         * Calcula o Score completo:
         * - hardViolations = quantidade de violações com Severity.HARD
         * - softPenalty = soma das penalidades
         */
        public Score score(Schedule schedule, ScheduleProblem problem) {
            List<Violation> violations = validateHard(schedule, problem);

            int hardCount = 0;
            for (Violation v : violations) {
                if (v.severity() == Severity.HARD) {
                    hardCount++;
                }
            }

            long soft = softPenalty(schedule, problem);

            return new Score(hardCount, soft);
        }

        /**
         * Filtra somente HARD violations.
         */
        public List<Violation> onlyHard(List<Violation> all) {
            Objects.requireNonNull(all, "all cannot be null");

            List<Violation> hard = new ArrayList<>();
            for (Violation v : all) {
                if (v.severity() == Severity.HARD) {
                    hard.add(v);
                }
            }
            return List.copyOf(hard);
        }

        /**
         * Filtra somente WARN.
         */
        public List<Violation> onlyWarnings(List<Violation> all) {
            Objects.requireNonNull(all, "all cannot be null");

            List<Violation> warns = new ArrayList<>();
            for (Violation v : all) {
                if (v.severity() == Severity.WARN) {
                    warns.add(v);
                }
            }
            return List.copyOf(warns);
        }
    }


