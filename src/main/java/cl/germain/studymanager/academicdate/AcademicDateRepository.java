package cl.germain.studymanager.academicdate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AcademicDateRepository extends JpaRepository<AcademicDate, Long> {

    List<AcademicDate> findAllByUserIdAndEventDateBetweenOrderByEventDateAscIdAsc(
            Long userId, LocalDate from, LocalDate to);

    Optional<AcademicDate> findByIdAndUserId(Long id, Long userId);
}
