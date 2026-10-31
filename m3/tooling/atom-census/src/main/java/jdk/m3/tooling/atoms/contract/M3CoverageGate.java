/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.atoms.contract;

import java.util.EnumSet;
import java.util.Objects;

/** Evidence gates attached to every atom in the machine-maintained M3JDK ledger. */
public enum M3CoverageGate {
    /** The atom is present in the complete bounded inventory. */
    INVENTORIED,
    /** Exact owning source bytes or source slice are hash-bound. */
    SOURCE_IDENTITY,
    /** The atom is assigned to a closed pattern catalogue entry. */
    PATTERN_BOUND,
    /** The atom has an explicit behavioral or declarative contract. */
    CONTRACT_BOUND,
    /** An explicit reviewed authorization permits a normally locked contract change. */
    CONTRACT_CHANGE_AUTHORIZED,
    /** Human-readable documentation names behavior, invariants and limitations. */
    DOCUMENTED,
    /** Source and donor provenance are verified. */
    PROVENANCE_VERIFIED,
    /** Applicable source, donor and distribution licences are verified. */
    LICENSE_VERIFIED,
    /** Public source, binary, ABI or other external surface is verified. */
    PUBLIC_SURFACE_VERIFIED,
    /** Correctness and negative-path behavior are tested. */
    CORRECTNESS_TESTED,
    /** Test reachability or coverage for the atom is measured. */
    TEST_COVERAGE_MEASURED,
    /** Regeneration or repeated application reaches the declared fixed point. */
    FIXED_POINT_VERIFIED,
    /** Java source compiles in the owning target. */
    JAVA_COMPILED,
    /** Applicable interpreter, C1 and C2/JIT behavior is tested. */
    JIT_TESTED,
    /** The Java atom executes in the owning JVM/runtime configuration. */
    JVM_RUNTIME_TESTED,
    /** A build or script atom executes in its owning tool environment. */
    TOOL_RUNTIME_TESTED,
    /** Native source compiles with the owning target toolchain. */
    NATIVE_COMPILED,
    /** Native symbols, descriptors, calling convention and layout are verified. */
    ABI_VERIFIED,
    /** Native/JNI behavior executes in the owning target runtime. */
    NATIVE_RUNTIME_TESTED,
    /** Performance is measured for a named workload and environment. */
    PERFORMANCE_MEASURED;

    private static final EnumSet<M3CoverageGate> COMMON = EnumSet.of(
            INVENTORIED,
            SOURCE_IDENTITY,
            PATTERN_BOUND,
            CONTRACT_BOUND,
            DOCUMENTED,
            PROVENANCE_VERIFIED,
            LICENSE_VERIFIED,
            PUBLIC_SURFACE_VERIFIED,
            CORRECTNESS_TESTED,
            TEST_COVERAGE_MEASURED,
            FIXED_POINT_VERIFIED);

    /**
     * Returns a fresh required-gate set for {@code kind}.
     *
     * <p>The profile is intentionally strict. A gate may be {@link M3GateStatus#NOT_APPLICABLE},
     * but only with a hash-bound disposition and rationale. Silence is never coverage.</p>
     *
     * @param kind atom kind whose proof profile is requested
     * @return fresh mutable set of required gates
     */
    public static EnumSet<M3CoverageGate> requiredFor(M3AtomKind kind) {
        Objects.requireNonNull(kind, "kind");
        EnumSet<M3CoverageGate> required = EnumSet.copyOf(COMMON);
        if (kind.javaAtom()) {
            required.add(JAVA_COMPILED);
            required.add(JVM_RUNTIME_TESTED);
            if (kind.executable()) {
                required.add(JIT_TESTED);
                required.add(PERFORMANCE_MEASURED);
            }
        }
        if (kind.nativeAtom()) {
            required.add(NATIVE_COMPILED);
            required.add(ABI_VERIFIED);
            required.add(NATIVE_RUNTIME_TESTED);
            if (kind.executable()) {
                required.add(PERFORMANCE_MEASURED);
            }
        }
        if (kind == M3AtomKind.BUILD_RULE || kind == M3AtomKind.SCRIPT_FUNCTION) {
            required.add(TOOL_RUNTIME_TESTED);
            required.add(PERFORMANCE_MEASURED);
        }
        return required;
    }
}
