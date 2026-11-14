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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
/**
 * Convergence Detector - stop condition detection.
 *
 * <p>
 * From ChatGPT: "When Δenergy &lt; ε for N passes → convergence reached."
 * </p>
 *
 * <h2>SPECIFICATION</h2>
 *
 * <h3>Convergence Criterion</h3>
 *
 * <pre>{@code
 * converged = ∀i ∈ [n-window, n] : energy(passᵢ) < threshold
 *
 * where:
 *   n         = current pass number
 *   window    = number of consecutive quiet passes required
 *   threshold = maximum energy for a "quiet" pass
 * }</pre>
 *
 * <h3>Default Settings</h3>
 * <ul>
 * <li>threshold = 0.1 (very quiet)</li>
 * <li>windowSize = 3 (three consecutive quiet passes)</li>
 * </ul>
 *
 * @author Hitesh Soliwal (hsoliwal@gmail.com)
 * @see PassMetrics
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/convergence/ConvergenceDetector.java}
 * blob {@code 84803ea367b04d146a3417a17ada617111039354} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public class ConvergenceDetector {
    private static final PlaceholderLogger log = PlaceholderLogger.of(ConvergenceDetector.class);


    /** Default energy threshold. */
    public static final double DEFAULT_THRESHOLD = 0.1;

    /** Default window size. */
    public static final int DEFAULT_WINDOW = 3;

    private final double energyThreshold;
    private final int windowSize;
    private final List<PassMetrics> history = new ArrayList<>();

    /**
     * Creates a detector with default settings.
     *
     * <p>
     * Default: energy &lt; 0.1 for 3 consecutive passes.
     * </p>
     */
    public ConvergenceDetector() {
        this(DEFAULT_THRESHOLD, DEFAULT_WINDOW);
    }

    /**
     * Creates a detector with custom settings.
     *
     * @param energyThreshold
     *            maximum energy for quiet pass
     * @param windowSize
     *            consecutive quiet passes required
     */
    public ConvergenceDetector(double energyThreshold, int windowSize) {
        this.energyThreshold = Math.max(0.0, energyThreshold);
        this.windowSize = Math.max(1, windowSize);
    }

    /**
     * Records pass metrics.
     *
     * @param metrics
     *            the pass metrics
     */
    public void record(PassMetrics metrics) {
        if (metrics == null) {
            throw new IllegalArgumentException("Pass metrics cannot be null");
        }
        history.add(metrics);
    }

    /**
     * Checks if convergence has been reached.
     *
     * <p>
     * Returns true when the last {@code windowSize} passes all have energy &lt;
     * {@code energyThreshold}.
     * </p>
     *
     * @return true if converged
     */
    public boolean hasConverged() {
        if (!hasEnoughHistory()) {
            return false;
        }
        return isWindowConverged();
    }

    /** Checks if we have enough history to evaluate convergence. */
    private boolean hasEnoughHistory() {
        return history.size() >= windowSize;
    }

    /** Evaluates if the current window meets convergence criteria. */
    private boolean isWindowConverged() {
        int start = history.size() - windowSize;
        return IntStream.range(start, history.size())
                .noneMatch(i -> history.get(i).energy() >= energyThreshold);
    }

    /**
     * Gets convergence progress.
     *
     * <p>
     * Returns the number of consecutive quiet passes at the end.
     * </p>
     *
     * @return consecutive quiet passes [0, windowSize]
     */
    public int consecutiveQuietPasses() {
        int[] count = {0};
        IntStream.iterate(history.size() - 1, i -> i >= 0, i -> i - 1)
                .takeWhile(i -> history.get(i).energy() < energyThreshold)
                .forEach(i -> count[0]++);
        return Math.min(count[0], windowSize);
    }

    /**
     * Gets convergence percentage.
     *
     * @return progress [0.0, 1.0]
     */
    public double progress() {
        return (double) consecutiveQuietPasses() / windowSize;
    }

    /**
     * Gets the complete pass history.
     *
     * @return immutable copy of history
     */
    public List<PassMetrics> history() {
        return List.copyOf(history);
    }

    /**
     * Gets the number of passes recorded.
     *
     * @return pass count
     */
    public int passCount() {
        return history.size();
    }

    /**
     * Gets the last pass metrics.
     *
     * @return last metrics or null if no passes
     */
    public PassMetrics lastPass() {
        return history.isEmpty() ? null : history.get(history.size() - 1);
    }

    /** Clears the history for a fresh run. */
    public void reset() {
        history.clear();
    }

    /**
     * Gets the energy threshold.
     *
     * @return threshold value
     */
    public double getEnergyThreshold() {
        return energyThreshold;
    }

    /**
     * Gets the window size.
     *
     * @return window size
     */
    public int getWindowSize() {
        return windowSize;
    }
}
