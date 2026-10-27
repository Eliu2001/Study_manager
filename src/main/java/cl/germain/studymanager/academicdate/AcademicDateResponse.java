package cl.germain.studymanager.academicdate;

import java.time.LocalDate;

public record AcademicDateResponse(
        Long id,
        Long courseId,
        String title,
        LocalDate eventDate,
        AcademicDateType type
) {

    public static AcademicDateResponse from(AcademicDate date) {
        return new AcademicDateResponse(
                date.getId(),
                date.getCourse() == null ? null : date.getCourse().getId(),
                date.getTitle(),
                date.getEventDate(),
                date.getType());
    }
}
