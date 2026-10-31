/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.util.Objects;

import com.m3.fs.vfs.FileObject;
import com.m3.fs.vfs.FileSelector;
import com.m3.fs.vfs.FileSystemException;
import com.m3.fs.vfs.FileType;
import com.m3.fs.vfs.provider.AbstractFileName;
import com.m3.fs.vfs.provider.AbstractFileObject;
import com.m3.fs.vfs.provider.UriParser;
import com.m3.fs.vfs.util.FileObjectUtils;

/** Backend atoms only; AbstractFileObject owns the provider-neutral operations. */
final class DatabaseFileObject extends AbstractFileObject<DatabaseFileSystem> {
    private static final int FILE_MODE = 0644;
    private static final int DIRECTORY_MODE = 0755;
    private static final long NANOS_PER_MILLISECOND = 1_000_000L;
    private static final char[] LAYER_RESERVED = {'!'};
    private final String path;

    DatabaseFileObject(AbstractFileName name, DatabaseFileSystem fileSystem)
            throws FileSystemException {
        super(name, fileSystem);
        String decoded = name.getPathDecoded();
        path = decoded.length() > 1 && decoded.endsWith("/")
                ? decoded.substring(0, decoded.length() - 1) : decoded;
    }

    private M3FS.Stat stat() throws IOException {
        return getAbstractFileSystem().owner().stat(path);
    }

    private void requireNonRoot() throws FileSystemException {
        if ("/".equals(path)) throw new FileSystemException("M3FS database root cannot be deleted or moved");
    }

    private static void requireRegularFile(M3FS.Stat metadata) throws IOException {
        if (metadata.kind() != M3FS.Kind.FILE) {
            throw new IOException("M3FS database content I/O requires a regular file; links are not followed");
        }
    }

    @Override
    protected FileType doGetType() throws IOException {
        try {
            return stat().kind() == M3FS.Kind.DIRECTORY ? FileType.FOLDER : FileType.FILE;
        } catch (NoSuchFileException absent) {
            return FileType.IMAGINARY;
        }
    }

    @Override
    protected long doGetContentSize() throws IOException {
        return stat().size();
    }

    @Override
    protected long doGetLastModifiedTime() throws IOException {
        return stat().modifiedTimeNanos() / NANOS_PER_MILLISECOND;
    }

    @Override
    protected boolean doSetLastModifiedTime(long modified) throws IOException {
        if (modified < 0 || modified > Long.MAX_VALUE / NANOS_PER_MILLISECOND) {
            throw new IOException("M3FS modification time is outside the native nanosecond range");
        }
        getAbstractFileSystem().writableOwner().setMetadata(path, 0,
                modified * NANOS_PER_MILLISECOND, false, true);
        return true;
    }

    @Override
    protected String[] doListChildren() throws IOException {
        M3DirectoryTable table = getAbstractFileSystem().owner().listTable(path);
        String[] names = new String[table.size()];
        for (int row = 0; row < names.length; row++) {
            String name = table.name(row);
            if (name.indexOf('\\') >= 0) {
                throw new IOException("A native M3FS basename contains a VFS path separator");
            }
            names[row] = UriParser.encode(name, LAYER_RESERVED);
        }
        return names;
    }

    @Override
    protected boolean doIsReadable() throws IOException {
        return stat().kind() != M3FS.Kind.SYMLINK;
    }

    @Override
    protected boolean doIsWriteable() throws IOException {
        return !getAbstractFileSystem().isReadOnlyMount() && stat().kind() != M3FS.Kind.SYMLINK;
    }

    @Override
    protected boolean doIsSymbolicLink() throws IOException {
        return stat().kind() == M3FS.Kind.SYMLINK;
    }

    @Override
    protected InputStream doGetInputStream(int bufferSize) throws IOException {
        M3FS owner = getAbstractFileSystem().owner();
        M3FS.Stat metadata = owner.stat(path);
        requireRegularFile(metadata);
        return new DatabaseInput(owner, path, metadata.inode());
    }

    @Override
    protected OutputStream doGetOutputStream(boolean append) throws IOException {
        M3FS owner = getAbstractFileSystem().writableOwner();
        M3FS.Stat metadata;
        try {
            metadata = owner.stat(path);
        } catch (NoSuchFileException absent) {
            try {
                owner.createFile(path, FILE_MODE);
            } catch (FileAlreadyExistsException racedCreation) {
                // The open binds whichever current regular inode the next stat observes.
            }
            metadata = owner.stat(path);
        }
        requireRegularFile(metadata);
        if (!append) owner.truncateIfInode(path, metadata.inode(), 0);
        return new DatabaseOutput(owner, path, metadata.inode(), append);
    }

    @Override
    protected void doCreateFolder() throws IOException {
        M3FS owner = getAbstractFileSystem().writableOwner();
        try {
            owner.mkdir(path, DIRECTORY_MODE);
        } catch (FileAlreadyExistsException racedCreation) {
            if (owner.stat(path).kind() != M3FS.Kind.DIRECTORY) throw racedCreation;
        }
    }

    @Override
    public int delete(FileSelector selector) throws FileSystemException {
        requireNonRoot();
        getAbstractFileSystem().writableOwner();
        return super.delete(selector);
    }

    @Override
    protected void doDelete() throws IOException {
        requireNonRoot();
        M3FS owner = getAbstractFileSystem().writableOwner();
        if (owner.stat(path).kind() == M3FS.Kind.DIRECTORY) owner.rmdir(path);
        else owner.unlink(path);
    }

    @Override
    public void moveTo(FileObject destination) throws FileSystemException {
        Objects.requireNonNull(destination, "destination");
        requireNonRoot();
        getAbstractFileSystem().writableOwner();
        try {
            // The inherited move can delete its destination before opening the source.
            M3FS.Stat source = stat();
            DatabaseFileObject target = databaseObject(destination);
            if (target == null) {
                throw new FileSystemException("M3FS move requires the same mounted filesystem");
            }
            target.requireNonRoot();
            target.getAbstractFileSystem().writableOwner();
            if (getAbstractFileSystem().sameDatabase(target.getAbstractFileSystem())) {
                if (path.startsWith(target.path + '/')
                        || source.kind() == M3FS.Kind.DIRECTORY && target.path.startsWith(path + '/')) {
                    throw new FileSystemException("M3FS move cannot overlap an ancestor or descendant");
                }
                if (sameInode(target, source.inode())) return;
            }
            if (target.getFileSystem() != getFileSystem()) {
                throw new FileSystemException("M3FS move requires the same mounted filesystem");
            }
        } catch (IOException failure) {
            throw new FileSystemException("Cannot prepare M3FS move", failure);
        }
        // The inherited move may remove the destination first; it is not an atomic replace.
        super.moveTo(destination);
    }

    @Override
    protected boolean doIsSameFile(FileObject destination) throws FileSystemException {
        DatabaseFileObject target = databaseObject(destination);
        if (target == null) return false;
        try {
            return getAbstractFileSystem().sameDatabase(target.getAbstractFileSystem())
                    && sameInode(target, stat().inode());
        } catch (NoSuchFileException absent) {
            return false;
        } catch (IOException failure) {
            throw new FileSystemException("Cannot inspect M3FS inode identity", failure);
        }
    }

    private static DatabaseFileObject databaseObject(FileObject file) throws FileSystemException {
        return FileObjectUtils.isInstanceOf(file, DatabaseFileObject.class)
                ? (DatabaseFileObject) FileObjectUtils.getAbstractFileObject(file) : null;
    }

    private static boolean sameInode(DatabaseFileObject target, long inode) throws IOException {
        try {
            return target.stat().inode() == inode;
        } catch (NoSuchFileException absent) {
            return false;
        }
    }

    @Override
    protected void doRename(FileObject destination) throws IOException {
        DatabaseFileObject target = databaseObject(destination);
        if (target == null || target.getFileSystem() != getFileSystem()) {
            throw new IOException("M3FS rename requires the same mounted filesystem");
        }
        getAbstractFileSystem().writableOwner().rename(path, target.path);
    }

    /** The existing VFS content owner supplies buffering and tracks this raw stream. */
    private static final class DatabaseInput extends InputStream {
        private final M3FS owner;
        private final String path;
        private final long inode;
        private final byte[] single = new byte[1];
        private long position;
        private boolean closed;

        private DatabaseInput(M3FS owner, String path, long inode) {
            this.owner = owner;
            this.path = path;
            this.inode = inode;
        }

        @Override
        public synchronized int read() throws IOException {
            return read(single, 0, 1) < 0 ? -1 : Byte.toUnsignedInt(single[0]);
        }

        @Override
        public synchronized int read(byte[] bytes, int offset, int length) throws IOException {
            Objects.checkFromIndexSize(offset, length, Objects.requireNonNull(bytes, "bytes").length);
            synchronized (owner) {
                if (closed || !owner.isOpen()) throw new IOException("M3FS database input is closed");
                if (length == 0) return 0;
                int count = owner.readIfInode(path, inode, position, bytes, offset, length);
                position += count;
                return count == 0 ? -1 : count;
            }
        }

        @Override
        public synchronized void close() {
            closed = true;
        }
    }

    private static final class DatabaseOutput extends OutputStream {
        private final M3FS owner;
        private final String path;
        private final long inode;
        private final boolean append;
        private final byte[] single = new byte[1];
        private long position;
        private boolean closed;

        private DatabaseOutput(M3FS owner, String path, long inode, boolean append) {
            this.owner = owner;
            this.path = path;
            this.inode = inode;
            this.append = append;
        }

        @Override
        public synchronized void write(int value) throws IOException {
            single[0] = (byte) value;
            write(single, 0, 1);
        }

        @Override
        public synchronized void write(byte[] bytes, int offset, int length) throws IOException {
            Objects.checkFromIndexSize(offset, length, Objects.requireNonNull(bytes, "bytes").length);
            synchronized (owner) {
                requireOpen();
                if (length == 0) return;
                if (append) owner.appendIfInode(path, inode, bytes, offset, length);
                else {
                    if (length > Long.MAX_VALUE - position) throw new IOException("M3FS output position overflow");
                    owner.writeIfInode(path, inode, position, bytes, offset, length);
                    position += length;
                }
            }
        }

        @Override
        public synchronized void flush() throws IOException {
            synchronized (owner) {
                requireOpen(); // Each completed native write has already committed.
            }
        }

        private void requireOpen() throws IOException {
            if (closed || !owner.isOpen()) throw new IOException("M3FS database output is closed");
        }

        @Override
        public synchronized void close() {
            closed = true;
        }
    }
}
