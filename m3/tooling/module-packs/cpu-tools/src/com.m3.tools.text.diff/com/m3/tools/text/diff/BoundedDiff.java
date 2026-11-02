// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.tools.text.diff;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Bounded, deterministic shortest insert/delete script over exact UTF-16 code units.
 * A replacement counts as one deletion plus one insertion, not as one substitution.
 * The incumbent {@link TextDelta#changedSpan(String, String)} remains unchanged.
 *
 * <p>One primitive furthest-reaching lane is retained per edit depth; no per-character
 * node graph is retained. The source/target fingerprints encode the actual UTF-16
 * units, including unmatched surrogate halves, without a lossy UTF-8 conversion.
 */
public final class BoundedDiff {
    public static final int MAX_INPUT_UTF16 = 262_144;
    public static final int MAX_EDITS = 512;
    private static final Runnable NOOP = () -> { };

    private BoundedDiff() { }

    /** A replacement of the half-open original range by exact inserted UTF-16. */
    public record Change(int start, int end, String inserted) {
        public Change {
            if (start < 0 || end < start) throw new IllegalArgumentException("invalid edit range");
            Objects.requireNonNull(inserted, "inserted");
            if (start == end && inserted.isEmpty()) throw new IllegalArgumentException("empty edit");
        }
    }

    /**
     * Immutable complete patch or explicit distance-budget miss. An incomplete
     * result has distance -1, no changes, and cannot be applied.
     */
    public static final class Patch {
        private final int sourceLength;
        private final int targetLength;
        private final int distance;
        private final String sourceSha256;
        private final String targetSha256;
        private final List<Change> changes;

        private Patch(int sourceLength, int targetLength, int distance, String sourceSha256,
                      String targetSha256, List<Change> changes) {
            this.sourceLength = sourceLength;
            this.targetLength = targetLength;
            this.distance = distance;
            this.sourceSha256 = sourceSha256;
            this.targetSha256 = targetSha256;
            this.changes = List.copyOf(changes);
        }

        public boolean complete() { return distance >= 0; }
        public int distance() { return distance; }
        public int sourceLength() { return sourceLength; }
        public int targetLength() { return targetLength; }
        public String sourceSha256() { return sourceSha256; }
        public String targetSha256() { return targetSha256; }
        public List<Change> changes() { return changes; }

        /** Applies only to the exact original UTF-16 value; checks the result too. */
        public String apply(String source) {
            Objects.requireNonNull(source, "source");
            if (!complete()) throw new IllegalStateException("edit budget exceeded");
            if (source.length() != sourceLength || !utf16Sha256(source, NOOP).equals(sourceSha256)) {
                throw new IllegalArgumentException("patch source identity mismatch");
            }
            StringBuilder output = new StringBuilder(targetLength);
            int position = 0;
            for (Change change : changes) {
                if (change.start() < position || change.end() > sourceLength) {
                    throw new IllegalStateException("patch edit order/range");
                }
                output.append(source, position, change.start()).append(change.inserted());
                position = change.end();
            }
            output.append(source, position, sourceLength);
            String result = output.toString();
            if (result.length() != targetLength || !utf16Sha256(result, NOOP).equals(targetSha256)) {
                throw new IllegalStateException("patch target identity mismatch");
            }
            return result;
        }
    }

    public static Patch diff(String source, String target, int maxEdits) {
        return diff(source, target, maxEdits, NOOP);
    }

    /**
     * Exact shortest insertion/deletion script when distance is at most maxEdits.
     * A budget miss is not a proof of the actual distance. Checkpoints are
     * cooperative and the caller retains ownership of its cancellation monitor.
     */
    public static Patch diff(String source, String target, int maxEdits, Runnable checkpoint) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(checkpoint, "checkpoint");
        if (maxEdits < 0 || maxEdits > MAX_EDITS) throw new IllegalArgumentException("edit-distance budget");
        if (source.length() > MAX_INPUT_UTF16 || target.length() > MAX_INPUT_UTF16) {
            throw new IllegalArgumentException("UTF-16 input budget");
        }
        checkpoint.run();
        String sourceHash = utf16Sha256(source, checkpoint);
        String targetHash = utf16Sha256(target, checkpoint);
        if (Math.abs(source.length() - target.length()) > maxEdits) {
            return new Patch(source.length(), target.length(), -1, sourceHash, targetHash, List.of());
        }

        int offset = maxEdits + 1;
        int[] previous = new int[2 * maxEdits + 3];
        Arrays.fill(previous, -1);
        previous[offset + 1] = 0;
        List<int[]> trace = new ArrayList<>(maxEdits + 1);
        int observations = 0;
        for (int depth = 0; depth <= maxEdits; depth++) {
            checkpoint.run();
            int[] current = previous.clone();
            for (int diagonal = -depth; diagonal <= depth; diagonal += 2) {
                int lane = offset + diagonal;
                boolean insert = diagonal == -depth || (diagonal != depth
                        && previous[lane - 1] < previous[lane + 1]);
                int x = insert ? previous[lane + 1] : previous[lane - 1] + 1;
                int y = x - diagonal;
                if (x < 0 || y < 0 || x > source.length() || y > target.length()) {
                    current[lane] = -1;
                    continue;
                }
                while (x < source.length() && y < target.length() && source.charAt(x) == target.charAt(y)) {
                    x++;
                    y++;
                    if ((++observations & 8191) == 0) checkpoint.run();
                }
                current[lane] = x;
                if (x == source.length() && y == target.length()) {
                    trace.add(current);
                    List<Change> changes = backtrack(trace, source, target, depth, offset, checkpoint);
                    checkpoint.run();
                    return new Patch(source.length(), target.length(), depth, sourceHash, targetHash, changes);
                }
            }
            trace.add(current);
            previous = current;
        }
        checkpoint.run();
        return new Patch(source.length(), target.length(), -1, sourceHash, targetHash, List.of());
    }

    /** Reconstruct into one flat primitive lane, then project only coalesced API edits. */
    private static List<Change> backtrack(List<int[]> trace, String source, String target, int depth,
                                           int offset, Runnable checkpoint) {
        int[] lane = new int[depth * 4];
        int x = source.length();
        int y = target.length();
        for (int distance = depth; distance > 0; distance--) {
            checkpoint.run();
            int[] previous = trace.get(distance - 1);
            int diagonal = x - y;
            int index = offset + diagonal;
            boolean insert = diagonal == -distance || (diagonal != distance
                    && previous[index - 1] < previous[index + 1]);
            int previousDiagonal = insert ? diagonal + 1 : diagonal - 1;
            int previousX = previous[offset + previousDiagonal];
            int previousY = previousX - previousDiagonal;
            while (x > previousX && y > previousY) {
                x--;
                y--;
            }
            int base = (distance - 1) * 4;
            if (insert) {
                lane[base] = x;
                lane[base + 1] = x;
                lane[base + 2] = --y;
                lane[base + 3] = y + 1;
            } else {
                lane[base] = --x;
                lane[base + 1] = x + 1;
                lane[base + 2] = y;
                lane[base + 3] = y;
            }
        }
        List<Change> result = new ArrayList<>();
        for (int step = 0; step < depth;) {
            int base = step * 4;
            int start = lane[base];
            int end = lane[base + 1];
            int insertedStart = lane[base + 2];
            int insertedEnd = lane[base + 3];
            step++;
            while (step < depth && lane[step * 4] == end && lane[step * 4 + 2] == insertedEnd) {
                end = lane[step * 4 + 1];
                insertedEnd = lane[step * 4 + 3];
                step++;
            }
            result.add(new Change(start, end, target.substring(insertedStart, insertedEnd)));
        }
        return List.copyOf(result);
    }

    /** Length-framed big-endian UTF-16 units, including unmatched surrogates. */
    private static String utf16Sha256(String text, Runnable checkpoint) {
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
        digest.update("m3-bounded-diff-utf16-v1".getBytes(StandardCharsets.US_ASCII));
        int length = text.length();
        digest.update((byte) (length >>> 24));
        digest.update((byte) (length >>> 16));
        digest.update((byte) (length >>> 8));
        digest.update((byte) length);
        for (int index = 0; index < length; index++) {
            if ((index & 8191) == 0) checkpoint.run();
            char unit = text.charAt(index);
            digest.update((byte) (unit >>> 8));
            digest.update((byte) unit);
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
