/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import com.m3.fs.vfs.Capability;
import com.m3.fs.vfs.FileName;
import com.m3.fs.vfs.FileObject;
import com.m3.fs.vfs.FileSystem;
import com.m3.fs.vfs.FileSystemConfigBuilder;
import com.m3.fs.vfs.FileSystemException;
import com.m3.fs.vfs.FileSystemOptions;
import com.m3.fs.vfs.FileType;
import com.m3.fs.vfs.provider.AbstractLayeredFileProvider;
import com.m3.fs.vfs.provider.LayeredFileName;
import com.m3.fs.vfs.provider.local.LocalFile;
import com.m3.fs.vfs.provider.local.LocalFileName;
import com.m3.fs.vfs.util.FileObjectUtils;

/**
 * VFS template hooks backed by the existing native SQLite {@link M3FS} owner.
 * Addresses have the form {@code m3db:file:///absolute/store.sqlite!/folder/file}.
 * The backing file must already be a local, non-symlink regular M3FS database;
 * create it explicitly with {@link M3FS#create(Path)} before mounting it.
 * The provider never replicates a database to a temporary file or issues SQL.
 *
 * <p>The inherited VFS methods supply traversal, copy, recursive folder creation,
 * selectors, content conveniences and buffering. Native calls own persistence,
 * inode identity and each individual mutation. Moves require the same mounted
 * filesystem; cross-mount and cross-provider transfers can use inherited copying.
 * Moving an entry onto the same native inode is a no-op, including hard-link
 * aliases across mounts. Overlapping ancestor/descendant moves are rejected before
 * destination deletion. Generic VFS moves, recursive operations and complete
 * output streams are not atomic transactions.</p>
 *
 * <p>Streams check their opened inode on each native transfer. An external path
 * replacement or rename can therefore make a stream fail; it never switches to
 * the replacement inode. Live reads are not snapshots. External metadata changes
 * follow the existing VFS refresh/cache strategy. Close streams before closing
 * their manager; closing the filesystem ends its native content lifetime.</p>
 *
 * <p>Native mode bits remain metadata, without a user/group authorization model.
 * Read-only mounting is enforced; permission setters and random access are not
 * supplied. Symbolic links are visible as no-follow file entries with link
 * metadata, but content I/O through them is rejected. Database roots cannot be
 * deleted or moved. Existing VFS path normalization applies; native basenames
 * containing a backslash cannot be represented and are rejected on listing.</p>
 */
public final class DatabaseFileProvider extends AbstractLayeredFileProvider {
    static final List<Capability> READ_CAPABILITIES = List.of(
            Capability.GET_TYPE, Capability.LIST_CHILDREN, Capability.READ_CONTENT,
            Capability.GET_LAST_MODIFIED, Capability.URI);
    static final List<Capability> WRITE_CAPABILITIES = List.of(
            Capability.CREATE, Capability.DELETE, Capability.RENAME,
            Capability.WRITE_CONTENT, Capability.APPEND_CONTENT);
    private static final List<Capability> CAPABILITIES = List.of(
            Capability.GET_TYPE, Capability.LIST_CHILDREN, Capability.READ_CONTENT,
            Capability.GET_LAST_MODIFIED, Capability.URI, Capability.CREATE,
            Capability.DELETE, Capability.RENAME, Capability.WRITE_CONTENT,
            Capability.APPEND_CONTENT, Capability.SET_LAST_MODIFIED_FILE,
            Capability.SET_LAST_MODIFIED_FOLDER);
    private static final Configuration CONFIGURATION = new Configuration();

    /** Creates a provider; native loading is deferred until a database is mounted. */
    public DatabaseFileProvider() {
    }

    /**
     * Creates ordinary VFS options selecting a read-only or writable existing
     * database mount. Default options select a writable mount. The same manager
     * can keep separate read-only and writable mounts of one database.
     *
     * @param readOnly whether mutations must be refused
     * @return new options for {@code FileSystemManager.resolveFile(address, options)}
     */
    public static FileSystemOptions options(boolean readOnly) {
        FileSystemOptions options = new FileSystemOptions();
        CONFIGURATION.setReadOnly(options, readOnly);
        return options;
    }

    static boolean readOnly(FileSystemOptions options) {
        return CONFIGURATION.readOnly(options);
    }

    @Override
    public FileSystemConfigBuilder getConfigBuilder() {
        return CONFIGURATION;
    }

    @Override
    public Collection<Capability> getCapabilities() {
        return CAPABILITIES;
    }

    @Override
    public FileName parseUri(FileName base, String uri) throws FileSystemException {
        LayeredFileName parsed = (LayeredFileName) super.parseUri(base, uri);
        return new DatabaseFileName(parsed.getScheme(), parsed.getOuterName(),
                parsed.getPath(), parsed.getType());
    }

    @Override
    protected FileSystem doCreateFileSystem(String scheme, FileObject file,
            FileSystemOptions options) throws FileSystemException {
        if (!"file".equals(file.getName().getScheme())
                || !(file.getName() instanceof LocalFileName localName)
                || !FileObjectUtils.isInstanceOf(file, LocalFile.class)) {
            throw new FileSystemException("M3FS database backing must be a local file");
        }
        // This is the same root-plus-decoded-path projection used by LocalFile.
        // Do not replicate or reinterpret VFS escapes through a second URI parser.
        final Path database;
        try {
            database = Path.of(localName.getRootFile() + localName.getPathDecoded())
                    .toAbsolutePath().normalize();
        } catch (RuntimeException failure) {
            throw new FileSystemException("Invalid local M3FS database path", failure);
        }
        LayeredFileName root = new DatabaseFileName(scheme, file.getName(),
                FileName.ROOT_PATH, FileType.FOLDER);
        return new DatabaseFileSystem(root, file, database, options);
    }

    private static final class Configuration extends FileSystemConfigBuilder {
        private static final String READ_ONLY = "readOnly";

        @Override
        protected Class<? extends FileSystem> getConfigClass() {
            return DatabaseFileSystem.class;
        }

        private void setReadOnly(FileSystemOptions options, boolean value) {
            setParam(options, READ_ONLY, value);
        }

        private boolean readOnly(FileSystemOptions options) {
            return Boolean.TRUE.equals(getParam(options, READ_ONLY));
        }
    }
}
