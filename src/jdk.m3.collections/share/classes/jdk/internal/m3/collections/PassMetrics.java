/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections;


/**
 * Pass Metrics - convergence tracking per pass.
 *
 * <h2>SPECIFICATION</h2>
 *
 * <h3>Energy Formula</h3>
 *
 * <pre>{@code
 * energy = adds + drops + consolidates + |scoreDelta|
 *
 * High energy → significant changes
 * Low energy  → approaching convergence
 * }</pre>
 *
 * <h3>Convergence Pattern</h3>
 *
 * <pre>{@code
 * Early passes  → high variance, high novelty
 * Middle passes → structure reinforcement
 * Late passes   → diminishing updates
 * }</pre>
 *
 * @author Hitesh Soliwal (hsoliwal@gmail.com)
 * @param passNumber
 *            the pass number (1-indexed)
 * @param addCount
 *            number of AddEvents
 * @param dropCount
 *            number of DropEvents
 * @param consolidateCount
 *            number of ConsolidateEvents
 * @param totalScoreDelta
 *            net score change
 * @param memorySize
 *            chunks in memory after pass
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/convergence/PassMetrics.java}
 * blob {@code 1a17f1af102029334005eb71a1645d165d329daf} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public record PassMetrics(
        int passNumber, int addCount, int dropCount, int consolidateCount, double totalScoreDelta, int memorySize) {

    // --- INTERNED STRING CONSTANTS ---
    private static final TokenInterner INTERNER = TokenInterner.getInstance();
    private static final String FORMAT_COMPACT = INTERNER.intern("📊 Pass %d: +%d -%d ~%d Δ=%.2f mem=%d energy=%.2");

    /**
     * Computes the energy of this pass.
     *
     * <p>
     * Energy measures the magnitude of changes in this pass. Lower energy indicates
     * convergence.
     * </p>
     *
     * @return energy value (always >= 0)
     */
    public double energy() {
        return addCount + dropCount + consolidateCount + Math.abs(totalScoreDelta);
    }

    /**
     * Checks if this pass is "quiet" (low energy).
     *
     * @param threshold
     *            energy threshold
     * @return true if energy < threshold
     */
    public boolean isQuiet(double threshold) {
        return energy() < threshold;
    }

    /**
     * Gets total event count.
     *
     * @return adds + drops + consolidates
     */
    public int eventCount() {
        return addCount + dropCount + consolidateCount;
    }

    /**
     * Creates a compact single-line representation.
     *
     * @return formatted string
     */
    public String toCompactLine() {
        return String.format(
                java.util.Locale.ROOT,
                FORMAT_COMPACT,
                passNumber,
                addCount,
                dropCount,
                consolidateCount,
                totalScoreDelta,
                memorySize,
                energy());
    }

    /**
     * Creates an empty metrics instance (no changes).
     *
     * @param passNumber
     *            the pass number
     * @param memorySize
     *            current memory size
     * @return empty metrics
     */
    public static PassMetrics empty(int passNumber, int memorySize) {
        return new PassMetrics(passNumber, 0, 0, 0, 0.0, memorySize);
    }

    /** Builder for constructing PassMetrics incrementally. */
    public static class Builder {

        private final int passNumber;
        private int adds = 0;
        private int drops = 0;
        private int consolidates = 0;
        private double scoreDelta = 0.0;
        private int memorySize = 0;

        /**
         * Creates a builder for the given pass number.
         *
         * @param passNumber
         *            pass number
         */
        public Builder(int passNumber) {
            this.passNumber = passNumber;
        }

        /**
         * Increments the add count.
         *
         * @return this builder
         */
        public Builder add() {
            adds++;
            return this;
        }

        /**
         * Increments the drop count.
         *
         * @return this builder
         */
        public Builder drop() {
            drops++;
            return this;
        }

        /**
         * Increments the consolidate count.
         *
         * @return this builder
         */
        public Builder consolidate() {
            consolidates++;
            return this;
        }

        /**
         * Adds to the score delta.
         *
         * @param delta
         *            delta to add
         * @return this builder
         */
        public Builder addScoreDelta(double delta) {
            scoreDelta += delta;
            return this;
        }

        /**
         * Sets the memory size.
         *
         * @param size
         *            memory size
         * @return this builder
         */
        public Builder memorySize(int size) {
            this.memorySize = size;
            return this;
        }

        /**
         * Builds the pass metrics.
         *
         * @return pass metrics
         */
        public PassMetrics build() {
            return new PassMetrics(passNumber, adds, drops, consolidates, scoreDelta, memorySize);
        }
    }
}
