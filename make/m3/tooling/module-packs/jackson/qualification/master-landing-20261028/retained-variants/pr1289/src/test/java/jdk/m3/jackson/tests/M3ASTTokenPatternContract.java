// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.jackson.tests;

import java.util.Objects;
import jdk.m3.jackson.M3AST;
import jdk.m3.jackson.M3ASTTokenPattern;
import jdk.m3.jackson.M3JsonLimits;
import jdk.m3.jackson.M3ObjectMapper;

/** Standalone Java 21 structural token regex contract; no external testing framework. */
public final class M3ASTTokenPatternContract {
    private static int checks;

    private M3ASTTokenPatternContract() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 0) throw new IllegalArgumentException("no arguments accepted");
        M3ObjectMapper mapper = new M3ObjectMapper(
                new M3JsonLimits(1_000_000L, 100_000, 100_000L, 1_000_000L, 512));
        M3AST ast = mapper.readAST(
                "{\"status\":\"failed\",\"code\":500,\"status\":\"ok\","
                + "\"details\":{\"status\":\"failed\"}}");
        int root = ast.rootAtomIndex();
        equal(4, ast.childCountAt(root), "ordered object field occurrences");

        M3ASTTokenPattern pair = M3ASTTokenPattern.compile(
                "[{kind:FIELD}&{name:status}&{value:failed}] "
                + "[{kind:FIELD}&{name:code}&{value:500}&{valueKind:NUMBER_INT}]");
        span(pair.find(ast, root), 0, 2, "structural field sequence");
        check(!pair.matches(ast, root), "partial match is not full match");
        span(pair.find(ast, root, 1), -1, -1, "from ordinal excludes earlier match");

        M3ASTTokenPattern duplicates = M3ASTTokenPattern.compile(
                "[{name:status}] [] [{name:status}]");
        span(duplicates.find(ast, root), 0, 3, "wildcard and duplicate occurrences");

        M3ASTTokenPattern alternate = M3ASTTokenPattern.compile(
                "([{name:status}]|[{name:code}])+ [{name:details}]");
        check(alternate.matches(ast, root), "alternation and plus consume all direct children");

        M3ASTTokenPattern nestedName = M3ASTTokenPattern.compile(
                "[{name:\"sta\\u0074us\"}&{value:failed}]");
        int details = ast.objectFieldValueAt(root, "details", 0);
        span(nestedName.find(ast, details), 0, 1, "explicit nested container traversal");
        span(nestedName.find(ast, root), 0, 1, "root match retains first occurrence");
        span(nestedName.find(ast, root, 1), -1, -1,
                "nested status never leaks across container boundary");

        M3AST numbers = mapper.readAST("[1,2,3]");
        int array = numbers.rootAtomIndex();
        check(M3ASTTokenPattern.compile("[{kind:NUMBER_INT}]+").matches(numbers, array),
                "array numeric sequence");
        span(M3ASTTokenPattern.compile("[{kind:NUMBER_INT}]").find(numbers, array),
                0, 1, "single array element token");

        M3AST empty = mapper.readAST("[]");
        check(M3ASTTokenPattern.compile("[]*").matches(empty, empty.rootAtomIndex()),
                "empty repeated wildcard");
        span(M3ASTTokenPattern.compile("[]*").find(empty, empty.rootAtomIndex()),
                0, 0, "empty span is explicit");

        M3AST multi = mapper.readAST(
                "[{\"k\":\"value\"},{\"k\":\"value\"},{\"k\":\"value\"}]");
        check(M3ASTTokenPattern.compile("[{kind:OBJECT}]+").matches(multi,
                multi.rootAtomIndex()), "repeated shared-object DAG occurrences retained");
        equal(3, multi.childCountAt(multi.rootAtomIndex()), "three logical occurrences");

        rejects("[{pos:NN}]", "unknown property must be refused");
        rejects("[{name:x}**]", "repeated quantifier refused");
        rejects("[{name:\"\\q\"}]", "bad escape refused");
        rejects("[{name:x}", "unterminated token refused");
        rejects("[]".repeat(64), "bounded automaton states");
        try {
            pair.find(ast, root, 5);
            throw new AssertionError("out-of-range ordinal admitted");
        } catch (IndexOutOfBoundsException expected) {
            checks++;
        }
        try {
            pair.find(ast, ast.childAtomIndexAt(root, 0));
            throw new AssertionError("FIELD admitted as sequence container");
        } catch (IllegalArgumentException expected) {
            checks++;
        }
        System.out.println("M3_AST_TOKEN_REGEX_PASS checks=" + checks);
    }

    private static void span(long actual, int start, int end, String label) {
        if (start == -1 && end == -1) {
            equal(-1L, actual, label);
            return;
        }
        check(actual >= 0L, label + " missing");
        equal(start, M3ASTTokenPattern.startOrdinal(actual), label + " start");
        equal(end, M3ASTTokenPattern.endOrdinal(actual), label + " end");
    }

    private static void rejects(String pattern, String message) {
        try {
            M3ASTTokenPattern.compile(pattern);
            throw new AssertionError(message);
        } catch (IllegalArgumentException expected) {
            checks++;
        }
    }

    private static void equal(Object expected, Object actual, String message) {
        check(Objects.equals(expected, actual),
                message + "; expected=" + expected + " actual=" + actual);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
