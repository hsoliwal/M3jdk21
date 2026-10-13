// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.vendor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** Read-only git plumbing: {@code git -C <repo> show <base>:<path>} and {@code rev-parse}; never writes the repository. */
final class Git {

    private Git() {
    }

    /** The blob bytes, or empty when git exits non-zero (the oracle's {@code POM_NOT_FOUND}). */
    static Optional<byte[]> show(Path repo, String base, String path) {
        return run(repo, "show", base + ":" + path);
    }

    static String commit(Path repo, String base) {
        return run(repo, "rev-parse", "--verify", base + "^{commit}").map(b -> new String(b, StandardCharsets.UTF_8).strip())
                .orElseThrow(() -> new IllegalStateException("base does not resolve to a commit: " + base));
    }

    private static Optional<byte[]> run(Path repo, String... args) {
        List<String> command = Stream.concat(Stream.of("git", "-C", repo.toString()), Stream.of(args)).toList();
        try {
            Process process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            byte[] out = process.getInputStream().readAllBytes();
            return process.waitFor() == 0 ? Optional.of(out) : Optional.empty();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
