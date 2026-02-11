package io.github.mcardozo.escala.engine.validation;

/* Severidade de um problema encontrado na escala.
 *
 * HARD:
 * - Quebra de regra dura (não pode passar).
 *
 * WARN:
 * - Aviso (ex.: edição manual que deixa a escala "estranha", mas permitimos).
 */
public enum Severity {
    HARD,
    WARN
}
