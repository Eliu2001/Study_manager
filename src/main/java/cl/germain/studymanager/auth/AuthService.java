package cl.germain.studymanager.auth;

import cl.germain.studymanager.common.BusinessRuleException;
import cl.germain.studymanager.security.JwtService;
import cl.germain.studymanager.security.LoginAttemptService;
import cl.germain.studymanager.user.User;
import cl.germain.studymanager.user.UserRepository;
import cl.germain.studymanager.user.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AuthService {

    /** BCrypt solo admite contrasenas de hasta 72 bytes (no caracteres). */
    private static final int BCRYPT_MAX_BYTES = 72;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttempts;
    private final String dummyHash;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       LoginAttemptService loginAttempts) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginAttempts = loginAttempts;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (exceedsBcryptLimit(request.password())) {
            throw new BusinessRuleException("Password must not exceed 72 bytes");
        }
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException();
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        User saved = userRepository.save(user);
        return new UserResponse(saved.getId(), saved.getName(), saved.getEmail());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request, String clientIp) {
        String email = normalize(request.email());

        // Se comprueba el bloqueo antes de gastar tiempo de CPU en BCrypt.
        long secondsToWait = loginAttempts.secondsUntilAllowed(clientIp, email);
        if (secondsToWait > 0) {
            throw new TooManyLoginAttemptsException(secondsToWait);
        }

        boolean valid = false;
        User user = null;
        if (!exceedsBcryptLimit(request.password())) {
            user = userRepository.findByEmail(email).orElse(null);
            String hash = user != null ? user.getPasswordHash() : dummyHash;
            // Aunque el correo no exista se compara contra un hash falso, para que el tiempo
            // de respuesta no delate si la cuenta existe.
            valid = passwordEncoder.matches(request.password(), hash) && user != null;
        }

        if (!valid) {
            loginAttempts.recordFailure(clientIp, email);
            throw new InvalidCredentialsException();
        }
        loginAttempts.recordSuccess(clientIp, email);
        return new LoginResponse(jwtService.generateToken(user), "Bearer", jwtService.expiresInSeconds());
    }

    private boolean exceedsBcryptLimit(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES;
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
