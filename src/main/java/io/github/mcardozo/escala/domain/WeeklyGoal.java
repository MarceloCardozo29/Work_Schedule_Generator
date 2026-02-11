package io.github.mcardozo.escala.domain;

/* Meta semanal plugável (opcional) do blueprint.
 *
 * Exemplo (meta da semana):
 * - alvo: 3P + 2 NonP (NonP = H ou F)
 *
 * Além do alvo, guardamos tolerâncias (min/max) para não ficar rígido demais.
 */
public record WeeklyGoal(
        int targetP,
        int targetNonP,
        int minP,
        int maxP,
        int minNonP,
        int maxNonP
) {
    public WeeklyGoal {
        if (targetP < 0 || targetNonP < 0) {
            throw new IllegalArgumentException("WeeklyGoal targets cannot be negative");
        }
        if (minP < 0 || maxP < 0 || minNonP < 0 || maxNonP < 0) {
            throw new IllegalArgumentException("WeeklyGoal min/max cannot be negative");
        }
        if (minP > maxP) {
            throw new IllegalArgumentException("WeeklyGoal minP cannot be greater than maxP");
        }
        if (minNonP > maxNonP) {
            throw new IllegalArgumentException("WeeklyGoal minNonP cannot be greater than maxNonP");
        }
    }
}
