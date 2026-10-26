package cl.germain.studymanager.common;

public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}