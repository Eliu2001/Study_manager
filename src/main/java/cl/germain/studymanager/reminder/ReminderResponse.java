package cl.germain.studymanager.reminder;

import java.time.Instant;

public record ReminderResponse(
        Long id,
        Long assessmentId,
        Short daysBefore,
        boolean sent,
        Instant sentAt
) {

    public static ReminderResponse from(Reminder reminder) {
        return new ReminderResponse(
                reminder.getId(),
                reminder.getAssessment().getId(),
                reminder.getDaysBefore(),
                reminder.isSent(),
                reminder.getSentAt());
    }
}
