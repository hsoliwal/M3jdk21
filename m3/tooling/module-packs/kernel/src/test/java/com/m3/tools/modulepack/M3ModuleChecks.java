// SPDX-License-Identifier: Apache-2.0
package com.m3.tools.modulepack;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.module.ModuleDescriptor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.tools.ToolProvider;

/**
 * Shared executable contract cases; callable from real JUnit or the offline main.
 * The offline runner is explicitly not a JUnit implementation or coverage report.
 */
public final class M3ModuleChecks {
    private int checks;
    private final M3ModuleInspector inspector = new M3ModuleInspector();

    /** Run actual archive, graph and CLI cases in a caller-owned fresh directory. */
    public static int run(Path root) throws Exception {
        M3ModuleChecks suite = new M3ModuleChecks();
        suite.archives(root);
        suite.graphs();
        return suite.checks;
    }

    /** Independent Java-21 entry point when Maven/JUnit artifacts are unavailable. */
    public static void main(String[] args) throws Exception {
        System.out.println("M3_MODULE_CHECKS_PASS checks=" + run(Path.of(args[0])));
    }

    private void archives(Path root) throws Exception {
        Files.createDirectories(root);
        Path source = Files.createDirectory(root.resolve("source"));
        Path classes = Files.createDirectory(root.resolve("classes"));
        Files.writeString(source.resolve("module-info.java"), "module example.app { exports example; }");
        Path packageDir = Files.createDirectory(source.resolve("example"));
        Files.writeString(packageDir.resolve("Value.java"), "package example; public class Value {}");
        int compiled = ToolProvider.getSystemJavaCompiler().run(null, null, null, "--release", "21",
                "-d", classes.toString(), source.resolve("module-info.java").toString(),
                packageDir.resolve("Value.java").toString());
        expect(compiled == 0, "fixture compile");
        byte[] descriptor = Files.readAllBytes(classes.resolve("module-info.class"));
        byte[] value = Files.readAllBytes(classes.resolve("example/Value.class"));
        Map<String, byte[]> content = Map.of("module-info.class", descriptor,
                "example/Value.class", value, "data.txt", new byte[] {1, 2},
                "lib/native.so", new byte[] {1}, "LICENSE", "fixture".getBytes(StandardCharsets.UTF_8));
        Path jar = archive(root.resolve("explicit.jar"), content, false);
        M3ModuleArtifact inspected = inspector.inspect(jar);
        expect(inspected.descriptor().name().equals("example.app"), "descriptor name");
        expect(inspected.kind().equals("JAR"), "jar kind");
        expect(inspected.nativeEntries().equals(List.of("lib/native.so")), "native inventory");
        expect(inspected.resources().contains("LICENSE"), "legal resource retention");
        expect(inspected.sha256().equals(M3ModuleInspector.sha256(jar)), "checksum");
        expect(inspected.equals(inspector.inspect(jar)), "deterministic inspection");
        refuses(() -> inspected.resources().add("drift"), "immutable resources");
        refuses(() -> new M3ModuleArtifact(null, inspected.descriptor(), "", "JAR",
                List.of(), List.of()), "null metadata");

        Path jmod = root.resolve("explicit.jmod");
        var tool = java.util.spi.ToolProvider.findFirst("jmod").orElseThrow();
        int result = tool.run(new PrintWriter(System.out), new PrintWriter(System.err), "create",
                "--class-path", classes.toString(), jmod.toString());
        expect(result == 0, "real jmod creation");
        expect(inspector.inspect(jmod).kind().equals("JMOD"), "real jmod inspect");
        Path fakeHeader = archive(root.resolve("bad-header.jmod"), content, false);
        refuses(() -> inspector.inspect(fakeHeader), "fake jmod header");
        Path missing = archive(root.resolve("missing.jar"),
                Map.of("example/Value.class", value), false);
        refuses(() -> inspector.inspect(missing), "automatic module");
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().putValue("Automatic-Module-Name", "example.auto");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        manifest.write(bytes);
        Path namedAuto = archive(root.resolve("named-auto.jar"),
                Map.of("META-INF/MANIFEST.MF", bytes.toByteArray(), "example/Value.class", value), false);
        refuses(() -> inspector.inspect(namedAuto), "automatic-module-name is not descriptor");

        TreeMap<String, byte[]> altered = new TreeMap<>(content);
        altered.put("META-INF/SIGN.SF", new byte[] {1});
        Path signed = archive(root.resolve("signed.jar"), altered, false);
        refuses(() -> inspector.inspect(signed), "signature refusal");
        altered = new TreeMap<>(content);
        byte[] newer = value.clone();
        newer[7] = 66;
        altered.put("example/Value.class", newer);
        Path future = archive(root.resolve("future.jar"), altered, false);
        refuses(() -> inspector.inspect(future), "newer bytecode");
        byte[] preview = value.clone();
        preview[4] = (byte) 255;
        preview[5] = (byte) 255;
        altered.put("example/Value.class", preview);
        Path previewJar = archive(root.resolve("preview.jar"), altered, false);
        refuses(() -> inspector.inspect(previewJar), "preview bytecode");
        altered.put("example/Value.class", new byte[] {0, 1});
        Path truncated = archive(root.resolve("truncated.jar"), altered, false);
        refuses(() -> inspector.inspect(truncated), "truncated bytecode");

        altered = new TreeMap<>(content);
        altered.put("META-INF/versions/22/example/Value.class", newer);
        Path multi = archive(root.resolve("multi.jar"), altered, true);
        expect(inspector.inspect(multi).descriptor().name().equals("example.app"),
                "ignore future MR classes in Java21 view");
        altered.put("META-INF/versions/21/example/Value.class", newer);
        Path badMulti = archive(root.resolve("bad-multi.jar"), altered, true);
        refuses(() -> inspector.inspect(badMulti), "check effective MR class");
        Path unsupported = Files.writeString(root.resolve("not-an-archive.txt"), "x");
        refuses(() -> inspector.inspect(unsupported), "unsupported artifact");
        refuses(() -> inspector.inspect(root.resolve("absent.jar")), "absent input");
        expect(M3ModuleInventory.paths(jar).equals(List.of(jar)), "single path");
        List<Path> expanded = M3ModuleInventory.paths(root);
        expect(expanded.contains(jar) && !expanded.contains(unsupported), "directory filtering");
        expect(expanded.equals(expanded.stream().sorted().toList()), "stable expansion");

        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            M3ModuleInventory.main(new String[] {"example.app", jar.toString(),
                    Path.of(System.getProperty("java.home"), "jmods", "java.base.jmod").toString()});
        } finally {
            System.setOut(original);
        }
        String tsv = output.toString(StandardCharsets.UTF_8);
        expect(tsv.contains("example.app\t") && tsv.contains("java.base\t"), "CLI closure");
        refuses(() -> M3ModuleInventory.main(new String[0]), "CLI usage");
    }

    private void graphs() throws Exception {
        M3ModuleGraph graph = new M3ModuleGraph();
        M3ModuleArtifact base = atom(ModuleDescriptor.newModule("java.base").build());
        M3ModuleArtifact library = atom(ModuleDescriptor.newModule("example.lib")
                .packages(Set.of("example.lib")).build());
        M3ModuleArtifact app = atom(ModuleDescriptor.newModule("example.app")
                .requires("example.lib").packages(Set.of("example.app")).build());
        List<M3ModuleArtifact> all = List.of(app, library, base);
        expect(graph.resolve(all, List.of("example.app")).stream()
                .map(a -> a.descriptor().name()).toList()
                .equals(List.of("example.app", "example.lib", "java.base")), "required closure");
        expect(graph.resolve(all, List.of("example.app", "example.app")).size() == 3,
                "duplicate roots are a set");
        expect(graph.resolve(List.of(base, library, app), List.of("example.app"))
                .equals(graph.resolve(all, List.of("example.app"))), "permutation invariant");
        refuses(() -> graph.resolve(all, List.of()), "empty roots");
        refuses(() -> graph.resolve(all, List.of("missing")), "missing root");
        refuses(() -> graph.resolve(List.of(app, base), List.of("example.app")), "missing dependency");
        refuses(() -> graph.resolve(List.of(app, app, base, library), List.of("example.app")),
                "module name collision");
        M3ModuleArtifact auto = atom(ModuleDescriptor.newAutomaticModule("example.auto").build());
        refuses(() -> graph.resolve(List.of(base, auto), List.of("example.auto")), "automatic graph");
        M3ModuleArtifact split = atom(ModuleDescriptor.newModule("example.split")
                .packages(Set.of("example.lib")).build());
        refuses(() -> graph.resolve(List.of(base, library, split),
                List.of("example.lib", "example.split")), "split packages");
        expect(graph.resolve(List.of(base, library, split), List.of("example.lib")).size() == 2,
                "unselected split package does not poison closure");
        M3ModuleArtifact optional = atom(ModuleDescriptor.newModule("example.optional")
                .requires(Set.of(ModuleDescriptor.Requires.Modifier.STATIC), "example.lib").build());
        expect(graph.resolve(List.of(base, optional), List.of("example.optional")).size() == 2,
                "missing static dependency is permitted");
        expect(graph.resolve(List.of(base, optional, library), List.of("example.optional")).size() == 2,
                "static dependency is not silently selected");
        expect(graph.resolve(List.of(base, optional, library),
                List.of("example.optional", "example.lib")).size() == 3, "explicit static root");
        M3ModuleArtifact cycleA = atom(ModuleDescriptor.newModule("cycle.a").requires("cycle.b").build());
        M3ModuleArtifact cycleB = atom(ModuleDescriptor.newModule("cycle.b").requires("cycle.a").build());
        refuses(() -> graph.resolve(List.of(base, cycleA, cycleB), List.of("cycle.a")), "requires cycle");
    }

    private static M3ModuleArtifact atom(ModuleDescriptor descriptor) {
        return new M3ModuleArtifact(Path.of(descriptor.name() + ".jar"), descriptor, "fixture",
                "JAR", List.of(), List.of());
    }

    private static Path archive(Path path, Map<String, byte[]> entries, boolean multiRelease)
            throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(path))) {
            if (multiRelease) {
                zip.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
                zip.write("Manifest-Version: 1.0\r\nMulti-Release: true\r\n\r\n"
                        .getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            for (var entry : new TreeMap<>(entries).entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        return path;
    }

    private void expect(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private void refuses(Throwing action, String message) throws Exception {
        checks++;
        try {
            action.run();
        } catch (IOException | IllegalArgumentException | UnsupportedOperationException
                | NullPointerException | java.lang.module.FindException expected) {
            return;
        }
        throw new AssertionError("did not refuse: " + message);
    }

    @FunctionalInterface
    private interface Throwing { void run() throws Exception; }
}
