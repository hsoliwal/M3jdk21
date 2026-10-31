/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.parser;

import java.util.Objects;
import jdk.m3.parser.grammar.M3GrammarParseResult;
import jdk.m3.parser.grammar.M3GrammarParseTree;

/** A detached grammar result bound to its source snapshot, including failed conformance. */
public final class M3ParseResult {
    private final M3File source;
    private final M3GrammarParseResult syntax;

    M3ParseResult(M3File source, M3GrammarParseResult syntax) {
        this.source = Objects.requireNonNull(source, "source");
        this.syntax = Objects.requireNonNull(syntax, "syntax");
        syntax.tree().ifPresent(tree -> {
            if (tree.tokens().source() != source.text()) {
                throw new IllegalArgumentException("tree does not reference the source String owner");
            }
        });
    }

    public M3File source() { return source; }
    public M3GrammarParseResult syntax() { return syntax; }

    /** Acceptance honors the grammar's strictness policy; inspect conforms for exact recognition. */
    public boolean accepted() { return syntax.accepted(); }
    public boolean conforms() { return syntax.conforms(); }
    public M3GrammarParseTree requireTree() { return syntax.requireTree(); }
}
