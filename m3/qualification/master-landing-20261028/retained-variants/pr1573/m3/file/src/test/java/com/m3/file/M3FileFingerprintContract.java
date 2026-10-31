// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Objects;

/** Standalone executable contracts; test oracles independently hash actual file bytes. */
public final class M3FileFingerprintContract {
    private static int checks;

    private M3FileFingerprintContract() { }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("m3-file-fingerprint-");
        try {
            emptyAndLimits(directory);
            binaryAndMultipleBlocks(directory);
            observedContentAndMetadata(directory);
            realClassOutputs(directory);
            rejectedInputs(directory);
            descriptorContract();
            existingSurface(directory);
            System.out.println("M3_FILE_FINGERPRINT_PASS checks=" + checks);
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
    }

    private static void emptyAndLimits(Path directory) throws Exception {
        Path empty = Files.write(directory.resolve("empty.bin"), new byte[0]);
        M3File file = M3File.of(empty);
        assertFingerprint(file, 0);
        equal(0L, file.fingerprint(0).size(), "empty size");
        equal("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                file.fingerprint(0).sha256(), "published empty SHA-256 vector");
        equal(file.fingerprint(0), file.fingerprint(Long.MAX_VALUE), "maximum long limit");
        Path one = Files.write(directory.resolve("one.bin"), new byte[] {0});
        M3File single = M3File.of(one);
        expect(IOException.class, () -> single.fingerprint(0), "zero limit rejects content");
        assertFingerprint(single, 1);
        equal(single.fingerprint(1), single.fingerprint(Long.MAX_VALUE), "inclusive limit");
        expect(IllegalArgumentException.class, () -> single.fingerprint(-1), "negative limit");
        expect(IllegalArgumentException.class,
                () -> M3File.of(directory.resolve("never-created")).fingerprint(-1),
                "negative limit rejected before opening missing input");
    }

    private static void binaryAndMultipleBlocks(Path directory) throws Exception {
        byte[] bytes = new byte[8192 * 3 + 17];
        for (int index = 0; index < bytes.length; index++) bytes[index] = (byte) (index * 37 + 19);
        bytes[0] = 0;
        bytes[8191] = 0;
        bytes[8192] = (byte) 0xff;
        bytes[16384] = (byte) 0x80;
        Path path = Files.write(directory.resolve("binary.bin"), bytes);
        M3File file = M3File.of(path);
        assertFingerprint(file, bytes.length);
        expect(IOException.class, () -> file.fingerprint(bytes.length - 1L), "last-byte overflow");
        expect(IOException.class, () -> file.fingerprint(8192), "block-boundary overflow");
        equal(file.fingerprint(bytes.length), file.fingerprint(Long.MAX_VALUE), "bounded multi-block parity");
        check(Arrays.equals(bytes, Files.readAllBytes(path)), "fingerprint leaves original binary intact");
    }

    private static void observedContentAndMetadata(Path directory) throws Exception {
        Path path = Files.write(directory.resolve("metadata.bin"), new byte[] {1, 2, 3, 4});
        M3File file = M3File.of(path);
        M3File.Metadata beforeMetadata = file.metadata();
        M3File.Fingerprint before = file.fingerprint(4);
        Files.write(path, new byte[] {1, 2, 7, 4});
        Files.setLastModifiedTime(path, beforeMetadata.lastModified());
        M3File.Metadata afterMetadata = file.metadata();
        equal(beforeMetadata, afterMetadata, "size and mtime intentionally unchanged");
        M3File.Fingerprint after = file.fingerprint(4);
        check(!before.equals(after), "same-size restored-mtime edit detected");
        assertFingerprint(file, 4);

        long oldLength = file.metadata().size();
        Files.write(path, new byte[] {5}, StandardOpenOption.APPEND);
        expect(IOException.class, () -> file.fingerprint(oldLength), "growth beyond prior length rejected");
        assertFingerprint(file, oldLength + 1);
        Files.write(path, new byte[] {9, 8});
        equal(2L, file.fingerprint(2).size(), "observed shrink uses current content");
    }

    private static void realClassOutputs(Path directory) throws Exception {
        Path source = directory.resolve("FingerprintFixture.java");
        Path output = Files.createDirectory(directory.resolve("classes"));
        compile(source, output, 7);
        Path binary = output.resolve("FingerprintFixture.class");
        byte[] original = Files.readAllBytes(binary);
        check(original.length > 4 && ByteBuffer.wrap(original).getInt() == 0xcafebabe,
                "actual javac binary fixture");
        M3File file = M3File.of(binary);
        M3File.Fingerprint first = file.fingerprint(original.length);
        assertFingerprint(file, original.length);
        FileTime originalTime = Files.getLastModifiedTime(binary);
        compile(source, output, 8);
        Files.setLastModifiedTime(binary, originalTime);
        equal((long) original.length, Files.size(binary), "class edit preserves byte count");
        equal(originalTime, Files.getLastModifiedTime(binary), "class edit preserves mtime");
        M3File.Fingerprint second = file.fingerprint(original.length);
        check(!first.sha256().equals(second.sha256()), "class method-byte change detected");
        equal(first.size(), second.size(), "class descriptors retain same size");
        assertFingerprint(file, original.length);
        expect(IOException.class, () -> file.fingerprint(original.length - 1L), "class byte limit");
    }

    private static void compile(Path source, Path output, int value) throws Exception {
        Files.writeString(source, "public final class FingerprintFixture { "
                + "public int value() { return " + value + "; } }", StandardCharsets.UTF_8);
        String executable = System.getProperty("os.name", "").startsWith("Windows") ? "javac.exe" : "javac";
        Path compiler = Path.of(System.getProperty("java.home"), "bin", executable);
        check(Files.isExecutable(compiler), "JDK compiler available");
        Process process = new ProcessBuilder(compiler.toString(), "--release", "21", "-g:none",
                "-d", output.toString(), source.toString()).redirectErrorStream(true).start();
        try {
            byte[] diagnostics;
            try (var compilerOutput = process.getInputStream()) {
                diagnostics = compilerOutput.readAllBytes();
            }
            int result = process.waitFor();
            if (result != 0) throw new AssertionError("fixture compilation failed: "
                    + new String(diagnostics, StandardCharsets.UTF_8));
            checks++;
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }

    private static void rejectedInputs(Path directory) throws Exception {
        expect(IOException.class, () -> M3File.of(directory.resolve("missing.bin")).fingerprint(1),
                "missing input");
        expect(IOException.class, () -> M3File.of(directory).fingerprint(Long.MAX_VALUE),
                "directory input");
        Path target = Files.write(directory.resolve("link-target.bin"), new byte[] {1, 2, 3});
        Path link = Files.createSymbolicLink(directory.resolve("link.bin"), target.getFileName());
        expect(IOException.class, () -> M3File.of(link).fingerprint(3), "symbolic link input");
        equal(3L, Files.size(target), "symlink refusal leaves target unchanged");
        Path failed = Files.write(directory.resolve("failed-read.bin"), new byte[] {1, 2});
        expect(IOException.class, () -> M3File.of(failed).fingerprint(1), "bounded failure");
        Files.delete(failed);
        check(!Files.exists(failed), "failed operation releases input for removal");
    }

    private static void descriptorContract() throws Exception {
        String digest = "0123456789abcdef".repeat(4);
        M3File.Fingerprint first = new M3File.Fingerprint(17, digest);
        M3File.Fingerprint equalValue = new M3File.Fingerprint(17, digest);
        equal(first, equalValue, "descriptor value equality");
        equal(first.hashCode(), equalValue.hashCode(), "descriptor hash equality");
        check(!first.equals(new M3File.Fingerprint(18, digest)), "descriptor length participates");
        expect(IllegalArgumentException.class, () -> new M3File.Fingerprint(-1, digest), "negative size");
        expect(NullPointerException.class, () -> new M3File.Fingerprint(0, null), "null digest");
        expect(IllegalArgumentException.class, () -> new M3File.Fingerprint(0, "0".repeat(63)),
                "short digest");
        expect(IllegalArgumentException.class, () -> new M3File.Fingerprint(0, "0".repeat(65)),
                "long digest");
        expect(IllegalArgumentException.class, () -> new M3File.Fingerprint(0, "A".repeat(64)),
                "noncanonical uppercase digest");
        expect(IllegalArgumentException.class, () -> new M3File.Fingerprint(0, "g".repeat(64)),
                "nonhex digest");
        equal(Long.MAX_VALUE, new M3File.Fingerprint(Long.MAX_VALUE, digest).size(), "long size range");
        var components = M3File.Fingerprint.class.getRecordComponents();
        check(components != null && components.length == 2
                && components[0].getType() == long.class && components[1].getType() == String.class,
                "descriptor retains only length and digest metadata");
    }

    private static void existingSurface(Path directory) throws Exception {
        byte[] bytes = new byte[] {0, 1, (byte) 0xff};
        Path path = Files.write(directory.resolve("existing.bin"), bytes);
        M3File file = M3File.of(path);
        check(file.path() == path, "original Path retained by identity");
        check(file.metadata().regularFile(), "existing metadata");
        check(Arrays.equals(bytes, file.readAllBytes(bytes.length)), "existing bounded read");
        expect(IOException.class, () -> file.readAllBytes(bytes.length - 1), "existing read limit");
        expect(IllegalArgumentException.class, () -> file.readAllBytes(-1), "existing negative limit");
        try (var stream = file.openInputStream()) {
            check(Arrays.equals(bytes, stream.readAllBytes()), "existing stream");
        }
        try (var channel = file.openReadChannel()) {
            ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
            while (buffer.hasRemaining()) check(channel.read(buffer) > 0, "existing channel progress");
            check(Arrays.equals(bytes, buffer.array()), "existing channel");
        }
    }

    private static void assertFingerprint(M3File file, long limit) throws Exception {
        byte[] bytes = Files.readAllBytes(file.path());
        String expected = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        M3File.Fingerprint result = file.fingerprint(limit);
        equal((long) bytes.length, result.size(), "actual byte length");
        equal(expected, result.sha256(), "independent full-byte SHA-256 oracle");
    }

    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
        checks++;
    }

    private static void equal(Object expected, Object actual, String description) {
        check(Objects.equals(expected, actual), description + ": expected=" + expected + ", actual=" + actual);
    }

    private static void expect(Class<? extends Throwable> expected, CheckedAction action,
                               String description) throws Exception {
        try {
            action.run();
        } catch (Throwable failure) {
            if (!expected.isInstance(failure)) throw new AssertionError(description, failure);
            checks++;
            return;
        }
        throw new AssertionError(description + ": expected " + expected.getName());
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }
}
