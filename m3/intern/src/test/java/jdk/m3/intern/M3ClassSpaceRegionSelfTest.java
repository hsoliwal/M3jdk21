// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.intern;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Independent baseline vs region differential oracle; no JDK intern semantics modified. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/d5ad5090d87ef8477a56317e60ac0d29c65029f7/synexia-openrewrite-recipes/recipe-crates/m3-classspace-region-intern-20261030/src/test/java/com/synexia/m3/classspace/M3ClassSpaceRegionSelfTest.java blob 8739275e04fe; recipe m3/intern/recipe/class-space-20261107/relocate.py v1
public final class M3ClassSpaceRegionSelfTest {
    private static long assertions;

    private M3ClassSpaceRegionSelfTest() { }

    public static void main(String[] args) throws Exception {
        invalidAndBoundaryRanges();
        collisionAndCallbackProvenance();
        quotaAndCopiedArena();
        randomizedBaselineParity();
        realClassfileConstantPoolParity();
        concurrentImmutableRangeFanIn();
        System.out.println("M3_REGION_INTERN_PASS assertions=" + assertions);
    }

    private static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message + " (#" + assertions + ")");
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        assertions++;
        try {
            action.run();
        } catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("Expected " + type.getSimpleName() + " got " + error, error);
        }
        throw new AssertionError("Expected " + type.getSimpleName() + " (" + assertions + ")");
    }

    private static void invalidAndBoundaryRanges() {
        M3ClassSpace store = new M3ClassSpace(128);
        byte[] input = {17, 18, 19, 20, 21};
        expect(NullPointerException.class, () -> store.internRegion(null, 0, 0));
        expect(IndexOutOfBoundsException.class, () -> store.internRegion(input, -1, 1));
        expect(IndexOutOfBoundsException.class, () -> store.internRegion(input, 0, -1));
        expect(IndexOutOfBoundsException.class, () -> store.internRegion(input, 6, 0));
        expect(IndexOutOfBoundsException.class, () -> store.internRegion(input, 5, 1));
        expect(IndexOutOfBoundsException.class, () -> store.internRegion(input, Integer.MAX_VALUE, 1));
        expect(IndexOutOfBoundsException.class, () -> store.internRegion(input, 1, Integer.MAX_VALUE));
        check(store.imageCount() == 0 && store.retainedByteCount() == 0, "invalid region is no-op");
        int empty = store.internRegion(input, input.length, 0);
        check(empty == store.intern(new byte[0]), "zero length at array end is canonical");
        check(store.copy(empty).length == 0, "copy empty remains empty");
        int id = store.internRegion(input, 1, 3);
        check(Arrays.equals(store.copy(id), new byte[] {18,19,20}), "selected region only");
        check(store.digestHex(id).equals(store.digestHex(store.intern(new byte[] {18,19,20}))),
                "existing full-array hash identity");
        input[2] = 99;
        check(Arrays.equals(store.copy(id), new byte[] {18,19,20}), "source array is never retained");
        byte[] exposed = store.copy(id);
        exposed[0] = 0;
        check(store.copy(id)[0] == 18, "copy never aliases shared arena");
    }

    private static void collisionAndCallbackProvenance() {
        AtomicReference<byte[]> seen = new AtomicReference<>();
        AtomicInteger calls = new AtomicInteger();
        M3ClassSpace.DigestFunction f = bytes -> {
            seen.set(bytes);
            calls.incrementAndGet();
            return new byte[32]; // Deliberately every candidate collides.
        };
        M3ClassSpace original = new M3ClassSpace(16384, f);
        M3ClassSpace regions = new M3ClassSpace(16384, f);
        byte[] full = "abc".getBytes(StandardCharsets.US_ASCII);
        check(original.intern(full) == 0 && seen.get() == full,
                "existing full-array callback receives the original array");
        byte[] bounded = "#abc!".getBytes(StandardCharsets.US_ASCII);
        int regionId = regions.internRegion(bounded, 1, 3);
        check(regionId == 0 && seen.get() != bounded && Arrays.equals(seen.get(), full),
                "custom callback sees selected byte array, not caller guards");
        for (int i = 0; i < 75; i++) {
            byte[] payload = new byte[] {(byte) i, (byte) (i >>> 3), 0, (byte) (i ^ 63)};
            byte[] guards = new byte[] {77, payload[0], payload[1], payload[2], payload[3], 66};
            int expected = original.intern(payload);
            int actual = regions.internRegion(guards, 1, payload.length);
            check(expected == actual, "same prefix and serial mutation produce stable ordinal");
            check(Arrays.equals(regions.copy(actual), payload), "collision never shares unequal bytes");
            check(Arrays.equals(original.copy(expected), payload), "original store retains same bytes");
            check(regions.internRegion(guards, 1, payload.length) == actual,
                    "collision chain exact duplicate ID stable");
        }
        check(calls.get() >= 75, "custom digest callback exercised in both paths");
    }

    private static void quotaAndCopiedArena() {
        M3ClassSpace store = new M3ClassSpace(4);
        byte[] source = {(byte) 0xfc, 1, 2, 3, (byte) 0xfd};
        int id = store.internRegion(source, 1, 3);
        check(store.retainedByteCount() == 3, "quota counts only selected bytes");
        check(id == store.intern(new byte[] {1,2,3}), "full-array alias at quota");
        expect(IllegalStateException.class, () -> store.internRegion(source, 0, 2));
        check(store.imageCount() == 1 && store.retainedByteCount() == 3,
                "insufficient budget cannot retain a partial symbol");
        int finalId = store.internRegion(source, 3, 1);
        check(finalId == 1 && store.retainedByteCount() == 4, "exact remaining budget allowed");
        check(store.internRegion(source, 3, 1) == finalId, "duplicate admitted at full quota");
        expect(IllegalStateException.class, () -> store.internRegion(source, 2, 2));
        check(store.imageCount() == 2 && store.retainedByteCount() == 4,
                "owner bounds preserved after repeated failures");
    }

    private static void randomizedBaselineParity() {
        for (int permutation = 0; permutation < 3; permutation++) {
            Random random = new Random(0x434c415353L + permutation);
            M3ClassSpace baseline = new M3ClassSpace(1024 * 1024);
            M3ClassSpace candidate = new M3ClassSpace(1024 * 1024);
            List<byte[]> prior = new ArrayList<>();
            for (int iteration = 0; iteration < 2100; iteration++) {
                int count = random.nextInt(193);
                byte[] bytes = new byte[count];
                if (!prior.isEmpty() && iteration % 5 == 0) {
                    bytes = prior.get(random.nextInt(prior.size())).clone();
                } else {
                    random.nextBytes(bytes);
                    prior.add(bytes.clone());
                }
                byte[] whole = new byte[bytes.length + 13];
                random.nextBytes(whole);
                System.arraycopy(bytes, 0, whole, 7, bytes.length);
                byte[] guardBefore = whole.clone();
                int ref = baseline.intern(bytes);
                int id = candidate.internRegion(whole, 7, bytes.length);
                check(ref == id, "exact same ordinal in serial CP-like replay");
                check(Arrays.equals(baseline.copy(ref), candidate.copy(id)), "baseline vs candidate bytes");
                check(baseline.digestHex(ref).equals(candidate.digestHex(id)), "SHA-256 candidate identity parity");
                check(Arrays.equals(whole, guardBefore), "region intern does not mutate source bytes");
                // Change guards after interning; candidate must be independent of them.
                if (whole.length > 0) whole[0] ^= 1;
                check(Arrays.equals(candidate.copy(id), bytes), "guards excluded from digest and content");
            }
            check(baseline.imageCount() == candidate.imageCount(), "identical retained IDs");
            check(baseline.retainedByteCount() == candidate.retainedByteCount(), "identical arena occupancy");
        }
    }

    private static void realClassfileConstantPoolParity() throws IOException {
        byte[] compiled;
        try (InputStream in = Objects.requireNonNull(
                M3ClassFileAtoms.class.getResourceAsStream("M3ClassFileAtoms.class"))) {
            compiled = in.readAllBytes();
        }
        M3ClassFileAtoms.Plan plan = M3ClassFileAtoms.scan(compiled);
        check(plan.utf8Count() > 10, "actual Java21 class file symbolic lanes");
        M3ClassSpace oldStore = new M3ClassSpace(1024 * 1024);
        M3ClassSpace optimized = new M3ClassSpace(1024 * 1024);
        M3ClassFileAtoms.Interned actual = plan.internInto(optimized);
        for (int i = 1; i < plan.constantPoolCount(); i++) {
            if (plan.tagAt(i) == 1) {
                int expected = oldStore.intern(plan.copyUtf8At(i));
                check(expected == actual.symbolIdAt(i), "CP ordinal to canonical ID preserved");
                check(Arrays.equals(oldStore.copy(expected), optimized.copy(expected)), "CP raw MUTF8 parity");
            } else {
                check(actual.symbolIdAt(i) == -1, "non-UTF8 CP index unchanged");
            }
        }
        check(oldStore.imageCount() == optimized.imageCount(), "same distinct CP symbols");
        check(oldStore.retainedByteCount() == optimized.retainedByteCount(), "same CP bytes retained");
        int savedCount = optimized.imageCount();
        M3ClassFileAtoms.Interned again = plan.internInto(optimized);
        check(savedCount == optimized.imageCount(), "repeat serial CP pass is exact fixed point");
        for (int i = 1; i < plan.constantPoolCount(); i++) {
            check(actual.symbolIdAt(i) == again.symbolIdAt(i), "CP replay ID stability");
        }
    }

    private static void concurrentImmutableRangeFanIn() throws Exception {
        byte[] classLike = "class Shared<T> { static final String regex = \"[a-z]*\"; }"
                .getBytes(StandardCharsets.UTF_8);
        byte[] guarded = new byte[classLike.length + 12];
        System.arraycopy(classLike, 0, guarded, 5, classLike.length);
        M3ClassSpace pool = new M3ClassSpace(2048);
        ExecutorService executor = Executors.newFixedThreadPool(6);
        try {
            List<Callable<Integer>> work = new ArrayList<>();
            for (int i = 0; i < 192; i++) {
                work.add(() -> pool.internRegion(guarded, 5, classLike.length));
            }
            for (Future<Integer> f : executor.invokeAll(work)) {
                check(f.get() == 0, "serialized duplicate identification with parallel callers");
            }
        } finally {
            executor.shutdown();
            check(executor.awaitTermination(30, TimeUnit.SECONDS), "workers terminate normally");
        }
        check(pool.imageCount() == 1, "only a single immutable range retained");
        check(pool.retainedByteCount() == classLike.length, "no repeated interned byte copies");
        check(Arrays.equals(pool.copy(0), classLike), "shared content reconstructs exactly");
    }
}
