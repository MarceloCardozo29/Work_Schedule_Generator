package io.github.mcardozo.escala.domain;


public enum DayType {
    NORMAL, // Segunda a sexta (dias úteis)
    SAT,    // Sábado
    SUN     // Domingo
}


/* Define o tipo de dia usado pelo motor de escala.
 * A ideia é separar regras de:
 * - dias normais (segunda a sexta)
 * - sábado
 * - domingo
 *
 * Observação:
 * - Feriado não muda o tipo do dia (continua NORMAL/SAT/SUN).
 * - A flag de feriado fica no objeto Day apenas para relatório.
 */