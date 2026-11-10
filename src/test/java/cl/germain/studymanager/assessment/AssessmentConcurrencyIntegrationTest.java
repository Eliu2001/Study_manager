package cl.germain.studymanager.assessment;

import cl.germain.studymanager.common.BusinessRuleException;
import cl.germain.studymanager.common.ConflictException;
import cl.germain.studymanager.common.ResourceNotFoundException;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Usa exclusivamente el perfil test y la base PostgreSQL separada, igual que ApiIntegrationTest. */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIf("cl.germain.studymanager.TestDatabase#isAvailable")
class AssessmentConcurrencyIntegrationTest {

    @Autowired private AssessmentService service;
    @Autowired private AssessmentRepository assessments;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;

    private Long userId;
    private Long courseId;

    @BeforeEach
    void setUp() {
        userId = jdbc.queryForObject("""
                insert into users (name, email, password_hash) values ('Concurrency test', ?, 'unused') returning id
                """, Long.class, "concurrency-" + UUID.randomUUID() + "@example.com");
        Long termId = jdbc.queryForObject("""
                insert into terms (user_id, name, start_date, end_date)
                values (?, 'Concurrency test', '2027-03-01', '2027-07-01') returning id
                """, Long.class, userId);
        courseId = jdbc.queryForObject("insert into courses (term_id, name) values (?, 'Concurrency test') returning id",
                Long.class, termId);
    }

    @AfterEach
    void cleanUp() {
        if (userId != null) {
            jdbc.update("delete from users where id = ?", userId);
        }
    }

    private AssessmentRequest request(AssessmentType type, String weight) {
        return new AssessmentRequest("Prueba", type, new BigDecimal(weight), null, null, null, null, null);
    }

    private List<String> race(Callable<String> operation) throws Exception {
        var barrier = new CyclicBarrier(2);
        Callable<String> contender = () -> {
            barrier.await(10, TimeUnit.SECONDS);
            return operation.call();
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(contender);
            var second = executor.submit(contender);
            return List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
        }
    }

    @Test
    void concurrentCreatesCannotExceedOneHundredPercent() throws Exception {
        List<String> outcomes = race(() -> {
            try {
                service.create(userId, courseId, request(AssessmentType.SUMMATIVE, "60"));
                return "created";
            } catch (BusinessRuleException expected) {
                return "rejected";
            }
        });

        assertThat(outcomes).containsExactlyInAnyOrder("created", "rejected");
        assertThat(assessments.sumWeightByCourseId(courseId)).isEqualByComparingTo("60");
    }

    @Test
    void concurrentCreatesCannotProduceTwoExams() throws Exception {
        List<String> outcomes = race(() -> {
            try {
                service.create(userId, courseId, request(AssessmentType.EXAM, "20"));
                return "created";
            } catch (ConflictException expected) {
                return "rejected";
            }
        });

        assertThat(outcomes).containsExactlyInAnyOrder("created", "rejected");
        assertThat(assessments.findAllByCourseIdOrderByDueDateAscIdAsc(courseId)).hasSize(1);
    }

    @Test
    void concurrentUpdatesCannotExceedOneHundredPercent() throws Exception {
        var ids = new ConcurrentLinkedQueue<Long>();
        ids.add(service.create(userId, courseId, request(AssessmentType.SUMMATIVE, "20")).id());
        ids.add(service.create(userId, courseId, request(AssessmentType.SUMMATIVE, "20")).id());

        List<String> outcomes = race(() -> {
            try {
                service.update(userId, ids.remove(), request(AssessmentType.SUMMATIVE, "70"));
                return "updated";
            } catch (BusinessRuleException expected) {
                return "rejected";
            }
        });

        assertThat(outcomes).containsExactlyInAnyOrder("updated", "rejected");
        assertThat(assessments.sumWeightByCourseId(courseId)).isEqualByComparingTo("90");
    }

    @Test
    void updateRefreshesAnEntityLoadedBeforeAnotherTransactionCommits() {
        Long id = service.create(userId, courseId, request(AssessmentType.SUMMATIVE, "20")).id();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            // Precarga deliberada: una segunda consulta JPA no basta para obtener el estado nuevo.
            assessments.findByIdAndCourseTermUserId(id, userId).orElseThrow();
            inAnotherTransaction(() -> service.update(userId, id, request(AssessmentType.SUMMATIVE, "40")));
            service.update(userId, id, request(AssessmentType.SUMMATIVE, "20"));
        });

        assertThat(assessments.findById(id).orElseThrow().getWeight()).isEqualByComparingTo("20");
    }

    @Test
    void updateDoesNotResurrectAnEntityDeletedAfterTheInitialRead() {
        Long id = service.create(userId, courseId, request(AssessmentType.SUMMATIVE, "20")).id();
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            assessments.findByIdAndCourseTermUserId(id, userId).orElseThrow();
            inAnotherTransaction(() -> service.delete(userId, id));
            service.update(userId, id, request(AssessmentType.SUMMATIVE, "20"));
        })).isInstanceOf(ResourceNotFoundException.class);

        assertThat(assessments.findById(id)).isEmpty();
    }

    private void inAnotherTransaction(Runnable operation) {
        try (var executor = Executors.newSingleThreadExecutor()) {
            executor.submit(operation).get(20, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new AssertionError("La transaccion concurrente no termino correctamente", exception);
        }
    }
}
