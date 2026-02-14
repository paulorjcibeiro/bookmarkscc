package pt.isep.bookmarkscc.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

public final class KeyDerivation {
    private KeyDerivation() {}

    public static byte[] deriveKey32(byte[] certDerBytes, String documentNumber) {
        if (certDerBytes == null || certDerBytes.length == 0) {
            throw new IllegalArgumentException("certDerBytes vazio");
        }
        if (documentNumber == null || documentNumber.isBlank()) {
            throw new IllegalArgumentException("documentNumber vazio");
        }

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(certDerBytes);
            md.update(documentNumber.getBytes(StandardCharsets.UTF_8));
            return md.digest(); // 32 bytes
        } catch (Exception e) {
            throw new RuntimeException("Falha na derivação de chave", e);
        }
    }

    public static void zeroize(byte[] k) {
        if (k != null) Arrays.fill(k, (byte) 0);
    }
}
