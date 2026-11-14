// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.intern;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/** Standalone Java 21 behavioral proof; also invoked from the module's JUnit bridge. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/fd9a233680b43197eb8c5745a729c342ca56c4e2/synexia-common/src/test/java/com/synexia/common/intern/M3InternSpaceProof.java blob 39f66fc2a137; recipe make/m3/intern/recipe/intern-space-20261106/relocate.py v4
public final class InternSpaceProof {
    private static long checks;

    private InternSpaceProof() { }

    public static void main(String[] args) throws Exception {
        run();
        System.out.println("InternSpaceProof PASS checks=" + checks);
    }

    public static void run() throws Exception {
        checks = 0;
        InternSpace m = new InternSpace();
        long empty = m.internString("");
        eq("", m.string(empty));
        eq(empty, m.internString(""));
        for (String str : new String[] {"alpha", "A\u0000\ud800?", "\udc00", "\ud83d\ude00", "é", "𐐀"}) {
            long id = m.internString(str);
            eq(str, m.string(id));
            eq(id, m.internString(new String(str.toCharArray())));
        }

        byte[] mutable = {1, 2, 3};
        long blob = m.blobs().intern(mutable);
        mutable[1] = 88;
        eq(true, Arrays.equals(new byte[] {1, 2, 3}, m.blobs().copy(blob)));
        byte[] escape = m.blobs().copy(blob);
        escape[2] = 99;
        eq(true, Arrays.equals(new byte[] {1, 2, 3}, m.blobs().copy(blob)));
        eq(blob, m.blobs().intern(new byte[] {1, 2, 3}));
        eq(0, m.blobs().length(m.blobs().intern(new byte[0])));

        long[] members = {9, 7, 13};
        long shape = m.shapes().intern(1, members);
        members[0] = -5;
        eq(true, Arrays.equals(new long[] {9, 7, 13}, m.shapes().members(shape)));
        long[] exposed = m.shapes().members(shape);
        exposed[0] = 777;
        eq(shape, m.shapes().intern(1, 9, 7, 13));
        eq(false, m.shapes().intern(2, 9, 7, 13) == shape);
        eq(false, m.shapes().intern(1, 9, 7, 14) == shape);
        eq(1, m.shapes().kind(shape));

        long class1 = m.internType(10, "Lcom/example/Foo;");
        long class2 = m.internType(11, "Lcom/example/Foo;");
        eq(false, class1 == class2);
        eq(class1, m.internType(10, "Lcom/example/Foo;"));
        long generic = m.internType(10, "Ljava/util/List;", class1);
        eq(generic, m.internType(10, "Ljava/util/List;", class1));
        refuses(IllegalArgumentException.class, () -> m.internType(10, "bad", shape));
        refuses(IllegalArgumentException.class, () -> m.internType(-1, "bad"));

        long path = m.internPath(42, "dir", "..", "File.java");
        eq(path, m.internPath(42, "dir", "..", "File.java"));
        eq(false, path == m.internPath(43, "dir", "..", "File.java"));
        eq(false, path == m.internPath(42, "File.java"));
        long[] pathMembers = m.shapes().members(path);
        eq(42L, pathMembers[0]);
        eq("..", m.string(pathMembers[2]));

        int[] original = {0, -1, Integer.MIN_VALUE, Integer.MAX_VALUE};
        long frozen = m.internFrozenInts(original);
        original[0] = 400;
        int[] first = m.frozenInts(frozen);
        first[0] = 500;
        eq(true, Arrays.equals(new int[] {0, -1, Integer.MIN_VALUE, Integer.MAX_VALUE}, m.frozenInts(frozen)));
        eq(frozen, m.internFrozenInts(new int[] {0, -1, Integer.MIN_VALUE, Integer.MAX_VALUE}));
        refuses(IllegalArgumentException.class, () -> m.frozenInts(path));

        long leaf = m.graphs().intern(3, class1);
        long root = m.graphs().intern(9, generic, leaf, leaf);
        eq(root, m.graphs().intern(9, generic, leaf, leaf));
        eq(9, m.graphs().opcode(root));
        eq(generic, m.graphs().payload(root));
        eq(true, Arrays.equals(new long[] {leaf, leaf}, m.graphs().children(root)));
        refuses(IllegalArgumentException.class, () -> m.graphs().intern(9, 0, root + 1));
        refuses(IndexOutOfBoundsException.class, () -> m.graphs().opcode(0));
        refuses(IndexOutOfBoundsException.class, () -> m.shapes().kind(99999));

        // More than one byte page and several long pages; exercise arena crossover,
        // hash-slot collisions, table growth and reverse lookups with seeded inputs.
        Random random = new Random(0x1f3a93ddL);
        long[] ids = new long[4000];
        byte[][] expected = new byte[4000][];
        for (int i = 0; i < ids.length; i++) {
            byte[] item = new byte[31 + i % 19];
            random.nextBytes(item);
            expected[i] = item;
            ids[i] = m.blobs().intern(item);
            eq(ids[i], m.blobs().intern(item));
            long[] tuple = {i, ids[i], i % 17};
            long idx = m.shapes().intern(800, tuple);
            eq(idx, m.shapes().intern(800, tuple));
        }
        for (int i = 0; i < ids.length; i++) {
            eq(true, Arrays.equals(expected[i], m.blobs().copy(ids[i])));
        }
        eq(true, m.blobs().storedBytes() > 65536L);

        // Concurrent duplicate contention: no lost inserts or duplicate canonical IDs.
        InternSpace concurrent = new InternSpace();
        int threads = 6;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        for (int t = 0; t < threads; t++) {
            Thread worker = new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < 1500; i++) {
                        long id = concurrent.internString("key-" + (i % 37));
                        eq("key-" + (i % 37), concurrent.string(id));
                    }
                } catch (Throwable ex) {
                    failure.compareAndSet(null, ex);
                } finally {
                    done.countDown();
                }
            }, "m3-intern-proof-" + t);
            worker.start();
        }
        start.countDown();
        done.await();
        if (failure.get() != null) throw new AssertionError("concurrent proof", failure.get());
        eq(37, concurrent.atoms().size());

        m.freeze();
        eq(true, m.isFrozen());
        eq(shape, m.shapes().intern(1, 9, 7, 13));
        refuses(IllegalStateException.class, () -> m.shapes().intern(999, 1, 2));
        refuses(IllegalStateException.class, () -> m.internString("unseen"));
        refuses(IllegalStateException.class, () -> m.blobs().intern(new byte[] {92}));
        refuses(IllegalStateException.class, () -> m.graphs().intern(345, 1));

        byte[] archive = archive(m);
        InternSpace restored = InternSpace.readFrom(new DataInputStream(new ByteArrayInputStream(archive)));
        eq(true, restored.isFrozen());
        eq(shape, restored.shapes().intern(1, 9, 7, 13));
        eq(root, restored.graphs().intern(9, generic, leaf, leaf));
        eq(frozen, restored.internFrozenInts(new int[] {0, -1, Integer.MIN_VALUE, Integer.MAX_VALUE}));
        eq(true, Arrays.equals(archive, archive(restored)));
        refuses(IOException.class, () -> InternSpace.readFrom(
                new DataInputStream(new ByteArrayInputStream(Arrays.copyOf(archive, archive.length - 4)))));
        byte[] header = archive.clone();
        header[0] = 0;
        refuses(IOException.class, () -> InternSpace.readFrom(
                new DataInputStream(new ByteArrayInputStream(header))));
        refuses(IllegalStateException.class, () -> archive(new InternSpace()));
        System.out.println("archiveBytes=" + archive.length + " atoms=" + m.atoms().size()
                + " blobs=" + m.blobs().size() + " tuples=" + m.shapes().size());
    }

    private static byte[] archive(InternSpace space) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        space.writeTo(new DataOutputStream(bytes));
        return bytes.toByteArray();
    }

    private static synchronized void eq(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
        checks++;
    }

    private static void refuses(Class<? extends Throwable> type, ThrowingRunnable action) {
        try {
            action.run();
        } catch (Throwable error) {
            if (type.isInstance(error)) {
                synchronized (InternSpaceProof.class) { checks++; }
                return;
            }
            throw new AssertionError("expected " + type.getSimpleName() + " got " + error, error);
        }
        throw new AssertionError("expected " + type.getSimpleName());
    }

    @FunctionalInterface
    private interface ThrowingRunnable { void run() throws Exception; }
}
