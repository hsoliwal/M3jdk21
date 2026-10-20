/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.parser;

import java.util.Objects;
import jdk.m3.parser.grammar.M3Grammar;
import jdk.m3.parser.grammar.M3GrammarLexer;
import jdk.m3.parser.grammar.M3GrammarLimits;
import jdk.m3.parser.grammar.M3GrammarParser;
import jdk.m3.parser.grammar.M3GrammarRecognizer;
import jdk.m3.parser.grammar.M3GrammarValidation;
import jdk.m3.parser.grammar.M3TokenStream;

/** Reusable immutable parser configuration; every operation creates its own bounded session. */
public final class M3Parser {
    private final M3Grammar grammar;
    private final M3GrammarLimits limits;

    public M3Parser(M3Grammar grammar) {
        this(grammar, M3GrammarLimits.defaults());
    }

    public M3Parser(M3Grammar grammar, M3GrammarLimits limits) {
        this.grammar = Objects.requireNonNull(grammar, "grammar");
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    /** Share a single grammar parse and its source/AST across interested consumers. */
    public M3ParserSession session(M3File source) {
        return new M3ParserSession(this, Objects.requireNonNull(source, "source"));
    }

    public M3Grammar grammar() { return grammar; }
    public M3GrammarLimits limits() { return limits; }

    public M3TokenStream lex(M3File source) {
        Objects.requireNonNull(source, "source");
        return new M3GrammarLexer(grammar.lexerProfile(), limits).lex(source.text());
    }

    /** Recognize without allocating a parse-tree backpointer arena. */
    public M3GrammarValidation validate(M3File source) {
        Objects.requireNonNull(source, "source");
        return new M3GrammarRecognizer(grammar, limits).validate(source.text());
    }

    /** Retain one deterministic derivation plus the exact immutable source. */
    public M3ParseResult parse(M3File source) {
        Objects.requireNonNull(source, "source");
        return new M3ParseResult(source, new M3GrammarParser(grammar, limits).parse(source.text()));
    }
}
