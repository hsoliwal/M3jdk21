// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.intern;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Random;

/** Bounded, deterministic archive adversarial oracle. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/fd9a233680b43197eb8c5745a729c342ca56c4e2/synexia-common/src/test/java/com/synexia/common/intern/M3InternArchiveGuardProof.java blob 66d1bdb314b6; recipe m3/intern/recipe/intern-space-20261106/relocate.py v4
public final class InternArchiveGuardProof {
    private static int checks;
    private InternArchiveGuardProof() { }
    public static void main(String[] args) throws Exception {
        run();
        System.out.println("InternArchiveGuardProof PASS checks=" + checks);
    }
    public static void run() throws Exception {
        checks = 0;
        InternSpace space = new InternSpace();
        long[] ids = new long[400];
        Random random = new Random(945917);
        for (int i = 0; i < ids.length; i++) {
            byte[] bytes = new byte[1 + (i % 17) * 23];
            random.nextBytes(bytes);
            ids[i] = space.blobs().intern(bytes);
            eq(ids[i], space.blobs().intern(bytes));
        }
        long leaf = space.graphs().intern(100, ids[0]);
        long root = space.graphs().intern(200, ids[1], leaf);
        space.freeze();
        byte[] archive = InternArchiveGuard.write(space, 1 << 20);
        InternSpace parsed = InternArchiveGuard.read(archive, 1 << 20, 2000);
        eq(root, parsed.graphs().intern(200, ids[1], leaf));
        eq(true, Arrays.equals(archive, InternArchiveGuard.write(parsed, 1 << 20)));
        rejects(IOException.class, () -> InternArchiveGuard.read(archive, 1 << 20, 1));
        rejects(IOException.class, () -> InternArchiveGuard.write(space, 128));
        rejects(IOException.class, () -> InternArchiveGuard.read(archive, 128, 2000));
        rejects(IllegalArgumentException.class, () -> InternArchiveGuard.read(archive, 0, 1000));
        rejects(IllegalArgumentException.class, () -> InternArchiveGuard.read(archive, 1 << 20, -1));
        byte[] corruption = archive.clone();
        corruption[corruption.length - 1] ^= 7;
        rejects(IOException.class, () -> InternArchiveGuard.read(corruption, 1 << 20, 2000));
        byte[] wrongMagic = archive.clone();
        wrongMagic[0] = 0;
        rejects(IOException.class, () -> InternArchiveGuard.read(wrongMagic, 1 << 20, 2000));
        byte[] tooSmall = Arrays.copyOf(archive, archive.length - 3);
        rejects(IOException.class, () -> InternArchiveGuard.read(tooSmall, 1 << 20, 2000));
        // A checksum is not authorization. An attacker can recalculate it; the
        // scanner still must reject malformed inner sizes and invalid DAG edges.
        byte[] badCount = archive.clone();
        ByteBuffer.wrap(badCount).putInt(44 + 8, Integer.MAX_VALUE);
        resign(badCount);
        rejects(IOException.class, () -> InternArchiveGuard.read(badCount, 1 << 20, 2000));
        byte[] badLength = archive.clone();
        ByteBuffer.wrap(badLength).putInt(44 + 12, Integer.MAX_VALUE);
        resign(badLength);
        rejects(IOException.class, () -> InternArchiveGuard.read(badLength, 1 << 20, 2000));
        byte[] trailing = Arrays.copyOf(archive, archive.length + 1);
        rejects(IOException.class, () -> InternArchiveGuard.read(trailing, 1 << 20, 2000));
        // Crafted valid envelope with self-referential DAG, avoiding runtime graph insertion.
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(content);
        out.writeInt(0x4d334953); out.writeInt(1); // original inner magic/version
        out.writeInt(0); out.writeInt(0); out.writeInt(0); // two byte stores and tuples
        out.writeInt(1); // one graph node
        out.writeInt(7); out.writeInt(2); out.writeLong(3); out.writeLong(1); // self child
        byte[] invalid = framed(content.toByteArray());
        rejects(IOException.class, () -> InternArchiveGuard.read(invalid, 1 << 20, 2000));
    }
    private static byte[] framed(byte[] payload) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeInt(0x4d334947); out.writeInt(1); out.writeInt(payload.length);
        out.write(MessageDigest.getInstance("SHA-256").digest(payload));
        out.write(payload);
        return bytes.toByteArray();
    }
    private static void resign(byte[] framed) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                Arrays.copyOfRange(framed, 44, framed.length));
        System.arraycopy(digest, 0, framed, 12, digest.length);
    }
    private static void eq(Object expected, Object actual) { if (!expected.equals(actual)) throw new AssertionError(); checks++; }
    private interface Action { void run() throws Exception; }
    private static void rejects(Class<? extends Throwable> type, Action action) {
        try { action.run(); } catch (Throwable error) {
            if (type.isInstance(error)) { checks++; return; }
            throw new AssertionError("wrong exception " + error, error);
        }
        throw new AssertionError("missing rejection: " + type);
    }
}
