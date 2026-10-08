// SPDX-License-Identifier: Apache-2.0
package com.m3.synexia.importer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/** Operational Git seal for real Synexia source checkouts. */
final class SynexiaGitCheckout {
    private static final int MAX_OUTPUT_BYTES = 8192;
    private static final long TIMEOUT_SECONDS = 10L;

    private SynexiaGitCheckout() {}

    static void requireExactCleanHead(Path repositoryRoot, String expectedRevision) {
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
