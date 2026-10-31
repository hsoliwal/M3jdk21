/*
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0
 * Derived from Synexia 2ab439480ecb0643615e85b42def954dbf3b5627:
 * cpullm-opdag/java/chrome-ai-mechanical/src/main/java/jdk/internal/m3/cpullm/chrome/FileCheckpointRepository.java
 * Adapted for the M3JDK21-owned namespace and qualified summary contracts.
 */
package jdk.m3.llm.chrome;

import static jdk.m3.llm.chrome.MechanicalModel.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

/** Atomic, deterministic, traversal-safe file checkpoint repository. */
public final class FileCheckpointRepository implements CheckpointRepository {
    private final Path root;

    public FileCheckpointRepository(Path root) throws IOException {
        this.root = root.toAbsolutePath().normalize();
        Files.createDirectories(this.root);
    }

    @Override
    public void save(Checkpoint checkpoint) throws IOException {
        Path target = path(checkpoint.runId());
        String content = String.join("\n",
                checkpoint.runId(), checkpoint.stage().name(),
                checkpoint.mechanicalRoot(), checkpoint.candidateRoot()) + "\n";
        Path temp = Files.createTempFile(root, checkpoint.runId() + "-", ".tmp");
        Files.writeString(temp, content, StandardCharsets.UTF_8);
        try {
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public Optional<Checkpoint> load(String runId) throws IOException {
        Path target = path(runId);
        if (!Files.exists(target)) return Optional.empty();
        List<String> lines = Files.readAllLines(target, StandardCharsets.UTF_8);
        if (lines.size() != 4 || !lines.getFirst().equals(runId)) throw new IOException("malformed checkpoint");
        if (!Hashing.isSha256(lines.get(2)) || !Hashing.isSha256(lines.get(3))) throw new IOException("invalid checkpoint roots");
        return Optional.of(new Checkpoint(runId, PipelineStage.valueOf(lines.get(1)), lines.get(2), lines.get(3)));
    }

    private Path path(String runId) {
        if (runId == null || !runId.matches("[A-Za-z0-9._-]{1,128}")) throw new IllegalArgumentException("invalid runId");
        Path path = root.resolve(runId + ".checkpoint").normalize();
        if (!path.startsWith(root)) throw new IllegalArgumentException("checkpoint escaped root");
        return path;
    }
}
