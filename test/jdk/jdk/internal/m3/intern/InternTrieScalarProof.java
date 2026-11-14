// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.intern;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/** Primitive scalar reads must exactly match copied tuple snapshots. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/fd9a233680b43197eb8c5745a729c342ca56c4e2/synexia-common/src/test/java/com/synexia/common/intern/M3InternTrieScalarProof.java blob 55eccf9ab7a1; recipe m3/intern/recipe/intern-space-20261106/relocate.py v4
public final class InternTrieScalarProof {
    private static long checks;
    private static final int ROOT_KIND = 0x4d335052;
    private static final int NODE_KIND = 0x4d33504e;
    private InternTrieScalarProof() { }

    public static void main(String[] args) throws Exception {
        run();
        System.out.println("InternTrieScalarProof PASS checks=" + checks);
    }

    public static void run() throws Exception {
        checks = 0;
        InternSpace space = new InternSpace();
        InternPathTrie trie = new InternPathTrie(space, 42);
        InternPathTrie foreign = new InternPathTrie(space, 43);
        InternSpace.TupleStore tuples = space.shapes();
        long root = trie.root();
        long otherRoot = foreign.root();
        equal(ROOT_KIND, tuples.kind(root));
        equal(42L, tuples.members(root)[0]);
        equal(0, trie.depth(root));
        equal(0L, trie.parent(root));
        rejects(IllegalArgumentException.class,
                () -> tuples.scopedMember(root, NODE_KIND, 4, root, 1));
        rejects(IndexOutOfBoundsException.class,
                () -> tuples.scopedMember(0, NODE_KIND, 4, root, 1));

        Random random = new Random(0x20261101L);
        long[] nodes = new long[3_000];
        int[] depths = new int[nodes.length];
        nodes[0] = root;
        for (int i = 1; i < nodes.length; i++) {
            int parent = random.nextInt(i);
            nodes[i] = trie.append(nodes[parent], "name" + random.nextInt(200));
            depths[i] = depths[parent] + 1;
            equal(depths[i], trie.depth(nodes[i]));
            equal(nodes[parent], trie.parent(nodes[i]));
            long[] copy = tuples.members(nodes[i]);
            equal(4, copy.length);
            equal(root, copy[0]);
            equal(NODE_KIND, tuples.kind(nodes[i]));
            for (int at = 0; at < 4; at++) {
                equal(copy[at], tuples.scopedMember(nodes[i], NODE_KIND, 4, root, at));
            }
            copy[1] = -1;
            equal(nodes[parent], trie.parent(nodes[i]));
        }

        // Store-level negative gates: wrong expected kind, width, owner and field.
        long example = nodes[1];
        rejects(IllegalArgumentException.class,
                () -> tuples.scopedMember(example, ROOT_KIND, 4, root, 1));
        rejects(IllegalArgumentException.class,
                () -> tuples.scopedMember(example, NODE_KIND, 3, root, 1));
        rejects(IllegalArgumentException.class,
                () -> tuples.scopedMember(example, NODE_KIND, 4, otherRoot, 1));
        rejects(IndexOutOfBoundsException.class,
                () -> tuples.scopedMember(example, NODE_KIND, 4, root, 4));
        rejects(IndexOutOfBoundsException.class,
                () -> tuples.scopedMember(example, NODE_KIND, 4, root, -1));
        rejects(IllegalArgumentException.class, () -> trie.depth(otherRoot));
        rejects(IllegalArgumentException.class, () -> trie.depth(foreign.append(otherRoot, "x")));

        // These are deliberately forged primitive tuples in the SAME public
        // TupleStore; the PathTrie must retain its previous fail-closed gates.
        long atom = space.internString("forged");
        long invalidParent = tuples.intern(NODE_KIND, root, 0, atom, 1);
        long invalidDepth = tuples.intern(NODE_KIND, root, root, atom, 0);
        long excessiveDepth = tuples.intern(NODE_KIND, root, root, atom, 1_000_001);
        long wrongWidth = tuples.intern(NODE_KIND, root, root, atom);
        for (long malformed : new long[]{invalidParent,invalidDepth,excessiveDepth,wrongWidth}) {
            rejects(IllegalArgumentException.class, () -> trie.depth(malformed));
            rejects(IllegalArgumentException.class, () -> trie.parent(malformed));
            rejects(IllegalArgumentException.class, () -> trie.lastSegment(malformed));
        }

        // Independent root-to-node identities must retain the exact original
        // lexical prefix contract even when the common prefix is shallow.
        for (int trial = 0; trial < 4_000; trial++) {
            long a = nodes[random.nextInt(nodes.length)];
            long b = nodes[random.nextInt(nodes.length)];
            String[] aSegments = trie.segments(a);
            String[] bSegments = trie.segments(b);
            int common = 0;
            while (common < Math.min(aSegments.length, bSegments.length)
                    && aSegments[common].equals(bSegments[common])) common++;
            equal(common, trie.commonPrefixLength(a,b));
            equal(common == aSegments.length, trie.isPrefix(a,b));
            equal(common == bSegments.length, trie.isPrefix(b,a));
            equal(true, Arrays.equals(aSegments,trie.segments(a)));
        }

        // Verify native-free lock and publication safety with many concurrent
        // readers of a fully frozen space; no mutable tuple facade is exposed.
        space.freeze();
        CountDownLatch begin = new CountDownLatch(1);
        CountDownLatch end = new CountDownLatch(4);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        for (int thread = 0; thread < 4; thread++) {
            Thread worker = new Thread(() -> {
                try {
                    begin.await();
                    for (int i = 0; i < 1_000; i++) {
                        long id = nodes[i];
                        if (trie.depth(id) != depths[i]
                                || trie.commonPrefixLength(id,id) != depths[i]) {
                            throw new AssertionError("read after freeze");
                        }
                    }
                } catch (Throwable problem) { failure.compareAndSet(null,problem); }
                finally { end.countDown(); }
            });
            worker.start();
        }
        begin.countDown();
        end.await();
        if (failure.get()!=null) throw new AssertionError("concurrent read",failure.get());
        equal(true, space.isFrozen());

        ByteArrayOutputStream archive = new ByteArrayOutputStream();
        space.writeTo(new DataOutputStream(archive));
        InternSpace replay = InternSpace.readFrom(
                new DataInputStream(new ByteArrayInputStream(archive.toByteArray())));
        InternPathTrie restored = new InternPathTrie(replay,42);
        for (int i=0; i<Math.min(300,nodes.length); i++) {
            equal(depths[i],restored.depth(nodes[i]));
            equal(true,Arrays.equals(trie.segments(nodes[i]),restored.segments(nodes[i])));
        }
        equal(true,replay.isFrozen());
    }

    private static void equal(Object expected,Object actual) {
        if (!expected.equals(actual)) throw new AssertionError("expected="+expected+" actual="+actual);
        checks++;
    }
    private static void rejects(Class<? extends Throwable> type,Runnable task) {
        try { task.run(); } catch(Throwable error) {
            if (type.isInstance(error)) { checks++; return; }
            throw new AssertionError("unexpected exception",error);
        }
        throw new AssertionError("did not reject "+type);
    }
}
