package pt.isep.bookmarkscc.pteid;

public record AuthResult(
        boolean ok,
        String displayName,
        String documentNumber,
        byte[] authCertDer,
        byte[] photoBytes,
        String error
) {
    public static AuthResult success(String displayName,
                                     String documentNumber,
                                     byte[] authCertDer,
                                     byte[] photoBytes) {
        return new AuthResult(
                true,
                displayName,
                documentNumber,
                authCertDer,
                photoBytes,
                null
        );
    }

    public static AuthResult fail(String error) {
        return new AuthResult(false, null, null, null, null, error);
    }
}
