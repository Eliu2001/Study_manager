package cl.germain.studymanager.grading;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class GradeCalculator {

    /** Las notas conservan su primer decimal; el resto se corta, no se redondea. */
    static final RoundingMode GRADE_ROUNDING = RoundingMode.DOWN;

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal MIN_GRADE = new BigDecimal("1.0");
    private static final BigDecimal MAX_GRADE = new BigDecimal("7.0");

    private GradeCalculator() {
    }

    public static CourseGradeResult calculate(BigDecimal passingGrade, List<GradedItem> items) {
        BigDecimal gradedWeight = BigDecimal.ZERO;
        BigDecimal points = BigDecimal.ZERO;
        for (GradedItem item : items) {
            if (item.grade() != null) {
                gradedWeight = gradedWeight.add(item.weight());
                points = points.add(item.grade().multiply(item.weight()).movePointLeft(2));
            }
        }

        BigDecimal currentPoints = points.setScale(2, RoundingMode.DOWN);
        BigDecimal currentAverage = gradedWeight.signum() == 0
                ? null
                : points.divide(gradedWeight.movePointLeft(2), 1, GRADE_ROUNDING);
        BigDecimal remainingWeight = HUNDRED.subtract(gradedWeight);

        if (remainingWeight.signum() <= 0) {
            BigDecimal finalGrade = points.setScale(1, GRADE_ROUNDING);
            CourseStatus status = finalGrade.compareTo(passingGrade) >= 0
                    ? CourseStatus.PASSED
                    : CourseStatus.FAILED;
            return new CourseGradeResult(gradedWeight, currentPoints, currentAverage, finalGrade, null, status);
        }

        BigDecimal required = passingGrade.subtract(points)
                .divide(remainingWeight.movePointLeft(2), 1, RoundingMode.UP);
        CourseStatus status;
        if (required.compareTo(MIN_GRADE) <= 0) {
            status = CourseStatus.SECURED;
            required = MIN_GRADE;
        } else if (required.compareTo(MAX_GRADE) > 0) {
            status = CourseStatus.IMPOSSIBLE;
        } else {
            status = CourseStatus.IN_PROGRESS;
        }
        return new CourseGradeResult(gradedWeight, currentPoints, currentAverage, null, required, status);
    }

    /**
     * Promedio de notas finales. Es ponderado por créditos solo si todas las
     * asignaturas tienen créditos; si falta alguno, es un promedio simple.
     */
    public static BigDecimal average(List<FinalGrade> finals) {
        if (finals.isEmpty()) {
            return null;
        }
        boolean weighted = finals.stream().allMatch(f -> f.credits() != null);
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal divisor = BigDecimal.ZERO;
        for (FinalGrade f : finals) {
            BigDecimal weight = weighted ? BigDecimal.valueOf(f.credits()) : BigDecimal.ONE;
            total = total.add(f.grade().multiply(weight));
            divisor = divisor.add(weight);
        }
        return total.divide(divisor, 1, GRADE_ROUNDING);
    }
}