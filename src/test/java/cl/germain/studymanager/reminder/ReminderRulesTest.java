package cl.germain.studymanager.reminder;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ReminderRulesTest {

    private static final LocalDate DUE = LocalDate.of(2027, 9, 20);

    @Test
    void notDueBeforeTheNoticeWindowStarts() {
        assertThat(ReminderRules.isDue(LocalDate.of(2027, 9, 16), DUE, 3)).isFalse();
    }

    @Test
    void dueOnTheFirstDayOfTheNoticeWindow() {
        assertThat(ReminderRules.isDue(LocalDate.of(2027, 9, 17), DUE, 3)).isTrue();
    }

    @Test
    void dueOnTheAssessmentDay() {
        assertThat(ReminderRules.isDue(DUE, DUE, 3)).isTrue();
    }

    @Test
    void notDueAfterTheAssessmentDay() {
        assertThat(ReminderRules.isDue(LocalDate.of(2027, 9, 21), DUE, 3)).isFalse();
    }

    @Test
    void zeroDaysOfNoticeIsOnlyDueOnTheAssessmentDay() {
        assertThat(ReminderRules.isDue(LocalDate.of(2027, 9, 19), DUE, 0)).isFalse();
        assertThat(ReminderRules.isDue(DUE, DUE, 0)).isTrue();
    }

    @Test
    void countsDaysLeft() {
        assertThat(ReminderRules.daysLeft(LocalDate.of(2027, 9, 17), DUE)).isEqualTo(3);
        assertThat(ReminderRules.daysLeft(DUE, DUE)).isZero();
    }
}
