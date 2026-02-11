package io.github.mcardozo.escala.engine.validation;

import io.github.mcardozo.escala.domain.EmployeeId;

import java.time.LocalDate;
import java.util.Objects;

/* Representa uma violação (ou aviso) detectado durante validação.
 *
 * Campos importantes:
 * - code: código curto para facilitar debug e testes (ex.: "WEEKEND_COMPOSITION")
 * - severity: HARD ou WARN
 * - date: data associada (pode ser null se for algo "global", mas aqui vamos permitir null)
 * - employeeId: funcionário associado (pode ser null se for algo "global")
 * - message: descrição legível
 *
 * Observação:
 * - date e employeeId podem ser null para violações que não se aplicam a um ponto único.
 * - message e code nunca devem ser null/blank.
 */
public record Violation(
        String code,
        Severity severity,
        LocalDate date,
        EmployeeId employeeId,
        String message
) {
    public Violation {
        Objects.requireNonNull(code, "Violation.code cannot be null");
        Objects.requireNonNull(severity, "Violation.severity cannot be null");
        Objects.requireNonNull(message, "Violation.message cannot be null");

        if (code.isBlank()) {
            throw new IllegalArgumentException("Violation.code cannot be blank");
        }
        if (message.isBlank()) {
            throw new IllegalArgumentException("Violation.message cannot be blank");
        }
    }

    /**
     * Factory rápida para violações HARD.
     */
    public static Violation hard(String code, LocalDate date, EmployeeId employeeId, String message) {
        return new Violation(code, Severity.HARD, date, employeeId, message);
    }

    /**
     * Factory rápida para warnings.
     */
    public static Violation warn(String code, LocalDate date, EmployeeId employeeId, String message) {
        return new Violation(code, Severity.WARN, date, employeeId, message);
    }
}
