package cl.germain.studymanager.reminder;

import cl.germain.studymanager.assessment.Assessment;
import cl.germain.studymanager.assessment.AssessmentRepository;
import cl.germain.studymanager.assessment.AssessmentStatus;
import cl.germain.studymanager.common.BusinessRuleException;
import cl.germain.studymanager.common.ConflictException;
import cl.germain.studymanager.common.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final AssessmentRepository assessmentRepository;
    private final Clock reminderClock;

    public ReminderService(ReminderRepository reminderRepository,
                           AssessmentRepository assessmentRepository,
                           @Qualifier("reminderClock") Clock reminderClock) {
        this.reminderRepository = reminderRepository;
        this.assessmentRepository = assessmentRepository;
        this.reminderClock = reminderClock;
    }

    @Transactional
    public ReminderResponse create(Long userId, Long assessmentId, ReminderRequest request) {
        Assessment assessment = findAssessment(userId, assessmentId);
        if (assessment.getDueDate() == null) {
            throw new BusinessRuleException("The assessment needs a due date before adding reminders");
        }
        if (reminderRepository.countWithNotice(assessmentId, request.daysBefore()) > 0) {
            throw new ConflictException(
                    "A reminder with " + request.daysBefore() + " days of notice already exists");
        }

        Reminder reminder = new Reminder();
        reminder.setAssessment(assessment);
        reminder.setDaysBefore(request.daysBefore());
        return ReminderResponse.from(reminderRepository.save(reminder));
    }

    @Transactional(readOnly = true)
    public List<ReminderResponse> list(Long userId, Long assessmentId) {
        findAssessment(userId, assessmentId);
        return reminderRepository.findAllByAssessmentId(assessmentId).stream()
                .sorted(Comparator.comparing(Reminder::getDaysBefore, Comparator.reverseOrder()))
                .map(ReminderResponse::from)
                .toList();
    }

    @Transactional
    public void delete(Long userId, Long id) {
        reminderRepository.delete(find(userId, id));
    }

    /**
     * Marca el aviso como visto para que deje de aparecer en la lista de pendientes.
     */
    @Transactional
    public void dismiss(Long userId, Long id) {
        Reminder reminder = find(userId, id);
        if (!reminder.isSent()) {
            reminder.setSent(true);
            reminder.setSentAt(reminderClock.instant());
        }
    }

    @Transactional(readOnly = true)
    public List<ReminderNotification> upcoming(Long userId) {
        return upcoming(userId, LocalDate.now(reminderClock));
    }

    List<ReminderNotification> upcoming(Long userId, LocalDate today) {
        return reminderRepository.findAllByAssessmentCourseTermUserIdAndSentFalse(userId).stream()
                .filter(r -> r.getAssessment().getDueDate() != null)
                .filter(r -> r.getAssessment().getStatus() != AssessmentStatus.GRADED)
                .filter(r -> ReminderRules.isDue(today, r.getAssessment().getDueDate(), r.getDaysBefore()))
                .sorted(Comparator
                        .comparing((Reminder r) -> r.getAssessment().getDueDate())
                        .thenComparing(Reminder::getId))
                .map(r -> toNotification(r, today))
                .toList();
    }

    private ReminderNotification toNotification(Reminder reminder, LocalDate today) {
        Assessment assessment = reminder.getAssessment();
        return new ReminderNotification(
                reminder.getId(),
                assessment.getId(),
                assessment.getName(),
                assessment.getCourse().getId(),
                assessment.getCourse().getName(),
                assessment.getDueDate(),
                ReminderRules.daysLeft(today, assessment.getDueDate()),
                reminder.getDaysBefore());
    }

    private Assessment findAssessment(Long userId, Long assessmentId) {
        return assessmentRepository.findByIdAndCourseTermUserId(assessmentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment", assessmentId));
    }

    private Reminder find(Long userId, Long id) {
        return reminderRepository.findByIdAndAssessmentCourseTermUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder", id));
    }
}
