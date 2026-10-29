// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

//FLLDCIM: IDL - atom wc.native_bridge: libm3textio loader and long[] state ABI v1
/**
 * JNI bridge to the libm3textio wc kernel. The caller owns a {@code long[]} state
 * (ABI v1, slots below); the kernel keeps no handle, address or heap between calls.
 * Loading is explicit: a missing library surfaces as {@link UnsatisfiedLinkError}.
 */
final class WcNative {
    static final int LINES = 0;
    static final int WORDS = 1;
    static final int CHARS = 2;
    static final int FLAGS = 3;
    static final int MAGIC = 4;
    static final int SLOTS = 5;
    static final long MAGIC_V1 = 0x4D33574300000001L;
    static final long FINISHED = 0x20L;
    /** Optional absolute library path, checked before {@code java.home/lib}. */
    static final String PATH_PROPERTY = "jdk.m3.text.native.path";

    private static boolean loaded;

    private WcNative() { }

    static synchronized void ensureLoaded() {
        if (loaded) return;
        String explicit = System.getProperty(PATH_PROPERTY);
        if (explicit != null && !explicit.isBlank()) {
            System.load(Path.of(explicit).toAbsolutePath().normalize().toString());
        } else {
            Path bundled = Path.of(System.getProperty("java.home"), "lib", System.mapLibraryName("m3textio"));
            if (Files.isRegularFile(bundled)) System.load(bundled.toAbsolutePath().toString());
            else System.loadLibrary("m3textio");
        }
        loaded = true;
    }

    static long[] newState() {
        long[] state = new long[SLOTS];
        state[MAGIC] = MAGIC_V1;
        return state;
    }

    /** Feeds {@code units} UTF-16 (width 2) or Latin-1 (width 1) units at {@code byteOffset}. */
    static native void count0(ByteBuffer window, int byteOffset, int units, int width,
            boolean bigEndian, boolean last, long[] state);

    /** Feeds SoA line lanes: content and ending lengths over one UTF-16 buffer. */
    static native void countLines0(ByteBuffer utf16, int byteOffset, boolean bigEndian,
            int[] contentLengths, int[] endingLengths, int fromLine, int toLine,
            boolean last, long[] state);
}
