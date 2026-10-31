// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.util.Objects;
import javax.lang.model.element.Modifier;

/**
 * A single bounded projection of an ORIGINAL javac compilation unit into M3ASTIndex.
 * Does not parse, attribute, retain compiler Trees, or duplicate source storage.
 * Input M3File must be the exact source generation supplied to the original task;
 * a URI check cannot independently prove matching content bytes.
 */
public final class M3JavacIndexIngress {
    private M3JavacIndexIngress() { }

    public static M3ASTIndex javac(M3File source, CompilationUnitTree unit,
            Trees trees, M3ASTIngress.Limits limits) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(unit, "original javac unit");
        Objects.requireNonNull(trees, "original javac Trees");
        Objects.requireNonNull(limits, "limits");
        if (source.length() > limits.maxSourceUnits()) {
            throw new IllegalArgumentException("javac source exceeds unit budget");
        }
        if (unit.getSourceFile() == null || !source.uri().equals(unit.getSourceFile().toUri())) {
            throw new IllegalArgumentException("javac source URI does not match M3File");
        }
        SourcePositions positions = trees.getSourcePositions();
        LineMap lines = unit.getLineMap();
        M3ASTIndex.Builder out = M3ASTIndex.builder(source, M3ASTIndex.Domain.JAVAC, limits.maxNodes());
        new TreePathScanner<Void, Integer>() {
            int depth;
            @Override public Void scan(Tree tree, Integer parent) {
                if (tree == null) return null;
                if (depth == limits.maxDepth()) {
                    throw new IllegalArgumentException("javac index depth budget exceeded");
                }
                int begin = checkedOffset(positions.getStartPosition(unit, tree), source.length());
                int finish = checkedOffset(positions.getEndPosition(unit, tree), source.length());
                if (begin == -1 || finish == -1) {
                    begin = M3ASTIndex.NO_RANGE;
                    finish = M3ASTIndex.NO_RANGE;
                }
                if (begin >= 0 && begin > finish) {
                    throw new IllegalArgumentException("descending javac source positions");
                }
                int flags = begin == -1 ? M3ASTIndex.SYNTAX_ONLY : M3ASTIndex.EXACT_RANGE;
                ModifiersTree modifiers = switch (tree) {
                    case ClassTree value -> value.getModifiers();
                    case MethodTree value -> value.getModifiers();
                    case VariableTree value -> value.getModifiers();
                    default -> null;
                };
                if (modifiers != null) {
                    if (modifiers.getFlags().contains(Modifier.STATIC)) flags |= M3ASTIndex.STATIC;
                    if (modifiers.getFlags().contains(Modifier.FINAL)) flags |= M3ASTIndex.FINAL;
                }
                if (tree.getKind() == Tree.Kind.INTERFACE || tree.getKind() == Tree.Kind.ANNOTATION_TYPE) {
                    flags |= M3ASTIndex.INTERFACE;
                }
                int row = out.add(parent == null ? -1 : parent, "JAVAC_" + tree.getKind().name(),
                        begin, finish, flags);
                String name = switch (tree) {
                    case ClassTree value -> value.getSimpleName().toString();
                    case MethodTree value -> value.getName().toString();
                    case VariableTree value -> value.getName().toString();
                    case IdentifierTree value -> value.getName().toString();
                    case MemberSelectTree value -> value.getIdentifier().toString();
                    default -> null;
                };
                String literal = tree instanceof LiteralTree value
                        ? String.valueOf(value.getValue()) : null;
                int line = -1, lastLine = -1;
                if (lines != null && begin >= 0) {
                    line = checkedLine(lines.getLineNumber(begin));
                    lastLine = checkedLine(lines.getLineNumber(Math.max(begin, finish - 1)));
                }
                out.annotateJavac(row, line, lastLine, name, literal);
                depth++;
                try {
                    return super.scan(tree, row);
                } finally {
                    depth--;
                }
            }
        }.scan(unit, null);
        return out.seal();
    }

    private static int checkedOffset(long position, int size) {
        if (position == -1L) return -1;
        if (position < 0L || position > size) {
            throw new IllegalArgumentException("javac source offset out of bounds");
        }
        return (int) position;
    }

    private static int checkedLine(long value) {
        if (value < 1L || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("unavailable javac line map");
        }
        return (int) value;
    }
}
