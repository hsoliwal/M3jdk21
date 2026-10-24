/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.util.Locale;
import java.util.Objects;

/**
 * Manufacturer of precise SOURCE-OBSERVATION quality descriptors from the existing
 * SHA-256-checked javac signal image, not a second parser or a runtime profiler.
 *
 * <p>The stored values describe source constructs recorded by the compiler.
 * Even DIRECT_CALLS and ALLOCATION_SITES count static call/allocation expressions,
 * not calls or allocations actually executed at runtime. External, reflective,
 * native and dynamically dispatched dependencies remain outside its proof.</p>
 */
public final class M3SourceSignalQualityFactory {
    private static final M3SourceSignalImage.Scalar[] SCALARS =
            M3SourceSignalImage.Scalar.values();

    private M3SourceSignalQualityFactory() { }

    /**
     * Receive the original graph generation owner, with dispatch policy selected by
     * the caller. Returning a schema does not transfer or hide invalidation control.
     */
    public static M3SignalQualitySchema fromVerifiedCompilerImage(
            M3SourceSignalImage image, M3SignalDependencyEpochs epochs, long sourceVersion) {
        Objects.requireNonNull(image, "verified compiler image");
        Objects.requireNonNull(epochs, "dependency epochs");
        M3SignalColumns columns = image.columns();
        if (columns.columnCount() != SCALARS.length || image.rowCount() != epochs.rowCount()) {
            throw new IllegalArgumentException("original compiler scalar schema / row mismatch");
        }
        M3SignalQualitySchema.Spec[] specifications =
                new M3SignalQualitySchema.Spec[SCALARS.length];
        for (int i = 0; i < SCALARS.length; i++) {
            M3SourceSignalImage.Scalar scalar = SCALARS[i];
            if (columns.origin(i) != M3SignalColumns.Origin.STATIC) {
                throw new IllegalArgumentException("source image non-static scalar");
            }
            specifications[i] = new M3SignalQualitySchema.Spec(
                    "javac." + scalar.name().toLowerCase(Locale.ROOT),
                    family(scalar), M3SignalQualitySchema.Phase.COMPILER,
                    M3SignalQualitySchema.Reliability.EXACT_FACT,
                    M3SignalQualitySchema.Invalidation.SOURCE_VERSION,
                    unit(scalar), "M3SourceSignals/javac21", Double.NaN, "");
        }
        return M3SignalQualitySchema.fromCompilerImage(
                image, epochs, sourceVersion, specifications);
    }

    private static M3SignalQualitySchema.Family family(M3SourceSignalImage.Scalar scalar) {
        return switch (scalar) {
            case FIELDS, METHODS, CONSTRUCTORS, LAMBDAS ->
                    M3SignalQualitySchema.Family.STRUCTURAL;
            case DISTINCT_TYPE_DEPENDENCIES, MEMBER_REFERENCES ->
                    M3SignalQualitySchema.Family.DEPENDENCY;
            case CALL_SITES, DIRECT_CALLS, DISPATCH_CANDIDATES, UNRESOLVED_CALLS ->
                    M3SignalQualitySchema.Family.CALL_GRAPH;
            case ALLOCATION_SITES -> M3SignalQualitySchema.Family.ALLOCATION;
            case LOOPS, BRANCHES, TRY_BLOCKS, CATCHES ->
                    M3SignalQualitySchema.Family.CONTROL_FLOW;
            case INTERNAL_INCOMING_SITES, INTERNAL_OUTGOING_SITES ->
                    M3SignalQualitySchema.Family.GRAPH;
        };
    }

    private static String unit(M3SourceSignalImage.Scalar scalar) {
        return switch (scalar) {
            case DISTINCT_TYPE_DEPENDENCIES -> "recorded-types";
            case INTERNAL_INCOMING_SITES, INTERNAL_OUTGOING_SITES, CALL_SITES,
                    DIRECT_CALLS, DISPATCH_CANDIDATES, UNRESOLVED_CALLS,
                    ALLOCATION_SITES, MEMBER_REFERENCES -> "source-sites";
            default -> "source-declarations";
        };
    }
}
