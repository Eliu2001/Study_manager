package cl.germain.studymanager.assessment;

import cl.germain.studymanager.common.BusinessRuleException;
import cl.germain.studymanager.common.ResourceNotFoundException;
import cl.germain.studymanager.course.Course;
import cl.germain.studymanager.course.CourseRepository;
import cl.germain.studymanager.reminder.ReminderRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceLockingTest {

    @Mock private AssessmentRepository assessments;
    @Mock private CourseRepository courses;
    @Mock private EntityManager entityManager;
    @Mock private ReminderRepository reminders;
    @InjectMocks private AssessmentService service;

    private Course course;
    private Assessment assessment;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setId(10L);
        assessment = new Assessment();
        assessment.setId(20L);
        assessment.setCourse(course);
    }

    private AssessmentRequest request() {
        return new AssessmentRequest("Prueba", AssessmentType.SUMMATIVE,
                new BigDecimal("60"), null, null, null, null, null);
    }

    @Test
    void createLocksOwnedCourseBeforeCheckingTheTotal() {
        when(courses.findByIdAndTermUserIdForUpdate(10L, 1L)).thenReturn(Optional.of(course));
        when(assessments.sumWeightByCourseId(10L)).thenReturn(new BigDecimal("50"));

        assertThatThrownBy(() -> service.create(1L, 10L, request()))
                .isInstanceOf(BusinessRuleException.class);

        var order = inOrder(courses, assessments);
        order.verify(courses).findByIdAndTermUserIdForUpdate(10L, 1L);
        order.verify(assessments).sumWeightByCourseId(10L);
        verify(assessments, never()).save(any());
    }

    @Test
    void updateRefreshesOnlyAfterLockingAndBeforeValidation() {
        when(assessments.findByIdAndCourseTermUserId(20L, 1L)).thenReturn(Optional.of(assessment));
        when(courses.findByIdAndTermUserIdForUpdate(10L, 1L)).thenReturn(Optional.of(course));
        when(assessments.sumWeightByCourseIdExcluding(10L, 20L)).thenReturn(new BigDecimal("50"));

        assertThatThrownBy(() -> service.update(1L, 20L, request()))
                .isInstanceOf(BusinessRuleException.class);

        var order = inOrder(courses, entityManager, assessments);
        order.verify(assessments).findByIdAndCourseTermUserId(20L, 1L);
        order.verify(courses).findByIdAndTermUserIdForUpdate(10L, 1L);
        order.verify(entityManager).refresh(assessment);
        order.verify(assessments).sumWeightByCourseIdExcluding(10L, 20L);
    }

    @Test
    void deleteUsesTheSameCourseLockAndRefresh() {
        when(assessments.findByIdAndCourseTermUserId(20L, 1L)).thenReturn(Optional.of(assessment));
        when(courses.findByIdAndTermUserIdForUpdate(10L, 1L)).thenReturn(Optional.of(course));

        service.delete(1L, 20L);

        var order = inOrder(courses, entityManager, assessments);
        order.verify(assessments).findByIdAndCourseTermUserId(20L, 1L);
        order.verify(courses).findByIdAndTermUserIdForUpdate(10L, 1L);
        order.verify(entityManager).refresh(assessment);
        order.verify(assessments).delete(assessment);
    }

    @Test
    void deletionWhileWaitingIsReportedAsNotFound() {
        when(assessments.findByIdAndCourseTermUserId(20L, 1L)).thenReturn(Optional.of(assessment));
        when(courses.findByIdAndTermUserIdForUpdate(10L, 1L)).thenReturn(Optional.of(course));
        doThrow(new EntityNotFoundException()).when(entityManager).refresh(assessment);

        assertThatThrownBy(() -> service.update(1L, 20L, request()))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(assessments, never()).sumWeightByCourseIdExcluding(any(), any());
    }

    @Test
    void foreignCourseCannotBeUsedToCreateAnAssessment() {
        when(courses.findByIdAndTermUserIdForUpdate(10L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(2L, 10L, request()))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(assessments, entityManager);
    }

    @Test
    void foreignAssessmentCannotBeUpdatedOrDeleted() {
        when(assessments.findByIdAndCourseTermUserId(20L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(2L, 20L, request()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.delete(2L, 20L))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(courses, entityManager);
    }
}
