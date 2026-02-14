
package pt.isep.bookmarkscc.pteid;

import java.security.PublicKey;
import java.security.Signature;
import java.util.Objects;

public final class PteidSignatureVerifier {

    private PteidSignatureVerifier() {}

    /** RSA PKCS#1 v1.5 sobre o hash puro (32 bytes). */
    public static boolean verifyRsaOverHash(PublicKey publicKey, byte[] sha256Hash, byte[] signatureBytes) {
        Objects.requireNonNull(publicKey);
        Objects.requireNonNull(sha256Hash);
        Objects.requireNonNull(signatureBytes);

        try {
            Signature verifier = Signature.getInstance("NONEwithRSA");
            verifier.initVerify(publicKey);
            verifier.update(sha256Hash);
            return verifier.verify(signatureBytes);
        } catch (Exception e) {
            return false;
        }
    }

    /** RSA PKCS#1 v1.5 sobre DigestInfo(SHA-256) (DER prefix + 32 bytes hash). */
    public static boolean verifyRsaOverSha256DigestInfo(PublicKey publicKey, byte[] sha256Hash, byte[] signatureBytes) {
        Objects.requireNonNull(publicKey);
        Objects.requireNonNull(sha256Hash);
        Objects.requireNonNull(signatureBytes);

        try {
            byte[] digestInfo = sha256DigestInfo(sha256Hash);

            Signature verifier = Signature.getInstance("NONEwithRSA");
            verifier.initVerify(publicKey);
            verifier.update(digestInfo);
            return verifier.verify(signatureBytes);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] sha256DigestInfo(byte[] sha256Hash) {
        if (sha256Hash.length != 32) {
            throw new IllegalArgumentException("SHA-256 hash deve ter 32 bytes, tem: " + sha256Hash.length);
        }

        // DER DigestInfo prefix para SHA-256:
        // 30 31 30 0d 06 09 60 86 48 01 65 03 04 02 01 05 00 04 20 || <32 bytes hash>
        byte[] prefix = new byte[] {
                0x30, 0x31,
                0x30, 0x0d,
                0x06, 0x09,
                0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x01,
                0x05, 0x00,
                0x04, 0x20
        };

        byte[] out = new byte[prefix.length + 32];
        System.arraycopy(prefix, 0, out, 0, prefix.length);
        System.arraycopy(sha256Hash, 0, out, prefix.length, 32);
        return out;
    }
}
