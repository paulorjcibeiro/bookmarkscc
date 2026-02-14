package pt.isep.bookmarkscc;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathValidator;
import java.security.cert.CertStore;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
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

            byte[] certBytes = card.getCertificates()
                    .getAuthentication()
                    .getCertData()
                    .GetBytes();

            X509Certificate cert = (X509Certificate)
                    CertificateFactory.getInstance("X.509")
                            .generateCertificate(new ByteArrayInputStream(certBytes));

            // ================= VALIDAR CERTIFICADO COM ROOT CA =================

                cert.checkValidity();

                CertificateFactory cf = CertificateFactory.getInstance("X.509");

                // 1) Trust Anchor (Root oficial do Estado) em resources
                X509Certificate rootCert;
                try (var in = PteidAuthService.class.getResourceAsStream("/ecraizestado002.crt")) {
                if (in == null) return AuthResult.fail("Root CA não encontrada: /ECRaizEstado.crt");
                rootCert = (X509Certificate) cf.generateCertificate(in);
                }
                TrustAnchor trustAnchor = new TrustAnchor(rootCert, null);
                PKIXParameters params = new PKIXParameters(Set.of(trustAnchor));
                params.setRevocationEnabled(false); // mínimo para o assignment

                // 2) Intermédio 0017 (vem do cartão via getCA())
                X509Certificate ecAut0017 = (X509Certificate)
                        cf.generateCertificate(new ByteArrayInputStream(
                                card.getCertificates().getCA().getCertData().GetBytes()
                        ));

                // 3) Intermédio "Cartão de Cidadão 006" (vem de resources)
                X509Certificate cc006;
                try (var in = PteidAuthService.class.getResourceAsStream("/CartaoCidadao006.crt")) {
                if (in == null) return AuthResult.fail("Intermédio não encontrado: /CartaoCidadao006.crt");
                cc006 = (X509Certificate) cf.generateCertificate(in);
                }
                System.out.println("006 ISSUER: " + cc006.getIssuerX500Principal());
                System.out.println("ROOT SUBJECT: " + rootCert.getSubjectX500Principal());

                // 4) Validar caminho explícito: user -> 0017 -> 006
                CertPath certPath = cf.generateCertPath(List.of(cert, ecAut0017, cc006));
                CertPathValidator.getInstance("PKIX").validate(certPath, params);

            // ================= FIM VALIDAÇÃO =================

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
