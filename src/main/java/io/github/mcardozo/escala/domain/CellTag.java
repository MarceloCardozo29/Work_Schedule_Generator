package io.github.mcardozo.escala.domain;


public enum CellTag {
    TRAINING
}

/* Tags opcionais para uma célula da escala.
 * Elas não substituem o State, mas adicionam "metadados".
 *
 * Exemplo:
 * - TRAINING: dia de treinamento que exige presença (P obrigatório).
 *
 * Usamos EnumSet para guardar tags de forma eficiente.
 */