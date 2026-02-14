package pt.isep.bookmarkscc.domain;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Bookmark(
        String id,
        String title,
        URI url,
        String tags,
        Instant createdAt
) {
    public Bookmark {
        Objects.requireNonNull(id);
        Objects.requireNonNull(title);
        Objects.requireNonNull(url);
        Objects.requireNonNull(createdAt);
    }

    public static Bookmark create(String title, URI url, String tags) {
        return new Bookmark(UUID.randomUUID().toString(), title, url, tags == null ? "" : tags, Instant.now());
    }
}

