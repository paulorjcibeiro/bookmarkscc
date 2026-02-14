package pt.isep.bookmarkscc;

public final class Session {

    private static String name;
    private static String doc;
    private static byte[] key;
    private static byte[] photo;

    private Session() {}

    public static void login(String n, String d) {
        name = n;
        doc = d;
    }

    public static void logout() {
        name = doc = null;
        key = photo = null;
    }

    public static boolean isLoggedIn() {
        return name != null;
    }

    public static String displayName() { return name; }
    public static String documentNumber() { return doc; }
    public static byte[] getDbKey() { return key; }
    public static byte[] getUserPhoto() { return photo; }
    public static void setUserPhoto(byte[] p) { photo = p; }

    public static void deriveAndSetKey(byte[] certDer, String doc) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            md.update(certDer);
            md.update(doc.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            key = md.digest();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
