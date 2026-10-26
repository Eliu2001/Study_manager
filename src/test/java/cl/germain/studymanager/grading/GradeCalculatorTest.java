package cl.germain.studymanager.grading;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GradeCalculatorTest {

    private static final BigDecimal PASSING = new BigDecimal("4.0");

    private static GradedItem item(String weight, String grade) {
        return new GradedItem(new BigDecimal(weight), grade == null ? null : new BigDecimal(grade));
    }

    private static FinalGrade finalGrade(String grade, Integer credits) {
        return new FinalGrade(new BigDecimal(grade), credits == null ? null : credits.shortValue());
    }

    @Test
    void requiredExamGradeWhenSummativesAverageFive() {
        var result = GradeCalculator.calculate(PASSING,
                List.of(item("35", "5.0"), item("35", "5.0"), item("30", null)));

        assertThat(result.currentPoints()).isEqualByComparingTo("3.50");
        assertThat(result.currentAverage()).isEqualByComparingTo("5.0");
        assertThat(result.requiredAverage()).isEqualByComparingTo("1.7");
        assertThat(result.status()).isEqualTo(CourseStatus.IN_PROGRESS);
        assertThat(result.finalGrade()).isNull();
    }

    @Test
    void requiredExamGradeWhenSummativesAverageThree() {
        var result = GradeCalculator.calculate(PASSING,
                List.of(item("35", "3.0"), item("35", "3.0"), item("30", null)));

        assertThat(result.requiredAverage()).isEqualByComparingTo("6.4");
        assertThat(result.status()).isEqualTo(CourseStatus.IN_PROGRESS);
    }

    @Test
    void impossibleWhenRequiredGradeExceedsSeven() {
        var result = GradeCalculator.calculate(PASSING,
                List.of(item("35", "2.0"), item("35", "2.0"), item("30", null)));

        assertThat(result.requiredAverage()).isEqualByComparingTo("8.7");
        assertThat(result.status()).isEqualTo(CourseStatus.IMPOSSIBLE);
    }

    @Test
    void securedWhenAlreadyPassedEvenWithMinimumGrade() {
        var result = GradeCalculator.calculate(PASSING,
                List.of(item("35", "7.0"), item("35", "7.0"), item("30", null)));

        assertThat(result.requiredAverage()).isEqualByComparingTo("1.0");
        assertThat(result.status()).isEqualTo(CourseStatus.SECURED);
    }

    @Test
    void finalGradeWhenEverythingIsGraded() {
        var result = GradeCalculator.calculate(PASSING,
                List.of(item("35", "5.0"), item("35", "5.0"), item("30", "4.0")));

        assertThat(result.finalGrade()).isEqualByComparingTo("4.7");
        assertThat(result.requiredAverage()).isNull();
        assertThat(result.status()).isEqualTo(CourseStatus.PASSED);
    }

    @Test
    void secondDecimalIsCutNotRounded() {
        var result = GradeCalculator.calculate(PASSING,
                List.of(item("50", "4.0"), item("50", "3.9")));

        assertThat(result.finalGrade()).isEqualByComparingTo("3.9");
        assertThat(result.status()).isEqualTo(CourseStatus.FAILED);
    }

    @Test
    void averageIsSimpleWhenSomeCourseHasNoCredits() {
        var result = GradeCalculator.average(List.of(finalGrade("5.0", 6), finalGrade("6.0", null)));

        assertThat(result).isEqualByComparingTo("5.5");
    }

    @Test
    void averageIsWeightedWhenAllCoursesHaveCredits() {
        var result = GradeCalculator.average(List.of(finalGrade("5.0", 6), finalGrade("6.0", 3)));

        assertThat(result).isEqualByComparingTo("5.3");
    }

    @Test
    void averageOfNothingIsNull() {
        assertThat(GradeCalculator.average(List.of())).isNull();
    }
}
