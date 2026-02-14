package pt.isep.bookmarkscc;

import pt.isep.bookmarkscc.pteid.PteidCardPoller;

import java.util.Arrays;

public final class AppResources {

    private static volatile PteidCardPoller poller;

    // chave da BD (derivada do Cartão de Cidadão)
    private static volatile byte[] dbKey;

    // ✅ NOVO: fotografia do utilizador (bytes do cartão, mantidos só em RAM)
    private static volatile byte[] userPhoto;

    private AppResources() {}

    // -------- Poller --------
    public static void setPoller(PteidCardPoller p) {
        poller = p;
    }

    // -------- DB Key --------
    public static void setDbKey(byte[] key32) {
        dbKey = key32;
    }

    public static byte[] getDbKey() {
        return dbKey;
    }

    // -------- User Photo --------
    public static void setUserPhoto(byte[] photoBytes) {
        userPhoto = photoBytes;
    }

    public static byte[] getUserPhoto() {
        return userPhoto;
    }

    // -------- Shutdown / Logout --------
    public static void shutdown() {

        // limpa chave da BD da RAM
        if (dbKey != null) {
            pt.isep.bookmarkscc.security.KeyDerivation.zeroize(dbKey);
            dbKey = null;
        }

        // ✅ limpa foto da RAM
        if (userPhoto != null) {
            Arrays.fill(userPhoto, (byte) 0);
            userPhoto = null;
        }

        // para monitor/poller do cartão
        if (poller != null) {
            poller.stop();
            poller = null;
        } else {
            // fallback: garante libertação do SDK caso não haja poller registado
            pt.isep.bookmarkscc.pteid.PteidSdk.stop();
        }
    }
}
