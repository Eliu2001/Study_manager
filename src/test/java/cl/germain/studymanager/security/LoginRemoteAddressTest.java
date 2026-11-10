package cl.germain.studymanager.security;

import cl.germain.studymanager.auth.AuthController;
import cl.germain.studymanager.auth.AuthService;
import cl.germain.studymanager.auth.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LoginRemoteAddressTest {

    @Test
    void loginUsesContainerAddressNotClientSuppliedForwardingHeaders() {
        var service = mock(AuthService.class);
        var controller = new AuthController(service);
        var login = new LoginRequest("ana@example.com", "incorrect-password");
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.9");
        request.addHeader("X-Forwarded-For", "198.51.100.77");
        request.addHeader("Forwarded", "for=198.51.100.77");
        request.addHeader("X-Real-IP", "198.51.100.77");

        controller.login(login, request);

        verify(service).login(login, "203.0.113.9");
    }
}
