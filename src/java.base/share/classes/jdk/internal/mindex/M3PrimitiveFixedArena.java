/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package jdk.internal.mindex;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Optional Linux fixed-VA projection of an immutable compile-time primitive image.
 *
 * <p>The canonical identity is the (image namespace, slot), never the VA. On
 * Windows/other OSes or when the requested VA is occupied, mapping is absent;
 * callers must use {@link M3PrimitiveImage#open(Path)} and stable offsets.
 * Existing VM memory is never overwritten to obtain a matching address.</p>
 *
 * <p>Native code is in the target libjava on Linux. Test-only stock JVMs may
 * explicitly load a matching native fixture with {@link #loadForTest(Path)}.</p>
 */
public final class M3PrimitiveFixedArena {
    private M3PrimitiveFixedArena() {}

    /** Only for isolated native-fixture testing, not necessary for a built M3JDK. */
    public static synchronized void loadForTest(Path library) {
        System.load(Objects.requireNonNull(library, "library").toAbsolutePath().toString());
    }

    /**
     * Returns null if the exact address cannot be reserved (or this JDK lacks
     * the optional Linux native symbol). Throws for invalid images and IO.
     * The requested address must be page-aligned and nonzero.
     */
    public static Mapping open(Path path, long preferredAddress) throws IOException {
        Objects.requireNonNull(path, "path");
        if (preferredAddress <= 0L) throw new IllegalArgumentException("preferred address");
        M3PrimitiveImage image = M3PrimitiveImage.open(path);
        long address;
        try {
            address = mapExact0(path.toAbsolutePath().toString(), preferredAddress,
                    image.byteLength());
        } catch (UnsatisfiedLinkError nativeUnavailable) {
            return null;
        }
        return address == 0L ? null : new Mapping(image, address, image.byteLength());
    }

    private static native long mapExact0(String path, long preferredAddress, long bytes)
            throws IOException;
    private static native boolean unmap0(long address, long bytes);

    /**
     * A pinned mapping. Do not retain returned raw addresses past close().
     * Synchronized address lookups guard against a concurrent close, but a
     * caller performing unsafe dereferences must retain a live mapping lease.
     */
    public static final class Mapping implements AutoCloseable {
        private final M3PrimitiveImage image;
        private final long base;
        private final long byteLength;
        private boolean closed;

        private Mapping(M3PrimitiveImage image, long base, long byteLength) {
            this.image = image;
            this.base = base;
            this.byteLength = byteLength;
        }

        public synchronized long baseAddress() {
            ensureOpen();
            return base;
        }

        public synchronized long addressOf(int slot) {
            ensureOpen();
            return Math.addExact(base, image.offset(slot));
        }

        public synchronized byte[] namespace() {
            ensureOpen();
            return image.namespace();
        }

        public synchronized int size() {
            ensureOpen();
            return image.size();
        }

        private void ensureOpen() {
            if (closed) throw new IllegalStateException("M3 fixed mapping already closed");
        }

        @Override public synchronized void close() throws IOException {
            if (!closed) {
                if (!unmap0(base, byteLength)) throw new IOException("munmap failed");
                closed = true;
            }
        }
    }
}
