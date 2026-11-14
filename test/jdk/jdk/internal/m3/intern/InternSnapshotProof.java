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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** Differential checks of immutable primitive snapshots under caller mutation. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/fd9a233680b43197eb8c5745a729c342ca56c4e2/synexia-common/src/test/java/com/synexia/common/intern/M3InternSnapshotProof.java blob 33f6037d8edb; recipe m3/intern/recipe/intern-space-20261106/relocate.py v4
public final class InternSnapshotProof {
    private static long checks;
    private InternSnapshotProof() { }

    public static void main(String[] args) throws Exception {
        run();
        System.out.println("InternSnapshotProof PASS checks=" + checks);
    }
    public static void run() throws Exception {
        checks = 0;
        Random random = new Random(0x65fa5bba);
        InternSpace space = new InternSpace();
        for (int size : new int[] {0, 1, 2, 7, 63, 127, 65535, 65536, 65537, 131071}) {
            byte[] input = new byte[size];
            random.nextBytes(input);
            byte[] before = input.clone();
            long id = space.blobs().intern(input);
            equal(id, space.blobs().intern(before));
            if (size != 0) input[0] ^= 1;
            equal(true, Arrays.equals(before, space.blobs().copy(id)));
            byte[] copy = space.blobs().copy(id);
            if (size != 0) copy[0] ^= 1;
            equal(true, Arrays.equals(before, space.blobs().copy(id)));
        }
        for (int size : new int[] {0, 1, 2, 8191, 8192, 8193, 16385}) {
            long[] input = new long[size];
            for (int i = 0; i < size; i++) input[i] = random.nextLong();
            long[] before = input.clone();
            long id = space.shapes().intern(1234, input);
            equal(id, space.shapes().intern(1234, before));
            if (size != 0) input[0] ^= 1;
            equal(true, Arrays.equals(before, space.shapes().members(id)));
            equal(false, space.shapes().intern(1235, before) == id);
        }
        // The original implementation associated a pre-write hash with
        // post-write payload bytes in all 12 baseline runs on Java21.
        for (int trial = 0; trial < 8; trial++) {
            InternSpace local = new InternSpace();
            byte[] input = new byte[8 * 1024 * 1024];
            AtomicLong handle = new AtomicLong();
            CountDownLatch started = new CountDownLatch(1);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread writer = new Thread(() -> {
                try {
                    started.countDown();
                    handle.set(local.blobs().intern(input));
                } catch (Throwable error) { failure.set(error); }
            });
            writer.start();
            started.await();
            Thread.sleep(3);
            input[0] = 1;
            writer.join();
            if (failure.get() != null) throw new AssertionError(failure.get());
            byte[] stored = local.blobs().copy(handle.get());
            equal(handle.get(), local.blobs().intern(stored));
            equal(1, local.blobs().size());
            equal(true, stored[0] == 0 || stored[0] == 1);
        }
        for (int trial = 0; trial < 8; trial++) {
            InternSpace local = new InternSpace();
            long[] input = new long[1_000_000];
            AtomicLong handle = new AtomicLong();
            CountDownLatch started = new CountDownLatch(1);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread writer = new Thread(() -> {
                try {
                    started.countDown();
                    handle.set(local.shapes().intern(2701, input));
                } catch (Throwable error) { failure.set(error); }
            });
            writer.start();
            started.await();
            Thread.sleep(3);
            input[0] = 9;
            writer.join();
            if (failure.get() != null) throw new AssertionError(failure.get());
            long[] stored = local.shapes().members(handle.get());
            equal(handle.get(), local.shapes().intern(2701, stored));
            equal(1, local.shapes().size());
            equal(true, stored[0] == 0 || stored[0] == 9);
        }
        space.freeze();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        space.writeTo(new DataOutputStream(stream));
        InternSpace replay = InternSpace.readFrom(new DataInputStream(
                new ByteArrayInputStream(stream.toByteArray())));
        equal(true, replay.isFrozen());
        equal(space.blobs().size(), replay.blobs().size());
        equal(space.shapes().size(), replay.shapes().size());
    }
    private static void equal(Object a, Object b) {
        if (!a.equals(b)) throw new AssertionError("expected " + a + " actual " + b);
        checks++;
    }
}
