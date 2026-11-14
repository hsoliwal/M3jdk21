// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.intern;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Random;

/** Exact byte/offset/budget/alias and adversarial collision tests of the copy-to atom. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/d5ad5090d87ef8477a56317e60ac0d29c65029f7/synexia-openrewrite-recipes/recipe-crates/m3-classspace-archive-copyto-20261030/src/test/java/com/synexia/m3/classspace/M3ArchiveCopyToSelfTest.java blob 907a28e766a2; recipe m3/intern/recipe/class-space-20261107/relocate.py v1
public final class ArchiveCopyToSelfTest {
    private static long checks;

    private ArchiveCopyToSelfTest() { }

    private static void check(boolean condition, String context) {
        checks++;
        if (!condition) throw new AssertionError(context);
    }

    private static void reject(Class<? extends Throwable> expected, Runnable task) {
        checks++;
        try {
            task.run();
        } catch (Throwable ex) {
            if (expected.isInstance(ex)) return;
            throw new AssertionError("Wrong exception: " + ex, ex);
        }
        throw new AssertionError("Failed to reject " + expected.getSimpleName());
    }

    private static void boundsAndAliasing() {
        ClassSpace owner = new ClassSpace(1024);
        byte[] raw = new byte[] {0, -1, 1, 2, 3, -128};
        int atom = owner.intern(raw);
        raw[0] = 95;
        byte[] dest = new byte[21];
        Arrays.fill(dest, (byte) 0x77);
        check(owner.copyInto(atom, dest, 5, 6) == 6, "valid copy length");
        check(Arrays.equals(Arrays.copyOfRange(dest, 5, 11),
                new byte[] {0, -1, 1, 2, 3, -128}), "exact byte span");
        byte[] prev = dest.clone();
        reject(IllegalStateException.class, () -> owner.copyInto(atom, dest, 5, 5));
        check(Arrays.equals(prev, dest), "no copy after insufficient payload");
        reject(IllegalStateException.class, () -> owner.copyInto(atom, dest, 5, -1));
        check(Arrays.equals(prev, dest), "no copy after negative remaining");
        reject(IndexOutOfBoundsException.class, () -> owner.copyInto(atom, dest, 20, 8));
        reject(IndexOutOfBoundsException.class, () -> owner.copyInto(atom, dest, -1, 8));
        reject(IndexOutOfBoundsException.class, () -> owner.copyInto(-1, dest, 0, 8));
        reject(IndexOutOfBoundsException.class, () -> owner.copyInto(9, dest, 0, 8));
        reject(NullPointerException.class, () -> owner.copyInto(atom, null, 0, 8));
        check(Arrays.equals(dest, prev), "no invalid mutation");
        dest[5] = 12;
        check(owner.copy(atom)[0] == 0, "caller owns destination, no alias");
        int empty = owner.intern(new byte[0]);
        check(owner.copyInto(empty, dest, dest.length, 0) == 0, "empty range at end");
        check(owner.copyInto(empty, dest, 0, 0) == 0, "empty range start");
    }

    private static void randomStableArchive() {
        ClassSpace owner = new ClassSpace(4 * 1024 * 1024);
        Random random = new Random(0x812749acL);
        for (int i = 0; i < 3000; i++) {
            byte[] v = new byte[i % 53 == 0 ? 0 : random.nextInt(1024)];
            random.nextBytes(v);
            int id = owner.intern(v);
            byte[] to = new byte[v.length + 13];
            int bytes = owner.copyInto(id, to, 7, v.length);
            check(bytes == v.length && Arrays.equals(v, Arrays.copyOfRange(to, 7, 7 + bytes)),
                    "copy/intern parity " + i);
        }
        byte[] frame = ClassSpaceArchive.freeze(owner);
        ClassSpaceArchive.View view = ClassSpaceArchive.openCanonicalCopy(frame);
        check(view.count() == owner.imageCount(), "frozen count");
        check(Arrays.equals(frame, ClassSpaceArchive.freeze(view.restore())), "frozen replay");
        for (int i = 0; i < owner.imageCount(); i++) {
            check(Arrays.equals(view.copyAt(i), owner.copy(i)), "stored atom " + i);
        }
    }

    private static void collisionAndNativeCompatibility() {
        byte[] constantDigest = new byte[32];
        Arrays.fill(constantDigest, (byte) 17);
        ClassSpace collisions = new ClassSpace(2048, bytes -> constantDigest.clone());
        for (int i = 0; i < 240; i++) {
            byte[] v = {(byte) i, (byte) (i >>> 8)};
            check(collisions.intern(v) == i, "forced collision ordinal");
            byte[] copied = new byte[2];
            check(collisions.copyInto(i, copied, 0, 2) == 2
                    && Arrays.equals(copied, v), "forced collision bytes");
        }
        byte[] encoded = ClassSpaceArchive.freeze(collisions);
        ClassSpaceArchive.View view = ClassSpaceArchive.openCanonicalCopy(encoded);
        check(view.count() == 240, "collision archive count");
        check(view.readOnlyImage().isReadOnly(), "read-only archive");
        // The archive wire format contains per-entry real SHA-256, not a
        // caller-injected candidate hash used for the owner's lookup index.
        byte[] original = collisions.copy(0);
        byte[] actual = new byte[32];
        try {
            actual = MessageDigest.getInstance("SHA-256").digest(original);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new AssertionError(ex);
        }
        byte[] encodedDigest = new byte[32];
        ByteBuffer.wrap(encoded).position(32).get(encodedDigest);
        check(Arrays.equals(actual, encodedDigest), "archive canonical SHA-256");
    }

    public static void main(String[] args) {
        boundsAndAliasing();
        randomStableArchive();
        collisionAndNativeCompatibility();
        System.out.println("M3_ARCHIVE_COPYTO_PASS checks=" + checks);
    }
}
