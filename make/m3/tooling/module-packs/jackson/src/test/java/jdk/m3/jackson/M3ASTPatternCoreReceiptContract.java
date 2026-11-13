// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package jdk.m3.jackson;

/**
 * Standalone contract for the source-sealed #1289 direct-child and token-event matchers.
 * Does not invoke Jackson ObjectMapper or add any runtime dependency.
 */
public final class M3ASTPatternCoreReceiptContract {
    private static int checks;

    private M3ASTPatternCoreReceiptContract() { }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static void rejects(Runnable action, String label) {
        checks++;
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("accepted invalid " + label);
    }

    public static void main(String[] args) {
        M3ASTBuilder builder = new M3ASTBuilder(M3JsonLimits.DEFAULT);
        int scalar = builder.leaf("NUMBER_INT", null, "42");
        int field = builder.field("id", scalar);
        int root = builder.atom("OBJECT", null, null, new int[]{field, field});
        M3AST ast = builder.build(root);

        var fields = M3ASTTokenPattern.compile(
                "[{kind:FIELD}&{name:id}&{value:42}&{valueKind:NUMBER_INT}]+");
        check(fields.matches(ast, root), "repeated direct field occurrences");
        long span = M3ASTTokenPattern.compile("[{name:id}]").find(ast, root);
        check(M3ASTTokenPattern.startOrdinal(span) == 0, "first ordinal");
        check(M3ASTTokenPattern.endOrdinal(span) == 1, "exclusive end ordinal");
        check(!M3ASTTokenPattern.compile("[{kind:STRING}]").matches(ast, root),
                "direct-child match cannot leak through field to scalar");
        rejects(() -> M3ASTTokenPattern.compile("[{pos:NN}]"), "unknown predicate");

        var exact = M3TokenRegex.compile("^ OBJECT (FIELD NUMBER_INT)+ END_OBJECT $");
        check(exact.matches(ast, 128, 8), "ordered duplicate token occurrences");
        check(!M3TokenRegex.compile("FIELD NUMBER_INT").matches(ast, 128, 8),
                "partial event match is not a full match");
        check(new M3TokenRegex.Span(1, 3).equals(
                M3TokenRegex.compile("FIELD NUMBER_INT").findFirst(ast, 128, 8)),
                "first token occurrence span");
        rejects(() -> exact.matches(ast, 1, 8), "token budget");
        rejects(() -> exact.matches(ast, 128, 1), "depth budget");
        System.out.println("M3 AST PATTERN PASS checks=" + checks);
    }
}
