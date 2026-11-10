package cl.germain.studymanager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Las pruebas que usan la base de datos se saltan solas cuando PostgreSQL no esta levantado,
 * asi "./mvnw test" funciona igual sin Docker.
 */
public final class TestDatabase {

    private TestDatabase() {
    }

    public static boolean isAvailable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 5432), 500);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
