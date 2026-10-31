/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * Detached metadata and complete input-byte evidence from one selected file layer.
 * The manifest contains references only; constructing a capture neither stores
 * payload bytes nor computes word IDs, syntax trees, or other structures.
 *
 * <p>The provider metadata was sampled before opening input. The content identity
 * describes the bytes subsequently consumed from offset zero through EOF. These
 * are separate observations: a matching sampled size does not prove that the
 * provider remained unchanged or that the read was an atomic snapshot. A known
 * sampled size that differs from the observed byte count prevents capture.</p>
 *
 * <p>{@code capturedAtMillis} is the wall-clock time associated with collecting
 * the metadata sample, in milliseconds since the Unix epoch. It is independent
 * of the provider's modification timestamp. Both can have valid signed values.</p>
 *
 * @param observation metadata with the complete {@code RAW_BYTES} reference
 * @param manifest the raw reference and any explicitly attached structures
 * @param capturedAtMillis the time associated with the metadata sample
 */
public record MFileCapture(MFileObservation observation, M3FileManifest manifest, long capturedAtMillis) {
    /** Validates that the immutable observations and manifest describe the same completed input. */
    public MFileCapture {
        Objects.requireNonNull(observation, "observation");
        Objects.requireNonNull(manifest, "manifest");
        M3ObjectRef raw = observation.content().orElseThrow(
                () -> new IllegalArgumentException("capture requires complete observed input content"));
        if (!raw.equals(manifest.raw())) {
            throw new IllegalArgumentException("capture manifest raw reference differs from observed content");
        }
        if (observation.manifest().isPresent()
                && !observation.manifest().orElseThrow().id().equals(manifest.id())) {
            throw new IllegalArgumentException("capture manifest differs from the selected structure manifest");
        }
    }

    /**
     * Attaches completed input evidence to a provider metadata sample and creates
     * an empty raw manifest. Used by {@link MFileContent#captureReads} after EOF,
     * successful primary close, and successful user-shadow completion.
     *
     * <p>The caller attests that this completion covers the entire selected file
     * layer from offset zero and belongs to {@code sampledMetadata.sourceKey()}.
     * A descriptive completion layer label cannot establish that binding. An
     * arbitrary already-positioned stream or an output-write sequence is not
     * sufficient evidence. This method performs no provider I/O and stores no
     * payload; the reference describes bytes observed, not durable storage.</p>
     *
     * @throws IOException if the completion is not input EOF, or if its content
     *         contradicts the metadata sample or previously attached evidence
     */
    public static MFileCapture fromInput(MFileObservation sampledMetadata, long capturedAtMillis,
            MFileStreams.Completion completion) throws IOException {
        Objects.requireNonNull(sampledMetadata, "sampledMetadata");
        Objects.requireNonNull(completion, "completion");
        if (completion.kind() != MFileStreams.CompletionKind.INPUT_EOF) {
            throw new IOException("A file capture requires complete input EOF, not an output sequence");
        }
        try {
            M3ObjectRef raw = new M3ObjectRef(completion.contentHash(), completion.byteCount(), M3ObjectKind.RAW_BYTES);
            MFileObservation observed = sampledMetadata.withContent(raw);
            M3FileManifest rawManifest = new M3FileManifest(raw, Map.of(), Map.of(), Map.of());
            return new MFileCapture(observed, rawManifest, capturedAtMillis);
        } catch (IllegalArgumentException inconsistent) {
            throw new IOException("Completed input is inconsistent with sampled metadata: "
                    + inconsistent.getMessage(), inconsistent);
        }
    }

    /**
     * Attaches one structure already produced by an existing owner and selects it
     * for observation comparison. Other manifest references are retained. The
     * derivation key's first input must identify this capture's raw bytes; the
     * existing observation binding rules also apply.
     *
     * <p>The caller attests that {@code reference} was produced by {@code key}.
     * This method runs no parser or precompute, reads no stored object, and makes
     * no claim that a word-ID or syntax-tree reference establishes semantic
     * equivalence. Computing those structures remains an explicit operation.</p>
     */
    public MFileCapture withStructure(String name, M3ObjectRef reference, M3PrecomputeKey key) {
        M3FileManifest updated = manifest.withStructure(name, reference);
        return new MFileCapture(observation.withStructure(updated, name, key), updated, capturedAtMillis);
    }
}
