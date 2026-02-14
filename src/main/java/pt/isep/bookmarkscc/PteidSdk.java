package pt.isep.bookmarkscc.pteid;

import pt.gov.cartaodecidadao.*;

public final class PteidSdk {

    private static boolean started;

    private PteidSdk() {}

    public static synchronized void start() throws PTEID_Exception {
        if (started) return;

        System.load("/usr/local/lib/libpteidlibj.so");
        PTEID_ReaderSet.initSDK();
        started = true;
    }

    public static boolean isCardPresent() throws PTEID_Exception {
        return PTEID_ReaderSet.instance().getReader().isCardPresent();
    }

    public static PTEID_EIDCard getEidCard() throws PTEID_Exception {
        return PTEID_ReaderSet.instance().getReader().getEIDCard();
    }
}
