package cl.germain.studymanager.grading;

import java.math.BigDecimal;

public record CourseGradeResult(
        BigDecimal gradedWeight,
        BigDecimal currentPoints,
        BigDecimal currentAverage,
        BigDecimal finalGrade,
        BigDecimal requiredAverage,
        CourseStatus status
) {
}