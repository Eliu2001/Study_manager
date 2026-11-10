package cl.germain.studymanager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;

/**
 * Comprueba exclusivamente la base de pruebas, sin arrancar Spring ni ejecutar migraciones.
 * En local una base ausente omite las pruebas; TEST_DB_REQUIRED=true convierte esto en error.
 */
public final class TestDatabase {

    private TestDatabase() {
    }

    public static boolean isAvailable() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.setActiveProfiles("test");
        try {
            // Mismos imports (.env), placeholders y precedencia que la aplicacion.
            ConfigDataEnvironmentPostProcessor.applyTo(environment);
        } catch (RuntimeException e) {
            return unavailable(required(environment));
        }
        return isAvailable(environment, DriverManager::getConnection);
    }

    static boolean isAvailable(Environment environment, Connector connector) {
        boolean required = required(environment);
        try {
            String database = environment.getRequiredProperty("DB_NAME");
            // Impide inyectar rutas, parametros JDBC o nombres truncados por PostgreSQL.
            if (!database.matches("[A-Za-z0-9_]{1,58}")) {
                return unavailable(required);
            }
            String port = environment.getProperty("DB_PORT", "5432");
            if (!port.matches("[0-9]{1,5}") || Integer.parseInt(port) < 1
                    || Integer.parseInt(port) > 65535) {
                return unavailable(required);
            }
            String expectedUrl = "jdbc:postgresql://localhost:" + port + "/" + database + "_test";
            String url = environment.getRequiredProperty("spring.datasource.url");
            if (!expectedUrl.equals(url)) {
                return unavailable(required);
            }
            String username = environment.getRequiredProperty("spring.datasource.username");
            String password = environment.getRequiredProperty("spring.datasource.password");
            if (username.isBlank() || password.isBlank()) {
                return unavailable(required);
            }
            Properties properties = new Properties();
            properties.setProperty("user", username);
            properties.setProperty("password", password);
            properties.setProperty("connectTimeout", "2");
            properties.setProperty("socketTimeout", "2");
            try (Connection connection = connector.connect(url, properties)) {
                return connection.isValid(2) || unavailable(required);
            }
        } catch (SQLException | IllegalArgumentException | IllegalStateException e) {
            // No adjuntar el error original: podria contener URL, usuario o secretos.
            return unavailable(required);
        }
    }

    private static boolean required(Environment environment) {
        String value = environment.getProperty("TEST_DB_REQUIRED", "false");
        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
            throw new IllegalStateException("TEST_DB_REQUIRED debe ser true o false.");
        }
        return Boolean.parseBoolean(value);
    }

    private static boolean unavailable(boolean required) {
        if (required) {
            throw new IllegalStateException("TEST_DB_REQUIRED=true: no se pudo validar la base JDBC "
                    + "exclusiva DB_NAME_test. Revise DB_NAME, DB_PORT, DB_USER, DB_PASSWORD y el perfil test; "
                    + "las pruebas de integracion son obligatorias.");
        }
        return false;
    }

    @FunctionalInterface
    interface Connector {
        Connection connect(String url, Properties properties) throws SQLException;
    }
}
