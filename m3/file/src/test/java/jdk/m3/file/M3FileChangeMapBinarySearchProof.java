// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.file;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Independent linear-scan reference against the live {@link M3FileEditBatch.ChangeMap} queries.
 * Port of the donor oracle (hsoliwal/com.synexia 4a2e6452a7a, crate
 * m3-file-changemap-lower-bound-20261029, proof blob 6c38591926d2): same seed, same 50,001 maps,
 * same 9,341,740 comparisons. The candidate side is the real ChangeMap built through
 * {@code M3FileEditBatch.replaceBytes/apply} instead of an inlined copy of the lower bound, and
 * every answer feeds one SHA-256 digest so two builds of the owner (receiver, donor postimage,
 * preimage) can be compared byte for byte on the printed line.
 */
public final class M3FileChangeMapBinarySearchProof {
    private static final long SEED = 0x4d33464c5f434d41L;
    private static final int IMAGES = 50_000;
    private static final int MAX_OLD_LENGTH = 96;
    private static final int MAX_EDITS = 24;
    private static final int MAX_INSERTION = 9;
    private static final int PARITY_STRIDE_MASK = 1023;
    private static final int DIGEST_BUFFER = 1 << 16;

    private record MapImage(int oldLength, int newLength, int[] oldStart, int[] oldEnd,
                            int[] newStart, int[] newEnd, M3FileEditBatch.ChangeMap map) { }

    /** Buffered SHA-256 sink over the answer stream. */
    private static final class Digest {
        private final MessageDigest engine;
        private final byte[] buffer = new byte[DIGEST_BUFFER];
        private int used;

        Digest() {
            try { engine = MessageDigest.getInstance("SHA-256"); }
            catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
        }
        void putInt(int value) {
            if (used == buffer.length) flush();
            buffer[used++] = (byte) (value >>> 24);
            buffer[used++] = (byte) (value >>> 16);
            buffer[used++] = (byte) (value >>> 8);
            buffer[used++] = (byte) value;
        }
        void putText(String value) {
            flush();
            engine.update(value.getBytes(StandardCharsets.UTF_8));
        }
        private void flush() {
            engine.update(buffer, 0, used);
            used = 0;
        }
        String hex() {
            flush();
            return HexFormat.of().formatHex(engine.digest());
        }
    }

    private M3FileChangeMapBinarySearchProof() { }

    /** The ordered scan the recipe replaces, kept verbatim as the reference. */
    private static int linear(int position, M3FileEditBatch.Affinity affinity, int length,
                              int[] start, int[] end, int[] targetStart, int[] targetEnd) {
        Objects.checkIndex(position, length + 1);
        Objects.requireNonNull(affinity, "affinity");
        int correction = 0;
        for (int i = 0; i < start.length; i++) {
            if (position < start[i]) break;
            if (position == start[i]) {
                return affinity == M3FileEditBatch.Affinity.LEFT ? targetStart[i] : targetEnd[i];
            }
            if (position < end[i]) return -1;
            correction = targetEnd[i] - end[i];
        }
        return Math.addExact(position, correction);
    }

    /** Builds the live ChangeMap from {start, end, insertion} edits and checks its geometry. */
    private static MapImage image(int oldLength, List<int[]> edits) {
        int n = edits.size();
        int[] os = new int[n], oe = new int[n], ns = new int[n], ne = new int[n];
        int oldCursor = 0, newCursor = 0;
        M3FileEditBatch batch = M3FileEditBatch.on(M3FileSource.fromBytes(new byte[oldLength]));
        for (int i = 0; i < n; i++) {
            int[] edit = edits.get(i);
            newCursor += edit[0] - oldCursor;
            os[i] = edit[0];
            oe[i] = edit[1];
            ns[i] = newCursor;
            newCursor += edit[2];
            ne[i] = newCursor;
            oldCursor = edit[1];
            batch.replaceBytes(edit[0], edit[1], new byte[edit[2]]);
        }
        int newLength = newCursor + oldLength - oldCursor;
        M3FileEditBatch.ChangeMap map = batch.apply().map();
        if (map.changes() != n || map.oldLength() != oldLength || map.newLength() != newLength) {
            throw new AssertionError("change map geometry differs from the generated edits");
        }
        for (int i = 0; i < n; i++) {
            if (map.oldStart(i) != os[i] || map.oldEnd(i) != oe[i]
                    || map.newStart(i) != ns[i] || map.newEnd(i) != ne[i]) {
                throw new AssertionError("change map coordinates differ from the generated edits");
            }
        }
        return new MapImage(oldLength, newLength, os, oe, ns, ne, map);
    }

    private static MapImage randomImage(Random random) {
        int oldLength = random.nextInt(MAX_OLD_LENGTH);
        List<int[]> edits = new ArrayList<>();
        int lastStart = -1, lastEnd = 0;
        for (int i = 0, limit = random.nextInt(MAX_EDITS); i < limit; i++) {
            int minimum = Math.max(lastStart + 1, lastEnd);
            if (minimum > oldLength) break;
            int start = minimum + random.nextInt(oldLength - minimum + 1);
            int end = start + random.nextInt(oldLength - start + 1);
            int insertion = random.nextInt(MAX_INSERTION);
            edits.add(new int[] {start, end, insertion});
            lastStart = start;
            lastEnd = end;
        }
        return image(oldLength, edits);
    }

    private static void check(MapImage image, long[] count, Digest digest) {
        int[] oldStart = image.oldStart(), oldEnd = image.oldEnd();
        int[] newStart = image.newStart(), newEnd = image.newEnd();
        for (int i = 1; i < oldStart.length; i++) {
            if (oldStart[i] <= oldStart[i - 1] || oldStart[i] < oldEnd[i - 1]
                    || newStart[i] < newStart[i - 1]) {
                throw new AssertionError("invalid generated disjoint edit map");
            }
        }
        for (M3FileEditBatch.Affinity affinity : M3FileEditBatch.Affinity.values()) {
            for (int p = 0; p <= image.oldLength(); p++) {
                int expected = linear(p, affinity, image.oldLength(), oldStart, oldEnd, newStart, newEnd);
                record(digest, 0, p, affinity, expected, image.map().oldToNew(p, affinity), oldStart);
                count[0]++;
            }
            for (int p = 0; p <= image.newLength(); p++) {
                int expected = linear(p, affinity, image.newLength(), newStart, newEnd, oldStart, oldEnd);
                record(digest, 1, p, affinity, expected, image.map().newToOld(p, affinity), newStart);
                count[0]++;
            }
        }
    }

    private static void record(Digest digest, int direction, int position,
                               M3FileEditBatch.Affinity affinity, int expected, int actual, int[] starts) {
        if (expected != actual) {
            throw new AssertionError((direction == 0 ? "oldToNew" : "newToOld") + " p=" + position
                    + " affinity=" + affinity + " expected=" + expected + " actual=" + actual
                    + " starts=" + Arrays.toString(starts));
        }
        digest.putInt(direction);
        digest.putInt(position);
        digest.putInt(affinity.ordinal());
        digest.putInt(actual);
    }

    private static void exceptionParity(MapImage image, Digest digest) {
        M3FileEditBatch.Affinity left = M3FileEditBatch.Affinity.LEFT;
        for (int invalid : new int[] {-1, image.oldLength() + 1}) {
            sameFailure(digest, () -> linear(invalid, left, image.oldLength(), image.oldStart(),
                    image.oldEnd(), image.newStart(), image.newEnd()),
                    () -> image.map().oldToNew(invalid, left));
        }
        for (int invalid : new int[] {-1, image.newLength() + 1}) {
            sameFailure(digest, () -> linear(invalid, left, image.newLength(), image.newStart(),
                    image.newEnd(), image.oldStart(), image.oldEnd()),
                    () -> image.map().newToOld(invalid, left));
        }
        sameFailure(digest, () -> linear(0, null, image.oldLength(), image.oldStart(),
                image.oldEnd(), image.newStart(), image.newEnd()),
                () -> image.map().oldToNew(0, null));
    }

    private static void sameFailure(Digest digest, Runnable reference, Runnable live) {
        Throwable first = failure(reference), second = failure(live);
        if (first == null || second == null || first.getClass() != second.getClass()) {
            throw new AssertionError("exception class parity");
        }
        digest.putText(second.getClass().getName());
    }

    private static Throwable failure(Runnable operation) {
        try {
            operation.run();
            return null;
        } catch (RuntimeException error) {
            return error;
        }
    }

    public static void main(String[] args) {
        long[] assertions = {0};
        Digest digest = new Digest();
        MapImage collapsed = image(8, List.of(new int[] {0, 2, 0}, new int[] {2, 4, 0}));
        check(collapsed, assertions, digest);
        if (collapsed.map().newToOld(0, M3FileEditBatch.Affinity.RIGHT) != 2) {
            throw new AssertionError("duplicate newStart must choose first edit");
        }
        Random random = new Random(SEED);
        for (int i = 0; i < IMAGES; i++) {
            MapImage image = randomImage(random);
            check(image, assertions, digest);
            if ((i & PARITY_STRIDE_MASK) == 0) exceptionParity(image, digest);
        }
        System.out.println("PASS M3_FILE_CHANGEMAP_LOWER_BOUND_V1 maps=" + (IMAGES + 1)
                + " checks=" + assertions[0] + " digest=" + digest.hex());
    }
}
