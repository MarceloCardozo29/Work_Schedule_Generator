package io.github.mcardozo.escala.domain;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * ✅ CLASS final (modelo principal do domínio para o mês)
 *
 * Representa a escala do mês inteiro.
 *
 * Estrutura do grid:
 * - Para cada LocalDate do mês
 * - Existe um Map<EmployeeId, Cell> com a célula daquele funcionário naquele dia
 *
 * Exemplo mental:
 * grid.get(2026-03-10).get(empIdFulano) -> Cell{state=P, manualLock=false, tags=[...], note="..."}
 *
 * IMPORTANTE (design):
 * - A escala pode ser mutável internamente (setCell), porque o gerador/otimizador precisa editar.
 * - Mesmo assim, oferecemos deepCopy() para estratégias de otimização e segurança.
 * - employees é um snapshot imutável: não deve mudar dentro do mês.
 */
public final class Schedule {

    private final YearMonth month;
    private final List<Employee> employees; // snapshot imutável
    private final Map<LocalDate, Map<EmployeeId, Cell>> grid;

    /**
     * Construtor principal.
     *
     * Normalmente você cria um Schedule "vazio" com createEmpty(...)
     * e depois o motor vai preenchendo com setCell(...).
     */
    public Schedule(YearMonth month,
                    List<Employee> employees,
                    Map<LocalDate, Map<EmployeeId, Cell>> grid) {

        this.month = Objects.requireNonNull(month, "Schedule.month cannot be null");
        Objects.requireNonNull(employees, "Schedule.employees cannot be null");
        Objects.requireNonNull(grid, "Schedule.grid cannot be null");

        // Guardamos snapshot imutável de employees (evita mudanças externas)
        this.employees = List.copyOf(employees);

        // grid é uma estrutura mutável (setCell precisa atualizar),
        // mas validamos se está minimamente consistente.
        this.grid = new HashMap<>();
        for (Map.Entry<LocalDate, Map<EmployeeId, Cell>> entry : grid.entrySet()) {
            LocalDate date = Objects.requireNonNull(entry.getKey(), "Schedule.grid date key cannot be null");
            Map<EmployeeId, Cell> dayMap = Objects.requireNonNull(entry.getValue(), "Schedule.grid dayMap cannot be null");

            // Copiamos o map interno para evitar referência externa.
            Map<EmployeeId, Cell> safeDayMap = new HashMap<>();
            for (Map.Entry<EmployeeId, Cell> cellEntry : dayMap.entrySet()) {
                EmployeeId empId = Objects.requireNonNull(cellEntry.getKey(), "EmployeeId key cannot be null");
                Cell cell = Objects.requireNonNull(cellEntry.getValue(), "Cell cannot be null");
                safeDayMap.put(empId, cell);
            }

            this.grid.put(date, safeDayMap);
        }
    }

    /**
     * Factory para criar uma escala vazia do mês com todas as células inicializadas.
     *
     * Estado padrão recomendado:
     * - Começar com F (folga) deixa claro que o motor vai preencher depois.
     * - Você pode trocar para H/P se preferir, mas F é mais neutro.
     */
    public static Schedule createEmpty(YearMonth month, List<Employee> employees, State defaultState) {
        Objects.requireNonNull(month, "month cannot be null");
        Objects.requireNonNull(employees, "employees cannot be null");
        Objects.requireNonNull(defaultState, "defaultState cannot be null");

        Map<LocalDate, Map<EmployeeId, Cell>> grid = new HashMap<>();

        // Mapeamos os ids uma vez para evitar recalcular em loops
        List<EmployeeId> employeeIds = employees.stream().map(Employee::id).toList();

        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);

            Map<EmployeeId, Cell> dayMap = new HashMap<>();
            for (EmployeeId id : employeeIds) {
                // célula default: sem lock, sem tags
                dayMap.put(id, Cell.of(defaultState));
            }

            grid.put(date, dayMap);
        }

        return new Schedule(month, employees, grid);
    }

    public YearMonth month() {
        return month;
    }

    /**
     * Snapshot imutável dos funcionários do mês.
     */
    public List<Employee> employees() {
        return employees;
    }

    /**
     * Retorna as datas disponíveis na escala (normalmente todos os dias do mês).
     * Usamos uma lista ordenada para varreduras previsíveis.
     */
    public List<LocalDate> dates() {
        List<LocalDate> dates = new ArrayList<>(grid.keySet());
        dates.sort(Comparator.naturalOrder());
        return List.copyOf(dates);
    }

    /**
     * Obtém a célula de um funcionário em um dia.
     * Lança exceção se a data/funcionário não existirem no grid.
     */
    public Cell getCell(LocalDate date, EmployeeId employeeId) {
        Objects.requireNonNull(date, "date cannot be null");
        Objects.requireNonNull(employeeId, "employeeId cannot be null");

        Map<EmployeeId, Cell> dayMap = grid.get(date);
        if (dayMap == null) {
            throw new IllegalArgumentException("Schedule has no entry for date: " + date);
        }

        Cell cell = dayMap.get(employeeId);
        if (cell == null) {
            throw new IllegalArgumentException("Schedule has no entry for employeeId: " + employeeId + " on date: " + date);
        }

        return cell;
    }

    /**
     * Atualiza a célula de um funcionário em um dia.
     *
     * Observação:
     * - Essa operação NÃO faz validação de regras (isso é papel do engine/constraints).
     * - Aqui validamos apenas integridade estrutural do grid.
     */
    public void setCell(LocalDate date, EmployeeId employeeId, Cell cell) {
        Objects.requireNonNull(date, "date cannot be null");
        Objects.requireNonNull(employeeId, "employeeId cannot be null");
        Objects.requireNonNull(cell, "cell cannot be null");

        Map<EmployeeId, Cell> dayMap = grid.get(date);
        if (dayMap == null) {
            throw new IllegalArgumentException("Schedule has no entry for date: " + date);
        }

        if (!dayMap.containsKey(employeeId)) {
            throw new IllegalArgumentException("Schedule has no entry for employeeId: " + employeeId + " on date: " + date);
        }

        dayMap.put(employeeId, cell);
    }

    /**
     * Retorna uma cópia profunda (deep copy) do Schedule.
     *
     * Usos:
     * - otimização (testar swaps sem mexer no original)
     * - simulações
     */
    public Schedule deepCopy() {
        Map<LocalDate, Map<EmployeeId, Cell>> newGrid = new HashMap<>();

        for (Map.Entry<LocalDate, Map<EmployeeId, Cell>> entry : this.grid.entrySet()) {
            LocalDate date = entry.getKey();
            Map<EmployeeId, Cell> dayMap = entry.getValue();

            Map<EmployeeId, Cell> copiedDayMap = new HashMap<>();
            for (Map.Entry<EmployeeId, Cell> cellEntry : dayMap.entrySet()) {
                // Cell é imutável, então podemos reutilizar a referência com segurança
                copiedDayMap.put(cellEntry.getKey(), cellEntry.getValue());
            }

            newGrid.put(date, copiedDayMap);
        }

        return new Schedule(this.month, this.employees, newGrid);
    }

    /**
     * Utilitário para varrer todas as células do mês de forma ordenada.
     * Isso ajuda relatórios e validações.
     */
    public Iterable<CellRef> allCells() {
        List<CellRef> refs = new ArrayList<>();

        for (LocalDate date : dates()) {
            Map<EmployeeId, Cell> dayMap = grid.get(date);

            // Ordena por id apenas para ser determinístico (debug e testes)
            List<EmployeeId> ids = new ArrayList<>(dayMap.keySet());
            ids.sort(Comparator.comparing(EmployeeId::value));

            for (EmployeeId id : ids) {
                refs.add(new CellRef(date, id, dayMap.get(id)));
            }
        }

        return refs;
    }

    /**
     * ✅ RECORD interno (imutável)
     *
     * Representa uma "visão" de uma célula: data + funcionário + célula.
     * Ajuda a varrer a escala sem expor a estrutura interna do Map.
     */
    public record CellRef(LocalDate date, EmployeeId employeeId, Cell cell) { }
}
