/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 */
import java.io.IOException;
import java.nio.ByteBuffer;

/** Actual Linux mmap and production JNI entry points in a checked JVM.
 * Test-only RegisterNatives binds the production symbols to this fixture. */
public final class M3StringPoolNativeTest {
    private static final int LENGTH = 65536;
    private static int checks;

    static {
        System.load(System.getProperty("m3.test.library"));
    }
    private static native ByteBuffer mapLexicon0(int fd, long handle, long address, long length) throws IOException;
    private static native boolean unmapLexicon0(long address, long length);
    private static native int openFixture0(int length) throws IOException;
    private static native void closeFixture0(int fd) throws IOException;
    private static native long reserveSentinel0(int length) throws IOException;
    private static native boolean sentinelIntact0(long address, int length);
    private static native void releaseSentinel0(long address, int length) throws IOException;

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private interface IOAction { void run() throws IOException; }
    private static void expectIOException(String expected, IOAction action) throws IOException {
        try {
            action.run();
            throw new AssertionError("Expected IOException: " + expected);
        } catch (IOException failure) {
            require(expected.equals(failure.getMessage()), "Wrong message: " + failure);
        }
        checks++;
    }

    public static void main(String[] args) throws Exception {
        int fd = openFixture0(LENGTH);
        long sentinel = 0;
        long mapping = 0;
        try {
            sentinel = reserveSentinel0(LENGTH);
            require(sentinel > 0 && (sentinel & 65535L) == 0, "Sentinel address alignment");
            final long occupied = sentinel;
            try {
                ByteBuffer collision = mapLexicon0(fd, 0, occupied, LENGTH);
                if (collision != null) {
                    throw new AssertionError("Mapping replaced occupied sentinel");
                }
                throw new AssertionError("Collision returned null without IOException");
            } catch (IOException expected) {
                require(expected.getMessage().equals("Fixed M3 lexicon address unavailable")
                        || expected.getMessage().equals("Kernel did not honor the fixed M3 lexicon address"),
                        "Unexpected collision error: " + expected);
            }
            require(sentinelIntact0(occupied, LENGTH), "Collision changed existing mapping");
            checks++;

            expectIOException("Invalid fixed M3 lexicon address or size",
                    () -> mapLexicon0(fd, 0, occupied + 1, LENGTH));
            expectIOException("Invalid fixed M3 lexicon address or size",
                    () -> mapLexicon0(fd, 0, occupied, 0));
            expectIOException("Invalid fixed M3 lexicon address or size",
                    () -> mapLexicon0(fd, 0, occupied, (long)Integer.MAX_VALUE + 1));
            expectIOException("Invalid or changed fixed M3 lexicon descriptor",
                    () -> mapLexicon0(-1, 0, occupied, LENGTH));
            expectIOException("Invalid or changed fixed M3 lexicon descriptor",
                    () -> mapLexicon0(fd, 0, occupied, LENGTH - 1));
            require(sentinelIntact0(occupied, LENGTH), "Validation changed existing mapping");

            releaseSentinel0(sentinel, LENGTH);
            sentinel = 0;
            ByteBuffer buffer = mapLexicon0(fd, 0, occupied, LENGTH);
            mapping = occupied;
            require(buffer != null && buffer.isDirect() && buffer.capacity() == LENGTH,
                    "Exact mapping did not return expected direct buffer");
            for (int i = 0; i < LENGTH; i++) {
                require((buffer.get(i) & 255) == ((i * 17 + 3) & 255),
                        "Mapped file content differs at " + i);
            }
            checks++;
            require(unmapLexicon0(mapping, LENGTH), "Unmap failed");
            mapping = 0;
            require(!unmapLexicon0(0, LENGTH), "Invalid unmap succeeded");
            checks++;
            System.out.println("JVM_LINUX_CHECKS=" + checks);
        } finally {
            if (mapping != 0) require(unmapLexicon0(mapping, LENGTH), "Mapping cleanup failed");
            if (sentinel != 0) releaseSentinel0(sentinel, LENGTH);
            closeFixture0(fd);
        }
    }
}
