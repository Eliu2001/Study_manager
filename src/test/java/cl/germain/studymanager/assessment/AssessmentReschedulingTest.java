package cl.germain.studymanager.assessment;

import cl.germain.studymanager.common.BusinessRuleException;
import cl.germain.studymanager.course.Course;
import cl.germain.studymanager.course.CourseRepository;
import cl.germain.studymanager.reminder.Reminder;
import cl.germain.studymanager.reminder.ReminderRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssessmentReschedulingTest {

    @Mock private AssessmentRepository assessments;
    @Mock private CourseRepository courses;
    @Mock private EntityManager entityManager;
    @Mock private ReminderRepository reminders;
    @InjectMocks private AssessmentService service;

    private Assessment assessment;
    private final Instant dismissedAt = Instant.parse("2026-11-10T12:00:00Z");

    @BeforeEach
    void setUp() {
        Course course = new Course();
        course.setId(10L);
        assessment = new Assessment();
        assessment.setId(20L);
        assessment.setCourse(course);
        when(assessments.findByIdAndCourseTermUserId(20L, 1L)).thenReturn(Optional.of(assessment));
        when(courses.findByIdAndTermUserIdForUpdate(10L, 1L)).thenReturn(Optional.of(course));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "2026-11-10,2026-11-12", // Un aviso descartado hoy vuelve a ser elegible al reprogramar.
            "NULL,2026-11-12",
            "2026-11-10,NULL"
    }, nullValues = "NULL")
    void changedDateReactivatesAllReminders(LocalDate previous, LocalDate next) {
        assessment.setDueDate(previous);
        Reminder dismissed = reminder(true, dismissedAt);
        Reminder pending = reminder(false, null);
        when(reminders.findAllByAssessmentId(20L)).thenReturn(List.of(dismissed, pending));

        service.update(1L, 20L, request(next));

        assertThat(assessment.getDueDate()).isEqualTo(next);
        assertThat(dismissed.isSent()).isFalse();
        assertThat(dismissed.getSentAt()).isNull();
        assertThat(pending.isSent()).isFalse();
        assertThat(pending.getSentAt()).isNull();
        verify(entityManager).refresh(assessment);
        verify(entityManager, never()).clear();
    }

    @ParameterizedTest
    @CsvSource(value = {"2026-11-10", "NULL"}, nullValues = "NULL")
    void unchangedDateDoesNotReadOrResetReminders(LocalDate date) {
        assessment.setDueDate(date);

        service.update(1L, 20L, request(date));

        assertThat(assessment.getDueDate()).isEqualTo(date);
        verifyNoInteractions(reminders);
    }

    @Test
    void validationFailureDoesNotChangeDateOrReminders() {
        LocalDate previous = LocalDate.of(2026, 11, 10);
        assessment.setDueDate(previous);
        when(assessments.sumWeightByCourseIdExcluding(10L, 20L)).thenReturn(new BigDecimal("90"));

        assertThatThrownBy(() -> service.update(1L, 20L, request(previous.plusDays(2))))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(assessment.getDueDate()).isEqualTo(previous);
        verifyNoInteractions(reminders);
    }

    private Reminder reminder(boolean sent, Instant sentAt) {
        Reminder reminder = new Reminder();
        reminder.setAssessment(assessment);
        reminder.setSent(sent);
        reminder.setSentAt(sentAt);
        return reminder;
    }

    private AssessmentRequest request(LocalDate date) {
        return new AssessmentRequest("Reprogramada", AssessmentType.SUMMATIVE,
                new BigDecimal("20"), date, null, null, null, null);
    }
}
