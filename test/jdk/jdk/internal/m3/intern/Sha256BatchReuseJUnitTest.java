// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.intern;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Exact-byte and SHA-256 reset regression; a finite test is not equivalence proof. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/d5ad5090d87ef8477a56317e60ac0d29c65029f7/synexia-openrewrite-recipes/recipe-crates/m3-classspace-sha256-batch-20261030/src/test/java/com/synexia/m3/classspace/M3Sha256BatchReuseJUnitTest.java blob 62ab659cdf3c; recipe m3/intern/recipe/class-space-20261107/relocate.py v1
class Sha256BatchReuseJUnitTest {
    @Test void repeatedProviderResetsAndStableOrdinals() throws Exception {
        byte[] source = {7, 8, 7, 8};
        byte[] tags = {0, 1, 1, 1, 1};
        int[] offsets = {0, 0, 1, 2, 1};
        int[] lengths = {0, 2, 1, 2, 0};
        ClassSpace store = new ClassSpace(32);
        int[] expected = {-1, 0, 1, 0, 2};
        assertArrayEquals(expected, store.internUtf8Batch(source, tags, offsets, lengths));
        assertEquals(3, store.imageCount());
        assertEquals(3, store.retainedByteCount());
        for (int id = 0; id < 3; id++) {
            byte[] data = store.copy(id);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
            assertEquals(java.util.HexFormat.of().formatHex(digest), store.digestHex(id));
            byte[] mutableCopy = store.copy(id);
            if (mutableCopy.length > 0) mutableCopy[0] ^= 1;
            assertArrayEquals(data, store.copy(id));
        }
        assertArrayEquals(expected, store.internUtf8Batch(source, tags, offsets, lengths));
        assertArrayEquals(new byte[] {7, 8, 7, 8}, source);
        assertArrayEquals(new int[] {-1, -1}, store.internUtf8Batch(source,
                new byte[] {0, 3}, new int[] {0, -2}, new int[] {0, -1}));
    }

    @Test void randomizedSerialBatchParity() {
        Random r = new Random(0x4d33534841323536L);
        for (int round = 0; round < 150; round++) {
            byte[] source = new byte[512];
            r.nextBytes(source);
            byte[] tags = new byte[33];
            int[] offsets = new int[33];
            int[] lengths = new int[33];
            for (int cp = 1; cp < tags.length; cp++) {
                tags[cp] = (byte) (r.nextInt(5) == 0 ? 0 : 1);
                offsets[cp] = r.nextInt(source.length - 8);
                lengths[cp] = r.nextInt(9);
            }
            ClassSpace batch = new ClassSpace(4096);
            ClassSpace serial = new ClassSpace(4096);
            int[] ids = batch.internUtf8Batch(source, tags, offsets, lengths);
            for (int cp = 1; cp < tags.length; cp++) {
                if (tags[cp] != 1) {
                    assertEquals(-1, ids[cp]);
                } else {
                    int id = serial.internRegion(source, offsets[cp], lengths[cp]);
                    assertEquals(id, ids[cp]);
                    assertArrayEquals(serial.copy(id), batch.copy(ids[cp]));
                    assertEquals(serial.digestHex(id), batch.digestHex(ids[cp]));
                }
            }
            assertEquals(serial.imageCount(), batch.imageCount());
            assertEquals(serial.retainedByteCount(), batch.retainedByteCount());
        }
    }

    @Test void customCallbacksAndQuotaRemainUnchanged() {
        AtomicInteger callbacks = new AtomicInteger();
        ClassSpace collisions = new ClassSpace(32, bytes -> {
            callbacks.incrementAndGet();
            return new byte[32];
        });
        byte[] source = {1, 2, 1, 2};
        assertArrayEquals(new int[] {-1, 0, 1, 0, 2}, collisions.internUtf8Batch(
                source, new byte[] {0, 1, 1, 1, 1},
                new int[] {0, 0, 1, 2, 0}, new int[] {0, 2, 1, 2, 1}));
        assertEquals(4, callbacks.get());
        assertEquals(3, collisions.imageCount());
        ClassSpace quota = new ClassSpace(1);
        assertThrows(IllegalStateException.class, () -> quota.internUtf8Batch(
                new byte[] {1, 2}, new byte[] {0, 1, 1},
                new int[] {0, 0, 1}, new int[] {0, 1, 1}));
        assertEquals(0, quota.imageCount());
        assertEquals(0, quota.retainedByteCount());
    }
}
