package jdk.internal.m3.collections.packed;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;

/** Deterministic baseline/candidate tape; executed against separately compiled real target. */
public final class PackedMultiMapFilterContract {
    private static long checks;
    private static long state;
    private static final long[] KEYS = {
            Long.MIN_VALUE, Long.MAX_VALUE, -9223372036854775807L,
            -1048576L, -1024L, -8L, -1L, 0L, 1L, 7L, 8L, 1024L, 1048576L
    };

    public static void main(String[] args) throws Exception {
        StringBuilder outcomes = new StringBuilder();
        scenario(outcomes);
        for (int seed = 0; seed < 16; seed++) {
            state = 0x6a09e667f3bcc909L + seed * 0x9e3779b97f4a7c15L;
            PackedLongLongMultiMap map = new PackedLongLongMultiMap(2, 2);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (int step = 0; step < 3000; step++) {
                long key = KEYS[nextInt(KEYS.length)];
                long value = value();
                switch (nextInt(12)) {
                    case 0, 1, 2, 3 -> map.add(key, value);
                    case 4 -> write(digest, map.remove(key, value) ? 1L : 0L);
                    case 5 -> write(digest, map.removeAll(key));
                    case 6 -> {
                        int mask = nextInt(8);
                        write(digest, map.filterInPlace((k, v) ->
                                (Long.bitCount(k ^ v) & 7) != mask));
                    }
                    case 7 -> {
                        long before = checksum(map);
                        long sentinel = value;
                        try {
                            map.filterInPlace((k, v) -> {
                                if (k == sentinel) throw new Intentional();
                                return true;
                            });
                        } catch (Intentional expected) {
                            write(digest, 1);
                            check(checksum(map) == before, "throwing predicate must not commit");
                        }
                    }
                    case 8 -> write(digest, map.containsEntry(key, value) ? 1L : 0L);
                    case 9 -> write(digest, map.valueCount(key));
                    case 10 -> {
                        if ((step & 255) == 0) map.clear();
                    }
                    default -> write(digest, map.containsKey(key) ? 1L : 0L);
                }
                if ((step & 7) == 0) record(map, digest);
            }
            record(map, digest);
            outcomes.append(seed).append(':').append(HexFormat.of().formatHex(digest.digest())).append('\n');
        }
        System.out.print("PASS checks=" + checks + " seeds=16 operations=48000\n" + outcomes);
    }

    private static void scenario(StringBuilder out) {
        PackedLongLongMultiMap map = new PackedLongLongMultiMap(1, 1);
        map.add(7, 100);
        map.add(7, 200);
        map.add(9, 300);
        check(map.remove(7, 100), "slot recycle setup");
        map.add(7, 400);
        check(Arrays.equals(map.values(7), new long[] {200, 400}), "before filtering order");
        check(map.filterInPlace((k, v) -> k != 9) == 1, "one filtered key");
        check(Arrays.equals(map.values(7), new long[] {400, 200}),
                "existing physical scan replay order after filtering");
        check(map.size() == 2 && map.distinctKeyCount() == 1, "count preserved");
        check(map.filterInPlace((k, v) -> true) == 0, "no-change fast path");
        check(map.filterInPlace((k, v) -> false) == 2, "remove all");
        check(map.isEmpty() && map.distinctKeyCount() == 0, "all removed");
        check(map.filterInPlace((k, v) -> true) == 0, "empty path");
        boolean npe = false;
        try { map.filterInPlace(null); } catch (NullPointerException expected) { npe = true; }
        check(npe, "null filter refusal");
        out.append("recycle-order-ok\n");
    }

    private static void record(PackedLongLongMultiMap map, MessageDigest d) {
        write(d, map.size());
        write(d, map.distinctKeyCount());
        final long[] count = {0};
        map.forEach((k, v) -> {
            write(d, k);
            write(d, v);
            count[0]++;
        });
        check(count[0] == map.size(), "physical traversal cardinality");
        long counted = 0;
        for (long key : KEYS) {
            long[] values = map.values(key);
            write(d, values.length);
            check(values.length == map.valueCount(key), "per-key cardinality");
            check((values.length != 0) == map.containsKey(key), "presence consistency");
            for (long v : values) {
                write(d, v);
                check(map.containsEntry(key, v), "every returned entry present");
            }
            counted += values.length;
        }
        check(counted == map.size(), "all values accounted");
    }

    private static long checksum(PackedLongLongMultiMap map) {
        final long[] result = {map.size() * 0x9e3779b97f4a7c15L};
        map.forEach((k,v)-> result[0] = Long.rotateLeft(result[0] ^ k, 11) * 31 + v);
        return result[0];
    }
    private static long value() {
        return switch (nextInt(7)) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> 0;
            case 3 -> 1;
            default -> random();
        };
    }
    private static long random() {
        long x = state;
        x ^= x >>> 12; x ^= x << 25; x ^= x >>> 27;
        state = x;
        return x * 2685821657736338717L;
    }
    private static int nextInt(int bound) { return (int) Long.remainderUnsigned(random(), bound); }
    private static void write(MessageDigest digest, long value) {
        digest.update(ByteBuffer.allocate(Long.BYTES).putLong(value).array());
    }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static final class Intentional extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
