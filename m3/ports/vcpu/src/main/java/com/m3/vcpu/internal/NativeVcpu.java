/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.vcpu.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * JNI binding of the m3vcpu native HAL ({@code m3vcpu_jni} over {@code m3vcpu}). Six entry points, {@code long}
 * handles, no policy. A session handle owns its buffer handles; closing a session releases their device storage.
 * Every handle may be closed more than once (later closes are no-ops), so a {@code Cleaner} can own it.
 *
 * <p>Native status codes map to {@link IllegalArgumentException} (invalid argument),
 * {@link IndexOutOfBoundsException} (range / geometry), {@link OutOfMemoryError},
 * {@link UnsupportedOperationException} (backend, device or kernel format unavailable) and
 * {@link IllegalStateException} (stale handle, driver, build or launch failure).</p>
 *
 * <p>Loader order: system property {@value #PATH_PROPERTY} (absolute library path), then the file named by
 * {@value #PATH_FILE_PROPERTY} (whose content is that path), then {@code System.loadLibrary("m3vcpu_jni")}. The
 * {@code m3vcpu} core next to the JNI library is loaded first so its dependency resolves on every platform.</p>
 */
public final class NativeVcpu {
    /** System property naming the absolute path of the {@code m3vcpu_jni} library. */
    public static final String PATH_PROPERTY = "m3.vcpu.native.path";
    /** System property naming a file that contains the absolute path of the {@code m3vcpu_jni} library. */
    public static final String PATH_FILE_PROPERTY = "m3.vcpu.native.pathFile";
    /** Library name for {@link System#loadLibrary(String)}. */
    public static final String LIBRARY = "m3vcpu_jni";
    /** Core library name. */
    public static final String CORE_LIBRARY = "m3vcpu";
    /** Built-in RV32IM slice kernel (arguments: memory, registers, state, config int buffers). */
    public static final String RV32IM_SLICE = "m3k_rv32im_slice";

    private static volatile boolean loaded;

    private NativeVcpu() {
    }

    /** Loads the native libraries once; later calls return immediately. */
    public static void load() {
        if (!loaded) {
            synchronized (NativeVcpu.class) {
                if (!loaded) {
                    loadLibraries();
                    loaded = true;
                }
            }
        }
    }

    /**
     * Reports whether {@link #load()} succeeded.
     *
     * @return {@code true} once the libraries are loaded
     */
    public static boolean isLoaded() {
        return loaded;
    }

    private static void loadLibraries() {
        String explicit = System.getProperty(PATH_PROPERTY);
        if (explicit != null && !explicit.isBlank()) {
            loadWithCore(Path.of(explicit));
            return;
        }
        String pathFile = System.getProperty(PATH_FILE_PROPERTY);
        if (pathFile != null && !pathFile.isBlank()) {
            String path;
            try {
                path = Files.readString(Path.of(pathFile)).strip();
            } catch (IOException failure) {
                throw new IllegalStateException("cannot read m3vcpu native path file " + pathFile, failure);
            }
            if (path.isEmpty()) {
                throw new IllegalStateException("empty m3vcpu native path file " + pathFile);
            }
            loadWithCore(Path.of(path));
            return;
        }
        try {
            System.loadLibrary(CORE_LIBRARY);
        } catch (UnsatisfiedLinkError absent) {
            // the platform loader may still resolve the core as a dependency of the JNI library
        }
        System.loadLibrary(LIBRARY);
    }

    private static void loadWithCore(Path jni) {
        Path library = jni.toAbsolutePath().normalize();
        Path dir = library.getParent();
        if (dir != null) {
            Path core = dir.resolve(System.mapLibraryName(CORE_LIBRARY));
            if (Files.isRegularFile(core)) {
                System.load(core.toString());
            }
        }
        System.load(library.toString());
    }

    /**
     * Opens a session: backend + device + context.
     *
     * @param selector {@code null} or {@code "auto"} (M3VCPU_BACKEND, else best score), {@code "gpu"}, or an
     *                 ordered list {@code "name[:device],..."} such as {@code "opencl,cpu"}
     * @param device   device index, or {@code -1} for the backend's best device
     * @return session handle
     */
    public static native long open(String selector, int device);

    /**
     * Allocates a zero-filled device buffer owned by the session.
     *
     * @param session session handle
     * @param bytes   size in bytes, {@code > 0}
     * @return buffer handle
     */
    public static native long alloc(long session, long bytes);

    /**
     * Copies host data into a device buffer.
     *
     * @param buffer     buffer handle
     * @param byteOffset destination offset in bytes
     * @param src        {@code int[]} (offset and length in ints) or a direct {@code ByteBuffer} (in bytes)
     * @param srcOffset  source offset
     * @param length     element count
     */
    public static native void upload(long buffer, long byteOffset, Object src, int srcOffset, int length);

    /**
     * Runs a built-in kernel over {@code global} work-items and returns when every work-item finished.
     *
     * @param session session handle
     * @param kernel  kernel entry, for example {@link #RV32IM_SLICE}
     * @param global  work-items (rounded up to {@code local}; the kernel guards the tail)
     * @param local   work-group size, {@code 0} = backend choice
     * @param buffers buffer handles bound to the kernel arguments in order
     */
    public static native void launch(long session, String kernel, long global, int local, long[] buffers);

    /**
     * Copies a device buffer range into an {@code int[]}.
     *
     * @param buffer     buffer handle
     * @param byteOffset source offset in bytes
     * @param dst        destination array
     * @param dstOffset  destination offset in ints
     * @param length     int count
     */
    public static native void download(long buffer, long byteOffset, int[] dst, int dstOffset, int length);

    /**
     * Closes a session or buffer handle. Idempotent; {@code 0} and stale handles are ignored.
     *
     * @param handle session or buffer handle
     */
    public static native void close(long handle);
}
