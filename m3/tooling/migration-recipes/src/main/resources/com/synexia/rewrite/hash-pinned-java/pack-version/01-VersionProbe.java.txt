// SPDX-License-Identifier: Apache-2.0
package com.m3.tools.modulepack;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import javax.tools.ToolProvider;

/**
 * Real Java-21 class-loader versus JAR/JMOD header-admission tests.
 * The standalone entry shares cases with JUnit but is not a JUnit or coverage substitute.
 */
public final class VersionProbe {
    private static final String CLASS_NAME = "example.VersionValue";
    private static final String CLASS_PATH = "example/VersionValue.class";
    private static final List<Integer> MINORS = List.of(0, 1, 32767, 65534, 65535);
    private int checks;

    private VersionProbe() {}

    /** Run against a caller-owned directory, refusing fixture overwrite. */
    public static int run(Path root) throws Exception {
        if (Runtime.version().feature() != 21) {
            throw new IllegalStateException("Java 21 VM required for this differential oracle");
        }
        Files.createDirectories(root);
        VersionProbe suite = new VersionProbe();
        Path classes = compileFixture(root);
        byte[] descriptor = Files.readAllBytes(classes.resolve("module-info.class"));
        byte[] value = Files.readAllBytes(classes.resolve(CLASS_PATH));
        suite.actualJmod(classes, root);
        for (int major : new int[] {0, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53,
                54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 65535}) {
            for (int minor : MINORS) {
                suite.version(root, descriptor, value, major, minor);
            }
        }
        suite.multiRelease(root, descriptor, value);
        return suite.checks;
    }

    /** Execute the same corpus without pretending that a testing framework ran. */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("fresh fixture directory required");
        System.out.println("M3_CLASS_VERSION_PASS checks=" + run(Path.of(args[0])));
    }

    /** Compile a deliberately simple, owned fixture; no third-party class body is loaded. */
    private static Path compileFixture(Path root) throws IOException {
        Path source = Files.createDirectory(root.resolve("source"));
        Path packageDir = Files.createDirectory(source.resolve("example"));
        Path module = source.resolve("module-info.java");
        Path value = packageDir.resolve("VersionValue.java");
        Files.writeString(module, "module version.fixture { exports example; }", StandardOpenOption.CREATE_NEW);
        Files.writeString(value, "package example; public class VersionValue { public VersionValue() {} }", StandardOpenOption.CREATE_NEW);
        Path classes = Files.createDirectory(root.resolve("classes"));
        int exit = ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "--release", "21", "-Xlint:all", "-Werror", "-d", classes.toString(),
                module.toString(), value.toString());
        if (exit != 0) throw new AssertionError("owned fixture compilation failed: " + exit);
        return classes;
    }

    /** Include a JMOD emitted by the actual JDK tool as a container control. */
    private void actualJmod(Path classes, Path root) throws IOException {
        Path path = root.resolve("tool-created.jmod");
        int exit = java.util.spi.ToolProvider.findFirst("jmod").orElseThrow().run(
                new PrintWriter(System.out), new PrintWriter(System.err), "create",
                "--class-path", classes.toString(), path.toString());
        expect(exit == 0, "real jmod create");
        expect(new M3ModuleInspector().inspect(path).kind().equals("JMOD"), "real jmod control");
    }

    /** Test the same class body with one changed major/minor header in both archive formats. */
    private void version(Path root, byte[] descriptor, byte[] original, int major, int minor)
            throws IOException {
        byte[] value = withVersion(original, major, minor);
        boolean expected = major >= 45 && major <= 65 && (major <= 55 || minor == 0);
        boolean vm = acceptsVm(value);
        expect(vm == expected, "JVMS/VM mismatch at " + major + "." + minor);
        for (boolean jmod : List.of(false, true)) {
            Path path = archive(root.resolve("v" + major + "-" + minor
                    + (jmod ? ".jmod" : ".jar")), Map.of(
                            "module-info.class", descriptor, CLASS_PATH, value), jmod);
            String before = M3ModuleInspector.sha256(path);
            boolean admitted = acceptsInspector(path);
            expect(admitted == vm, "archive/VM mismatch at " + major + "." + minor + " " + path);
            expect(M3ModuleInspector.sha256(path).equals(before), "inspector mutated input");
        }
    }

    /** Test Java-21 effective-entry selection, rather than validating unused future overlays. */
    private void multiRelease(Path root, byte[] descriptor, byte[] value) throws IOException {
        byte[] invalidMinor = withVersion(value, 65, 1);
        byte[] legacyMinor = withVersion(value, 45, 65535);
        for (int release : List.of(9, 21, 22)) {
            for (boolean valid : List.of(false, true)) {
                TreeMap<String, byte[]> entries = new TreeMap<>();
                entries.put("module-info.class", descriptor);
                entries.put(CLASS_PATH, value);
                entries.put("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\r\nMulti-Release: true\r\n\r\n"
                        .getBytes(StandardCharsets.UTF_8));
                entries.put("META-INF/versions/" + release + "/" + CLASS_PATH,
                        valid ? legacyMinor : invalidMinor);
                Path jar = archive(root.resolve("mr" + release + "-" + valid + ".jar"), entries, false);
                byte[] selected;
                try (JarFile input = new JarFile(jar.toFile(), false, ZipFile.OPEN_READ,
                        Runtime.Version.parse("21"));
                        var bytes = input.getInputStream(input.getJarEntry(CLASS_PATH))) {
                    selected = bytes.readAllBytes();
                }
                boolean vm = acceptsVm(selected);
                expect(vm == (release > 21 || valid), "multi-release VM selection");
                expect(acceptsInspector(jar) == vm, "multi-release inspector selection");
            }
        }
    }

    private static byte[] withVersion(byte[] original, int major, int minor) {
        byte[] bytes = original.clone();
        bytes[4] = (byte) (minor >>> 8);
        bytes[5] = (byte) minor;
        bytes[6] = (byte) (major >>> 8);
        bytes[7] = (byte) major;
        return bytes;
    }

    /** Only version rejection is expected; other VM errors fail the test. */
    private static boolean acceptsVm(byte[] bytes) {
        try {
            new FixtureLoader().accept(bytes);
            return true;
        } catch (UnsupportedClassVersionError rejected) {
            return false;
        }
    }

    /** Only the existing version-policy error is accepted as a negative test result. */
    private static boolean acceptsInspector(Path path) throws IOException {
        try {
            new M3ModuleInspector().inspect(path);
            return true;
        } catch (IllegalArgumentException rejected) {
            if (!rejected.getMessage().startsWith("NOT_JAVA21_NONPREVIEW: ")) throw rejected;
            return false;
        }
    }

    /** Rebuild controlled archive entries with the real JMOD header when requested. */
    private static Path archive(Path path, Map<String, byte[]> entries, boolean jmod)
            throws IOException {
        try (OutputStream output = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW)) {
            if (jmod) output.write(new byte[] {0x4a, 0x4d, 0x01, 0x00});
            try (ZipOutputStream zip = new ZipOutputStream(output)) {
                for (var item : new TreeMap<>(entries).entrySet()) {
                    ZipEntry entry = new ZipEntry((jmod ? "classes/" : "") + item.getKey());
                    entry.setTime(0L);
                    zip.putNextEntry(entry);
                    zip.write(item.getValue());
                    zip.closeEntry();
                }
            }
        }
        return path;
    }

    private void expect(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    /** One fresh loader per owned fixture avoids name reuse and initialization side effects. */
    private static final class FixtureLoader extends ClassLoader {
        private FixtureLoader() { super(null); }
        private Class<?> accept(byte[] bytes) {
            return defineClass(CLASS_NAME, bytes, 0, bytes.length);
        }
    }
}
