/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

import java.io.IOException;
import java.nio.file.Path;

/** Selects a backend and applies verified incremental reuse. */
public final class M3ImageBuilder {
    private M3ImageBuilder() {
    }

    /** Builds or restores an image. */
    public static M3ImageBuildResult build(M3ImagePlan plan, M3JdkTools tools, M3BackendKind backendKind,
                                           M3BuildOptions options, Path output)
            throws IOException, InterruptedException {
        String cacheKey = M3IncrementalStore.key(plan, backendKind, tools, options);
        if (options.cacheDirectory() != null) {
            M3ImageBuildResult restored = M3IncrementalStore.restore(
                    options.cacheDirectory(), cacheKey, output, backendKind);
            if (restored != null) {
                return restored;
            }
        }
        M3ImageBackend backend = switch (backendKind) {
            case JDK21_SHIFTED -> new M3Jdk21ShiftedBackend();
            case NATIVE_IMAGE -> new M3NativeImageBackend();
        };
        M3ImageBuildResult result = backend.build(plan, tools, options, output);
        if (options.cacheDirectory() != null) {
            M3IncrementalStore.publish(options.cacheDirectory(), cacheKey, result.directory());
        }
        return result;
    }
}
