// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package jdk.m3.parser.test;

import java.net.URI;
import java.util.Arrays;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3File;

/** Exact-source behavioral oracle: String representatives, occurrence geometry and refusal order. */
public final class M3ASTKindDictionarySelfTest {
    private static int checks;
    private M3ASTKindDictionarySelfTest() { }

    public static void main(String[] args) {
        identityAndGeometry();
        for (int size : new int[] { 1, 2, 15, 16, 17, 31, 32, 33, 63, 64, 65, 255,
                256, 257, 1024, 2048, 4096 }) {
            String[] names = new String[size];
            for (int i = 0; i < size; i++) names[i] = new String("Kind" + i);
            dictionary(names);
        }
        String[] collisions = new String[4096];
        for (int i = 0; i < collisions.length; i++) {
            StringBuilder value = new StringBuilder(24);
            for (int bit = 0; bit < 12; bit++) value.append((i & (1 << bit)) == 0 ? "Aa" : "BB");
            collisions[i] = value.toString();
            equal(collisions[0].hashCode(), collisions[i].hashCode(), "collision fixture");
        }
        dictionary(collisions);
        dictionary(new String[] { new String(new char[] { 0 }), new String(new char[] { 0, 0 }),
                new String(new char[] { 0, 0, 0 }), "Aa", "BB",
                new String(new char[] { (char) 0xd800 }), new String(new char[] { (char) 0xdc00 }),
                new String(new char[] { (char) 0xd83d, (char) 0xde03 }), "kind".repeat(256) });
        refusals();
        System.out.println("M3_AST_KIND_DICTIONARY_PASS checks=" + checks
                + " dictionary_sizes=17 collisions=4096 zero_hashes=3");
    }

    private static M3File source(String text) {
        return M3File.of(URI.create("memory:/kind-dictionary.java"), text);
    }

    private static M3ASTIndex.Builder builder(M3File file, int max) {
        return M3ASTIndex.builder(file, M3ASTIndex.Domain.ECLIPSE_JDT, max);
    }

    private static void dictionary(String[] names) {
        M3File file = source("0123456789");
        M3ASTIndex.Builder build = builder(file, names.length * 3 + 8);
        for (int i = 0; i < names.length; i++) {
            equal(i, build.add(i == 0 ? -1 : 0, names[i], 0, 10, M3ASTIndex.EXACT_RANGE),
                    "first occurrence");
        }
        for (int i = names.length - 1; i >= 0; i--) {
            build.add(0, new String(names[i]), 1, 9, M3ASTIndex.EXACT_RANGE);
        }
        if (names.length == 4096) {
            fails(IllegalArgumentException.class, "AST kind dictionary exceeded",
                    () -> build.add(0, "DistinctOverBudget", 0, 1, M3ASTIndex.EXACT_RANGE));
        }
        build.add(0, new String(names[0]), -1, -1, M3ASTIndex.SYNTAX_ONLY);
        M3ASTIndex index = build.seal();
        equal(2 * names.length + 1, index.nodeCount(), "node count after refusal");
        for (int i = 0; i < names.length; i++) {
            same(names[i], index.kind(i), "first identity");
            same(names[i], index.kind(names.length + names.length - 1 - i), "equal identity");
            equal(i == 0 ? -1 : 0, index.parent(i), "parent");
        }
        same(names[0], index.kind(index.nodeCount() - 1), "known name at capacity");
        same(file, index.source(), "retained source");
        same(file.text(), index.source().text(), "retained text");
        equal((long) index.nodeCount() * 32L, index.primitiveBytes(), "frozen primitive lanes");
        int child = index.firstChild(0), ordinal = 1;
        while (child != -1) {
            equal(ordinal++, child, "stable sibling order");
            child = index.nextSibling(child);
        }
        equal(index.nodeCount(), ordinal, "complete sibling chain");
        fails(IllegalStateException.class, "index already sealed",
                () -> build.add(-99, null, -3, -4, -1));
    }

    private static void identityAndGeometry() {
        String text = new String(new char[] { 'a', (char) 0xd83d, (char) 0xde03, 'b',
                (char) 0xd800, 'c', (char) 0xdc00 });
        M3File file = source(text);
        same(text, file.text(), "String source by identity");
        String first = new String("Leaf"), equalName = new String("Leaf");
        M3ASTIndex.Builder build = builder(file, 16);
        build.add(-1, "Unit", 0, text.length(), M3ASTIndex.EXACT_RANGE);
        build.add(0, first, 1, 3, M3ASTIndex.EXACT_RANGE);
        build.add(0, equalName, 4, 5, M3ASTIndex.EXACT_RANGE);
        build.add(0, "Unknown", -1, -1, M3ASTIndex.SYNTAX_ONLY | M3ASTIndex.SOURCE_UNVERIFIED);
        build.add(0, "Complete", -1, -1, M3ASTIndex.SYNTAX_ONLY);
        build.completeRange(4, 5, 7, true);
        M3ASTIndex index = build.seal();
        same(first, index.kind(2), "first equal caller representative");
        equal(text.substring(1, 3), index.sourceText(1), "surrogate pair coordinates");
        equal(text.substring(4, 5), index.sourceText(2), "isolated surrogate coordinates");
        equal(text.substring(5, 7), index.sourceText(4), "completed range");
        equal(4, index.childCount(0), "child count");
        truth(!index.hasExactRange(3), "unknown remains unverified");
        fails(IllegalStateException.class, "unverified/unknown source range", () -> index.sourceText(3));
        fails(IndexOutOfBoundsException.class, null, () -> index.kind(-1));
        fails(IndexOutOfBoundsException.class, null, () -> index.kind(5));
        StringBuilder mutable = new StringBuilder("before");
        M3File snapshot = M3File.of(URI.create("memory:/snapshot"), mutable);
        mutable.replace(0, mutable.length(), "after");
        equal("before", snapshot.text(), "non-String snapshot");
    }

    private static void refusals() {
        M3File file = source("0123456789");
        M3ASTIndex.Builder b = builder(file, 8);
        fails(IllegalStateException.class, "AST index not open or empty", b::seal);
        fails(IllegalArgumentException.class, "bad AST parent or root coordinate",
                () -> b.add(0, null, -2, 99, -1));
        fails(IllegalArgumentException.class, "invalid UTF-16 half-open AST range",
                () -> b.add(-1, null, -2, 99, -1));
        fails(IllegalArgumentException.class, "unknown AST index flags",
                () -> b.add(-1, null, 0, 10, 16));
        fails(IllegalArgumentException.class, "exact range without source coordinates",
                () -> b.add(-1, null, -1, -1, M3ASTIndex.EXACT_RANGE));
        fails(NullPointerException.class, "kind", () -> b.add(-1, null, 0, 10, 1));
        fails(IllegalArgumentException.class, "invalid bounded AST kind",
                () -> b.add(-1, "", 0, 10, 1));
        fails(IllegalArgumentException.class, "invalid bounded AST kind",
                () -> b.add(-1, "x".repeat(1025), 0, 10, 1));
        equal(0, b.add(-1, "x".repeat(1024), 2, 8, 1), "refusals do not add a root");
        fails(IllegalArgumentException.class, "AST child escapes known parent range",
                () -> b.add(0, null, 0, 9, 1));
        equal(1, b.add(0, "Child", 3, 7, 1), "refusals do not add a child");
        b.seal();
        fails(IllegalStateException.class, "AST index not open or empty", b::seal);
        fails(IllegalStateException.class, "index already sealed",
                () -> b.completeRange(-1, -1, -1, true));
        M3ASTIndex.Builder limited = builder(file, 1);
        limited.add(-1, "Root", 0, 10, 1);
        fails(IllegalArgumentException.class, "AST index node budget exceeded",
                () -> limited.add(-1, null, -2, -3, -1));
        M3ASTIndex.Builder complete = builder(file, 4);
        complete.add(-1, "Root", -1, -1, 2);
        complete.add(0, "Child", 2, 8, 1);
        fails(IllegalArgumentException.class, "completed AST range excludes an indexed child",
                () -> complete.completeRange(0, 3, 7, true));
        complete.completeRange(0, 0, 10, true);
        truth(complete.seal().hasExactRange(0), "completion after refused range");
    }

    private static void fails(Class<? extends Throwable> type, String message, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (!type.isInstance(failure) || (message != null && !message.equals(failure.getMessage()))) {
                throw new AssertionError("expected " + type.getName() + ": " + message, failure);
            }
            return;
        }
        throw new AssertionError("expected " + type.getName() + ": " + message);
    }

    private static void same(Object expected, Object actual, String label) {
        checks++;
        if (expected != actual) throw new AssertionError(label);
    }

    private static void truth(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label);
    }

    private static void equal(Object expected, Object actual, String label) {
        checks++;
        if (!expected.equals(actual)) throw new AssertionError(label + ": " + expected + " != " + actual);
    }
}
