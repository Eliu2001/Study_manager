package cl.germain.studymanager.course;

import cl.germain.studymanager.common.ConflictException;
import cl.germain.studymanager.common.ResourceNotFoundException;
import cl.germain.studymanager.term.Term;
import cl.germain.studymanager.term.TermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private static final BigDecimal DEFAULT_PASSING_GRADE = new BigDecimal("4.0");

    private final CourseRepository courseRepository;
    private final TermRepository termRepository;

    @Transactional
    public CourseResponse create(Long userId, Long termId, CourseRequest request) {
        Term term = findTerm(userId, termId);
        String name = request.name().trim();
        if (courseRepository.existsByTermIdAndName(termId, name)) {
            throw new ConflictException("A course named '" + name + "' already exists in this term");
        }

        Course course = new Course();
        course.setTerm(term);
        apply(course, name, request);
        return CourseResponse.from(courseRepository.save(course));
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> list(Long userId, Long termId) {
        findTerm(userId, termId);
        return courseRepository.findAllByTermIdOrderByNameAsc(termId).stream()
                .map(CourseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseResponse get(Long userId, Long id) {
        return CourseResponse.from(find(userId, id));
    }

    @Transactional
    public CourseResponse update(Long userId, Long id, CourseRequest request) {
        Course course = find(userId, id);
        String name = request.name().trim();
        if (courseRepository.existsByTermIdAndNameAndIdNot(course.getTerm().getId(), name, id)) {
            throw new ConflictException("A course named '" + name + "' already exists in this term");
        }

        apply(course, name, request);
        return CourseResponse.from(course);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        courseRepository.delete(find(userId, id));
    }

    private Term findTerm(Long userId, Long termId) {
        return termRepository.findByIdAndUserId(termId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Term", termId));
    }

    private Course find(Long userId, Long id) {
        return courseRepository.findByIdAndTermUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", id));
    }

    private void apply(Course course, String name, CourseRequest request) {
        course.setName(name);
        course.setCode(request.code() == null || request.code().isBlank() ? null : request.code().trim());
        course.setCredits(request.credits());
        course.setPassingGrade(request.passingGrade() == null
                ? DEFAULT_PASSING_GRADE
                : request.passingGrade().setScale(1));
    }
}