// Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.filesystem;

import com.m3.fs.M3DirectoryTable;
import com.m3.fs.M3FS;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.util.Arrays;
import java.util.Objects;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

/** A requested Eclipse handle; no inode or child metadata is cached here. */
final class M3EclipseFileStore extends FileStore {
    // This flag is defined by the pinned Eclipse source contract but was unnamed in EFS 1.11.400.
    private static final int IGNORE_NAME_CASE = 1 << 13;
    private static final Object COPY_LOCK = new Object();
    private static final ThreadLocal<CopyPath> COPY_PATH = new ThreadLocal<>();
    private static final byte[] EMPTY = new byte[0];
    private final M3Mounts mounts;
    private final int slot;
    private final long generation;
    private final String path;

    M3EclipseFileStore(M3Mounts mounts, int slot, long generation, String path) {
        this.mounts = mounts;
        this.slot = slot;
        this.generation = generation;
        this.path = path;
    }

    private M3FS image() throws IOException { return mounts.image(slot, generation); }

    private M3FS writable() throws IOException, CoreException {
        M3FS image = image();
        if (image.isReadOnly()) throw failure(EFS.ERROR_READ_ONLY, "M3FS image is read-only", null);
        return image;
    }

    private static void canceled(IProgressMonitor monitor) {
        if (monitor != null && monitor.isCanceled()) throw new OperationCanceledException();
    }

    private static void options(int options, int allowed) {
        if ((options & ~allowed) != 0) throw new IllegalArgumentException("Unsupported EFS option flags");
    }

    private static CoreException failure(int code, String message, Throwable cause) {
        return new CoreException(new Status(IStatus.ERROR, M3EclipseFileSystem.PLUGIN_ID, code, message, cause));
    }

    private CoreException io(int operation, IOException problem) {
        int code = problem instanceof NoSuchFileException ? EFS.ERROR_NOT_EXISTS
                : problem instanceof FileAlreadyExistsException ? EFS.ERROR_EXISTS
                : problem instanceof NotDirectoryException ? EFS.ERROR_WRONG_TYPE : operation;
        return failure(code, path + ": " + problem.getMessage(), problem);
    }

    @Override public String getName() { return M3Paths.basename(path); }
    @Override public URI toURI() { return M3Paths.uri(mounts.name(slot), path); }
    @Override public IFileSystem getFileSystem() { return mounts.filesystem; }

    @Override public IFileStore getChild(String name) {
        return new M3EclipseFileStore(mounts, slot, generation, M3Paths.child(path, name));
    }

    @Override public IFileStore getParent() {
        String parent = M3Paths.parent(path);
        return parent == null ? null : new M3EclipseFileStore(mounts, slot, generation, parent);
    }

    /** Resolve symbolic links inside this namespace; the final native path is retained by streams. */
    private static String resolve(M3FS image, String original, boolean followFinal) throws IOException {
        String current = original;
        for (int links = 0; links <= 40; links++) {
            if (current.equals("/")) return current;
            boolean restarted = false;
            for (int end = 1; end <= current.length(); end++) {
                if (end != current.length() && current.charAt(end) != '/') continue;
                String prefix = current.substring(0, end);
                M3FS.Stat stat = image.stat(prefix);
                boolean last = end == current.length();
                if (stat.kind() == M3FS.Kind.SYMLINK && (!last || followFinal)) {
                    if (links == 40) throw new IOException("Too many M3FS symbolic links");
                    try {
                        String target = M3Paths.linkTarget(M3Paths.parent(prefix), image.readlink(prefix));
                        String joined = (target.equals("/") ? "" : target) + current.substring(end);
                        current = M3Paths.path(joined.isEmpty() ? "/" : joined);
                    } catch (IllegalArgumentException invalid) {
                        throw new IOException("Invalid M3FS symbolic link target", invalid);
                    }
                    restarted = true;
                    break;
                }
                if (!last && stat.kind() != M3FS.Kind.DIRECTORY) throw new NotDirectoryException(prefix);
            }
            if (!restarted) return current;
        }
        throw new IOException("Too many M3FS symbolic links");
    }

    private static String parents(M3FS image, String path) throws IOException {
        String parent = M3Paths.parent(path);
        return parent == null ? "/" : M3Paths.child(resolve(image, parent, true), M3Paths.basename(path));
    }

    private static FileInfo project(String name, M3FS.Kind kind, long size, long time, int mode, boolean readOnly) {
        FileInfo info = new FileInfo(name);
        info.setExists(true);
        info.setDirectory(kind == M3FS.Kind.DIRECTORY);
        info.setLength(size);
        info.setLastModified(time / 1_000_000L);
        info.setAttribute(EFS.ATTRIBUTE_READ_ONLY, readOnly || (mode & 0222) == 0);
        info.setAttribute(EFS.ATTRIBUTE_EXECUTABLE, (mode & 0111) != 0);
        return info;
    }

    private static FileInfo info(M3FS image, String path, String name) throws IOException {
        String physical = parents(image, path);
        M3FS.Stat entry = image.stat(physical);
        if (entry.kind() != M3FS.Kind.SYMLINK) {
            return project(name, entry.kind(), entry.size(), entry.modifiedTimeNanos(), entry.mode(), image.isReadOnly());
        }
        String target = image.readlink(physical);
        FileInfo result;
        try {
            M3FS.Stat actual = image.stat(resolve(image, physical, true));
            result = project(name, actual.kind(), actual.size(), actual.modifiedTimeNanos(), actual.mode(), image.isReadOnly());
} catch (NoSuchFileException | NotDirectoryException missingTarget) {
            // EFS keeps broken links in enumeration but projects exists=false.
            result = new FileInfo(name);
        }
        result.setAttribute(EFS.ATTRIBUTE_SYMLINK, true);
        result.setStringAttribute(EFS.ATTRIBUTE_LINK_TARGET, target);
        return result;
    }

    @Override public IFileInfo fetchInfo(int options, IProgressMonitor monitor) throws CoreException {
        options(options, IGNORE_NAME_CASE);
        canceled(monitor);
        synchronized (mounts) {
            try { return info(image(), path, getName()); }
catch (NoSuchFileException | NotDirectoryException missing) { return new FileInfo(getName()); }
            catch (IOException problem) { throw io(EFS.ERROR_READ, problem); }
        }
    }

    @Override public String[] childNames(int options, IProgressMonitor monitor) throws CoreException {
        options(options, EFS.NONE);
        canceled(monitor);
        synchronized (mounts) {
            try {
                M3FS image = image();
                String directory = resolve(image, path, true);
                String[] names = new String[32];
                int count = 0;
                String after = "";
                for (;;) {
                    canceled(monitor);
                    M3FS.DirectoryPage page = image.listPage(directory, after, 512);
                    M3DirectoryTable table = page.table();
                    for (int row = 0; row < table.size(); row++) {
                        if (count == names.length) names = Arrays.copyOf(names, Math.multiplyExact(count, 2));
                        names[count++] = table.name(row);
                    }
                    if (!page.hasMore()) return Arrays.copyOf(names, count);
                    if (table.size() == 0) throw new IOException("M3FS directory cursor made no progress");
                    after = page.nextAfter();
                }
            } catch (IOException problem) { throw io(EFS.ERROR_READ, problem); }
        }
    }

    @Override public IFileInfo[] childInfos(int options, IProgressMonitor monitor) throws CoreException {
        options(options, EFS.NONE);
        canceled(monitor);
        synchronized (mounts) {
            try {
                M3FS image = image();
                String directory = resolve(image, path, true);
                IFileInfo[] result = new IFileInfo[32];
                int count = 0;
                String after = "";
                for (;;) {
                    canceled(monitor);
                    M3FS.DirectoryPage page = image.listPage(directory, after, 512);
                    M3DirectoryTable table = page.table();
                    for (int row = 0; row < table.size(); row++) {
                        if (count == result.length) result = Arrays.copyOf(result, Math.multiplyExact(count, 2));
                        String name = table.name(row);
                        if (table.kind(row) == M3FS.Kind.SYMLINK) {
                            FileInfo projected;
                            try { projected = info(image, M3Paths.child(directory, name), name); }
                            catch (NoSuchFileException removed) { projected = new FileInfo(name); }
                            result[count++] = projected;
                        } else result[count++] = project(name, table.kind(row), table.fileSize(row),
                                table.modifiedTimeNanos(row), table.mode(row), image.isReadOnly());
                    }
                    if (!page.hasMore()) return Arrays.copyOf(result, count);
                    if (table.size() == 0) throw new IOException("M3FS directory cursor made no progress");
                    after = page.nextAfter();
                }
            } catch (IOException problem) { throw io(EFS.ERROR_READ, problem); }
        }
    }

    @Override public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException {
        options(options, EFS.NONE);
        canceled(monitor);
        synchronized (mounts) {
            try {
                M3FS image = image();
                String actual = resolve(image, path, true);
                M3FS.Stat stat = image.stat(actual);
                if (stat.kind() != M3FS.Kind.FILE) throw failure(EFS.ERROR_WRONG_TYPE, "M3FS input is not a file", null);
                return new M3Streams.Input(mounts, slot, generation, actual, stat.inode());
            } catch (IOException problem) { throw io(EFS.ERROR_READ, problem); }
        }
    }

    @Override public OutputStream openOutputStream(int options, IProgressMonitor monitor) throws CoreException {
        options(options, EFS.APPEND);
        canceled(monitor);
        synchronized (mounts) {
            try {
                M3FS image = writable();
                String actual = parents(image, path);
                M3FS.Stat stat;
                try { stat = image.stat(actual); }
                catch (NoSuchFileException absent) {
                    try { image.createFile(actual, 0644); }
                    catch (FileAlreadyExistsException concurrentCreate) { /* Re-evaluate the existing path. */ }
                    stat = image.stat(actual);
                }
                // Resolve an existing final link outside the create-if-absent branch.
                if (stat.kind() == M3FS.Kind.SYMLINK) {
                    actual = resolve(image, actual, true);
                    stat = image.stat(actual);
                }
                if (stat.kind() != M3FS.Kind.FILE) throw failure(EFS.ERROR_WRONG_TYPE, "M3FS output is not a file", null);
                if ((stat.mode() & 0222) == 0) throw failure(EFS.ERROR_READ_ONLY, "M3FS file is read-only", null);
                boolean append = (options & EFS.APPEND) != 0;
                if (!append) image.writeIfInode(actual, stat.inode(), 0, EMPTY, 0, 0, M3FS.WRITE_TRUNCATE);
                return new M3Streams.Output(mounts, slot, generation, actual, stat.inode(), append);
            } catch (IOException problem) { throw io(EFS.ERROR_WRITE, problem); }
        }
    }

    @Override public IFileStore mkdir(int options, IProgressMonitor monitor) throws CoreException {
        options(options, EFS.SHALLOW);
        canceled(monitor);
        synchronized (mounts) {
            try {
                M3FS image = image();
                if (path.equals("/")) return this;
                try {
                    if (image.stat(resolve(image, path, true)).kind() == M3FS.Kind.DIRECTORY) return this;
                } catch (NoSuchFileException absent) {
                    // Creation is needed; write authorization follows the no-op check.
                }
                writable();
                if ((options & EFS.SHALLOW) != 0) {
                    String actual = parents(image, path);
                    try { image.mkdir(actual, 0755); }
                    catch (FileAlreadyExistsException present) {
                        if (image.stat(resolve(image, actual, true)).kind() != M3FS.Kind.DIRECTORY) throw present;
                    }
                } else {
                    String current = "/";
                    int start = 1;
                    for (int end = 1; end <= path.length(); end++) {
                        if (end != path.length() && path.charAt(end) != '/') continue;
                        canceled(monitor);
                        current = M3Paths.child(current, path.substring(start, end));
                        start = end + 1;
                        try { image.mkdir(current, 0755); }
                        catch (FileAlreadyExistsException present) {
                            current = resolve(image, current, true);
                            if (image.stat(current).kind() != M3FS.Kind.DIRECTORY) throw new NotDirectoryException(current);
                        }
                    }
                }
                return this;
            } catch (IOException problem) { throw io(EFS.ERROR_WRITE, problem); }
        }
    }

    @Override public void putInfo(IFileInfo info, int options, IProgressMonitor monitor) throws CoreException {
        Objects.requireNonNull(info, "info");
        options(options, EFS.SET_ATTRIBUTES | EFS.SET_LAST_MODIFIED);
        canceled(monitor);
        synchronized (mounts) {
            try {
                M3FS image = image();
                if (options == EFS.NONE) return;
                writable();
                String actual = resolve(image, path, true);
                M3FS.Stat stat = image.stat(actual);
                int mode = stat.mode();
                boolean attributes = (options & EFS.SET_ATTRIBUTES) != 0;
                boolean time = (options & EFS.SET_LAST_MODIFIED) != 0;
                if (attributes) {
                    mode = info.getAttribute(EFS.ATTRIBUTE_READ_ONLY) ? mode & ~0222 : mode | 0200;
                    mode = info.getAttribute(EFS.ATTRIBUTE_EXECUTABLE) ? mode | 0100 : mode & ~0111;
                }
                long nanos = stat.modifiedTimeNanos();
                if (time) {
                    if (info.getLastModified() < 0) throw new IOException("Negative M3FS modification time");
                    try { nanos = Math.multiplyExact(info.getLastModified(), 1_000_000L); }
                    catch (ArithmeticException overflow) { throw new IOException("M3FS modification time overflow", overflow); }
                }
                image.setMetadata(actual, mode, nanos, attributes, time);
            } catch (IOException problem) { throw io(EFS.ERROR_WRITE, problem); }
        }
    }

    @Override public void delete(int options, IProgressMonitor monitor) throws CoreException {
        options(options, EFS.NONE);
        canceled(monitor);
        if (path.equals("/")) throw failure(EFS.ERROR_DELETE, "Cannot delete the M3FS root", null);
        synchronized (mounts) {
            try {
                M3FS image = image();
                String actual = parents(image, path);
                image.stat(actual); // A missing target is a no-op, even on a read-only image.
                writable();
                String[] paths = new String[32];
                boolean[] visited = new boolean[32];
                int size = 1;
                paths[0] = actual;
                MultiStatus failures = null;
                while (size != 0) {
                    canceled(monitor);
                    String next = paths[--size];
                    boolean post = visited[size];
                    paths[size] = null;
                    try {
                        M3FS.Stat stat = image.stat(next);
                        if (stat.kind() != M3FS.Kind.DIRECTORY) image.unlink(next);
                        else if (post) image.rmdir(next);
                        else {
                            if (size == paths.length) {
                                paths = Arrays.copyOf(paths, Math.multiplyExact(size, 2));
                                visited = Arrays.copyOf(visited, paths.length);
                            }
                            paths[size] = next;
                            visited[size++] = true;
                            String after = "";
                            for (;;) {
                                //FLLDCIM: MARK — EFS_CANCEL m3fs-page-loop CONTRACT
                                canceled(monitor);
                                M3FS.DirectoryPage page = image.listPage(next, after, 512);
                                M3DirectoryTable table = page.table();
                                for (int row = 0; row < table.size(); row++) {
                                    if (size == paths.length) {
                                        paths = Arrays.copyOf(paths, Math.multiplyExact(size, 2));
                                        visited = Arrays.copyOf(visited, paths.length);
                                    }
                                    paths[size] = M3Paths.child(next, table.name(row));
                                    visited[size++] = false;
                                }
                                if (!page.hasMore()) break;
                                if (table.size() == 0) throw new IOException("M3FS directory cursor made no progress");
                                after = page.nextAfter();
                            }
                        }
                    } catch (NoSuchFileException absent) {
                        // Deletion of an already absent entry is complete.
                    } catch (IOException problem) {
                        if (failures == null) failures = new MultiStatus(M3EclipseFileSystem.PLUGIN_ID,
                                EFS.ERROR_DELETE, "M3FS recursive deletion failed", null);
                        failures.add(new Status(IStatus.ERROR, M3EclipseFileSystem.PLUGIN_ID, EFS.ERROR_DELETE,
                                next + ": " + problem.getMessage(), problem));
                    }
                }
                if (failures != null) throw new CoreException(failures);
            } catch (NoSuchFileException absent) {
                // A removed parent means the requested entry is absent too.
            } catch (IOException problem) { throw io(EFS.ERROR_DELETE, problem); }
        }
    }

    private static boolean sameView(M3FS left, M3FS right) throws IOException {
        return left == right || (!left.isSnapshot() && !right.isSnapshot()
                && Files.isSameFile(left.database(), right.database()));
    }

    private void guardCopy(IFileStore destination, M3FS sourceImage, String sourcePath,
                           M3FS.Stat sourceStat) throws IOException, CoreException {
        if (!(destination instanceof M3EclipseFileStore other)) return;
        synchronized (other.mounts) {
            M3FS targetImage = other.image();
            if (!sameView(sourceImage, targetImage)) return;
            String targetPath = parents(targetImage, other.path);
            M3FS.Stat target = null;
            try {
                targetPath = resolve(targetImage, targetPath, true);
                target = targetImage.stat(targetPath);
            } catch (NoSuchFileException missing) { /* Destination does not exist. */ }
            if (sourcePath.equals(targetPath)
                    || (sourceStat.kind() == M3FS.Kind.DIRECTORY && targetPath.startsWith(sourcePath.equals("/") ? "/" : sourcePath + "/"))
                    || (target != null && sourceStat.inode() == target.inode())) {
                throw failure(EFS.ERROR_WRITE, "Cannot copy an M3FS resource into itself or an inode alias", null);
            }
        }
    }

    @Override public void copy(IFileStore destination, int options, IProgressMonitor monitor) throws CoreException {
        Objects.requireNonNull(destination, "destination");
        options(options, EFS.OVERWRITE | EFS.SHALLOW);
        canceled(monitor);
        synchronized (COPY_LOCK) {
            CopyPath ancestry = COPY_PATH.get();
            boolean root = ancestry == null;
            if (root) { ancestry = new CopyPath(); COPY_PATH.set(ancestry); }
            boolean pushed = false;
            try {
                synchronized (mounts) {
                    M3FS sourceImage = image();
                    String sourcePath = resolve(sourceImage, path, true);
                    M3FS.Stat sourceStat = sourceImage.stat(sourcePath);
                    guardCopy(destination, sourceImage, sourcePath, sourceStat);
                    if (sourceStat.kind() == M3FS.Kind.DIRECTORY) {
                        ancestry.push(sourceImage, sourceStat.inode());
                        pushed = true;
                    }
                    super.copy(destination, options, monitor);
                }
            } catch (IOException problem) { throw io(EFS.ERROR_WRITE, problem); }
            finally {
                if (pushed) ancestry.pop();
                if (root) COPY_PATH.remove();
            }
        }
    }

    /** Retain inherited directory admission/attributes while reporting all independent child failures. */
    @Override protected void copyDirectory(IFileInfo sourceInfo, IFileStore destination,
                                           int options, IProgressMonitor monitor) throws CoreException {
        if ((options & EFS.SHALLOW) != 0) {
            super.copyDirectory(sourceInfo, destination, options, monitor);
            return;
        }
        IFileStore[] children = childStores(EFS.NONE, monitor);
        SubMonitor progress = SubMonitor.convert(monitor, children.length + 1);
        super.copyDirectory(sourceInfo, destination, options | EFS.SHALLOW, progress.newChild(1));
        MultiStatus failures = null;
        for (IFileStore child : children) {
            canceled(progress);
            try {
                child.copy(destination.getChild(child.getName()), options, progress.newChild(1));
            } catch (CoreException problem) {
                if (failures == null) failures = new MultiStatus(M3EclipseFileSystem.PLUGIN_ID,
                        EFS.ERROR_WRITE, "M3FS recursive copy failed", null);
                failures.add(problem.getStatus());
            }
        }
        if (failures != null) throw new CoreException(failures);
    }

    @Override public void move(IFileStore destination, int options, IProgressMonitor monitor) throws CoreException {
        Objects.requireNonNull(destination, "destination");
        options(options, EFS.OVERWRITE);
        canceled(monitor);
        synchronized (COPY_LOCK) {
            if (destination instanceof M3EclipseFileStore other && other.mounts == mounts
                    && other.slot == slot && other.generation == generation) {
                synchronized (mounts) {
                    try {
                        M3FS image = writable();
                        String from = parents(image, path);
                        String to = parents(image, other.path);
                        if (from.equals(to)) throw failure(EFS.ERROR_WRITE, "Cannot move M3FS resource to itself", null);
                        image.stat(from); // The final link is intentionally not followed for namespace moves.
                        if ((options & EFS.OVERWRITE) == 0) image.renameNoReplace(from, to);
                        else image.rename(from, to);
                        return;
                    } catch (IOException problem) { throw io(EFS.ERROR_WRITE, problem); }
                }
            }
            super.move(destination, options, monitor);
        }
    }

    /** Active recursive-copy ancestry only; removed from the thread on return. */
    private static final class CopyPath {
        private M3FS[] owners = new M3FS[16];
        private long[] inodes = new long[16];
        private int size;

        void push(M3FS image, long inode) throws IOException {
            for (int i = 0; i < size; i++) {
                if (inodes[i] == inode && sameView(owners[i], image)) throw new IOException("Cyclic M3FS directory copy");
            }
            if (size == inodes.length) {
                inodes = Arrays.copyOf(inodes, Math.multiplyExact(size, 2));
                owners = Arrays.copyOf(owners, inodes.length);
            }
            owners[size] = image;
            inodes[size++] = inode;
        }

        void pop() { owners[--size] = null; }
    }
}
