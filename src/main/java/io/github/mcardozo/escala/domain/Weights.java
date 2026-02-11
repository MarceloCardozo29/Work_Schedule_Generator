package io.github.mcardozo.escala.domain;

/* Pesos das Soft Constraints (penalidades).
 * Quanto maior o peso, mais "caro" fica violar aquela otimização.
 *
 * Isso permite calibrar o comportamento do otimizador sem mudar código.
 */
public record Weights(
        long antiPairPresential,
        long antiPairWeekend,
        long balanceTotals,
        long weeklyGoalNormal,
        long weeklyGoalDuringTraining
) {
    public Weights {
        if (antiPairPresential < 0 || antiPairWeekend < 0 || balanceTotals < 0 || weeklyGoalNormal < 0 || weeklyGoalDuringTraining < 0) {
            throw new IllegalArgumentException("Weights cannot be negative");
        }
    }
}
