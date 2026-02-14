package pt.isep.bookmarkscc.pteid;

import pt.gov.cartaodecidadao.PTEID_ByteArray;
import pt.gov.cartaodecidadao.PTEID_Certificate;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public final class PteidX509 {

    private PteidX509() {}

    public static X509Certificate toX509(PTEID_Certificate cert) throws Exception {
        PTEID_ByteArray der = cert.getCertData();
        byte[] bytes = der.GetBytes();
        return (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(bytes));
    }
}
