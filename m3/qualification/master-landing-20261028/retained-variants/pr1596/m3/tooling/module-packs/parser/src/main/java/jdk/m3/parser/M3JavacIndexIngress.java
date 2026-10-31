// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package jdk.m3.parser;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.util.Objects;

/**
 * One bounded preorder walk of javac's original compilation tree, into the
 * existing M3ASTIndex primitive lanes. No duplicate AST or compiler is created.
 * Positionless compiler nodes retain unknown ranges, never invented offsets.
 */
final class M3JavacIndexIngress {
    private M3JavacIndexIngress() { }

    static M3ASTIndex capture(M3File source, CompilationUnitTree unit,
            Trees trees, M3ASTIngress.Limits limits) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(trees, "trees");
        Objects.requireNonNull(limits, "limits");
        if (source.length() > limits.maxSourceUnits()) {
            throw new IllegalArgumentException("javac source exceeds UTF-16 budget");
        }
        if (unit.getSourceFile() == null || !source.uri().equals(unit.getSourceFile().toUri())) {
            throw new IllegalArgumentException("javac unit URI differs from M3File source");
        }
        SourcePositions positions = trees.getSourcePositions();
        M3ASTIndex.Builder output = M3ASTIndex.builder(
                source, M3ASTIndex.Domain.JAVAC, limits.maxNodes());
        new TreePathScanner<Void, Integer>() {
            private int depth;
            @Override public Void scan(Tree tree, Integer parent) {
                if (tree == null) return null;
                if (++depth > limits.maxDepth()) {
                    depth--;
                    throw new IllegalArgumentException("javac tree depth budget exceeded");
                }
                try {
                    int start = position(positions.getStartPosition(unit, tree), source.length());
                    int end = position(positions.getEndPosition(unit, tree), source.length());
                    boolean exact = start >= 0 && end >= start;
                    if (!exact) { start = M3ASTIndex.NO_RANGE; end = M3ASTIndex.NO_RANGE; }
                    int flags = M3ASTIndex.SYNTAX_ONLY
                            | (exact ? M3ASTIndex.EXACT_RANGE : M3ASTIndex.SOURCE_UNVERIFIED);
                    int row = output.add(parent, tree.getKind().name(), start, end, flags);
                    return super.scan(tree, row);
                } finally {
                    depth--;
                }
            }
        }.scan(unit, -1);
        return output.seal();
    }

    private static int position(long offset, int sourceLength) {
        if (offset == -1) return -1;
        if (offset < 0 || offset > sourceLength) {
            throw new IllegalArgumentException("javac range outside source");
        }
        return (int) offset;
    }
}
