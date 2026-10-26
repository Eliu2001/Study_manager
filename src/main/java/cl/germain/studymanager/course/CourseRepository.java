package cl.germain.studymanager.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findAllByTermIdOrderByNameAsc(Long termId);

    Optional<Course> findByIdAndTermUserId(Long id, Long userId);

    boolean existsByTermIdAndName(Long termId, String name);

    boolean existsByTermIdAndNameAndIdNot(Long termId, String name, Long id);
}
