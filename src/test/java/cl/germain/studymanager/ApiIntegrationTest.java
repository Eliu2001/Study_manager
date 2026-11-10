package cl.germain.studymanager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de punta a punta: levantan la aplicacion completa en un puerto libre y la usan como lo
 * haria el frontend, con peticiones HTTP reales. Necesitan PostgreSQL levantado (docker compose up -d)
 * y la base "<DB_NAME>_test"; si no esta disponible, se saltan.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EnabledIf("cl.germain.studymanager.TestDatabase#isAvailable")
class ApiIntegrationTest {

    private static final String PASSWORD = "Clave12345";

    @Value("${local.server.port}")
    private int port;

    private final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

    // ------------------------------------------------------------------ utilidades

    private HttpResponse<String> send(String method, String path, String token, String json)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (json != null) {
            builder.header("Content-Type", "application/json");
            builder.method(method, HttpRequest.BodyPublishers.ofString(json));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String field(String body, String name) {
        Matcher matcher = Pattern.compile("\"" + name + "\":\"?([^\",}]*)\"?").matcher(body);
        assertThat(matcher.find()).as("campo '%s' en %s", name, body).isTrue();
        return matcher.group(1);
    }

    private static String uniqueEmail() {
        return "it-" + UUID.randomUUID() + "@example.com";
    }

    private void register(String email) throws IOException, InterruptedException {
        HttpResponse<String> response = send("POST", "/api/auth/register", null,
                "{\"name\":\"Usuario IT\",\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}");
        assertThat(response.statusCode()).isEqualTo(201);
    }

    private String login(String email) throws IOException, InterruptedException {
        HttpResponse<String> response = send("POST", "/api/auth/login", null,
                "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}");
        assertThat(response.statusCode()).isEqualTo(200);
        return field(response.body(), "accessToken");
    }

    private String newUserToken() throws IOException, InterruptedException {
        String email = uniqueEmail();
        register(email);
        return login(email);
    }

    private String create(String path, String token, String json) throws IOException, InterruptedException {
        HttpResponse<String> response = send("POST", path, token, json);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
        return field(response.body(), "id");
    }

    /** Crea periodo y asignatura; devuelve el id de la asignatura. */
    private String newCourse(String token) throws IOException, InterruptedException {
        String termId = create("/api/terms", token,
                "{\"name\":\"IT-" + UUID.randomUUID() + "\",\"startDate\":\"2027-03-01\",\"endDate\":\"2027-07-15\"}");
        return create("/api/terms/" + termId + "/courses", token, "{\"name\":\"Bases de Datos\"}");
    }

    // ------------------------------------------------------------------ pruebas

    @Test
    void registerLoginAndReadTheProfile() throws Exception {
        String email = uniqueEmail();
        register(email);
        String token = login(email);

        HttpResponse<String> me = send("GET", "/api/users/me", token, null);

        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(me.body()).contains(email);
    }

    @Test
    void duplicatedEmailIsRejected() throws Exception {
        String email = uniqueEmail();
        register(email);

        HttpResponse<String> again = send("POST", "/api/auth/register", null,
                "{\"name\":\"Otra\",\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}");

        assertThat(again.statusCode()).isEqualTo(409);
    }

    @Test
    void protectedRoutesNeedAValidToken() throws Exception {
        assertThat(send("GET", "/api/terms", null, null).statusCode()).isEqualTo(401);
        assertThat(send("GET", "/api/terms", "token-falso", null).statusCode()).isEqualTo(401);
    }

    @Test
    void usersCannotSeeEachOthersData() throws Exception {
        String tokenA = newUserToken();
        String tokenB = newUserToken();
        String termId = create("/api/terms", tokenA,
                "{\"name\":\"IT-" + UUID.randomUUID() + "\",\"startDate\":\"2027-03-01\",\"endDate\":\"2027-07-15\"}");

        assertThat(send("GET", "/api/terms/" + termId, tokenA, null).statusCode()).isEqualTo(200);
        assertThat(send("GET", "/api/terms/" + termId, tokenB, null).statusCode()).isEqualTo(404);
        assertThat(send("GET", "/api/terms/" + termId + "/summary", tokenB, null).statusCode()).isEqualTo(404);
        assertThat(send("DELETE", "/api/terms/" + termId, tokenB, null).statusCode()).isEqualTo(404);
    }

    @Test
    void assessmentWeightsCannotExceedOneHundredPercent() throws Exception {
        String token = newUserToken();
        String courseId = newCourse(token);
        String path = "/api/courses/" + courseId + "/assessments";

        create(path, token, "{\"name\":\"Sumativa 1\",\"type\":\"SUMMATIVE\",\"weight\":35}");
        create(path, token, "{\"name\":\"Sumativa 2\",\"type\":\"SUMMATIVE\",\"weight\":35}");
        create(path, token, "{\"name\":\"Examen\",\"type\":\"EXAM\",\"weight\":30}");

        HttpResponse<String> extra = send("POST", path, token,
                "{\"name\":\"Extra\",\"type\":\"SUMMATIVE\",\"weight\":10}");

        assertThat(extra.statusCode()).isEqualTo(400);
    }

    @Test
    void gradesAreTruncatedAndSummariesAreComputed() throws Exception {
        String token = newUserToken();
        String courseId = newCourse(token);
        String path = "/api/courses/" + courseId + "/assessments";
        String s1 = create(path, token, "{\"name\":\"Sumativa 1\",\"type\":\"SUMMATIVE\",\"weight\":35}");
        String s2 = create(path, token, "{\"name\":\"Sumativa 2\",\"type\":\"SUMMATIVE\",\"weight\":35}");
        String exam = create(path, token, "{\"name\":\"Examen\",\"type\":\"EXAM\",\"weight\":30}");

        HttpResponse<String> graded = send("PUT", "/api/assessments/" + s1, token,
                "{\"name\":\"Sumativa 1\",\"type\":\"SUMMATIVE\",\"weight\":35,\"grade\":5.57}");
        assertThat(graded.statusCode()).isEqualTo(200);
        assertThat(graded.body()).contains("\"grade\":5.5").contains("\"status\":\"GRADED\"");

        HttpResponse<String> partial = send("GET", "/api/courses/" + courseId + "/summary", token, null);
        assertThat(partial.body()).contains("\"status\":\"IN_PROGRESS\"").contains("\"requiredAverage\":3.2");

        send("PUT", "/api/assessments/" + s2, token,
                "{\"name\":\"Sumativa 2\",\"type\":\"SUMMATIVE\",\"weight\":35,\"grade\":5.0}");
        send("PUT", "/api/assessments/" + exam, token,
                "{\"name\":\"Examen\",\"type\":\"EXAM\",\"weight\":30,\"grade\":4.0}");

        HttpResponse<String> full = send("GET", "/api/courses/" + courseId + "/summary", token, null);
        assertThat(full.body()).contains("\"finalGrade\":4.8").contains("\"status\":\"PASSED\"");

        HttpResponse<String> overall = send("GET", "/api/summary", token, null);
        assertThat(overall.body()).contains("\"cumulativeAverage\":4.8");
    }

    @Test
    void remindersAppearWhenTheirNoticeWindowStarts() throws Exception {
        String token = newUserToken();
        String courseId = newCourse(token);
        String dueDate = java.time.LocalDate.now().plusDays(2).toString();
        String assessmentId = create("/api/courses/" + courseId + "/assessments", token,
                "{\"name\":\"Prueba\",\"type\":\"SUMMATIVE\",\"weight\":50,\"dueDate\":\"" + dueDate + "\"}");

        String reminderId = create("/api/assessments/" + assessmentId + "/reminders", token, "{\"daysBefore\":3}");

        HttpResponse<String> upcoming = send("GET", "/api/reminders/upcoming", token, null);
        assertThat(upcoming.body()).contains("\"reminderId\":" + reminderId);

        assertThat(send("POST", "/api/reminders/" + reminderId + "/dismiss", token, null).statusCode())
                .isEqualTo(204);
        assertThat(send("GET", "/api/reminders/upcoming", token, null).body())
                .doesNotContain("\"reminderId\":" + reminderId);
    }

    @Test
    void loginIsBlockedAfterTooManyFailedAttempts() throws Exception {
        String email = uniqueEmail();
        register(email);
        String wrong = "{\"email\":\"" + email + "\",\"password\":\"incorrecta123\"}";

        for (int attempt = 1; attempt <= 5; attempt++) {
            assertThat(send("POST", "/api/auth/login", null, wrong).statusCode()).isEqualTo(401);
        }

        HttpResponse<String> blocked = send("POST", "/api/auth/login", null, wrong);
        assertThat(blocked.statusCode()).isEqualTo(429);
        assertThat(blocked.headers().firstValue("Retry-After")).isPresent();

        // Mientras dure el bloqueo, ni siquiera la clave correcta entra.
        HttpResponse<String> correct = send("POST", "/api/auth/login", null,
                "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}");
        assertThat(correct.statusCode()).isEqualTo(429);
    }
}
