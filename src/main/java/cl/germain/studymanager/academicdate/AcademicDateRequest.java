package cl.germain.studymanager.academicdate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AcademicDateRequest(
        @NotBlank @Size(max = 150) String title,
        @NotNull LocalDate eventDate,
        @NotNull AcademicDateType type,
        Long courseId
) {
}
