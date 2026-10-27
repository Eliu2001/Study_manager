package cl.germain.studymanager.reminder;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    List<Reminder> findAllByAssessmentId(Long assessmentId);

    Optional<Reminder> findByIdAndAssessmentCourseTermUserId(Long id, Long userId);

    List<Reminder> findAllByAssessmentCourseTermUserIdAndSentFalse(Long userId);

    // Consulta explicita a proposito: en un nombre derivado, "DaysBefore" se leeria como
    // la propiedad "days" mas la palabra clave "Before" y Spring Data no arrancaria.
    @Query("select count(r) from Reminder r "
            + "where r.assessment.id = :assessmentId and r.daysBefore = :daysBefore")
    long countWithNotice(@Param("assessmentId") Long assessmentId,
                         @Param("daysBefore") Short daysBefore);
}