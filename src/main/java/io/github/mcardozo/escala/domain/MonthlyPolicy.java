package io.github.mcardozo.escala.domain;

import java.util.Objects;

/**
 * ✅ CLASS final
 *
 * Configuração do mês (policy).
 * Ela define quais regras e pesos estão ativos para aquele mês.
 *
 * Por que class e não record?
 * - Policy tende a crescer (novos flags, novas opções)
 * - queremos construtor explícito, validações e defaults
 */
 //Preferi class final (não record) porque é uma “configuração rica” com validações e defaults, e pode crescer bastante
public final class MonthlyPolicy {

    private final DayComposition compositionWhen4Available;  // ex.: 2P+2H
    private final DayComposition compositionWhen3Available;  // ex.: 2P+1H+1F
    private final DayComposition weekendComposition;         // ex.: 2P+2F

    private final boolean trainingEnabled;
    private final boolean weeklyGoalEnabled;

    private final WeeklyGoal weeklyGoal; // pode existir mesmo se weeklyGoalEnabled=false
    private final Weights weights;

    private final long randomSeed;

    public MonthlyPolicy(DayComposition compositionWhen4Available,
                         DayComposition compositionWhen3Available,
                         DayComposition weekendComposition,
                         boolean trainingEnabled,
                         boolean weeklyGoalEnabled,
                         WeeklyGoal weeklyGoal,
                         Weights weights,
                         long randomSeed) {

        this.compositionWhen4Available = Objects.requireNonNull(compositionWhen4Available, "compositionWhen4Available cannot be null");
        this.compositionWhen3Available = Objects.requireNonNull(compositionWhen3Available, "compositionWhen3Available cannot be null");
        this.weekendComposition = Objects.requireNonNull(weekendComposition, "weekendComposition cannot be null");
        this.weeklyGoal = Objects.requireNonNull(weeklyGoal, "weeklyGoal cannot be null");
        this.weights = Objects.requireNonNull(weights, "weights cannot be null");

        this.trainingEnabled = trainingEnabled;
        this.weeklyGoalEnabled = weeklyGoalEnabled;
        this.randomSeed = randomSeed;

        // Validações "de sanidade" (não são regras do motor, apenas consistência de config)
        if (compositionWhen4Available.total() <= 0) {
            throw new IllegalArgumentException("compositionWhen4Available total must be > 0");
        }
        if (compositionWhen3Available.total() <= 0) {
            throw new IllegalArgumentException("compositionWhen3Available total must be > 0");
        }
        if (weekendComposition.total() <= 0) {
            throw new IllegalArgumentException("weekendComposition total must be > 0");
        }
    }

    public DayComposition compositionWhen4Available() {
        return compositionWhen4Available;
    }

    public DayComposition compositionWhen3Available() {
        return compositionWhen3Available;
    }

    public DayComposition weekendComposition() {
        return weekendComposition;
    }

    public boolean trainingEnabled() {
        return trainingEnabled;
    }

    public boolean weeklyGoalEnabled() {
        return weeklyGoalEnabled;
    }

    public WeeklyGoal weeklyGoal() {
        return weeklyGoal;
    }

    public Weights weights() {
        return weights;
    }

    public long randomSeed() {
        return randomSeed;
    }
}
