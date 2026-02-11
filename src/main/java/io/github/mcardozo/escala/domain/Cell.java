package io.github.mcardozo.escala.domain;

import java.util.EnumSet;
import java.util.Objects;

/**
 * ✅ CLASS final (imutável)
 *
 * Representa a célula da escala: "qual é o estado do funcionário X no dia Y".
 *
 * Por que não record?
 * - Dá para usar record também, mas aqui preferimos class para:
 *   - controlar cópias defensivas do EnumSet
 *   - oferecer métodos utilitários (withState, lock, addTag, etc.)
 *
 * Regras:
 * - state nunca pode ser null (P/H/F/FB/do)
 * - manualLock - se foi editado manualmente ou travado por request)
 * - tags nunca pode ser null (guardamos como EnumSet) -  TRAINING etc
 * - note pode ser null, mas vamos normalizar para "" para simplificar - Informação livre
 */
  //Design: classe imutável (cada alteração cria uma nova instância). Isso deixa o motor mais seguro e evita bugs.

public final class Cell {

    private final State state;
    private final boolean manualLock;
    private final EnumSet<CellTag> tags;
    private final String note;

    private Cell(State state, boolean manualLock, EnumSet<CellTag> tags, String note) {
        this.state = Objects.requireNonNull(state, "Cell.state cannot be null");
        this.manualLock = manualLock;
        this.tags = Objects.requireNonNull(tags, "Cell.tags cannot be null");
        this.note = (note == null) ? "" : note;
    }

    /**
     * Factory padrão: cria uma célula "normal" (sem lock, sem tags).
     */
    public static Cell of(State state) {
        return new Cell(state, false, EnumSet.noneOf(CellTag.class), "");
    }

    /**
     * Factory completa.
     * Fazemos cópia defensiva do EnumSet para manter imutabilidade.
     */
    public static Cell of(State state, boolean manualLock, EnumSet<CellTag> tags, String note) {
        EnumSet<CellTag> safeCopy = (tags == null)
                ? EnumSet.noneOf(CellTag.class)
                : EnumSet.copyOf(tags);

        return new Cell(state, manualLock, safeCopy, note);
    }

    public State state() {
        return state;
    }

    public boolean manualLock() {
        return manualLock;
    }

    /**
     * Retorna cópia defensiva para proteger o estado interno.
     */
    public EnumSet<CellTag> tags() {
        return EnumSet.copyOf(tags);
    }

    public String note() {
        return note;
    }

    public boolean hasTag(CellTag tag) {
        return tags.contains(tag);
    }

    /**
     * Cria uma nova Cell com outro estado, preservando lock/tags/note.
     */
    public Cell withState(State newState) {
        return new Cell(newState, this.manualLock, EnumSet.copyOf(this.tags), this.note);
    }

    /**
     * Cria uma nova Cell com lock manual ativado.
     */
    public Cell lock() {
        return new Cell(this.state, true, EnumSet.copyOf(this.tags), this.note);
    }

    /**
     * Cria uma nova Cell com lock manual desativado.
     */
    public Cell unlock() {
        return new Cell(this.state, false, EnumSet.copyOf(this.tags), this.note);
    }

    /**
     * Cria uma nova Cell adicionando uma tag.
     */
    public Cell addTag(CellTag tag) {
        EnumSet<CellTag> newTags = EnumSet.copyOf(this.tags);
        newTags.add(Objects.requireNonNull(tag, "tag cannot be null"));
        return new Cell(this.state, this.manualLock, newTags, this.note);
    }

    /**
     * Cria uma nova Cell removendo uma tag.
     */
    public Cell removeTag(CellTag tag) {
        EnumSet<CellTag> newTags = EnumSet.copyOf(this.tags);
        newTags.remove(tag);
        return new Cell(this.state, this.manualLock, newTags, this.note);
    }

    /**
     * Cria uma nova Cell com uma nota (texto livre).
     */
    public Cell withNote(String newNote) {
        return new Cell(this.state, this.manualLock, EnumSet.copyOf(this.tags), newNote);
    }

    @Override
    public String toString() {
        return "Cell{state=" + state + ", manualLock=" + manualLock + ", tags=" + tags + ", note='" + note + "'}";
    }
}
