package cl.germain.studymanager.grading;

import java.math.BigDecimal;
import java.util.List;

public record OverallSummaryResponse(BigDecimal cumulativeAverage, List<TermAverage> terms) {
}