// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.intern;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Java21 regression of ordinary owner growth; generated OOME fixtures run separately. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/d5ad5090d87ef8477a56317e60ac0d29c65029f7/synexia-openrewrite-recipes/recipe-crates/m3-classspace-failure-atomic-growth-20261030/src/test/java/com/synexia/m3/classspace/M3GrowthSafetyJUnitTest.java blob 6a26956cbbca; recipe m3/intern/recipe/class-space-20261107/relocate.py v1
class GrowthSafetyJUnitTest {
    @Test void serialAndHashBucketGrowthKeepsStableIds() {
        ClassSpace store = new ClassSpace(1024 * 1024);
        int bytes = 0;
        for (int i = 0; i < 2500; i++) {
            byte[] atom = ("jdk/m3/fixture/Class" + i + "#method").getBytes(StandardCharsets.UTF_8);
            bytes += atom.length;
            assertEquals(i, store.intern(atom));
            assertEquals(i + 1, store.imageCount());
            assertEquals(bytes, store.retainedByteCount());
            assertArrayEquals(atom, store.copy(i));
        }
        Random random = new Random(0x20261030);
        for (int i = 0; i < 8000; i++) {
            int index = random.nextInt(2500);
            byte[] atom = ("jdk/m3/fixture/Class" + index + "#method")
                    .getBytes(StandardCharsets.UTF_8);
            assertEquals(index, store.intern(atom));
        }
        assertEquals(2500, store.imageCount());
        assertEquals(bytes, store.retainedByteCount());
    }

    @Test void exactCollisionAndBudgetContractsSurviveRebuild() {
        ClassSpace store = new ClassSpace(512, bytes -> new byte[32]);
        for (int i = 0; i < 85; i++) {
            byte[] candidate = {(byte) i, (byte) (i >>> 8), 1, 2};
            assertEquals(i, store.intern(candidate));
            assertArrayEquals(candidate, store.copy(i));
        }
        for (int i = 0; i < 85; i++) {
            assertEquals(i, store.intern(new byte[] {(byte) i, (byte) (i >>> 8), 1, 2}));
        }
        ClassSpace budget = new ClassSpace(8);
        byte[] atom = new byte[8];
        Arrays.fill(atom, (byte) 7);
        assertEquals(0, budget.intern(atom));
        assertThrows(IllegalStateException.class, () -> budget.intern(new byte[] {8}));
        assertEquals(1, budget.imageCount());
        assertEquals(8, budget.retainedByteCount());
        assertEquals(0, budget.intern(atom));
        assertEquals(1, budget.intern(new byte[0]));
    }

    @Test void batchParityAcrossRepeatedCapacityChanges() {
        ClassSpace serial = new ClassSpace(1024 * 1024);
        ClassSpace batch = new ClassSpace(1024 * 1024);
        Random random = new Random(789012);
        for (int pass = 0; pass < 75; pass++) {
            byte[] source = new byte[512];
            random.nextBytes(source);
            byte[] tags = new byte[48];
            int[] offsets = new int[48], lengths = new int[48];
            for (int cp = 1; cp < tags.length; cp++) {
                if (cp % 7 == 0) continue;
                tags[cp] = 1;
                offsets[cp] = random.nextInt(490);
                lengths[cp] = random.nextInt(22);
            }
            int[] ids = batch.internUtf8Batch(source, tags, offsets, lengths);
            for (int cp = 1; cp < tags.length; cp++) {
                if (tags[cp] == 1) {
                    int expected = serial.internRegion(source, offsets[cp], lengths[cp]);
                    assertEquals(expected, ids[cp]);
                    assertArrayEquals(serial.copy(expected), batch.copy(ids[cp]));
                } else {
                    assertEquals(-1, ids[cp]);
                }
            }
            assertEquals(serial.imageCount(), batch.imageCount());
            assertEquals(serial.retainedByteCount(), batch.retainedByteCount());
        }
    }
}
