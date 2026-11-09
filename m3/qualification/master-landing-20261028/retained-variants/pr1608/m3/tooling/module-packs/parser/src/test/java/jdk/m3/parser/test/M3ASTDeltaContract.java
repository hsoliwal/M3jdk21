// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.test;

import java.net.URI;
import java.util.Arrays;
import java.util.Random;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3File;

/** Independent lane oracle over every current domain and hostile UTF-16 kinds. */
public final class M3ASTDeltaContract {
    private M3ASTDeltaContract() { }
    private static int checks;
    private static void require(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("check " + checks);
    }
    private static M3ASTIndex build(M3ASTIndex.Domain domain, String text, int count,
            int changedKind, int changedRange, int changedFlags) {
        var source = M3File.of(URI.create("memory:/source"), text);
        var builder = M3ASTIndex.builder(source, domain, 1024);
        builder.add(-1, "root", 0, text.length(), M3ASTIndex.EXACT_RANGE);
        for (int i = 1; i < count; i++) {
            builder.add(0, i == changedKind ? "changed\ud800" : "kind" + i % 7,
                    i == changedRange ? 1 : 0, text.length(),
                    M3ASTIndex.EXACT_RANGE | (i == changedFlags ? M3ASTIndex.RECOVERED : 0));
        }
        return builder.seal();
    }
    public static void main(String[] args) {
        Random random = new Random(210021);
        for (var domain : M3ASTIndex.Domain.values()) {
            for (int pass = 0; pass < 100; pass++) {
                int oldCount = 1 + random.nextInt(200), newCount = 1 + random.nextInt(200);
                var before = build(domain, "abc\ud800", oldCount, -1, -1, -1);
                var after = build(domain, pass % 2 == 0 ? "abc\ud800" : "abc\ud801", newCount,
                        random.nextInt(newCount), random.nextInt(newCount), random.nextInt(newCount));
                var delta = after.deltaFrom(before);
                require(delta.previousNodeCount() == oldCount && delta.currentNodeCount() == newCount);
                require(delta.rows() == Math.max(oldCount, newCount));
                require(delta.sourceChanged() == (pass % 2 != 0));
                int[] expectedRows = new int[delta.rows()];
                int changed = 0;
                boolean shapeEqual = oldCount == newCount;
                for (int i = 0; i < delta.rows(); i++) {
                    int expected;
                    if (i >= oldCount) expected = M3ASTIndex.Delta.ADDED;
                    else if (i >= newCount) expected = M3ASTIndex.Delta.REMOVED;
                    else {
                        expected = 0;
                        if (!before.kind(i).equals(after.kind(i)) || before.parent(i) != after.parent(i)
                                || before.firstChild(i) != after.firstChild(i) || before.nextSibling(i) != after.nextSibling(i)
                                || before.childCount(i) != after.childCount(i)) expected |= M3ASTIndex.Delta.STRUCTURE;
                        if (before.start(i) != after.start(i) || before.end(i) != after.end(i)) expected |= M3ASTIndex.Delta.RANGE;
                        if (before.flags(i) != after.flags(i)) expected |= M3ASTIndex.Delta.FLAGS;
                        if ((expected & M3ASTIndex.Delta.STRUCTURE) != 0) shapeEqual = false;
                    }
                    require(delta.stateAt(i) == expected);
                    if (expected != 0) expectedRows[changed++] = i;
                }
                require(Arrays.equals(Arrays.copyOf(expectedRows, changed), delta.changedRows()));
                require(before.structureSha256().equals(after.structureSha256()) == shapeEqual);
                require(before.deltaFrom(before).changedRows().length == 0);
                require(!before.deltaFrom(before).sourceChanged());
                require(after.structureSha256().length() == 64);
            }
            var before = build(domain, "abcd", 3, -1, -1, -1);
            var ranges = build(domain, "abcd", 3, -1, 1, -1);
            require(before.structureSha256().equals(ranges.structureSha256()));
            require(ranges.deltaFrom(before).stateAt(1) == M3ASTIndex.Delta.RANGE);
            var flags = build(domain, "abcd", 3, -1, -1, 1);
            require(before.structureSha256().equals(flags.structureSha256()));
            require(flags.deltaFrom(before).stateAt(1) == M3ASTIndex.Delta.FLAGS);
            try { before.deltaFrom(null); throw new AssertionError(); }
            catch (NullPointerException expected) { require(true); }
            try { before.deltaFrom(before).stateAt(3); throw new AssertionError(); }
            catch (IndexOutOfBoundsException expected) { require(true); }
        }
        var first = build(M3ASTIndex.Domain.ECLIPSE_JDT, "abcd", 2, -1, -1, -1);
        var second = build(M3ASTIndex.Domain.MAVEN_POM, "abcd", 2, -1, -1, -1);
        try { first.deltaFrom(second); throw new AssertionError(); }
        catch (IllegalArgumentException expected) { require(true); }
        var foreign = M3ASTIndex.builder(M3File.of(URI.create("memory:/other"), "abcd"), first.domain(), 2);
        foreign.add(-1, "root", 0, 4, 1);
        try { first.deltaFrom(foreign.seal()); throw new AssertionError(); }
        catch (IllegalArgumentException expected) { require(true); }
        System.out.println("M3_AST_DELTA_PASS checks=" + checks);
    }
}
