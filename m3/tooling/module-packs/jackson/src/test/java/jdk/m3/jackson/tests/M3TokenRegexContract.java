// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.jackson.tests;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import jdk.m3.jackson.M3AST;
import jdk.m3.jackson.M3ObjectMapper;
import jdk.m3.jackson.M3TokenRegex;

/** Source-built Jackson AST occurrence/token-NFA regression, no synthetic token list. */
public final class M3TokenRegexContract {
    private static final AtomicInteger CHECKS = new AtomicInteger();

    private M3TokenRegexContract() { }

    private static void eq(Object expected, Object actual, String purpose) {
        CHECKS.incrementAndGet();
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(purpose + ": expected=" + expected + " actual=" + actual);
        }
    }

    private static void valid(M3AST ast, String pattern, long start, long end) {
        eq(new M3TokenRegex.Span(start, end),
                M3TokenRegex.compile(pattern).findFirst(ast, 10_000, 100),
                "token regex " + pattern);
    }

    private static void invalid(String expression) {
        CHECKS.incrementAndGet();
        try {
            M3TokenRegex.compile(expression);
            throw new AssertionError("invalid token pattern admitted: " + expression);
        } catch (IllegalArgumentException expected) {
            // Intentionally refused.
        }
    }

    private static void rejected(Runnable runnable, String purpose) {
        CHECKS.incrementAndGet();
        try {
            runnable.run();
            throw new AssertionError("missing budget refusal: " + purpose);
        } catch (IllegalArgumentException expected) {
            // Bounded by caller.
        }
    }

    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 0) throw new IllegalArgumentException("no command arguments");
        M3ObjectMapper mapper = new M3ObjectMapper();
        M3AST ast = mapper.readAST(
                "{\"id\":17,\"id\":99,\"note\":\"id 17\",\"ary\":[{\"id\":34}]}");

        valid(ast, "[{kind:FIELD}&{name:id}] NUMBER_INT", 1, 3);
        valid(ast, "[{name:id}] [{kind:NUMBER_INT}&{value:/[0-9]+/}]", 1, 3);
        valid(ast, "FIELD NUMBER_INT", 1, 3);
        valid(ast, "(FIELD NUMBER_INT)+", 1, 5);
        valid(ast, "(FIELD NUMBER_INT|STRING)", 1, 3);
        valid(ast, "^ OBJECT", 0, 1);
        valid(ast, "END_OBJECT $", 14, 15);
        valid(ast, "[{kind:FIELD}&{name:note}] [{value:/id 17/}]", 5, 7);
        valid(ast, "[{kind:FIELD}&{name:/id|ary/}]", 1, 2);
        valid(ast, "ARRAY OBJECT FIELD NUMBER_INT END_OBJECT END_ARRAY", 8, 14);
        valid(ast, "[{value:/\\d+/}]", 2, 3);
        valid(ast, "(STRING | NUMBER_INT)", 2, 3);
        valid(ast, "^ (OBJECT .*) $", 0, 15);
        eq(null, M3TokenRegex.compile("[{kind:FIELD}&{name:nothere}] NUMBER_INT")
                .findFirst(ast, 10_000, 100), "missing named field");
        eq(null, M3TokenRegex.compile("[{kind:FIELD}&{name:id}] STRING")
                .findFirst(ast, 10_000, 100), "reject string after numeric field");
        eq(Boolean.TRUE,
                M3TokenRegex.compile("^ OBJECT .* END_OBJECT $").matches(ast, 10_000, 100),
                "full event stream");
        eq(Boolean.FALSE,
                M3TokenRegex.compile("FIELD NUMBER_INT").matches(ast, 10_000, 100),
                "partial pattern must not match entire stream");

        M3AST shared = mapper.readAST("[[1],[1]]");
        valid(shared, "ARRAY NUMBER_INT END_ARRAY", 1, 4);
        eq(Boolean.TRUE,
                M3TokenRegex.compile("^ ARRAY (ARRAY NUMBER_INT END_ARRAY)+ END_ARRAY $")
                        .matches(shared, 10_000, 100),
                "hash-consed children remain separate occurrences");

        M3AST escaped = mapper.readAST("{\"λ\":\"A\\uD83D\\uDE00B\",\"text\":\"λ\"}");
        valid(escaped, "[{kind:FIELD}&{name:/λ/}] STRING", 1, 3);
        valid(escaped, "[{kind:FIELD}&{name:text}] [{value:/λ/}]", 3, 5);
        eq(null, M3TokenRegex.compile("[{value:/λ/}]")
                .findFirst(mapper.readAST("{\"value\":\"literal λ\"}"), 10_000, 100),
                "attribute regex matches whole attribute, not substring");

        for (String bad : new String[] {
                "[", "[{kind:FIELD}", "[{unknown:x}]", "a??", "a**",
                "(abc", "abc)", "[{kind:/[/}]", "[{name:\"unterminated}]", "*"
        }) invalid(bad);
        rejected(() -> M3TokenRegex.compile("FIELD").findFirst(ast, 3, 100), "events");
        rejected(() -> M3TokenRegex.compile("FIELD").findFirst(ast, 100, 2), "depth");
        rejected(() -> M3TokenRegex.compile("FIELD").findFirst(ast, 0, 100), "nonpositive events");
        rejected(() -> M3TokenRegex.compile("FIELD").findFirst(ast, 100, 0), "nonpositive depth");
        invalid("(".repeat(65) + "FIELD" + ")".repeat(65));

        M3TokenRegex concurrent = M3TokenRegex.compile("[{name:/id|note/}] [{value:/[0-9]+/}]");
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread[] threads = new Thread[4];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                try {
                    for (int sample = 0; sample < 40; sample++) {
                        eq(new M3TokenRegex.Span(1, 3), concurrent.findFirst(ast, 100, 100),
                                "concurrent shared compiled plan");
                    }
                } catch (Throwable error) {
                    failure.compareAndSet(null, error);
                }
            }, "m3-token-regex-check-" + i);
            threads[i].start();
        }
        for (Thread thread : threads) thread.join();
        if (failure.get() != null) throw new AssertionError("concurrent executor", failure.get());

        System.out.println("M3_TOKEN_REGEX_CONTRACT_PASS checks=" + CHECKS.get());
    }
}
