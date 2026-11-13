/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Bounded external-process execution with retained logs. */
public final class M3Process {
    /** Process result. */
    public record Result(int exitCode, String stdout, String stderr, List<String> command) {
    }

    private M3Process() {
    }

    /** Runs a command and captures output. */
    public static Result run(List<String> command, Path workingDirectory, Duration timeout,
                             Map<String, String> environment) throws IOException, InterruptedException {
        if (command.isEmpty()) {
            throw new IllegalArgumentException("Command must not be empty");
        }
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("Process timeout must be positive");
        }
        Path work = workingDirectory.toAbsolutePath().normalize();
        if (!Files.isDirectory(work)) {
            throw new IOException("Process working directory is missing: " + work);
        }
        ProcessBuilder builder = new ProcessBuilder(new ArrayList<>(command));
        builder.directory(work.toFile());
        builder.environment().putAll(environment);
        Path stdoutFile = Files.createTempFile("m3-process-", ".stdout");
        Path stderrFile = Files.createTempFile("m3-process-", ".stderr");
        try {
            builder.redirectOutput(stdoutFile.toFile());
            builder.redirectError(stderrFile.toFile());
            Process process = builder.start();
            boolean completed = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!completed) {
                process.destroy();
                if (!process.waitFor(2, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    process.waitFor(2, TimeUnit.SECONDS);
                }
                String stdout = Files.readString(stdoutFile, StandardCharsets.UTF_8);
                String stderr = Files.readString(stderrFile, StandardCharsets.UTF_8);
                throw new IOException("Command timed out after " + timeout + ": " + command
                        + "\nstdout:\n" + stdout + "\nstderr:\n" + stderr);
            }
            String stdout = Files.readString(stdoutFile, StandardCharsets.UTF_8);
            String stderr = Files.readString(stderrFile, StandardCharsets.UTF_8);
            return new Result(process.exitValue(), stdout, stderr, List.copyOf(command));
        } finally {
            Files.deleteIfExists(stdoutFile);
            Files.deleteIfExists(stderrFile);
        }
    }

    /** Runs a command and rejects nonzero exit status. */
    public static Result requireSuccess(List<String> command, Path workingDirectory, Duration timeout,
                                        Map<String, String> environment) throws IOException, InterruptedException {
        Result result = run(command, workingDirectory, timeout, environment);
        if (result.exitCode() != 0) {
            throw new IOException("Command failed with exit " + result.exitCode() + ": " + command
                    + "\nstdout:\n" + result.stdout() + "\nstderr:\n" + result.stderr());
        }
        return result;
    }
}
