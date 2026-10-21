// Copyright 2026 Hitesh Soliwal and contributors
// Full donor dependency receiver; source custody in JSON_CLOSURE_PROVENANCE.json.
/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.m3.file;


/**
 * M3ProgressCalculations - Common progress calculation utilities.
 *
 * <p>
 * Extracts duplicate progress calculation logic from:
 * <ul>
 * <li>AbstractJob - Progress reporting calculations</li>
 * <li>AbstractJobListener - Metrics computation</li>
 * </ul>
 *
 * <h2>Features</h2>
 * <ul>
 * <li>Percentage calculation (0-100)</li>
 * <li>Processing rate calculation (units/sec)</li>
 * <li>ETA estimation in milliseconds</li>
 * </ul>
 *
 * @since ContextOS 1.0
 */
/**
 * M3ProgressCalculations.
 * @author Synexia
 */
public final class M3ProgressCalculations {

    private static final double PERCENTAGE_MULTIPLIER = 100.0;
    private static final double RATE_MULTIPLIER = 1000.0;

    private M3ProgressCalculations() {
        // Utility class
    }

    /**
     * Calculate percentage of work completed.
     *
     * @param worked
     *            units completed
     * @param total
     *            total units
     * @return percentage (0-100) or -1 if total is unknown
     */
    public static double calculatePercentage(final long worked, final long total) {
        return total > 0 ? (worked * PERCENTAGE_MULTIPLIER) / total : -1;
    }

    /**
     * Calculate processing rate per second.
     *
     * @param worked
     *            units completed
     * @param elapsedMs
     *            elapsed time in milliseconds
     * @return units per second or 0 if elapsed is 0
     */
    public static double calculateRate(final long worked, final long elapsedMs) {
        return elapsedMs > 0 ? worked * RATE_MULTIPLIER / elapsedMs : 0;
    }

    /**
     * Calculate estimated time to completion in milliseconds.
     *
     * @param worked
     *            units completed
     * @param total
     *            total units
     * @param rate
     *            current processing rate (units/sec)
     * @return ETA in ms or -1 if cannot be calculated
     */
    public static long calculateEta(final long worked, final long total, final double rate) {
        return (rate > 0 && total > 0) ? (long) ((total - worked) / rate * RATE_MULTIPLIER) : -1;
    }

    /**
     * Calculate ETA using worked units, total, and elapsed time.
     *
     * <p>
     * Convenience method combining rate and ETA calculation.
     *
     * @param worked
     *            units completed
     * @param total
     *            total units
     * @param elapsedMs
     *            elapsed time in milliseconds
     * @return ETA in ms or -1 if cannot be calculated
     */
    public static long calculateEta(final long worked, final long total, final long elapsedMs) {
        final double rate = calculateRate(worked, elapsedMs);
        return calculateEta(worked, total, rate);
    }
}
