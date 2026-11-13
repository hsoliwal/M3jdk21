/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Explicitly loaded JNI implementation of the CSR bitset breadth-first closure. */
public final class M3JniReachabilityKernel implements M3ReachabilityKernel {
    private static final Object LOAD_LOCK = new Object();
    private static Path loadedLibrary;
    private final Path library;
    private final String librarySha256;

    /** Loads one exact native library path for the current class loader. */
    public M3JniReachabilityKernel(Path library) {
        if (library == null) {
            throw new IllegalArgumentException("library must not be null");
        }
        this.library = library.toAbsolutePath().normalize();
        if (!Files.isRegularFile(this.library)) {
            throw new IllegalArgumentException("Missing JNI reachability library: " + this.library);
        }
        try {
            this.librarySha256 = M3Hashing.sha256(this.library);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Cannot hash JNI reachability library: " + this.library, exception);
        }
        load(this.library);
    }

    @Override
    public String id() {
        return "jni-csr-bitset-bfs-v1:" + librarySha256;
    }

    /** Returns the exact loaded library path. */
    public Path library() {
        return library;
    }

    @Override
    public int[] close(M3ReachabilityGraph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("graph must not be null");
        }
        int[] closure = close0(graph.vertexCount(), graph.offsetsView(), graph.edgesView(), graph.rootsView());
        return graph.validateClosure(closure);
    }

    static int[] closeRawForTesting(int vertexCount, int[] offsets, int[] edges, int[] roots) {
        return close0(vertexCount, offsets, edges, roots);
    }

    private static void load(Path library) {
        synchronized (LOAD_LOCK) {
            if (loadedLibrary == null) {
                System.load(library.toString());
                loadedLibrary = library;
            } else if (!loadedLibrary.equals(library)) {
                throw new IllegalStateException("A different JNI reachability library is already loaded: "
                        + loadedLibrary);
            }
        }
    }

    private static native int[] close0(int vertexCount, int[] offsets, int[] edges, int[] roots);
}
