package pt.isep.bookmarkscc.persistence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public final class Db {
    private static Connection conn;

    private Db() {}

    public static synchronized Connection open() {
        if (conn != null) return conn;

        try {
            Path dir = Path.of(System.getProperty("user.home"), ".bookmarkscc");
            Files.createDirectories(dir);

            String url = "jdbc:sqlite:" + dir.resolve("bookmarks.db");
            conn = DriverManager.getConnection(url);

            try (Statement st = conn.createStatement()) {
                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS bookmarks (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      nonce BLOB NOT NULL,
                      data  BLOB NOT NULL,
                      created_at TEXT NOT NULL
                    )
                """);
            }

            return conn;
        } catch (Exception e) {
            throw new RuntimeException("Falha a abrir/criar DB", e);
        }
    }

    public static synchronized void close() {
        if (conn == null) return;
        try { conn.close(); } catch (Exception ignored) {}
        conn = null;
    }
}
