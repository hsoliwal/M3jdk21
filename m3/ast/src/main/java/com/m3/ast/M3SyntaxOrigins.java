// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ast;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Multi-origin source correspondences for generated, macro-expanded, embedded,
 * or preprocessed syntax. This is an optional, projection-bound *sidecar* over
 * the original M3SyntaxTree's source bytes, not another parser or source store.
 *
 * <p>One generated range can correspond to multiple original ranges, possibly
 * from different source files. Only a unique, explicitly EXACT, equal-byte-
 * length mapping may translate an individual byte point. Non-affine or
 * overlapping mappings remain discoverable but are never guessed into a point.
 * Source registration is an indexed, insertion-stable builder operation; a
 * large number of distinct provenance files does not require a full scan per
 * origin. The immutable sidecar retains the original flat integer columns.
 */
public final class M3SyntaxOrigins {
    public enum Relation { EXACT_BYTES, DERIVED, MACRO_EXPANSION, EMBEDDED_LANGUAGE }

    public record Origin(String sourceId, String sha256, int startByte, int endByte,
                         Relation relation) { }
    private final String projectionSha256;
    private final int generatedByteLength;
    private final String[] sourceIds, sha256s;
    private final int[] sourceIndex, from, to, originalStart, originalEnd, maxEndThrough;
    private final byte[] relation;

    // Builder-only directory entry. Finalized origins retain primitive columns
    // and two parallel identity-string arrays, never per-origin objects.
    private record SourceIdentity(int index, String sha256) { }

    private M3SyntaxOrigins(Builder b) {
        projectionSha256 = b.tree.projectionSha256();
        generatedByteLength = b.tree.sourceLength();
        sourceIds = b.sourceIds.keySet().toArray(String[]::new);
        sha256s = b.sourceIds.values().stream().map(SourceIdentity::sha256)
                .toArray(String[]::new);
        sourceIndex = Arrays.copyOf(b.sourceIndex, b.size);
        from = Arrays.copyOf(b.from, b.size);
        to = Arrays.copyOf(b.to, b.size);
        originalStart = Arrays.copyOf(b.originalStart, b.size);
        originalEnd = Arrays.copyOf(b.originalEnd, b.size);
        relation = Arrays.copyOf(b.relation, b.size);
        maxEndThrough = new int[b.size];
        int max = 0;
        for (int i = 0; i < b.size; i++) {
            max = Math.max(max, to[i]);
            maxEndThrough[i] = max;
        }
    }
    public static Builder builder(M3SyntaxTree tree) { return new Builder(tree); }
    public int count() { return from.length; }
    public String projectionSha256() { return projectionSha256; }
    public void requireProjection(M3SyntaxTree tree) {
        if (!projectionSha256.equals(Objects.requireNonNull(tree, "tree").projectionSha256()))
            throw new IllegalStateException("source origin metadata belongs to another syntax generation");
    }
    public int generatedStart(int index) { return from[Objects.checkIndex(index, count())]; }
    public int generatedEnd(int index) { return to[Objects.checkIndex(index, count())]; }
    public Origin origin(int index) {
        int at = Objects.checkIndex(index, count());
        int source = sourceIndex[at];
        return new Origin(sourceIds[source], sha256s[source], originalStart[at],
                originalEnd[at], Relation.values()[Byte.toUnsignedInt(relation[at])]);
    }

    /** Overlapping macro/template mappings yield every applicable row. */
    public int[] originRowsAt(int generatedByte) {
        Objects.checkIndex(generatedByte, generatedByteLength + 1);
        int right = upperBound(from, generatedByte);
        int[] result = new int[Math.min(4, right)];
        int size = 0;
        for (int i = right - 1; i >= 0 && maxEndThrough[i] > generatedByte; i--) {
            if (generatedByte < to[i]) {
                if (size == result.length) result = Arrays.copyOf(result, size + (size >>> 1) + 1);
                result[size++] = i;
            }
        }
        // Stable source addition order rather than reverse scan order.
        for (int i = 0, j = size - 1; i < j; i++, j--) {
            int swap = result[i]; result[i] = result[j]; result[j] = swap;
        }
        return Arrays.copyOf(result, size);
    }
    /** The only safe point map: unique one-to-one byte translation. */
    public int exactOriginalByteAt(int generatedByte) {
        int[] matches = originRowsAt(generatedByte);
        if (matches.length != 1) return -1;
        int row = matches[0];
        if (relation[row] != (byte) Relation.EXACT_BYTES.ordinal()
                || to[row] - from[row] != originalEnd[row] - originalStart[row]) return -1;
        return Math.addExact(originalStart[row], generatedByte - from[row]);
    }
    private static int upperBound(int[] a, int x) {
        int lo = 0, hi = a.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (a[mid] <= x) lo = mid + 1;
            else hi = mid;
        }
        return lo;
    }
    public static final class Builder {
        private final M3SyntaxTree tree;
        private final Map<String, SourceIdentity> sourceIds = new LinkedHashMap<>();
        private int[] sourceIndex = new int[16], from = new int[16], to = new int[16];
        private int[] originalStart = new int[16], originalEnd = new int[16];
        private byte[] relation = new byte[16];
        private int size;
        private boolean frozen;
        private Builder(M3SyntaxTree tree) { this.tree = Objects.requireNonNull(tree, "tree"); }
        public Builder add(int generatedStart, int generatedEnd, String sourceId,
                           String sourceSha256, int sourceStart, int sourceEnd,
                           Relation relation) {
            if (frozen) throw new IllegalStateException("source origin sidecar already built");
            Objects.requireNonNull(sourceId, "sourceId");
            Objects.requireNonNull(sourceSha256, "sourceSha256");
            Objects.requireNonNull(relation, "relation");
            if (sourceId.isBlank() || sourceId.length() > 2048 || sourceId.contains("\0")
                    || !sourceSha256.matches("[0-9a-f]{64}"))
                throw new IllegalArgumentException("invalid source identity");
            if (relation == Relation.EXACT_BYTES
                    && generatedEnd - generatedStart != sourceEnd - sourceStart)
                throw new IllegalArgumentException("exact source bytes require equal range widths");
            if (generatedStart < 0 || generatedEnd <= generatedStart
                    || generatedEnd > tree.sourceLength()
                    || sourceStart < 0 || sourceEnd < sourceStart
                    || (size > 0 && generatedStart < from[size - 1]))
                throw new IllegalArgumentException("invalid or unordered origin coordinates");
            SourceIdentity known = sourceIds.get(sourceId);
            if (known != null && !known.sha256().equals(sourceSha256))
                throw new IllegalArgumentException("conflicting content generation for source id");
            // Complete capacity/validation checks before admitting a source ID.
            if (size == from.length) grow();
            if (known == null) {
                known = new SourceIdentity(sourceIds.size(), sourceSha256);
                sourceIds.put(sourceId, known);
            }
            sourceIndex[size] = known.index();
            from[size] = generatedStart; to[size] = generatedEnd;
            originalStart[size] = sourceStart; originalEnd[size] = sourceEnd;
            this.relation[size] = (byte) relation.ordinal();
            size++;
            return this;
        }
        public M3SyntaxOrigins freeze() {
            if (frozen) throw new IllegalStateException("source origins already frozen");
            frozen = true;
            return new M3SyntaxOrigins(this);
        }
        private void grow() {
            int next = Math.addExact(size, (size >>> 1) + 1);
            sourceIndex = Arrays.copyOf(sourceIndex, next);
            from = Arrays.copyOf(from, next); to = Arrays.copyOf(to, next);
            originalStart = Arrays.copyOf(originalStart, next);
            originalEnd = Arrays.copyOf(originalEnd, next);
            relation = Arrays.copyOf(relation, next);
        }
    }
}
