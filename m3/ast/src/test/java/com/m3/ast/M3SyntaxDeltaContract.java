// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ast;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Standalone AST-only byte-coordinate delta contract: no M3File dependency. */
public final class M3SyntaxDeltaContract {
    private static int checks;
    private static void check(boolean ok, String why) {
        if (!ok) throw new AssertionError(why);
        checks++;
    }
    private static void refuse(Class<? extends Throwable> expected, Runnable action) {
        try { action.run(); }
        catch (Throwable actual) {
            if (!expected.isInstance(actual)) throw new AssertionError("wrong rejection", actual);
            checks++; return;
        }
        throw new AssertionError("accepted invalid AST delta");
    }
    private static M3SyntaxTree expr(String source, String grammar) {
        byte[] b = source.getBytes(StandardCharsets.UTF_8);
        int leftEnd = "α".getBytes(StandardCharsets.UTF_8).length;
        int rightStart = b.length - "β".getBytes(StandardCharsets.UTF_8).length;
        var tree = M3SyntaxTree.builder(grammar, b);
        tree.open("expr", "", 0, M3SyntaxTree.NAMED);
        tree.leaf("id", "left", 0, leftEnd, M3SyntaxTree.NAMED | M3SyntaxTree.TOKEN);
        tree.leaf("op", "operator", leftEnd + 1, leftEnd + 2, M3SyntaxTree.TOKEN);
        tree.leaf("id", "right", rightStart, b.length, M3SyntaxTree.NAMED | M3SyntaxTree.TOKEN);
        tree.close(b.length);
        return tree.build();
    }
    private record SimpleMap(String oldSha256, String newSha256, int from, int to,
                             int insertedBytes) implements M3SyntaxDelta.SourceEditMap {
        public boolean touchesOldSpan(int start, int end) {
            if (from == to) return start <= from && from <= end;
            return start < to && from < end;
        }
        public int oldToNew(int p, boolean rightAffinity) {
            if (p < from) return p;
            if (p == from) return rightAffinity ? from + insertedBytes : from;
            if (p < to) return -1;
            return p + insertedBytes - (to - from);
        }
    }
    private static M3SyntaxDelta.SourceEditMap unchanged(M3SyntaxTree prior, M3SyntaxTree next) {
        return new SimpleMap(prior.sourceSha256(), next.sourceSha256(), -1, -1, 0);
    }
    public static void main(String[] args) {
        var before = expr("α + β", "java/expression");
        var after = expr("α - β", "java/expression");
        var change = new SimpleMap(before.sourceSha256(), after.sourceSha256(), 3, 4, 1);
        var matched = M3SyntaxDelta.match(new M3SyntaxIndex(before), new M3SyntaxIndex(after), change);
        check(matched.candidates() == 2 && !matched.rootCandidate(), "unmodified AST leaves survive reparse");
        check(matched.newNodeForOld(1) == 1 && matched.newNodeForOld(3) == 3,
                "native kinds, fields and byte offsets retained");
        check(Arrays.equals(matched.oldUnmatched(), new int[] {0,2})
                && Arrays.equals(matched.newUnmatched(), new int[] {0,2}), "changed root and operator");
        check(matched.oldNodeForNew(1) == 1 && matched.oldNodeForNew(3) == 3,
                "bidirectional, unique occurrence mapping");
        check(!matched.oldProjectionSha256().equals(matched.newProjectionSha256()),
                "source projections remain generation-specific");
        var identity = M3SyntaxDelta.match(new M3SyntaxIndex(before), new M3SyntaxIndex(before),
                unchanged(before, before));
        check(identity.rootCandidate() && identity.candidates() == before.size(), "exact fixed point");
        var prefix = M3SyntaxTree.builder("java/expression", "##α + β".getBytes(StandardCharsets.UTF_8));
        prefix.open("expr", "", 0, M3SyntaxTree.NAMED);
        prefix.leaf("id", "left", 2, 4, M3SyntaxTree.NAMED | M3SyntaxTree.TOKEN);
        prefix.leaf("op", "operator", 5, 6, M3SyntaxTree.TOKEN);
        prefix.leaf("id", "right", 7, 9, M3SyntaxTree.NAMED | M3SyntaxTree.TOKEN);
        prefix.close(9);
        var shifted = prefix.build();
        var withPrefix = M3SyntaxDelta.match(new M3SyntaxIndex(before), new M3SyntaxIndex(shifted),
                new SimpleMap(before.sourceSha256(), shifted.sourceSha256(), 0, 0, 2));
        check(withPrefix.newNodeForOld(3) == 3 && withPrefix.newNodeForOld(1) == -1,
                "shifted unaffected spans match; insertion boundary remains conservative");
        refuse(IllegalArgumentException.class, () -> M3SyntaxDelta.match(new M3SyntaxIndex(before),
                new M3SyntaxIndex(expr("α - β", "other/parser")), change));
        refuse(IllegalStateException.class, () -> M3SyntaxDelta.match(new M3SyntaxIndex(before),
                new M3SyntaxIndex(after),
                new SimpleMap(after.sourceSha256(), after.sourceSha256(), 3, 4, 1)));
        var ambiguous = M3SyntaxTree.builder("partial/error", new byte[] {'a'});
        ambiguous.open("file", "", 0, M3SyntaxTree.NAMED);
        ambiguous.leaf("missing", "expected", 1, 1, M3SyntaxTree.TOKEN | M3SyntaxTree.MISSING);
        ambiguous.leaf("missing", "expected", 1, 1, M3SyntaxTree.TOKEN | M3SyntaxTree.MISSING);
        ambiguous.close(1);
        var doubled = ambiguous.build();
        var noGuess = M3SyntaxDelta.match(new M3SyntaxIndex(doubled), new M3SyntaxIndex(doubled),
                unchanged(doubled, doubled));
        check(noGuess.candidates() == 1 && noGuess.newNodeForOld(1) == -1
                && noGuess.newNodeForOld(2) == -1, "ambiguous zero-width nodes never arbitrarily matched");
        var longBuilder = M3SyntaxTree.builder("large/js", new byte[1200]);
        longBuilder.open("file", "", 0, 0);
        for (int i = 0; i < 1200; i++)
            longBuilder.leaf("token", "", i, i + 1, M3SyntaxTree.TOKEN);
        longBuilder.close(1200);
        var large = longBuilder.build();
        check(M3SyntaxDelta.match(new M3SyntaxIndex(large), new M3SyntaxIndex(large),
                unchanged(large, large)).candidates() == large.size(), "1200-node primitive join");
        System.out.println("M3_SYNTAX_DELTA_PASS checks=" + checks);
    }
}
