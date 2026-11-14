// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.intern;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Random;

/** Unchanged-source-compatible freeze oracle, compiled against old and new owner images. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/d5ad5090d87ef8477a56317e60ac0d29c65029f7/synexia-openrewrite-recipes/recipe-crates/m3-classspace-archive-copyto-20261030/src/test/java/com/synexia/m3/classspace/M3ArchiveFreezeParitySelfTest.java blob 62ead79f3a96; recipe make/m3/intern/recipe/class-space-20261107/relocate.py v1
public final class ArchiveFreezeParitySelfTest {
    private static long checks;

    private ArchiveFreezeParitySelfTest() { }

    private static void check(boolean condition, String context) {
        checks++;
        if (!condition) throw new AssertionError(context);
    }

    public static void main(String[] args) throws Exception {
        MessageDigest roots = MessageDigest.getInstance("SHA-256");
        for (int seed = 0; seed < 22; seed++) {
            ClassSpace space = new ClassSpace(1 << 20);
            Random random = new Random(0x53c149b6L ^ seed);
            int count = seed == 0 ? 0 : 28 + seed * 23;
            for (int i = 0; i < count; i++) {
                byte[] bytes;
                if (i % 7 == 0 && space.imageCount() != 0) {
                    bytes = space.copy(random.nextInt(space.imageCount()));
                } else {
                    bytes = new byte[(i + seed) % 11 == 0 ? 0 : random.nextInt(197)];
                    random.nextBytes(bytes);
                }
                int id = space.intern(bytes);
                check(Arrays.equals(space.copy(id), bytes), "intern " + seed + ':' + i);
            }
            byte[] frame = ClassSpaceArchive.freeze(space);
            ClassSpaceArchive.View opened = ClassSpaceArchive.openCanonicalCopy(frame);
            check(opened.count() == space.imageCount(), "atom count " + seed);
            check(opened.payloadBytes() == space.retainedByteCount(), "payload " + seed);
            check(Arrays.equals(frame, ClassSpaceArchive.freeze(opened.restore())),
                    "canonical replay " + seed);
            for (int id = 0; id < space.imageCount(); id++) {
                check(Arrays.equals(space.copy(id), opened.copyAt(id)), "ordinal " + seed + ':' + id);
            }
            roots.update((byte) seed);
            roots.update(frame);
            check(Arrays.equals(frame, ClassSpaceArchive.freeze(space)), "stable " + seed);
        }
        System.out.println("M3_ARCHIVE_FREEZE_PARITY sha256="
                + HexFormat.of().formatHex(roots.digest()) + " checks=" + checks);
    }
}
