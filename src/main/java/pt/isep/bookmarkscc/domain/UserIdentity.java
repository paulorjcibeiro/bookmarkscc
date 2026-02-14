package pt.isep.bookmarkscc.domain;

import java.util.Arrays;
import java.util.Objects;

public final class UserIdentity {
    private final String fullName;
    private final byte[] photo; // pode ser null

    public UserIdentity(String fullName, byte[] photo) {
        this.fullName = Objects.requireNonNull(fullName);
        this.photo = photo; // pode ser null
    }

    public String fullName() {
        return fullName;
    }

    public byte[] photoCopy() {
        return photo == null ? null : Arrays.copyOf(photo, photo.length);
    }
}

