/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

import java.nio.file.Path;

/** Verified image build result. */
public record M3ImageBuildResult(Path directory, String imageSha256, M3BackendKind backend,
                                 String backendStatus, boolean cacheHit) {
    /** Validates a result. */
    public M3ImageBuildResult {
        directory = directory.toAbsolutePath().normalize();
        imageSha256 = M3Hashing.requireSha256(imageSha256, "imageSha256");
        if (backend == null || backendStatus == null || backendStatus.isBlank()) {
            throw new IllegalArgumentException("Invalid build result");
        }
    }

    /** Returns a copy with cache-hit status. */
    public M3ImageBuildResult withCacheHit(boolean value) {
        return new M3ImageBuildResult(directory, imageSha256, backend, backendStatus, value);
    }
}
