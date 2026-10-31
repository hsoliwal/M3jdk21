// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.ast.partial;

import com.synexia.mindex.ast.MIndexAST;
import com.synexia.mindex.ast.MIndexASTSnapshot;
import java.util.Objects;

/** Immutable parser-neutral partial-AST result backed by the canonical MIndex AST snapshot owner. */
public record MIndexPartialAstProjection(
        MIndexPartialAstKey key,
        MIndexASTSnapshot snapshot,
        int rootId,
        long retainedBytes,
        int flags) {

    public static final int RECOVERED = 1;
    public static final int MALFORMED = 1 << 1;
    public static final int BINDINGS_RESOLVED = 1 << 2;
    public static final int REWRITE_SAFE = 1 << 3;
    public static final int ERROR_DIAGNOSTIC = 1 << 4;
    private static final int KNOWN_FLAGS =
            RECOVERED | MALFORMED | BINDINGS_RESOLVED | REWRITE_SAFE | ERROR_DIAGNOSTIC;

    public MIndexPartialAstProjection {
        key = Objects.requireNonNull(key, "key");
        snapshot = Objects.requireNonNull(snapshot, "snapshot");
        if (rootId < 0 || rootId >= snapshot.size()) {
            throw new IllegalArgumentException("partial AST root outside snapshot");
        }
        if (retainedBytes < 0) throw new IllegalArgumentException("negative retainedBytes");
        if ((flags & ~KNOWN_FLAGS) != 0) throw new IllegalArgumentException("unknown projection flags");
    }

    public MIndexAST root() {
        return snapshot.ref(rootId);
    }

    public String structureSha256() {
        return root().fingerprint();
    }

    public boolean recovered() {
        return (flags & RECOVERED) != 0;
    }

    public boolean malformed() {
        return (flags & MALFORMED) != 0;
    }

    public boolean bindingsResolved() {
        return (flags & BINDINGS_RESOLVED) != 0;
    }

    public boolean errorDiagnostic() {
        return (flags & ERROR_DIAGNOSTIC) != 0;
    }

    /**
     * A parser/adapter may nominate a projection for rewriting, but recovery/malformed evidence
     * always vetoes that nomination. Host-side exact-preimage/compile/test gates remain required.
     */
    public boolean rewriteSafe() {
        return (flags & REWRITE_SAFE) != 0
                && !recovered() && !malformed() && !errorDiagnostic();
    }
}
