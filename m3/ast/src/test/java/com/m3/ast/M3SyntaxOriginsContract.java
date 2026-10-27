// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ast;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Conservatively preserve generated syntax macro/inclusion/embedded origins. */
public final class M3SyntaxOriginsContract {
    private static int checks;
    private static void ok(boolean cond, String why) {
        if (!cond) throw new AssertionError(why);
        checks++;
    }
    private static void rejects(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); }
        catch (Throwable t) {
            if (!type.isInstance(t)) throw new AssertionError("wrong rejection", t);
            checks++; return;
        }
        throw new AssertionError("invalid source correspondence accepted");
    }
    public static void main(String[] args) {
        byte[] expanded = "ABαCDxyz".getBytes(StandardCharsets.UTF_8);
        var b = M3SyntaxTree.builder("lang/one", expanded);
        b.open("root", "", 0, 0); b.close(expanded.length);
        var tree = b.build();
        String a = "a".repeat(64), c = "c".repeat(64), d = "d".repeat(64);
        var map = M3SyntaxOrigins.builder(tree)
                .add(0, 2, "source/A", a, 4, 6, M3SyntaxOrigins.Relation.EXACT_BYTES)
                .add(2, 5, "source/MACRO", c, 30, 35, M3SyntaxOrigins.Relation.MACRO_EXPANSION)
                .add(2, 5, "source/A", a, 100, 103, M3SyntaxOrigins.Relation.DERIVED)
                .add(5, 7, "source/C", d, 0, 2, M3SyntaxOrigins.Relation.EXACT_BYTES)
                .freeze();
        ok(map.count() == 4 && map.projectionSha256().equals(tree.projectionSha256()), "bound projection");
        ok(map.exactOriginalByteAt(0) == 4 && map.exactOriginalByteAt(1) == 5,
                "unique exact byte mapping");
        ok(Arrays.equals(map.originRowsAt(3), new int[] {1, 2})
                && map.exactOriginalByteAt(3) == -1, "overlapping macro origins are ambiguous");
        ok(map.origin(1).sourceId().equals("source/MACRO")
                && map.origin(1).relation() == M3SyntaxOrigins.Relation.MACRO_EXPANSION,
                "preserve source identity and provenance relation");
        ok(map.exactOriginalByteAt(5) == 0 && map.exactOriginalByteAt(7) == -1,
                "half-open boundaries and only covered points");
        ok(map.originRowsAt(expanded.length).length == 0, "EOF is not silently mapped");
        rejects(IndexOutOfBoundsException.class, () -> map.originRowsAt(expanded.length + 1));
        rejects(IllegalArgumentException.class, () -> M3SyntaxOrigins.builder(tree)
                .add(0, 2, "source/A", a, 0, 3, M3SyntaxOrigins.Relation.EXACT_BYTES));
        map.requireProjection(tree);
        var nextBuilder = M3SyntaxTree.builder("lang/one", "else".getBytes(StandardCharsets.UTF_8));
        nextBuilder.open("root", "", 0, 0); nextBuilder.close(4);
        rejects(IllegalStateException.class, () -> map.requireProjection(nextBuilder.build()));
        rejects(IllegalArgumentException.class, () -> M3SyntaxOrigins.builder(tree)
                .add(0, 2, "id", "broken", 0, 1, M3SyntaxOrigins.Relation.DERIVED));
        rejects(IllegalArgumentException.class, () -> M3SyntaxOrigins.builder(tree)
                .add(2, 3, "source/A", a, 0, 1, M3SyntaxOrigins.Relation.EXACT_BYTES)
                .add(1, 2, "source/A", a, 0, 1, M3SyntaxOrigins.Relation.EXACT_BYTES));
        rejects(IllegalArgumentException.class, () -> M3SyntaxOrigins.builder(tree)
                .add(0, 1, "source/A", a, 0, 1, M3SyntaxOrigins.Relation.EXACT_BYTES)
                .add(1, 2, "source/A", c, 0, 1, M3SyntaxOrigins.Relation.EXACT_BYTES));
        var singleton = M3SyntaxOrigins.builder(tree);
        singleton.add(0, 1, "source/A", a, 0, 1, M3SyntaxOrigins.Relation.EXACT_BYTES);
        singleton.freeze();
        rejects(IllegalStateException.class, singleton::freeze);
        rejects(IllegalStateException.class, () -> singleton.add(1, 2, "source/A", a, 2, 3,
                M3SyntaxOrigins.Relation.EXACT_BYTES));
        var big = M3SyntaxOrigins.builder(tree);
        for (int i = 0; i < 70; i++) big.add(0, 1, "file-" + i, a, i, i+1,
                M3SyntaxOrigins.Relation.EXACT_BYTES);
        var many = big.freeze();
        ok(many.originRowsAt(0).length == 70 && many.exactOriginalByteAt(0) == -1,
                "growth and many-overlapping-sources refuse point projection");
        // Many independent translation units are expected in a generated
        // source graph. Preserve first-registration dictionary IDs without
        // an O(unique-files) linear scan on each new provenance record.
        int sourceCount = 8192;
        var indexedBuilder = M3SyntaxOrigins.builder(tree);
        for (int i = 0; i < sourceCount; i++) {
            indexedBuilder.add(0, 1, "source/generated-" + i, a,
                    i, i + 1, M3SyntaxOrigins.Relation.EXACT_BYTES);
        }
        // Reuse source #12 instead of registering a duplicate dictionary row.
        indexedBuilder.add(1, 2, "source/generated-12", a, 20000, 20001,
                M3SyntaxOrigins.Relation.EXACT_BYTES);
        var indexed = indexedBuilder.freeze();
        ok(indexed.count() == sourceCount + 1, "indexed source-origin count");
        for (int i : new int[] {0, 1, 12, 70, 4096, sourceCount - 1}) {
            var item = indexed.origin(i);
            ok(item.sourceId().equals("source/generated-" + i)
                    && item.sha256().equals(a)
                    && item.startByte() == i && item.endByte() == i + 1,
                    "stable insertion-order source identity for " + i);
        }
        ok(indexed.origin(sourceCount).sourceId().equals("source/generated-12")
                && indexed.origin(sourceCount).startByte() == 20000,
                "repeated provenance uses the original indexed source identity");
        rejects(IllegalArgumentException.class, () -> M3SyntaxOrigins.builder(tree)
                .add(0, 1, "source/generated-12", a, 0, 1, M3SyntaxOrigins.Relation.EXACT_BYTES)
                .add(1, 2, "source/generated-12", c, 0, 1, M3SyntaxOrigins.Relation.EXACT_BYTES));
        ok(indexed.originRowsAt(0).length == sourceCount
                && indexed.exactOriginalByteAt(0) == -1,
                "overlapping multi-source origins stay ambiguous");
        System.out.println("M3_SYNTAX_ORIGINS_PASS checks=" + checks);
    }
}
