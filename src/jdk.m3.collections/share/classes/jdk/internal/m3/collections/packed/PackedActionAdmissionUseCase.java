/*******************************************************************************
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
package jdk.internal.m3.collections.packed;

/**
 * Canonical end-to-end use case for {@link PackedSemanticMachine}.
 *
 * <p>The use case models admission and execution of an autonomous action. It deliberately
 * includes lifecycle events, derived Boolean state, persistent invariants, transition
 * guards, mutual exclusion, byte-scaled confidence/risk/pressure signals and hysteresis.
 * It is intended as the design guideline for deciding whether a new domain belongs in the
 * packed semantic kernel.
 *
 * <p>Runtime mutable state consists of:
 * <ul>
 *   <li>19 semantic facts, which fit in one 64-bit word;</li>
 *   <li>four unsigned-byte scalar channels.</li>
 * </ul>
 * The semantic payload is therefore 12 bytes per state (excluding Java object/array headers).
 *
 * <p>Names in this class are interpretation-boundary constants. Runtime state stores only
 * integer indexes, primitive fact words and scalar bytes.
 *
 * @since 1.0
 * Reference use case for the packed semantic-state-machine guideline.
 *
 * <p>The problem: decide whether an autonomous action can execute, react to risk/resource
 * changes, and record terminal outcomes. Runtime state uses only one semantic bit-vector
 * plus four unsigned-byte scalars. Meaning is compiled into primitive relation/rule arrays.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/semantic/PackedActionAdmissionUseCase.java}
 * blob {@code d3ceefbd4e243d1d4a11689ceb5f9c60dadd39d0} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedActionAdmissionUseCase {
    private PackedActionAdmissionUseCase() {}

    /** External/history fact: an action request exists. */
    public static final int REQUESTED = 0;

    /** External evidence fact: the subject/resource identity has been established. */
    public static final int IDENTIFIED = 1;

    /** External evidence fact: authentication has succeeded. */
    public static final int AUTHENTICATED = 2;

    /** External evidence fact: policy evaluation permits the action. */
    public static final int POLICY_OK = 3;

    /** External evidence fact: required input is available. */
    public static final int INPUT_READY = 4;

    /** External evidence fact: required execution resources are available. */
    public static final int RESOURCE_OK = 5;

    /** Derived fact: confidence is currently in the high hysteresis state. */
    public static final int HIGH_CONFIDENCE = 6;

    /** Derived fact: priority is currently in the high hysteresis state. */
    public static final int HIGH_PRIORITY = 7;

    /** Derived fact: risk is currently in the high hysteresis state. */
    public static final int RISK_HIGH = 8;

    /** Derived fact: resource pressure is currently in the high hysteresis state. */
    public static final int PRESSURE_HIGH = 9;

    /** Derived/history fact: all admission conditions were satisfied at least once. */
    public static final int ADMITTED = 10;

    /** External lifecycle fact: execution was explicitly started after admission. */
    public static final int STARTED = 11;

    /** Derived fact: the action is currently executing. */
    public static final int EXECUTING = 12;

    /** External terminal lifecycle fact: execution succeeded. */
    public static final int SUCCEEDED = 13;

    /** External terminal lifecycle fact: execution failed. */
    public static final int FAILED = 14;

    /** External terminal lifecycle fact: execution was cancelled. */
    public static final int CANCELLED = 15;

    /** Derived fact: the current failed action may be retried. */
    public static final int RETRYABLE = 16;

    /** Derived fact: current semantic conditions block execution. */
    public static final int BLOCKED = 17;

    /** Derived fact: operator/observer attention is warranted. */
    public static final int ALERT = 18;

    /** Number of semantic fact indexes used by this use case. */
    public static final int FACT_COUNT = 19;

    /** Unsigned-byte scalar channel: confidence, {@code 0..255}. */
    public static final int CONFIDENCE = 0;

    /** Unsigned-byte scalar channel: action priority, {@code 0..255}. */
    public static final int PRIORITY = 1;

    /** Unsigned-byte scalar channel: risk, {@code 0..255}. */
    public static final int RISK = 2;

    /** Unsigned-byte scalar channel: resource pressure, {@code 0..255}. */
    public static final int RESOURCE_PRESSURE = 3;

    /** Number of unsigned-byte scalar channels. */
    public static final int SCALAR_COUNT = 4;

    /** Confidence activates {@link #HIGH_CONFIDENCE} at or above this value. */
    public static final int HIGH_CONFIDENCE_ON = 192;

    /** Confidence clears {@link #HIGH_CONFIDENCE} at or below this value. */
    public static final int HIGH_CONFIDENCE_OFF = 176;

    /** Priority activates {@link #HIGH_PRIORITY} at or above this value. */
    public static final int HIGH_PRIORITY_ON = 192;

    /** Priority clears {@link #HIGH_PRIORITY} at or below this value. */
    public static final int HIGH_PRIORITY_OFF = 176;

    /** Risk activates {@link #RISK_HIGH} at or above this value. */
    public static final int HIGH_RISK_ON = 200;

    /** Risk clears {@link #RISK_HIGH} at or below this value. */
    public static final int HIGH_RISK_OFF = 180;

    /** Resource pressure activates {@link #PRESSURE_HIGH} at or above this value. */
    public static final int HIGH_PRESSURE_ON = 224;

    /** Resource pressure clears {@link #PRESSURE_HIGH} at or below this value. */
    public static final int HIGH_PRESSURE_OFF = 208;

    /**
     * Creates an empty state with the exact fact/scalar dimensions used by this policy.
     *
     * @return new state with all facts/scalars cleared
     */
    // External evidence / lifecycle stimuli.

    // Derived scalar facts.

    // Derived/lifecycle semantic facts.


    // Four global 0..255 scalars. No float is retained.


    public static PackedSemanticState newState() {
        return new PackedSemanticState(FACT_COUNT, SCALAR_COUNT);
    }

    /**
     * Returns whether a fact is derived by the semantic program rather than directly
     * controlled by the caller.
     *
     * @param fact fact index
     * @return true for derived facts
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    public static boolean isDerivedFact(int fact) {
        return switch (fact) {
            case HIGH_CONFIDENCE, HIGH_PRIORITY, RISK_HIGH, PRESSURE_HIGH,
                    ADMITTED, EXECUTING, RETRYABLE, BLOCKED, ALERT -> true;
            case REQUESTED, IDENTIFIED, AUTHENTICATED, POLICY_OK, INPUT_READY,
                    RESOURCE_OK, STARTED, SUCCEEDED, FAILED, CANCELLED -> false;
            default -> throw new IndexOutOfBoundsException("fact=" + fact);
        };
    }

    /**
     * Returns the stable interpretation-boundary name of a fact index.
     *
     * <p>This method is for logs, diagnostics, tests and UI boundaries. The runtime state
     * does not retain these strings per fact.
     *
     * @param fact fact index
     * @return stable symbolic name
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    public static String factName(int fact) {
        return switch (fact) {
            case REQUESTED -> "REQUESTED";
            case IDENTIFIED -> "IDENTIFIED";
            case AUTHENTICATED -> "AUTHENTICATED";
            case POLICY_OK -> "POLICY_OK";
            case INPUT_READY -> "INPUT_READY";
            case RESOURCE_OK -> "RESOURCE_OK";
            case HIGH_CONFIDENCE -> "HIGH_CONFIDENCE";
            case HIGH_PRIORITY -> "HIGH_PRIORITY";
            case RISK_HIGH -> "RISK_HIGH";
            case PRESSURE_HIGH -> "PRESSURE_HIGH";
            case ADMITTED -> "ADMITTED";
            case STARTED -> "STARTED";
            case EXECUTING -> "EXECUTING";
            case SUCCEEDED -> "SUCCEEDED";
            case FAILED -> "FAILED";
            case CANCELLED -> "CANCELLED";
            case RETRYABLE -> "RETRYABLE";
            case BLOCKED -> "BLOCKED";
            case ALERT -> "ALERT";
            default -> throw new IndexOutOfBoundsException("fact=" + fact);
        };
    }

    /**
     * Returns the stable interpretation-boundary name of a scalar channel.
     *
     * @param scalar scalar index
     * @return stable symbolic name
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    public static String scalarName(int scalar) {
        return switch (scalar) {
            case CONFIDENCE -> "CONFIDENCE";
            case PRIORITY -> "PRIORITY";
            case RISK -> "RISK";
            case RESOURCE_PRESSURE -> "RESOURCE_PRESSURE";
            default -> throw new IndexOutOfBoundsException("scalar=" + scalar);
        };
    }

    /**
     * Builds and compiles the complete reference semantic program.
     *
     * <p>The resulting machine demonstrates the recommended modeling rules:
     * <ul>
     *   <li>{@code FAILED -> ALERT} and {@code RISK_HIGH -> ALERT} are event implications;</li>
     *   <li>terminal outcomes are mutually exclusive event/exclusion facts;</li>
     *   <li>{@code STARTED} is guarded by historical {@code ADMITTED};</li>
     *   <li>{@code RETRYABLE -> FAILED} is a persistent invariant;</li>
     *   <li>admission/execution/blocking/retry are compound bit rules;</li>
     *   <li>confidence/priority/risk/pressure are unsigned-byte scalar rules with hysteresis;</li>
     *   <li>all derived facts are protected from direct external mutation.</li>
     * </ul>
     *
     * @return compiled, ready-to-execute machine
     * Compiles the entire reference policy into primitive masks and byte thresholds.
     */
    public static PackedSemanticMachine machine() {
        PackedSemanticMachine machine = new PackedSemanticMachine(FACT_COUNT, SCALAR_COUNT, 16);

        machine.imply(RISK_HIGH, ALERT);
        machine.imply(FAILED, ALERT);

        // Direct relations: an activated fact can trigger a complete consequence row.
        machine.imply(RISK_HIGH, ALERT);
        machine.imply(FAILED, ALERT);

        // Terminal outcomes replace older terminal outcomes. The newly activated outcome wins.
        machine.excludeMutual(SUCCEEDED, FAILED);
        machine.excludeMutual(SUCCEEDED, CANCELLED);
        machine.excludeMutual(FAILED, CANCELLED);

        // Guards are transition-time preconditions, not permanent invariants.
        machine.guard(STARTED, ADMITTED);
        machine.guard(SUCCEEDED, STARTED);
        machine.guard(FAILED, STARTED);
        machine.guard(CANCELLED, REQUESTED);

        machine.require(EXECUTING, STARTED);
        machine.require(RETRYABLE, FAILED);

        // Persistent invariants apply only while the derived fact itself remains active.
        machine.require(EXECUTING, STARTED);
        machine.require(RETRYABLE, FAILED);

        // Derived facts cannot be directly toggled by callers.
        for (int derived : new int[] {
                HIGH_CONFIDENCE,
                HIGH_PRIORITY,
                RISK_HIGH,
                PRESSURE_HIGH,
                ADMITTED,
                EXECUTING,
                RETRYABLE,
                BLOCKED,
                ALERT
        }) {
            machine.externallyMutable(derived, false);
        }

        long[] none = machine.newMask();

        // Quantized scalar -> semantic fact. Separate ON/OFF ranges create integer hysteresis:
        // values in the dead band preserve the previous derived fact and avoid threshold chatter.
        machine.addScalarRule(
                none, none, none,
                CONFIDENCE, HIGH_CONFIDENCE_ON, 255,
                machine.newMask(HIGH_CONFIDENCE), none);
        machine.addScalarRule(
                none, none, none,
                CONFIDENCE, 0, HIGH_CONFIDENCE_OFF,
                none, machine.newMask(HIGH_CONFIDENCE));

        machine.addScalarRule(
                none, none, none,
                PRIORITY, HIGH_PRIORITY_ON, 255,
                machine.newMask(HIGH_PRIORITY), none);
        machine.addScalarRule(
                none, none, none,
                PRIORITY, 0, HIGH_PRIORITY_OFF,
                none, machine.newMask(HIGH_PRIORITY));

        machine.addScalarRule(
                none, none, none,
                RISK, HIGH_RISK_ON, 255,
                machine.newMask(RISK_HIGH), none);
        machine.addScalarRule(
                none, none, none,
                RISK, 0, HIGH_RISK_OFF,
                none, machine.newMask(RISK_HIGH));

        machine.addScalarRule(
                none, none, none,
                RESOURCE_PRESSURE, HIGH_PRESSURE_ON, 255,
                machine.newMask(PRESSURE_HIGH), none);
        machine.addScalarRule(
                none, none, none,
                RESOURCE_PRESSURE, 0, HIGH_PRESSURE_OFF,
                none, machine.newMask(PRESSURE_HIGH));

        // Any blocking source derives BLOCKED; when all sources disappear, BLOCKED clears.
        machine.addRule(
                none,
                machine.newMask(RISK_HIGH, PRESSURE_HIGH),
                none,
                machine.newMask(BLOCKED),
                none);
        machine.addRule(
                none,
                none,
                machine.newMask(RISK_HIGH, PRESSURE_HIGH),
                none,
                machine.newMask(BLOCKED));

        // Admission is a conjunction over evidence plus confidence, and cannot be granted
        // while a blocker is active. ADMITTED is historical once granted.
        machine.addRule(
                machine.newMask(
                        REQUESTED,
                        IDENTIFIED,
                        AUTHENTICATED,
                        POLICY_OK,
                        INPUT_READY,
                        RESOURCE_OK,
                        HIGH_CONFIDENCE),
                none,
                machine.newMask(BLOCKED),
                machine.newMask(ADMITTED),
                none);

        // STARTED + ADMITTED executes while not blocked/terminal.
        machine.addRule(
                machine.newMask(ADMITTED, STARTED, HIGH_CONFIDENCE),
                none,
                machine.newMask(BLOCKED, SUCCEEDED, FAILED, CANCELLED),
                machine.newMask(EXECUTING),
                none);

        // A blocker or terminal outcome stops current execution.
        machine.addRule(
                none,
                machine.newMask(BLOCKED, SUCCEEDED, FAILED, CANCELLED),
                none,
                none,
                machine.newMask(EXECUTING));

        machine.addRule(
                none,
                none,
                machine.newMask(HIGH_CONFIDENCE),
                none,
                machine.newMask(EXECUTING));

        // Failure can be retried only while risk is below the high-risk threshold.
        machine.addRule(
                machine.newMask(FAILED),
                none,
                machine.newMask(RISK_HIGH),
                machine.newMask(RETRYABLE),
                none);
        machine.addRule(
                machine.newMask(RISK_HIGH),
                none,
                none,
                none,
                machine.newMask(RETRYABLE));
        machine.addRule(
                none,
                machine.newMask(SUCCEEDED, CANCELLED),
                none,
                none,
                machine.newMask(RETRYABLE));

        machine.compile();
        return machine;
    }
}
