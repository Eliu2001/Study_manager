package cl.germain.studymanager.reminder;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReminderRequest(
        @NotNull @Min(0) @Max(365) Short daysBefore
) {
}
