// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser.test;

import java.net.URI;
import java.util.BitSet;
import java.util.Arrays;
import java.util.Random;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTFacts;
import jdk.m3.parser.M3ASTDependencyImpact;
import jdk.m3.parser.M3ASTSourceOrigins;
import jdk.m3.parser.M3File;

/** Executable pass-by-pass source and NODE_REF impact closure admission. */
public final class M3ASTDependencyImpactContract {
    private static int checks;
    private static void check(boolean yes, String message) {
        checks++;
        if (!yes) throw new AssertionError(message);
    }
    private static void reject(Class<? extends Throwable> expected, Runnable run) {
        checks++;
        try { run.run(); }
        catch (Throwable found) {
            if (expected.isInstance(found)) return;
            throw new AssertionError("wrong exception for bounded AST impact", found);
        }
        throw new AssertionError("expected bounded AST impact refusal");
    }
    private static void closure() {
        M3File file = M3File.of(URI.create("memory:/impact.java"), "abcdefghijklmnopqrst");
        M3ASTIndex.Builder builder = M3ASTIndex.builder(file, M3ASTIndex.Domain.JAVAC, 10);
        int root = builder.add(-1, "ROOT", 0, 20, M3ASTIndex.EXACT_RANGE);
        int declaration = builder.add(root, "DECL", 0, 4, M3ASTIndex.EXACT_RANGE);
        int consumer = builder.add(root, "READ", 5, 8, M3ASTIndex.EXACT_RANGE);
        int chain = builder.add(root, "CALL", 9, 13, M3ASTIndex.EXACT_RANGE);
        int stable = builder.add(root, "STABLE", 14, 19, M3ASTIndex.EXACT_RANGE);
        int unknown = builder.add(root, "UNKNOWN", -1, -1, 0);
        int nested = builder.add(stable, "NESTED", 15, 17, M3ASTIndex.EXACT_RANGE);
        M3ASTIndex index = builder.seal();
        var facts = M3ASTFacts.builder(index, 12, 12, 1000)
                .nodeRef(declaration, "cycle", chain)
                .nodeRef(consumer, "reads", declaration)
                .nodeRef(chain, "depends", consumer)
                .nodeRef(chain, "cyclic", declaration)
                .text(stable, "explanatory", "no-ref")
                .seal();
        BitSet changed = new BitSet();
        changed.set(declaration);
        var result = M3ASTDependencyImpact.propagate(index, facts, changed, 10, 12);
        check(M3ASTDependencyImpact.SCHEMA.equals("M3_AST_DEPENDENCY_IMPACT_V1"),
                "schema is explicitly versioned");
        check(result.nodeCount() == 7, "original occurrence dimensions");
        check(result.impactedCount() == 5 && result.unchangedCandidateCount() == 2,
                "only closure and unavailable geometry marked");
        for (int row : new int[]{root, declaration, consumer, chain, unknown})
            check(result.impacted(row), "transitive node marked");
        check(!result.impacted(stable) && !result.impacted(nested), "unrelated occurrences unmarked");
        check((result.reasonBits(declaration) & M3ASTDependencyImpact.DIRECT) != 0,
                "root edit cause retained");
        check((result.reasonBits(unknown) & M3ASTDependencyImpact.UNKNOWN_RANGE) != 0,
                "unknown source range refused for reuse");
        check((result.reasonBits(consumer) & M3ASTDependencyImpact.REFERENCER) != 0,
                "typed references reverse propagate");
        check((result.reasonBits(root) & M3ASTDependencyImpact.ANCESTOR) != 0,
                "parents conservatively invalidated");
        check(Arrays.equals(result.impactedRows(5), new int[]{0,1,2,3,5}),
                "deterministic original occurrence order");
        BitSet leaked = result.impactedMask(); leaked.clear();
        check(result.impactedCount() == 5, "returned mask cannot mutate impact proof");
        changed.clear();
        check(result.impacted(declaration), "input BitSet not retained");
        reject(IllegalArgumentException.class, () -> result.impactedRows(4));
        reject(IllegalArgumentException.class, () -> M3ASTDependencyImpact.propagate(index, facts, new BitSet(), 6, 12));
        reject(IllegalArgumentException.class, () -> M3ASTDependencyImpact.propagate(index, facts, new BitSet(), 10, 4));
        BitSet alien = new BitSet(); alien.set(7);
        reject(IllegalArgumentException.class, () -> M3ASTDependencyImpact.propagate(index, facts, alien, 10, 12));
        M3ASTIndex.Builder otherBuilder = M3ASTIndex.builder(file, M3ASTIndex.Domain.JAVAC, 2);
        otherBuilder.add(-1, "ROOT", 0, 20, M3ASTIndex.EXACT_RANGE);
        M3ASTIndex foreign = otherBuilder.seal();
        reject(IllegalArgumentException.class, () ->
                M3ASTDependencyImpact.propagate(foreign, facts, new BitSet(), 10, 12));
        var noFacts = M3ASTDependencyImpact.propagate(index, null, new BitSet(), 10, 0);
        check(noFacts.impactedCount() == 2, "unknown row and parent without semantic facts");
        reject(IndexOutOfBoundsException.class, () -> result.reasonBits(7));
    }
    private static void externalOrigins() {
        M3File primary = M3File.of(URI.create("memory:/main.java"), "0123456789");
        M3File auxiliary = M3File.of(URI.create("memory:/import.java"), "abcdefghij");
        M3File differentGeneration = M3File.of(auxiliary.uri(), "abcdefghij");
        var b = M3ASTIndex.builder(primary, M3ASTIndex.Domain.EXTERNAL, 5);
        int root = b.add(-1, "ROOT", 0, 10, M3ASTIndex.EXACT_RANGE);
        int reference = b.add(root, "REF", 1, 4, M3ASTIndex.EXACT_RANGE);
        int expansion = b.add(root, "EXPAND", 4, 7, M3ASTIndex.EXACT_RANGE);
        b.portable(root, null, 0);
        b.portable(reference, "spelling", M3ASTIndex.NAMED);
        b.portable(expansion, "expansion", M3ASTIndex.NAMED);
        var index = b.seal();
        var sources = M3ASTSourceOrigins.builder(index, 6, 3, 100)
                .add(reference, M3ASTSourceOrigins.Role.SPELLING, auxiliary, 1, 3)
                .add(expansion, M3ASTSourceOrigins.Role.EXPANSION, auxiliary, 5, 8)
                .add(expansion, M3ASTSourceOrigins.Role.GENERATED, primary, 4, 7)
                .seal();
        BitSet touched = M3ASTDependencyImpact.touchedOrigins(sources, auxiliary, 2, 2, 6);
        check(touched.cardinality() == 1 && touched.get(reference) && !touched.get(expansion),
                "cross-file exact spelling origin seed");
        var impact = M3ASTDependencyImpact.propagate(index, null, touched, 5, 0);
        check(impact.impactedCount() == 2 && impact.impacted(root),
                "cross-file origin propagates to syntax owner");
        check(M3ASTDependencyImpact.touchedOrigins(sources, auxiliary, 8, 8, 6).get(expansion),
                "closed boundary conservative origin scan");
        reject(IllegalArgumentException.class, () ->
                M3ASTDependencyImpact.touchedOrigins(sources, differentGeneration, 2, 2, 6));
        reject(IllegalArgumentException.class, () ->
                M3ASTDependencyImpact.touchedOrigins(sources, auxiliary, 2, 2, 2));
        reject(IndexOutOfBoundsException.class, () ->
                M3ASTDependencyImpact.touchedOrigins(sources, auxiliary, 0, 11, 6));
    }
    private static void randomizedDependencyOracle() {
        Random random = new Random(0x4d33415354L);
        for (int trial = 0; trial < 360; trial++) {
            int count = 1 + random.nextInt(80);
            M3File file = M3File.of(URI.create("memory:/fuzz"), "x".repeat(128));
            var builder = M3ASTIndex.builder(file, M3ASTIndex.Domain.JAVAC, count);
            for (int row = 0; row < count; row++) {
                int parent = row == 0 ? -1 : random.nextInt(row);
                boolean known = row == 0 || random.nextInt(9) != 0;
                builder.add(parent, "NODE", known ? 0 : -1, known ? 128 : -1,
                        known ? M3ASTIndex.EXACT_RANGE : 0);
            }
            M3ASTIndex index = builder.seal();
            int budget = Math.multiplyExact(count, 4);
            var factsBuilder = M3ASTFacts.builder(index, budget, 40, 500);
            for (int row = 0; row < count; row++) {
                int degree = random.nextInt(4);
                for (int edge = 0; edge < degree; edge++) {
                    factsBuilder.nodeRef(row, "references", random.nextInt(count));
                }
            }
            M3ASTFacts facts = factsBuilder.seal();
            BitSet initial = new BitSet();
            for (int row = 0; row < count; row++) if (random.nextInt(7) == 0) initial.set(row);
            boolean[] oracle = new boolean[count];
            for (int row = 0; row < count; row++) {
                oracle[row] = initial.get(row) || !index.hasExactRange(row);
            }
            boolean moved;
            do {
                moved = false;
                for (int row = 0; row < count; row++) {
                    if (oracle[row] && index.parent(row) >= 0 && !oracle[index.parent(row)]) {
                        oracle[index.parent(row)] = true;
                        moved = true;
                    }
                }
                for (int fact = 0; fact < facts.factCount(); fact++) {
                    if (facts.kind(fact) == M3ASTFacts.Kind.NODE_REF
                            && oracle[facts.nodeRef(fact)] && !oracle[facts.owner(fact)]) {
                        oracle[facts.owner(fact)] = true;
                        moved = true;
                    }
                }
            } while (moved);
            var result = M3ASTDependencyImpact.propagate(index, facts, initial, count, budget);
            int expectedCount = 0;
            for (int row = 0; row < count; row++) {
                check(result.impacted(row) == oracle[row], "independent fixed-point graph oracle");
                if (oracle[row]) expectedCount++;
            }
            check(result.impactedCount() == expectedCount, "closure cardinality matches oracle");
            check(result.unchangedCandidateCount() == count - expectedCount,
                    "unimpacted candidate cardinality");
        }
    }
    public static void main(String[] args) {
        closure(); externalOrigins(); randomizedDependencyOracle();
        System.out.println("M3_AST_DEPENDENCY_IMPACT_PASS checks=" + checks);
    }
}
