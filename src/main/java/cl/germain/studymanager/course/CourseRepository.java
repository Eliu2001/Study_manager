package cl.germain.studymanager.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findAllByTermIdOrderByNameAsc(Long termId);

    Optional<Course> findByIdAndTermUserId(Long id, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Course c where c.id = :id and c.term.user.id = :userId")
    Optional<Course> findByIdAndTermUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    boolean existsByTermIdAndName(Long termId, String name);

    boolean existsByTermIdAndNameAndIdNot(Long termId, String name, Long id);

    List<Course> findAllByTermUserIdOrderByNameAsc(Long userId);
}
