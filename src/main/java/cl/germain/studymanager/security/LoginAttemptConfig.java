package cl.germain.studymanager.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class LoginAttemptConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    LoginAttemptService loginAttemptService(
            Clock clock,
            @Value("${app.login.max-failures:5}") int maxFailures,
            @Value("${app.login.max-failures-per-ip:20}") int maxFailuresPerIp,
            @Value("${app.login.window-minutes:15}") long windowMinutes,
            @Value("${app.login.lock-minutes:15}") long lockMinutes) {
        return new LoginAttemptService(
                clock,
                maxFailures,
                maxFailuresPerIp,
                Duration.ofMinutes(windowMinutes),
                Duration.ofMinutes(lockMinutes));
    }
}
