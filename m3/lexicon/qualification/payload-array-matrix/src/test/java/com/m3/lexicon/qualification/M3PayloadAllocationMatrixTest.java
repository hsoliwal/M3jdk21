// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
package com.m3.lexicon.qualification;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipFile;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Size/setup qualification of exact existing owners. Timing is observational. */
final class M3PayloadAllocationMatrixTest {
    static final int[] SIZES = {0, 1, 2, 7, 8, 31, 128, 8192};
    static final Path OUT = Path.of(System.getProperty("m3.measurement.root"));
    static final Path METRICS = OUT.resolve("measurements.tsv");
    static final String OWNER = "com.m3.text.SynexiaPrecomputePayload";
    static volatile Object sink;
    static Api before;
    static Api after;
    static com.sun.management.ThreadMXBean memory;

    static final class Api implements AutoCloseable {
        final URLClassLoader loader;
        final Class<?> owner;
        final Method parse;
        final Method project;
        final Method canonical;
        final Object[] projectionArgs = {"offsets"};
        Api(Path classes) throws Exception {
            loader = new URLClassLoader(new URL[] {classes.toUri().toURL()},
                    ClassLoader.getPlatformClassLoader());
            owner = loader.loadClass(OWNER);
            assertSame(loader, owner.getClassLoader());
            assertEquals(classes.toUri().toURL(),
                    owner.getProtectionDomain().getCodeSource().getLocation());
            parse = owner.getMethod("parse", String.class);
            project = owner.getMethod("requireIntArray", String.class);
            canonical = owner.getMethod("canonicalJson");
        }
        Object parse(String json) throws Exception { return parse.invoke(null, json); }
        int[] project(Object payload) throws Exception {
            return (int[]) project.invoke(payload, projectionArgs);
        }
        String canonical(Object payload) throws Exception { return (String) canonical.invoke(payload); }
        @Override public void close() throws java.io.IOException { loader.close(); }
    }

    static String sha(byte[] value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    }

    static Api compile(Path source, Path classes, String variant) throws Exception {
        Files.createDirectories(classes);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "JDK 21 compiler required");
        int code = compiler.run(null, output, output, "--release", "21", "-encoding", "UTF-8",
                "-Xlint:all", "-Werror", "-classpath", "", "-d", classes.toString(), source.toString());
        Files.write(OUT.resolve(variant + "-javac.log"), output.toByteArray());
        assertEquals(0, code, output.toString(StandardCharsets.UTF_8));
        return new Api(classes);
    }

    static void compareJar(Path classes, String property, String hash) throws Exception {
        String value = System.getProperty(property, "");
        if (value.isEmpty()) return; // Source proof remains; packaged identity is separately reported.
        Path jar = Path.of(value).toRealPath();
        assertEquals(hash, sha(Files.readAllBytes(jar)));
        int count = 0;
        try (ZipFile archive = new ZipFile(jar.toFile());
             var files = Files.walk(classes)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".class")).toList()) {
                String entry = classes.relativize(file).toString().replace('\\', '/');
                var member = archive.getEntry(entry);
                assertNotNull(member, entry);
                try (var stream = archive.getInputStream(member)) {
                    assertArrayEquals(Files.readAllBytes(file), stream.readAllBytes(), entry);
                }
                count++;
            }
        }
        assertEquals(5, count, "complete compiled owner and nestmates");
    }

    @BeforeAll static void setup() throws Exception {
        Files.createDirectories(OUT);
        Properties inputs = new Properties();
        try (var stream = M3PayloadAllocationMatrixTest.class.getResourceAsStream("/inputs.txt")) {
            assertNotNull(stream); inputs.load(stream);
        }
        byte[] baseline;
        try (var stream = M3PayloadAllocationMatrixTest.class.getResourceAsStream("/baseline.java.txt")) {
            assertNotNull(stream); baseline = stream.readAllBytes();
        }
        Path source = Path.of(System.getProperty("m3.receiver.root"))
                .resolve("m3/core/src/com/m3/text/SynexiaPrecomputePayload.java").toRealPath();
        assertEquals(inputs.getProperty("baseline.source.sha256"), sha(baseline));
        assertEquals(inputs.getProperty("candidate.source.sha256"), sha(Files.readAllBytes(source)));
        Path baselineSource = OUT.resolve("baseline-source/SynexiaPrecomputePayload.java");
        Files.createDirectories(baselineSource.getParent());
        Files.write(baselineSource, baseline);
        before = compile(baselineSource, OUT.resolve("baseline-classes"), "baseline");
        after = compile(source, OUT.resolve("candidate-classes"), "candidate");
        compareJar(OUT.resolve("baseline-classes"), "m3.baseline.jar", inputs.getProperty("baseline.jar.sha256"));
        compareJar(OUT.resolve("candidate-classes"), "m3.candidate.jar", inputs.getProperty("candidate.jar.sha256"));
        assertInstanceOf(com.sun.management.ThreadMXBean.class, ManagementFactory.getThreadMXBean());
        memory = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        assertTrue(memory.isThreadAllocatedMemorySupported(), "allocation instrumentation required");
        memory.setThreadAllocatedMemoryEnabled(true);
        assertTrue(memory.isThreadAllocatedMemoryEnabled());
        Files.writeString(METRICS, "stage\tlength\tvariant\tsample\toperations\tallocatedBytes\tnanos\n",
                StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        String runtime = "java=" + System.getProperty("java.runtime.version")
                + "\nvm=" + System.getProperty("java.vm.name")
                + "\nos=" + System.getProperty("os.name") + "/" + System.getProperty("os.arch")
                + "\nbaseline.source=" + sha(baseline)
                + "\ncandidate.source=" + sha(Files.readAllBytes(source))
                + "\nbaseline.jar.checked=" + !System.getProperty("m3.baseline.jar", "").isEmpty()
                + "\ncandidate.jar.checked=" + !System.getProperty("m3.candidate.jar", "").isEmpty()
                + "\nscope=thread allocation; parser+projection setup measured separately; timing observational\n";
        Files.writeString(OUT.resolve("runtime.txt"), runtime, StandardCharsets.UTF_8);
    }

    @AfterAll static void close() throws Exception {
        if (after != null) after.close();
        if (before != null) before.close();
    }

    static String json(int length) {
        StringBuilder text = new StringBuilder("{\"offsets\":[");
        for (int i = 0; i < length; i++) { if (i != 0) text.append(','); text.append(i); }
        return text.append("]}").toString();
    }

    @Test void valuesOwnershipAndCanonicalBytesMatchAcrossSizes() throws Exception {
        for (int length : SIZES) {
            String text = json(length);
            Object oldValue = before.parse(text), newValue = after.parse(text);
            int[] expected = before.project(oldValue), actual = after.project(newValue);
            assertArrayEquals(expected, actual);
            assertNotSame(actual, after.project(newValue));
            if (length > 0) actual[0] = -1;
            assertArrayEquals(expected, after.project(newValue));
            assertEquals(before.canonical(oldValue), after.canonical(newValue));
        }
    }

    static void sample(Api api, String variant, String stage, int length,
                       String text, Object payload, int sample, int operations) throws Exception {
        long thread = Thread.currentThread().threadId();
        long startBytes = memory.getThreadAllocatedBytes(thread);
        long startTime = System.nanoTime();
        for (int i = 0; i < operations; i++) {
            Object value = payload == null ? api.parse(text) : payload;
            sink = api.project(value);
        }
        long nanos = System.nanoTime() - startTime;
        long allocated = memory.getThreadAllocatedBytes(thread) - startBytes;
        int[] result = (int[]) sink;
        assertEquals(length, result.length);
        if (length > 0) assertEquals(length - 1, result[length - 1]);
        assertTrue(allocated >= 0);
        Files.writeString(METRICS, stage + "\t" + length + "\t" + variant + "\t" + sample
                + "\t" + operations + "\t" + allocated + "\t" + nanos + "\n",
                StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        if (api == after && payload != null) {
            assertTrue(allocated / (double) operations <= 4.0 * length + 160.0,
                    "candidate contains allocation beyond the required public int copy");
        }
    }

    @Test void preparsedProjectionAllocationAndTimingMatrix() throws Exception {
        for (int length : SIZES) {
            String text = json(length);
            Object oldValue = before.parse(text), newValue = after.parse(text);
            for (int warm = 0; warm < 512; warm++) {
                sink = before.project(oldValue); sink = after.project(newValue);
            }
            for (int sample = 0; sample < 8; sample++) {
                if ((sample & 1) == 0) {
                    sample(before, "baseline", "projection", length, text, oldValue, sample, 64);
                    sample(after, "candidate", "projection", length, text, newValue, sample, 64);
                } else {
                    sample(after, "candidate", "projection", length, text, newValue, sample, 64);
                    sample(before, "baseline", "projection", length, text, oldValue, sample, 64);
                }
            }
        }
    }

    @Test void parseAndProjectionSetupInclusiveMatrix() throws Exception {
        for (int length : SIZES) {
            String text = json(length);
            for (int warm = 0; warm < 32; warm++) {
                sink = before.project(before.parse(text)); sink = after.project(after.parse(text));
            }
            int operations = length >= 8192 ? 4 : 32;
            for (int sample = 0; sample < 8; sample++) {
                if ((sample & 1) == 0) {
                    sample(before, "baseline", "parse+projection", length, text, null, sample, operations);
                    sample(after, "candidate", "parse+projection", length, text, null, sample, operations);
                } else {
                    sample(after, "candidate", "parse+projection", length, text, null, sample, operations);
                    sample(before, "baseline", "parse+projection", length, text, null, sample, operations);
                }
            }
        }
    }
}
