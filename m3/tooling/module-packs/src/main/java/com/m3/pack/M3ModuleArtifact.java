// SPDX-License-Identifier: Apache-2.0
package com.m3.pack;

import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Immutable inventory value; records observed facts, not distribution approval. */
public record M3ModuleArtifact(
        Path path, String sha256, ModuleDescriptor descriptor,
        int maximumClassVersion, List<String> nativeEntries) {
    /** Seals caller-owned collections and requires complete identity evidence. */
    public M3ModuleArtifact {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(sha256, "sha256");
        Objects.requireNonNull(descriptor, "descriptor");
        nativeEntries = List.copyOf(nativeEntries);
    }
}
