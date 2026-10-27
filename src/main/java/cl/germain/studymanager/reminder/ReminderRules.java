package cl.germain.studymanager.reminder;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Reglas puras de los recordatorios, sin base de datos, para poder probarlas facilmente.
 */
public final class ReminderRules {

    private ReminderRules() {
    }

    /**
     * Un recordatorio esta vigente desde (fecha de la prueba - dias de anticipacion)
     * hasta el mismo dia de la prueba, ambos inclusive.
     */
    public static boolean isDue(LocalDate today, LocalDate dueDate, int daysBefore) {
        return !today.isBefore(dueDate.minusDays(daysBefore)) && !today.isAfter(dueDate);
    }

    public static long daysLeft(LocalDate today, LocalDate dueDate) {
        return ChronoUnit.DAYS.between(today, dueDate);
    }
}
