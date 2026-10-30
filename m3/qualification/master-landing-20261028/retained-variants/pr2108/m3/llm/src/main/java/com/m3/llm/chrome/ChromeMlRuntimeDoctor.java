// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package com.m3.llm.chrome;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

/** Explicit operator-only audit/smoke. Never downloads, discovers profiles, or starts an HTTP server. */
public final class ChromeMlRuntimeDoctor {
    private static final Set<String> KEYS = Set.of("workingDirectory", "runner", "chromeMlDll", "dawnDll",
            "weights", "runner.sha256", "chromeMlDll.sha256", "dawnDll.sha256", "weights.sha256");
    private static final long MAX_ASSET_BYTES = 32L * 1024 * 1024 * 1024;

    private ChromeMlRuntimeDoctor() { }

    public static void main(String[] args) throws Exception { run(args, System.out); }

    public static void run(String[] args, PrintStream output) throws Exception {
        if (args.length == 0 || (args.length == 1 && "--help".equals(args[0]))) {
            output.println("ChromeML runtime doctor (Java 21)");
            output.println("audit <explicit-config.properties>");
            output.println("smoke <explicit-config.properties> --execute-native [--backend cpu|gpu]");
            output.println("No model downloads, remote calls, profile discovery or server startup.");
            return;
        }
        boolean smoke = (args.length == 3 || args.length == 5)
                && "smoke".equals(args[0]) && "--execute-native".equals(args[2]);
        ChromeMlBackend backend = ChromeMlBackend.GPU;
        if (args.length == 5) {
            if (!"--backend".equals(args[3])) { throw new IllegalArgumentException("BACKEND_OPTION_REQUIRED"); }
            backend = ChromeMlBackend.parse(args[4]);
        }
        if (!smoke && !(args.length == 2 && "audit".equals(args[0]))) {
            throw new IllegalArgumentException("EXPLICIT_AUDIT_OR_SMOKE_REQUIRED");
        }
        if (smoke && (!System.getProperty("os.name").startsWith("Windows")
                || !Set.of("amd64", "x86_64").contains(System.getProperty("os.arch")))) {
            throw new IOException("NATIVE_WINDOWS_X64_REQUIRED");
        }
        Config config = load(Path.of(args[1]));
        List<Stamp> before = stamps(config.layout());
        var identity = config.layout().validate(config.hashes());
        requireUnchanged(before, stamps(config.layout()));
        output.println("schema=CHROMEML-RUNTIME-DOCTOR/1");
        output.println("assetState=BYTES_MATCH_ONLY");
        output.println("assetRoot=" + identity.root());
        if (smoke) {
            try (var runner = new ChromeMlProcessRunner(config.layout(), config.hashes(), Duration.ofSeconds(120), 1_048_576, backend)) {
                var request = new ChromeMlRunnerRequest("Say exactly CHROMEML_READY.", List.of(), null,
                        16000, 64, null, null, null, false, false, false);
                var result = runner.generate(request);
                requireUnchanged(before, stamps(config.layout()));
                output.println("nativeState=INFERENCE_COMPLETED");
                output.println("sentinelMatch=" + "CHROMEML_READY".equals(result.text().strip()));
                output.println("outputSha256=" + sha256(result.text()));
                output.println("authority=CANDIDATE_ONLY");
            }
        } else {
            output.println("nativeState=NOT_EXECUTED");
        }
        output.println("gpuState=NOT_OBSERVED");
        output.println("gpuRequested=" + (smoke && backend == ChromeMlBackend.GPU));
        output.println("requestedBackend=" + (smoke ? backend.name() : "NONE"));
    }

    private record Config(ChromeMlRuntimeLayout layout, ChromeMlRuntimeLayout.ExpectedHashes hashes) { }
    private record Stamp(Path path, long size, Object key, java.nio.file.attribute.FileTime modified) { }

    private static Config load(Path path) throws IOException {
        rejectLinks(path.toAbsolutePath().normalize());
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw new IOException("REGULAR_CONFIG_REQUIRED");
        byte[] bytes;
        try (var input = Files.newInputStream(path)) {
            bytes = input.readNBytes(65537);
        }
        if (bytes.length > 65536) throw new IOException("CONFIG_TOO_LARGE");
        Properties properties = new UniqueProperties();
        properties.load(new ByteArrayInputStream(bytes));
        if (!properties.stringPropertyNames().equals(KEYS)) throw new IOException("EXACT_CONFIG_KEYS_REQUIRED");
        var layout = new ChromeMlRuntimeLayout(explicitPath(properties, "workingDirectory"),
                explicitPath(properties, "runner"), explicitPath(properties, "chromeMlDll"),
                explicitPath(properties, "dawnDll"), explicitPath(properties, "weights"));
        var hashes = new ChromeMlRuntimeLayout.ExpectedHashes(requiredHash(properties, "runner"),
                requiredHash(properties, "chromeMlDll"), requiredHash(properties, "dawnDll"),
                requiredHash(properties, "weights"));
        return new Config(layout, hashes);
    }

    private static Path explicitPath(Properties properties, String key) throws IOException {
        Path value = Path.of(properties.getProperty(key));
        if (!value.isAbsolute()) throw new IOException("ABSOLUTE_PATH_REQUIRED:" + key);
        return value.normalize();
    }

    private static String requiredHash(Properties properties, String key) throws IOException {
        String hash = properties.getProperty(key + ".sha256").toLowerCase(Locale.ROOT);
        if (!hash.matches("[0-9a-f]{64}")) throw new IOException("EXPLICIT_SHA256_REQUIRED:" + key);
        return hash;
    }

    private static List<Stamp> stamps(ChromeMlRuntimeLayout layout) throws IOException {
        rejectLinks(layout.workingDirectory());
        var result = new java.util.ArrayList<Stamp>();
        for (Path file : List.of(layout.runner(), layout.chromeMlDll(), layout.dawnDll(), layout.weights())) {
            rejectLinks(file);
            BasicFileAttributes a = Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!a.isRegularFile() || a.size() == 0 || a.size() > MAX_ASSET_BYTES) {
                throw new IOException("INVALID_ASSET_SIZE_OR_TYPE");
            }
            result.add(new Stamp(file, a.size(), a.fileKey(), a.lastModifiedTime()));
        }
        return List.copyOf(result);
    }

    private static void rejectLinks(Path path) throws IOException {
        Path cursor = path.getRoot();
        for (Path component : path) {
            cursor = cursor.resolve(component);
            if (Files.isSymbolicLink(cursor)) throw new IOException("SYMLINK_NOT_ALLOWED");
        }
    }

    private static void requireUnchanged(List<Stamp> before, List<Stamp> after) throws IOException {
        if (!Objects.equals(before, after)) throw new IOException("ASSETS_CHANGED_DURING_AUDIT_OR_SMOKE");
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException failure) { throw new IllegalStateException(failure); }
    }

    private static final class UniqueProperties extends Properties {
        private static final long serialVersionUID = 1L;
        @Override public synchronized Object put(Object key, Object value) {
            if (containsKey(key)) throw new IllegalArgumentException("DUPLICATE_CONFIG_KEY");
            return super.put(key, value);
        }
    }
}
