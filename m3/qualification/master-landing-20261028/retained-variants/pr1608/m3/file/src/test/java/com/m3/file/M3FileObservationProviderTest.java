// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardOpenOption;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Deterministic provider observations exercise mutation/refusal paths without timing races. */
class M3FileObservationProviderTest {
    @Test
    void originalProviderAndNoFollowOptionsAreUsedAndStreamIsClosed() throws Exception {
        Fixture provider = new Fixture(new byte[] {1, 2, 3});
        M3FileSnapshot snapshot = M3File.of(provider.path).snapshot(3);
        assertSame(provider.path, snapshot.path());
        assertEquals(1, provider.opens);
        assertEquals(1, provider.closes);
        assertEquals(2, provider.attributeReads);
        assertEquals(Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS), provider.options);
        assertTrue(provider.noFollowAttributes);
    }

    @Test
    void observedFileKeySizeModificationAndCreationChangesRefusePublication() {
        FileTime one = FileTime.fromMillis(1);
        FileTime two = FileTime.fromMillis(2);
        for (State changed : List.of(
                new State(3, one, one, "replacement"),
                new State(4, one, one, "original"),
                new State(3, two, one, "original"),
                new State(3, one, two, "original"))) {
            Fixture provider = new Fixture(new byte[] {1, 2, 3});
            provider.after = changed;
            assertThrows(IOException.class, () -> M3File.of(provider.path).snapshot(32));
            assertEquals(1, provider.closes);
        }
    }

    @Test
    void shortStreamAndGrowthBeyondTheActualBudgetAreRefused() {
        Fixture shortInput = new Fixture(new byte[] {1, 2});
        shortInput.before = shortInput.after = new State(3);
        assertThrows(IOException.class, () -> M3File.of(shortInput.path).snapshot(3));
        assertEquals(1, shortInput.closes);

        Fixture grownInput = new Fixture(new byte[] {1, 2, 3});
        grownInput.before = grownInput.after = new State(2);
        assertThrows(IOException.class, () -> M3File.of(grownInput.path).snapshot(2));
        assertEquals(3, grownInput.bytesRead);
        assertEquals(1, grownInput.closes);
    }

    @Test
    void disappearanceAfterReadingDoesNotBecomeAnAbsentObservation() {
        Fixture provider = new Fixture(new byte[] {1});
        var failure = new NoSuchFileException("removed after stream");
        provider.laterAttributeFailure = failure;
        assertSame(failure, assertThrows(NoSuchFileException.class,
                () -> M3File.of(provider.path).snapshot(1)));
        assertEquals(1, provider.closes);
    }

    @Test
    void permissionOpenReadAndCloseFailuresPropagate() {
        Fixture denied = new Fixture(new byte[] {1});
        denied.initialAttributeFailure = new AccessDeniedException("denied");
        assertSame(denied.initialAttributeFailure, assertThrows(AccessDeniedException.class,
                () -> M3File.of(denied.path).snapshot(1)));
        assertEquals(0, denied.opens);

        Fixture failedOpen = new Fixture(new byte[] {1});
        failedOpen.openFailure = new IOException("open failure");
        assertSame(failedOpen.openFailure, assertThrows(IOException.class,
                () -> M3File.of(failedOpen.path).snapshot(1)));

        Fixture failedRead = new Fixture(new byte[] {1});
        failedRead.readFailure = new IOException("read failure");
        assertSame(failedRead.readFailure, assertThrows(IOException.class,
                () -> M3File.of(failedRead.path).snapshot(1)));
        assertEquals(1, failedRead.closes);

        Fixture failedClose = new Fixture(new byte[] {1});
        failedClose.closeFailure = new IOException("close failure");
        assertSame(failedClose.closeFailure, assertThrows(IOException.class,
                () -> M3File.of(failedClose.path).snapshot(1)));
        assertEquals(1, failedClose.closes);
    }

    @Test
    void initialMissingObservationAndInvalidBudgetDoNotOpenAStream() throws Exception {
        Fixture absent = new Fixture(new byte[0]);
        absent.initialAttributeFailure = new NoSuchFileException("missing");
        assertFalse(M3File.of(absent.path).snapshot(0).exists());
        assertEquals(0, absent.opens);

        Fixture invalid = new Fixture(new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> M3File.of(invalid.path).snapshot(-1));
        assertEquals(0, invalid.attributeReads);
        assertEquals(0, invalid.opens);
    }

    @Test
    void separateFilesystemsWithTheSamePathSpellingCannotCompare() throws Exception {
        Fixture first = new Fixture(new byte[] {1});
        Fixture second = new Fixture(new byte[] {1});
        M3FileSnapshot before = M3File.of(first.path).snapshot(1);
        M3FileSnapshot after = M3File.of(second.path).snapshot(1);
        assertEquals(before.path().toString(), after.path().toString());
        assertThrows(IllegalArgumentException.class, () -> after.changeFrom(before));
    }

    @Test
    void longBudgetUsesBoundedReadRequestsAndSupportsAbsentProviderFileKeys() throws Exception {
        Fixture provider = new Fixture(new byte[24_577]);
        provider.before = provider.after = new State(provider.data.length,
                FileTime.fromMillis(1), FileTime.fromMillis(1), null);
        assertTrue(M3File.of(provider.path).snapshot(Long.MAX_VALUE).exists());
        assertEquals(provider.data.length, provider.bytesRead);
        assertTrue(provider.largestRequest <= 8192);
        assertEquals(1, provider.closes);
    }

    @Test
    void versionViewIsBracketingAndDoesNotReadPayload() throws Exception {
        Fixture provider = new Fixture(new byte[] {1, 2, 3});
        provider.versionBefore = provider.versionAfter = new M3FileVersionView.Version("store-epoch", "7");
        M3FileObservation first = M3File.of(provider.path).observe();
        assertEquals(2, provider.versionReads);
        assertEquals(2, provider.attributeReads);
        assertEquals(0, provider.opens);
        assertTrue(first.sameProviderVersion(first));
        assertEquals("m3version", provider.getFileAttributeView(provider.path, M3FileVersionView.class).name());
        provider.versionBefore = provider.versionAfter = new M3FileVersionView.Version("store-epoch", "8");
        M3FileObservation next = M3File.of(provider.path).observe();
        assertEquals(M3FileObservation.PROVIDER_VERSION, next.changesFrom(first));
        assertFalse(next.sameProviderVersion(first));
        provider.versionAfter = new M3FileVersionView.Version("new-epoch", "8");
        M3FileObservation remount = M3File.of(provider.path).observe();
        assertFalse(remount.sameProviderVersion(next));
        provider.versionBefore = provider.versionAfter = null;
        M3FileObservation unavailable = M3File.of(provider.path).observe();
        assertEquals(M3FileObservation.PROVIDER_VERSION, unavailable.changesFrom(next));
        assertFalse(unavailable.sameProviderVersion(unavailable));
    }

    @Test
    void changingVersionAndProviderErrorsRefuseObservation() {
        Fixture changed = new Fixture(new byte[] {1});
        changed.versionBefore = new M3FileVersionView.Version("store", "1");
        changed.versionAfter = new M3FileVersionView.Version("store", "2");
        assertThrows(IOException.class, () -> M3File.of(changed.path).observe());
        assertEquals(0, changed.opens);
        Fixture failed = new Fixture(new byte[] {1});
        failed.versionBefore = failed.versionAfter = changed.versionBefore;
        failed.versionFailure = new IOException("provider version failure");
        assertSame(failed.versionFailure, assertThrows(IOException.class, () -> M3File.of(failed.path).observe()));
        Fixture missing = new Fixture(new byte[0]);
        missing.initialAttributeFailure = new NoSuchFileException("initial absence");
        assertFalse(assertDoesNotThrowObserve(missing).exists());
        Fixture later = new Fixture(new byte[0]);
        later.versionBefore = later.versionAfter = changed.versionBefore;
        later.laterAttributeFailure = new NoSuchFileException("later disappearance");
        assertSame(later.laterAttributeFailure, assertThrows(IOException.class, () -> M3File.of(later.path).observe()));
    }

    private static M3FileObservation assertDoesNotThrowObserve(Fixture fixture) {
        return org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> M3File.of(fixture.path).observe());
    }

    @Test
    void hostileHashCollisionsPreserveProviderIdentityAndCapacity() {
        Fixture first = new Fixture(new byte[0]);
        Fixture other = new Fixture(new byte[0]);
        try (var scope = new M3FileScope(1025)) {
            var files = new M3File[1024];
            for (int i = 0; i < files.length; i++) files[i] = scope.file(first.filesystem.getPath("/" + i));
            for (int i = 0; i < files.length; i++) assertSame(files[i], scope.file(first.filesystem.getPath("/" + i)));
            org.junit.jupiter.api.Assertions.assertNotSame(files[0], scope.file(other.filesystem.getPath("/0")));
            assertEquals(1025, scope.size());
            assertThrows(IllegalStateException.class, () -> scope.file(first.filesystem.getPath("/more")));
        }
    }

    private record State(long size, FileTime lastModifiedTime, FileTime creationTime,
            Object fileKey) implements BasicFileAttributes {
        State(long size) {
            this(size, FileTime.fromMillis(1), FileTime.fromMillis(1), "original");
        }
        @Override public FileTime lastAccessTime() { return FileTime.fromMillis(1); }
        @Override public boolean isRegularFile() { return true; }
        @Override public boolean isDirectory() { return false; }
        @Override public boolean isSymbolicLink() { return false; }
        @Override public boolean isOther() { return false; }
    }

    private static final class Fixture extends FileSystemProvider {
        private final byte[] data;
        private final FixtureFileSystem filesystem = new FixtureFileSystem(this);
        private final Path path = new FixturePath(filesystem, Path.of("/input"));
        private State before;
        private State after;
        private IOException initialAttributeFailure;
        private IOException laterAttributeFailure;
        private IOException openFailure;
        private IOException readFailure;
        private IOException closeFailure;
        private Set<? extends OpenOption> options;
        private M3FileVersionView.Version versionBefore;
        private M3FileVersionView.Version versionAfter;
        private IOException versionFailure;
        private int versionReads;
        private int attributeReads;
        private int opens;
        private int closes;
        private int bytesRead;
        private int largestRequest;
        private boolean noFollowAttributes = true;

        Fixture(byte[] data) {
            this.data = data;
            before = after = new State(data.length);
        }

        @Override public String getScheme() { return "m3-snapshot-test"; }
        @Override public FileSystem newFileSystem(URI uri, Map<String, ?> env) { throw unused(); }
        @Override public FileSystem getFileSystem(URI uri) { return filesystem; }
        @Override public Path getPath(URI uri) { return path; }
        @Override public SeekableByteChannel newByteChannel(Path value,
                Set<? extends OpenOption> supplied, FileAttribute<?>... attrs) throws IOException {
            assertSame(path, value);
            options = Set.copyOf(supplied);
            opens++;
            if (openFailure != null) throw openFailure;
            return new MemoryChannel(this);
        }
        @Override public <A extends BasicFileAttributes> A readAttributes(Path value,
                Class<A> type, LinkOption... supplied) throws IOException {
            assertSame(path, value);
            noFollowAttributes &= Arrays.asList(supplied).contains(LinkOption.NOFOLLOW_LINKS);
            attributeReads++;
            IOException failure = attributeReads == 1 ? initialAttributeFailure : laterAttributeFailure;
            if (failure != null) throw failure;
            return type.cast(attributeReads == 1 ? before : after);
        }
        @Override public DirectoryStream<Path> newDirectoryStream(Path dir,
                DirectoryStream.Filter<? super Path> filter) { throw unused(); }
        @Override public void createDirectory(Path dir, FileAttribute<?>... attrs) { throw unused(); }
        @Override public void delete(Path value) { throw unused(); }
        @Override public void copy(Path from, Path to, CopyOption... options) { throw unused(); }
        @Override public void move(Path from, Path to, CopyOption... options) { throw unused(); }
        @Override public boolean isSameFile(Path first, Path second) { throw unused(); }
        @Override public boolean isHidden(Path value) { throw unused(); }
        @Override public FileStore getFileStore(Path value) { throw unused(); }
        @Override public void checkAccess(Path value, AccessMode... modes) { throw unused(); }
        @Override public <V extends FileAttributeView> V getFileAttributeView(Path value,
                Class<V> type, LinkOption... options) {
            if (type != M3FileVersionView.class || versionBefore == null) return null;
            return type.cast(new M3FileVersionView() {
                @Override public Version readVersion() throws IOException {
                    if (versionFailure != null) throw versionFailure;
                    return ++versionReads == 1 ? versionBefore : versionAfter;
                }
            });
        }
        @Override public Map<String, Object> readAttributes(Path value, String attrs,
                LinkOption... options) { throw unused(); }
        @Override public void setAttribute(Path value, String attribute, Object item,
                LinkOption... options) { throw unused(); }
    }

    private static final class MemoryChannel implements SeekableByteChannel {
        private final Fixture fixture;
        private int position;
        private boolean open = true;

        MemoryChannel(Fixture fixture) { this.fixture = fixture; }
        @Override public int read(ByteBuffer destination) throws IOException {
            if (!open) throw new ClosedChannelException();
            if (fixture.readFailure != null) throw fixture.readFailure;
            fixture.largestRequest = Math.max(fixture.largestRequest, destination.remaining());
            if (!destination.hasRemaining()) return 0;
            if (position == fixture.data.length) return -1;
            int count = Math.min(destination.remaining(), fixture.data.length - position);
            destination.put(fixture.data, position, count);
            fixture.bytesRead += count;
            position += count;
            return count;
        }
        @Override public int write(ByteBuffer source) { throw unused(); }
        @Override public long position() { return position; }
        @Override public SeekableByteChannel position(long value) {
            if (value < 0 || value > fixture.data.length) throw new IllegalArgumentException();
            position = (int) value;
            return this;
        }
        @Override public long size() { return fixture.data.length; }
        @Override public SeekableByteChannel truncate(long size) { throw unused(); }
        @Override public boolean isOpen() { return open; }
        @Override public void close() throws IOException {
            open = false;
            fixture.closes++;
            if (fixture.closeFailure != null) throw fixture.closeFailure;
        }
    }

    private static final class FixtureFileSystem extends FileSystem {
        private final Fixture fixture;

        FixtureFileSystem(Fixture fixture) { this.fixture = fixture; }
        @Override public FileSystemProvider provider() { return fixture; }
        @Override public void close() { }
        @Override public boolean isOpen() { return true; }
        @Override public boolean isReadOnly() { return true; }
        @Override public String getSeparator() { return "/"; }
        @Override public Iterable<Path> getRootDirectories() { return List.of(getPath("/")); }
        @Override public Iterable<FileStore> getFileStores() { return List.of(); }
        @Override public Set<String> supportedFileAttributeViews() { return Set.of("basic"); }
        @Override public Path getPath(String first, String... more) {
            return new FixturePath(this, Path.of(first, more));
        }
        @Override public PathMatcher getPathMatcher(String syntaxAndPattern) { throw unused(); }
        @Override public UserPrincipalLookupService getUserPrincipalLookupService() { throw unused(); }
        @Override public WatchService newWatchService() { throw unused(); }
    }

    private record FixturePath(FixtureFileSystem getFileSystem, Path delegate) implements Path {
        private FixturePath wrap(Path value) {
            return value == null ? null : new FixturePath(getFileSystem, value);
        }
        private Path unwrap(Path value) {
            if (!(value instanceof FixturePath item) || getFileSystem != item.getFileSystem) {
                throw new IllegalArgumentException("different fixture filesystem");
            }
            return item.delegate;
        }
        @Override public boolean isAbsolute() { return delegate.isAbsolute(); }
        @Override public Path getRoot() { return wrap(delegate.getRoot()); }
        @Override public Path getFileName() { return wrap(delegate.getFileName()); }
        @Override public Path getParent() { return wrap(delegate.getParent()); }
        @Override public int getNameCount() { return delegate.getNameCount(); }
        @Override public Path getName(int index) { return wrap(delegate.getName(index)); }
        @Override public Path subpath(int begin, int end) { return wrap(delegate.subpath(begin, end)); }
        @Override public boolean startsWith(Path other) { return delegate.startsWith(unwrap(other)); }
        @Override public boolean endsWith(Path other) { return delegate.endsWith(unwrap(other)); }
        @Override public Path normalize() { return wrap(delegate.normalize()); }
        @Override public Path resolve(Path other) { return wrap(delegate.resolve(unwrap(other))); }
        @Override public Path relativize(Path other) { return wrap(delegate.relativize(unwrap(other))); }
        @Override public URI toUri() { return URI.create("m3-snapshot-test:" + delegate); }
        @Override public Path toAbsolutePath() { return wrap(delegate.toAbsolutePath()); }
        @Override public Path toRealPath(LinkOption... options) { throw unused(); }
        @Override public File toFile() { throw unused(); }
        @Override public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events,
                WatchEvent.Modifier... modifiers) { throw unused(); }
        @Override public Iterator<Path> iterator() {
            var names = new java.util.ArrayList<Path>();
            for (Path name : delegate) names.add(wrap(name));
            return names.iterator();
        }
        @Override public int compareTo(Path other) { return delegate.compareTo(unwrap(other)); }
        @Override public int hashCode() { return 1; }
        @Override public String toString() { return delegate.toString(); }
    }

    private static UnsupportedOperationException unused() {
        return new UnsupportedOperationException("unused test-provider operation");
    }
}
