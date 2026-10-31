/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.parallel.spi;

import java.util.Objects;

/**
 * Result of probing a backend runtime.
 *
 * @param available {@code true} when the backend can prepare and launch kernels
 * @param reason    human-readable reason; {@code "ready"} when available
 */
public record Probe(boolean available, String reason) {
    private static final Probe READY = new Probe(true, "ready");

    /**
     * Validates the reason.
     *
     * @param available availability
     * @param reason    non-blank reason
     */
    public Probe {
        Objects.requireNonNull(reason, "reason");
        if (reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
    }

    /**
     * Returns the available probe.
     *
     * @return an available probe with reason {@code "ready"}
     */
    public static Probe ready() {
        return READY;
    }

    /**
     * Returns an unavailable probe.
     *
     * @param reason why the backend cannot run
     * @return an unavailable probe
     */
    public static Probe unavailable(String reason) {
        return new Probe(false, reason);
    }
}
