package cl.germain.studymanager.assessment;

import cl.germain.studymanager.common.BusinessRuleException;
import cl.germain.studymanager.common.ConflictException;
import cl.germain.studymanager.common.ResourceNotFoundException;
import cl.germain.studymanager.course.Course;
import cl.germain.studymanager.course.CourseRepository;
import cl.germain.studymanager.reminder.ReminderRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AssessmentService {

    private static final BigDecimal MAX_TOTAL_WEIGHT = new BigDecimal("100");

    private final AssessmentRepository assessmentRepository;
    private final CourseRepository courseRepository;
    private final EntityManager entityManager;
    private final ReminderRepository reminderRepository;

    @Transactional
    public AssessmentResponse create(Long userId, Long courseId, AssessmentRequest request) {
        Course course = lockCourse(userId, courseId);
        BigDecimal othersWeight = zeroIfNull(assessmentRepository.sumWeightByCourseId(courseId));
        validate(request, courseId, null, othersWeight);

        Assessment assessment = new Assessment();
        assessment.setCourse(course);
        apply(assessment, request);
        return AssessmentResponse.from(assessmentRepository.save(assessment));
    }

    @Transactional(readOnly = true)
    public List<AssessmentResponse> list(Long userId, Long courseId) {
        findCourse(userId, courseId);
        return assessmentRepository.findAllByCourseIdOrderByDueDateAscIdAsc(courseId).stream()
                .map(AssessmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AssessmentResponse get(Long userId, Long id) {
        return AssessmentResponse.from(find(userId, id));
    }

    @Transactional
    public AssessmentResponse update(Long userId, Long id, AssessmentRequest request) {
        Assessment assessment = findForUpdate(userId, id);
        Long courseId = assessment.getCourse().getId();
        BigDecimal othersWeight = zeroIfNull(assessmentRepository.sumWeightByCourseIdExcluding(courseId, id));
        validate(request, courseId, id, othersWeight);

        boolean dueDateChanged = !Objects.equals(assessment.getDueDate(), request.dueDate());
        apply(assessment, request);
        if (dueDateChanged) {
            reminderRepository.findAllByAssessmentId(id).forEach(reminder -> {
                reminder.setSent(false);
                reminder.setSentAt(null);
            });
        }
        return AssessmentResponse.from(assessment);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        assessmentRepository.delete(findForUpdate(userId, id));
    }

    private Course lockCourse(Long userId, Long courseId) {
        return courseRepository.findByIdAndTermUserIdForUpdate(courseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));
    }

    private Assessment findForUpdate(Long userId, Long id) {
        Assessment assessment = find(userId, id);
        lockCourse(userId, assessment.getCourse().getId());
        // La primera lectura pudo ocurrir antes de esperar el bloqueo. Una segunda
        // consulta devolvería la misma instancia obsoleta del contexto de persistencia.
        try {
            entityManager.refresh(assessment);
        } catch (EntityNotFoundException exception) {
            throw new ResourceNotFoundException("Assessment", id);
        }
        return assessment;
    }

    private Course findCourse(Long userId, Long courseId) {
        return courseRepository.findByIdAndTermUserId(courseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));
    }

    private Assessment find(Long userId, Long id) {
        return assessmentRepository.findByIdAndCourseTermUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment", id));
    }

    private void validate(AssessmentRequest request, Long courseId, Long excludeId, BigDecimal othersWeight) {
        if (othersWeight.add(request.weight()).compareTo(MAX_TOTAL_WEIGHT) > 0) {
            throw new BusinessRuleException("The total weight of the course cannot exceed 100% "
                    + "(the other assessments already add " + othersWeight + "%)");
        }
        if (request.type() == AssessmentType.EXAM) {
            boolean examExists = excludeId == null
                    ? assessmentRepository.existsByCourseIdAndType(courseId, AssessmentType.EXAM)
                    : assessmentRepository.existsByCourseIdAndTypeAndIdNot(courseId, AssessmentType.EXAM, excludeId);
            if (examExists) {
                throw new ConflictException("This course already has an exam");
            }
        }
        if (request.grade() == null && request.status() == AssessmentStatus.GRADED) {
            throw new BusinessRuleException("A GRADED assessment needs a grade");
        }
    }

    private void apply(Assessment assessment, AssessmentRequest request) {
        assessment.setName(request.name().trim());
        assessment.setType(request.type());
        assessment.setWeight(request.weight().setScale(2));
        assessment.setDueDate(request.dueDate());
        assessment.setGrade(request.grade() == null ? null : request.grade().setScale(1, RoundingMode.DOWN));
        assessment.setStatus(resolveStatus(request));
        assessment.setTopics(blankToNull(request.topics()));
        assessment.setComment(blankToNull(request.comment()));
    }

    private AssessmentStatus resolveStatus(AssessmentRequest request) {
        if (request.grade() != null) {
            return AssessmentStatus.GRADED;
        }
        return request.status() == null ? AssessmentStatus.PENDING : request.status();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
