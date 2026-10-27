package cl.germain.studymanager.assessment;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AssessmentRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull AssessmentType type,
        @NotNull @DecimalMin("0.01") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2) BigDecimal weight,
        LocalDate dueDate,
        AssessmentStatus status,
        @DecimalMin("1.0") @DecimalMax("7.0") @Digits(integer = 1, fraction = 2) BigDecimal grade,
        @Size(max = 5000) String topics,
        @Size(max = 5000) String comment
) {
}
