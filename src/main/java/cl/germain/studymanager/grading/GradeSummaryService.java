package cl.germain.studymanager.grading;

import cl.germain.studymanager.assessment.Assessment;
import cl.germain.studymanager.assessment.AssessmentRepository;
import cl.germain.studymanager.common.ResourceNotFoundException;
import cl.germain.studymanager.course.Course;
import cl.germain.studymanager.course.CourseRepository;
import cl.germain.studymanager.term.Term;
import cl.germain.studymanager.term.TermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GradeSummaryService {

    private final CourseRepository courseRepository;
    private final TermRepository termRepository;
    private final AssessmentRepository assessmentRepository;

    @Transactional(readOnly = true)
    public CourseSummaryResponse courseSummary(Long userId, Long courseId) {
        Course course = courseRepository.findByIdAndTermUserId(courseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));
        return summarize(course, assessmentRepository.findAllByCourseIdOrderByDueDateAscIdAsc(courseId));
    }

    @Transactional(readOnly = true)
    public TermSummaryResponse termSummary(Long userId, Long termId) {
        Term term = termRepository.findByIdAndUserId(termId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Term", termId));
        Map<Long, List<Assessment>> byCourse =
                groupByCourse(assessmentRepository.findAllByCourseTermId(termId));

        List<CourseSummaryResponse> summaries = courseRepository.findAllByTermIdOrderByNameAsc(termId).stream()
                .map(course -> summarize(course, byCourse.getOrDefault(course.getId(), List.of())))
                .toList();
        return new TermSummaryResponse(term.getId(), term.getName(), averageOf(summaries), summaries);
    }

    @Transactional(readOnly = true)
    public OverallSummaryResponse overallSummary(Long userId) {
        Map<Long, List<Assessment>> byCourse =
                groupByCourse(assessmentRepository.findAllByCourseTermUserId(userId));

        List<CourseSummaryResponse> all = new ArrayList<>();
        Map<Long, List<CourseSummaryResponse>> byTerm = new HashMap<>();
        for (Course course : courseRepository.findAllByTermUserIdOrderByNameAsc(userId)) {
            CourseSummaryResponse summary = summarize(course, byCourse.getOrDefault(course.getId(), List.of()));
            all.add(summary);
            byTerm.computeIfAbsent(course.getTerm().getId(), id -> new ArrayList<>()).add(summary);
        }

        List<TermAverage> termAverages = termRepository.findAllByUserIdOrderByStartDateDesc(userId).stream()
                .map(term -> new TermAverage(term.getId(), term.getName(),
                        averageOf(byTerm.getOrDefault(term.getId(), List.of()))))
                .toList();
        return new OverallSummaryResponse(averageOf(all), termAverages);
    }

    private Map<Long, List<Assessment>> groupByCourse(List<Assessment> assessments) {
        return assessments.stream().collect(Collectors.groupingBy(a -> a.getCourse().getId()));
    }

    private CourseSummaryResponse summarize(Course course, List<Assessment> assessments) {
        List<GradedItem> items = assessments.stream()
                .map(a -> new GradedItem(a.getWeight(), a.getGrade()))
                .toList();
        CourseGradeResult result = GradeCalculator.calculate(course.getPassingGrade(), items);
        return new CourseSummaryResponse(
                course.getId(),
                course.getName(),
                course.getCredits(),
                course.getPassingGrade(),
                result.gradedWeight(),
                result.currentPoints(),
                result.currentAverage(),
                result.finalGrade(),
                result.requiredAverage(),
                result.status());
    }

    private BigDecimal averageOf(List<CourseSummaryResponse> summaries) {
        List<FinalGrade> finals = summaries.stream()
                .filter(s -> s.finalGrade() != null)
                .map(s -> new FinalGrade(s.finalGrade(), s.credits()))
                .toList();
        return GradeCalculator.average(finals);
    }
}
