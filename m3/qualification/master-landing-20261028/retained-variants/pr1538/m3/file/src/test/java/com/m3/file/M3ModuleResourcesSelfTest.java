/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.file;

import com.m3.collections.M3IndexMaskCompactor;
import com.m3.collections.M3IndexMaskCompactor.Backend;
import com.m3.tools.modulepack.M3ModuleInspector;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.lang.reflect.InaccessibleObjectException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/** Actual-reader, Java21 module-layer, and existing-JNI differential proof. */
public final class M3ModuleResourcesSelfTest {
    private static int checks;

    private M3ModuleResourcesSelfTest() { }

    public static void main(String[] args) throws Exception {
        boolean nativeRun = args.length == 1 && args[0].equals("--native");
        if (nativeRun && !M3IndexMaskCompactor.nativeAvailable()) {
            throw new AssertionError("actual m3_collections_mask JNI library required");
        }
        Backend[] backends = nativeRun
                ? new Backend[] {Backend.JAVA, Backend.NATIVE_REQUIRED}
                : new Backend[] {Backend.JAVA};
        captureAndQueries(backends);
        generatedWindows(backends);
        captureRefusals();
        lifecycleAndFailures();
        reentrantCloseCallbacks();
        reentrantOpenRefusesAndClosesReturnedHandles();
        concurrentClose();
        actualModularJar(backends);
        System.out.println("PASS M3ModuleResourcesSelfTest checks=" + checks
                + " actualJNI=" + nativeRun);
    }

    private static void captureAndQueries(Backend[] backends) throws Exception {
        String[] names = {"z/last.txt", "module-info.class", "a/Z.class", "a/",
                "a/A.class", "a/e\u0301", "a/\u00e9", "a/\ud800", "a/\uffff",
                "a0/other", "\uffff/edge", "data with spaces.txt"};
        FakeReader reader = new FakeReader(names);
        Reference reference = new Reference(() -> reader);
        try (M3ModuleResources owner = M3ModuleResources.open(reference)) {
            same(reference, owner.reference());
            same(reference.descriptor(), owner.descriptor());
            same(reference.location().orElseThrow(), owner.reference().location().orElseThrow());
            equal(names, owner.names());
            try (Stream<String> listed = owner.list()) { equal(names, listed.toArray(String[]::new)); }
            equal(1, reader.listCalls);
            equal(1, reader.listCloses);
            equal(1, reference.opens);
            String[] copy = owner.names();
            copy[0] = "changed";
            equal(names[0], owner.nameAt(0));
            equal(names.length, owner.size());
            expect(IndexOutOfBoundsException.class, () -> owner.nameAt(-1));
            expect(IndexOutOfBoundsException.class, () -> owner.nameAt(names.length));
            for (String name : names) {
                check(owner.contains(name));
                equal(name, owner.canonicalName(name).orElseThrow());
            }
            check(owner.contains("a")); // ModuleReader's directory alias.
            equal("a/", owner.canonicalName("a").orElseThrow());
            check(owner.find("a").isPresent());
            int beforeFind = reader.findCalls;
            for (String invalid : new String[] {"", "/", "/a", ".", "..", "a/..",
                    "a//Z.class", "a\\Z.class", "a/./Z.class", "a\0z"}) {
                check(!owner.contains(invalid));
                check(owner.canonicalName(invalid).isEmpty());
                check(owner.find(invalid).isEmpty());
                check(owner.open(invalid).isEmpty());
                expect(java.io.FileNotFoundException.class, () -> owner.openStream(invalid));
            }
            equal(beforeFind, reader.findCalls); // Refused names never reach provider I/O.
            expect(NullPointerException.class, () -> owner.contains(null));
            expect(NullPointerException.class, () -> owner.canonicalName(null));
            expect(NullPointerException.class, () -> owner.select(null, 0, 0, Backend.JAVA));
            expect(NullPointerException.class, () -> owner.select("", 0, 0, null));
            expect(IllegalArgumentException.class, () -> owner.select("a", -1, 0, Backend.JAVA));
            expect(IllegalArgumentException.class, () -> owner.select("a", 0, 256, Backend.JAVA));
            int[] masks = {0, 1, 2, 3, 4, 5, 6, 7, 8, 128, 255};
            for (String prefix : new String[] {"", "a", "a/", "a/A", "a/\uffff", "a/\ud800",
                    "a/e", "a/\u00e9", "module", "absent", "z/last.txt/", "\uffff"}) {
                for (int required : masks) for (int excluded : masks) {
                    int[] expected = linearOracle(names, prefix, required, excluded);
                    for (Backend backend : backends) {
                        int[] selected = owner.select(prefix, required, excluded, backend);
                        equal(expected, selected);
                        if (selected.length != 0) selected[0] = -1;
                        equal(expected, owner.select(prefix, required, excluded, backend));
                    }
                }
            }
            if (!M3IndexMaskCompactor.nativeAvailable()) {
                expect(UnsatisfiedLinkError.class,
                        () -> owner.select("absent", 0, 0, Backend.NATIVE_REQUIRED));
            }
        }
        equal(1, reader.closes);
    }

    private static void generatedWindows(Backend[] backends) throws Exception {
        Random random = new Random(0x4d334a504d53L);
        for (int size : new int[] {0, 1, 2, 7, 31, 255, 256, 257, 513}) {
            List<String> values = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                values.add((i % 3 == 0 ? "a/" : i % 3 == 1 ? "middle/" : "z/")
                        + i + (i % 5 == 0 ? ".class" : ".txt"));
            }
            for (int permutation = 0; permutation < 5; permutation++) {
                Collections.shuffle(values, random);
                String[] names = values.toArray(String[]::new);
                try (M3ModuleResources owner = M3ModuleResources.open(
                        new Reference(() -> new FakeReader(names)), size)) {
                    for (Backend backend : backends) {
                        for (String prefix : new String[] {"", "a/", "middle/", "z/", "missing/"}) {
                            for (int required = 0; required < 8; required++) {
                                for (int excluded = 0; excluded < 8; excluded++) {
                                    equal(linearOracle(names, prefix, required, excluded),
                                            owner.select(prefix, required, excluded, backend));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void captureRefusals() throws Exception {
        Reference unopened = new Reference(() -> new FakeReader("ok"));
        expect(IllegalArgumentException.class, () -> M3ModuleResources.open(unopened, -1));
        expect(IllegalArgumentException.class, () -> M3ModuleResources.open(unopened, 1, -1));
        equal(0, unopened.opens);
        expect(NullPointerException.class, () -> M3ModuleResources.open((ModuleReference) null));
        for (String[] invalid : new String[][] {{"dup", "other", "dup"}, {null}, {""}, {"/x"},
                {"../x"}, {"x/../z"}, {"x//z"}, {"x\\z"}, {"x\0z"}, {"x/./"}}) {
            FakeReader reader = new FakeReader(invalid);
            expect(IOException.class, () -> M3ModuleResources.open(new Reference(() -> reader)));
            equal(1, reader.closes);
            equal(1, reader.listCloses);
        }
        for (long chars : new long[] {0, 1, 2}) {
            FakeReader reader = new FakeReader("abc");
            expect(IOException.class, () -> M3ModuleResources.open(new Reference(() -> reader), 1, chars));
            equal(1, reader.closes);
        }
        FakeReader overEntries = new FakeReader("a", "b");
        expect(IOException.class, () -> M3ModuleResources.open(new Reference(() -> overEntries), 1));
        equal(1, overEntries.closes);
        try (M3ModuleResources zero = M3ModuleResources.open(
                new Reference(() -> new FakeReader()), 0, 0)) { equal(0, zero.size()); }
        try (M3ModuleResources maximum = M3ModuleResources.open(
                new Reference(() -> new FakeReader("abc")), Integer.MAX_VALUE, Long.MAX_VALUE)) {
            equal("abc", maximum.nameAt(0));
        }
        UncheckedIOException lazy = new UncheckedIOException(new IOException("lazy enumeration"));
        FakeReader failed = new FakeReader("first") {
            @Override public Stream<String> list() {
                return Stream.concat(Stream.of("first"), Stream.<String>generate(() -> { throw lazy; }))
                        .onClose(() -> listCloses++);
            }
        };
        failed.closeFailure = new IOException("reader close after enumeration");
        same(lazy, expect(UncheckedIOException.class,
                () -> M3ModuleResources.open(new Reference(() -> failed))));
        equal(1, failed.listCloses);
        equal(1, failed.closes);
        same(failed.closeFailure, lazy.getSuppressed()[0]);
        IOException listFailure = new IOException("list invocation");
        FakeReader listFailed = new FakeReader() {
            @Override public Stream<String> list() throws IOException { throw listFailure; }
        };
        same(listFailure, expect(IOException.class,
                () -> M3ModuleResources.open(new Reference(() -> listFailed))));
        equal(1, listFailed.closes);
    }

    private static void lifecycleAndFailures() throws Exception {
        FakeReader reader = new FakeReader("one", "two");
        M3ModuleResources owner = M3ModuleResources.open(new Reference(() -> reader));
        InputStream first = owner.openStream("one");
        InputStream second = owner.openStream("two");
        first.close();
        first.close();
        equal(1, reader.streams.get(0).closes);
        check(owner.isOpen());
        check(second.read() != -1);
        ByteBuffer detached = owner.read("one").orElseThrow();
        owner.release(detached);
        owner.close();
        owner.close();
        check(!owner.isOpen());
        equal(1, reader.closes);
        for (RawStream stream : reader.streams) equal(1, stream.closes);
        expect(ClosedChannelException.class, second::read);
        expect(ClosedChannelException.class, () -> second.read(new byte[0]));
        expect(ClosedChannelException.class, () -> second.skip(1));
        expect(ClosedChannelException.class, second::available);
        expect(ClosedChannelException.class, () -> owner.open("one"));
        expect(ClosedChannelException.class, () -> owner.openStream("one"));
        expect(ClosedChannelException.class, () -> owner.find("one"));
        expect(ClosedChannelException.class, owner::list);
        equal(2, owner.size());
        equal(new int[] {0, 1}, owner.select("", 1, 0, Backend.JAVA));
        check(owner.contains("one"));
        check(detached.hasRemaining()); // Default ModuleReader.read returned caller-owned heap bytes.

        FakeReader closing = new FakeReader("a", "b");
        M3ModuleResources closingOwner = M3ModuleResources.open(new Reference(() -> closing));
        closingOwner.openStream("a");
        closingOwner.openStream("b");
        IOException streamFailure = new IOException("first close");
        closing.streams.get(0).closeFailure = streamFailure;
        closing.streams.get(1).closeFailure = new IOException("second close");
        closing.closeFailure = new IOException("reader close");
        same(streamFailure, expect(IOException.class, closingOwner::close));
        equal(2, streamFailure.getSuppressed().length);
        same(closing.streams.get(1).closeFailure, streamFailure.getSuppressed()[0]);
        same(closing.closeFailure, streamFailure.getSuppressed()[1]);
        closingOwner.close();
        equal(1, closing.closes);
        for (RawStream stream : closing.streams) equal(1, stream.closes);

        FakeReader failingIO = new FakeReader("a");
        try (M3ModuleResources ioOwner = M3ModuleResources.open(new Reference(() -> failingIO))) {
            failingIO.findFailure = new IOException("find failure");
            same(failingIO.findFailure, expect(IOException.class, () -> ioOwner.find("a")));
            failingIO.openFailure = new IOException("open failure");
            same(failingIO.openFailure, expect(IOException.class, () -> ioOwner.openStream("a")));
            failingIO.openFailure = null;
            try (InputStream input = ioOwner.openStream("a")) {
                IOException readFailure = new IOException("read failure");
                failingIO.streams.get(0).readFailure = readFailure;
                same(readFailure, expect(IOException.class, input::read));
            }
        }
        equal(1, failingIO.closes);
        equal(1, failingIO.streams.get(0).closes);

        AtomicReference<String[]> revision = new AtomicReference<>(new String[] {"v1"});
        Reference changing = new Reference(() -> new FakeReader(revision.get()));
        M3ModuleResources old = M3ModuleResources.open(changing);
        try {
            revision.set(new String[] {"v2"});
            try (M3ModuleResources fresh = M3ModuleResources.open(changing)) {
                check(old.contains("v1") && !old.contains("v2"));
                check(fresh.contains("v2") && !fresh.contains("v1"));
                old.close();
                try (InputStream current = fresh.openStream("v2")) { check(current.read() != -1); }
            }
        } finally { old.close(); }
    }

    private static void concurrentClose() throws Exception {
        CountDownLatch reading = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch closing = new CountDownLatch(1);
        CountDownLatch closed = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        FakeReader reader = new FakeReader("blocked") {
            @Override protected RawStream newStream(String name) {
                return new RawStream(name) {
                    @Override public int read() throws IOException {
                        reading.countDown();
                        try {
                            if (!release.await(5, TimeUnit.SECONDS)) throw new IOException("read release timeout");
                        } catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IOException(ex); }
                        return super.read();
                    }
                };
            }
        };
        M3ModuleResources owner = M3ModuleResources.open(new Reference(() -> reader));
        InputStream input = owner.openStream("blocked");
        Thread readThread = new Thread(() -> {
            try { input.read(); } catch (Throwable ex) { failure.compareAndSet(null, ex); }
        }, "module-resource-read");
        Thread closeThread = new Thread(() -> {
            closing.countDown();
            try { owner.close(); } catch (Throwable ex) { failure.compareAndSet(null, ex); }
            finally { closed.countDown(); }
        }, "module-resource-close");
        readThread.start();
        check(reading.await(5, TimeUnit.SECONDS));
        closeThread.start();
        check(closing.await(5, TimeUnit.SECONDS));
        try { check(!closed.await(100, TimeUnit.MILLISECONDS)); }
        finally { release.countDown(); }
        readThread.join(5000);
        closeThread.join(5000);
        check(!readThread.isAlive() && !closeThread.isAlive());
        if (failure.get() != null) throw new AssertionError("concurrent read/close", failure.get());
        equal(1, reader.closes);
        equal(1, reader.streams.get(0).closes);
        expect(ClosedChannelException.class, input::read);
    }

    private static void reentrantCloseCallbacks() throws Exception {
        AtomicReference<InputStream> later = new AtomicReference<>();
        AtomicReference<M3ModuleResources> resourceOwner = new AtomicReference<>();
        FakeReader reader = new FakeReader("first", "middle", "last") {
            @Override protected RawStream newStream(String name) {
                return new RawStream(name) {
                    @Override public void close() throws IOException {
                        super.close();
                        if (name.equals("first")) later.get().close();
                        resourceOwner.get().close();
                    }
                };
            }
        };
        M3ModuleResources owner = M3ModuleResources.open(new Reference(() -> reader));
        resourceOwner.set(owner);
        owner.openStream("first");
        later.set(owner.openStream("middle"));
        owner.openStream("last");
        owner.close();
        equal(1, reader.closes);
        for (RawStream stream : reader.streams) equal(1, stream.closes);
        expect(ClosedChannelException.class, later.get()::read);
    }

    private static void reentrantOpenRefusesAndClosesReturnedHandles() throws Exception {
        for (int mode = 0; mode < 3; mode++) {
            boolean empty = mode == 2;
            AtomicReference<M3ModuleResources> scope = new AtomicReference<>();
            RawStream raw = new RawStream("payload");
            if (mode == 1) raw.closeFailure = new IOException("returned-handle cleanup");
            FakeReader reader = new FakeReader("payload") {
                @Override public Optional<InputStream> open(String name) throws IOException {
                    scope.get().close();
                    return empty ? Optional.empty() : Optional.of(raw);
                }
            };
            M3ModuleResources owner = M3ModuleResources.open(new Reference(() -> reader));
            scope.set(owner);
            ClosedChannelException refusal = expect(ClosedChannelException.class, () -> owner.open("payload"));
            check(!owner.isOpen());
            equal(empty ? 0 : 1, raw.closes);
            equal(1, reader.closes);
            if (raw.closeFailure != null) same(raw.closeFailure, refusal.getSuppressed()[0]);
            owner.close();
            equal(empty ? 0 : 1, raw.closes);
            equal("payload", owner.canonicalName("payload").orElseThrow());
        }
    }

    private static void actualModularJar(Backend[] backends) throws Exception {
        Path root = Files.createTempDirectory("m3-module-resources-");
        String property = "m3.module.resources.fixture.initialized";
        System.clearProperty(property);
        try {
            Path source = Files.createDirectories(root.resolve("src"));
            Path api = Files.createDirectories(source.resolve("fixture/api"));
            Path hidden = Files.createDirectories(source.resolve("fixture/hidden"));
            Path classes = Files.createDirectories(root.resolve("classes"));
            Files.writeString(source.resolve("module-info.java"),
                    "module fixture.module { exports fixture.api; }\n");
            Files.writeString(api.resolve("Visible.java"), """
                    package fixture.api;
                    public class Visible {
                        static { System.setProperty("m3.module.resources.fixture.initialized", "yes"); }
                        public static int answer() { return 42; }
                    }
                    """);
            Files.writeString(hidden.resolve("Secret.java"), """
                    package fixture.hidden;
                    public class Secret {
                        private static int secret = 7;
                        public static int value() { return secret; }
                    }
                    """);
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            check(compiler != null);
            equal(0, compiler.run(null, null, null, "--release", "21", "-d", classes.toString(),
                    source.resolve("module-info.java").toString(), api.resolve("Visible.java").toString(),
                    hidden.resolve("Secret.java").toString()));
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            Path jar = root.resolve("fixture.module.jar");
            try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar), manifest)) {
                for (String name : new String[] {"fixture/hidden/Secret.class", "data/nested/value.txt",
                        "module-info.class", "data/", "fixture/api/Visible.class", "data/plain.txt"}) {
                    out.putNextEntry(new JarEntry(name));
                    if (name.endsWith(".class")) Files.copy(classes.resolve(name), out);
                    else if (!name.endsWith("/")) out.write(name.getBytes(StandardCharsets.UTF_8));
                    out.closeEntry();
                }
            }
            equal("fixture.module", new M3ModuleInspector().inspect(jar).descriptor().name());
            ModuleFinder finder = ModuleFinder.of(jar);
            ModuleReference reference = finder.find("fixture.module").orElseThrow();
            String[] expected;
            try (ModuleReader direct = reference.open(); Stream<String> listed = direct.list()) {
                expected = listed.toArray(String[]::new);
            }
            try (M3ModuleResources owner = M3ModuleResources.open(reference)) {
                equal(expected, owner.names());
                same(reference, owner.reference());
                same(reference.descriptor(), owner.descriptor());
                check(System.getProperty(property) == null);
                for (Backend backend : backends) {
                    equal(linearOracle(expected, "fixture/", 2, 4),
                            owner.select("fixture/", M3ModuleResources.CLASS,
                                    M3ModuleResources.MODULE_DESCRIPTOR, backend));
                }
                try (InputStream input = owner.openStream("fixture/api/Visible.class")) {
                    equal(new byte[] {(byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe}, input.readNBytes(4));
                }
                check(System.getProperty(property) == null);
                try (ModuleReader direct = reference.open()) {
                    for (String name : expected) equal(direct.find(name), owner.find(name));
                    equal(direct.find("data"), owner.find("data"));
                }
                var configuration = ModuleLayer.boot().configuration().resolve(finder, ModuleFinder.of(),
                        Set.of("fixture.module"));
                ModuleLayer layer = ModuleLayer.defineModulesWithOneLoader(configuration,
                        List.of(ModuleLayer.boot()), ClassLoader.getSystemClassLoader()).layer();
                Module module = layer.findModule("fixture.module").orElseThrow();
                ClassLoader loader = layer.findLoader("fixture.module");
                Class<?> visible = Class.forName("fixture.api.Visible", false, loader);
                same(module, visible.getModule());
                same(visible, Class.forName(module, "fixture.api.Visible"));
                check(System.getProperty(property) == null);
                check(module.isExported("fixture.api"));
                check(!module.isExported("fixture.hidden"));
                check(!module.isOpen("fixture.hidden"));
                Class<?> secret = Class.forName("fixture.hidden.Secret", false, loader);
                expect(IllegalAccessException.class, () -> secret.getMethod("value").invoke(null));
                expect(InaccessibleObjectException.class, () -> secret.getDeclaredField("secret").setAccessible(true));
                equal(42, visible.getMethod("answer").invoke(null));
                equal("yes", System.getProperty(property));
                same(visible, loader.loadClass("fixture.api.Visible"));
            }
        } finally {
            System.clearProperty(property);
            try (Stream<Path> paths = Files.walk(root)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }

    /** Intentionally linear name oracle, independent of candidate bounds/index/mask implementation. */
    private static int[] linearOracle(String[] names, String prefix, int required, int excluded) {
        int[] buffer = new int[names.length];
        int count = 0;
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            int bits = 1;
            if (name.endsWith(".class")) bits |= 2;
            if (name.equals("module-info.class")) bits |= 4;
            if (name.startsWith(prefix) && (bits & required) == required && (bits & excluded) == 0) {
                buffer[count++] = i;
            }
        }
        return Arrays.copyOf(buffer, count);
    }

    private static class FakeReader implements ModuleReader {
        final String[] names;
        final List<RawStream> streams = new ArrayList<>();
        int listCalls, listCloses, findCalls, closes;
        IOException closeFailure, findFailure, openFailure;
        FakeReader(String... names) { this.names = names.clone(); }
        @Override public Stream<String> list() throws IOException {
            listCalls++;
            return Arrays.stream(names).onClose(() -> listCloses++);
        }
        @Override public Optional<URI> find(String name) throws IOException {
            findCalls++;
            if (findFailure != null) throw findFailure;
            for (int i = 0; i < names.length; i++) {
                if (name.equals(names[i]) || (name + "/").equals(names[i])) {
                    return Optional.of(URI.create("memory:/resource/" + i));
                }
            }
            return Optional.empty();
        }
        @Override public Optional<InputStream> open(String name) throws IOException {
            if (openFailure != null) throw openFailure;
            for (String listed : names) if (name.equals(listed) || (name + "/").equals(listed)) {
                RawStream stream = newStream(name);
                streams.add(stream);
                return Optional.of(stream);
            }
            return Optional.empty();
        }
        protected RawStream newStream(String name) { return new RawStream(name); }
        @Override public void close() throws IOException {
            closes++;
            if (closeFailure != null) throw closeFailure;
        }
    }

    private static class RawStream extends InputStream {
        final byte[] bytes;
        int position, closes;
        IOException closeFailure, readFailure;
        RawStream(String name) { bytes = name.getBytes(StandardCharsets.UTF_8); }
        @Override public int read() throws IOException {
            if (readFailure != null) throw readFailure;
            return position == bytes.length ? -1 : bytes[position++] & 255;
        }
        @Override public void close() throws IOException {
            closes++;
            if (closeFailure != null) throw closeFailure;
        }
    }

    private static final class Reference extends ModuleReference {
        private final ReaderSupplier supplier;
        int opens;
        Reference(ReaderSupplier supplier) {
            super(ModuleDescriptor.newModule("m3.fixture.synthetic").build(), URI.create("memory:/module"));
            this.supplier = supplier;
        }
        @Override public ModuleReader open() throws IOException { opens++; return supplier.get(); }
    }

    @FunctionalInterface private interface ReaderSupplier { ModuleReader get() throws IOException; }
    @FunctionalInterface private interface Action { void run() throws Exception; }

    private static <T extends Throwable> T expect(Class<T> type, Action action) throws Exception {
        checks++;
        try { action.run(); }
        catch (Throwable actual) {
            if (type.isInstance(actual)) return type.cast(actual);
            throw new AssertionError("expected " + type.getName() + " but got " + actual, actual);
        }
        throw new AssertionError("expected " + type.getName());
    }
    private static void check(boolean condition) { checks++; if (!condition) throw new AssertionError("check " + checks); }
    private static void same(Object expected, Object actual) { check(expected == actual); }
    private static void equal(Object expected, Object actual) {
        check(java.util.Objects.deepEquals(expected, actual));
    }
}
