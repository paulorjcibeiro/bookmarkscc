package pt.isep.bookmarkscc.pteid;

import pt.gov.cartaodecidadao.PTEID_EIDCard;
import pt.gov.cartaodecidadao.PTEID_Exception;
import pt.gov.cartaodecidadao.PTEID_ReaderContext;
import pt.gov.cartaodecidadao.PTEID_ReaderSet;

public final class PteidSdk {

    private static volatile boolean started = false;

    private PteidSdk() {}

   public static synchronized void start() throws PTEID_Exception {
    if (started) return;

    try {
        System.load("/usr/local/lib/libpteidlibj.so");
    } catch (UnsatisfiedLinkError e) {
        throw new RuntimeException(
            "Falha a carregar libpteidlibj.so. java.library.path=" + System.getProperty("java.library.path"),
            e
        );
    }

    PTEID_ReaderSet.initSDK();

    started = true;
    System.out.println("[PTEID] SDK inicializado");
}


    public static synchronized void stop() {
    if (!started) return;

    try {
        PTEID_ReaderSet.releaseSDK();
        System.out.println("[PTEID] SDK libertado");
    } catch (PTEID_Exception e) {
        throw new RuntimeException("Falha a libertar o SDK", e);
    } finally {
        started = false;
    }
}


    public static PTEID_ReaderContext getReader() throws PTEID_Exception {
        return PTEID_ReaderSet.instance().getReader();
    }

    public static boolean isCardPresent() throws PTEID_Exception {
        return getReader().isCardPresent();
    }

    public static PTEID_EIDCard getEidCard() throws PTEID_Exception {
        return getReader().getEIDCard();
    }
}
