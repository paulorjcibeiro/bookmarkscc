package pt.isep.bookmarkscc.security;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

public final class CryptoBox {
    private static final SecureRandom RNG = new SecureRandom();
    private static final int NONCE_LEN = 12;      // recomendado para GCM
    private static final int TAG_BITS = 128;      // tag 16 bytes

    private CryptoBox() {}

    public static byte[] newNonce() {
        byte[] n = new byte[NONCE_LEN];
        RNG.nextBytes(n);
        return n;
    }

    public static byte[] encrypt(byte[] key32, byte[] nonce, byte[] plaintext) {
        if (key32 == null || key32.length != 32) throw new IllegalArgumentException("key32 tem de ter 32 bytes");
        if (nonce == null || nonce.length != NONCE_LEN) throw new IllegalArgumentException("nonce tem de ter 12 bytes");

        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key32, "AES"), new GCMParameterSpec(TAG_BITS, nonce));
            return c.doFinal(plaintext);
        } catch (Exception e) {
            throw new RuntimeException("Falha a cifrar", e);
        }
    }

    public static byte[] decrypt(byte[] key32, byte[] nonce, byte[] ciphertext) {
        if (key32 == null || key32.length != 32) throw new IllegalArgumentException("key32 tem de ter 32 bytes");
        if (nonce == null || nonce.length != NONCE_LEN) throw new IllegalArgumentException("nonce tem de ter 12 bytes");
        if (ciphertext == null || ciphertext.length == 0) throw new IllegalArgumentException("ciphertext vazio");

        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key32, "AES"), new GCMParameterSpec(TAG_BITS, nonce));
            return c.doFinal(ciphertext);
        } catch (Exception e) {
            throw new RuntimeException("Falha a decifrar", e);
        }
    }
}
