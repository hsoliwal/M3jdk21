// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.ast.partial;

import com.synexia.mindex.MIndexProgressMonitor;

/**
 * Parser/compiler adapter boundary. Implementations may use JDT, javac, a SQL grammar, tree-sitter,
 * or another authoritative parser, but only immutable MIndex projections cross this boundary.
 */
@FunctionalInterface
public interface MIndexPartialAstAdapter {
    MIndexPartialAstAttempt parse(
            MIndexPartialAstKey key,
            MIndexProgressMonitor monitor);
}
