// SPDX-License-Identifier: Apache-2.0
package com.m3.indexdb;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests private behavioral leaves without widening API visibility or corrupting public graphs. */
final class DbAtomTest {
    @TempDir Path home;

    @Test
    void sizeFoldPreservesCountsAndClosesItsStream() throws Throwable {
        Path a = Files.write(home.resolve("a"), new byte[] {1, 2});
        Path b = Files.write(home.resolve("b"), new byte[] {3});
        AtomicBoolean closed = new AtomicBoolean();
        Stream<Path> paths = Stream.of(a, b).onClose(() -> closed.set(true));
        assertEquals(3L, call(M3IndexDB.class, "bytes", new Class<?>[] {Stream.class}, paths));
        assertTrue(closed.get());
    }

    @Test
    void sizeFailureIsUnwrappedOnlyAfterCleanup() {
        AtomicBoolean closed = new AtomicBoolean();
        RuntimeException closing = new IllegalStateException("close failure");
        Stream<Path> paths = Stream.of(home.resolve("absent")).onClose(() -> {
            closed.set(true);
            throw closing;
        });
        IOException failure = assertThrows(IOException.class,
                () -> call(M3IndexDB.class, "bytes", new Class<?>[] {Stream.class}, paths));
        assertInstanceOf(NoSuchFileException.class, failure);
        assertTrue(closed.get());
        // The existing wrapper is discarded after close; its suppressed close failure is not moved.
        assertEquals(0, failure.getSuppressed().length);
    }

    @Test
    void closeFailureIsNotMistakenForAFileSizeFailure() {
        RuntimeException closing = new IllegalStateException("close failure");
        Stream<Path> paths = Stream.<Path>empty().onClose(() -> { throw closing; });
        assertSame(closing, assertThrows(IllegalStateException.class,
                () -> call(M3IndexDB.class, "bytes", new Class<?>[] {Stream.class}, paths)));
    }

    @Test
    void moveFallbackUsesRealDifferentFilesystemProviders() throws Throwable {
        Path target = Files.writeString(home.resolve("target"), "old");
        try (var zip = FileSystems.newFileSystem(home.resolve("move.zip"), Map.of("create", "true"))) {
            Path source = Files.writeString(zip.getPath("/source"), "new payload");
            call(M3IndexDB.class, "move", new Class<?>[] {Path.class, Path.class}, source, target);
            assertEquals("new payload", Files.readString(target));
            assertFalse(Files.exists(source));
        }
    }

    @Test
    void failedFallbackLeavesTheSourceAndTargetContentsIntact() throws IOException {
        Path target = Files.createDirectory(home.resolve("target"));
        Files.writeString(target.resolve("child"), "keep");
        try (var zip = FileSystems.newFileSystem(home.resolve("failure.zip"), Map.of("create", "true"))) {
            Path source = Files.writeString(zip.getPath("/source"), "keep source");
            assertThrows(IOException.class,
                    () -> call(M3IndexDB.class, "move", new Class<?>[] {Path.class, Path.class}, source, target));
            assertEquals("keep source", Files.readString(source));
            assertEquals("keep", Files.readString(target.resolve("child")));
        }
    }

    @Test
    void edgeAtomValidatesBothEndpointsBeforeEmittingBytes() throws Throwable {
        var edge = new M3IndexDbSemanticEdge("0".repeat(64), "1".repeat(64), "BODY", 7);
        Object strings = call(M3IndexDbSemanticCodec.class, "strings",
                new Class<?>[] {List.class, List.class}, List.of(), List.of(edge));
        Class<?>[] types = {DataOutputStream.class, M3IndexDbSemanticEdge.class, Map.class, strings.getClass()};
        for (Map<String, Integer> incomplete : List.of(Map.<String, Integer>of(),
                Map.of(edge.parentId(), 3), Map.of(edge.childId(), 9))) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            assertThrows(IllegalStateException.class, () -> call(M3IndexDbSemanticCodec.class,
                    "edge", types, new DataOutputStream(bytes), edge, incomplete, strings));
            assertEquals(0, bytes.size());
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        call(M3IndexDbSemanticCodec.class, "edge", types, new DataOutputStream(bytes), edge,
                Map.of(edge.parentId(), 3, edge.childId(), 9), strings);
        assertArrayEquals(ByteBuffer.allocate(16).putInt(3).putInt(9).putInt(0).putInt(7).array(),
                bytes.toByteArray());
    }

    @Test
    void privateTableRefusesUninternedMembership() throws Throwable {
        Object strings = call(M3IndexDbSemanticCodec.class, "strings",
                new Class<?>[] {List.class, List.class}, List.of(), List.of());
        var method = strings.getClass().getDeclaredMethod("id", String.class);
        method.setAccessible(true);
        InvocationTargetException failure = assertThrows(InvocationTargetException.class,
                () -> method.invoke(strings, "missing"));
        assertInstanceOf(IllegalStateException.class, failure.getCause());
        assertEquals("uninterned semantic string", failure.getCause().getMessage());
    }

    @Test
    void shaAtomRejectsWrongWidthsWithoutPartialOutput() throws Throwable {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        var out = new DataOutputStream(bytes);
        for (String invalid : List.of("", "00", "00".repeat(33))) {
            assertThrows(IllegalArgumentException.class, () -> call(M3IndexDbSemanticCodec.class,
                    "writeSha", new Class<?>[] {DataOutputStream.class, String.class}, out, invalid));
            assertEquals(0, bytes.size());
        }
        call(M3IndexDbSemanticCodec.class, "writeSha",
                new Class<?>[] {DataOutputStream.class, String.class}, out, "00".repeat(32));
        assertArrayEquals(new byte[32], bytes.toByteArray());
    }

    @Test
    void textAtomRejectsShortReadsRatherThanDecodingAPartialBody() {
        byte[] frame = ByteBuffer.allocate(5).putInt(2).put((byte) 0xc3).array();
        // Fault stream: availability overstates the body; the read still reports its real EOF.
        var input = new DataInputStream(new ByteArrayInputStream(frame) {
            @Override public synchronized int available() { return super.available() + 1; }
        });
        EOFException failure = assertThrows(EOFException.class,
                () -> call(M3IndexDbSemanticCodec.class, "text", new Class<?>[] {DataInputStream.class}, input));
        assertEquals("truncated semantic string", failure.getMessage());
    }

    @Test
    void missingDigestIsFailClosedInAnIsolatedJvmOnly() throws Exception {
        List<String> providers = Arrays.stream(Security.getProviders()).map(p -> p.getName()).toList();
        Path configuration = Files.writeString(home.resolve("empty.security"), "# isolated fault JVM\n");
        Path output = home.resolve("digest.log");
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.add("-Xmx32m");
        // Reuse the real agent only. Append preserves parent and child observations in one report.
        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (arg.startsWith("-javaagent:") && arg.contains("jacoco")) {
                command.add(arg.replace("append=false", "append=true"));
            }
        }
        command.add("-Djava.security.properties==" + configuration.toUri());
        command.add("-cp");
        command.add(System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")));
        command.add(DbDigest.class.getName());
        command.add(home.resolve("digest-db").toString());
        Process child = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            assertTrue(child.waitFor(30, TimeUnit.SECONDS), "digest fault JVM timeout");
            String observed = Files.readString(output, StandardCharsets.UTF_8);
            assertEquals(0, child.exitValue(), observed);
            assertTrue(observed.contains("DB_DIGEST_FAULT_PASS"), observed);
        } finally {
            child.destroyForcibly();
        }
        assertEquals(providers, Arrays.stream(Security.getProviders()).map(p -> p.getName()).toList());
    }

    private static Object call(Class<?> owner, String name, Class<?>[] types, Object... arguments)
            throws Throwable {
        var method = owner.getDeclaredMethod(name, types);
        method.setAccessible(true);
        try {
            return method.invoke(null, arguments);
        } catch (InvocationTargetException wrapped) {
            throw wrapped.getCause();
        }
    }
}
