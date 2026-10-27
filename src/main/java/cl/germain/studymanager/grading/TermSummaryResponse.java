package cl.germain.studymanager.grading;

import java.math.BigDecimal;
import java.util.List;

public record TermSummaryResponse(
        Long termId,
        String termName,
        BigDecimal average,
        List<CourseSummaryResponse> courses
) {
}
