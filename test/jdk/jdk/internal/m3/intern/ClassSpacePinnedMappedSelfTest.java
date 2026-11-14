// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.intern;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Random;

/** Strict, standalone Java 21 pinned-mapping safety oracle. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/d5ad5090d87ef8477a56317e60ac0d29c65029f7/synexia-openrewrite-recipes/recipe-crates/m3-classspace-pinned-mapped-20261030/src/test/java/com/synexia/m3/classspace/M3ClassSpacePinnedMappedSelfTest.java blob 70f37ddf7728; recipe make/m3/intern/recipe/class-space-20261107/relocate.py v1
public final class ClassSpacePinnedMappedSelfTest {
    private static long assertions;
    private ClassSpacePinnedMappedSelfTest() { }

    public static void main(String[] args) throws Exception {
        emptyAndInvalid();
        originalAndPinnedViews();
        changedPayload();
        changedIndexAndDigest();
        externalFileModification();
        seededIdReplay();
        System.out.println("M3_PINNED_MAPPED_PASS assertions=" + assertions);
    }

    private static void check(boolean condition, String what) {
        assertions++;
        if (!condition) throw new AssertionError(what);
    }

    private static void rejects(Class<? extends Throwable> expected, Runnable action) {
        assertions++;
        try { action.run(); }
        catch (Throwable ex) {
            if (expected.isInstance(ex)) return;
            throw new AssertionError("Expected " + expected.getName() + ", got " + ex, ex);
        }
        throw new AssertionError("Missing " + expected.getName());
    }

    private static byte[] frame(int count) {
        ClassSpace owner = new ClassSpace(700_000);
        for (int id = 0; id < count; id++) {
            owner.intern(new byte[] {(byte) id, (byte) (id >>> 8), (byte) 255, 0});
        }
        return ClassSpaceArchive.freeze(owner);
    }

    private static ByteBuffer direct(byte[] source) {
        ByteBuffer bytes = ByteBuffer.allocateDirect(source.length).order(ByteOrder.BIG_ENDIAN);
        bytes.put(source);
        bytes.flip();
        return bytes;
    }

    private static void emptyAndInvalid() {
        ByteBuffer value = direct(frame(0));
        ClassSpaceArchive.PinnedView pinned =
                ClassSpaceArchive.openPinnedCanonicalTrustedMapped(value.asReadOnlyBuffer());
        check(pinned.count() == 0, "empty count");
        pinned.verifyRoot();
        rejects(IndexOutOfBoundsException.class, () -> pinned.copyVerifiedAt(0));
        rejects(IllegalArgumentException.class,
                () -> ClassSpaceArchive.openPinnedCanonicalTrustedMapped(value));
        rejects(IllegalArgumentException.class,
                () -> ClassSpaceArchive.openPinnedCanonicalTrustedMapped(
                        ByteBuffer.wrap(frame(0)).asReadOnlyBuffer()));
    }

    private static void originalAndPinnedViews() {
        byte[] serialized = frame(8);
        ClassSpaceArchive.View old = ClassSpaceArchive.openCanonicalCopy(serialized);
        ByteBuffer buffer = direct(serialized);
        ClassSpaceArchive.PinnedView pinned =
                ClassSpaceArchive.openPinnedCanonicalTrustedMapped(buffer.asReadOnlyBuffer());
        check(pinned.count() == old.count(), "identity count");
        for (int i = 0; i < old.count(); i++) {
            byte[] output = pinned.copyVerifiedAt(i);
            check(Arrays.equals(output, old.copyAt(i)), "exact bytes");
            output[0] ^= 21;
            check(Arrays.equals(pinned.copyVerifiedAt(i), old.copyAt(i)), "defensive copy");
        }
        pinned.verifyRoot();
        rejects(IndexOutOfBoundsException.class, () -> pinned.copyVerifiedAt(-1));
        rejects(IndexOutOfBoundsException.class, () -> pinned.copyVerifiedAt(8));
    }

    private static void changedPayload() {
        ByteBuffer mutable = direct(frame(3));
        ClassSpaceArchive.PinnedView pinned =
                ClassSpaceArchive.openPinnedCanonicalTrustedMapped(mutable.asReadOnlyBuffer());
        int payloadBase = 24 + 3 * 40;
        mutable.put(payloadBase, (byte) (mutable.get(payloadBase) ^ 1));
        rejects(IllegalStateException.class, () -> pinned.copyVerifiedAt(0));
        rejects(IllegalStateException.class, pinned::verifyRoot);
        check(Arrays.equals(pinned.copyVerifiedAt(2),
                new byte[] {2, 0, (byte) 255, 0}), "unchanged entry");
    }

    private static void changedIndexAndDigest() {
        ByteBuffer mutable = direct(frame(2));
        ClassSpaceArchive.PinnedView pinned =
                ClassSpaceArchive.openPinnedCanonicalTrustedMapped(mutable.asReadOnlyBuffer());
        byte[] original = pinned.copyVerifiedAt(0);
        mutable.putInt(24, 4);
        check(Arrays.equals(original, pinned.copyVerifiedAt(0)), "pinned offset");
        rejects(IllegalStateException.class, pinned::verifyRoot);
        ByteBuffer checksums = direct(frame(1));
        ClassSpaceArchive.PinnedView stable =
                ClassSpaceArchive.openPinnedCanonicalTrustedMapped(checksums.asReadOnlyBuffer());
        checksums.put(24 + 8, (byte) (checksums.get(24 + 8) ^ 1));
        check(Arrays.equals(stable.copyVerifiedAt(0), new byte[] {0, 0, (byte) 255, 0}),
                "pinned digest");
        rejects(IllegalStateException.class, stable::verifyRoot);
    }

    private static void externalFileModification() throws Exception {
        byte[] archive = frame(2);
        Path file = Files.createTempFile("m3classspace-pinned", ".bin");
        try {
            Files.write(file, archive);
            try (FileChannel reader = FileChannel.open(file, StandardOpenOption.READ);
                    FileChannel writer = FileChannel.open(file, StandardOpenOption.WRITE)) {
                ByteBuffer mapping = reader.map(FileChannel.MapMode.READ_ONLY, 0, archive.length);
                ClassSpaceArchive.PinnedView pinned =
                        ClassSpaceArchive.openPinnedCanonicalTrustedMapped(mapping);
                check(pinned.count() == 2, "mmap count");
                byte[] update = archive.clone();
                update[24 + 2 * 40] ^= 1;
                ByteBuffer write = ByteBuffer.wrap(update);
                while (write.hasRemaining()) writer.write(write);
                writer.force(true);
                rejects(IllegalStateException.class, () -> pinned.copyVerifiedAt(0));
                rejects(IllegalStateException.class, pinned::verifyRoot);
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static void seededIdReplay() {
        Random random = new Random(0x2A5CE1L);
        for (int seed = 0; seed < 4; seed++) {
            ClassSpace owner = new ClassSpace(100_000);
            for (int j = 0; j < 1000; j++) {
                byte[] image = new byte[1 + random.nextInt(40)];
                random.nextBytes(image);
                owner.intern(image);
            }
            ClassSpaceArchive.PinnedView pinned =
                    ClassSpaceArchive.openPinnedCanonicalTrustedMapped(
                            direct(ClassSpaceArchive.freeze(owner)).asReadOnlyBuffer());
            check(pinned.count() == owner.imageCount(), "random count");
            pinned.verifyRoot();
            for (int id = 0; id < owner.imageCount(); id++) {
                check(Arrays.equals(pinned.copyVerifiedAt(id), owner.copy(id)), "ordinal " + id);
            }
        }
    }
}
