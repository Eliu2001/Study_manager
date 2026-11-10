package cl.germain.studymanager.reminder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ReminderTimeConfig {

    // Solo se inyecta mediante su qualifier; el Clock genérico del login sigue en UTC.
    @Bean(defaultCandidate = false)
    Clock reminderClock(@Value("${app.time-zone:America/Santiago}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
