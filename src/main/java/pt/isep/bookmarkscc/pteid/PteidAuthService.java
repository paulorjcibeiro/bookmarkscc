package pt.isep.bookmarkscc.pteid;

import pt.gov.cartaodecidadao.PTEID_ByteArray;
import pt.gov.cartaodecidadao.PTEID_Certificates;
import pt.gov.cartaodecidadao.PTEID_EIDCard;
import pt.gov.cartaodecidadao.PTEID_EId;
import pt.gov.cartaodecidadao.PTEID_Photo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public final class PteidAuthService {

    private static final SecureRandom RNG = new SecureRandom();

    private PteidAuthService() {}

    public static AuthResult authenticateOffline() {
        try {
            PteidSdk.start();

            if (!PteidSdk.isCardPresent()) {
                return AuthResult.fail("Nenhum Cartão de Cidadão presente no leitor.");
            }

            PTEID_EIDCard card = PteidSdk.getEidCard();

            // 1) Identidade básica
            PTEID_EId eid = card.getID();
            String displayName = safe(eid.getGivenName()) + " " + safe(eid.getSurname());
            String documentNumber = safe(eid.getDocumentNumber());

            // ✅ FOTO (Java SDK): eid.getPhotoObj().getphoto() -> PNG
            byte[] photoBytes = null;
            try {
                PTEID_Photo photoObj = eid.getPhotoObj();
                PTEID_ByteArray png = photoObj.getphoto();   // PNG
                photoBytes = (png != null) ? png.GetBytes() : null;
            } catch (Throwable ignored) {
                // se a foto falhar por qualquer motivo, não bloqueia login
                photoBytes = null;
            }

            // 2) Certificados
            PTEID_Certificates certs = card.getCertificates();

            X509Certificate authLeaf = PteidX509.toX509(certs.getAuthentication());
            byte[] authCertDer = authLeaf.getEncoded();

            // Pool de certificados
            long total = certs.countAll();
            List<X509Certificate> pool = new ArrayList<>((int) total);

            for (long i = 0; i < total; i++) {
                try {
                    pool.add(PteidX509.toX509(certs.getCert(i)));
                } catch (Exception ignored) {}
            }

            PteidChainBuilder.buildAndValidateAuthChain(authLeaf, pool);

            return AuthResult.success(
                    displayName.trim(),
                    documentNumber,
                    authCertDer,
                    photoBytes
            );

        } catch (Throwable t) {
            String msg = t.getClass().getSimpleName() + ": " +
                    (t.getMessage() == null ? "" : t.getMessage());
            return AuthResult.fail(msg.trim());
        }
    }

    /**
     * Autenticação com prova de posse (PIN):
     * - valida cadeia (como offline)
     * - gera challenge
     * - pede ao cartão para assinar (PIN)
     * - valida assinatura com a chave pública do certificado autenticado
     */
    public static AuthResult authenticateWithPinChallenge() {
        try {
            PteidSdk.start();

            if (!PteidSdk.isCardPresent()) {
                return AuthResult.fail("Nenhum Cartão de Cidadão presente no leitor.");
            }

            PTEID_EIDCard card = PteidSdk.getEidCard();

            // 1) Identidade básica
            PTEID_EId eid = card.getID();
            String displayName = safe(eid.getGivenName()) + " " + safe(eid.getSurname());
            String documentNumber = safe(eid.getDocumentNumber());

            // ✅ FOTO (não bloqueia login)
            byte[] photoBytes = null;
            try {
                PTEID_Photo photoObj = eid.getPhotoObj();
                PTEID_ByteArray png = photoObj.getphoto();
                photoBytes = (png != null) ? png.GetBytes() : null;
            } catch (Throwable ignored) {
                photoBytes = null;
            }

            // 2) Certificados + cadeia
            PTEID_Certificates certs = card.getCertificates();
            X509Certificate authLeaf = PteidX509.toX509(certs.getAuthentication());
            byte[] authCertDer = authLeaf.getEncoded();

            long total = certs.countAll();
            List<X509Certificate> pool = new ArrayList<>((int) total);
            for (long i = 0; i < total; i++) {
                try {
                    pool.add(PteidX509.toX509(certs.getCert(i)));
                } catch (Exception ignored) {}
            }

            PteidChainBuilder.buildAndValidateAuthChain(authLeaf, pool);

            // 3) Prova de posse: challenge -> sign (PIN) -> verify
            byte[] challenge = buildChallenge(documentNumber);
            byte[] signature = PteidSdkChallengeSigner.signChallenge(card, challenge);

            byte[] hash = MessageDigest.getInstance("SHA-256").digest(challenge);

            boolean ok = PteidSignatureVerifier.verifyRsaOverSha256DigestInfo(
                    authLeaf.getPublicKey(),
                    hash,
                    signature
            );

            if (!ok) {
                return AuthResult.fail("Assinatura inválida (prova de posse falhou).");
            }

            return AuthResult.success(
                    displayName.trim(),
                    documentNumber,
                    authCertDer,
                    photoBytes
            );

        } catch (Throwable t) {
            String msg = t.getClass().getSimpleName() + ": " +
                    (t.getMessage() == null ? "" : t.getMessage());
            return AuthResult.fail(msg.trim());
        }
    }

    private static byte[] buildChallenge(String documentNumber) {
        byte[] nonce = new byte[32];
        RNG.nextBytes(nonce);

        long now = System.currentTimeMillis();
        byte[] doc = safe(documentNumber).getBytes(StandardCharsets.UTF_8);

        // nonce(32) || timestamp(8) || doc(N)
        byte[] out = new byte[32 + 8 + doc.length];
        System.arraycopy(nonce, 0, out, 0, 32);

        for (int i = 0; i < 8; i++) {
            out[32 + i] = (byte) (now >>> (56 - 8 * i));
        }

        System.arraycopy(doc, 0, out, 40, doc.length);
        return out;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
