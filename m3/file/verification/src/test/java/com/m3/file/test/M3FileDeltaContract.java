// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file.test;

import com.m3.file.M3File;

import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardOpenOption;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

/**
 * Independent public-contract checks for bounded M3File content fingerprints.
 *
 * <p>This is a standalone Java 21 program, with no test-framework dependency.
 * SHA-256 expectations come from MessageDigest over fixture bytes. The compiler
 * fixture uses real javac but does not claim Maven/Gradle planner integration,
 * parse-cache integration, ABI analysis, or collision-free content equality.</p>
 */
public final class M3FileDeltaContract {
    private static int checks;

    public static void main(String[] args) throws Exception {
        Path temporary = Files.createTempDirectory("m3file-delta-contract-");
        try {
            recordAndDeltaContract();
            digestVectors(temporary.resolve("vectors"));
            byteBounds(temporary.resolve("bounds"));
            scriptedProviderObservations();
            pathsTimestampsAndChanges(temporary.resolve("observations"));
            refusalAndAbsence(temporary.resolve("refusal"));
            compilerBinaryFixture(temporary.resolve("compiler"));
            System.out.println("M3FileDeltaContract: PASS; checks=" + checks
                    + "; runtime=" + Runtime.version()
                    + "; javac fixture uses unchanged binary dependency without its source");
        } finally {
            deleteTree(temporary);
        }
    }

    private static void recordAndDeltaContract() throws Exception {
        equal(1, M3File.ADDED, "ADDED flag");
        equal(2, M3File.REMOVED, "REMOVED flag");
        equal(4, M3File.CONTENT_CHANGED, "CONTENT_CHANGED flag");
        expect(IllegalArgumentException.class,
                () -> new M3File.Fingerprint(-1, 0, 0, 0, 0), "negative record size");
        M3File.Fingerprint before = new M3File.Fingerprint(17, 1, 2, 3, 4);
        M3File.Fingerprint same = new M3File.Fingerprint(17, 1, 2, 3, 4);
        equal(before, same, "record value equality");
        equal(before.hashCode(), same.hashCode(), "record equal hash codes");
        equal(17L, before.size(), "record byte size");
        equal(1L, before.digest0(), "digest limb zero");
        equal(2L, before.digest1(), "digest limb one");
        equal(3L, before.digest2(), "digest limb two");
        equal(4L, before.digest3(), "digest limb three");
        M3File.Fingerprint fullRange = new M3File.Fingerprint(
                Long.MAX_VALUE, Long.MIN_VALUE, -1, 0, Long.MAX_VALUE);
        equal(Long.MAX_VALUE, fullRange.size(), "record supports long size");
        equal(Long.MIN_VALUE, fullRange.digest0(), "digest retains high sign bit");
        equal(-1L, fullRange.digest1(), "digest retains all one bits");
        equal(0, M3File.contentDelta(null, null), "two known absences");
        equal(M3File.ADDED, M3File.contentDelta(null, before), "known absence to present");
        equal(M3File.REMOVED, M3File.contentDelta(before, null), "present to known absence");
        equal(0, M3File.contentDelta(before, before), "same record instance");
        equal(0, M3File.contentDelta(before, same), "same fingerprint tuple");
        List<M3File.Fingerprint> different = List.of(
                new M3File.Fingerprint(18, 1, 2, 3, 4),
                new M3File.Fingerprint(17, 9, 2, 3, 4),
                new M3File.Fingerprint(17, 1, 9, 3, 4),
                new M3File.Fingerprint(17, 1, 2, 9, 4),
                new M3File.Fingerprint(17, 1, 2, 3, 9));
        for (M3File.Fingerprint changed : different) {
            equal(M3File.CONTENT_CHANGED, M3File.contentDelta(before, changed),
                    "changed size or digest limb");
            equal(M3File.CONTENT_CHANGED, M3File.contentDelta(changed, before),
                    "content change is symmetric");
        }
    }

    private static void digestVectors(Path directory) throws Exception {
        Files.createDirectories(directory);
        byte[] everyByte = new byte[256];
        for (int i = 0; i < everyByte.length; i++) everyByte[i] = (byte) i;
        List<byte[]> vectors = new ArrayList<>();
        vectors.add(new byte[0]);
        vectors.add("abc".getBytes(StandardCharsets.US_ASCII));
        vectors.add(everyByte);
        for (int length : new int[] {8191, 8192, 8193, 16384, 32791}) {
            byte[] bytes = new byte[length];
            for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) (i * 131 + i / 7);
            vectors.add(bytes);
        }
        for (int i = 0; i < vectors.size(); i++) {
            byte[] bytes = vectors.get(i);
            Path path = directory.resolve("vector-" + i + ".bin");
            Files.write(path, bytes);
            M3File.Fingerprint expected = oracle(bytes);
            equal(expected, M3File.of(path).fingerprint(bytes.length),
                    "independent SHA-256 oracle for vector " + i);
            equal(expected, M3File.of(path).fingerprint(Long.MAX_VALUE),
                    "unrestricted long limit for vector " + i);
            equal((long) bytes.length, expected.size(), "fingerprint counts raw bytes");
        }
    }

    private static void byteBounds(Path directory) throws Exception {
        Files.createDirectories(directory);
        Path empty = Files.write(directory.resolve("empty.bin"), new byte[0]);
        Path small = Files.write(directory.resolve("small.bin"), new byte[] {3, 7, 11});
        M3File emptyFile = M3File.of(empty);
        M3File smallFile = M3File.of(small);
        expect(IllegalArgumentException.class, () -> emptyFile.fingerprint(-1),
                "negative limit on empty file");
        expect(IllegalArgumentException.class, () -> smallFile.fingerprint(Long.MIN_VALUE),
                "minimum long limit");
        equal(oracle(new byte[0]), emptyFile.fingerprint(0), "empty file at zero limit");
        equal(oracle(new byte[] {3, 7, 11}), smallFile.fingerprint(3), "exact byte limit");
        equal(oracle(new byte[] {3, 7, 11}), smallFile.fingerprint(4), "larger byte limit");
        expect(IOException.class, () -> smallFile.fingerprint(2), "one byte over limit");
        expect(IOException.class, () -> smallFile.fingerprint(0), "nonempty file at zero limit");
        equal(oracle(new byte[] {3, 7, 11}), smallFile.fingerprint(Long.MAX_VALUE),
                "Long.MAX_VALUE does not overflow the bounded read");
        Files.delete(small);
        expect(NoSuchFileException.class, () -> smallFile.fingerprint(3),
                "same M3File observes deletion instead of retaining prior content");
    }

    private static void pathsTimestampsAndChanges(Path directory) throws Exception {
        Files.createDirectories(directory);
        byte[] original = "class Example { int value = 1; }\n".getBytes(StandardCharsets.UTF_8);
        Path first = Files.write(directory.resolve("first.java"), original);
        Path second = Files.write(directory.resolve("second.java"), original);
        M3File file = M3File.of(first);
        M3File.Fingerprint before = file.fingerprint(Long.MAX_VALUE);
        equal(before, M3File.of(second).fingerprint(Long.MAX_VALUE),
                "path is absent from the content fingerprint");
        FileTime oldTime = FileTime.fromMillis(1_600_000_000_000L);
        FileTime newTime = FileTime.fromMillis(1_650_000_000_000L);
        Files.setLastModifiedTime(first, oldTime);
        equal(0, M3File.contentDelta(before, file.fingerprint(Long.MAX_VALUE)),
                "timestamp alone does not change fingerprint");
        Files.setLastModifiedTime(first, newTime);
        equal(0, M3File.contentDelta(before, file.fingerprint(Long.MAX_VALUE)),
                "another timestamp alone remains unchanged");
        byte[] changed = "class Example { int value = 2; }\n".getBytes(StandardCharsets.UTF_8);
        equal(original.length, changed.length, "changed fixture preserves byte length");
        Files.write(first, changed);
        Files.setLastModifiedTime(first, oldTime);
        M3File.Fingerprint after = file.fingerprint(Long.MAX_VALUE);
        equal(oracle(changed), after, "changed bytes observed through existing handle");
        equal(M3File.CONTENT_CHANGED, M3File.contentDelta(before, after),
                "same length and restored timestamp cannot conceal changed content");
        Files.write(first, original);
        equal(0, M3File.contentDelta(before, file.fingerprint(Long.MAX_VALUE)),
                "restored content restores original fingerprint");
    }

    /** Scripts stream and metadata behavior without timing-sensitive file races. */
    private static void scriptedProviderObservations() throws Exception {
        ScriptedProvider exact = new ScriptedProvider(new byte[] {1, 2, 3}, 3);
        equal(oracle(new byte[] {1, 2, 3}), exact.file().fingerprint(3),
                "scripted regular-file observation");
        equal(3L, exact.delivered, "successful stream supplies exact byte count");
        assertClosed(exact, "successful observation");

        ScriptedProvider growing = new ScriptedProvider(new byte[64], 3);
        expect(IOException.class, () -> growing.file().fingerprint(3),
                "stream growth exceeds budget despite passing metadata precheck");
        equal(4L, growing.delivered, "overflow reads at most budget plus one probe byte");
        assertClosed(growing, "growth refusal");

        ScriptedProvider growsFromEmpty = new ScriptedProvider(new byte[] {1, 2}, 0);
        expect(IOException.class, () -> growsFromEmpty.file().fingerprint(0),
                "zero-budget observation refuses content hidden from metadata");
        equal(1L, growsFromEmpty.delivered, "zero-budget overflow reads one probe byte");
        assertClosed(growsFromEmpty, "zero-budget growth refusal");

        ScriptedProvider failing = new ScriptedProvider(new byte[8], 8);
        failing.failAfter = 2;
        expect(IOException.class, () -> failing.file().fingerprint(8),
                "midstream read failure propagates");
        equal(2L, failing.delivered, "scripted failure occurs after a partial stream");
        assertClosed(failing, "partial-read failure");

        ScriptedProvider changedTime = new ScriptedProvider(new byte[] {4, 5, 6}, 3);
        changedTime.changeTimeAfterRead = true;
        expect(IOException.class, () -> changedTime.file().fingerprint(3),
                "post-read modification-time change is refused");
        assertClosed(changedTime, "modification-time change");

        ScriptedProvider changedKey = new ScriptedProvider(new byte[] {4, 5, 6}, 3);
        changedKey.changeKeyAfterRead = true;
        expect(IOException.class, () -> changedKey.file().fingerprint(3),
                "post-read file-key replacement is refused");
        assertClosed(changedKey, "file-key replacement");

        ScriptedProvider shorter = new ScriptedProvider(new byte[] {1, 2}, 3);
        expect(IOException.class, () -> shorter.file().fingerprint(3),
                "observed stream shorter than declared metadata is refused");
        equal(2L, shorter.delivered, "short stream is observed through EOF");
        assertClosed(shorter, "stream-size disagreement");
    }

    private static void assertClosed(ScriptedProvider provider, String description) {
        equal(1, provider.opens, description + " opens one channel");
        equal(1, provider.closes, description + " closes its channel exactly once");
        check(!provider.channelOpen, description + " leaves no open channel");
        check(provider.noFollowMetadata && provider.noFollowOpen,
                description + " retains NOFOLLOW_LINKS at the provider boundary");
    }

    private static void refusalAndAbsence(Path directory) throws Exception {
        Files.createDirectories(directory);
        Path missing = directory.resolve("missing.bin");
        expect(NoSuchFileException.class, () -> M3File.of(missing).fingerprint(1024),
                "missing read propagates I/O failure");
        expect(IOException.class, () -> M3File.of(directory).fingerprint(Long.MAX_VALUE),
                "directory cannot be fingerprinted as a regular file");
        Path target = Files.write(directory.resolve("target.bin"), new byte[] {42});
        Path link = directory.resolve("link.bin");
        Files.createSymbolicLink(link, target.getFileName());
        expect(IOException.class, () -> M3File.of(link).fingerprint(Long.MAX_VALUE),
                "symbolic link to regular file is not followed");
        equal(oracle(new byte[] {42}), M3File.of(target).fingerprint(1),
                "link refusal leaves target unchanged");
        Path dangling = directory.resolve("dangling.bin");
        Files.createSymbolicLink(dangling, missing.getFileName());
        expect(IOException.class, () -> M3File.of(dangling).fingerprint(Long.MAX_VALUE),
                "dangling symbolic link is refused");
        M3File.Fingerprint present = M3File.of(target).fingerprint(1);
        Files.delete(target);
        check(Files.notExists(target), "caller independently observes absence");
        equal(M3File.REMOVED, M3File.contentDelta(present, null),
                "caller supplies known-absent observation explicitly");
        expect(NoSuchFileException.class, () -> M3File.of(target).fingerprint(1),
                "read failure is not converted into a null fingerprint");
    }

    private static void compilerBinaryFixture(Path directory) throws Exception {
        Path binaries = Files.createDirectories(directory.resolve("binaries"));
        Path consumers = Files.createDirectories(directory.resolve("consumers"));
        Path emptySources = Files.createDirectories(directory.resolve("empty-sources"));
        Source initialB = new Source("fixture.B", bSource(1, 7));
        compile(initialB, binaries, List.of(), emptySources);
        check(initialB.reads > 0, "initial B source is read by javac");
        Path binaryB = binaries.resolve("fixture/B.class");
        M3File.Fingerprint beforeB = M3File.of(binaryB).fingerprint(Long.MAX_VALUE);
        int beforeReads = initialB.reads;
        Source sourceA = new Source("fixture.A", "package fixture; public class A { "
                + "public static int evaluate() { return new B().value() + B.CODE; } }");
        List<URI> parses = compile(sourceA, consumers, List.of(binaries), emptySources);
        equal(beforeReads, initialB.reads, "compiling A performs no additional B source read");
        equal(List.of(sourceA.toUri()), parses, "A task parses only its supplied source");
        equal(beforeB, M3File.of(binaryB).fingerprint(Long.MAX_VALUE),
                "compiling A preserves B.class bytes");
        equal(0, M3File.contentDelta(beforeB,
                M3File.of(binaryB).fingerprint(Long.MAX_VALUE)), "binary dependency delta is zero");
        equal(8, evaluate(consumers, binaries), "initial compiled behavior");
        Path binaryA = consumers.resolve("fixture/A.class");
        M3File.Fingerprint beforeA = M3File.of(binaryA).fingerprint(Long.MAX_VALUE);

        Path editedConsumers = Files.createDirectories(directory.resolve("edited-consumers"));
        Source editedA = new Source("fixture.A", "package fixture; public class A { "
                + "public static int evaluate() { return new B().value() + B.CODE + 100; } }");
        List<URI> editedParses = compile(editedA, editedConsumers, List.of(binaries), emptySources);
        equal(List.of(editedA.toUri()), editedParses,
                "edited A generation parses only its supplied source");
        equal(beforeReads, initialB.reads,
                "edited A generation performs no additional B source read");
        equal(beforeB, M3File.of(binaryB).fingerprint(Long.MAX_VALUE),
                "edited A generation preserves unchanged B.class fingerprint");
        equal(108, evaluate(editedConsumers, binaries),
                "edited A generation executes changed behavior against unchanged B.class");
        equal(beforeA, M3File.of(binaryA).fingerprint(Long.MAX_VALUE),
                "separate edited output preserves original A for dependency-change witnesses");

        compile(new Source("fixture.B", bSource(2, 7)), binaries, List.of(), emptySources);
        M3File.Fingerprint bodyB = M3File.of(binaryB).fingerprint(Long.MAX_VALUE);
        equal(M3File.CONTENT_CHANGED, M3File.contentDelta(beforeB, bodyB),
                "body change changes class content without claiming an ABI classification");
        equal(beforeA, M3File.of(binaryA).fingerprint(Long.MAX_VALUE),
                "consumer artifact remains available after producer body change");
        equal(9, evaluate(consumers, binaries),
                "unchanged consumer executes new dependency body");

        compile(new Source("fixture.B", bSource(2, 11)), binaries, List.of(), emptySources);
        M3File.Fingerprint constantB = M3File.of(binaryB).fingerprint(Long.MAX_VALUE);
        equal(M3File.CONTENT_CHANGED, M3File.contentDelta(bodyB, constantB),
                "constant change also reports raw content change");
        equal(9, evaluate(consumers, binaries),
                "unrecompiled consumer still contains the old inlined constant");
        compile(sourceA, consumers, List.of(binaries), emptySources);
        equal(13, evaluate(consumers, binaries),
                "consumer recompilation observes changed constant");
        equal(M3File.CONTENT_CHANGED,
                M3File.contentDelta(beforeA, M3File.of(binaryA).fingerprint(Long.MAX_VALUE)),
                "constant use demonstrates why content equality is not a rebuild planner");
    }

    private static String bSource(int value, int constant) {
        return "package fixture; public class B { public static final int CODE = "
                + constant + "; public int value() { return " + value + "; } }";
    }

    private static List<URI> compile(Source source, Path output, List<Path> classpath,
            Path emptySources) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        check(compiler != null, "JDK compiler is available");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StringWriter compilerText = new StringWriter();
        List<URI> parses = new ArrayList<>();
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(
                diagnostics, java.util.Locale.ROOT, StandardCharsets.UTF_8)) {
            manager.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, List.of(output));
            manager.setLocationFromPaths(StandardLocation.CLASS_PATH, classpath);
            manager.setLocationFromPaths(StandardLocation.SOURCE_PATH, List.of(emptySources));
            JavaCompiler.CompilationTask task = compiler.getTask(compilerText, manager,
                    diagnostics, List.of("--release", "21", "-proc:none", "-implicit:none", "-g:none"),
                    null, List.of(source));
            check(task instanceof JavacTask, "compiler exposes parse events");
            ((JavacTask) task).addTaskListener(new TaskListener() {
                @Override public void started(TaskEvent event) { }
                @Override public void finished(TaskEvent event) {
                    if (event.getKind() == TaskEvent.Kind.PARSE) {
                        parses.add(event.getSourceFile().toUri());
                    }
                }
            });
            check(Boolean.TRUE.equals(task.call()), "javac compiles " + source.toUri()
                    + ": " + compilerText + diagnostics.getDiagnostics());
        }
        return List.copyOf(parses);
    }

    private static int evaluate(Path consumers, Path binaries) throws Exception {
        try (URLClassLoader loader = new URLClassLoader(
                new java.net.URL[] {consumers.toUri().toURL(), binaries.toUri().toURL()},
                ClassLoader.getPlatformClassLoader())) {
            return (Integer) Class.forName("fixture.A", true, loader)
                    .getMethod("evaluate").invoke(null);
        }
    }

    private static M3File.Fingerprint oracle(byte[] bytes) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        ByteBuffer words = ByteBuffer.wrap(digest).order(ByteOrder.BIG_ENDIAN);
        return new M3File.Fingerprint(bytes.length,
                words.getLong(), words.getLong(), words.getLong(), words.getLong());
    }

    private static void check(boolean condition, String description) {
        checks++;
        if (!condition) throw new AssertionError(description);
    }

    private static void equal(Object expected, Object actual, String description) {
        check(java.util.Objects.equals(expected, actual),
                description + "; expected=" + expected + ", actual=" + actual);
    }

    private static <T extends Throwable> void expect(Class<T> type,
            CheckedAction action, String description) throws Exception {
        checks++;
        try {
            action.run();
        } catch (Throwable thrown) {
            if (type.isInstance(thrown)) return;
            throw new AssertionError(description + "; expected " + type.getName()
                    + ", observed " + thrown, thrown);
        }
        throw new AssertionError(description + "; expected " + type.getName());
    }

    private static void deleteTree(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    @FunctionalInterface private interface CheckedAction { void run() throws Exception; }

    /** Minimal read-only provider whose observations are controlled by each test. */
    private static final class ScriptedProvider extends FileSystemProvider {
        private final byte[] bytes;
        private final long reportedSize;
        private final ScriptedFileSystem fileSystem = new ScriptedFileSystem(this);
        private int failAfter = -1;
        private boolean changeTimeAfterRead;
        private boolean changeKeyAfterRead;
        private long delivered;
        private int opens;
        private int closes;
        private boolean channelOpen;
        private boolean noFollowMetadata = true;
        private boolean noFollowOpen;

        ScriptedProvider(byte[] bytes, long reportedSize) {
            this.bytes = bytes.clone();
            this.reportedSize = reportedSize;
        }

        M3File file() { return M3File.of(fileSystem.getPath("/observation.bin")); }
        @Override public String getScheme() { return "m3file-test"; }
        @Override public FileSystem newFileSystem(URI uri, Map<String, ?> environment) {
            throw unsupported();
        }
        @Override public FileSystem getFileSystem(URI uri) { return fileSystem; }
        @Override public Path getPath(URI uri) { return fileSystem.getPath(uri.getPath()); }

        @Override public SeekableByteChannel newByteChannel(Path path,
                Set<? extends OpenOption> options, FileAttribute<?>... attributes) {
            noFollowOpen = options.contains(LinkOption.NOFOLLOW_LINKS)
                    && options.contains(StandardOpenOption.READ);
            opens++;
            channelOpen = true;
            return new SeekableByteChannel() {
                private int position;
                @Override public int read(ByteBuffer destination) throws IOException {
                    if (!channelOpen) throw new ClosedChannelException();
                    if (failAfter >= 0 && position >= failAfter) {
                        throw new IOException("scripted failure after partial stream");
                    }
                    if (!destination.hasRemaining()) return 0;
                    if (position == bytes.length) return -1;
                    int length = Math.min(destination.remaining(), bytes.length - position);
                    if (failAfter >= 0) length = Math.min(length, failAfter - position);
                    destination.put(bytes, position, length);
                    position += length;
                    delivered += length;
                    return length;
                }
                @Override public int write(ByteBuffer source) { throw unsupported(); }
                @Override public long position() { return position; }
                @Override public SeekableByteChannel position(long value) { throw unsupported(); }
                @Override public long size() { return bytes.length; }
                @Override public SeekableByteChannel truncate(long value) { throw unsupported(); }
                @Override public boolean isOpen() { return channelOpen; }
                @Override public void close() {
                    if (channelOpen) { channelOpen = false; closes++; }
                }
            };
        }

        @Override public <A extends BasicFileAttributes> A readAttributes(
                Path path, Class<A> type, LinkOption... options) {
            noFollowMetadata &= java.util.Arrays.asList(options).contains(LinkOption.NOFOLLOW_LINKS);
            boolean afterRead = delivered > 0;
            FileTime time = FileTime.fromMillis(changeTimeAfterRead && afterRead ? 2 : 1);
            Object key = changeKeyAfterRead && afterRead ? "replacement" : "original";
            return type.cast(new ScriptedAttributes(reportedSize, time, key));
        }
        @Override public Map<String, Object> readAttributes(
                Path path, String attributes, LinkOption... options) { throw unsupported(); }
        @Override public <V extends FileAttributeView> V getFileAttributeView(
                Path path, Class<V> type, LinkOption... options) { return null; }
        @Override public DirectoryStream<Path> newDirectoryStream(
                Path path, DirectoryStream.Filter<? super Path> filter) { throw unsupported(); }
        @Override public void createDirectory(Path path, FileAttribute<?>... attributes) {
            throw unsupported();
        }
        @Override public void delete(Path path) { throw unsupported(); }
        @Override public void copy(Path from, Path to, CopyOption... options) { throw unsupported(); }
        @Override public void move(Path from, Path to, CopyOption... options) { throw unsupported(); }
        @Override public boolean isSameFile(Path first, Path second) { return first.equals(second); }
        @Override public boolean isHidden(Path path) { return false; }
        @Override public FileStore getFileStore(Path path) { throw unsupported(); }
        @Override public void checkAccess(Path path, AccessMode... modes) { throw unsupported(); }
        @Override public void setAttribute(Path path, String attribute, Object value,
                LinkOption... options) { throw unsupported(); }
    }

    private record ScriptedAttributes(long size, FileTime lastModifiedTime, Object fileKey)
            implements BasicFileAttributes {
        @Override public FileTime lastAccessTime() { return lastModifiedTime; }
        @Override public FileTime creationTime() { return lastModifiedTime; }
        @Override public boolean isRegularFile() { return true; }
        @Override public boolean isDirectory() { return false; }
        @Override public boolean isSymbolicLink() { return false; }
        @Override public boolean isOther() { return false; }
    }

    private static final class ScriptedFileSystem extends FileSystem {
        private final FileSystemProvider provider;
        private boolean open = true;
        ScriptedFileSystem(FileSystemProvider provider) { this.provider = provider; }
        @Override public FileSystemProvider provider() { return provider; }
        @Override public void close() { open = false; }
        @Override public boolean isOpen() { return open; }
        @Override public boolean isReadOnly() { return true; }
        @Override public String getSeparator() { return "/"; }
        @Override public Iterable<Path> getRootDirectories() { return List.of(); }
        @Override public Iterable<FileStore> getFileStores() { return List.of(); }
        @Override public Set<String> supportedFileAttributeViews() { return Set.of("basic"); }
        @Override public Path getPath(String first, String... more) {
            Path delegate = Path.of(first, more);
            // Path is an interface; only filesystem dispatch differs for this fixture.
            return (Path) java.lang.reflect.Proxy.newProxyInstance(Path.class.getClassLoader(),
                    new Class<?>[] {Path.class}, (proxy, method, arguments) -> {
                        if (method.getName().equals("getFileSystem")) return this;
                        if (method.getName().equals("equals")) return proxy == arguments[0];
                        if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                        try {
                            return method.invoke(delegate, arguments);
                        } catch (java.lang.reflect.InvocationTargetException failure) {
                            throw failure.getCause();
                        }
                    });
        }
        @Override public PathMatcher getPathMatcher(String syntaxAndPattern) { throw unsupported(); }
        @Override public UserPrincipalLookupService getUserPrincipalLookupService() {
            throw unsupported();
        }
        @Override public WatchService newWatchService() { throw unsupported(); }
    }

    private static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("outside scripted observation fixture");
    }

    private static final class Source extends SimpleJavaFileObject {
        private final String text;
        private int reads;

        Source(String binaryName, String text) {
            super(URI.create("string:///" + binaryName.replace('.', '/') + Kind.SOURCE.extension),
                    Kind.SOURCE);
            this.text = text;
        }

        @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            reads++;
            return text;
        }

        @Override public InputStream openInputStream() {
            reads++;
            return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
        }
    }
}
