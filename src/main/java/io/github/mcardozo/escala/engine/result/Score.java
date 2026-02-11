package io.github.mcardozo.escala.engine.result;

/* Score final de uma escala:
 * - hardViolations: quantidade de violações duras encontradas
 * - softPenalty: soma das penalidades (otimização)
 *
 * Quanto menor, melhor.
 */
public record Score(int hardViolations, long softPenalty) {

    public Score {
        if (hardViolations < 0) {
            throw new IllegalArgumentException("Score.hardViolations cannot be negative");
        }
        if (softPenalty < 0) {
            throw new IllegalArgumentException("Score.softPenalty cannot be negative");
        }
    }

    public boolean isHardValid() {
        return hardViolations == 0;
    }
}
