// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser.test;

import java.net.URI;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTRelations;
import jdk.m3.parser.M3File;

/**
 * An already-built Maven-like AST, queried by multiple consumers WITHOUT parse
 * or another source read. Verifies exact retained structural shape, not Maven
 * artifact identity or effective-model semantic equivalence.
 */
public final class M3ASTRelationsContract {
    private static final AtomicInteger CHECKS = new AtomicInteger();

    private M3ASTRelationsContract() { }

    private static void check(boolean condition, String purpose) {
        CHECKS.incrementAndGet();
        if (!condition) throw new AssertionError(purpose);
    }

    private static void same(Object expected, Object actual, String purpose) {
        check(Objects.equals(expected, actual),
                purpose + " expected=" + expected + " actual=" + actual);
    }

    private static void reject(Class<? extends Throwable> expected,
                               Runnable action, String purpose) {
        CHECKS.incrementAndGet();
        try {
            action.run();
        } catch (Throwable failure) {
            if (expected.isInstance(failure)) return;
            throw new AssertionError(purpose + ": wrong failure", failure);
        }
        throw new AssertionError(purpose + ": action succeeded unexpectedly");
    }

    private record Built(M3ASTIndex index, int left, int leftText,
                         int second, int secondText, int distinct, int distinctText) { }

    private static Built detached() {
        M3File original = M3File.of(URI.create("memory:/project/pom.xml"),
                "a".repeat(40));
        var b = M3ASTIndex.builder(original, M3ASTIndex.Domain.MAVEN_POM, 15);
        int root = b.add(-1, "MAVEN_EFFECTIVE_MODEL", 0, 40, M3ASTIndex.EXACT_RANGE);
        int first = b.add(root, "MAVEN_DEPENDENCY", 0, 10,
                M3ASTIndex.EXACT_RANGE | M3ASTIndex.SYNTAX_ONLY);
        int leftText = b.add(first, "ARTIFACT", 1, 8, M3ASTIndex.EXACT_RANGE);
        b.details(leftText, -1, -1, "artifact", M3ASTIndex.LiteralKind.STRING,
                "example:one", 0L, -1, -1);
        int second = b.add(root, "MAVEN_DEPENDENCY", 10, 20,
                M3ASTIndex.EXACT_RANGE | M3ASTIndex.SYNTAX_ONLY);
        int secondText = b.add(second, "ARTIFACT", 11, 18, M3ASTIndex.EXACT_RANGE);
        b.details(secondText, -1, -1, "artifact", M3ASTIndex.LiteralKind.STRING,
                "example:one", 0L, -1, -1);
        int distinct = b.add(root, "MAVEN_DEPENDENCY", 20, 30,
                M3ASTIndex.EXACT_RANGE | M3ASTIndex.SYNTAX_ONLY);
        int distinctText = b.add(distinct, "ARTIFACT", 21, 28, M3ASTIndex.EXACT_RANGE);
        b.details(distinctText, -1, -1, "artifact", M3ASTIndex.LiteralKind.STRING,
                "example:two", 0L, -1, -1);
        return new Built(b.seal(), first, leftText, second, secondText, distinct, distinctText);
    }

    private static void postings(Built build) {
        M3ASTRelations shared = M3ASTRelations.of(build.index());
        check(shared.index() == build.index() && shared.source() == build.index().source(),
                "borrowed original index and M3File owners");
        same(0L, shared.retainedPrimitiveBytes(), "no eager arrays, parser, or AST");
        check(shared.countKind("MAVEN_DEPENDENCY") == 3, "Maven-like kind count");
        check(shared.countKind("NOT_PRESENT") == 0, "unknown kind");
        check(shared.retainedPrimitiveBytes() > 0
                && shared.retainedPrimitiveBytes() < (long) build.index().nodeCount() * 4,
                "count-only Maven linter retains kind counters, not all postings");
        int[] expected = {build.left(), build.second(), build.distinct()};
        check(Arrays.equals(expected, shared.rowsOfKind("MAVEN_DEPENDENCY", 3)),
                "kind postings preserve original preorder");
        check(shared.rowOfKind("MAVEN_DEPENDENCY", 0) == build.left()
                && shared.rowOfKind("MAVEN_DEPENDENCY", 2) == build.distinct(),
                "repeated static analyses use original occurrence coordinates");
        reject(IllegalArgumentException.class,
                () -> shared.rowsOfKind("MAVEN_DEPENDENCY", 2),
                "linter must refuse truncated result");
        reject(IndexOutOfBoundsException.class,
                () -> shared.rowOfKind("MAVEN_DEPENDENCY", 3),
                "row lookup bounds");
        reject(IndexOutOfBoundsException.class,
                () -> shared.rowOfKind("NOT_PRESENT", 0),
                "unknown kind row bounds");
        int[] visited = new int[3];
        int[] used = {0};
        shared.eachKind("MAVEN_DEPENDENCY", row -> visited[used[0]++] = row);
        check(Arrays.equals(visited, expected), "callback-based primitive query");
        int[] directChildren = new int[3];
        int[] count = {0};
        shared.eachChild(0, row -> directChildren[count[0]++] = row);
        check(Arrays.equals(directChildren, expected), "reuse existing child lanes");
        same(build.left(), shared.nearestAncestor(build.leftText(), "MAVEN_DEPENDENCY"),
                "reuse existing ancestor operation");
        same(-1, shared.nearestAncestor(build.left(), "MAVEN_DEPENDENCY"),
                "strict ancestor excludes self");
        check(shared.retainedPrimitiveBytes() > 0,
                "one admitted reusable primitive postings sidecar");
        M3File sameSource = M3File.of(build.index().source().uri(), "a".repeat(40));
        shared.requireSource(sameSource);
        reject(IllegalArgumentException.class,
                () -> shared.requireSource(M3File.of(URI.create("memory:/foreign/pom.xml"),
                        "a".repeat(40))), "foreign resource not interchangeable");
        reject(IllegalArgumentException.class,
                () -> shared.requireSource(M3File.of(build.index().source().uri(),
                        "b".repeat(40))), "same URI with changed source generation");
    }

    private static void structural(Built build) {
        M3ASTRelations shared = M3ASTRelations.of(build.index());
        int n = build.index().nodeCount();
        var hashes = build.index().structuralHashes(n, 100_000);
        same(hashes.sha256(build.left()), hashes.sha256(build.second()),
                "original SHA algorithm agrees on repeated stored shapes");
        check(!hashes.sha256(build.left()).equals(hashes.sha256(build.distinct())),
                "original digest distinguishes different scalar detail");
        var classes = shared.structuralClasses(n, 100_000);
        check(classes == shared.structuralClasses(n, 100_000),
                "shared cached structural result reused, no repeated hashing");
        check(classes.totalOccurrences() == n && classes.distinctShapes() < n,
                "dedup saves shape classes without coalescing source occurrences");
        check(classes.sameRetainedShape(build.left(), build.second())
                && classes.sameRetainedShape(build.leftText(), build.secondText()),
                "identical intrinsic lanes and child class topology");
        check(!classes.sameRetainedShape(build.left(), build.distinct())
                && !classes.sameRetainedShape(build.leftText(), build.distinctText()),
                "different retained data never merges");
        check(classes.representative(build.second()) == build.left(),
                "earliest original source occurrence selected as representative");
        check(classes.multiplicity(build.left()) == 2,
                "duplicate shape count, not semantic Maven dependency count");
        check(Arrays.equals(new int[] {build.left(), build.second()},
                classes.occurrences(build.second(), 2)),
                "source-order members without retained per-group node objects");
        reject(IllegalArgumentException.class,
                () -> classes.occurrences(build.left(), 1),
                "never truncate duplicate-shape evidence silently");
        reject(IllegalArgumentException.class,
                () -> shared.structuralClasses(n - 1, 100_000),
                "budget admission checked even after grouping cached");
        reject(IllegalArgumentException.class,
                () -> shared.structuralClasses(n, 1),
                "text budget admission checked even after grouping cached");
        reject(IndexOutOfBoundsException.class,
                () -> classes.classOf(n), "group coordinates bounded");
        check(shared.retainedPrimitiveBytes() > 0,
                "structural group arrays counted excluding original AST and hash scratch");
        shared.discardDerived();
        same(0L, shared.retainedPrimitiveBytes(),
                "explicit release of optional postings and grouping sidecars");
        check(classes.sameRetainedShape(build.left(), build.second()),
                "consumer-held immutable result valid after cache release");
        check(shared.structuralClasses(n, 100_000) != classes,
                "explicit fresh on-demand result after discard");
    }

    private static void concurrent(Built build) throws InterruptedException {
        M3ASTRelations shared = M3ASTRelations.of(build.index());
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread[] threads = new Thread[4];
        for (int worker = 0; worker < threads.length; worker++) {
            threads[worker] = new Thread(() -> {
                try {
                    for (int i = 0; i < 100; i++) {
                        if (shared.countKind("MAVEN_DEPENDENCY") != 3
                                || !shared.structuralClasses(build.index().nodeCount(), 100_000)
                                  .sameRetainedShape(build.left(), build.second())) {
                            throw new AssertionError("concurrent shared source relation mismatch");
                        }
                    }
                } catch (Throwable error) {
                    failure.compareAndSet(null, error);
                }
            }, "m3-ast-relation-consumer-" + worker);
            threads[worker].start();
        }
        for (Thread thread : threads) thread.join();
        check(failure.get() == null, "concurrent analyzer reads reuse one immutable result");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 0) throw new IllegalArgumentException("no arguments");
        Built build = detached(); // AST supplied exactly once, never re-parsed below.
        postings(build);
        structural(build);
        concurrent(build);
        System.out.println("M3_AST_RELATIONS_PASS checks=" + CHECKS.get()
                + " rows=" + build.index().nodeCount());
    }
}
