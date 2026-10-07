// SPDX-License-Identifier: Apache-2.0
package com.m3.tools.modulepack;

import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Immutable archive inventory. Pattern/IOP role: InventoryRecord.
 * A checksum records bytes; it does not certify license, ABI or compatibility.
 *
 * @param path canonical inspected archive
 * @param descriptor explicit module descriptor
 * @param sha256 archive content digest
 * @param kind JAR or JMOD
 * @param nativeEntries native-looking archive entries, not ABI validation
 * @param resources non-class entries, including legal notices
 */
public record M3ModuleArtifact(Path path, ModuleDescriptor descriptor, String sha256,
        String kind, List<String> nativeEntries, List<String> resources) {
    /** Snapshot mutable collections and require complete inventory facts. */
    public M3ModuleArtifact {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(sha256, "sha256");
        Objects.requireNonNull(kind, "kind");
        nativeEntries = List.copyOf(nativeEntries);
        resources = List.copyOf(resources);
    }
}
