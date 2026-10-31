/*
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0
 * Derived from Synexia 2ab439480ecb0643615e85b42def954dbf3b5627:
 * cpullm-opdag/java/chrome-ai-mechanical/src/main/java/jdk/internal/m3/cpullm/chrome/CheckpointRepository.java
 * Adapted for the M3JDK21-owned namespace and qualified summary contracts.
 */
package jdk.m3.llm.chrome;

import static jdk.m3.llm.chrome.MechanicalModel.*;

import java.io.IOException;
import java.util.Optional;

/** Repository pattern for restartable mechanical run checkpoints. */
public interface CheckpointRepository {
    void save(Checkpoint checkpoint) throws IOException;
    Optional<Checkpoint> load(String runId) throws IOException;

    static CheckpointRepository noop() {
        return new CheckpointRepository() {
            @Override public void save(Checkpoint checkpoint) {}
            @Override public Optional<Checkpoint> load(String runId) { return Optional.empty(); }
        };
    }
}
