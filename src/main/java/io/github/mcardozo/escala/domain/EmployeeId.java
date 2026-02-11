package io.github.mcardozo.escala.domain;

import java.util.Objects;

/* Identificador único de um funcionário.
 *
 * Por que usar record aqui?
 * - É imutável por padrão.
 * - Ótimo para "Value Object" (objeto-valor) no domínio.
 * - Facilita comparação e uso como chave em Map.
 *
 * Regras de validação:
 * - não pode ser null
 * - não pode ser vazio/em branco
 */
public record EmployeeId(String value) {

    /**
     * Construtor compacto do record.
     * Esse bloco roda sempre que eu criar um EmployeeId.
     */
    public EmployeeId {
        Objects.requireNonNull(value, "EmployeeId.value cannot be null");

        if (value.isBlank()) {
            throw new IllegalArgumentException("EmployeeId.value cannot be blank");
        }
    }

    /**
     * toString() retorna somente o valor,
     * deixando logs e mensagens mais legíveis.
     */
    @Override
    public String toString() {
        return value;
    }
}
