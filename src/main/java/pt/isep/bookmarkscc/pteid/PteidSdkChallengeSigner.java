package pt.isep.bookmarkscc.pteid;

import pt.gov.cartaodecidadao.PTEID_ByteArray;
import pt.gov.cartaodecidadao.PTEID_EIDCard;
import pt.gov.cartaodecidadao.PTEID_Exception;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public final class PteidSdkChallengeSigner {

    private PteidSdkChallengeSigner() {}

    public static byte[] signChallenge(PTEID_EIDCard card, byte[] challenge) throws PTEID_Exception {
        Objects.requireNonNull(card, "card");
        Objects.requireNonNull(challenge, "challenge");

        byte[] hash = sha256(challenge);

        PTEID_ByteArray toSign = new PTEID_ByteArray(hash, hash.length);

        // Pode lançar PTEID_Exception (PIN cancelado, cartão removido, reader error, etc.)
        PTEID_ByteArray out = card.Sign(toSign, false);

        return out.GetBytes();
    }

    private static byte[] sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível no runtime.", e);
        }
    }
}
