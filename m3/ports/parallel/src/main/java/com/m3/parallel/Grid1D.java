/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.parallel;

/**
 * One-dimensional launch grid.
 *
 * <p>{@code global} is the logical work size. A launch runs {@link #launchSize()} work-items: {@code global} rounded
 * up to a multiple of {@code local}. Work-items with {@code index >= global} still run, exactly as on a GPU, so every
 * kernel must guard its own logical bound.</p>
 *
 * @param global logical work-item count, {@code >= 1}
 * @param local  work-group size, {@code >= 1}
 */
public record Grid1D(int global, int local) {

    /**
     * Validates the grid and its rounded launch size.
     *
     * @param global logical work-item count, {@code >= 1}
     * @param local  work-group size, {@code >= 1}
     */
    public Grid1D {
        if (global < 1) {
            throw new IllegalArgumentException("global must be >= 1: " + global);
        }
        if (local < 1) {
            throw new IllegalArgumentException("local must be >= 1: " + local);
        }
        if (roundUp(global, local) > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("global rounded up to local exceeds int range");
        }
    }

    /**
     * Creates a grid.
     *
     * @param global logical work-item count
     * @param local  work-group size
     * @return the grid
     */
    public static Grid1D of(int global, int local) {
        return new Grid1D(global, local);
    }

    /**
     * Returns the number of work-items a launch executes: {@code global} rounded up to {@code local}.
     *
     * @return launch size
     */
    public int launchSize() {
        return (int) roundUp(global, local);
    }

    private static long roundUp(int value, int quantum) {
        return ((long) value + quantum - 1L) / quantum * quantum;
    }
}
