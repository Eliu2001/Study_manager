package cl.germain.studymanager.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita los intentos fallidos de login para frenar la fuerza bruta.
 *
 * <p>Cuenta los fallos de dos formas: por cuenta desde una IP (correo + IP) y por IP en total.
 * Al llegar al limite, bloquea esa clave durante un tiempo. El estado vive en memoria, asi que se
 * reinicia con la aplicacion, y no depende de ninguna libreria externa.
 */
public class LoginAttemptService {

    private static final int CLEANUP_THRESHOLD = 10_000;

    private record State(int failures, Instant windowStart, Instant lockedUntil) {
    }

    private final Clock clock;
    private final int maxFailuresPerAccount;
    private final int maxFailuresPerIp;
    private final Duration window;
    private final Duration lockDuration;
    private final Map<String, State> states = new ConcurrentHashMap<>();

    public LoginAttemptService(Clock clock,
                               int maxFailuresPerAccount,
                               int maxFailuresPerIp,
                               Duration window,
                               Duration lockDuration) {
        this.clock = clock;
        this.maxFailuresPerAccount = maxFailuresPerAccount;
        this.maxFailuresPerIp = maxFailuresPerIp;
        this.window = window;
        this.lockDuration = lockDuration;
    }

    /** Segundos que faltan para poder volver a intentar; 0 si el login esta permitido. */
    public long secondsUntilAllowed(String ip, String email) {
        Instant now = clock.instant();
        return Math.max(
                secondsLeft(accountKey(ip, email), now),
                secondsLeft(ipKey(ip), now));
    }

    public void recordFailure(String ip, String email) {
        Instant now = clock.instant();
        register(accountKey(ip, email), maxFailuresPerAccount, now);
        register(ipKey(ip), maxFailuresPerIp, now);
        if (states.size() > CLEANUP_THRESHOLD) {
            states.entrySet().removeIf(entry -> isExpired(entry.getValue(), now));
        }
    }

    /** Un login correcto reinicia el contador de esa cuenta; el de la IP sigue su curso. */
    public void recordSuccess(String ip, String email) {
        states.remove(accountKey(ip, email));
    }

    private void register(String key, int maxFailures, Instant now) {
        states.compute(key, (k, current) -> {
            State state = (current == null || isExpired(current, now))
                    ? new State(0, now, null)
                    : current;
            int failures = state.failures() + 1;
            Instant lockedUntil = failures >= maxFailures ? now.plus(lockDuration) : state.lockedUntil();
            return new State(failures, state.windowStart(), lockedUntil);
        });
    }

    private long secondsLeft(String key, Instant now) {
        State state = states.get(key);
        if (state == null || state.lockedUntil() == null || !now.isBefore(state.lockedUntil())) {
            return 0;
        }
        Duration left = Duration.between(now, state.lockedUntil());
        return left.toSeconds() + (left.toNanosPart() > 0 ? 1 : 0);
    }

    private boolean isExpired(State state, Instant now) {
        if (state.lockedUntil() != null) {
            return !now.isBefore(state.lockedUntil());
        }
        return !now.isBefore(state.windowStart().plus(window));
    }

    private String accountKey(String ip, String email) {
        return "account|" + ip + "|" + email.trim().toLowerCase(Locale.ROOT);
    }

    private String ipKey(String ip) {
        return "ip|" + ip;
    }
}
