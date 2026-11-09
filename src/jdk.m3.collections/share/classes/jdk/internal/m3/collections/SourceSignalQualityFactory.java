/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.internal.m3.collections;

import java.util.Locale;
import java.util.Objects;
import jdk.m3.collections.SignalColumns;
import jdk.m3.collections.SignalDependencyEpochs;
import jdk.m3.collections.SignalQualitySchema;
import jdk.m3.collections.SourceSignalImage;

/**
 * Manufacturer of precise SOURCE-OBSERVATION quality descriptors from the existing
 * SHA-256-checked javac signal image, not a second parser or a runtime profiler.
 *
 * <p>The stored values describe source constructs recorded by the compiler.
 * Even DIRECT_CALLS and ALLOCATION_SITES count static call/allocation expressions,
 * not calls or allocations actually executed at runtime. External, reflective,
 * native and dynamically dispatched dependencies remain outside its proof.</p>
 */
public final class SourceSignalQualityFactory {
    private static final SourceSignalImage.Scalar[] SCALARS =
            SourceSignalImage.Scalar.values();

    private SourceSignalQualityFactory() { }

    /**
     * One image-scoped binding with a graph policy fixed at creation.
     * The compiler graph is recorded evidence, not complete runtime call closure.
     * Invalidation remains explicitly caller-driven, including invalidateAll() for
     * reflection, native/external mutation, and otherwise unrecorded relationships.
     */
    public static final class RecordedGraphBinding {
        private final SignalQualitySchema schema;
        private final SignalDependencyEpochs invalidations;
        private final boolean includeDispatchCandidates;

        private RecordedGraphBinding(SignalQualitySchema schema,
                SignalDependencyEpochs invalidations, boolean includeDispatchCandidates) {
            this.schema = schema;
            this.invalidations = invalidations;
            this.includeDispatchCandidates = includeDispatchCandidates;
        }

        public SignalQualitySchema schema() { return schema; }
        public SignalDependencyEpochs invalidations() { return invalidations; }
        public boolean includesDispatchCandidates() { return includeDispatchCandidates; }
    }

    /**
     * Construct freshness and typed quality from the SAME SHA-256-verified compiler
     * image. No separate caller-created CSR index can accidentally be bound here.
     * The returned owner tracks only recorded source relationships under the
     * explicitly chosen direct/candidate policy; unrecorded edges require
     * conservative invalidation by the consuming runtime.
     */
    public static RecordedGraphBinding bindRecordedGraph(SourceSignalImage image,
            long sourceVersion, boolean includeDispatchCandidates) {
        Objects.requireNonNull(image, "verified compiler image");
        SignalDependencyEpochs invalidations = SignalDependencyEpochs.from(
                image.relationships(), includeDispatchCandidates);
        return new RecordedGraphBinding(fromVerifiedCompilerImage(image,
                invalidations, sourceVersion), invalidations, includeDispatchCandidates);
    }

    /**
     * Receive the original graph generation owner, with dispatch policy selected by
     * the caller. Returning a schema does not transfer or hide invalidation control.
     */
    public static SignalQualitySchema fromVerifiedCompilerImage(
            SourceSignalImage image, SignalDependencyEpochs epochs, long sourceVersion) {
        Objects.requireNonNull(image, "verified compiler image");
        Objects.requireNonNull(epochs, "dependency epochs");
        SignalColumns columns = image.columns();
        if (columns.columnCount() != SCALARS.length || image.rowCount() != epochs.rowCount()) {
            throw new IllegalArgumentException("original compiler scalar schema / row mismatch");
        }
        SignalQualitySchema.Spec[] specifications =
                new SignalQualitySchema.Spec[SCALARS.length];
        for (int i = 0; i < SCALARS.length; i++) {
            SourceSignalImage.Scalar scalar = SCALARS[i];
            if (columns.origin(i) != SignalColumns.Origin.STATIC) {
                throw new IllegalArgumentException("source image non-static scalar");
            }
            specifications[i] = new SignalQualitySchema.Spec(
                    "javac." + scalar.name().toLowerCase(Locale.ROOT),
                    family(scalar), SignalQualitySchema.Phase.COMPILER,
                    SignalQualitySchema.Reliability.EXACT_FACT,
                    SignalQualitySchema.Invalidation.SOURCE_VERSION,
                    unit(scalar), "M3SourceSignals/javac21", Double.NaN, "");
        }
        return SignalQualitySchema.fromCompilerImage(
                image, epochs, sourceVersion, specifications);
    }

    private static SignalQualitySchema.Family family(SourceSignalImage.Scalar scalar) {
        return switch (scalar) {
            case FIELDS, METHODS, CONSTRUCTORS, LAMBDAS ->
                    SignalQualitySchema.Family.STRUCTURAL;
            case DISTINCT_TYPE_DEPENDENCIES, MEMBER_REFERENCES ->
                    SignalQualitySchema.Family.DEPENDENCY;
            case CALL_SITES, DIRECT_CALLS, DISPATCH_CANDIDATES, UNRESOLVED_CALLS ->
                    SignalQualitySchema.Family.CALL_GRAPH;
            case ALLOCATION_SITES -> SignalQualitySchema.Family.ALLOCATION;
            case LOOPS, BRANCHES, TRY_BLOCKS, CATCHES ->
                    SignalQualitySchema.Family.CONTROL_FLOW;
            case INTERNAL_INCOMING_SITES, INTERNAL_OUTGOING_SITES ->
                    SignalQualitySchema.Family.GRAPH;
        };
    }

    private static String unit(SourceSignalImage.Scalar scalar) {
        return switch (scalar) {
            case DISTINCT_TYPE_DEPENDENCIES -> "recorded-types";
            case INTERNAL_INCOMING_SITES, INTERNAL_OUTGOING_SITES, CALL_SITES,
                    DIRECT_CALLS, DISPATCH_CANDIDATES, UNRESOLVED_CALLS,
                    ALLOCATION_SITES, MEMBER_REFERENCES -> "source-sites";
            default -> "source-declarations";
        };
    }
}
