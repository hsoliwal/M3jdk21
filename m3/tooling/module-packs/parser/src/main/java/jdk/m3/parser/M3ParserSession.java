// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser;

import java.util.Objects;
import jdk.m3.parser.grammar.M3GrammarValidation;

/**
 * One M3 grammar source generation, one original parse result, any number of
 * validators/AST consumers. Never lex or recognize the same source a second
 * time merely because a second consumer requests facts.
 *
 * <p>The original M3Parser still exposes its independent cheap validate()
 * operation for callers who require only recognition; when callers expect
 * validation AND a tree, this session computes one full parse on demand and
 * shares the exact result and its existing token/source owner.
 */
public final class M3ParserSession implements AutoCloseable {
    private M3Parser owner;
    private M3File source;
    private M3ParseResult result;
    private RuntimeException failure;
    private int executions;
    private boolean closed;

    M3ParserSession(M3Parser owner, M3File source) {
        this.owner = Objects.requireNonNull(owner, "original M3 grammar parser");
        this.source = Objects.requireNonNull(source, "original source generation");
    }

    public synchronized M3ParseResult parseOnce() {
        requireOpen();
        if (result != null) return result;
        if (failure != null) throw failure;
        executions++;
        try {
            result = owner.parse(source);
            return result;
        } catch (RuntimeException error) {
            failure = error;
            throw error;
        } finally {
            // No second parser-owner reference is needed after admission.
            owner = null;
        }
    }

    /** Validation is part of the existing parse result, not another parse pass. */
    public synchronized M3GrammarValidation validate() {
        return parseOnce().syntax().validation();
    }

    public synchronized int parseExecutions() { return executions; }
    public synchronized boolean hasResult() { return result != null; }
    public synchronized M3File source() {
        requireOpen();
        return source;
    }

    private void requireOpen() {
        if (closed) throw new IllegalStateException("M3 parser session is closed");
    }

    /** Release the session's retained source and AST; caller snapshots remain valid. */
    @Override public synchronized void close() {
        closed = true;
        owner = null;
        source = null;
        result = null;
        failure = null;
    }
}
