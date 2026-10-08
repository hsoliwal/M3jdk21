// SPDX-License-Identifier: Apache-2.0
package com.m3.synexia.importer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/** Operational Git seal for real Synexia source checkouts. */
final class SynexiaGitCheckout {
    private static final int MAX_OUTPUT_BYTES = 512 * 1024;
    private static final int TRACKED_PATH_BATCH = 32;
    private static final long TIMEOUT_SECONDS = 10L;

    private SynexiaGitCheckout() {}

    static void requireExactCleanHead(Path repositoryRoot, String expectedRevision) {
        requireExactCleanHead(repositoryRoot, expectedRevision, List.of());
    }

    static void requireExactCleanHead(
            Path repositoryRoot, String expectedRevision, List<String> sourcePaths) {
        Path root = Objects.requireNonNull(repositoryRoot, "repositoryRoot")
                .toAbsolutePath()
                .normalize();
        String expected = Objects.requireNonNull(expectedRevision, "expectedRevision");
        if (!expected.matches("(?:[0-9a-f]{40}|[0-9a-f]{64})")) {
            throw new IllegalArgumentException("sourceRevision must be an exact Git object id");
        }
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(root)) {
            throw new IllegalArgumentException("repositoryRoot must be a real directory");
        }

        String actual = run(root, "rev-parse", "--verify", "HEAD").trim();
        if (!expected.equals(actual)) {
            throw new IllegalStateException(
                    "Synexia checkout revision mismatch: expected="
                            + expected
                            + " actual="
                            + actual);
        }
        String status = run(root, "status", "--porcelain=v1", "--untracked-files=no");
        if (!status.isBlank()) {
            throw new IllegalStateException("Synexia checkout has tracked source drift");
        }
        requireTrackedPaths(root, sourcePaths);
    }

    private static void requireTrackedPaths(Path root, List<String> suppliedPaths) {
        List<String> paths =
                Objects.requireNonNull(suppliedPaths, "sourcePaths").stream()
                        .map(SynexiaGitCheckout::relativePath)
                        .distinct()
                        .sorted()
                        .toList();
        for (int start = 0; start < paths.size(); start += TRACKED_PATH_BATCH) {
            int end = Math.min(paths.size(), start + TRACKED_PATH_BATCH);
            List<String> command = new ArrayList<>();
            command.add("ls-files");
            command.add("--error-unmatch");
            command.add("--");
            command.addAll(paths.subList(start, end));
            run(root, command.toArray(String[]::new));
        }
    }

    private static String relativePath(String value) {
        String checked = Objects.requireNonNull(value, "sourcePath");
        if (checked.isBlank()
                || checked.startsWith("/")
                || checked.indexOf('\\') >= 0
                || checked.matches("^[A-Za-z]:.*")) {
            throw new IllegalArgumentException("invalid sourcePath");
        }
        for (String part : checked.split("/", -1)) {
            if (part.isEmpty()
                    || ".".equals(part)
                    || "..".equals(part)
                    || ".git".equalsIgnoreCase(part)) {
                throw new IllegalArgumentException("invalid sourcePath");
            }
        }
        return checked;
    }

    private static String run(Path root, String... args) {
        String[] command = new String[args.length + 3];
        command[0] = "git";
        command[1] = "-C";
        command[2] = root.toString();
        System.arraycopy(args, 0, command, 3, args.length);
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            byte[] output = process.getInputStream().readNBytes(MAX_OUTPUT_BYTES + 1);
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("git command timed out");
            }
            if (output.length > MAX_OUTPUT_BYTES) {
                throw new IllegalStateException("git command output exceeded budget");
            }
            String text = new String(output, StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                throw new IllegalStateException("git command failed: " + text.strip());
            }
            return text;
        } catch (IOException failure) {
            throw new IllegalStateException("cannot execute git checkout verification", failure);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("git checkout verification interrupted", interrupted);
        }
    }
}
