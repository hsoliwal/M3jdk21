// Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.filesystem;

import com.m3.fs.M3FS;
import java.io.IOException;
import java.util.Arrays;

/** Parallel mount columns. Caller holds this monitor during storage operations. */
final class M3Mounts {
    final M3EclipseFileSystem filesystem;
    private String[] names = new String[4];
    private M3FS[] images = new M3FS[4];
    private long[] generations = new long[4];
    private int size;

    M3Mounts(M3EclipseFileSystem filesystem) { this.filesystem = filesystem; }

    synchronized int reserve(String name) {
        for (int i = 0; i < size; i++) if (name.equals(names[i])) return i;
        if (size == names.length) {
            int capacity = Math.multiplyExact(size, 2);
            names = Arrays.copyOf(names, capacity);
            images = Arrays.copyOf(images, capacity);
            generations = Arrays.copyOf(generations, capacity);
        }
        names[size] = name;
        return size++;
    }

    synchronized String name(int slot) { return names[slot]; }
    synchronized long generation(int slot) { return generations[slot]; }

    synchronized M3FS image(int slot, long generation) throws IOException {
        if (generations[slot] != generation || images[slot] == null || !images[slot].isOpen()) {
            throw new IOException("M3FS mount is no longer available: " + names[slot]);
        }
        return images[slot];
    }

    synchronized int mount(String name, M3FS image) throws IOException {
        if (!image.isOpen()) throw new IOException("Cannot mount a closed M3FS image");
        int slot = reserve(name);
        if (images[slot] != null) throw new IOException("M3FS mount already exists: " + name);
        for (int i = 0; i < size; i++) if (images[i] == image) throw new IOException("Image lifetime already owned by a mount");
        long next = Math.incrementExact(generations[slot]);
        images[slot] = image;
        generations[slot] = next;
        return slot;
    }

    synchronized void unmount(String name) {
        for (int i = 0; i < size; i++) {
            if (!name.equals(names[i]) || images[i] == null) continue;
            M3FS image = images[i];
            images[i] = null;
            generations[i] = Math.incrementExact(generations[i]);
            image.close();
            return;
        }
    }

    synchronized void close() {
        for (int i = 0; i < size; i++) {
            if (images[i] != null) {
                M3FS image = images[i];
                images[i] = null;
                generations[i] = Math.incrementExact(generations[i]);
                image.close();
            }
        }
    }
}
