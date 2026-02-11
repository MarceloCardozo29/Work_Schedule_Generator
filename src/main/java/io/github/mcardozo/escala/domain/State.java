package io.github.mcardozo.escala.domain;

    public enum State {
        P,
        H,
        F,
        FB,
        DO,

    }

/**
  * Representa o estado de um funcionário em um dia específico da escala.
 * Um funcionário só pode ter 1 estado por dia.
 *
 * P  = Presencial
 * H  = Home office
 * F  = Folga
 * FB = Folga Banco de Horas
 * DO = Folga de Aniversário
 */