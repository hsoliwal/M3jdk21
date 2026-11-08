// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import java.util.concurrent.Executors;
import java.util.stream.IntStream;

/** Dependency-free deterministic verification for the complete immutable-pooling stack. */
//FLLDCIM: RELOCATE - donor blob 77d4ddabf134 at commit 3385d79a2968 (repository, path, URL and digests: m3/collections/recipe/immutable-pools-20261106/PROVENANCE.tsv); recipe m3/collections/recipe/immutable-pools-20261106/relocate.py v2
public final class ImmutablePoolSelfTest {
    private static int checks;

    private ImmutablePoolSelfTest() { }

    private record Value(String name, int number) { }

    public static void main(String[] args) throws Exception {
        checks = 0;
        strongAndWeakLivePooling();
        System.out.println("PASS: " + checks + " immutable-pool checks");
    }

    private static void strongAndWeakLivePooling() throws Exception {
        StrongValuePool<Value> strong = new StrongValuePool<>(11, 8);
        Value first = new Value("alpha", 1);
        Value equal = new Value("alpha", 1);
        ImmutableId firstId = strong.internId(first);
        ImmutableId secondId = strong.internId(equal);
        check(firstId.equals(secondId), "equal values share strong id");
        check(strong.resolve(firstId) == first, "first strong representative retained");
        check(strong.canonicalize(equal) == first, "strong canonical reference reuse");

        try (var executor = Executors.newFixedThreadPool(8)) {
            var futures = IntStream.range(0, 64)
                .mapToObj(i -> executor.submit(() ->
                    strong.internId(new Value("concurrent", 9))))
                .toList();
            ImmutableId expected = futures.getFirst().get();
            for (var future : futures) check(future.get().equals(expected), "concurrent id convergence");
        }
        check(strong.size() == 2, "one representative per strong equality class");

        StrongValuePool<String> bounded = new StrongValuePool<>(
            12, 1, StrongPoolOverflowPolicy.RETURN_INPUT);
        bounded.internId("a");
        String overflow = new String("b");
        check(!bounded.internId(overflow).present(), "strong overflow has no identity");
        check(bounded.canonicalize(overflow) == overflow, "overflow value remains caller-owned");

        WeakValueCanonicalizer<String> weak = new WeakValueCanonicalizer<>();
        String weakA = new String("weak");
        String weakB = new String("weak");
        String canonical = weak.canonicalize(weakA);
        check(weak.canonicalize(weakB) == canonical, "weak live representative reused");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
