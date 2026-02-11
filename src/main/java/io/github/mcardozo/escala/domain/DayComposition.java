package io.github.mcardozo.escala.domain;

/*
 * Define a "composição" obrigatória de um dia:
 * quantos Presenciais (P), quantos Home (H) e quantas Folgas (F).
 *
 * Exemplos do blueprint:
 * - 4 disponíveis: 2P + 2H
 * - 3 disponíveis: 2P + 1H + 1F
 * - fim de semana: 2P + 2F
 *
 * Observação:
 * - Aqui só guardamos a regra numérica.
 * - A validação de "quem" e "como" entra no motor de regras.
 */
public record DayComposition(int requiredP, int requiredH, int requiredF) {

    public DayComposition {
        if (requiredP < 0 || requiredH < 0 || requiredF < 0) {
            throw new IllegalArgumentException("DayComposition values cannot be negative");
        }
    }

    /**
     * Soma total esperada (ex.: 2P+2H+0F = 4).
     */
    public int total() {
        return requiredP + requiredH + requiredF;
    }
}
