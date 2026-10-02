/* Copyright 2026 Hitesh Soliwal. SPDX-License-Identifier: Apache-2.0 */
package com.m3.text;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

/** Tests only explicit stock-JVM JNI projection boundaries, never VM segmented support. */
public final class ProjectionProbe {
    private ProjectionProbe() { }
    private static native String roundTrip(String value, int mode);
    private static native byte[] modifiedUtf8(String value);
    private static native String region(String value, int start, int length);
    private static byte[] oracle(String value) {
        byte[] bytes = new byte[Math.multiplyExact(value.length(), 3)];
        int at = 0;
        for (int i = 0; i < value.length(); i++) {
            int unit = value.charAt(i);
            if (unit > 0 && unit < 128) bytes[at++] = (byte) unit;
            else if (unit < 2048) {
                bytes[at++] = (byte) (0xc0 | (unit >>> 6));
                bytes[at++] = (byte) (0x80 | (unit & 63));
            } else {
                bytes[at++] = (byte) (0xe0 | (unit >>> 12));
                bytes[at++] = (byte) (0x80 | ((unit >>> 6) & 63));
                bytes[at++] = (byte) (0x80 | (unit & 63));
            }
        }
        return Arrays.copyOf(bytes, at);
    }
    private static long verify(String expected) {
        M3Text view = M3Text.empty();
        for (int i = 0; i < expected.length(); i += 7) {
            view = view.concat(M3Text.fromString(expected.substring(i, Math.min(i + 7, expected.length()))));
        }
        // The ordinary String allocation is intentional and observable at this API boundary.
        String projected = view.asString();
        long checks = 0;
        for (int mode = 0; mode < 3; mode++) {
            if (!expected.equals(roundTrip(projected, mode))) throw new AssertionError("JNI round trip " + mode);
            checks++;
        }
        if (!Arrays.equals(modifiedUtf8(projected), oracle(expected))) throw new AssertionError("modified UTF-8");
        checks++;
        int start = expected.length() / 2;
        if (!expected.substring(start).equals(region(projected, start, expected.length() - start))) {
            throw new AssertionError("JNI code-unit region");
        }
        return checks + 1;
    }
    public static void main(String[] args) {
        if (args.length != 1) throw new IllegalArgumentException("absolute shared library path required");
        System.load(Path.of(args[0]).toAbsolutePath().toString());
        long checks = verify("") + verify("A\0\ud83d\ude00\ud800x\udc00");
        char[] all = new char[65536];
        for (int i = 0; i < all.length; i++) all[i] = (char) i;
        checks += verify(new String(all));
        Random random = new Random(0x4d334a4e494cL);
        for (int trial = 0; trial < 1200; trial++) {
            char[] units = new char[random.nextInt(96)];
            for (int i = 0; i < units.length; i++) units[i] = (char) random.nextInt(65536);
            checks += verify(new String(units));
        }
        try { region("x", -1, 1); throw new AssertionError("negative JNI region accepted"); }
        catch (StringIndexOutOfBoundsException expected) { checks++; }
        try { roundTrip(null, 0); throw new AssertionError("null JNI input accepted"); }
        catch (NullPointerException expected) { checks++; }
        System.out.println("M3_JNI_PROJECTION_PASS checks=" + checks + " modes=chars,critical,modified-utf8");
    }
}
