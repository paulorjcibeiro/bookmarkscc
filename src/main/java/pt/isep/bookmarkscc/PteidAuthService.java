package pt.isep.bookmarkscc.pteid;

import pt.gov.cartaodecidadao.*;
import java.io.ByteArrayInputStream;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public final class PteidAuthService {

    private PteidAuthService() {}

    public static AuthResult authenticateWithPinChallenge() {
        try {
            PteidSdk.start();
            if (!PteidSdk.isCardPresent())
                return AuthResult.fail("Nenhum cartão presente.");

            var card = PteidSdk.getEidCard();
            var eid = card.getID();

            String name = 
                    (eid.getGivenName() == null ? "" : eid.getGivenName()) + " " +
                    (eid.getSurname() == null ? "" : eid.getSurname());

            String doc = eid.getDocumentNumber() == null ? "" : eid.getDocumentNumber();

            byte[] photo = null;
            try {
                var b = eid.getPhotoObj().getphoto();
                photo = b != null ? b.GetBytes() : null;
            } catch (Exception ignored) {}

            byte[] certBytes = card.getCertificates()
                                   .getAuthentication()
                                   .getCertData()
                                   .GetBytes();

            var cert = (X509Certificate)
                    CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(certBytes));

            byte[] challenge = new byte[32];
            new SecureRandom().nextBytes(challenge);

            byte[] hash = MessageDigest.getInstance("SHA-256").digest(challenge);
            byte[] signature = card.Sign(new PTEID_ByteArray(hash, hash.length), false)
                                   .GetBytes();

            var sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(cert.getPublicKey());
            sig.update(challenge);

            if (!sig.verify(signature))
                return AuthResult.fail("Assinatura inválida.");

            return AuthResult.success(name.trim(), doc, cert.getEncoded(), photo);

        } catch (Exception e) {
            return AuthResult.fail(e.getMessage());
        }
    }
}
