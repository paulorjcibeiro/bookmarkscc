package pt.isep.bookmarkscc.pteid;

public record AuthResult(
        boolean ok,
        String displayName,
        String documentNumber,
        byte[] authCertDer,
        byte[] photoBytes,
        String error) {

    public static AuthResult success(String n, String d, byte[] c, byte[] p) {
        return new AuthResult(true, n, d, c, p, null);
    }

    public static AuthResult fail(String e) {
        return new AuthResult(false, null, null, null, null, e);
    }
}
