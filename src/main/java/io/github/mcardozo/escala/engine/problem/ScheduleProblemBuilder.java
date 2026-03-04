// O que esta classe faz?
// - Monta um ScheduleProblem "coeso" para um mês específico (YearMonth)
// - Gera a lista de dias (Day) APENAS dentro daquele mês (nunca passa para o mês seguinte)
// - Centraliza validações básicas de entrada
// - Oferece um "build mínimo" (6 args) e um "build completo" (7 args com history)
//
// IMPORTANTE:
// - Aqui NÃO criamos o Schedule (grid de cells). Isso é trabalho do Generator.
// - Aqui montamos APENAS o "problema": month + employees + days + policy + requests + constraints + history.

package io.github.mcardozo.escala.engine.problem;

import io.github.mcardozo.escala.domain.Day;
import io.github.mcardozo.escala.domain.enums.DayType;
import io.github.mcardozo.escala.domain.Employee;
import io.github.mcardozo.escala.domain.MonthlyPolicy;
import io.github.mcardozo.escala.domain.TimeOffRequest;
import io.github.mcardozo.escala.engine.history.EmptyHistoryProvider;
import io.github.mcardozo.escala.engine.history.HistoryProvider;
import io.github.mcardozo.escala.engine.validation.HardConstraint;
import io.github.mcardozo.escala.engine.validation.SoftConstraint;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ScheduleProblemBuilder {

    /**
     * ✅ Build mínimo e seguro:
     * - Usa EmptyHistoryProvider por padrão
     * - Constraints e requests já vêm de fora (Main ou futuro UI)
     */
    public ScheduleProblem build(YearMonth month,
                                 List<Employee> employees,
                                 MonthlyPolicy policy,
                                 List<TimeOffRequest> requests,
                                 List<HardConstraint> hardConstraints,
                                 List<SoftConstraint> softConstraints) {

        return build(month, employees, policy, requests, hardConstraints, softConstraints, new EmptyHistoryProvider());
    }

    /**
     * ✅ Build completo:
     * - Permite receber um HistoryProvider (ex.: banco, arquivo, memória)
     */
    public ScheduleProblem build(YearMonth month,
                                 List<Employee> employees,
                                 MonthlyPolicy policy,
                                 List<TimeOffRequest> requests,
                                 List<HardConstraint> hardConstraints,
                                 List<SoftConstraint> softConstraints,
                                 HistoryProvider history) {

        // 1) Validação básica de entradas
        Objects.requireNonNull(month, "month cannot be null");
        Objects.requireNonNull(employees, "employees cannot be null");
        Objects.requireNonNull(policy, "policy cannot be null");
        Objects.requireNonNull(requests, "requests cannot be null");
        Objects.requireNonNull(hardConstraints, "hardConstraints cannot be null");
        Objects.requireNonNull(softConstraints, "softConstraints cannot be null");
        Objects.requireNonNull(history, "history cannot be null");

        if (employees.isEmpty()) {
            throw new IllegalArgumentException("employees cannot be empty");
        }

        // 2) Gera os dias do mês (NUNCA passa para o próximo mês)
        List<Day> days = generateDays(month);

        // 3) Monta o problema com snapshots imutáveis
        //    (evita alguém alterar as listas "por fora" depois que o problema foi criado)
        return new ScheduleProblem(
                month,
                List.copyOf(employees),
                List.copyOf(days),
                policy,
                List.copyOf(requests),
                List.copyOf(hardConstraints),
                List.copyOf(softConstraints),
                history
        );
    }

    /**
     * ✅ Gera a lista de dias do mês.
     *
     * REGRA DE OURO para não ter bug de "dia do mês seguinte":
     * - iterar d = 1..lengthOfMonth()
     * - usar month.atDay(d)
     *
     * Isso garante que:
     * - fevereiro não vai gerar março
     * - meses de 30/31 dias não "estouram"
     */
     List<Day> generateDays(YearMonth month) {
        Objects.requireNonNull(month, "month cannot be null");

        List<Day> days = new ArrayList<>(month.lengthOfMonth());

        for (int d = 1; d <= month.lengthOfMonth(); d++) {
            LocalDate date = month.atDay(d);

            DayType type = switch (date.getDayOfWeek()) {
                case SATURDAY -> DayType.SAT;
                case SUNDAY -> DayType.SUN;
                default -> DayType.NORMAL;
            };

            // holiday aqui é só para relatório.
            // Se no futuro você integrar feriados, você vai preencher esse boolean.
            days.add(new Day(date, type, false));
        }

        return List.copyOf(days);
    }
}
