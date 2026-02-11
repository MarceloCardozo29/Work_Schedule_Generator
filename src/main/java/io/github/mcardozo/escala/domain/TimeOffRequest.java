package io.github.mcardozo.escala.domain;

import java.time.LocalDate;
import java.util.Objects;

/*
 * Representa um pedido/restrição do tipo:
 * "no dia X, o funcionário Y deve estar no estado Z".
 *
 * Exemplos:
 * - "Dia 12 fulano precisa F (folga)"
 * - "Dia 20 beltrano precisa DO (aniversário)"
 *
 * O motor transformará isso em "lock" inicial na célula correspondente.
 */
public record TimeOffRequest(
        EmployeeId employeeId,
        LocalDate date,
        State requiredState,
        String reason,
        RequestSource source
) {
    public TimeOffRequest {
        Objects.requireNonNull(employeeId, "TimeOffRequest.employeeId cannot be null");
        Objects.requireNonNull(date, "TimeOffRequest.date cannot be null");
        Objects.requireNonNull(requiredState, "TimeOffRequest.requiredState cannot be null");
        Objects.requireNonNull(source, "TimeOffRequest.source cannot be null");

        // reason é texto livre: pode ser null, mas normalizamos para "" para evitar null-checks
        if (reason == null) {
            reason = "";
        }
    }
}
