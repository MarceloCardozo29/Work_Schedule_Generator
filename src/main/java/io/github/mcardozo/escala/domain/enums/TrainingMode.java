package io.github.mcardozo.escala.domain.enums;

/**
 * Modo de treinamento do funcionário.
 *
 * NONE: não está em treinamento.
 * TRAINING_1: presencial seg-sex, não trabalha finais de semana.
 * TRAINING_2: presencial, pode trabalhar finais de semana e segue folga compensatória padrão.
 */
public enum TrainingMode {
    NONE,
    TRAINING_1,
    TRAINING_2
}