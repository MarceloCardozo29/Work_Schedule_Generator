package io.github.mcardozo.escala.domain;

public enum CompPreference {
    A,
    B,
    NONE
}

/* Preferência do funcionário sobre a folga do final de semana trabalhado.
 *
 * - A: folga na quinta anterior + segunda posterior
 * - B: folga na sexta anterior + terça posterior
 * - NONE: sem preferência (o motor decide)
 *
 * Deixei no domínio por estar no blueprint e para evolução futura.
 */