package cl.germain.studymanager.auth;

public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds) {
}
