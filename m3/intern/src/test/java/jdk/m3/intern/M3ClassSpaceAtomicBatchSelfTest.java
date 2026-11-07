// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.intern;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Executed Java21 oracle, independent of JUnit, with no production side effects. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/d5ad5090d87ef8477a56317e60ac0d29c65029f7/synexia-openrewrite-recipes/recipe-crates/m3-classspace-atomic-batch-20261030/src/test/java/com/synexia/m3/classspace/M3ClassSpaceAtomicBatchSelfTest.java blob 07bd7aa4a4ce; recipe m3/intern/recipe/class-space-20261107/relocate.py v1
public final class M3ClassSpaceAtomicBatchSelfTest {
    private static long assertions;

    private M3ClassSpaceAtomicBatchSelfTest() { }

    public static void main(String[] args) throws Exception {
        invalidInputAndNoMutation();
        transactionalQuota();
        callbacksAndCollisionChains();
        digestCallbackFailureRollback();
        densePrimitiveLaneExpansion();
        randomizedReferenceParity();
        realClassfileFixedPoint();
        parallelExactAdmission();
        System.out.println("M3_CLASS_BATCH_ATOMIC_PASS assertions=" + assertions);
    }

    private static void check(boolean ok, String label) {
        assertions++;
        if (!ok) throw new AssertionError(label + " at " + assertions);
    }

    private static void expect(Class<? extends Throwable> kind, Runnable action) {
        assertions++;
        try {
            action.run();
        } catch (Throwable throwable) {
            if (kind.isInstance(throwable)) return;
            throw new AssertionError("Expected " + kind + ", saw " + throwable, throwable);
        }
        throw new AssertionError("Expected " + kind + " but no exception at " + assertions);
    }

    private static void invalidInputAndNoMutation() {
        M3ClassSpace store = new M3ClassSpace(16);
        byte[] b = {1, 2, 3};
        byte[] tags = {0, 1, 1};
        int[] offsets = {0, 0, 1};
        int[] lengths = {0, 1, 2};
        expect(NullPointerException.class, () -> store.internUtf8Batch(null, tags, offsets, lengths));
        expect(NullPointerException.class, () -> store.internUtf8Batch(b, null, offsets, lengths));
        expect(NullPointerException.class, () -> store.internUtf8Batch(b, tags, null, lengths));
        expect(NullPointerException.class, () -> store.internUtf8Batch(b, tags, offsets, null));
        expect(IllegalArgumentException.class, () -> store.internUtf8Batch(b, new byte[0], offsets, lengths));
        expect(IllegalArgumentException.class, () -> store.internUtf8Batch(b, tags, new int[2], lengths));
        expect(IllegalArgumentException.class, () -> store.internUtf8Batch(b, tags, offsets, new int[1]));
        expect(IndexOutOfBoundsException.class, () -> store.internUtf8Batch(b, tags,
                new int[] {0, 0, 4}, lengths));
        expect(IndexOutOfBoundsException.class, () -> store.internUtf8Batch(b, tags,
                offsets, new int[] {0, 1, Integer.MAX_VALUE}));
        expect(IndexOutOfBoundsException.class, () -> store.internUtf8Batch(b, tags,
                offsets, new int[] {0, 1, -1}));
        check(store.imageCount() == 0 && store.retainedByteCount() == 0,
                "invalid batch must have no admitted prefix");
        int[] ids = store.internUtf8Batch(b, tags, offsets, lengths);
        check(Arrays.equals(ids, new int[] {-1, 0, 1}), "validated CP order");
        check(store.imageCount() == 2 && store.retainedByteCount() == 3,
                "distinct primitive regions retained once");
        check(Arrays.equals(b, new byte[] {1, 2, 3}), "input buffer unchanged");
    }

    private static void transactionalQuota() {
        byte[] source = {'A', 'B', 'B', 'C', 'C', 'C'};
        byte[] tags = {0, 1, 1, 1};
        int[] offsets = {0, 0, 1, 3};
        int[] lengths = {0, 1, 2, 3};
        M3ClassSpace store = new M3ClassSpace(3);
        expect(IllegalStateException.class, () -> store.internUtf8Batch(source, tags, offsets, lengths));
        check(store.imageCount() == 0 && store.retainedByteCount() == 0,
                "budget failure must not commit a valid prefix");
        check(store.intern(new byte[] {'A'}) == 0, "legacy interning still works after rejection");
        expect(IllegalStateException.class, () -> store.internUtf8Batch(source, tags, offsets, lengths));
        check(store.imageCount() == 1 && store.retainedByteCount() == 1,
                "preexisting symbols survive failed atomic admission");
        int[] remap = store.internUtf8Batch(source, tags, offsets,
                new int[] {0, 1, 1, 1});
        check(Arrays.equals(remap, new int[] {-1, 0, 1, 2}),
                "exactly enough space for additional unique symbols");
        check(store.retainedByteCount() == 3, "boundary exact budget");
        check(store.internUtf8Batch(source, tags, offsets,
                new int[] {0, 1, 1, 1})[2] == 1, "duplicate at full capacity succeeds");
        expect(IllegalStateException.class, () -> store.internUtf8Batch(source, tags, offsets, lengths));
        check(store.imageCount() == 3 && store.retainedByteCount() == 3,
                "no state corruption after repeat over-budget candidate");
        M3ClassSpace zero = new M3ClassSpace(1);
        int[] empty = zero.internUtf8Batch(new byte[0], new byte[] {0, 1, 1},
                new int[] {0, 0, 0}, new int[3]);
        check(Arrays.equals(empty, new int[] {-1, 0, 0}), "empty regions share one canonical ID");
        check(zero.imageCount() == 1 && zero.retainedByteCount() == 0,
                "zero-length canonical ID has zero retained bytes");
    }

    private static void callbacksAndCollisionChains() {
        AtomicInteger calls = new AtomicInteger();
        M3ClassSpace.DigestFunction collisions = value -> {
            calls.incrementAndGet();
            return new byte[32];
        };
        M3ClassSpace store = new M3ClassSpace(512, collisions);
        byte[] source = {'A', 'B', 'C', 'B', 'C', 'A'};
        byte[] tags = {0, 1, 1, 1, 1, 1, 1};
        int[] offsets = {0, 0, 1, 2, 3, 4, 5};
        int[] lengths = {0, 1, 1, 1, 1, 1, 1};
        int[] ids = store.internUtf8Batch(source, tags, offsets, lengths);
        check(Arrays.equals(ids, new int[] {-1, 0, 1, 2, 1, 2, 0}),
                "digest collisions do not merge unequal payloads");
        check(store.imageCount() == 3 && store.retainedByteCount() == 3,
                "collision chain retains three distinct byte payloads");
        check(calls.get() == 6, "one callback per requested symbol, including duplicates");
        check(Arrays.equals(store.copy(1), new byte[] {'B'}), "collision ordinal stable");
        check(Arrays.equals(store.internUtf8Batch(source, tags, offsets, lengths), ids),
                "collision replay is exact fixed point");
        check(store.imageCount() == 3, "fixed point did not allocate additional atoms");
        M3ClassSpace.DigestFunction invalid = bytes -> new byte[12];
        M3ClassSpace wrongDigest = new M3ClassSpace(100, invalid);
        expect(IllegalArgumentException.class, () -> wrongDigest.internUtf8Batch(source, tags,
                offsets, lengths));
        check(wrongDigest.imageCount() == 0, "invalid digest cannot mutate store");
        AtomicInteger attempts = new AtomicInteger();
        M3ClassSpace[] owner = new M3ClassSpace[1];
        owner[0] = new M3ClassSpace(100, bytes -> {
            if (attempts.getAndIncrement() == 0) {
                owner[0].intern(new byte[] {42});
            }
            return new byte[32];
        });
        expect(IllegalStateException.class, () -> owner[0].internUtf8Batch(source, tags,
                offsets, lengths));
        check(attempts.get() == 1 && owner[0].imageCount() == 0,
                "digest callback cannot mutate during batch preflight");
        check(owner[0].intern(new byte[] {7}) == 0,
                "reentry refusal flag resets after failure");
    }

    private static void digestCallbackFailureRollback() {
        AtomicInteger calls = new AtomicInteger();
        M3ClassSpace store = new M3ClassSpace(1024, bytes -> {
            if (calls.incrementAndGet() == 4) {
                throw new IllegalArgumentException("injected digest callback failure");
            }
            return new byte[32];
        });
        byte[] source = {'A', 'B', 'C', 'D', 'E', 'F'};
        expect(IllegalArgumentException.class, () -> store.internUtf8Batch(source,
                new byte[] {0, 1, 1, 1, 1, 1, 1},
                new int[] {0, 0, 1, 2, 3, 4, 5},
                new int[] {0, 1, 1, 1, 1, 1, 1}));
        check(calls.get() == 4 && store.imageCount() == 0
                && store.retainedByteCount() == 0,
                "digest failure must not commit any staged earlier atom");
        check(store.intern(new byte[] {'Z'}) == 0,
                "subsequent single-entry interning resumes normally");
    }

    private static void densePrimitiveLaneExpansion() {
        final int unique = 4096;
        byte[] source = new byte[unique * 2];
        byte[] tags = new byte[unique + 1];
        int[] offsets = new int[unique + 1];
        int[] lengths = new int[unique + 1];
        for (int i = 0; i < unique; i++) {
            source[i * 2] = (byte) ((i + 1) >>> 8);
            source[i * 2 + 1] = (byte) (i + 1);
            tags[i + 1] = 1;
            offsets[i + 1] = i * 2;
            lengths[i + 1] = 2;
        }
        M3ClassSpace store = new M3ClassSpace(unique * 2);
        int[] first = store.internUtf8Batch(source, tags, offsets, lengths);
        check(store.imageCount() == unique && store.retainedByteCount() == unique * 2,
                "growth across multiple digest/head capacity thresholds");
        for (int cp = 1; cp <= unique; cp += 61) {
            check(first[cp] == cp - 1, "deterministic dense CP insertion order");
            check(Arrays.equals(store.copy(first[cp]),
                    new byte[] {source[offsets[cp]], source[offsets[cp] + 1]}),
                    "dense primitive arena reconstructs the correct 2-byte region");
        }
        check(Arrays.equals(first, store.internUtf8Batch(source, tags, offsets, lengths)),
                "dense duplicate-only replay at full owner quota");
        expect(IllegalStateException.class, () -> store.internUtf8Batch(
                new byte[] {99}, new byte[] {0, 1}, new int[] {0, 0},
                new int[] {0, 1}));
        check(store.imageCount() == unique && store.retainedByteCount() == unique * 2,
                "over-budget refusal preserves full dense state");
    }

    private static void randomizedReferenceParity() {
        for (int seed = 0; seed < 6; seed++) {
            Random r = new Random(0x4D3341544F4D4CL + seed);
            M3ClassSpace legacy = new M3ClassSpace(8 * 1024 * 1024);
            M3ClassSpace atomic = new M3ClassSpace(8 * 1024 * 1024);
            List<byte[]> corpus = new ArrayList<>();
            for (int iteration = 0; iteration < 600; iteration++) {
                int symbols = 1 + r.nextInt(75);
                byte[] tags = new byte[symbols + 1];
                int[] offsets = new int[tags.length];
                int[] lengths = new int[tags.length];
                byte[] source = new byte[symbols * 72];
                int used = 0;
                for (int cp = 1; cp < tags.length; cp++) {
                    if (r.nextInt(6) == 0) continue;
                    tags[cp] = 1;
                    byte[] content;
                    if (!corpus.isEmpty() && r.nextBoolean()) {
                        content = corpus.get(r.nextInt(corpus.size()));
                    } else {
                        content = new byte[r.nextInt(65)];
                        r.nextBytes(content);
                        corpus.add(content.clone());
                    }
                    offsets[cp] = used;
                    lengths[cp] = content.length;
                    System.arraycopy(content, 0, source, used, content.length);
                    used += content.length;
                }
                int[] ids = atomic.internUtf8Batch(source, tags, offsets, lengths);
                for (int cp = 1; cp < tags.length; cp++) {
                    if (tags[cp] != 1) {
                        check(ids[cp] == -1, "non-UTF8 ordinal must not map to symbol");
                        continue;
                    }
                    int expected = legacy.internRegion(source, offsets[cp], lengths[cp]);
                    check(ids[cp] == expected, "batch ID matches serial historical admission");
                    check(Arrays.equals(legacy.copy(expected), atomic.copy(ids[cp])),
                            "bytes retained identically");
                    check(legacy.digestHex(expected).equals(atomic.digestHex(ids[cp])),
                            "hash candidate lane retains exact same digest");
                }
                check(legacy.imageCount() == atomic.imageCount(), "equal cardinality");
                check(legacy.retainedByteCount() == atomic.retainedByteCount(),
                        "equal retained byte budget accounting");
                if (iteration % 37 == 0) {
                    check(Arrays.equals(ids, atomic.internUtf8Batch(source, tags, offsets, lengths)),
                            "replay reaches exact symbol ID fixed point");
                }
            }
        }
    }

    private static void realClassfileFixedPoint() throws Exception {
        byte[] classfile;
        try (InputStream in = Objects.requireNonNull(
                M3ClassFileAtoms.class.getResourceAsStream("M3ClassFileAtoms.class"))) {
            classfile = in.readAllBytes();
        }
        M3ClassFileAtoms.Plan plan = M3ClassFileAtoms.scan(classfile);
        M3ClassSpace legacy = new M3ClassSpace(1024 * 1024);
        M3ClassSpace atomic = new M3ClassSpace(1024 * 1024);
        M3ClassFileAtoms.Interned before = plan.internInto(legacy);
        M3ClassFileAtoms.Interned after = plan.internAtomicallyInto(atomic);
        check(plan.utf8Count() > 20, "real Java21 CP exercised");
        for (int cp = 1; cp < plan.constantPoolCount(); cp++) {
            check(before.symbolIdAt(cp) == after.symbolIdAt(cp),
                    "real CP exact stable ID mapping");
        }
        check(legacy.imageCount() == atomic.imageCount(), "real CP distinct symbols parity");
        check(legacy.retainedByteCount() == atomic.retainedByteCount(), "real CP byte parity");
        M3ClassFileAtoms.Interned again = plan.internAtomicallyInto(atomic);
        for (int cp = 1; cp < plan.constantPoolCount(); cp++) {
            check(again.symbolIdAt(cp) == after.symbolIdAt(cp), "real CP replay fixed point");
        }
        check(atomic.imageCount() == legacy.imageCount(), "no extra symbols after replay");
        M3ClassSpace constrained = new M3ClassSpace(1);
        expect(IllegalStateException.class, () -> plan.internAtomicallyInto(constrained));
        check(constrained.imageCount() == 0 && constrained.retainedByteCount() == 0,
                "real classfile over-budget plan is all-or-nothing");
    }

    private static void parallelExactAdmission() throws Exception {
        byte[] source = "class C<T> { String p = \"[a-z]*\"; }".getBytes(StandardCharsets.UTF_8);
        byte[] tags = new byte[] {0, 1, 1, 1, 1, 1};
        int[] offsets = {0, 0, 6, 12, 18, 24};
        int[] lengths = {0, 6, 6, 6, 6, 6};
        M3ClassSpace store = new M3ClassSpace(512);
        ExecutorService pool = Executors.newFixedThreadPool(6);
        try {
            List<Callable<int[]>> tasks = new ArrayList<>();
            for (int i = 0; i < 192; i++) {
                tasks.add(() -> store.internUtf8Batch(source, tags, offsets, lengths));
            }
            List<Future<int[]>> futures = pool.invokeAll(tasks);
            int[] first = futures.getFirst().get();
            for (Future<int[]> f : futures) {
                check(Arrays.equals(first, f.get()), "parallel callers receive deterministic IDs");
            }
            check(store.imageCount() == 5, "exactly five parallel distinct source ranges");
        } finally {
            pool.shutdown();
            check(pool.awaitTermination(30, TimeUnit.SECONDS), "worker cleanup completed");
        }
    }
}
