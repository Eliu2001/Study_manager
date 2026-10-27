package cl.germain.studymanager.grading;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GradeCalculatorEdgeCasesTest {

    private static final BigDecimal PASSING = new BigDecimal("4.0");

    private static GradedItem item(String weight, String grade) {
        return new GradedItem(new BigDecimal(weight), grade == null ? null : new BigDecimal(grade));
    }

    @Test
    void exactlyFourPasses() {
        var result = GradeCalculator.calculate(PASSING, List.of(item("100", "4.0")));

        assertThat(result.finalGrade()).isEqualByComparingTo("4.0");
        assertThat(result.status()).isEqualTo(CourseStatus.PASSED);
    }

    @Test
    void threePointNineSevenDoesNotPass() {
        var result = GradeCalculator.calculate(PASSING,
                List.of(item("35", "4.0"), item("35", "4.0"), item("30", "3.9")));

        assertThat(result.finalGrade()).isEqualByComparingTo("3.9");
        assertThat(result.status()).isEqualTo(CourseStatus.FAILED);
    }

    @Test
    void nothingGradedYetNeedsThePassingGrade() {
        var result = GradeCalculator.calculate(PASSING,
                List.of(item("35", null), item("35", null), item("30", null)));

        assertThat(result.currentAverage()).isNull();
        assertThat(result.requiredAverage()).isEqualByComparingTo("4.0");
        assertThat(result.status()).isEqualTo(CourseStatus.IN_PROGRESS);
    }

    @Test
    void courseWithoutAssessmentsNeedsThePassingGrade() {
        var result = GradeCalculator.calculate(PASSING, List.of());

        assertThat(result.requiredAverage()).isEqualByComparingTo("4.0");
        assertThat(result.status()).isEqualTo(CourseStatus.IN_PROGRESS);
    }

    @Test
    void pendingWeightKeepsTheCourseInProgressEvenIfNoExamWasRegistered() {
        var result = GradeCalculator.calculate(PASSING, List.of(item("35", "5.0"), item("35", "5.0")));

        assertThat(result.status()).isEqualTo(CourseStatus.IN_PROGRESS);
        assertThat(result.requiredAverage()).isEqualByComparingTo("1.7");
    }

    @Test
    void requiredGradeJustAboveSevenIsImpossible() {
        var result = GradeCalculator.calculate(PASSING, List.of(item("70", "2.7")));

        assertThat(result.status()).isEqualTo(CourseStatus.IMPOSSIBLE);
    }

    @Test
    void requiredGradeJustBelowSevenIsStillPossible() {
        var result = GradeCalculator.calculate(PASSING, List.of(item("70", "2.8")));

        assertThat(result.status()).isEqualTo(CourseStatus.IN_PROGRESS);
        assertThat(result.requiredAverage()).isEqualByComparingTo("6.8");
    }

    @Test
    void passingGradeOtherThanFourIsRespected() {
        var result = GradeCalculator.calculate(new BigDecimal("5.0"), List.of(item("100", "4.9")));

        assertThat(result.status()).isEqualTo(CourseStatus.FAILED);
    }

    @Test
    void gettingExactlyTheRequiredGradePassesAndOneTenthLessFails() {
        BigDecimal needed = GradeCalculator.calculate(PASSING,
                List.of(item("35", "5.2"), item("35", "3.7"), item("30", null))).requiredAverage();

        var exact = GradeCalculator.calculate(PASSING,
                List.of(item("35", "5.2"), item("35", "3.7"), item("30", needed.toPlainString())));
        var oneTenthLess = GradeCalculator.calculate(PASSING,
                List.of(item("35", "5.2"), item("35", "3.7"),
                        item("30", needed.subtract(new BigDecimal("0.1")).toPlainString())));

        assertThat(exact.status()).isEqualTo(CourseStatus.PASSED);
        assertThat(oneTenthLess.status()).isEqualTo(CourseStatus.FAILED);
    }
}
