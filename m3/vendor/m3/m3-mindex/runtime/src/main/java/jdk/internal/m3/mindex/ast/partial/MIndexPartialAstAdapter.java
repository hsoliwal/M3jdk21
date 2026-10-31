// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.mindex.ast.partial;

import jdk.internal.m3.mindex.MIndexProgressMonitor;

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
