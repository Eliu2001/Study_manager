package cl.germain.studymanager.assessment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    List<Assessment> findAllByCourseTermId(Long termId);

    List<Assessment> findAllByCourseTermUserId(Long userId);

    List<Assessment> findAllByCourseIdOrderByDueDateAscIdAsc(Long courseId);

    Optional<Assessment> findByIdAndCourseTermUserId(Long id, Long userId);

    boolean existsByCourseIdAndType(Long courseId, AssessmentType type);

    boolean existsByCourseIdAndTypeAndIdNot(Long courseId, AssessmentType type, Long id);

    @Query("select sum(a.weight) from Assessment a where a.course.id = :courseId")
    BigDecimal sumWeightByCourseId(@Param("courseId") Long courseId);

    @Query("select sum(a.weight) from Assessment a where a.course.id = :courseId and a.id <> :id")
    BigDecimal sumWeightByCourseIdExcluding(@Param("courseId") Long courseId, @Param("id") Long id);
}
