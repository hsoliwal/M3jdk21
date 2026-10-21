// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.collections;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntBinaryOperator;

/**
 * Caller-owned, progressively refined evidence over precomputed signal lanes.
 *
 * <p>This is an evidence consumer, not another sorter or a canonical-object store. The source
 * reader must describe an immutable snapshot for the lifetime of a plan. For example, it can read
 * existing versioned MatSignalTable lanes. Exactly one pair is cached in a reusable workspace;
 * there is no global all-pairs table, per-row wrapper, or comparison callback in radix sorting.
 * The caller explicitly begins another pair when its earlier evidence is no longer needed.</p>
 */
public final class M3ProgressiveSignalEvidence {
    public enum Family {
        IDENTITY, LEXICAL, STRUCTURAL, FIELDS, METHODS, DEPENDENCIES, GRAPH, SEMANTIC,
        DISTANCE, STATISTICAL, TEMPORAL, MEMORY, RUNTIME, COLLECTION, COMPILER,
        BEHAVIORAL, RELIABILITY, DOMAIN
    }

    public enum Provenance { SOURCE_TIME, DOMAIN, RUNTIME, MEASURED }

    /** Reads one precomputed primitive coordinate, not an object or transient Entry wrapper. */
    @FunctionalInterface
    public interface Reader {
        double valueAt(int dimension, int row);
    }

    /**
     * Declares a dimension's roles independently of its family. A metric coordinate means that
     * its weighted absolute difference contributes to a caller-declared L1 distance; it does not
     * by itself prove that this distance is a metric over the user's complete domain objects.
     * A finite maximumGap is a caller-supplied certificate, never inferred from a double value.
     */
    public record Dimension(
            String name, Family family, Provenance provenance, String sourceSymbol,
            boolean ordered, boolean exactOrdinal, boolean descending,
            double metricWeight, double maximumGap) {
        public Dimension {
            name = Objects.requireNonNull(name, "name");
            if (name.isBlank()) throw new IllegalArgumentException("blank dimension name");
            family = Objects.requireNonNull(family, "family");
            provenance = Objects.requireNonNull(provenance, "provenance");
            sourceSymbol = Objects.requireNonNull(sourceSymbol, "sourceSymbol");
            if (sourceSymbol.isBlank()) throw new IllegalArgumentException("blank provenance symbol");
            if (exactOrdinal && !ordered) {
                throw new IllegalArgumentException("exact ordinal must participate in order");
            }
            if (!Double.isFinite(metricWeight) || metricWeight < 0.0) {
                throw new IllegalArgumentException("invalid metric weight");
            }
            if (metricWeight == 0.0) {
                if (!Double.isNaN(maximumGap)) {
                    throw new IllegalArgumentException("nonmetric dimension must have unknown maximum gap");
                }
            } else if (!Double.isNaN(maximumGap)
                    && (!Double.isFinite(maximumGap) || maximumGap < 0.0)) {
                throw new IllegalArgumentException("invalid certified maximum gap");
            }
        }
    }

    /** Fixed schema and rows; retains only O(dimensions) metadata, not the signal payload. */
    public static final class Plan {
        private final int rows;
        private final long version;
        private final Dimension[] dimensions;
        private final Reader reader;
        private final boolean completeExactSchema;
        private final int metricDimensions;

        private Plan(int rows, long version, Dimension[] descriptors,
                Reader reader, boolean completeExactSchema) {
            if (rows < 0 || version < 0) throw new IllegalArgumentException("rows/version");
            this.rows = rows;
            this.version = version;
            this.reader = Objects.requireNonNull(reader, "reader");
            this.dimensions = Objects.requireNonNull(descriptors, "descriptors").clone();
            if (dimensions.length == 0) throw new IllegalArgumentException("empty schema");
            Set<String> names = new HashSet<>();
            boolean ordered = false;
            boolean allExact = true;
            int metrics = 0;
            for (int i = 0; i < dimensions.length; i++) {
                Dimension d = Objects.requireNonNull(dimensions[i], "dimensions[" + i + "]");
                if (!names.add(d.name())) throw new IllegalArgumentException("duplicate dimension");
                if (d.ordered()) {
                    ordered = true;
                    allExact &= d.exactOrdinal();
                }
                if (d.metricWeight() > 0.0) metrics++;
            }
            if (completeExactSchema && (!ordered || !allExact)) {
                throw new IllegalArgumentException("incomplete or heuristic exact schema");
            }
            this.completeExactSchema = completeExactSchema;
            this.metricDimensions = metrics;
        }

        public static Plan of(int rows, long version, Dimension[] descriptors,
                Reader reader, boolean completeExactSchema) {
            return new Plan(rows, version, descriptors, reader, completeExactSchema);
        }

        public Workspace workspace() { return new Workspace(this); }
        public int rows() { return rows; }
        public int dimensions() { return dimensions.length; }
        public long version() { return version; }
        public Dimension dimension(int index) {
            return dimensions[Objects.checkIndex(index, dimensions.length)];
        }
    }

    /** One mutable comparison context, owned exclusively by the caller or one operation. */
    public static final class Workspace {
        private final Plan plan;
        private final long[] evaluated;
        private final byte[] signs;
        private final double[] gaps;
        private int left = -1;
        private int right = -1;
        private boolean active;
        private boolean verified;
        private int verifiedSign;

        private Workspace(Plan plan) {
            this.plan = plan;
            int count = plan.dimensions.length;
            this.evaluated = new long[(count >>> 6) + ((count & 63) == 0 ? 0 : 1)];
            this.signs = new byte[count];
            this.gaps = new double[count];
            Arrays.fill(gaps, Double.NaN);
        }

        /** Explicitly discards the previous pair. Rebeginning the same pair reuses all evidence. */
        public Workspace begin(int leftRow, int rightRow) {
            Objects.checkIndex(leftRow, plan.rows);
            Objects.checkIndex(rightRow, plan.rows);
            if (active && left == leftRow && right == rightRow) return this;
            Arrays.fill(evaluated, 0L);
            Arrays.fill(signs, (byte) 0);
            Arrays.fill(gaps, Double.NaN);
            left = leftRow;
            right = rightRow;
            active = true;
            verified = false;
            verifiedSign = 0;
            return this;
        }

        /** Evaluate one dimension at most once for this pair, retaining its evidence. */
        public void refine(int dimension) {
            requireActive();
            int index = Objects.checkIndex(dimension, signs.length);
            if (has(index)) return;
            double a = plan.reader.valueAt(index, left);
            double b = left == right ? a : plan.reader.valueAt(index, right);
            double gap = Math.abs(a - b);
            gap = Double.isFinite(a) && Double.isFinite(b) && Double.isFinite(gap)
                    ? gap : Double.NaN;
            Dimension descriptor = plan.dimensions[index];
            if (descriptor.metricWeight() > 0.0 && Double.isFinite(gap)
                    && Double.isFinite(descriptor.maximumGap())
                    && gap > descriptor.maximumGap()) {
                throw new IllegalStateException("false metric maximum-gap certificate: "
                        + descriptor.name());
            }
            // Commit only after all applicable domain certificates have been checked.
            signs[index] = (byte) Integer.signum(Double.compare(a, b));
            gaps[index] = gap;
            evaluated[index >>> 6] |= 1L << (index & 63);
        }

        /** Stop at the first decisive ordered dimension; skip unrelated HINT dimensions. */
        public void refineUntilRankKnown() {
            requireActive();
            for (int d = 0; d < plan.dimensions.length; d++) {
                if (!plan.dimensions[d].ordered()) continue;
                refine(d);
                if (signs[d] != 0) break;
            }
        }

        /** Refine just the weighted metric lanes, reusing any order evidence already computed. */
        public void refineMetric() {
            requireActive();
            for (int d = 0; d < plan.dimensions.length; d++) {
                if (plan.dimensions[d].metricWeight() > 0.0) refine(d);
            }
        }

        public void refineAll() {
            requireActive();
            for (int d = 0; d < signs.length; d++) refine(d);
        }

        /** True when an ordered nonzero result or all ordered ties have been evaluated. */
        public boolean rankKnown() {
            requireActive();
            boolean anyOrdered = false;
            for (int d = 0; d < signs.length; d++) {
                if (!plan.dimensions[d].ordered()) continue;
                anyOrdered = true;
                if (!has(d)) return false;
                if (signs[d] != 0) return true;
            }
            return anyOrdered;
        }

        /** Sign of a determined lexicographic rank; zero alone never asserts object equality. */
        public int rankOrder() {
            requireActive();
            for (int d = 0; d < signs.length; d++) {
                Dimension dim = plan.dimensions[d];
                if (!dim.ordered()) continue;
                if (!has(d)) return 0;
                if (signs[d] != 0) return dim.descending() ? -signs[d] : signs[d];
            }
            return 0;
        }

        /** Exact order needs an explicitly complete exact schema or a canonical verifier. */
        public boolean exactOrderCertified() {
            requireActive();
            return verified || (plan.completeExactSchema && rankKnown());
        }

        /**
         * Invoke the canonical comparator only when the source-time exact schema cannot prove
         * order. Repeated requests for the same pair never reinvoke that verifier.
         */
        public int exactOrderOrVerify(IntBinaryOperator canonicalComparator) {
            requireActive();
            if (verified) return verifiedSign;
            if (plan.completeExactSchema) {
                refineUntilRankKnown();
                return rankOrder();
            }
            Objects.requireNonNull(canonicalComparator, "canonicalComparator");
            int sign = Integer.signum(canonicalComparator.applyAsInt(left, right));
            verifiedSign = sign;
            verified = true;
            return sign;
        }

        public boolean evaluated(int dimension) {
            requireActive();
            int index = Objects.checkIndex(dimension, signs.length);
            return has(index);
        }

        public double separationAt(int dimension) {
            requireActive();
            int index = Objects.checkIndex(dimension, signs.length);
            return has(index) ? gaps[index] : Double.NaN;
        }

        /**
         * Conservative IEEE-rounded bounds for the *declared* weighted-L1 coordinate distance.
         * Unknown/unbounded maxima make the upper bound unavailable, never guessed. Neither
         * these bounds nor rank signs prove equality of the original domain objects.
         */
        public Bounds metricBounds() {
            requireActive();
            if (plan.metricDimensions == 0) return new Bounds(Double.NaN, Double.NaN, false);
            double lower = 0.0;
            double upper = 0.0;
            boolean knownUpper = true;
            boolean complete = true;
            for (int d = 0; d < signs.length; d++) {
                Dimension dim = plan.dimensions[d];
                double weight = dim.metricWeight();
                if (weight == 0.0) continue;
                double distance;
                if (has(d)) {
                    distance = gaps[d];
                    if (!Double.isFinite(distance)) return new Bounds(Double.NaN, Double.NaN, false);
                    if (Double.isFinite(dim.maximumGap())
                            && distance > dim.maximumGap()) {
                        throw new IllegalStateException("metric maximum-gap certificate invalidated");
                    }
                    double lowPart = roundedDown(weight, distance);
                    double highPart = roundedUp(weight, distance);
                    lower = sumDown(lower, lowPart);
                    if (knownUpper) upper = sumUp(upper, highPart);
                } else {
                    complete = false;
                    if (Double.isNaN(dim.maximumGap())) knownUpper = false;
                    else if (knownUpper) upper = sumUp(upper,
                            roundedUp(weight, dim.maximumGap()));
                }
                if (!Double.isFinite(lower)) return new Bounds(Double.NaN, Double.NaN, false);
                if (knownUpper && !Double.isFinite(upper)) knownUpper = false;
            }
            return new Bounds(lower, knownUpper ? upper : Double.NaN, complete && knownUpper);
        }

        /** Materialize immutable evidence only on request; the hot path reuses workspace arrays. */
        public Evidence snapshot() {
            requireActive();
            int decisive = -1;
            for (int d = 0; d < signs.length; d++) {
                if (plan.dimensions[d].ordered() && has(d) && signs[d] != 0) {
                    boolean earlierKnown = true;
                    for (int prior = 0; prior < d; prior++) {
                        if (plan.dimensions[prior].ordered() && !has(prior)) {
                            earlierKnown = false;
                            break;
                        }
                    }
                    if (earlierKnown) decisive = d;
                    break;
                }
            }
            return new Evidence(left, right, plan.version,
                    verified ? verifiedSign : rankOrder(), rankKnown(),
                    exactOrderCertified(), verified, decisive, evaluated, gaps, metricBounds());
        }

        private boolean has(int d) {
            return (evaluated[d >>> 6] & (1L << (d & 63))) != 0;
        }

        private void requireActive() {
            if (!active) throw new IllegalStateException("begin a comparison first");
        }
    }

    public record Bounds(double lower, double upper, boolean complete) {
        public Bounds {
            if (!Double.isNaN(lower) && (!Double.isFinite(lower) || lower < 0.0)) {
                throw new IllegalArgumentException("invalid lower bound");
            }
            if (!Double.isNaN(upper) && (!Double.isFinite(upper) || upper < 0.0)) {
                throw new IllegalArgumentException("invalid upper bound");
            }
            if (!Double.isNaN(lower) && !Double.isNaN(upper) && upper < lower) {
                throw new IllegalArgumentException("bounds reversed");
            }
            if (complete && (Double.isNaN(lower) || Double.isNaN(upper))) {
                throw new IllegalArgumentException("complete metric must have finite bounds");
            }
        }

        public boolean lowerAvailable() { return Double.isFinite(lower); }
        public boolean upperAvailable() { return Double.isFinite(upper); }
    }

    public record Evidence(int leftRow, int rightRow, long sourceVersion,
            int order, boolean rankKnown, boolean exactOrderCertified,
            boolean canonicalVerifierUsed, int decisiveDimension,
            long[] evaluatedMask, double[] separations, Bounds metricBounds) {
        public Evidence {
            if (order < -1 || order > 1 || decisiveDimension < -1) {
                throw new IllegalArgumentException("comparison order/dimension");
            }
            evaluatedMask = Objects.requireNonNull(evaluatedMask, "evaluatedMask").clone();
            separations = Objects.requireNonNull(separations, "separations").clone();
            metricBounds = Objects.requireNonNull(metricBounds, "metricBounds");
        }

        @Override public long[] evaluatedMask() { return evaluatedMask.clone(); }
        @Override public double[] separations() { return separations.clone(); }

        public boolean evaluated(int dimension) {
            int i = Objects.checkIndex(dimension, separations.length);
            return (evaluatedMask[i >>> 6] & (1L << (i & 63))) != 0;
        }
    }

    private M3ProgressiveSignalEvidence() {}

    private static double roundedDown(double a, double b) {
        double value = a * b;
        if (!Double.isFinite(value)) return Double.NaN;
        return value == 0.0 ? 0.0 : Math.max(0.0, Math.nextDown(value));
    }

    private static double roundedUp(double a, double b) {
        double value = a * b;
        if (!Double.isFinite(value)) return Double.NaN;
        return Math.nextUp(value);
    }

    private static double sumDown(double a, double b) {
        double result = a + b;
        if (!Double.isFinite(result)) return Double.NaN;
        return result == 0.0 ? 0.0 : Math.max(0.0, Math.nextDown(result));
    }

    private static double sumUp(double a, double b) {
        double result = a + b;
        if (!Double.isFinite(result)) return Double.NaN;
        return Math.nextUp(result);
    }
}
