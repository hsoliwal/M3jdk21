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
import java.util.Properties;

/** Verifies image bytes and, when requested, executes the packaged application. */
public final class M3ImageVerifier {
    /** Verification result. */
    public record Result(String imageSha256, boolean runtimeExecuted, int runtimeExitCode,
                         String stdout, String stderr) {
    }

    private M3ImageVerifier() {
    }

    /** Verifies the complete manifest and optionally runs the launcher. */
    public static Result verify(Path image, boolean execute, List<String> applicationArguments,
                                Duration timeout) throws IOException, InterruptedException {
        Path root = image.toAbsolutePath().normalize();
        String hash = M3ImageManifest.verify(root);
        if (!execute) {
            return new Result(hash, false, -1, "", "");
        }
        Properties receipt = new Properties();
        try (var reader = Files.newBufferedReader(root.resolve("M3_BUILD_RECEIPT.properties"), StandardCharsets.UTF_8)) {
            receipt.load(reader);
        }
        M3BackendKind backend = M3BackendKind.valueOf(receipt.getProperty("backend"));
        String imageName = receipt.getProperty("imageName");
        if (imageName == null || imageName.isBlank()) {
            throw new IOException("Build receipt has no imageName");
        }
        List<String> command = new ArrayList<>();
        if (backend == M3BackendKind.JDK21_SHIFTED) {
            Path launcher = root.resolve("bin").resolve(imageName);
            if (isWindows()) {
                command.add("cmd.exe");
                command.add("/c");
                command.add(root.resolve("bin").resolve(imageName + ".cmd").toString());
            } else {
                command.add(launcher.toString());
            }
        } else {
            command.add(root.resolve("bin").resolve(imageName + (isWindows() ? ".exe" : "")).toString());
        }
        command.addAll(applicationArguments);
        M3Process.Result process = M3Process.run(command, root, timeout, Map.of());
        return new Result(hash, true, process.exitCode(), process.stdout(), process.stderr());
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win");
    }
}
