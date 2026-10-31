/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;

import com.m3.fs.vfs.Capability;
import com.m3.fs.vfs.FileObject;
import com.m3.fs.vfs.FileSystemException;
import com.m3.fs.vfs.FileSystemOptions;
import com.m3.fs.vfs.provider.AbstractFileName;
import com.m3.fs.vfs.provider.AbstractFileSystem;

/** One VFS mount borrows no payload store and owns one existing M3FS connection. */
final class DatabaseFileSystem extends AbstractFileSystem {
    private final Path database;
    private final boolean readOnly;
    private M3FS owner;
    private boolean closed;

    DatabaseFileSystem(AbstractFileName root, FileObject backing, Path database,
            FileSystemOptions options) throws FileSystemException {
        super(root, backing, options);
        this.database = database;
        this.readOnly = DatabaseFileProvider.readOnly(options);
        owner = openOwner();
    }

    private M3FS openOwner() throws FileSystemException {
        try {
            BasicFileAttributes attributes = Files.readAttributes(database,
                    BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!attributes.isRegularFile()) {
                throw new IOException("M3FS database backing must be a non-symlink regular file");
            }
            if (!M3FS.nativeAvailable() || !M3FS.supportsCheckedInodeIo()) {
                throw new IOException("M3FS native SQLite checked-inode I/O is unavailable");
            }
            if (readOnly) return M3FS.openReadOnly(database);
            // Validate the existing schema without changing a foreign SQLite database's journal mode.
            try (M3FS validation = M3FS.openReadOnly(database)) {
                if (validation.stat("/").kind() != M3FS.Kind.DIRECTORY) {
                    throw new IOException("M3FS database has no directory root");
                }
            }
            return M3FS.open(database);
        } catch (IOException | RuntimeException failure) {
            throw new FileSystemException("Cannot open M3FS database " + database, failure);
        }
    }

    synchronized M3FS owner() throws FileSystemException {
        if (closed) throw new FileSystemException("M3FS database filesystem is closed");
        if (owner == null) owner = openOwner();
        return owner;
    }

    synchronized M3FS writableOwner() throws FileSystemException {
        if (readOnly) throw new FileSystemException("M3FS database filesystem is read-only");
        return owner();
    }

    boolean isReadOnlyMount() {
        return readOnly;
    }

    boolean sameDatabase(DatabaseFileSystem other) throws IOException {
        return this == other || Files.isSameFile(database, other.database);
    }

    @Override
    protected FileObject createFile(AbstractFileName name) throws FileSystemException {
        owner();
        return new DatabaseFileObject(name, this);
    }

    @Override
    protected void addCapabilities(Collection<Capability> capabilities) {
        capabilities.addAll(DatabaseFileProvider.READ_CAPABILITIES);
        if (!readOnly) {
            capabilities.addAll(DatabaseFileProvider.WRITE_CAPABILITIES);
            if (M3FS.supportsSetMetadata()) {
                capabilities.add(Capability.SET_LAST_MODIFIED_FILE);
                capabilities.add(Capability.SET_LAST_MODIFIED_FOLDER);
            }
        }
    }

    @Override
    protected synchronized void doCloseCommunicationLink() {
        if (owner != null) {
            owner.close();
            owner = null;
        }
    }

    @Override
    public synchronized void close() {
        closed = true;
        super.close();
    }
}
