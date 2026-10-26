package cl.germain.studymanager.assessment;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AssessmentResponse(
        Long id,
        Long courseId,
        String name,
        AssessmentType type,
        BigDecimal weight,
        LocalDate dueDate,
        AssessmentStatus status,
        BigDecimal grade,
        String topics,
        String comment
) {

    public static AssessmentResponse from(Assessment a) {
        return new AssessmentResponse(
                a.getId(),
                a.getCourse().getId(),
                a.getName(),
                a.getType(),
                a.getWeight(),
                a.getDueDate(),
                a.getStatus(),
                a.getGrade(),
                a.getTopics(),
                a.getComment());
    }
}