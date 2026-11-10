package cl.germain.studymanager.reminder;

import cl.germain.studymanager.assessment.Assessment;
import cl.germain.studymanager.assessment.AssessmentRepository;
import cl.germain.studymanager.course.Course;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReminderServiceTimeTest {

    private static final ZoneId SANTIAGO = ZoneId.of("America/Santiago");
    private final ReminderRepository reminders = mock(ReminderRepository.class);
    private final AssessmentRepository assessments = mock(AssessmentRepository.class);

    @Test
    void utcMidnightDoesNotStartTheNextBusinessDay() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-11T00:30:00Z"), SANTIAGO);
        Reminder reminder = reminder(LocalDate.of(2026, 7, 11), (short) 0);
        when(reminders.findAllByAssessmentCourseTermUserIdAndSentFalse(1L)).thenReturn(List.of(reminder));

        assertThat(LocalDate.now(clock.withZone(ZoneOffset.UTC))).isEqualTo(LocalDate.of(2026, 7, 11));
        assertThat(new ReminderService(reminders, assessments, clock).upcoming(1L)).isEmpty();
    }

    @Test
    void reminderForTodayDoesNotExpireAtUtcMidnight() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-11T00:30:00Z"), SANTIAGO);
        Reminder reminder = reminder(LocalDate.of(2026, 7, 10), (short) 0);
        when(reminders.findAllByAssessmentCourseTermUserIdAndSentFalse(1L)).thenReturn(List.of(reminder));

        assertThat(new ReminderService(reminders, assessments, clock).upcoming(1L))
                .singleElement().satisfies(notification -> assertThat(notification.daysLeft()).isZero());
    }

    @Test
    void windowAndDaysLeftUseBusinessDateInSummer() {
        Clock clock = Clock.fixed(Instant.parse("2026-01-11T00:30:00Z"), SANTIAGO);
        Reminder reminder = reminder(LocalDate.of(2026, 1, 12), (short) 2);
        when(reminders.findAllByAssessmentCourseTermUserIdAndSentFalse(1L)).thenReturn(List.of(reminder));

        assertThat(new ReminderService(reminders, assessments, clock).upcoming(1L))
                .singleElement().satisfies(notification -> assertThat(notification.daysLeft()).isEqualTo(2));
    }

    @Test
    void windowStartsAtBusinessMidnight() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-11T04:00:00Z"), SANTIAGO);
        Reminder reminder = reminder(LocalDate.of(2026, 7, 11), (short) 0);
        when(reminders.findAllByAssessmentCourseTermUserIdAndSentFalse(1L)).thenReturn(List.of(reminder));

        assertThat(new ReminderService(reminders, assessments, clock).upcoming(1L)).hasSize(1);
    }

    @Test
    void dismissUsesTheInjectedInstantAndPreservesItOnRepeatedDismiss() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-11T00:30:00Z"), SANTIAGO);
        Reminder reminder = reminder(LocalDate.of(2026, 7, 11), (short) 0);
        when(reminders.findByIdAndAssessmentCourseTermUserId(7L, 1L)).thenReturn(Optional.of(reminder));

        new ReminderService(reminders, assessments, clock).dismiss(1L, 7L);
        new ReminderService(reminders, assessments, Clock.offset(clock, java.time.Duration.ofHours(1)))
                .dismiss(1L, 7L);

        assertThat(reminder.isSent()).isTrue();
        assertThat(reminder.getSentAt()).isEqualTo(clock.instant());
    }

    private Reminder reminder(LocalDate dueDate, short daysBefore) {
        Course course = new Course();
        course.setId(3L);
        course.setName("Curso");
        Assessment assessment = new Assessment();
        assessment.setId(5L);
        assessment.setName("Evaluación");
        assessment.setCourse(course);
        assessment.setDueDate(dueDate);
        Reminder reminder = new Reminder();
        reminder.setId(7L);
        reminder.setAssessment(assessment);
        reminder.setDaysBefore(daysBefore);
        return reminder;
    }
}
