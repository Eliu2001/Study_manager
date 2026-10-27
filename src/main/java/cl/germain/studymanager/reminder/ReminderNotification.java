package cl.germain.studymanager.reminder;

import java.time.LocalDate;

public record ReminderNotification(
        Long reminderId,
        Long assessmentId,
        String assessmentName,
        Long courseId,
        String courseName,
        LocalDate dueDate,
        long daysLeft,
        Short daysBefore
) {
}
