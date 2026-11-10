package cl.germain.studymanager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/** Comprueba la restriccion PostgreSQL sin pasar por las validaciones del servicio. */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIf("cl.germain.studymanager.TestDatabase#isAvailable")
@Transactional
class UniqueExamPerCourseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    private long courseId;
    private long otherCourseId;

    @BeforeEach
    void createCourses() {
        Long userId = jdbc.queryForObject("""
                INSERT INTO users (name, email, password_hash)
                VALUES ('Usuario IT', ?, 'not-used') RETURNING id
                """, Long.class, "exam-it-" + UUID.randomUUID() + "@example.com");
        Long termId = jdbc.queryForObject("""
                INSERT INTO terms (user_id, name, start_date, end_date)
                VALUES (?, 'Periodo IT', DATE '2027-03-01', DATE '2027-07-15') RETURNING id
                """, Long.class, userId);
        courseId = createCourse(termId, "Curso A");
        otherCourseId = createCourse(termId, "Curso B");
    }

    @Test
    void rejectsSecondExamInsertedDirectlyForTheSameCourse() {
        createAssessment(courseId, "EXAM");

        assertUniqueExamViolation(() -> createAssessment(courseId, "EXAM"));
    }

    @Test
    void rejectsChangingSummativeToExamWhenCourseAlreadyHasOne() {
        createAssessment(courseId, "EXAM");
        long summativeId = createAssessment(courseId, "SUMMATIVE");

        assertUniqueExamViolation(() -> jdbc.update(
                "UPDATE assessments SET type = 'EXAM' WHERE id = ?", summativeId));
    }

    @Test
    void rejectsMovingExamToCourseThatAlreadyHasOne() {
        createAssessment(courseId, "EXAM");
        long otherExamId = createAssessment(otherCourseId, "EXAM");

        assertUniqueExamViolation(() -> jdbc.update(
                "UPDATE assessments SET course_id = ? WHERE id = ?", courseId, otherExamId));
    }

    @Test
    void allowsMultipleSummativesAndOneExamInEachCourseAndEditingThatExam() {
        createAssessment(courseId, "SUMMATIVE");
        createAssessment(courseId, "SUMMATIVE");
        long examId = createAssessment(courseId, "EXAM");
        createAssessment(otherCourseId, "EXAM");

        assertThat(jdbc.update("UPDATE assessments SET name = 'Examen actualizado' WHERE id = ?", examId))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM assessments WHERE course_id = ?", Long.class, courseId))
                .isEqualTo(3L);
    }

    private long createCourse(long termId, String name) {
        return jdbc.queryForObject("INSERT INTO courses (term_id, name) VALUES (?, ?) RETURNING id",
                Long.class, termId, name);
    }

    private long createAssessment(long targetCourseId, String type) {
        return jdbc.queryForObject("""
                INSERT INTO assessments (course_id, name, type, weight)
                VALUES (?, 'Evaluacion IT', ?, 20) RETURNING id
                """, Long.class, targetCourseId, type);
    }

    private void assertUniqueExamViolation(Runnable write) {
        assertThatExceptionOfType(DataIntegrityViolationException.class)
                .isThrownBy(write::run)
                .satisfies(exception -> assertThat(exception.getMostSpecificCause())
                        .isInstanceOfSatisfying(SQLException.class, cause -> {
                            assertThat(cause.getSQLState()).isEqualTo("23505");
                            assertThat(cause.getMessage()).contains("uq_assessments_exam_per_course");
                        }));
    }
}
