// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.optane;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Read-only native-mapped tensor storage and a bounded, scalar/AVX2 INT8 matvec.
 * Candidate for a future jdk.m3.accelerator.pmem product JMOD; this vendor-scoped
 * source is not a GPU, a full LLM runtime, or a proof of Optane residency.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/56cbdc6312594f06e21ef56f0ce33b605383270b/synexia-m3-jmods/accelerator-pmem/src/main/java/com/synexia/m3/accelerator/pmem/PmemAccelerator.java blob fd3669994196; recipe make/m3/optane/recipe/pmem-accelerator-20261107/relocate.py v4
public final class PmemAccelerator {
    static { System.loadLibrary("m3pmemaccel"); }

    private PmemAccelerator() { }

    public enum DaxState { CONFIRMED, ABSENT, UNKNOWN }

    public static Mapping open(Path path, boolean requireFsDax) throws IOException {
        Objects.requireNonNull(path, "path");
        byte[] utf8 = path.toAbsolutePath().toString().getBytes(StandardCharsets.UTF_8);
        long handle = nativeOpen(utf8);
        boolean transferred = false;
        try {
            int state = nativeDaxState(handle);
            DaxState dax = state == 1 ? DaxState.CONFIRMED
                    : state == 0 ? DaxState.ABSENT : DaxState.UNKNOWN;
            if (requireFsDax && dax != DaxState.CONFIRMED) {
                throw new IOException("FS-DAX not confirmed: " + dax);
            }
            long length = nativeLength(handle);
            if (length <= 0) throw new IOException("invalid mapped region length");
            Mapping region = new Mapping(handle, dax, length);
            transferred = true;
            return region;
        } finally {
            if (!transferred) nativeClose(handle);
        }
    }

    /** Bit 0 AVX2; bit 1 FMA; bit 2 AVX512F; bit 3 AVX512VNNI.
     * Reporting an ISA bit does not mean a kernel uses that ISA. */
    public static int cpuFeatureMask() { return nativeCpuFeatures(); }

    /** Pins only the calling OS thread, not the memory pages or whole JVM. */
    public static void pinCurrentThreadToNode(int node) throws IOException {
        if (node < 0 || node > 4096) throw new IllegalArgumentException("NUMA node");
        nativePinNode(node);
    }

    /** A single Java owner prevents use-after-munmap via synchronized operations. */
    public static final class Mapping implements AutoCloseable {
        private long handle;
        private final DaxState dax;
        private final long length;
        private Mapping(long handle, DaxState dax, long length) {
            this.handle = handle; this.dax = dax; this.length = length;
        }
        public DaxState daxState() { return dax; }
        private long requireOpen() {
            if (handle == 0L) throw new IllegalStateException("mapping closed");
            return handle;
        }
        public synchronized long sizeBytes() {
            requireOpen();
            return length;
        }
        /** Check long offsets before proportional Java allocation or JNI entry. */
        private void checkRange(long offset, long size) {
            if (offset < 0 || size < 0 || offset > length || size > length - offset) {
                throw new IndexOutOfBoundsException("mapped region exceeded");
            }
        }
        public synchronized byte[] read(long offset, int size) {
            if (size < 0) throw new IllegalArgumentException("negative size");
            long owned = requireOpen();
            checkRange(offset, size);
            byte[] result = new byte[size];
            nativeRead(owned, offset, result);
            return result;
        }
        /** Signed row-major Q8 with one per-tensor scale; NOT GGUF Q8_0. */
        public synchronized float[] matVecQ8(long offset, int rows, int cols,
                float scale, float[] input) {
            Objects.requireNonNull(input, "input");
            if (rows <= 0 || cols <= 0 || input.length != cols || !Float.isFinite(scale)) {
                throw new IllegalArgumentException("matrix dimensions or scale");
            }
            long owned = requireOpen();
            // Positive int dimensions multiply exactly within signed long capacity.
            checkRange(offset, (long) rows * cols);
            float[] output = new float[rows];
            nativeMatVec(owned, offset, rows, cols, scale, input, output);
            return output;
        }
        @Override public synchronized void close() throws IOException {
            long owned = handle;
            handle = 0; // invalidate even if munmap fails
            if (owned != 0) nativeClose(owned);
        }
    }

    private static native long nativeOpen(byte[] path) throws IOException;
    private static native void nativeClose(long handle) throws IOException;
    private static native long nativeLength(long handle);
    private static native int nativeDaxState(long handle);
    private static native void nativeRead(long handle, long offset, byte[] destination);
    private static native void nativeMatVec(long handle, long offset, int rows, int cols,
            float scale, float[] input, float[] output);
    private static native int nativeCpuFeatures();
    private static native void nativePinNode(int node) throws IOException;
}
