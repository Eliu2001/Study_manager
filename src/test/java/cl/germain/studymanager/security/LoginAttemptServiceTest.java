package cl.germain.studymanager.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    /** Reloj que se mueve a mano, para probar el paso del tiempo sin esperar. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2027-03-01T12:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final MutableClock clock = new MutableClock();
    // 3 fallos por cuenta, 5 por IP, ventana de 15 minutos, bloqueo de 10 minutos.
    private final LoginAttemptService service =
            new LoginAttemptService(clock, 3, 5, Duration.ofMinutes(15), Duration.ofMinutes(10));

    private void fail(String ip, String email, int times) {
        for (int i = 0; i < times; i++) {
            service.recordFailure(ip, email);
        }
    }

    @Test
    void allowedBeforeReachingTheLimit() {
        fail("1.1.1.1", "ana@example.com", 2);

        assertThat(service.secondsUntilAllowed("1.1.1.1", "ana@example.com")).isZero();
    }

    @Test
    void blockedAfterReachingTheLimit() {
        fail("1.1.1.1", "ana@example.com", 3);

        assertThat(service.secondsUntilAllowed("1.1.1.1", "ana@example.com")).isEqualTo(600);
    }

    @Test
    void waitTimeDecreasesAsTimePasses() {
        fail("1.1.1.1", "ana@example.com", 3);
        clock.advance(Duration.ofMinutes(4));

        assertThat(service.secondsUntilAllowed("1.1.1.1", "ana@example.com")).isEqualTo(360);
    }

    @Test
    void unlockedAfterTheLockDurationAndCountingStartsOver() {
        fail("1.1.1.1", "ana@example.com", 3);
        clock.advance(Duration.ofMinutes(10));

        assertThat(service.secondsUntilAllowed("1.1.1.1", "ana@example.com")).isZero();

        // Si el conteo siguiera donde quedo, este fallo (el cuarto) volveria a bloquear la cuenta.
        fail("1.1.1.1", "ana@example.com", 1);
        assertThat(service.secondsUntilAllowed("1.1.1.1", "ana@example.com")).isZero();
    }

    @Test
    void successResetsTheAccountCounter() {
        fail("1.1.1.1", "ana@example.com", 2);
        service.recordSuccess("1.1.1.1", "ana@example.com");
        fail("1.1.1.1", "ana@example.com", 2);

        assertThat(service.secondsUntilAllowed("1.1.1.1", "ana@example.com")).isZero();
    }

    @Test
    void failuresOutsideTheWindowDoNotAccumulate() {
        fail("1.1.1.1", "ana@example.com", 2);
        clock.advance(Duration.ofMinutes(16));
        fail("1.1.1.1", "ana@example.com", 2);

        assertThat(service.secondsUntilAllowed("1.1.1.1", "ana@example.com")).isZero();
    }

    @Test
    void anotherAccountFromTheSameIpIsNotBlockedByAnAccountLimit() {
        fail("1.1.1.1", "ana@example.com", 3);

        assertThat(service.secondsUntilAllowed("1.1.1.1", "luis@example.com")).isZero();
    }

    @Test
    void sameAccountFromAnotherIpIsNotBlocked() {
        fail("1.1.1.1", "ana@example.com", 3);

        assertThat(service.secondsUntilAllowed("2.2.2.2", "ana@example.com")).isZero();
    }

    @Test
    void ipLimitBlocksEveryAccountFromThatIp() {
        fail("1.1.1.1", "a@example.com", 2);
        fail("1.1.1.1", "b@example.com", 2);
        fail("1.1.1.1", "c@example.com", 1);

        assertThat(service.secondsUntilAllowed("1.1.1.1", "nueva@example.com")).isEqualTo(600);
        assertThat(service.secondsUntilAllowed("2.2.2.2", "nueva@example.com")).isZero();
    }

    @Test
    void emailComparisonIgnoresCaseAndSurroundingSpaces() {
        service.recordFailure("1.1.1.1", "Ana@Example.com");
        service.recordFailure("1.1.1.1", " ana@example.com ");
        service.recordFailure("1.1.1.1", "ANA@EXAMPLE.COM");

        assertThat(service.secondsUntilAllowed("1.1.1.1", "ana@example.com")).isEqualTo(600);
    }
}
