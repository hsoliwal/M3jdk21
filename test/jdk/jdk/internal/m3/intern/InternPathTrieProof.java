// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.intern;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/** Differential original-owner path identity, prefix and archive oracle. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/fd9a233680b43197eb8c5745a729c342ca56c4e2/synexia-common/src/test/java/com/synexia/common/intern/M3InternPathTrieProof.java blob 1d52df26e517; recipe m3/intern/recipe/intern-space-20261106/relocate.py v4
public final class InternPathTrieProof {
    private static int checks;

    private InternPathTrieProof() { }

    public static void main(String[] arguments) throws Exception {
        run();
        System.out.println("InternPathTrieProof PASS checks=" + checks);
    }

    public static void run() throws Exception {
        checks = 0;
        InternSpace space = new InternSpace();
        InternPathTrie one = new InternPathTrie(space, 10);
        InternPathTrie another = new InternPathTrie(space, 10);
        InternPathTrie foreign = new InternPathTrie(space, 20);
        equal(one.root(), another.root());
        equal(0, one.depth(one.root()));
        equal(0L, one.parent(one.root()));
        equal(true, one.isPrefix(one.root(), one.root()));
        equal(0, one.commonPrefixLength(one.root(), one.root()));
        long ab = one.intern("a", "b");
        long ac = one.intern("a", "c");
        long a = one.intern("a");
        equal(ab, another.intern("a", "b"));
        equal(ab, one.append(a, "b"));
        equal(a, one.parent(ab));
        equal("b", one.lastSegment(ab));
        equal(2, one.depth(ab));
        equal(true, one.isPrefix(a, ab));
        equal(false, one.isPrefix(ab, a));
        equal(true, one.isPrefix(ab, ab));
        equal(false, one.isPrefix(ab, ac));
        equal(1, one.commonPrefixLength(ab, ac));
        equal(2, one.commonPrefixLength(ab, ab));
        equal(0, one.commonPrefixLength(ab, one.root()));
        equal(false, foreign.intern("a", "b") == ab);
        expect(IllegalArgumentException.class, () -> one.depth(foreign.root()));
        expect(IllegalArgumentException.class, () -> one.isPrefix(foreign.intern("a"), ab));
        expect(IllegalStateException.class, () -> one.findExisting("a"));
        expect(IllegalArgumentException.class, () -> new InternPathTrie(space, -1));
        expect(NullPointerException.class, () -> one.intern((String[]) null));
        expect(NullPointerException.class, () -> one.intern("a", null));
        expect(IndexOutOfBoundsException.class, () -> one.depth(0));
        expect(IllegalArgumentException.class, () -> one.lastSegment(one.root()));
        long forged = space.shapes().intern(73, 1L);
        expect(IllegalArgumentException.class, () -> one.depth(forged));

        String[] raw = {"", ".", "..", "a/b", "B", "b", "\uD800", "\uDC00", "\uD83D\uDE00", "é", "a\\b"};
        long uncommon = one.intern(raw);
        raw[0] = "mutated";
        String[] expected = {"", ".", "..", "a/b", "B", "b", "\uD800", "\uDC00", "\uD83D\uDE00", "é", "a\\b"};
        equal(true, Arrays.equals(expected, one.segments(uncommon)));
        String[] returned = one.segments(uncommon);
        returned[0] = "changed output";
        equal(true, Arrays.equals(expected, one.segments(uncommon)));
        equal(false, uncommon == one.intern(".", "..", "a/b", "B", "b", "\uD800", "\uDC00", "\uD83D\uDE00", "é", "a\\b"));

        Random random = new Random(0x20261031L);
        String[] alphabet = {"", "a", "b", "c", "d", "f", "G", "a/a", "X", "é", "\uD800", "\uD83D\uDE00"};
        Map<List<String>, Long> reference = new HashMap<>();
        List<String[]> inputs = new ArrayList<>();
        List<Long> handles = new ArrayList<>();
        for (int trial = 0; trial < 5000; trial++) {
            String[] path = new String[random.nextInt(21)];
            for (int j = 0; j < path.length; j++) path[j] = alphabet[random.nextInt(alphabet.length)];
            long id = one.intern(path);
            Long old = reference.putIfAbsent(List.of(path), id);
            if (old != null) equal(old, id);
            equal(path.length, one.depth(id));
            equal(true, Arrays.equals(path, one.segments(id)));
            if (path.length > 0) {
                equal(path[path.length - 1], one.lastSegment(id));
                String[] prefix = Arrays.copyOf(path, path.length - 1);
                long predecessor = one.intern(prefix);
                equal(predecessor, one.parent(id));
                equal(true, one.isPrefix(predecessor, id));
            }
            inputs.add(path);
            handles.add(id);
        }
        for (int i = 0; i < 7000; i++) {
            int left = random.nextInt(inputs.size());
            int right = random.nextInt(inputs.size());
            String[] aPath = inputs.get(left);
            String[] bPath = inputs.get(right);
            int expectedCommon = 0;
            while (expectedCommon < Math.min(aPath.length, bPath.length)
                    && aPath[expectedCommon].equals(bPath[expectedCommon])) expectedCommon++;
            equal(expectedCommon, one.commonPrefixLength(handles.get(left), handles.get(right)));
            equal(expectedCommon == aPath.length, one.isPrefix(handles.get(left), handles.get(right)));
        }
        // Synchronized canonical arrays supply thread safety; no per-node objects.
        CountDownLatch gate = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(6);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        for (int thread = 0; thread < 6; thread++) {
            Thread worker = new Thread(() -> {
                try {
                    gate.await();
                    for (int i = 0; i < 250; i++) {
                        long got = another.intern("thread", "key" + (i % 13), "end");
                        if (got != one.intern("thread", "key" + (i % 13), "end")) {
                            throw new AssertionError("duplicate path handle");
                        }
                    }
                } catch (Throwable problem) { failure.compareAndSet(null, problem); }
                finally { finished.countDown(); }
            });
            worker.start();
        }
        gate.countDown();
        finished.await();
        if (failure.get() != null) throw new AssertionError("concurrent trie interning", failure.get());
        equal(true, one.isPrefix(one.root(), one.intern("thread", "key2", "end")));

        // Compare tuple-cell capacity for many paths sharing the same spine.
        // This is a structural count, NOT a measured retained-heap benchmark.
        InternSpace flatSpace = new InternSpace();
        InternSpace trieSpace = new InternSpace();
        InternPathTrie packed = new InternPathTrie(trieSpace, 47);
        for (int i = 0; i < 2_000; i++) {
            String[] components = {"repository", "src", "main", "java", "com", "example",
                    "pkg" + (i % 40), "File" + i + ".java"};
            flatSpace.internPath(47, components);
            packed.intern(components);
        }
        equal(2_000, flatSpace.shapes().size());
        equal(2_047, trieSpace.shapes().size()); // root, 6 common, 40 pkg, 2k files
        equal(true, (1 + 4 * (2_047 - 1)) < 9 * 2_000);

        space.freeze();
        int frozenAtoms = space.atoms().size();
        int frozenShapes = space.shapes().size();
        equal(a, one.findExisting("a"));
        equal(ab, one.findExisting("a", "b"));
        equal(0L, one.findExisting("a", "b", "never-seen"));
        equal(0L, one.findExisting("not-seen-before"));
        equal(one.root(), one.findExisting());
        equal(frozenAtoms, space.atoms().size());
        equal(frozenShapes, space.shapes().size());
        expect(IllegalStateException.class, () -> one.intern("brand-new"));
        expect(IllegalStateException.class, () -> one.append(a, "new-tail"));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        space.writeTo(new DataOutputStream(bytes));
        byte[] saved = bytes.toByteArray();
        InternSpace restored = InternSpace.readFrom(
                new DataInputStream(new ByteArrayInputStream(saved)));
        InternPathTrie replay = new InternPathTrie(restored, 10);
        equal(ab, replay.findExisting("a", "b"));
        equal(uncommon, replay.intern(expected));
        equal(true, Arrays.equals(expected, replay.segments(uncommon)));
        equal(1, replay.commonPrefixLength(ab, ac));
        ByteArrayOutputStream copy = new ByteArrayOutputStream();
        restored.writeTo(new DataOutputStream(copy));
        equal(true, Arrays.equals(saved, copy.toByteArray()));
    }

    private static void equal(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("expected=" + expected + ", actual=" + actual);
        }
        checks++;
    }

    private static void expect(Class<? extends Throwable> type, Runnable command) {
        try {
            command.run();
        } catch (Throwable exception) {
            if (type.isInstance(exception)) { checks++; return; }
            throw new AssertionError("expected " + type.getName() + ", found " + exception, exception);
        }
        throw new AssertionError("expected exception " + type.getName());
    }
}
