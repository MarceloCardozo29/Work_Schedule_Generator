package io.github.mcardozo.escala.domain;

/* Origem de uma solicitação (request) que vira uma restrição/lock na escala.
 *
 * Exemplos:
 * - MANUAL_EDIT: usuário alterou a célula na UI
 * - ADMIN_REQUEST: alguém "de cima" pediu/forçou algo
 * - IMPORT: veio de um arquivo/importação externa
 */
public enum RequestSource {
    MANUAL_EDIT,
    ADMIN_REQUEST,
    IMPORT
}
