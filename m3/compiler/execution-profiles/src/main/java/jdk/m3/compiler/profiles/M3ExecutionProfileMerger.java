/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.compiler.profiles;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Deterministic, compatibility-checked set-union merger for execution-profile bundles. */
public final class M3ExecutionProfileMerger {
    /** Stable producer identity written to merged bundles. */
    public static final String PRODUCER = "m3-execution-profile-merger";
    /** Stable producer version written to merged bundles. */
    public static final String PRODUCER_VERSION = "1";

    private M3ExecutionProfileMerger() {
    }

    /**
     * Merges one or more compatible bundles by observation identity.
     *
     * @param inputs input bundles
     * @return canonical merged bundle
     */
    public static M3ExecutionProfileBundle merge(Collection<M3ExecutionProfileBundle> inputs) {
        Objects.requireNonNull(inputs, "inputs");
        if (inputs.isEmpty()) {
            throw new IllegalArgumentException("at least one input bundle is required");
        }
        List<M3ExecutionProfileBundle> bundles = List.copyOf(inputs);
        M3ExecutionProfileMetadata reference = bundles.get(0).metadata();
        Map<String, M3ExecutionProfileObservation> union = new TreeMap<>();
        for (M3ExecutionProfileBundle bundle : bundles) {
            Objects.requireNonNull(bundle, "bundle");
            requireCompatible(reference, bundle.metadata());
            for (M3ExecutionProfileObservation observation : bundle.observations()) {
                M3ExecutionProfileObservation previous = union.putIfAbsent(
                        observation.observationId(), observation);
                if (previous != null && !previous.equals(observation)) {
                    throw new IllegalArgumentException(
                            "conflicting observation identity " + observation.observationId());
                }
            }
        }
        return M3ExecutionProfileBundle.create(
                reference.runtimeCommit(),
                reference.workloadSha256(),
                reference.captureTarget(),
                reference.deploymentTarget(),
                PRODUCER,
                PRODUCER_VERSION,
                union.values());
    }

    /**
     * Reads, merges and writes bundle directories.
     *
     * @param output explicit output directory
     * @param inputDirectories one or more input bundle directories
     * @return merged bundle
     * @throws IOException when an input cannot be read or output cannot be written
     */
    public static M3ExecutionProfileBundle mergeDirectories(
            Path output, Collection<Path> inputDirectories) throws IOException {
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(inputDirectories, "inputDirectories");
        List<M3ExecutionProfileBundle> bundles = new ArrayList<>();
        for (Path directory : inputDirectories) {
            bundles.add(M3ExecutionProfileBundle.read(directory));
        }
        M3ExecutionProfileBundle merged = merge(bundles);
        merged.write(output);
        return merged;
    }

    private static void requireCompatible(
            M3ExecutionProfileMetadata reference, M3ExecutionProfileMetadata candidate) {
        if (!reference.formatVersion().equals(candidate.formatVersion())) {
            throw new IllegalArgumentException("incompatible format_version");
        }
        if (!reference.runtimeCommit().equals(candidate.runtimeCommit())) {
            throw new IllegalArgumentException("incompatible runtime_commit");
        }
        if (!reference.workloadSha256().equals(candidate.workloadSha256())) {
            throw new IllegalArgumentException("incompatible workload_sha256");
        }
        if (!reference.captureTarget().equals(candidate.captureTarget())) {
            throw new IllegalArgumentException("incompatible capture_target");
        }
        if (!reference.deploymentTarget().equals(candidate.deploymentTarget())) {
            throw new IllegalArgumentException("incompatible deployment_target");
        }
    }
}
