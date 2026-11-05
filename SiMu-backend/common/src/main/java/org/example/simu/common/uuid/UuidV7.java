package org.example.simu.common.uuid;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * RFC 9562 UUID version 7 generator (48-bit unix-epoch milliseconds + 74 random bits).
 * <p>
 * Time-ordered identifiers keep B-tree indexes compact: new rows are appended at the end of the
 * index instead of being scattered over the whole page set like random v4 UUIDs, and the id never
 * leaks how many rows exist in the table.
 */
public final class UuidV7 {

    private static final long RAND_A_MASK = 0x0FFFL;
    private static final long VARIANT_AND_RANDOM_B_MASK = 0x3FFFFFFFFFFFFFFFL;
    private static final long VERSION_7_BITS = 0x7000L;
    private static final long VARIANT_10_BITS = 0x8000000000000000L;

    private UuidV7() {
    }

    public static UUID generate() {
        long epochMillis = System.currentTimeMillis();
        long randomA = ThreadLocalRandom.current().nextLong() & RAND_A_MASK;
        long msb = (epochMillis << 16) | VERSION_7_BITS | randomA;

        long randomB = ThreadLocalRandom.current().nextLong() & VARIANT_AND_RANDOM_B_MASK;
        long lsb = VARIANT_10_BITS | randomB;

        return new UUID(msb, lsb);
    }

    /**
     * @return creation instant encoded in a v7 UUID, or {@code null} for any other version
     */
    public static java.time.Instant timestampOf(UUID uuid) {
        if (uuid == null || uuid.version() != 7) {
            return null;
        }
        return java.time.Instant.ofEpochMilli(uuid.getMostSignificantBits() >>> 16);
    }
}
