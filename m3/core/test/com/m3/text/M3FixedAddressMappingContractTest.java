/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Spawned in separate JVMs by verify-fixed-address-jni.py. */
public final class M3FixedAddressMappingContractTest {
    private static long checks;
    private M3FixedAddressMappingContractTest() { }

    private static void check(boolean condition, String why) {
        checks++;
        if (!condition) throw new AssertionError(why + " [" + checks + "]");
    }

    private static void expectedFailure(Class<? extends Throwable> kind, Action action)
            throws Exception {
        try {
            action.run();
            throw new AssertionError("missing refusal: " + kind.getSimpleName());
        } catch (Throwable found) {
            check(kind.isInstance(found), "incorrect refusal: " + found);
        }
    }

    private interface Action { void run() throws Exception; }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("mode library image digest");
        String mode = args[0];
        Path library = Path.of(args[1]);
        Path file = Path.of(args[2]);
        String digest = args[3];
        long base = M3FixedAddressMapping.DEFAULT_BASE;
        long length = Files.size(file);
        if (mode.equals("collision")) {
            M3FixedAddressMapping.reserveCollisionForProof(base, length, library);
            expectedFailure(IOException.class,
                    () -> M3FixedAddressMapping.open(file, base, digest, library));
            System.out.println("M3_GVAS_COLLISION_REFUSED checks=" + checks);
            return;
        }
        if (mode.equals("digest")) {
            expectedFailure(IOException.class,
                    () -> M3FixedAddressMapping.open(file, base, "0".repeat(64), library));
            System.out.println("M3_GVAS_DIGEST_REFUSED checks=" + checks);
        }
        expectedFailure(IllegalArgumentException.class,
                () -> M3FixedAddressMapping.open(file, base + 1, digest, library));
        M3FixedAddressMapping.Image image = M3FixedAddressMapping.open(file, base, digest, library);
        check(image.address() == base, "exact numeric base");
        check(image.addressOfOffset(128) == base + 128, "offset is process-independent");
        check(image.byteLength() == length, "source byte length");
        check(image.sha256().equals(digest), "source digest");
        check(image == M3FixedAddressMapping.open(file, base, digest, library),
                "idempotent process owner");
        ByteBuffer view = image.readOnlyBuffer();
        check(view.isReadOnly() && view.isDirect(), "read-only direct view");
        check(view.get(128) == (byte)0x5a, "canonical payload byte");
        check(view.get(129) == (byte)0x36, "canonical payload byte 2");
        expectedFailure(ReadOnlyBufferException.class, () -> view.put(0, (byte)1));
        expectedFailure(IllegalStateException.class,
                () -> M3FixedAddressMapping.open(file, base, "1".repeat(64), library));
        if (mode.equals("child")) {
            System.out.printf("M3_GVAS_READY base=0x%x offset=0x%x checks=%d%n",
                    image.address(), image.addressOfOffset(128), checks);
            System.out.flush();
            if (System.in.read() == -1) throw new IOException("missing coordination release");
            check(image.readOnlyBuffer().get(128) == (byte)0x5a, "mapping survives handshake");
        }
        System.out.println("M3_GVAS_JAVA_PASS checks=" + checks);
    }
}
