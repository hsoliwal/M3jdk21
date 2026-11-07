// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.intern;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

/** Minimal independently runnable acceptance oracle for the named source recipe. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/d5ad5090d87ef8477a56317e60ac0d29c65029f7/synexia-openrewrite-recipes/recipe-crates/m3-classspace-pinned-snapshot-20261030/src/test/java/com/synexia/m3/classspace/M3ClassSpacePinnedSnapshotScopedTest.java blob 81e0a128693c; recipe m3/intern/recipe/class-space-20261107/relocate.py v1
public final class M3ClassSpacePinnedSnapshotScopedTest {
    private static long assertions;
    private M3ClassSpacePinnedSnapshotScopedTest() { }

    public static void main(String[] args) throws Exception {
        M3ClassSpace owner = new M3ClassSpace(4096);
        owner.intern(new byte[] {0, 1, 2});
        owner.intern(new byte[] {3, 4, 5});
        byte[] original = M3ClassSpaceArchive.freeze(owner);
        ByteBuffer writable = ByteBuffer.allocateDirect(original.length).order(ByteOrder.BIG_ENDIAN);
        writable.put(original).flip();
        M3ClassSpaceArchive.PinnedView pin = M3ClassSpaceArchive.openPinnedCanonicalTrustedMapped(
                writable.asReadOnlyBuffer());
        M3ClassSpaceArchive.View copy = pin.snapshotCanonicalCopy();
        check(copy.count() == 2, "count");
        check(copy.readOnlyImage().isReadOnly(), "immutable snapshot view");
        check(Arrays.equals(copy.copyAt(0), owner.copy(0)), "exact owned bytes");
        writable.put(24 + 2 * 40, (byte) 0x6a);
        rejects(pin::snapshotCanonicalCopy, "modified source refused");
        check(Arrays.equals(copy.copyAt(0), owner.copy(0)), "snapshot survives mapping change");
        check(Arrays.equals(copy.restore().copy(1), owner.copy(1)), "restorable snapshot");

        Path file = Files.createTempFile("m3-pinned-snapshot-scoped", ".bin");
        try {
            Files.write(file, original);
            try (FileChannel reader = FileChannel.open(file, StandardOpenOption.READ);
                    FileChannel writer = FileChannel.open(file, StandardOpenOption.WRITE)) {
                ByteBuffer mapped = reader.map(FileChannel.MapMode.READ_ONLY, 0, original.length);
                M3ClassSpaceArchive.PinnedView fromFile =
                        M3ClassSpaceArchive.openPinnedCanonicalTrustedMapped(mapped);
                check(Arrays.equals(fromFile.snapshotCanonicalCopy().copyAt(1), owner.copy(1)),
                        "mapped original");
                byte[] changed = original.clone();
                changed[24 + 2 * 40] ^= 1;
                writer.write(ByteBuffer.wrap(changed));
                writer.force(true);
                rejects(fromFile::snapshotCanonicalCopy, "external descriptor mutation refused");
            }
        } finally {
            Files.deleteIfExists(file);
        }
        System.out.println("M3_SNAPSHOT_SCOPED_PASS assertions=" + assertions);
    }

    private static void check(boolean valid, String label) {
        assertions++;
        if (!valid) throw new AssertionError(label);
    }

    private static void rejects(Runnable action, String label) {
        assertions++;
        try {
            action.run();
        } catch (IllegalStateException accepted) {
            return;
        }
        throw new AssertionError(label);
    }
}
