/* ******************************************************************************
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

import java.util.Map;
/**
 * Fluid Recall - soft context field (bias, not storage).
 *
 * <p>
 * From ChatGPT: "Fluid recall does NOT inject facts. It biases consolidation,
 * nudges retention, increases survival probability of high-signal structures."
 * </p>
 *
 * <h2>SPECIFICATION</h2>
 *
 * <h3>What Fluid Recall IS</h3>
 * <ul>
 * <li>A soft influence on scoring</li>
 * <li>A survival bias for similar content</li>
 * <li>A proximity detector (not a fact injector)</li>
 * </ul>
 *
 * <h3>What Fluid Recall is NOT</h3>
 * <ul>
 * <li>NOT a memory store</li>
 * <li>NOT a fact injector</li>
 * <li>NOT authoritative</li>
 * </ul>
 *
 * <h3>Formula</h3>
 *
 * <pre>{@code
 * bias(candidate, memory) = min(1.0, Σ chunk.score where candidate ∩ chunk ≠ ∅)
 *
 * Intersection detected via:
 *   - Substring containment
 *   - Signal overlap (future)
 * }</pre>
 *
 * @author Hitesh Soliwal (hsoliwal@gmail.com)
 * @see ConvergenceChunk
 * @see ConvergenceProjection
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/convergence/FluidRecall.java}
 * blob {@code 41bc21c2c205c57e51c6a1b3f8003ed7af7939b3} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public class FluidRecall {
    private static final PlaceholderLogger log = PlaceholderLogger.of(FluidRecall.class);

    /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
    public FluidRecall() {
    }


    /** Weight for text-based bias in combined calculation. */
    private static final double TEXT_BIAS_WEIGHT = 0.6;

    /** Weight for signal-based bias in combined calculation. */
    private static final double SIGNAL_BIAS_WEIGHT = 0.4;

    /**
     * Produces a recall bias score based on proximity to existing memory.
     *
     * <p>
     * <b>KEY INVARIANT:</b> No facts are returned. Only influence.
     * </p>
     *
     * <p>
     * The bias score influences:
     * </p>
     * <ul>
     * <li>Chunk scoring during add/consolidate decisions</li>
     * <li>Retention probability during decay</li>
     * <li>Nothing else</li>
     * </ul>
     *
     * @param candidate
     *            the candidate text being processed
     * @param memory
     *            current memory state
     * @return bias score in range [0.0, 1.0]
     */
    public double bias(String candidate, Map<Long, ConvergenceChunk> memory) {
        if (candidate == null || candidate.isEmpty() || memory == null || memory.isEmpty()) {
            return 0.0;
        }

        double bias = memory.values().stream()
                .filter(chunk -> intersects(candidate, chunk))
                .mapToDouble(ConvergenceChunk::score)
                .sum();

        return Math.min(bias, 1.0);
    }

    /**
     * Checks if candidate intersects with chunk.
     *
     * <p>
     * Current implementation: substring containment. Future: signal set overlap.
     * </p>
     *
     * @param candidate
     *            candidate text
     * @param chunk
     *            memory chunk
     * @return true if intersection detected
     */
    protected boolean intersects(String candidate, ConvergenceChunk chunk) {
        String chunkText = chunk.text();
        return candidate.contains(chunkText) || chunkText.contains(candidate);
    }

    /**
     * Produces a bias score with signal-based intersection.
     *
     * <p>
     * <b>DEFERRED:</b> More expensive, use only when needed.
     * </p>
     *
     * @param candidateSignals
     *            signals from candidate
     * @param memory
     *            current memory state
     * @return bias score in range [0.0, 1.0]
     */
    public double biasWithSignals(java.util.Set<String> candidateSignals, Map<Long, ConvergenceChunk> memory) {

        if (candidateSignals == null || candidateSignals.isEmpty() || memory == null || memory.isEmpty()) {
            return 0.0;
        }

        double bias = memory.values().stream()
                .mapToDouble(chunk -> {
                    long overlap = chunk.signals().stream()
                            .filter(candidateSignals::contains)
                            .count();
                    if (overlap > 0) {
                        // Weight by overlap ratio
                        double ratio = (double) overlap / chunk.signals().size();
                        return chunk.score() * ratio;
                    }
                    return 0.0;
                })
                .sum();

        return Math.min(bias, 1.0);
    }

    /**
     * Produces combined bias (text + signals).
     *
     * <p>
     * Combines text proximity and signal overlap for comprehensive bias.
     * </p>
     *
     * @param candidate
     *            candidate text
     * @param candidateSignals
     *            candidate signals
     * @param memory
     *            current memory state
     * @return combined bias in range [0.0, 1.0]
     */
    public double combinedBias(
            String candidate, java.util.Set<String> candidateSignals, Map<Long, ConvergenceChunk> memory) {

        double textBias = bias(candidate, memory);
        double signalBias = biasWithSignals(candidateSignals, memory);

        // Weight: 60% text, 40% signals
        return Math.min(1.0, textBias * TEXT_BIAS_WEIGHT + signalBias * SIGNAL_BIAS_WEIGHT);
    }
}
