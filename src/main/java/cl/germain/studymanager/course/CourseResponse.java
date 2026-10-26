package cl.germain.studymanager.course;

import java.math.BigDecimal;

public record CourseResponse(
        Long id,
        Long termId,
        String name,
        String code,
        Short credits,
        BigDecimal passingGrade
) {

    public static CourseResponse from(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getTerm().getId(),
                course.getName(),
                course.getCode(),
                course.getCredits(),
                course.getPassingGrade());
    }
}
