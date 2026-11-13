/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Deterministic class-path inventory and class-file scanner. */
public final class M3ClassPathScanner {
    private static final String SERVICES_PREFIX = "META-INF/services/";
    /** One content-addressed class-path input. */
    public record Input(String id, Path path, String logicalName, String kind, String sha256) {
        /** Validates an input. */
        public Input {
            if (id == null || !id.matches("cp[0-9]{4}")) {
                throw new IllegalArgumentException("Invalid input id: " + id);
            }
            path = path.toAbsolutePath().normalize();
            if (logicalName == null || logicalName.isBlank() || kind == null || kind.isBlank()) {
                throw new IllegalArgumentException("Input metadata must not be blank");
            }
            sha256 = M3Hashing.requireSha256(sha256, "input sha256");
        }
    }

    /** Complete scan result. */
    public record Result(List<Input> inputs, Map<String, M3ClassInfo> classes,
                         Map<String, List<String>> shadowedDefinitions,
                         Set<String> serviceProviders) {
        /** Normalizes immutable collections. */
        public Result {
            inputs = List.copyOf(inputs);
            classes = Collections.unmodifiableMap(new TreeMap<>(classes));
            TreeMap<String, List<String>> shadowed = new TreeMap<>();
            shadowedDefinitions.forEach((key, value) -> shadowed.put(key, List.copyOf(value)));
            shadowedDefinitions = Collections.unmodifiableMap(shadowed);
            serviceProviders = Collections.unmodifiableSet(new TreeSet<>(serviceProviders));
        }
    }

    private record ScannedInput(Map<String, M3ClassInfo> classes, Set<String> serviceProviders) {
    }

    private M3ClassPathScanner() {
    }

    /** Splits a platform class path. */
    public static List<Path> parseClassPath(String classPath) {
        if (classPath == null || classPath.isBlank()) {
            throw new IllegalArgumentException("Classpath must not be blank");
        }
        List<Path> paths = new ArrayList<>();
        for (String element : classPath.split(java.util.regex.Pattern.quote(File.pathSeparator), -1)) {
            if (element.isBlank()) {
                throw new IllegalArgumentException("Empty classpath element is not admitted");
            }
            paths.add(Path.of(element).toAbsolutePath().normalize());
        }
        return List.copyOf(paths);
    }

    /** Scans all inputs in class-path order; the first definition wins. */
    public static Result scan(List<Path> classPath) throws IOException {
        if (classPath == null || classPath.isEmpty()) {
            throw new IllegalArgumentException("Classpath must contain at least one input");
        }
        List<Input> inputs = new ArrayList<>();
        Map<String, M3ClassInfo> classes = new LinkedHashMap<>();
        Map<String, List<String>> shadowed = new LinkedHashMap<>();
        Set<String> providers = new TreeSet<>();
        for (int index = 0; index < classPath.size(); index++) {
            Path path = classPath.get(index).toAbsolutePath().normalize();
            if (!Files.exists(path) || Files.isSymbolicLink(path)) {
                throw new IOException("Missing or symbolic classpath input: " + path);
            }
            String id = "cp" + String.format(java.util.Locale.ROOT, "%04d", index);
            String kind = Files.isDirectory(path) ? "DIRECTORY" : "ARCHIVE";
            if (!Files.isDirectory(path) && !Files.isRegularFile(path)) {
                throw new IOException("Unsupported classpath input: " + path);
            }
            String logicalName = path.getFileName() == null ? id : path.getFileName().toString();
            Input input = new Input(id, path, logicalName, kind, M3Hashing.hashPath(path));
            inputs.add(input);
            ScannedInput scanned = Files.isDirectory(path) ? scanDirectory(input) : scanArchive(input);
            providers.addAll(scanned.serviceProviders());
            for (Map.Entry<String, M3ClassInfo> entry : scanned.classes().entrySet()) {
                M3ClassInfo existing = classes.putIfAbsent(entry.getKey(), entry.getValue());
                if (existing != null) {
                    shadowed.computeIfAbsent(entry.getKey(), ignored -> new ArrayList<>())
                            .add(entry.getValue().inputId() + ":" + entry.getValue().entryName());
                }
            }
        }
        return new Result(inputs, classes, shadowed, providers);
    }

    private static ScannedInput scanDirectory(Input input) throws IOException {
        Map<String, M3ClassInfo> result = new TreeMap<>();
        Set<String> providers = new TreeSet<>();
        try (var stream = Files.walk(input.path())) {
            for (Path file : stream.sorted().toList()) {
                if (Files.isSymbolicLink(file)) {
                    throw new IOException("Symbolic path inside classpath directory: " + file);
                }
                if (!Files.isRegularFile(file)) {
                    continue;
                }
                String entry = input.path().relativize(file).toString()
                        .replace(file.getFileSystem().getSeparator(), "/");
                if (entry.startsWith(SERVICES_PREFIX)) {
                    providers.addAll(readProviders(Files.readString(file, StandardCharsets.UTF_8), entry));
                }
                if (!entry.endsWith(".class") || M3ArchiveIndex.isMetadataClass(entry)) {
                    continue;
                }
                byte[] bytes = Files.readAllBytes(file);
                M3ClassInfo info = M3ClassFileParser.parse(bytes, input.id(), entry);
                requireEntryMatchesBinaryName(entry, info.binaryName());
                if (result.putIfAbsent(info.binaryName(), info) != null) {
                    throw new IOException("Duplicate class definition inside " + input.path() + ": "
                            + info.binaryName());
                }
            }
        }
        return new ScannedInput(result, providers);
    }

    private static ScannedInput scanArchive(Input input) throws IOException {
        M3ArchiveIndex index = M3ArchiveIndex.read(input.path());
        Map<String, M3ClassInfo> result = new TreeMap<>();
        Set<String> providers = new TreeSet<>();
        try (ZipFile zip = new ZipFile(input.path().toFile())) {
            for (String resource : index.resourceEntries()) {
                if (!resource.startsWith(SERVICES_PREFIX)) {
                    continue;
                }
                ZipEntry entry = zip.getEntry(resource);
                if (entry == null) {
                    throw new IOException("Archive entry disappeared: " + resource);
                }
                try (var stream = zip.getInputStream(entry)) {
                    providers.addAll(readProviders(new String(stream.readAllBytes(), StandardCharsets.UTF_8), resource));
                }
            }
            for (Map.Entry<String, String> selectedEntry : index.selectedClassEntries().entrySet()) {
                ZipEntry entry = zip.getEntry(selectedEntry.getValue());
                if (entry == null) {
                    throw new IOException("Archive entry disappeared: " + selectedEntry.getValue());
                }
                byte[] bytes;
                try (var stream = zip.getInputStream(entry)) {
                    bytes = stream.readAllBytes();
                }
                M3ClassInfo info = M3ClassFileParser.parse(bytes, input.id(), selectedEntry.getKey());
                requireEntryMatchesBinaryName(selectedEntry.getKey(), info.binaryName());
                if (result.putIfAbsent(info.binaryName(), info) != null) {
                    throw new IOException("Duplicate class definition inside " + input.path() + ": "
                            + info.binaryName());
                }
            }
        }
        return new ScannedInput(result, providers);
    }

    private static Set<String> readProviders(String text, String source) throws IOException {
        Set<String> providers = new TreeSet<>();
        for (String line : text.lines().toList()) {
            int comment = line.indexOf('#');
            String provider = (comment < 0 ? line : line.substring(0, comment)).trim();
            if (provider.isEmpty()) {
                continue;
            }
            if (!M3Names.isBinaryName(provider)) {
                throw new IOException("Invalid service provider in " + source + ": " + provider);
            }
            providers.add(provider);
        }
        return providers;
    }

    private static void requireEntryMatchesBinaryName(String entry, String binaryName) throws IOException {
        String expected = entry.substring(0, entry.length() - ".class".length()).replace('/', '.');
        if (!expected.equals(binaryName)) {
            throw new IOException("Class entry/name mismatch: " + entry + " declares " + binaryName);
        }
    }
}
