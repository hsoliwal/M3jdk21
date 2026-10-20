// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser.test;

import java.net.URI;
import jdk.m3.parser.M3File;
import jdk.m3.parser.M3ParseResult;
import jdk.m3.parser.M3Parser;
import jdk.m3.parser.M3ParserSession;

/** One real generic-grammar parse across validation and downstream AST users. */
public final class M3ParserSessionContract {
    private static int checks;
    private M3ParserSessionContract() { }

    private static void check(boolean accepted, String message) {
        checks++;
        if (!accepted) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 0) throw new IllegalArgumentException("no arguments");
        M3Parser parser = new M3Parser(M3ParserFixtures.expression());
        String contents = "12 + 30";
        M3File file = M3File.of(URI.create("memory:/expression"), contents);
        M3ParseResult captured;
        try (M3ParserSession session = parser.session(file)) {
            check(!session.hasResult() && session.parseExecutions() == 0,
                    "session creation does not lex/parse eagerly");
            check(session.source() == file, "one original immutable source");
            check(session.validate().conforms(), "validation from first actual grammar parse");
            captured = session.parseOnce();
            check(captured == session.parseOnce(), "two AST consumers share exact result object");
            check(session.validate() == captured.syntax().validation(),
                    "cached validation is original parser result, not a second recognizer");
            check(session.parseExecutions() == 1 && session.hasResult(),
                    "one grammar parse across three consumers");
            check(captured.source() == file && captured.requireTree().tokens().source() == contents,
                    "original source and token buffer identity");
        }
        check(captured.conforms() && captured.requireTree() != null,
                "snapshot remains immutable after session is released");
        M3ParserSession closed = parser.session(file);
        closed.close();
        try {
            closed.parseOnce();
            throw new AssertionError("closed source session allowed access");
        } catch (IllegalStateException expected) {
            checks++;
        }
        try (M3ParserSession other = parser.session(
                M3File.of(URI.create("memory:/invalid"), "12 +"))) {
            check(!other.validate().conforms(), "invalid grammar admitted only as nonconforming");
            check(other.parseOnce() == other.parseOnce() && other.parseExecutions() == 1,
                    "even a nonconforming source parses only once");
        }
        System.out.println("M3_PARSER_SINGLE_PASS_PASS checks=" + checks);
    }
}
