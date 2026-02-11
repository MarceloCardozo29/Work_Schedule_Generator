package io.github.mcardozo.escala.domain;

import java.util.Objects;

/* Representa um funcionário do sistema.
 *
 * Por que record?
 * - É imutável (ótimo para domínio).
 * - Tem equals/hashCode/toString prontos.
 * - Bom para snapshot do mês (Schedule guarda uma lista fixa de Employees).
 *
 * Campos:
 * - id: identificador único (value object)
 * - name: nome
 * - seniorityRank: número para desempates/regras futuras (ex.: mais antigo)
 * - preferredComp: preferência para compensação A/B (opcional)
 *
 *  * IMPORTANTE SOBRE preferredComp:
 * - preferredComp NÃO deve ser null.
 * - Se a pessoa não tem preferência, use CompPreference.NONE.
 * - A regra de "se um é NONE, atende a preferência do outro" é uma POLÍTICA do motor,
 *   não do domínio. O domínio só expressa o dado: A/B/NONE
 */
public record Employee(
        EmployeeId id,
        String name,
        int seniorityRank,
        CompPreference preferredComp
) {

    /**
     * Construtor compacto do record para validar invariantes do domínio.
     */
    public Employee {
        Objects.requireNonNull(id, "Employee.id cannot be null");
        Objects.requireNonNull(name, "Employee.name cannot be null");
        Objects.requireNonNull(preferredComp, "Employee.preferredComp cannot be null");

        if (name.isBlank()) {
            throw new IllegalArgumentException("Employee.name cannot be blank");
        }

        if (seniorityRank < 0) {
            throw new IllegalArgumentException("Employee.seniorityRank cannot be negative");
        }
    }
}
