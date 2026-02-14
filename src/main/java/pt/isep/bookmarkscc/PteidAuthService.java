package pt.isep.bookmarkscc;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;

import pt.gov.cartaodecidadao.PTEID_ByteArray;

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

            // ================= CERTIFICADO DO UTILIZADOR =================

            byte[] certBytes = card.getCertificates()
                    .getAuthentication()
                    .getCertData()
                    .GetBytes();

            X509Certificate cert = (X509Certificate)
                    CertificateFactory.getInstance("X.509")
                            .generateCertificate(new ByteArrayInputStream(certBytes));

            cert.checkValidity();

            // ================= VALIDAÇÃO PKIX =================

            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            // Root oficial do Estado (Trust Anchor)
            X509Certificate rootCert;
            try (var in = PteidAuthService.class
                    .getResourceAsStream("/ecraizestado002.crt")) {

                if (in == null)
                    return AuthResult.fail("Root CA (ecraizestado002.crt) não encontrada.");

                rootCert = (X509Certificate) cf.generateCertificate(in);
            }

            TrustAnchor trustAnchor = new TrustAnchor(rootCert, null);
            PKIXParameters params = new PKIXParameters(Set.of(trustAnchor));
            params.setRevocationEnabled(false);

            // Intermédio do cartão (EC Autenticação 0017)
            X509Certificate ecAut0017 = (X509Certificate)
                    cf.generateCertificate(new ByteArrayInputStream(
                            card.getCertificates()
                                .getCA()
                                .getCertData()
                                .GetBytes()
                    ));

            // Intermédio CC 006 (resource)
            X509Certificate cc006;
            try (var in = PteidAuthService.class
                    .getResourceAsStream("/CartaoCidadao006.crt")) {

                if (in == null)
                    return AuthResult.fail("Intermédio CartaoCidadao006.crt não encontrado.");

                cc006 = (X509Certificate) cf.generateCertificate(in);
            }

            // Cadeia explícita: user → 0017 → 006
            CertPath certPath = cf.generateCertPath(List.of(cert, ecAut0017, cc006));
            CertPathValidator.getInstance("PKIX").validate(certPath, params);

            // ================= ASSINATURA CHALLENGE =================

            byte[] challenge = new byte[32];
            new SecureRandom().nextBytes(challenge);

            byte[] hash = MessageDigest.getInstance("SHA-256").digest(challenge);

            byte[] signature = card
                    .Sign(new PTEID_ByteArray(hash, hash.length), false)
                    .GetBytes();

            Signature sig = Signature.getInstance("SHA256withRSA");
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
