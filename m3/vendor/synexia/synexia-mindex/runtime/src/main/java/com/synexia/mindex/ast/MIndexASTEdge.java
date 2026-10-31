// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.ast;

import java.util.Objects;

/** Labelled child at the construction boundary. Its position in the list is significant. */
public record MIndexASTEdge(String role, MIndexAST child) {
    public MIndexASTEdge {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(child, "child");
    }
}
