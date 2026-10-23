package cl.germain.studymanager.auth;

public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException() {
        super("Email is already registered");
    }
}
