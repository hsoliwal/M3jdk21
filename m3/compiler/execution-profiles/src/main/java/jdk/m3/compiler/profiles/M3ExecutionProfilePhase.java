/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.compiler.profiles;

/** Workload phase attached to one execution-profile observation. */
public enum M3ExecutionProfilePhase {
    /** Observation occurred during the producer-defined startup interval. */
    STARTUP,
    /** Observation occurred during a producer-defined steady-state interval. */
    STEADY_STATE,
    /** Observation occurred during a producer-defined shutdown interval. */
    SHUTDOWN,
    /** Producer did not assign a phase. */
    UNSCOPED
}
