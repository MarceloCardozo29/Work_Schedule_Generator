package io.github.mcardozo.escala.engine.result;

/* Status da geração.
 * SUCCESS: gerou uma escala válida (sem HARD violations)
 * FAILED: não conseguiu gerar escala válida
 */
public enum Status {
    SUCCESS,
    FAILED
}
