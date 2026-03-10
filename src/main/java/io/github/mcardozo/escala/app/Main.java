package io.github.mcardozo.escala.app;

import io.github.mcardozo.escala.domain.CompPreference;
import io.github.mcardozo.escala.domain.DayComposition;
import io.github.mcardozo.escala.domain.Employee;
import io.github.mcardozo.escala.domain.EmployeeId;
import io.github.mcardozo.escala.domain.MonthlyPolicy;
import io.github.mcardozo.escala.domain.Weights;
import io.github.mcardozo.escala.domain.WeeklyGoal;
import io.github.mcardozo.escala.engine.problem.ScheduleProblem;
import io.github.mcardozo.escala.engine.problem.ScheduleProblemBuilder;

import java.time.YearMonth;
import java.util.List;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        YearMonth month = YearMonth.of(2026, 2);

        List<Employee> employees = List.of(
                Employee.normal(new EmployeeId("E1"), "Beatriz", 2, CompPreference.NONE),
                Employee.normal(new EmployeeId("E2"), "Cristian", 4, CompPreference.NONE),
                Employee.normal(new EmployeeId("E3"), "Marcelo", 1, CompPreference.NONE),
                Employee.normal(new EmployeeId("E4"), "Thalles", 3, CompPreference.NONE)
        );

        MonthlyPolicy policy = new MonthlyPolicy(
                new DayComposition(2, 2, 0),
                new DayComposition(2, 1, 1),
                new DayComposition(2, 0, 2),
                false,
                false,
                new WeeklyGoal(3, 2, 2, 3, 2, 3),
                new Weights(10, 10, 10, 10, 10),
                42L
        );

        ScheduleProblem problem = new ScheduleProblemBuilder().build(
                month,
                employees,
                policy,
                List.of(),
                List.of(),
                List.of()
        );

        System.out.println("Problem created successfully.");
        System.out.println("Month: " + month);
        System.out.println("Employees: " + employees.size());
        System.out.println("Days generated: " + problem.days().size());
    }
}