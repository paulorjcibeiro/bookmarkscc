package pt.isep.bookmarkscc.persistence;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import pt.isep.bookmarkscc.AppResources;
import pt.isep.bookmarkscc.domain.Bookmark;

public final class BookmarkRepository {

    private final Connection conn;

    public BookmarkRepository(Connection conn) {
        this.conn = conn;
    }

    public void saveEncrypted(Bookmark b) {
        byte[] key32 = AppResources.getDbKey();
        if (key32 == null || key32.length != 32) {
            throw new IllegalStateException("DB key não está disponível (cartão não autenticado?)");
        }

        try (PreparedStatement ps = conn.prepareStatement("""
            INSERT INTO bookmarks (nonce, data, created_at)
            VALUES (?, ?, ?)
        """)) {

            byte[] nonce = pt.isep.bookmarkscc.security.CryptoBox.newNonce();
            byte[] plain = serializePlain(b);
            byte[] cipher = pt.isep.bookmarkscc.security.CryptoBox.encrypt(key32, nonce, plain);

            ps.setBytes(1, nonce);
            ps.setBytes(2, cipher);
            ps.setString(3, b.createdAt().toString());

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("Falha ao guardar bookmark", e);
        }
    }

    public List<Bookmark> listDecrypted() {
        byte[] key32 = AppResources.getDbKey();
        if (key32 == null || key32.length != 32) {
            throw new IllegalStateException("DB key não está disponível (cartão não autenticado?)");
        }

        List<Bookmark> out = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement("""
            SELECT nonce, data, created_at
            FROM bookmarks
            ORDER BY created_at DESC
        """);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                byte[] nonce = rs.getBytes("nonce");
                byte[] cipher = rs.getBytes("data");

                // ignora registos antigos / inválidos (ex.: nonce != 12 do "plain" antigo)
                if (nonce == null || nonce.length != 12 || cipher == null || cipher.length == 0) {
                    continue;
                }

                try {
                    byte[] plain = pt.isep.bookmarkscc.security.CryptoBox
                            .decrypt(key32, nonce, cipher);

                    Bookmark b = deserializePlain(
                            plain,
                            Instant.parse(rs.getString("created_at"))
                    );

                    out.add(b);
                } catch (Exception ignored) {
                    // ignora linhas que não decifram (key errada, dados corrompidos, etc.)
                }
            }

            return out;

        } catch (Exception e) {
            throw new RuntimeException("Falha ao ler bookmarks", e);
        }
    }

    private byte[] serializePlain(Bookmark b) {
        String s = b.id() + "|" + b.title() + "|" + b.url() + "|" + b.tags();
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private Bookmark deserializePlain(byte[] plain, Instant createdAt) {
        String s = new String(plain, StandardCharsets.UTF_8);
        String[] p = s.split("\\|", -1);

        return new Bookmark(
                p[0],
                p[1],
                java.net.URI.create(p[2]),
                p[3],
                createdAt
        );
    }
}
