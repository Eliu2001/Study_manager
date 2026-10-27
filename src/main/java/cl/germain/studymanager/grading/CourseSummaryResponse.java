package cl.germain.studymanager.grading;

import java.math.BigDecimal;

public record CourseSummaryResponse(
        Long courseId,
        String courseName,
        Short credits,
        BigDecimal passingGrade,
        BigDecimal gradedWeight,
        BigDecimal currentPoints,
        BigDecimal currentAverage,
        BigDecimal finalGrade,
        BigDecimal requiredAverage,
        CourseStatus status
) {
}