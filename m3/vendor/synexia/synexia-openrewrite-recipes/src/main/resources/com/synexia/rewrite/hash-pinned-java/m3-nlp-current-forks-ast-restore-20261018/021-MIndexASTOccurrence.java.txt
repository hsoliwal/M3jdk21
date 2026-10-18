// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.compiler.ast;

import com.synexia.mindex.ast.MIndexAST;
import java.util.Objects;

/** One compiler-tree occurrence, separate from shared atom identity. End offsets are exclusive. */
public record MIndexASTOccurrence(MIndexAST atom, int start, int end) {
    public MIndexASTOccurrence {
        Objects.requireNonNull(atom, "atom");
        if (!((start == -1 && end == -1) || (start >= 0 && end >= start))) {
            throw new IllegalArgumentException("invalid source span");
        }
    }
    public boolean hasSourceSpan() { return start >= 0; }
}
