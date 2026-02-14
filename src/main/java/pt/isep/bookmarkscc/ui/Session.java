package pt.isep.bookmarkscc.ui;

public final class Session {
    private static volatile String displayName;
    private static volatile String documentNumber;

    private Session() {}

    public static void login(String name, String doc) {
        displayName = name;
        documentNumber = doc;
    }

    public static void logout() {
        displayName = null;
        documentNumber = null;
    }

    public static boolean isLoggedIn() {
        return displayName != null && documentNumber != null;
    }

    public static String displayName() { return displayName; }
    public static String documentNumber() { return documentNumber; }
}
