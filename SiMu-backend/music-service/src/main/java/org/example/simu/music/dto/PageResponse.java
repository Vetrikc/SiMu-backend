package org.example.simu.music.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Keyset page: {@code nextCursor} is the id to pass to the next request, {@code null} when the
 * collection is exhausted.
 */
public record PageResponse<T>(List<T> items, Long nextCursor, boolean hasMore) implements Serializable {
}