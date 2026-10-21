/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.synexia.rewrite;

import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

/**
 * Compile-only adaptation of the existing qualified ToolProvider leaf.
 * Complete source units are compiled into a fresh owned directory and never loaded.
 */
final class M3StringConcatTypedCompiler {
    private static final long RESERVE = 8L * 1024 * 1024 * 1024;
    private static final long WORK_LIMIT = 2L * 1024 * 1024;
    private static final int FILE_LIMIT = 64;
    private static final int SOURCE_LIMIT = 64 * 1024;
    private static final int CLASS_LIMIT = 256 * 1024;
    private static final String FACTORY = "java/lang/invoke/StringConcatFactory.java";

    private M3StringConcatTypedCompiler() {
    }

    static List<String> compileCase(Path work, String id, Map<String, String> units,
            boolean platform, boolean success, Set<String> expectedClasses) throws Exception {
        require(Runtime.version().feature() == 21, "native Java21 required");
        require(id != null && id.matches("[a-z][a-z0-9-]{0,63}"), "invalid case id");
        require(work != null && work.isAbsolute() && work.equals(work.normalize()),
                "absolute normalized owned work root required");
        requireDirectoryChain(work);
        long[] before = census(work);
        require(Files.getFileStore(work).getUsableSpace() >= RESERVE + WORK_LIMIT,
                "8GiB reserve after planned 2MiB compiler output required");
        require(units != null && !units.isEmpty() && units.size() <= FILE_LIMIT,
                "complete source units required");
        require(expectedClasses != null, "expected class inventory required");
        Map<String, byte[]> sources = new TreeMap<>();
        long sourceBytes = 0;
        for (var entry : units.entrySet()) {
            String relative = relative(entry.getKey(), ".java");
            require(entry.getValue() != null, "null source unit");
            byte[] bytes = entry.getValue().getBytes(StandardCharsets.UTF_8);
            require(bytes.length <= SOURCE_LIMIT, "source unit exceeds 64KiB");
            require(new String(bytes, StandardCharsets.UTF_8).equals(entry.getValue()),
                    "source is not lossless UTF-8");
            require(sources.put(relative, bytes) == null, "duplicate source path");
            sourceBytes += bytes.length;
        }
        Set<String> expected = new TreeSet<>();
        for (String name : expectedClasses) {
            require(expected.add(relative(name, ".class")), "duplicate class path");
        }
        require(success ? !expected.isEmpty()
                : platform && sources.size() == 1 && sources.containsKey(FACTORY)
                        && expected.isEmpty(),
                "negative case must contain only complete Factory and no expected classes");
        require(before[0] + sourceBytes + (long) expected.size() * CLASS_LIMIT <= WORK_LIMIT,
                "planned compiler output exceeds 2MiB owned budget");
        require(before[1] + sources.size() + expected.size() <= FILE_LIMIT,
                "planned compiler output exceeds 64 files");
        Path directory = work.resolve(id);
        require(!Files.exists(directory, LinkOption.NOFOLLOW_LINKS), "case root already exists");
        Files.createDirectory(directory);
        Path sourceRoot = Files.createDirectory(directory.resolve("sources"));
        Path classes = Files.createDirectory(directory.resolve("classes"));
        Path empty = Files.createDirectory(directory.resolve("empty"));
        List<Path> files = new ArrayList<>();
        for (var entry : sources.entrySet()) {
            Path file = sourceRoot.resolve(entry.getKey());
            Files.createDirectories(file.getParent());
            requireDirectoryChain(file.getParent());
            Files.write(file, entry.getValue(), StandardOpenOption.CREATE_NEW);
            files.add(file);
            census(work);
        }
        verifySources(sourceRoot, sources);
        require(readFiles(empty, SOURCE_LIMIT).isEmpty(), "lookup root must remain empty");
        census(work);
        var compiler = ToolProvider.getSystemJavaCompiler();
        require(compiler != null, "full Java21 compiler required");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StringWriter log = new StringWriter();
        List<String> options = new ArrayList<>();
        if (platform) {
            options.addAll(List.of("--source", "21", "--target", "21",
                    "--patch-module", "java.base=" + sourceRoot));
        } else {
            options.addAll(List.of("--release", "21"));
        }
        options.addAll(List.of("-proc:none", "-implicit:none", "-Xlint:all", "-Werror",
                "-encoding", "UTF-8", "-classpath", empty.toString(),
                "-sourcepath", empty.toString(), "-d", classes.toString()));
        boolean compiled;
        try (var manager = compiler.getStandardFileManager(
                diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
            compiled = Boolean.TRUE.equals(compiler.getTask(log, manager, diagnostics,
                    options, null, manager.getJavaFileObjectsFromPaths(files)).call());
        }
        census(work);
        verifySources(sourceRoot, sources);
        require(readFiles(empty, SOURCE_LIMIT).isEmpty(), "compiler changed empty lookup root");
        require(log.toString().isEmpty(), "unexpected compiler writer output");
        Map<String, byte[]> classBytes = readFiles(classes, CLASS_LIMIT);
        if (success) {
            require(compiled, "strict Java21 compilation failed: " + diagnostics.getDiagnostics());
            require(diagnostics.getDiagnostics().isEmpty(), "unexpected compiler diagnostics");
        }
        require(classBytes.keySet().equals(expected), "closed class inventory mismatch");
        List<String> rows = new ArrayList<>();
        String profile = platform ? "PLATFORM" : "UNNAMED";
        rows.add("case\t" + id + "\t" + profile + "\t"
                + (success ? "SUCCESS" : "EXPECTED_MISSING_JLA"));
        for (var entry : sources.entrySet()) {
            rows.add("source\t" + id + "\t" + entry.getKey() + "\t"
                    + entry.getValue().length + "\t" + sha(entry.getValue()));
        }
        if (success) {
            for (var entry : classBytes.entrySet()) {
                byte[] bytes = entry.getValue();
                require(bytes.length >= 8 && (bytes[0] & 255) == 202
                        && (bytes[1] & 255) == 254 && (bytes[2] & 255) == 186
                        && (bytes[3] & 255) == 190, "invalid class magic");
                int minor = ((bytes[4] & 255) << 8) | (bytes[5] & 255);
                int major = ((bytes[6] & 255) << 8) | (bytes[7] & 255);
                require(major == 65 && minor == 0, "ordinary Java21 class required");
                rows.add("class\t" + id + "\t" + entry.getKey() + "\t"
                        + bytes.length + "\t" + sha(bytes) + "\t65\t0");
            }
        } else {
            require(!compiled && diagnostics.getDiagnostics().size() == 1,
                    "missing-JLA control must produce exactly one diagnostic");
            var diagnostic = diagnostics.getDiagnostics().get(0);
            String message = diagnostic.getMessage(Locale.ROOT);
            require(diagnostic.getKind() == Diagnostic.Kind.ERROR
                    && diagnostic.getCode().startsWith("compiler.err.cant.resolve")
                    && diagnostic.getLineNumber() == 492
                    && diagnostic.getSource() != null
                    && diagnostic.getSource().toUri().equals(sourceRoot.resolve(FACTORY).toUri())
                    && message.contains("stringConcatUsesM3Storage")
                    && message.contains("JavaLangAccess"),
                    "negative case failed for an unrelated reason");
            require(message.getBytes(StandardCharsets.UTF_8).length <= 8192,
                    "diagnostic exceeds receipt bound");
            rows.add("diagnostic\t" + id + "\t" + diagnostic.getCode()
                    + "\t492\t" + sha(message.getBytes(StandardCharsets.UTF_8)));
        }
        List<String> normalizedOptions = options.stream()
                .map(option -> option.replace(directory.toString(), "${CASE}")).toList();
        rows.add("options_b64\t" + profile + "\t" + Base64.getEncoder().encodeToString(
                String.join("\n", normalizedOptions).getBytes(StandardCharsets.UTF_8)));
        census(work);
        return List.copyOf(rows);
    }

    private static String relative(String name, String suffix) {
        require(name != null && name.endsWith(suffix), "invalid file suffix");
        String stem = name.substring(0, name.length() - suffix.length());
        require(stem.matches("[A-Za-z_$][A-Za-z0-9_$]*(/[A-Za-z_$][A-Za-z0-9_$]*)*"),
                "canonical relative Java path required");
        Path path = Path.of(name);
        require(!path.isAbsolute() && path.equals(path.normalize())
                && path.toString().replace('\\', '/').equals(name), "noncanonical file path");
        return name;
    }

    private static void verifySources(Path root, Map<String, byte[]> expected) throws Exception {
        Map<String, byte[]> observed = readFiles(root, SOURCE_LIMIT);
        require(observed.keySet().equals(expected.keySet()), "closed source inventory mismatch");
        for (var entry : expected.entrySet()) {
            require(Arrays.equals(entry.getValue(), observed.get(entry.getKey())),
                    "compiler source bytes changed");
        }
    }

    private static Map<String, byte[]> readFiles(Path root, int maximum) throws Exception {
        requireDirectoryChain(root);
        Map<String, byte[]> result = new TreeMap<>();
        try (var paths = Files.walk(root)) {
            var iterator = paths.iterator();
            int nodes = 0;
            while (iterator.hasNext()) {
                Path path = iterator.next();
                require(++nodes <= 256, "owned tree exceeds bounded node count");
                var attributes = Files.readAttributes(
                        path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                require(!attributes.isSymbolicLink(), "symlink in owned compiler tree");
                if (attributes.isDirectory()) continue;
                require(attributes.isRegularFile() && attributes.size() <= maximum,
                        "invalid or oversized compiler file");
                byte[] bytes;
                try (InputStream input = Files.newInputStream(
                        path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
                    bytes = input.readNBytes(maximum + 1);
                }
                var after = Files.readAttributes(
                        path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                require(after.isRegularFile() && !after.isSymbolicLink()
                        && bytes.length <= maximum && bytes.length == attributes.size()
                        && after.size() == attributes.size()
                        && (attributes.fileKey() == null
                            || attributes.fileKey().equals(after.fileKey())),
                        "compiler file changed during readback");
                String name = root.relativize(path).toString().replace('\\', '/');
                require(result.put(name, bytes) == null && result.size() <= FILE_LIMIT,
                        "duplicate or excessive compiler files");
            }
        }
        return result;
    }

    private static long[] census(Path work) throws Exception {
        requireDirectoryChain(work);
        long bytes = 0;
        long files = 0;
        try (var paths = Files.walk(work)) {
            var iterator = paths.iterator();
            int nodes = 0;
            while (iterator.hasNext()) {
                Path path = iterator.next();
                require(++nodes <= 256, "owned work exceeds bounded node count");
                var attributes = Files.readAttributes(
                        path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                require(!attributes.isSymbolicLink(), "symlink in owned work");
                if (attributes.isDirectory()) continue;
                require(attributes.isRegularFile(), "nonregular file in owned work");
                bytes += attributes.size();
                require(bytes <= WORK_LIMIT && ++files <= FILE_LIMIT,
                        "owned compiler work exceeds 2MiB/64 files");
            }
        }
        require(Files.getFileStore(work).getUsableSpace() >= RESERVE,
                "8GiB reserve breached during compiler qualification");
        return new long[]{bytes, files};
    }

    private static void requireDirectoryChain(Path path) throws Exception {
        Path absolute = path.toAbsolutePath().normalize();
        Path current = absolute.getRoot();
        require(current != null, "filesystem root required");
        require(Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)
                && !Files.isSymbolicLink(current), "invalid filesystem root");
        for (Path component : absolute) {
            current = current.resolve(component);
            require(Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)
                    && !Files.isSymbolicLink(current), "owned directory must be physical");
        }
    }

    private static String sha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
