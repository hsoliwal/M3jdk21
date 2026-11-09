/*
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: GPL-2.0-only
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 */

/*
 * @test
 * @summary Opt-in Object.toAdapter(SHA256Adapter.class) digest and compatibility contract
 * @run main/othervm/native -Xcheck:jni SHA256ObjectAdapterTest
 * @run main/othervm/native -Xcheck:jni -Xint SHA256ObjectAdapterTest
 */

import java.lang.M3Object;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SHA256Adapter;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

public class SHA256ObjectAdapterTest {
    private static native byte[] digestFromNative(Object source);
    private static int checks;

    private static void check(boolean valid, String message) {
        if (!valid) {
            throw new AssertionError(message);
        }
        checks++;
    }

    private static byte[] reference(byte[] source) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(source);
    }

    private static void verify(Object source, byte[] expected) {
        SHA256Adapter adapter = Objects.requireNonNull(
                source.toAdapter(SHA256Adapter.class), "adapter");
        check(Arrays.equals(adapter.digest(), expected), "sha256 bytes");
        check(Arrays.equals(digestFromNative(source), expected), "JNI adapter dispatch");
        check(adapter.hexDigest().equals(HexFormat.of().formatHex(expected)), "sha256 hex");
        check(adapter.matches(expected), "matching sha256");
        check(!adapter.matches(new byte[0]), "wrong length");
        byte[] result = adapter.digest();
        result[0] ^= 1;
        check(Arrays.equals(adapter.digest(), expected), "detached digest bytes");
    }

    public static void main(String[] args) throws Exception {
        System.loadLibrary("SHA256ObjectAdapterTest");
        Object bare = new Object();
        check(bare.toAdapter(Object.class) == bare, "assignable receiver");
        check(bare.toAdapter(SHA256Adapter.class) == null, "no arbitrary encoding");
        check(digestFromNative(bare) == null, "JNI no arbitrary encoding");
        check(bare.toAdapter(String.class) == null, "mismatch remains null");
        check(bare.toAdapter(M3Object.class).source() == bare, "existing M3Object path");
        try {
            bare.toAdapter(null);
            throw new AssertionError("null class should be rejected");
        } catch (NullPointerException expected) {
            checks++;
        }
        var hello = "abc";
        check(hello.toAdapter(String.class) == hello, "String identity");
        verify(hello, reference(hello.getBytes(StandardCharsets.UTF_8)));
        check(hello.toAdapter(SHA256Adapter.class).hexDigest().equals(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"),
                "known SHA-256 abc vector");
        verify("", reference(new byte[0]));
        verify("\uD800", reference("\uD800".getBytes(StandardCharsets.UTF_8)));

        int[] sizes = {0, 1, 31, 55, 56, 57, 63, 64, 65, 4095, 4096, 4097, 65536};
        for (int size : sizes) {
            byte[] bytes = new byte[size];
            for (int i = 0; i < size; i++) {
                bytes[i] = (byte) (i * 19 + size);
            }
            byte[] expected = reference(bytes);
            verify(bytes, expected);
            ByteBuffer direct = ByteBuffer.allocateDirect(size + 2);
            direct.put((byte) 99).put(bytes).put((byte) 88).flip();
            direct.position(1);
            direct.limit(size + 1);
            verify(direct, expected);
            check(direct.position() == 1 && direct.limit() == size + 1,
                    "caller buffer cursor unchanged");
            verify(direct.asReadOnlyBuffer(), expected);
            SHA256Adapter selectedRange = direct.toAdapter(SHA256Adapter.class);
            direct.clear();
            check(Arrays.equals(selectedRange.digest(), expected), "fixed byte range");
            SHA256Adapter live = bytes.toAdapter(SHA256Adapter.class);
            if (size != 0) {
                bytes[0] ^= 1;
                check(Arrays.equals(live.digest(), reference(bytes)),
                        "mutable source rehashed");
            }
        }

        SHA256Adapter custom = () -> new byte[SHA256Adapter.DIGEST_BYTES];
        check(custom.toAdapter(SHA256Adapter.class) == custom,
                "self-adapt supplied capability");
        check(SHA256Adapter.of(new Object()) == null, "factory refusal");
        System.out.println("SHA256_OBJECT_ADAPTER_PASS checks=" + checks);
    }
}
