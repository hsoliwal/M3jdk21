/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.atoms.contract;

/** Explicit gate state. Missing evidence is never interpreted as success. */
public enum M3GateStatus {
    /** The gate executed and the bounded claim passed. */
    PASS,
    /** The gate executed and failed. */
    FAIL,
    /** The gate has not executed. */
    NOT_RUN,
    /** The gate could not execute for the recorded reason. */
    BLOCKED,
    /** A hash-bound disposition proves the gate cannot apply. */
    NOT_APPLICABLE;

    /**
     * Returns whether this state can satisfy an admission gate.
     *
     * @return {@code true} only for {@link #PASS} and {@link #NOT_APPLICABLE}
     */
    public boolean admissible() {
        return this == PASS || this == NOT_APPLICABLE;
    }
}
