// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package com.m3.llm.chrome;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Caller-owned runtime identity. File checks are not a substitute for owner-only filesystem ACLs. */
public record ChromeMlRuntimeLayout(Path workingDirectory, Path runner, Path chromeMlDll,
                                    Path dawnDll, Path weights) {
    private static final long CODE_LIMIT = 512L * 1024 * 1024;
    private static final long WEIGHTS_LIMIT = 16L * 1024 * 1024 * 1024;
    private static final Duration AUDIT_LIMIT = Duration.ofMinutes(2);

    public record ExpectedHashes(String runner, String chromeMlDll, String dawnDll, String weights) {
        public ExpectedHashes {
            runner = optionalSha(runner); chromeMlDll = optionalSha(chromeMlDll);
            dawnDll = optionalSha(dawnDll); weights = optionalSha(weights);
        }
        public static ExpectedHashes none() { return new ExpectedHashes("", "", "", ""); }
    }
    public record Identity(String runnerSha256, String chromeMlDllSha256, String dawnDllSha256,
                           String weightsSha256, String root) {
        public Identity {
            runnerSha256 = sha(runnerSha256); chromeMlDllSha256 = sha(chromeMlDllSha256);
            dawnDllSha256 = sha(dawnDllSha256); weightsSha256 = sha(weightsSha256); root = sha(root);
        }
        /** Freeze the audited content for subsequent launch validation. */
        public ExpectedHashes expectedHashes() {
            return new ExpectedHashes(runnerSha256, chromeMlDllSha256, dawnDllSha256, weightsSha256);
        }
    }
    public ChromeMlRuntimeLayout {
        workingDirectory = normalized(workingDirectory, "workingDirectory");
        runner = normalized(runner, "runner"); chromeMlDll = normalized(chromeMlDll, "chromeMlDll");
        dawnDll = normalized(dawnDll, "dawnDll"); weights = normalized(weights, "weights");
    }
    public static ChromeMlRuntimeLayout chrome2apiDefaults(Path root) {
        Path base = normalized(root, "root");
        return new ChromeMlRuntimeLayout(base, base.resolve("runtime/ChromeMLBareRunner.exe"),
                base.resolve("runtime/optimization_guide_internal.dll"), base.resolve("runtime/webgpu_dawn.dll"),
                base.resolve("model/OptGuideOnDeviceModel/2025.8.8.1141/weights.bin"));
    }
    public Identity validate() throws IOException { return validate(ExpectedHashes.none()); }
    public Identity validate(ExpectedHashes expected) throws IOException { return validate(expected, AUDIT_LIMIT); }

    /** Streaming hash audit with a separate total time budget. Never loads a DLL or a model. */
    public Identity validate(ExpectedHashes expected, Duration limit) throws IOException {
        Objects.requireNonNull(expected, "expected"); Objects.requireNonNull(limit, "limit");
        if (limit.isNegative() || limit.isZero() || limit.compareTo(AUDIT_LIMIT) > 0) {
            throw new IllegalArgumentException("Audit limit must be > 0 and <= 2 minutes");
        }
        long started = System.nanoTime(); long budget = limit.toNanos();
        noLinks(workingDirectory);
        ChromeMlPathPolicy.requireDirectoryNoLinks(workingDirectory);
        String runnerHash = hashRegular(runner, CODE_LIMIT, started, budget);
        String chromeHash = hashRegular(chromeMlDll, CODE_LIMIT, started, budget);
        String dawnHash = hashRegular(dawnDll, CODE_LIMIT, started, budget);
        String weightsHash = hashRegular(weights, WEIGHTS_LIMIT, started, budget);
        match(expected.runner(), runnerHash, "runner"); match(expected.chromeMlDll(), chromeHash, "chromeMlDll");
        match(expected.dawnDll(), dawnHash, "dawnDll"); match(expected.weights(), weightsHash, "weights");
        String root = digestFields(List.of(runnerHash, chromeHash, dawnHash, weightsHash));
        return new Identity(runnerHash, chromeHash, dawnHash, weightsHash, root);
    }
    static void noLinks(Path path) throws IOException {
        ChromeMlPathPolicy.rejectSymlinkAncestry(path);
        // Also detects ancestor junctions/reparse targets on filesystems which expose them through realPath.
        if (!path.toRealPath().equals(path)) { throw new IOException("ALIASED_RUNTIME_PATH"); }
    }
    private static Path normalized(Path path, String name) {
        return Objects.requireNonNull(path, name).toAbsolutePath().normalize();
    }
    private static void budget(long started, long nanos) throws IOException {
        if (Thread.currentThread().isInterrupted()) { throw new IOException("RUNTIME_AUDIT_INTERRUPTED"); }
        if (System.nanoTime() - started >= nanos) { throw new IOException("RUNTIME_AUDIT_TIMEOUT"); }
    }
    private static String hashRegular(Path path, long maximum, long started, long nanos) throws IOException {
        budget(started, nanos); noLinks(path);
        BasicFileAttributes before = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!before.isRegularFile() || before.size() < 1 || before.size() > maximum) {
            throw new IOException("RUNTIME_FILE_SIZE_OR_TYPE");
        }
        MessageDigest digest = digest(); ByteBuffer buffer = ByteBuffer.allocate(64 * 1024); long count = 0;
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            while (true) {
                budget(started, nanos); int read = channel.read(buffer);
                if (read < 0) { break; }
                count += read;
                if (count > before.size() || count > maximum) { throw new IOException("RUNTIME_FILE_CHANGED"); }
                buffer.flip(); digest.update(buffer); buffer.clear();
            }
        }
        budget(started, nanos); noLinks(path);
        BasicFileAttributes after = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!after.isRegularFile() || count != before.size() || before.size() != after.size()
                || !before.lastModifiedTime().equals(after.lastModifiedTime())
                || !Objects.equals(before.fileKey(), after.fileKey())) { throw new IOException("RUNTIME_FILE_CHANGED"); }
        return HexFormat.of().formatHex(digest.digest());
    }
    private static String digestFields(List<String> fields) {
        StringBuilder canonical = new StringBuilder("CHROMEML-RUNTIME/1\n");
        fields.forEach(field -> canonical.append(field.length()).append(':').append(field).append(';'));
        return HexFormat.of().formatHex(digest().digest(canonical.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
    static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 unavailable", exception); }
    }
    private static void match(String expected, String actual, String name) throws IOException {
        if (!expected.isEmpty() && !expected.equals(actual)) { throw new IOException("RUNTIME_HASH_MISMATCH:" + name); }
    }
    private static String optionalSha(String value) { return value == null || value.isBlank() ? "" : sha(value); }
    private static String sha(String value) {
        String result = Objects.requireNonNull(value, "sha256").toLowerCase(Locale.ROOT);
        if (!result.matches("[0-9a-f]{64}")) { throw new IllegalArgumentException("SHA-256 required"); }
        return result;
    }
}
