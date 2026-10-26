package cl.germain.studymanager.course;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CourseRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 20) String code,
        @Positive Short credits,
        @DecimalMin("1.0") @DecimalMax("7.0") @Digits(integer = 1, fraction = 1) BigDecimal passingGrade
) {
}
