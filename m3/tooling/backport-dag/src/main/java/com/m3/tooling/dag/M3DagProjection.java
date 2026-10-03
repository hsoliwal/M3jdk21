// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import java.util.Objects;

/** Immutable generated orchestration artifact bound to one exact canonical DAG root. */
public record M3DagProjection(
        M3DagProjectionFormat format,
        String dagRoot,
        String content) {

    public M3DagProjection {
        format = Objects.requireNonNull(format, "format");
        dagRoot = Objects.requireNonNull(dagRoot, "dagRoot");
        content = Objects.requireNonNull(content, "content");
        if (!dagRoot.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("invalid DAG root");
        }
        if (content.isBlank()) {
            throw new IllegalArgumentException("projection content required");
        }
    }

    public String fileName() {
        return format.fileName();
    }
}
