package io.github.mcardozo.escala.domain;

import io.github.mcardozo.escala.domain.enums.DayType;

import java.time.LocalDate;
import java.util.Objects;

/* Representa um dia do calendário dentro do mês da escala.
 *
 * Campos:
 * - date: data do dia
 * - type: NORMAL, SAT, SUN (usado nas regras)
 * - holiday: true/false apenas para relatórios (não altera a escala)
 */
public record Day(
        LocalDate date,
        DayType type,
        boolean holiday
) {

    public Day {
        Objects.requireNonNull(date, "Day.date cannot be null");
        Objects.requireNonNull(type, "Day.type cannot be null");
    }
}
