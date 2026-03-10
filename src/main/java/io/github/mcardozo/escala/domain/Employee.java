package io.github.mcardozo.escala.domain;

import io.github.mcardozo.escala.domain.enums.TrainingMode;

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
 * IMPORTANTE SOBRE preferredComp:
 * - preferredComp NÃO deve ser null.
 * - Se a pessoa não tem preferência, use CompPreference.NONE.
 * - A regra de "se um é NONE, atende a preferência do outro" é uma POLÍTICA do motor,
 *   não do domínio. O domínio só expressa o dado: A/B/NONE
 *
 * IMPORTANTE SOBRE trainingMode:
 * - trainingMode NÃO deve ser null.
 * - Se a pessoa não está em treinamento, use TrainingMode.NONE.
 * - Regras de treinamento são aplicadas pelo motor/constraints, não por este record.
 */
public record Employee(
        EmployeeId id,
        String name,
        int seniorityRank,
        CompPreference preferredComp,
        TrainingMode trainingMode
) {

    /**
     * Construtor compacto do record para validar invariantes do domínio.
     */
    public Employee {
        Objects.requireNonNull(id, "Employee.id cannot be null");
        Objects.requireNonNull(name, "Employee.name cannot be null");
        Objects.requireNonNull(preferredComp, "Employee.preferredComp cannot be null");
        Objects.requireNonNull(trainingMode, "Employee.trainingMode cannot be null");

        if (name.isBlank()) {
            throw new IllegalArgumentException("Employee.name cannot be blank");
        }

        if (seniorityRank < 0) {
            throw new IllegalArgumentException("Employee.seniorityRank cannot be negative");
        }
    }

    /**
     * Factory para funcionário sem treinamento.
     */
    public static Employee normal(
            EmployeeId id,
            String name,
            int seniorityRank,
            CompPreference preferredComp
    ) {
        return new Employee(id, name, seniorityRank, preferredComp, TrainingMode.NONE);
    }

    /**
     * Factory para funcionário em TRAINING_1.
     */
    public static Employee training1(
            EmployeeId id,
            String name,
            int seniorityRank,
            CompPreference preferredComp
    ) {
        return new Employee(id, name, seniorityRank, preferredComp, TrainingMode.TRAINING_1);
    }

    /**
     * Factory para funcionário em TRAINING_2.
     */
    public static Employee training2(
            EmployeeId id,
            String name,
            int seniorityRank,
            CompPreference preferredComp
    ) {
        return new Employee(id, name, seniorityRank, preferredComp, TrainingMode.TRAINING_2);
    }
}