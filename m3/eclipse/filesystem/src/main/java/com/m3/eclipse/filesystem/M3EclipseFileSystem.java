// Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.filesystem;

import com.m3.fs.M3FS;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Objects;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileSystem;

/** Eclipse API ownership and explicit mounts over the M3JDK21 filesystem owner. */
public final class M3EclipseFileSystem extends FileSystem implements AutoCloseable {
    public static final String PLUGIN_ID = "com.m3.eclipse.filesystem";
    public static final String SCHEME = "m3fs";
    private final M3Mounts mounts = new M3Mounts(this);
    private boolean closed;

    public M3EclipseFileSystem() { M3EclipseActivator.track(this); }

    @Override public int attributes() {
        return EFS.ATTRIBUTE_READ_ONLY | EFS.ATTRIBUTE_EXECUTABLE
                | EFS.ATTRIBUTE_SYMLINK | EFS.ATTRIBUTE_LINK_TARGET;
    }
    @Override public boolean canDelete() { return true; }
    @Override public boolean canWrite() { return true; }
    @Override public boolean isCaseSensitive() { return true; }

    @Override public IFileStore getStore(URI uri) {
        Objects.requireNonNull(uri, "uri");
        String name;
        String path;
        try {
            name = M3Paths.uriMount(uri);
            path = M3Paths.uriPath(uri);
        } catch (IllegalArgumentException invalid) {
            return EFS.getNullFileSystem().getStore(uri);
        }
        synchronized (mounts) {
            int slot = mounts.reserve(name);
            return new M3EclipseFileStore(mounts, slot, mounts.generation(slot), path);
        }
    }

    public IFileStore mount(String name, M3FS image) throws IOException {
        M3Paths.mount(name);
        Objects.requireNonNull(image, "image");
        synchronized (mounts) {
            if (closed) throw new IOException("M3FS provider is closed");
            int slot = mounts.mount(name, image);
            return new M3EclipseFileStore(mounts, slot, mounts.generation(slot), "/");
        }
    }

    public IFileStore create(String name, Path database) throws IOException {
        M3Paths.mount(name);
        M3FS image = M3FS.create(database);
        try { return mount(name, image); }
        catch (IOException | RuntimeException failure) { image.close(); throw failure; }
    }

    public IFileStore open(String name, Path database, boolean readOnly) throws IOException {
        M3Paths.mount(name);
        M3FS image = readOnly ? M3FS.openReadOnly(database) : M3FS.open(database);
        try { return mount(name, image); }
        catch (IOException | RuntimeException failure) { image.close(); throw failure; }
    }

    public void unmount(String name) { mounts.unmount(M3Paths.mount(name)); }

    @Override public void close() {
        synchronized (mounts) {
            if (closed) return;
            closed = true;
            mounts.close();
        }
        M3EclipseActivator.untrack(this);
    }
}
