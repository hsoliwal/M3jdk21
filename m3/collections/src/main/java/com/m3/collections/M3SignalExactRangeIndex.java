/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Exact typed range selection over existing immutable M3 signal columns.
 *
 * <p>The retained index is only one primitive {@code int[]} of physical row coordinates,
 * plus a primitive missing-row lane. The existing M3SignalColumns owner retains the
 * original int/long/double values and validity masks. No Entry, Node, boxed value,
 * secondary object payload, or duplicated value column is retained.
 *
 * <p>Queries return rows in ascending <em>typed coordinate</em> order, with equal
 * coordinates ordered by physical row ID. {@code Double.compare} supplies total
 * ordering, including distinct signed zeros and NaN. This is an exact query about
 * stored coordinates only: a heuristic signal does not certify semantic equality,
 * behavioral substitutability, or a sound distance bound.
 *
 * <p>One complete source epoch is captured during construction. Queries reject any
 * intervening epoch change, including unrelated changes (conservative by design),
 * and require both source version and complete SHA-256. Callers must notify the
 * shared epoch owner about changes and recheck optimistic results before publishing.
 */
public final class M3SignalExactRangeIndex {
    private final M3SignalQualitySchema schema;
    private final M3SignalColumns values;
    private final M3SignalDependencyEpochs epochs;
    private final M3SignalColumns.Kind kind;
    private final int dimension;
    private final long builtEpoch;
    private final int[] ordered;
    private final int[] missing;

    private M3SignalExactRangeIndex(M3SignalQualitySchema schema, int dimension) {
        this.schema = Objects.requireNonNull(schema, "schema");
        this.kind = schema.kind(dimension);
        this.dimension = dimension;
        this.values = schema.valueColumnsOwner();
        this.epochs = schema.invalidationEpochsOwner();
        long firstEpoch = epochs.epoch();
        int rows = schema.rowCount();
        int present = 0;
        for (int row = 0; row < rows; row++) {
            if (schema.validAt(dimension, row)) present++;
        }
        int[] sorted = new int[present];
        int[] absent = new int[rows - present];
        int p = 0, m = 0;
        for (int row = 0; row < rows; row++) {
            if (schema.validAt(dimension, row)) sorted[p++] = row;
            else absent[m++] = row;
        }
        sortStable(sorted);
        if (epochs.epoch() != firstEpoch) {
            throw new IllegalStateException("signals changed while indexing");
        }
        this.builtEpoch = firstEpoch;
        this.ordered = sorted;
        this.missing = absent;
    }

    /** Build over the schema's ORIGINAL primitive lane owner and epoch owner. */
    public static M3SignalExactRangeIndex build(M3SignalQualitySchema schema, int dimension) {
        return new M3SignalExactRangeIndex(schema, dimension);
    }

    public M3SignalColumns.Kind kind() { return kind; }
    public int dimension() { return dimension; }
    public int presentCount() { return ordered.length; }
    public int missingCount() { return missing.length; }

    /** Exact physical row IDs with missing coordinates, ascending by physical row ID. */
    public int[] missingRows(long version, String sha256) {
        requireFresh(version, sha256);
        int[] rows = missing.clone();
        requireFresh(version, sha256);
        return rows;
    }

    /** Entire present index, ordered by the exact typed coordinate then physical row ID. */
    public int[] presentRows(long version, String sha256) {
        requireFresh(version, sha256);
        int[] rows = ordered.clone();
        requireFresh(version, sha256);
        return rows;
    }

    /** Inclusive signed integer range; does not convert through double. */
    public int[] betweenInts(int lower, int upper, long version, String sha256) {
        requireKind(M3SignalColumns.Kind.INTEGER);
        if (lower > upper) throw new IllegalArgumentException("inverted int interval");
        requireFresh(version, sha256);
        int start = lowerInt(lower);
        int end = upperInt(upper);
        return checkedSlice(start, end, version, sha256);
    }

    /** Inclusive full-width signed long range, safe beyond the exact-double limit. */
    public int[] betweenLongs(long lower, long upper, long version, String sha256) {
        requireKind(M3SignalColumns.Kind.LONG);
        if (lower > upper) throw new IllegalArgumentException("inverted long interval");
        requireFresh(version, sha256);
        int start = lowerLong(lower);
        int end = upperLong(upper);
        return checkedSlice(start, end, version, sha256);
    }

    /** Inclusive Double.compare total-order range; NaN and signed zero are distinct keys. */
    public int[] betweenDoubles(double lower, double upper, long version, String sha256) {
        requireKind(M3SignalColumns.Kind.DOUBLE);
        if (Double.compare(lower, upper) > 0) {
            throw new IllegalArgumentException("inverted double-total-order interval");
        }
        requireFresh(version, sha256);
        int start = lowerDouble(lower);
        int end = upperDouble(upper);
        return checkedSlice(start, end, version, sha256);
    }

    private void requireKind(M3SignalColumns.Kind expected) {
        if (kind != expected) throw new IllegalArgumentException("wrong typed signal interval");
    }

    private void requireFresh(long version, String digest) {
        schema.requireCurrent(version, digest);
        if (epochs.epoch() != builtEpoch) {
            throw new IllegalStateException("signal index stale; rebuild after source change");
        }
    }

    private int[] checkedSlice(int begin, int end, long version, String digest) {
        int[] found = Arrays.copyOfRange(ordered, begin, end);
        requireFresh(version, digest);
        return found;
    }

    private int lowerInt(int value) {
        int low = 0, high = ordered.length;
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            if (Integer.compare(values.exactIntAt(dimension, ordered[mid]), value) < 0) {
                low = mid + 1;
            } else high = mid;
        }
        return low;
    }
    private int upperInt(int value) {
        int low = 0, high = ordered.length;
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            if (Integer.compare(values.exactIntAt(dimension, ordered[mid]), value) <= 0) {
                low = mid + 1;
            } else high = mid;
        }
        return low;
    }
    private int lowerLong(long value) {
        int low = 0, high = ordered.length;
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            if (Long.compare(values.exactLongAt(dimension, ordered[mid]), value) < 0) {
                low = mid + 1;
            } else high = mid;
        }
        return low;
    }
    private int upperLong(long value) {
        int low = 0, high = ordered.length;
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            if (Long.compare(values.exactLongAt(dimension, ordered[mid]), value) <= 0) {
                low = mid + 1;
            } else high = mid;
        }
        return low;
    }
    private int lowerDouble(double value) {
        int low = 0, high = ordered.length;
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            if (Double.compare(values.exactDoubleAt(dimension, ordered[mid]), value) < 0) {
                low = mid + 1;
            } else high = mid;
        }
        return low;
    }
    private int upperDouble(double value) {
        int low = 0, high = ordered.length;
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            if (Double.compare(values.exactDoubleAt(dimension, ordered[mid]), value) <= 0) {
                low = mid + 1;
            } else high = mid;
        }
        return low;
    }

    /** Iterative stable mergesort on physical row IDs with no Integer boxing. */
    private void sortStable(int[] rows) {
        int length = rows.length;
        if (length < 2) return;
        int[] source = rows;
        int[] destination = new int[length];
        for (int width = 1; width < length;
                width = width >= length - width ? length : width << 1) {
            for (int start = 0; start < length;) {
                int middle = start + Math.min(width, length - start);
                int finish = middle + Math.min(width, length - middle);
                int left = start, right = middle, cursor = start;
                while (left < middle && right < finish) {
                    if (compareRows(source[left], source[right]) <= 0) {
                        destination[cursor++] = source[left++];
                    } else {
                        destination[cursor++] = source[right++];
                    }
                }
                while (left < middle) destination[cursor++] = source[left++];
                while (right < finish) destination[cursor++] = source[right++];
                start = finish;
            }
            int[] swap = source;
            source = destination;
            destination = swap;
        }
        if (source != rows) System.arraycopy(source, 0, rows, 0, length);
    }

    private int compareRows(int left, int right) {
        int sign = switch (kind) {
            case INTEGER -> Integer.compare(values.exactIntAt(dimension, left),
                    values.exactIntAt(dimension, right));
            case LONG -> Long.compare(values.exactLongAt(dimension, left),
                    values.exactLongAt(dimension, right));
            case DOUBLE -> Double.compare(values.exactDoubleAt(dimension, left),
                    values.exactDoubleAt(dimension, right));
        };
        return sign == 0 ? Integer.compare(left, right) : sign;
    }
}
