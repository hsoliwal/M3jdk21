// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.function.BiFunction;

/**
 * Naive per-row reference model of the relocated object-signal family (recipe object-signal-20261107; a test main,
 * not a test by name, driven by M3ObjectSignalTest and by the mutant gate).
 *
 * <p>Every row is evaluated one at a time from the signal the projector produced, with code that shares nothing with
 * the owners: the feature mask law {@code (bits & required) == required && (bits & forbidden) == 0}, the kind / domain
 * / length / hash / identity metadata law of the query record and the first-pass and exact-stage counters. The table
 * must agree row by row, on every random and edge table size (0, 1, 63, 64, 65, ..., 16,449 rows = 258 words, above
 * the donor's 256-word JNI threshold), with and without initial candidates; the projectors must keep the literal
 * containment law (no false negative) for String and byte[] signatures.</p>
 */
//FLLDCIM: TOP - reference model of M3ObjectSignalTable / M3ObjectSignalQuery / M3ObjectSignalMatcher; recipe m3/collections/qualification/object-signal-20261107
public final class M3ObjectSignalReferenceModel {
    private static final int[] SIZES = {0, 1, 2, 63, 64, 65, 127, 128, 129, 191, 500, 1000, 4096, 4097, 16449};
    private static final long[] EDGE_MASKS = {0L, -1L, 1L, Long.MIN_VALUE, Long.MAX_VALUE, 0x5555_5555_5555_5555L,
        0xAAAA_AAAA_AAAA_AAAAL, 0xFFFF_0000_0000_0000L, 0x0000_0000_FFFF_FFFFL};
    private static final M3ObjectProjectionKind[] KINDS = M3ObjectProjectionKind.values();
    private long checks;

    private M3ObjectSignalReferenceModel() { }

    public static void main(String[] args) {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 20261107L;
        M3ObjectSignalReferenceModel model = new M3ObjectSignalReferenceModel();
        model.tables(new Random(seed));
        model.matcher(new Random(seed + 1));
        model.containment(new Random(seed + 2));
        model.bitmaps(new Random(seed + 3));
        model.refusals();
        model.monitors();
        System.out.println("PASS: " + model.checks + " object-signal reference-model checks (seed " + seed + ")");
    }

    private void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    // ---- random rows and queries -----------------------------------------------------------------------------

    private static long mask(Random random) {
        return switch (random.nextInt(5)) {
            case 0 -> EDGE_MASKS[random.nextInt(EDGE_MASKS.length)];
            case 1 -> random.nextLong();
            case 2 -> random.nextLong() & random.nextLong() & random.nextLong();
            case 3 -> 1L << random.nextInt(64);
            default -> random.nextLong() | random.nextLong();
        };
    }

    private static M3ObjectSignal row(Random random) {
        int flags = random.nextInt(8);
        int length = (flags & M3ObjectSignal.HAS_LENGTH) != 0 || random.nextBoolean() ? random.nextInt(12) : -1;
        return new M3ObjectSignal(KINDS[random.nextInt(KINDS.length)], random.nextInt(4), length,
            random.nextInt(5), mask(random), random.nextInt(5), flags);
    }

    private static M3ObjectSignalQuery query(Random random, List<M3ObjectSignal> rows) {
        if (!rows.isEmpty() && random.nextInt(4) == 0) {
            M3ObjectSignal target = rows.get(random.nextInt(rows.size()));
            return random.nextBoolean() ? M3ObjectSignalQuery.exact(target) : M3ObjectSignalQuery.maybeContains(target);
        }
        long required = mask(random) & (random.nextBoolean() ? random.nextLong() : -1L);
        long forbidden = mask(random) & ~required & (random.nextBoolean() ? random.nextLong() : -1L);
        if (random.nextInt(5) == 0) {
            return random.nextBoolean() ? M3ObjectSignalQuery.anyMask(required, forbidden)
                : M3ObjectSignalQuery.mask(random.nextInt(4), required, forbidden);
        }
        int min = random.nextInt(3) == 0 ? M3ObjectSignalQuery.UNBOUNDED : random.nextInt(8);
        int max = random.nextInt(3) == 0 ? M3ObjectSignalQuery.UNBOUNDED : Math.max(min, 0) + random.nextInt(8);
        return new M3ObjectSignalQuery(random.nextInt(KINDS.length + 1) - 1, random.nextInt(5) - 1, min, max,
            required, forbidden, random.nextInt(5), random.nextInt(5), random.nextInt(4));
    }

    // ---- the naive model ------------------------------------------------------------------------------------

    private static boolean bitsMatch(M3ObjectSignalQuery q, M3ObjectSignal s) {
        return (s.featureBits() & q.requiredBits()) == q.requiredBits() && (s.featureBits() & q.forbiddenBits()) == 0L;
    }

    private static boolean metadataMatch(M3ObjectSignalQuery q, M3ObjectSignal s) {
        boolean hasLength = (s.flags() & M3ObjectSignal.HAS_LENGTH) != 0;
        boolean ok = q.kindOrdinal() < 0 || q.kindOrdinal() == s.kind().ordinal();
        ok &= q.domain() < 0 || q.domain() == s.domain();
        ok &= q.minLength() < 0 || (hasLength && s.logicalLength() >= q.minLength());
        ok &= q.maxLength() < 0 || (hasLength && s.logicalLength() <= q.maxLength());
        ok &= (q.requirements() & M3ObjectSignalQuery.REQUIRE_HASH) == 0
            || ((s.flags() & M3ObjectSignal.HAS_HASH) != 0 && s.exactHash64() == q.exactHash64());
        ok &= (q.requirements() & M3ObjectSignalQuery.REQUIRE_IDENTITY) == 0
            || ((s.flags() & M3ObjectSignal.HAS_IDENTITY) != 0 && s.rawIdentity() == q.exactIdentity());
        return ok;
    }

    private void tables(Random random) {
        for (int size : SIZES) {
            for (int round = 0; round < (size > 4096 ? 2 : 6); round++) {
                List<M3ObjectSignal> rows = new ArrayList<>(size);
                for (int i = 0; i < size; i++) rows.add(row(random));
                M3ObjectSignalTable table = M3ObjectSignalTable.build(
                    rows, M3ObjectProjectors.custom(s -> s), M3PoolProgressMonitor.none());
                check(table.size() == size, "size " + size);
                check(table.primitivePayloadBytes() == size * 29L + 64L * 8L * ((size + 63) / 64),
                    "payload bytes " + size);
                for (int i = 0; i < size; i++) {
                    check(table.signalAt(i).equals(rows.get(i)), "signalAt round trip " + i);
                    check(table.featureBitsAt(i) == rows.get(i).featureBits(), "featureBitsAt " + i);
                    factories(rows.get(i));
                }
                for (int q = 0; q < 24; q++) firstPass(random, table, rows, query(random, rows));
            }
        }
    }

    /** The query factories, field by field, from the signal's flags (exact, maybeContains, mask, anyMask). */
    private void factories(M3ObjectSignal s) {
        int length = s.hasLength() ? s.logicalLength() : M3ObjectSignalQuery.UNBOUNDED;
        int requirements = (s.hasHash() ? M3ObjectSignalQuery.REQUIRE_HASH : 0)
            | (s.hasIdentity() ? M3ObjectSignalQuery.REQUIRE_IDENTITY : 0);
        check(M3ObjectSignalQuery.exact(s).equals(new M3ObjectSignalQuery(s.kind().ordinal(), s.domain(), length,
            length, s.featureBits(), 0L, s.exactHash64(), s.rawIdentity(), requirements)), "exact factory");
        check(M3ObjectSignalQuery.maybeContains(s).equals(new M3ObjectSignalQuery(s.kind().ordinal(), s.domain(),
            length, M3ObjectSignalQuery.UNBOUNDED, s.featureBits(), 0L, 0L, 0L, 0)), "maybeContains factory");
        check(M3ObjectSignalQuery.mask(s.domain(), s.featureBits(), 0L).equals(new M3ObjectSignalQuery(
            M3ObjectSignalQuery.ANY_KIND, s.domain(), M3ObjectSignalQuery.UNBOUNDED, M3ObjectSignalQuery.UNBOUNDED,
            s.featureBits(), 0L, 0L, 0L, 0)), "mask factory");
        check(M3ObjectSignalQuery.anyMask(0L, s.featureBits()).equals(new M3ObjectSignalQuery(
            M3ObjectSignalQuery.ANY_KIND, M3ObjectSignalQuery.ANY_DOMAIN, M3ObjectSignalQuery.UNBOUNDED,
            M3ObjectSignalQuery.UNBOUNDED, 0L, s.featureBits(), 0L, 0L, 0)), "anyMask factory");
    }

    private void firstPass(Random random, M3ObjectSignalTable table, List<M3ObjectSignal> rows,
            M3ObjectSignalQuery query) {
        int size = rows.size();
        boolean[] initial = new boolean[size];
        M3ObjectSignalFirstPassResult pass;
        if (random.nextBoolean()) {
            java.util.Arrays.fill(initial, true);
            pass = table.firstPass(query, M3PoolProgressMonitor.none());
        } else {
            long[] words = new long[(size + 63) >>> 6];
            for (int i = 0; i < size; i++) {
                initial[i] = random.nextInt(3) != 0;
                if (initial[i]) words[i >>> 6] |= 1L << (i & 63);
            }
            pass = table.firstPass(query, new M3ObjectSignalBitmap(words, size, false), M3PoolProgressMonitor.none());
        }
        int total = 0;
        int afterBits = 0;
        int afterMetadata = 0;
        for (int i = 0; i < size; i++) {
            M3ObjectSignal s = rows.get(i);
            boolean bits = initial[i] && bitsMatch(query, s);
            boolean expected = bits && metadataMatch(query, s);
            total += initial[i] ? 1 : 0;
            afterBits += bits ? 1 : 0;
            afterMetadata += expected ? 1 : 0;
            check(pass.candidates().contains(i) == expected, "first-pass row " + i + " of " + size);
        }
        check(pass.totalObjects() == total, "totalObjects");
        check(pass.afterBitwiseSignals() == afterBits, "afterBitwiseSignals");
        check(pass.afterMetadata() == afterMetadata, "afterMetadata");
        check(pass.rejectedByBitwiseSignals() == total - afterBits, "rejectedByBitwiseSignals");
        check(pass.rejectedByMetadata() == afterBits - afterMetadata, "rejectedByMetadata");
        check(pass.candidates().cardinality() == afterMetadata, "candidate cardinality");
        check(Double.compare(pass.survivorRatio(), total == 0 ? 0.0d : (double) afterMetadata / total) == 0,
            "survivorRatio");
    }

    private void matcher(Random random) {
        for (int size : SIZES) {
            List<M3ObjectSignal> rows = new ArrayList<>(size);
            for (int i = 0; i < size; i++) rows.add(row(random));
            M3ObjectSignalTable table = M3ObjectSignalTable.build(
                rows, M3ObjectProjectors.custom(s -> s), M3PoolProgressMonitor.none());
            for (int q = 0; q < 8; q++) {
                M3ObjectSignalQuery query = query(random, rows);
                long target = random.nextInt(5);
                M3ObjectSignalFirstPassResult pass = table.firstPass(query, M3PoolProgressMonitor.none());
                M3ObjectSignalExactResult exact = M3ObjectSignalMatcher.verify(
                    rows, pass, s -> s.rawIdentity() == target, M3PoolProgressMonitor.none());
                int checked = 0;
                int matched = 0;
                for (int i = 0; i < size; i++) {
                    boolean survivor = pass.candidates().contains(i);
                    boolean expected = survivor && rows.get(i).rawIdentity() == target;
                    checked += survivor ? 1 : 0;
                    matched += expected ? 1 : 0;
                    check(exact.exactMatches().contains(i) == expected, "exact row " + i);
                }
                check(exact.exactChecks() == checked, "exactChecks");
                check(exact.exactMatchCount() == matched, "exactMatchCount");
                check(exact.exactChecksAvoided() == size - checked, "exactChecksAvoided");
                check(exact.firstPass() == pass, "firstPass carried");
            }
        }
    }

    // ---- projectors: the containment law and the projected fields --------------------------------------------

    private static String text(Random random, int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(random.nextInt(4) == 0 ? (char) random.nextInt(0x10000) : (char) ('a' + random.nextInt(6)));
        }
        return builder.toString();
    }

    private void containment(Random random) {
        BiFunction<byte[], M3PoolProgressMonitor, byte[]> identity = (bytes, monitor) -> bytes;
        M3ObjectProjector<String> strings = M3ObjectProjectors.byString(value -> value);
        M3ObjectProjector<byte[]> bytes = M3ObjectProjectors.byCanonicalBytes(M3ObjectSignalDomains.CANONICAL_BYTES,
            identity);
        for (int round = 0; round < 4000; round++) {
            String haystack = text(random, random.nextInt(40));
            int from = random.nextInt(haystack.length() + 1);
            String needle = haystack.substring(from, from + random.nextInt(haystack.length() - from + 1));
            M3ObjectSignal h = strings.project(haystack, M3PoolProgressMonitor.none());
            M3ObjectSignal n = strings.project(needle, M3PoolProgressMonitor.none());
            check((h.featureBits() & n.featureBits()) == n.featureBits(), "String containment law");
            check(h.logicalLength() == haystack.length() && h.flags() == (M3ObjectSignal.HAS_LENGTH
                | M3ObjectSignal.HAS_HASH) && h.domain() == M3ObjectSignalDomains.STRING, "String projection fields");
            check(h.exactHash64() == M3ObjectSignalMath.hashString(haystack), "String exact hash");
            byte[] hb = haystack.getBytes(StandardCharsets.UTF_8);
            int bf = random.nextInt(hb.length + 1);
            byte[] nb = java.util.Arrays.copyOfRange(hb, bf, bf + random.nextInt(hb.length - bf + 1));
            M3ObjectSignal hs = bytes.project(hb, M3PoolProgressMonitor.none());
            M3ObjectSignal ns = bytes.project(nb, M3PoolProgressMonitor.none());
            check((hs.featureBits() & ns.featureBits()) == ns.featureBits(), "byte[] containment law");
            check(hs.logicalLength() == hb.length && hs.exactHash64() == M3ObjectSignalMath.hashBytes(hb)
                && hs.kind() == M3ObjectProjectionKind.CANONICAL_BYTES, "byte[] projection fields");
            long raw = random.nextLong();
            M3ObjectSignal index = M3ObjectProjectors.<Long>byIndex(7, Long::longValue)
                .project(raw, M3PoolProgressMonitor.none());
            check(index.rawIdentity() == raw && index.exactHash64() == M3ObjectSignalMath.mix64(raw)
                && Long.bitCount(index.featureBits()) <= 3 && index.flags() == 7, "long projection fields");
        }
        List<String> values = new ArrayList<>();
        for (int i = 0; i < 3000; i++) values.add(text(random, random.nextInt(24)));
        M3ObjectSignalTable table = M3ObjectSignalTable.build(values, strings, M3PoolProgressMonitor.none());
        for (int q = 0; q < 60; q++) {
            String needle = text(random, 1 + random.nextInt(3));
            M3ObjectSignalFirstPassResult pass = table.firstPass(
                M3ObjectSignalQuery.maybeContains(strings.project(needle, M3PoolProgressMonitor.none())),
                M3PoolProgressMonitor.none());
            M3ObjectSignalExactResult exact = M3ObjectSignalMatcher.verify(
                values, pass, value -> value.contains(needle), M3PoolProgressMonitor.none());
            for (int i = 0; i < values.size(); i++) {
                boolean direct = values.get(i).contains(needle);
                check(!direct || pass.candidates().contains(i), "no contains false negative " + i);
                check(exact.exactMatches().contains(i) == direct, "String.contains parity " + i);
            }
        }
    }

    // ---- bitmap algebra ---------------------------------------------------------------------------------------

    private void bitmaps(Random random) {
        for (int size : SIZES) {
            long[] a = new long[(size + 63) >>> 6];
            long[] b = new long[a.length];
            for (int i = 0; i < a.length; i++) {
                a[i] = mask(random);
                b[i] = mask(random);
            }
            M3ObjectSignalBitmap left = new M3ObjectSignalBitmap(a, size, false);
            M3ObjectSignalBitmap right = new M3ObjectSignalBitmap(b, size, false);
            M3ObjectSignalBitmap and = left.and(right);
            M3ObjectSignalBitmap andNot = left.andNot(right);
            int count = 0;
            int countAnd = 0;
            for (int i = 0; i < size; i++) {
                boolean l = (a[i >>> 6] & (1L << (i & 63))) != 0L;
                boolean r = (b[i >>> 6] & (1L << (i & 63))) != 0L;
                count += l ? 1 : 0;
                countAnd += l && r ? 1 : 0;
                check(left.contains(i) == l && and.contains(i) == (l && r) && andNot.contains(i) == (l && !r),
                    "bitmap algebra " + i);
            }
            check(left.cardinality() == count && and.cardinality() == countAnd, "bitmap cardinality " + size);
            check(left.isEmpty() == (count == 0), "bitmap isEmpty " + size);
            int[] visited = {0, -1};
            left.forEachIndex(i -> {
                visited[0]++;
                if (i <= visited[1] || i >= size) throw new AssertionError("forEachIndex order " + i);
                visited[1] = i;
            });
            check(visited[0] == count, "forEachIndex count " + size);
            check(M3ObjectSignalBitmap.all(size).cardinality() == size, "all " + size);
            check(M3ObjectSignalBitmap.empty(size).isEmpty(), "empty " + size);
            check(refuses(() -> left.contains(size)) && refuses(() -> left.contains(-1)), "contains bounds " + size);
            check(refuses(() -> left.and(M3ObjectSignalBitmap.all(size + 1))), "size mismatch " + size);
        }
    }

    private static boolean refuses(Runnable action) {
        try {
            action.run();
            return false;
        } catch (RuntimeException expected) {
            return true;
        }
    }

    private void refusals() {
        check(refuses(() -> new M3ObjectSignalQuery(-1, -1, -1, -1, 1L, 1L, 0L, 0L, 0)), "required & forbidden");
        check(refuses(() -> new M3ObjectSignalQuery(KINDS.length, -1, -1, -1, 0L, 0L, 0L, 0L, 0)), "kind range");
        check(refuses(() -> new M3ObjectSignalQuery(-2, -1, -1, -1, 0L, 0L, 0L, 0L, 0)), "kind below ANY_KIND");
        check(refuses(() -> new M3ObjectSignalQuery(-1, -2, -1, -1, 0L, 0L, 0L, 0L, 0)), "domain below ANY_DOMAIN");
        check(refuses(() -> new M3ObjectSignalQuery(-1, -1, 5, 4, 0L, 0L, 0L, 0L, 0)), "max < min");
        check(refuses(() -> new M3ObjectSignalQuery(-1, -1, -2, -1, 0L, 0L, 0L, 0L, 0)), "length below UNBOUNDED");
        check(refuses(() -> new M3ObjectSignal(KINDS[0], -1, 0, 0L, 0L, 0L, 0)), "negative domain");
        check(refuses(() -> new M3ObjectSignal(KINDS[0], 0, -2, 0L, 0L, 0L, 0)), "length below -1");
        check(refuses(() -> new M3ObjectSignal(KINDS[0], 0, -1, 0L, 0L, 0L, M3ObjectSignal.HAS_LENGTH)),
            "HAS_LENGTH without length");
        check(refuses(() -> M3ObjectProjectors.byString(-1, value -> "")), "projector domain");
        M3ObjectSignalTable table = M3ObjectSignalTable.build(List.of("a", "b"), M3ObjectProjectors.byString(v -> v),
            M3PoolProgressMonitor.none());
        check(refuses(() -> table.firstPass(M3ObjectSignalQuery.anyMask(0L, 0L), M3ObjectSignalBitmap.all(3),
            M3PoolProgressMonitor.none())), "candidate size mismatch");
        check(refuses(() -> table.signalAt(2)) && refuses(() -> table.featureBitsAt(-1)), "row bounds");
        check(refuses(() -> M3ObjectSignalOps.applyFeatureMasks(new long[64], 2, 0L, 0L, new long[2])),
            "plane shape");
    }

    private void monitors() {
        long[] events = new long[3];
        M3PoolProgressMonitor counting = new M3PoolProgressMonitor() {
            @Override public void begin(String task, long totalWork) { events[0] += totalWork; }
            @Override public void worked(long work) { events[1] += work; }
            @Override public void done() { events[2]++; }
        };
        List<String> values = new ArrayList<>();
        for (int i = 0; i < 300; i++) values.add("v" + i);
        M3ObjectSignalTable table = M3ObjectSignalTable.build(values, M3ObjectProjectors.byString(v -> v), counting);
        check(events[0] == 300 && events[1] == 300 && events[2] == 1, "build progress");
        table.firstPass(M3ObjectSignalQuery.anyMask(0L, 0L), counting);
        check(events[0] == 305 && events[1] == 305 && events[2] == 2, "first-pass progress (one unit per word)");
        long[] worked = {0};
        M3PoolProgressMonitor cancelling = new M3PoolProgressMonitor() {
            @Override public void worked(long work) { worked[0] += work; }
            @Override public boolean cancelled() { return worked[0] >= 17; }
        };
        boolean cancelled = false;
        try {
            M3ObjectSignalTable.build(values, M3ObjectProjectors.byString(v -> v), cancelling);
        } catch (CancellationException expected) {
            cancelled = worked[0] == 17;
        }
        check(cancelled, "build cancellation after 17 rows");
    }
}
