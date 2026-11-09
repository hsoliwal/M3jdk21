/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

import java.io.IOException;
import java.nio.file.Path;

/** Isolated image-builder backend. */
public interface M3ImageBackend {
    /** Backend identity. */
    M3BackendKind kind();

    /** Builds a complete image into a fresh output directory. */
    M3ImageBuildResult build(M3ImagePlan plan, M3JdkTools tools, M3BuildOptions options, Path output)
            throws IOException, InterruptedException;
}
