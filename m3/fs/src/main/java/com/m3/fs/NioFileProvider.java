/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import com.m3.fs.vfs.CacheStrategy;
import com.m3.fs.vfs.Capability;
import com.m3.fs.vfs.FileName;
import com.m3.fs.vfs.FileObject;
import com.m3.fs.vfs.FileSystemException;
import com.m3.fs.vfs.FileSystemOptions;
import com.m3.fs.vfs.FileType;
import com.m3.fs.vfs.provider.AbstractFileName;
import com.m3.fs.vfs.provider.AbstractFileObject;
import com.m3.fs.vfs.provider.AbstractFileSystem;
import com.m3.fs.vfs.provider.AbstractOriginatingFileProvider;
import com.m3.fs.vfs.provider.UriParser;
import com.m3.fs.vfs.util.FileObjectUtils;

/**
 * Session-owned VFS projections of caller-owned NIO filesystems. The only shared
 * bindings are weak filesystem identities, never file contents or namespace state.
 * Internal addresses can resolve only while that same filesystem remains live and
 * open in this JVM; they never mount a filesystem or close the caller's instance.
 * File moves require the same borrowed filesystem instance. Directory moves and
 * replacement of nonempty directories are unsupported. Replacement retains VFS's
 * non-atomic semantics; this bridge does not provide a cross-filesystem move fallback.
 */
final class NioFileProvider extends AbstractOriginatingFileProvider {
    static final String SCHEME = "m3nio";
    private static final List<Binding> BINDINGS = new ArrayList<>();
    private static final List<Capability> READ = List.of(Capability.GET_TYPE, Capability.LIST_CHILDREN,
            Capability.READ_CONTENT, Capability.GET_LAST_MODIFIED, Capability.URI);
    private static final List<Capability> WRITE = List.of(Capability.WRITE_CONTENT, Capability.CREATE,
            Capability.DELETE, Capability.RENAME, Capability.SET_LAST_MODIFIED_FILE,
            Capability.SET_LAST_MODIFIED_FOLDER);

    private static final class Binding extends WeakReference<FileSystem> {
        private final String token = UUID.randomUUID().toString();

        Binding(FileSystem fileSystem) {
            super(fileSystem);
        }
    }

    private static synchronized String bind(FileSystem fileSystem) throws FileSystemException {
        if (!fileSystem.isOpen()) {
            throw new FileSystemException("The borrowed NIO filesystem is closed");
        }
        BINDINGS.removeIf(binding -> binding.get() == null);
        for (Binding binding : BINDINGS) {
            if (binding.get() == fileSystem) {
                return binding.token;
            }
        }
        Binding binding = new Binding(fileSystem);
        BINDINGS.add(binding);
        return binding.token;
    }

    private static synchronized FileSystem borrowed(String token) throws FileSystemException {
        BINDINGS.removeIf(binding -> binding.get() == null);
        for (Binding binding : BINDINGS) {
            FileSystem fileSystem = binding.get();
            if (binding.token.equals(token) && fileSystem != null && fileSystem.isOpen()) {
                return fileSystem;
            }
        }
        throw new FileSystemException("The m3nio address has no live, open filesystem binding");
    }

    FileObject resolve(Path path) throws FileSystemException {
        try {
            Path absolute = path.toAbsolutePath();
            NioName name = NioName.of(bind(absolute.getFileSystem()), absolute);
            NioFileSystem fileSystem = (NioFileSystem) getFileSystem(name.getRoot(), null);
            return fileSystem.resolve(absolute);
        } catch (UnsupportedOperationException | ClosedFileSystemException failure) {
            throw new FileSystemException("The NIO provider cannot represent this path", failure);
        }
    }

    @Override
    public Collection<Capability> getCapabilities() {
        return READ;
    }

    @Override
    public FileName parseUri(FileName base, String address) throws FileSystemException {
        try {
            URI uri = URI.create(address);
            if (!SCHEME.equals(uri.getScheme()) || uri.getRawAuthority() == null
                    || uri.getRawQuery() != null || uri.getRawFragment() != null) {
                throw new IllegalArgumentException("Not an internal NIO address");
            }
            String path = uri.getRawPath();
            int endRoot = path == null ? -1 : path.indexOf('/', 1);
            if (endRoot < 2) {
                throw new IllegalArgumentException("Missing native root component");
            }
            String root = new String(HexFormat.of().parseHex(path.substring(1, endRoot)),
                    StandardCharsets.UTF_8);
            StringBuilder normalized = new StringBuilder(path.substring(endRoot));
            FileType type = UriParser.normalisePath(normalized);
            StringBuilder encoded = new StringBuilder();
            for (String component : normalized.toString().substring(1).split("/")) {
                if (!component.isEmpty()) {
                    String decoded = URI.create(SCHEME + ":/" + component).getPath().substring(1);
                    encoded.append('/').append(encode(decoded));
                }
            }
            return new NioName(uri.getRawAuthority(), root, encoded.length() == 0 ? "/" : encoded.toString(), type);
        } catch (IllegalArgumentException failure) {
            throw new FileSystemException("Invalid internal NIO address", failure);
        }
    }

    @Override
    protected com.m3.fs.vfs.FileSystem doCreateFileSystem(FileName rootName, FileSystemOptions options)
            throws FileSystemException {
        NioName name = (NioName) rootName;
        return new NioFileSystem(name, borrowed(name.token), options);
    }

    // VFS decodes ASCII %nn escapes at the metadata boundary. Unicode remains
    // intact internally; NioName.createURI supplies standard UTF-8 URI encoding.
    private static String encode(String component) {
        StringBuilder result = new StringBuilder(component.length());
        boolean dots = component.equals(".") || component.equals("..");
        for (int index = 0; index < component.length(); index++) {
            char value = component.charAt(index);
            boolean unreserved = value >= 'a' && value <= 'z' || value >= 'A' && value <= 'Z'
                    || value >= '0' && value <= '9' || value == '-' || value == '_' || value == '~'
                    || value == '.' && !dots;
            if (value < 128 && !unreserved) {
                result.append('%').append(Character.forDigit(value >>> 4, 16))
                        .append(Character.forDigit(value & 15, 16));
            } else {
                result.append(value);
            }
        }
        return result.toString();
    }

    private static final class NioName extends AbstractFileName {
        private final String token;
        private final String nativeRoot;

        NioName(String token, String nativeRoot, String path, FileType type) {
            super(SCHEME, path, type);
            this.token = token;
            this.nativeRoot = nativeRoot;
        }

        static NioName of(String token, Path absolute) throws FileSystemException {
            Path root = absolute.getRoot();
            if (root == null) {
                throw new FileSystemException("The NIO path has no absolute filesystem root");
            }
            StringBuilder path = new StringBuilder();
            for (Path component : absolute) {
                path.append('/').append(encode(component.toString()));
            }
            return new NioName(token, root.toString(), path.length() == 0 ? "/" : path.toString(), FileType.FILE);
        }

        @Override
        protected void appendRootUri(StringBuilder buffer, boolean addPassword) {
            buffer.append(SCHEME).append("://").append(token).append('/')
                    .append(HexFormat.of().formatHex(nativeRoot.getBytes(StandardCharsets.UTF_8)));
        }

        @Override
        protected String createURI() {
            StringBuilder uri = new StringBuilder();
            appendRootUri(uri, false);
            return URI.create(uri.append(getPath()).toString()).toASCIIString();
        }

        @Override
        public String getFriendlyURI() {
            return getURI();
        }

        @Override
        public FileName createName(String path, FileType type) {
            return new NioName(token, nativeRoot, path, type);
        }
    }

    private static final class NioFileSystem extends AbstractFileSystem {
        private final FileSystem borrowed;
        private final Path nativeRoot;

        NioFileSystem(NioName rootName, FileSystem borrowed, FileSystemOptions options) throws FileSystemException {
            super(rootName, null, options);
            this.borrowed = borrowed;
            try {
                nativeRoot = borrowed.getPath(rootName.nativeRoot);
                if (!nativeRoot.isAbsolute() || !nativeRoot.equals(nativeRoot.getRoot())) {
                    throw new IllegalArgumentException("Not a native filesystem root");
                }
            } catch (IllegalArgumentException | UnsupportedOperationException failure) {
                throw new FileSystemException("Invalid borrowed NIO filesystem root", failure);
            }
        }

        @Override
        protected void addCapabilities(Collection<Capability> capabilities) {
            capabilities.addAll(READ);
            if (!borrowed.isReadOnly()) {
                capabilities.addAll(WRITE);
            }
        }

        @Override
        protected FileObject createFile(AbstractFileName fileName) throws Exception {
            Path path = nativeRoot;
            for (String component : fileName.getPath().substring(1).split("/")) {
                if (!component.isEmpty()) {
                    Path part = borrowed.getPath(UriParser.decode(component));
                    if (part.isAbsolute() || part.getNameCount() != 1) {
                        throw new FileSystemException("A NIO address component is not one native path name");
                    }
                    path = path.resolve(part);
                }
            }
            return new NioFileObject(fileName, this, path);
        }

        synchronized FileObject resolve(Path path) throws FileSystemException {
            if (path.getFileSystem() != borrowed || !nativeRoot.equals(path.getRoot())) {
                throw new FileSystemException("A NIO directory entry belongs to a different filesystem root");
            }
            NioName name = NioName.of(((NioName) getRootName()).token, path);
            FileObject file = getFileFromCache(name);
            if (file == null) {
                file = decorateFileObject(new NioFileObject(name, this, path));
                putFileToCache(file);
            }
            if (getFileSystemManager().getCacheStrategy() == CacheStrategy.ON_RESOLVE) {
                file.refresh();
            }
            return file;
        }

        // AbstractFileSystem closes its VFS resources. It has no ownership of borrowed.
    }

    private static final class NioFileObject extends AbstractFileObject<NioFileSystem> {
        private final Path path;

        NioFileObject(AbstractFileName name, NioFileSystem fileSystem, Path path) {
            super(name, fileSystem);
            this.path = path;
        }

        @Override
        public URI getURI() {
            return path.toUri();
        }

        @Override
        protected FileType doGetType() throws IOException {
            try {
                return Files.readAttributes(path, BasicFileAttributes.class).isDirectory()
                        ? FileType.FOLDER : FileType.FILE;
            } catch (NoSuchFileException missing) {
                return FileType.IMAGINARY;
            }
        }

        @Override
        protected boolean doIsSymbolicLink() throws IOException {
            try {
                return Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS).isSymbolicLink();
            } catch (NoSuchFileException missing) {
                return false;
            }
        }

        @Override
        protected long doGetContentSize() throws IOException {
            return Files.size(path);
        }

        @Override
        protected long doGetLastModifiedTime() throws IOException {
            return Files.getLastModifiedTime(path).toMillis();
        }

        @Override
        protected InputStream doGetInputStream(int bufferSize) throws IOException {
            try {
                return Files.newInputStream(path);
            } catch (UnsupportedOperationException | ClosedFileSystemException failure) {
                throw new IOException("The borrowed NIO provider cannot open input", failure);
            }
        }

        @Override
        protected OutputStream doGetOutputStream(boolean append) throws IOException {
            if (append) {
                throw new IOException("The NIO bridge does not advertise portable append support");
            }
            try {
                if (path.getFileSystem().isReadOnly()) {
                    throw new IOException("The borrowed NIO filesystem is read-only");
                }
                return Files.newOutputStream(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING);
            } catch (UnsupportedOperationException | ClosedFileSystemException failure) {
                throw new IOException("The borrowed NIO provider cannot open output", failure);
            }
        }

        @Override
        protected FileObject[] doListChildrenResolved() throws IOException {
            List<FileObject> children = new ArrayList<>();
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                for (Path child : entries) {
                    children.add(getAbstractFileSystem().resolve(child));
                }
            }
            return children.toArray(FileObject[]::new);
        }

        @Override
        protected String[] doListChildren() throws IOException {
            FileObject[] children = doListChildrenResolved();
            String[] names = new String[children.length];
            for (int index = 0; index < children.length; index++) {
                names[index] = children[index].getName().getBaseName();
            }
            return names;
        }

        @Override
        protected void doCreateFolder() throws IOException {
            Files.createDirectory(path);
        }

        @Override
        protected void doDelete() throws IOException {
            Files.delete(path);
        }

        @Override
        public boolean canRenameTo(FileObject destination) {
            return destination.getFileSystem() instanceof NioFileSystem fileSystem
                    && fileSystem.borrowed == path.getFileSystem();
        }

        @Override
        public void moveTo(FileObject destination) throws FileSystemException {
            FileObject unwrapped = FileObjectUtils.getAbstractFileObject(destination);
            if (!(unwrapped instanceof NioFileObject target)
                    || target.path.getFileSystem() != path.getFileSystem()) {
                throw new FileSystemException("NIO move requires the same borrowed filesystem instance");
            }
            try {
                // Validate existence before VFS can delete an existing destination.
                Path source = path.toRealPath();
                Path sourceName = path.toAbsolutePath().normalize();
                Path targetName = target.path.toAbsolutePath().normalize();
                Path targetLocation;
                boolean targetExists = true;
                try {
                    targetLocation = target.path.toRealPath();
                } catch (NoSuchFileException missing) {
                    targetExists = false;
                    Path parent = target.path.getParent();
                    if (parent == null) {
                        throw missing;
                    }
                    targetLocation = parent.toRealPath().resolve(target.path.getFileName());
                }
                if (sourceName.getParent() == null || targetName.getParent() == null
                        || source.getParent() == null || targetLocation.getParent() == null) {
                    throw new IOException("NIO filesystem roots cannot be moved or replaced");
                }
                if (targetExists && Files.isSameFile(path, target.path)) {
                    return;
                }
                if (sourceName.startsWith(targetName) || targetName.startsWith(sourceName)
                        || source.startsWith(targetLocation) || targetLocation.startsWith(source)) {
                    throw new IOException("NIO move source and destination overlap");
                }
                if (Files.readAttributes(path, BasicFileAttributes.class).isDirectory()) {
                    throw new IOException("The NIO bridge does not provide portable directory moves");
                }
                if (targetExists && Files.readAttributes(target.path, BasicFileAttributes.class).isDirectory()) {
                    try (DirectoryStream<Path> children = Files.newDirectoryStream(target.path)) {
                        if (children.iterator().hasNext()) {
                            throw new IOException("NIO move cannot replace a nonempty directory");
                        }
                    }
                }
            } catch (IOException | UnsupportedOperationException | ClosedFileSystemException failure) {
                throw new FileSystemException("Cannot safely move the borrowed NIO path", failure);
            }
            super.moveTo(destination);
        }

        @Override
        protected void doRename(FileObject destination) throws IOException {
            FileObject unwrapped = FileObjectUtils.getAbstractFileObject(destination);
            if (!(unwrapped instanceof NioFileObject target)
                    || target.path.getFileSystem() != path.getFileSystem()) {
                throw new IOException("NIO rename requires the same borrowed filesystem instance");
            }
            try {
                Files.move(path, target.path);
            } catch (UnsupportedOperationException | ClosedFileSystemException failure) {
                throw new IOException("The borrowed NIO provider cannot rename this file", failure);
            }
        }

        @Override
        protected boolean doSetLastModifiedTime(long time) throws IOException {
            Files.setLastModifiedTime(path, FileTime.fromMillis(time));
            return true;
        }

        @Override
        protected boolean doIsReadable() {
            return Files.isReadable(path);
        }

        @Override
        protected boolean doIsWriteable() {
            return !path.getFileSystem().isReadOnly() && Files.isWritable(path);
        }

        @Override
        protected boolean doIsHidden() throws IOException {
            return Files.isHidden(path);
        }

        @Override
        protected boolean doIsExecutable() {
            return Files.isExecutable(path);
        }

        @Override
        protected boolean doIsSameFile(FileObject destination) throws FileSystemException {
            FileObject unwrapped = FileObjectUtils.getAbstractFileObject(destination);
            if (!(unwrapped instanceof NioFileObject target)
                    || target.path.getFileSystem() != path.getFileSystem()) {
                return false;
            }
            try {
                return Files.isSameFile(path, target.path);
            } catch (IOException failure) {
                throw new FileSystemException("Cannot compare borrowed NIO files", failure);
            }
        }
    }
}
