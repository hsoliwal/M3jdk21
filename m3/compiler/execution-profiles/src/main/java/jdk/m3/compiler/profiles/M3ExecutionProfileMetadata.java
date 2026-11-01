/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.compiler.profiles;

/**
 * Immutable metadata for one execution-profile bundle.
 *
 * @param bundleId content-derived {@code m3p1:SHA-256} identity
 * @param formatVersion exact profile format version
 * @param runtimeCommit immutable runtime source commit
 * @param workloadSha256 lowercase SHA-256 of the workload definition
 * @param captureTarget normalized target on which observations were captured
 * @param deploymentTarget normalized target for which the profile is intended
 * @param producer stable producer identity
 * @param producerVersion stable producer version
 */
public record M3ExecutionProfileMetadata(
        String bundleId,
        String formatVersion,
        String runtimeCommit,
        String workloadSha256,
        String captureTarget,
        String deploymentTarget,
        String producer,
        String producerVersion) {

    /** Validates all metadata fields except the cross-file bundle hash relation. */
    public M3ExecutionProfileMetadata {
        M3ExecutionProfileFormat.requireBundleId(bundleId);
        if (!M3ExecutionProfileFormat.FORMAT_VERSION.equals(formatVersion)) {
            throw new IllegalArgumentException("format_version must be " + M3ExecutionProfileFormat.FORMAT_VERSION);
        }
        M3ExecutionProfileFormat.requireRuntimeCommit(runtimeCommit);
        M3ExecutionProfileFormat.requireSha256("workload_sha256", workloadSha256);
        M3ExecutionProfileFormat.requireTarget("capture_target", captureTarget);
        M3ExecutionProfileFormat.requireTarget("deployment_target", deploymentTarget);
        M3ExecutionProfileFormat.requireProducer(producer);
        M3ExecutionProfileFormat.requireVersion(producerVersion);
    }

    String row() {
        return String.join("\t", bundleId, formatVersion, runtimeCommit, workloadSha256, captureTarget,
                deploymentTarget, producer, producerVersion);
    }
}
