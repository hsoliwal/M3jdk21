/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.compiler.profiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Immutable, strictly validated M3 execution-profile bundle.
 *
 * @param metadata content-addressed bundle metadata
 * @param observations immutable observations in strict identity order
 */
public record M3ExecutionProfileBundle(
        M3ExecutionProfileMetadata metadata,
        List<M3ExecutionProfileObservation> observations) {

    /** Validates sort order, uniqueness and the bundle identity. */
    public M3ExecutionProfileBundle {
        Objects.requireNonNull(metadata, "metadata");
        Objects.requireNonNull(observations, "observations");
        observations = List.copyOf(observations);
        String previous = null;
        for (M3ExecutionProfileObservation observation : observations) {
            Objects.requireNonNull(observation, "observation");
            if (previous != null && previous.compareTo(observation.observationId()) >= 0) {
                throw new IllegalArgumentException("observations must be strictly sorted by observation_id");
            }
            previous = observation.observationId();
        }
        String expected = M3ExecutionProfileFormat.bundleId(
                metadata.runtimeCommit(), metadata.workloadSha256(), metadata.captureTarget(),
                metadata.deploymentTarget(), metadata.producer(), metadata.producerVersion(), observations);
        if (!expected.equals(metadata.bundleId())) {
            throw new IllegalArgumentException("bundle_id does not match canonical metadata and observations");
        }
    }

    /**
     * Creates a canonical bundle from unordered observations.
     *
     * <p>Exact duplicate observations collapse. A conflicting duplicate identity is refused.</p>
     *
     * @param runtimeCommit immutable runtime source commit
     * @param workloadSha256 lowercase workload SHA-256
     * @param captureTarget capture target token
     * @param deploymentTarget deployment target token
     * @param producer producer identity
     * @param producerVersion producer version
     * @param inputObservations observations in any order
     * @return canonical content-addressed bundle
     */
    public static M3ExecutionProfileBundle create(
            String runtimeCommit,
            String workloadSha256,
            String captureTarget,
            String deploymentTarget,
            String producer,
            String producerVersion,
            Collection<M3ExecutionProfileObservation> inputObservations) {
        Objects.requireNonNull(inputObservations, "inputObservations");
        Map<String, M3ExecutionProfileObservation> sorted = new TreeMap<>();
        for (M3ExecutionProfileObservation observation : inputObservations) {
            Objects.requireNonNull(observation, "observation");
            M3ExecutionProfileObservation previous = sorted.putIfAbsent(observation.observationId(), observation);
            if (previous != null && !previous.equals(observation)) {
                throw new IllegalArgumentException("conflicting observation identity " + observation.observationId());
            }
        }
        List<M3ExecutionProfileObservation> observations = List.copyOf(sorted.values());
        String bundleId = M3ExecutionProfileFormat.bundleId(runtimeCommit, workloadSha256, captureTarget,
                deploymentTarget, producer, producerVersion, observations);
        M3ExecutionProfileMetadata metadata = new M3ExecutionProfileMetadata(
                bundleId, M3ExecutionProfileFormat.FORMAT_VERSION, runtimeCommit, workloadSha256, captureTarget,
                deploymentTarget, producer, producerVersion);
        return new M3ExecutionProfileBundle(metadata, observations);
    }

    /**
     * Reads and strictly validates one bundle directory.
     *
     * @param directory directory containing {@code BUNDLE.tsv} and {@code OBSERVATIONS.tsv}
     * @return validated immutable bundle
     * @throws IOException when a normative file cannot be read
     */
    public static M3ExecutionProfileBundle read(Path directory) throws IOException {
        Objects.requireNonNull(directory, "directory");
        Path root = directory.toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("bundle directory does not exist: " + root);
        }
        Path bundlePath = root.resolve("BUNDLE.tsv");
        Path observationsPath = root.resolve("OBSERVATIONS.tsv");
        List<String> bundleLines = M3ExecutionProfileFormat.readStrictLines(
                bundlePath, M3ExecutionProfileFormat.BUNDLE_HEADER);
        if (bundleLines.size() != 2) {
            throw M3ExecutionProfileFormat.invalid(bundlePath, 1, "BUNDLE.tsv must contain exactly one data row");
        }
        String[] metadataCells = M3ExecutionProfileFormat.cells(bundleLines.get(1), 8, bundlePath, 2);
        M3ExecutionProfileMetadata metadata = new M3ExecutionProfileMetadata(
                metadataCells[0], metadataCells[1], metadataCells[2], metadataCells[3], metadataCells[4],
                metadataCells[5], metadataCells[6], metadataCells[7]);

        List<String> observationLines = M3ExecutionProfileFormat.readStrictLines(
                observationsPath, M3ExecutionProfileFormat.OBSERVATIONS_HEADER);
        List<M3ExecutionProfileObservation> observations = new ArrayList<>();
        String previous = null;
        for (int lineNumber = 2; lineNumber <= observationLines.size(); lineNumber++) {
            String[] cells = M3ExecutionProfileFormat.cells(
                    observationLines.get(lineNumber - 1), 8, observationsPath, lineNumber);
            String observationId = cells[0];
            if (previous != null && previous.compareTo(observationId) >= 0) {
                throw M3ExecutionProfileFormat.invalid(
                        observationsPath, lineNumber, "rows must be strictly sorted by observation_id");
            }
            previous = observationId;
            M3ExecutionProfileObservation observation;
            try {
                observation = new M3ExecutionProfileObservation(
                        observationId,
                        M3ExecutionProfileFormat.enumValue(
                                M3ExecutionProfileKind.class, cells[1], observationsPath, lineNumber),
                        cells[2],
                        cells[3],
                        M3ExecutionProfileFormat.enumValue(
                                M3ExecutionProfilePhase.class, cells[4], observationsPath, lineNumber),
                        M3ExecutionProfileFormat.parseCount(cells[5]),
                        cells[6],
                        cells[7]);
            } catch (IllegalArgumentException exception) {
                throw M3ExecutionProfileFormat.invalid(observationsPath, lineNumber, exception.getMessage());
            }
            observations.add(observation);
        }
        try {
            return new M3ExecutionProfileBundle(metadata, observations);
        } catch (IllegalArgumentException exception) {
            throw M3ExecutionProfileFormat.invalid(bundlePath, 2, exception.getMessage());
        }
    }

    /**
     * Writes this bundle atomically where supported.
     *
     * <p>An empty or absent directory is materialized. A nonempty directory is accepted only when its normative
     * files already describe this exact bundle; in that case the operation is a no-op fixed point.</p>
     *
     * @param directory explicit output directory
     * @throws IOException when the bundle cannot be written
     */
    public void write(Path directory) throws IOException {
        Objects.requireNonNull(directory, "directory");
        Path root = directory.toAbsolutePath().normalize();
        if (Files.exists(root) && !Files.isDirectory(root)) {
            throw new IllegalArgumentException("output path is not a directory: " + root);
        }
        if (Files.isDirectory(root)) {
            boolean nonempty;
            try (var stream = Files.list(root)) {
                nonempty = stream.findAny().isPresent();
            }
            if (nonempty) {
                Path bundlePath = root.resolve("BUNDLE.tsv");
                Path observationsPath = root.resolve("OBSERVATIONS.tsv");
                if (!Files.isRegularFile(bundlePath) || !Files.isRegularFile(observationsPath)) {
                    throw new IllegalStateException("refusing nonempty output directory without both normative files");
                }
                M3ExecutionProfileBundle existing = read(root);
                if (!equals(existing)) {
                    throw new IllegalStateException("refusing to overwrite a different execution-profile bundle");
                }
                return;
            }
        } else {
            Files.createDirectories(root);
        }
        String bundleText = M3ExecutionProfileFormat.BUNDLE_HEADER + "\n" + metadata.row() + "\n";
        StringBuilder observationsText = new StringBuilder(M3ExecutionProfileFormat.OBSERVATIONS_HEADER).append('\n');
        for (M3ExecutionProfileObservation observation : observations) {
            observationsText.append(observation.row()).append('\n');
        }
        writeAtomic(root.resolve("BUNDLE.tsv"), bundleText);
        writeAtomic(root.resolve("OBSERVATIONS.tsv"), observationsText.toString());
    }

    private static void writeAtomic(Path target, String content) throws IOException {
        Path temporary = Files.createTempFile(target.getParent(), "." + target.getFileName() + ".", ".tmp");
        boolean moved = false;
        try {
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target);
            }
            moved = true;
        } finally {
            if (!moved) {
                Files.deleteIfExists(temporary);
            }
        }
    }
}
