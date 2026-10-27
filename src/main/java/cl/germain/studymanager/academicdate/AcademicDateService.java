package cl.germain.studymanager.academicdate;

import cl.germain.studymanager.common.BusinessRuleException;
import cl.germain.studymanager.common.ResourceNotFoundException;
import cl.germain.studymanager.course.Course;
import cl.germain.studymanager.course.CourseRepository;
import cl.germain.studymanager.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AcademicDateService {

    private static final LocalDate MIN_DATE = LocalDate.of(1900, 1, 1);
    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    private final AcademicDateRepository academicDateRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @Transactional
    public AcademicDateResponse create(Long userId, AcademicDateRequest request) {
        AcademicDate date = new AcademicDate();
        date.setUser(userRepository.getReferenceById(userId));
        apply(userId, date, request);
        return AcademicDateResponse.from(academicDateRepository.save(date));
    }

    @Transactional(readOnly = true)
    public List<AcademicDateResponse> list(Long userId, LocalDate from, LocalDate to) {
        LocalDate start = from == null ? MIN_DATE : from;
        LocalDate end = to == null ? MAX_DATE : to;
        if (end.isBefore(start)) {
            throw new BusinessRuleException("'to' must not be before 'from'");
        }
        return academicDateRepository
                .findAllByUserIdAndEventDateBetweenOrderByEventDateAscIdAsc(userId, start, end).stream()
                .map(AcademicDateResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AcademicDateResponse get(Long userId, Long id) {
        return AcademicDateResponse.from(find(userId, id));
    }

    @Transactional
    public AcademicDateResponse update(Long userId, Long id, AcademicDateRequest request) {
        AcademicDate date = find(userId, id);
        apply(userId, date, request);
        return AcademicDateResponse.from(date);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        academicDateRepository.delete(find(userId, id));
    }

    private AcademicDate find(Long userId, Long id) {
        return academicDateRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic date", id));
    }

    private void apply(Long userId, AcademicDate date, AcademicDateRequest request) {
        Course course = null;
        if (request.courseId() != null) {
            course = courseRepository.findByIdAndTermUserId(request.courseId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Course", request.courseId()));
        }
        date.setCourse(course);
        date.setTitle(request.title().trim());
        date.setEventDate(request.eventDate());
        date.setType(request.type());
    }
}
