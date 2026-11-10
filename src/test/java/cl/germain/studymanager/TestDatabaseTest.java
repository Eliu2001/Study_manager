package cl.germain.studymanager;

import java.sql.Connection;
import java.sql.SQLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TestDatabaseTest {

    @TempDir
    Path directory;

    private MockEnvironment configured() {
        return new MockEnvironment()
                .withProperty("DB_NAME", "study_manager")
                .withProperty("DB_USER", "test_user")
                .withProperty("DB_PASSWORD", "test_password")
                .withProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/${DB_NAME}_test")
                .withProperty("spring.datasource.username", "${DB_USER}")
                .withProperty("spring.datasource.password", "${DB_PASSWORD}");
    }

    @Test
    void connectsToTestDatabaseWithResolvedCredentialsAndClosesConnection() throws Exception {
        Connection connection = mock(Connection.class);
        when(connection.isValid(2)).thenReturn(true);

        assertThat(TestDatabase.isAvailable(configured(), (url, properties) -> {
            assertThat(url).isEqualTo("jdbc:postgresql://localhost:5432/study_manager_test");
            assertThat(properties.getProperty("user")).isEqualTo("test_user");
            assertThat(properties.getProperty("password")).isEqualTo("test_password");
            assertThat(properties.getProperty("connectTimeout")).isEqualTo("2");
            assertThat(properties.getProperty("socketTimeout")).isEqualTo("2");
            return connection;
        })).isTrue();

        verify(connection).close();
    }

    @Test
    void unavailableJdbcIsSkippedLocallyButRequiredModeFailsWithoutLeakingSecrets() {
        TestDatabase.Connector refused = (url, properties) -> {
            throw new SQLException("sensitive URL and test_password");
        };
        assertThat(TestDatabase.isAvailable(configured(), refused)).isFalse();
        assertThatThrownBy(() -> TestDatabase.isAvailable(
                configured().withProperty("TEST_DB_REQUIRED", "true"), refused))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TEST_DB_REQUIRED=true")
                .hasMessageNotContaining("test_password")
                .hasNoCause();
    }

    @Test
    void connectsOnlyToTheConfiguredPort() throws Exception {
        Connection connection = mock(Connection.class);
        when(connection.isValid(2)).thenReturn(true);
        var environment = configured().withProperty("DB_PORT", "5433")
                .withProperty("spring.datasource.url", "jdbc:postgresql://localhost:${DB_PORT}/${DB_NAME}_test");

        assertThat(TestDatabase.isAvailable(environment, (url, properties) -> {
            assertThat(url).isEqualTo("jdbc:postgresql://localhost:5433/study_manager_test");
            return connection;
        })).isTrue();
        verify(connection).close();

        assertThat(TestDatabase.isAvailable(configured().withProperty("DB_PORT", "5433"),
                (url, properties) -> {
                    throw new AssertionError("No debe conectarse al puerto de otra instancia");
                })).isFalse();
    }

    @Test
    void rejectsInvalidPortsBeforeConnecting() {
        for (String port : new String[]{"", "0", "65536", "-1", "5433/other", "abc"}) {
            assertThat(TestDatabase.isAvailable(configured().withProperty("DB_PORT", port),
                    (url, properties) -> {
                        throw new AssertionError("No debe intentar conectarse");
                    })).isFalse();
        }
    }

    @Test
    void rejectsDevelopmentAndOverriddenUrlsBeforeConnecting() {
        for (String url : new String[] {
                "jdbc:postgresql://localhost:5432/study_manager",
                "jdbc:postgresql://localhost:5432/study_manager_test?user=other",
                "jdbc:postgresql://remote:5432/study_manager_test"
        }) {
            assertThat(TestDatabase.isAvailable(configured().withProperty("spring.datasource.url", url),
                    (ignoredUrl, ignoredProperties) -> {
                        throw new AssertionError("No debe intentar conectarse");
                    })).isFalse();
        }
    }

    @Test
    void rejectsUnsafeDatabaseNamesAndMissingCredentialsBeforeConnecting() {
        for (String name : new String[] {"", "study/other", "study?user=other", "a".repeat(59)}) {
            assertThat(TestDatabase.isAvailable(configured().withProperty("DB_NAME", name),
                    (url, properties) -> {
                        throw new AssertionError("No debe intentar conectarse");
                    })).isFalse();
        }
        assertThat(TestDatabase.isAvailable(new MockEnvironment(), (url, properties) -> {
            throw new AssertionError("No debe intentar conectarse");
        })).isFalse();
    }

    @Test
    void springLoadsDotEnvAndTestProfileWithEnvironmentTakingPrecedence() throws Exception {
        Files.writeString(directory.resolve(".env"),
                "DB_NAME=file_database\nDB_USER=file_user\nDB_PASSWORD=file_password\n");
        Files.writeString(directory.resolve("application.properties"),
                "spring.config.import=optional:" + directory.resolve(".env").toUri()
                        + "[.properties]\n"
                        + "spring.datasource.url=jdbc:postgresql://localhost:5432/${DB_NAME}\n"
                        + "spring.datasource.username=${DB_USER}\n"
                        + "spring.datasource.password=${DB_PASSWORD}\n");
        Files.writeString(directory.resolve("application-test.properties"),
                "spring.datasource.url=jdbc:postgresql://localhost:5432/${DB_NAME}_test\n");
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().replace(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME,
                new MapPropertySource("systemProperties", Map.of(
                        "spring.config.location", directory.toUri().toString())));
        environment.getPropertySources().replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                new SystemEnvironmentPropertySource("systemEnvironment", Map.of(
                        "DB_NAME", "environment_database", "DB_USER", "environment_user")));
        environment.setActiveProfiles("test");
        ConfigDataEnvironmentPostProcessor.applyTo(environment);
        Connection connection = mock(Connection.class);
        when(connection.isValid(2)).thenReturn(true);

        assertThat(TestDatabase.isAvailable(environment, (url, properties) -> {
            assertThat(url).isEqualTo("jdbc:postgresql://localhost:5432/environment_database_test");
            assertThat(properties.getProperty("user")).isEqualTo("environment_user");
            assertThat(properties.getProperty("password")).isEqualTo("file_password");
            return connection;
        })).isTrue();
        verify(connection).close();
    }

    @Test
    void invalidConnectionIsUnavailableAndClosed() throws Exception {
        Connection connection = mock(Connection.class);
        when(connection.isValid(2)).thenReturn(false);
        assertThat(TestDatabase.isAvailable(configured(), (url, properties) -> connection)).isFalse();
        verify(connection).close();
    }

    @Test
    void misspelledRequiredModeCannotSilentlySkipTests() {
        assertThatThrownBy(() -> TestDatabase.isAvailable(
                configured().withProperty("TEST_DB_REQUIRED", "tru"), (url, properties) -> null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("TEST_DB_REQUIRED debe ser true o false.");
    }
}
