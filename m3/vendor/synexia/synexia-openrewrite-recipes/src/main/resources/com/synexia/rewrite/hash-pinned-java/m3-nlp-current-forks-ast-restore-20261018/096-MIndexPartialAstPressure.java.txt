// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.ast.partial;

/** Heap/cache pressure policy for reconstructible partial-AST projections. */
public enum MIndexPartialAstPressure {
    NORMAL(100, true),
    ELEVATED(70, true),
    CRITICAL(30, false);

    private final int retainedPercent;
    private final boolean admit;

    MIndexPartialAstPressure(int retainedPercent, boolean admit) {
        this.retainedPercent = retainedPercent;
        this.admit = admit;
    }

    int retainedPercent() {
        return retainedPercent;
    }

    boolean admit() {
        return admit;
    }
}
