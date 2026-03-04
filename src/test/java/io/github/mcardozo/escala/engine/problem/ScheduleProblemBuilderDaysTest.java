package io.github.mcardozo.escala.engine.problem;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.mcardozo.escala.domain.Day;

/**
 * BUG-001 Regression Tests (ADR-005 / MUB — Month Under Bounds)
 *
 * Contexto:
 * - O sistema falhava em runtime ao gerar 2026-02, tentando acessar 2026-03-01
 *   ("Schedule has no entry for date: 2026-03-01").
 *
 * O que estes testes garantem:
 * - A geração de dias do mês é estritamente limitada ao YearMonth informado (MUB).
 * - Regras verificadas com asserts diretos (sem reflection):
 *   1) size == month.lengthOfMonth()
 *   2) first == month.atDay(1)
 *   3) last == month.atEndOfMonth()
 *   4) allMatchMonth: nenhum Day possui date fora do YearMonth
 *
 * Decisão de teste (QA-approved):
 * - Usar o método generateDays(YearMonth) como package-private e testar no mesmo package,
 *   evitando acoplamento com build() e evitando reflection.
 */


class ScheduleProblemBuilderDaysTest {

    @Test
    void feb_2026_has_28_days_and_bounds_are_correct() {
        assertMonthDays(YearMonth.of(2026, 2));
    }

    @Test
    void feb_2024_leap_year_has_29_days_and_bounds_are_correct() {
        assertMonthDays(YearMonth.of(2024, 2));
    }

    @Test
    void mar_2026_has_31_days_and_bounds_are_correct() {
        assertMonthDays(YearMonth.of(2026, 3));
    }

    @Test
    void apr_2026_has_30_days_and_bounds_are_correct() {
        assertMonthDays(YearMonth.of(2026, 4));
    }

    private static void assertMonthDays(YearMonth month) {
        ScheduleProblemBuilder builder = new ScheduleProblemBuilder();
        List<Day> days = builder.generateDays(month); // package-private

        int expectedSize = month.lengthOfMonth();
        LocalDate expectedFirst = month.atDay(1);
        LocalDate expectedLast = month.atEndOfMonth();

        assertNotNull(days, "days must not be null for month=" + month);
        assertEquals(expectedSize, days.size(), "size mismatch for month=" + month);

        LocalDate actualFirst = days.get(0).date();
        LocalDate actualLast = days.get(days.size() - 1).date();

        assertEquals(expectedFirst, actualFirst, "first day mismatch for month=" + month);
        assertEquals(expectedLast, actualLast, "last day mismatch for month=" + month);

        assertTrue(
                days.stream().allMatch(d -> YearMonth.from(d.date()).equals(month)),
                "found date outside month=" + month
        );
    }
}