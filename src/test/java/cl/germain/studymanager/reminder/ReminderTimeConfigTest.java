package cl.germain.studymanager.reminder;

import cl.germain.studymanager.security.LoginAttemptConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class ReminderTimeConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ReminderTimeConfig.class, LoginAttemptConfig.class);

    @Test
    void defaultsToSantiagoWithoutChangingTheGenericLoginClock() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean("reminderClock", Clock.class).getZone())
                    .isEqualTo(ZoneId.of("America/Santiago"));
            assertThat(context.getBean("clock", Clock.class).getZone()).isEqualTo(ZoneOffset.UTC);
        });
    }

    @Test
    void acceptsConfiguredBusinessZone() {
        contextRunner.withPropertyValues("app.time-zone=Pacific/Auckland").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean("reminderClock", Clock.class).getZone())
                    .isEqualTo(ZoneId.of("Pacific/Auckland"));
        });
    }

    @Test
    void rejectsInvalidZoneAtStartup() {
        contextRunner.withPropertyValues("app.time-zone=Invalid/Zone").run(context ->
                assertThat(context).hasFailed());
    }
}
