/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable, typed, column-major M3 signal coordinates. Canonical objects stay in their
 * original owner; only source row indices and primitive facts are retained.
 * Missing values use validity bits, never an invented numeric sentinel.
 *
 * Static/compiler-known, domain, historical and runtime-profiled facts have separate provenance.
 * Numeric separation is diagnostic, not a semantic-equivalence proof.
 */
public final class M3SignalColumns {
    public enum Kind { INTEGER, LONG, DOUBLE }
    public enum Origin { STATIC, PROFILED, DOMAIN, HISTORY }

    public record Column(Kind kind, Origin origin, long evidenceId, boolean descending,
            long[] validWords, int[] integers, long[] longs, double[] doubles) {
        public Column {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(origin, "origin");
            Objects.requireNonNull(validWords, "validWords");
            int supplied = (integers != null ? 1 : 0) + (longs != null ? 1 : 0)
                    + (doubles != null ? 1 : 0);
            if (supplied != 1 || (kind == Kind.INTEGER) != (integers != null)
                    || (kind == Kind.LONG) != (longs != null)
                    || (kind == Kind.DOUBLE) != (doubles != null)) {
                throw new IllegalArgumentException("exactly one matching primitive lane required");
            }
            validWords = validWords.clone();
            if (integers != null) integers = integers.clone();
            if (longs != null) longs = longs.clone();
            if (doubles != null) doubles = doubles.clone();
        }
        @Override public long[] validWords() { return validWords.clone(); }
        @Override public int[] integers() { return integers == null ? null : integers.clone(); }
        @Override public long[] longs() { return longs == null ? null : longs.clone(); }
        @Override public double[] doubles() { return doubles == null ? null : doubles.clone(); }

        private int length() {
            return switch (kind) {
                case INTEGER -> integers.length;
                case LONG -> longs.length;
                case DOUBLE -> doubles.length;
            };
        }
        private boolean present(int row) {
            return (validWords[row >>> 6] & (1L << (row & 63))) != 0;
        }
        private int compare(int a, int b) {
            return switch (kind) {
                case INTEGER -> Integer.compare(integers[a], integers[b]);
                case LONG -> Long.compare(longs[a], longs[b]);
                case DOUBLE -> Double.compare(doubles[a], doubles[b]);
            };
        }
        private double gap(int a, int b) {
            if (!present(a) || !present(b)) return Double.NaN;
            return switch (kind) {
                case INTEGER -> Math.abs((double) integers[a] - integers[b]);
                case LONG -> {
                    double value = Math.abs((double) longs[a] - (double) longs[b]);
                    yield longs[a] != longs[b] && value == 0.0 ? Double.NaN : value;
                }
                case DOUBLE -> {
                    double x = doubles[a], y = doubles[b], difference = Math.abs(x - y);
                    yield Double.isFinite(x) && Double.isFinite(y)
                            && Double.isFinite(difference) ? difference : Double.NaN;
                }
            };
        }
    }

    public record Comparison(int sign, int decisiveColumn, double numericSeparation,
            boolean exactCoordinateComparison, long evidenceId) {
        public Comparison {
            if (sign < -1 || sign > 1 || decisiveColumn < -1
                    || (sign == 0) != (decisiveColumn == -1)) {
                throw new IllegalArgumentException("invalid comparison");
            }
        }
    }

    private final int rows;
    private final Column[] columns;

    public M3SignalColumns(int rows, Column... columns) {
        if (rows < 0) throw new IllegalArgumentException("negative row count");
        Objects.requireNonNull(columns, "columns");
        if (columns.length == 0) throw new IllegalArgumentException("no columns");
        this.rows = rows;
        this.columns = columns.clone();
        int words = (int) (((long) rows + 63L) >>> 6);
        for (int index = 0; index < this.columns.length; index++) {
            Column c = Objects.requireNonNull(this.columns[index], "column[" + index + "]");
            if (c.length() != rows || c.validWords.length != words) {
                throw new IllegalArgumentException("row/validity alignment");
            }
            if (words != 0 && rows % 64 != 0
                    && (c.validWords[words - 1] >>> (rows & 63)) != 0L) {
                throw new IllegalArgumentException("validity bits exceed row count");
            }
        }
    }

    public int rowCount() { return rows; }
    public int columnCount() { return columns.length; }
    private Column column(int index) { return columns[Objects.checkIndex(index, columns.length)]; }
    public Column snapshotColumn(int index) { return column(index); }
    public Kind kind(int index) { return column(index).kind; }
    public Origin origin(int index) { return column(index).origin; }
    public long evidenceId(int index) { return column(index).evidenceId; }
    public boolean validAt(int column, int row) {
        Objects.checkIndex(row, rows);
        return column(column).present(row);
    }

    /**
     * Source-validated numeric coordinate for the *existing* progressive L1 engine.
     * Reject missing/nonfinite facts and long values whose conversion to double could
     * silently change the declared metric. Ordinal sorting continues to use exact
     * int/long/Double.compare through orderAt and never calls this method.
     *
     * <p>Package-private: no public JDK or M3 caller contract is changed.</p>
     */
    double finiteMetricAt(int dimension, int row) {
        Column c = column(dimension);
        Objects.checkIndex(row, rows);
        if (!c.present(row)) {
            throw new IllegalArgumentException("missing numeric metric coordinate");
        }
        double value = switch (c.kind) {
            case INTEGER -> c.integers[row];
            case LONG -> {
                long number = c.longs[row];
                if (number < -(1L << 53) || number > (1L << 53)) {
                    throw new IllegalArgumentException("long metric loses 64-bit precision");
                }
                yield (double) number;
            }
            case DOUBLE -> c.doubles[row];
        };
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("nonfinite metric coordinate");
        }
        return value;
    }

    /** Exact typed ordinal order; missing coordinates are always last. */
    public Comparison compare(int left, int right) {
        Objects.checkIndex(left, rows);
        Objects.checkIndex(right, rows);
        for (int i = 0; i < columns.length; i++) {
            Column c = columns[i];
            boolean a = c.present(left), b = c.present(right);
            int sign = a == b ? (a ? c.compare(left, right) : 0) : (a ? -1 : 1);
            if (sign != 0) {
                if (a && b && c.descending) sign = -Integer.signum(sign);
                return new Comparison(Integer.signum(sign), i, c.gap(left, right),
                        a && b, c.evidenceId);
            }
        }
        return new Comparison(0, -1, Double.NaN, true, 0L);
    }

    /** One typed domain coordinate comparison; no diagnostic record or heap Entry allocation. */
    int orderAt(int dimension, int left, int right) {
        Column column = columns[Objects.checkIndex(dimension, columns.length)];
        Objects.checkIndex(left, rows);
        Objects.checkIndex(right, rows);
        boolean a = column.present(left), b = column.present(right);
        if (a != b) return a ? -1 : 1;
        if (!a) return 0;
        int sign = Integer.signum(column.compare(left, right));
        return column.descending ? -sign : sign;
    }

    /** Private stable sorter now compares primitive signs without per-pair objects. */
    int orderOnly(int left, int right) {
        Objects.checkIndex(left, rows);
        Objects.checkIndex(right, rows);
        for (int dimension = 0; dimension < columns.length; dimension++) {
            int sign = orderAt(dimension, left, right);
            if (sign != 0) return sign;
        }
        return 0;
    }

    /** Stable primitive-index mergesort. Equal coordinates retain selected encounter order. */
    public int[] sortSelected(int[] selectedRows) {
        Objects.requireNonNull(selectedRows, "selectedRows");
        int[] order = selectedRows.clone();
        for (int row : order) Objects.checkIndex(row, rows);
        if (order.length < 2) return order;
        int[] scratch = new int[order.length];
        for (long width = 1; width < order.length; width <<= 1) {
            for (long base = 0; base < order.length; base += width << 1) {
                int lo = (int) base, mid = (int) Math.min(base + width, order.length);
                int hi = (int) Math.min(base + (width << 1), order.length);
                int a = lo, b = mid, out = lo;
                while (a < mid && b < hi) {
                    scratch[out++] = orderOnly(order[a], order[b]) <= 0
                            ? order[a++] : order[b++];
                }
                while (a < mid) scratch[out++] = order[a++];
                while (b < hi) scratch[out++] = order[b++];
            }
            int[] temp = order; order = scratch; scratch = temp;
        }
        return order;
    }

    /** 32-bit physical state planes are independent from per-column fact validity masks. */
    public int[] selectedByState(int[] states, int required, int forbidden) {
        Objects.requireNonNull(states, "states");
        if (states.length != rows) throw new IllegalArgumentException("state length mismatch");
        int count = 0;
        for (int state : states) {
            if ((state & required) == required && (state & forbidden) == 0) count++;
        }
        int[] selected = new int[count];
        for (int i = 0, at = 0; i < rows; i++) {
            int state = states[i];
            if ((state & required) == required && (state & forbidden) == 0) {
                selected[at++] = i;
            }
        }
        return selected;
    }

    public static long[] allValid(int rows) {
        if (rows < 0) throw new IllegalArgumentException("negative rows");
        long[] bits = new long[(int) (((long) rows + 63L) >>> 6)];
        Arrays.fill(bits, -1L);
        if (rows % 64 != 0) bits[bits.length - 1] = (1L << (rows & 63)) - 1L;
        return bits;
    }
}
