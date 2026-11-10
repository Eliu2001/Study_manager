package cl.germain.studymanager.assessment;

import cl.germain.studymanager.reminder.ReminderRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIf("cl.germain.studymanager.TestDatabase#isAvailable")
class AssessmentReschedulingIntegrationTest {

    @Autowired private AssessmentService service;
    @Autowired private AssessmentRepository assessments;
    @Autowired private ReminderRepository reminders;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;

    private Long userId;
    private Long assessmentId;
    private Long reminderId;
    private final LocalDate originalDate = LocalDate.of(2026, 11, 10);

    @BeforeEach
    void setUp() {
        userId = jdbc.queryForObject("""
                insert into users (name, email, password_hash) values ('Rescheduling test', ?, 'unused') returning id
                """, Long.class, "rescheduling-" + UUID.randomUUID() + "@example.com");
        Long termId = jdbc.queryForObject("""
                insert into terms (user_id, name, start_date, end_date)
                values (?, 'Rescheduling test', '2026-03-01', '2026-12-01') returning id
                """, Long.class, userId);
        Long courseId = jdbc.queryForObject(
                "insert into courses (term_id, name) values (?, 'Rescheduling test') returning id", Long.class, termId);
        assessmentId = service.create(userId, courseId, request(originalDate)).id();
        reminderId = jdbc.queryForObject("""
                insert into reminders (assessment_id, days_before, sent, sent_at)
                values (?, 1, true, '2026-11-10T12:00:00Z') returning id
                """, Long.class, assessmentId);
    }

    @AfterEach
    void cleanUp() {
        if (userId != null) {
            jdbc.update("delete from users where id = ?", userId);
        }
    }

    @Test
    void reschedulingPersistsManagedAssessmentAndReminderTogether() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Assessment managedAssessment = assessments.findById(assessmentId).orElseThrow();
            var managedReminder = reminders.findById(reminderId).orElseThrow();

            service.update(userId, assessmentId, request(originalDate.plusDays(2)));

            assertThat(entityManager.contains(managedAssessment)).isTrue();
            assertThat(entityManager.contains(managedReminder)).isTrue();
            assertThat(managedReminder.isSent()).isFalse();
            assertThat(managedReminder.getSentAt()).isNull();
        });

        assertThat(assessments.findById(assessmentId).orElseThrow().getDueDate()).isEqualTo(originalDate.plusDays(2));
        var persisted = reminders.findById(reminderId).orElseThrow();
        assertThat(persisted.isSent()).isFalse();
        assertThat(persisted.getSentAt()).isNull();
    }

    @Test
    void rollbackRestoresBothDateAndDismissalEvenAfterFlush() {
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            service.update(userId, assessmentId, request(originalDate.plusDays(2)));
            entityManager.flush();
            throw new IllegalStateException("Force rollback after rescheduling");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(assessments.findById(assessmentId).orElseThrow().getDueDate()).isEqualTo(originalDate);
        var persisted = reminders.findById(reminderId).orElseThrow();
        assertThat(persisted.isSent()).isTrue();
        assertThat(persisted.getSentAt()).isEqualTo(java.time.Instant.parse("2026-11-10T12:00:00Z"));
    }

    private AssessmentRequest request(LocalDate date) {
        return new AssessmentRequest("Reprogramada", AssessmentType.SUMMATIVE,
                new BigDecimal("20"), date, null, null, null, null);
    }
}
