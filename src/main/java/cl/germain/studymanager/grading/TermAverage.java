package cl.germain.studymanager.grading;

import java.math.BigDecimal;

public record TermAverage(Long termId, String termName, BigDecimal average) {
}
