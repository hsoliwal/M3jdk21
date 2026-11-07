/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Optional process adapter for an independently installed GraalVM Native Image tool. */
public final class M3NativeImageBackend implements M3ImageBackend {
    /** {@inheritDoc} */
    @Override
    public M3BackendKind kind() {
        return M3BackendKind.NATIVE_IMAGE;
    }

    /** {@inheritDoc} */
    @Override
    public M3ImageBuildResult build(M3ImagePlan plan, M3JdkTools tools, M3BuildOptions options, Path output)
            throws IOException, InterruptedException {
        Path executable = resolveExecutable(tools, options.nativeImageExecutable());
        Path image = M3Files.freshDirectory(output);
        Path bin = image.resolve("bin");
        Files.createDirectories(bin);
        Path nativeOutput = bin.resolve(options.imageName() + executableSuffix());
        List<String> command = new ArrayList<>();
        command.add(executable.toString());
        if (plan.mode() == M3Mode.CLOSED_WORLD) {
            command.add("--no-fallback");
        }
        command.addAll(options.backendArguments());
        command.add("-cp");
        command.add(plan.inputs().stream().map(input -> input.path().toString())
                .collect(java.util.stream.Collectors.joining(File.pathSeparator)));
        command.add("-o");
        command.add(nativeOutput.toString());
        command.add(plan.mainClass());
        M3Process.Result process = M3Process.requireSuccess(command, image, options.timeout(), Map.of());
        M3Files.writeUtf8(image.resolve("native-image.stdout.log"), process.stdout());
        M3Files.writeUtf8(image.resolve("native-image.stderr.log"), process.stderr());
        if (!Files.isRegularFile(nativeOutput)) {
            throw new IOException("Native Image reported success but did not create " + nativeOutput);
        }
        M3Files.writeUtf8(image.resolve("M3_BUILD_RECEIPT.properties"),
                "schema=1\n"
                        + "backend=" + kind().name() + "\n"
                        + "backendStatus=PASS\n"
                        + "imageName=" + options.imageName() + "\n"
                        + "planSha256=" + plan.planSha256() + "\n"
                        + "javaRuntimeIdentity=" + tools.runtimeIdentity() + "\n"
                        + "nativeImageExecutableSha256=" + M3Hashing.sha256(executable) + "\n"
                        + "mode=" + plan.mode().name() + "\n");
        String imageHash = M3ImageManifest.seal(image);
        M3ImageManifest.verify(image);
        return new M3ImageBuildResult(image, imageHash, kind(), "PASS", false);
    }

    private static Path resolveExecutable(M3JdkTools tools, Path explicit) throws IOException {
        Path candidate = explicit;
        if (candidate == null) {
            candidate = tools.javaHome().resolve("bin").resolve("native-image" + executableSuffix());
        }
        candidate = candidate.toAbsolutePath().normalize();
        if (!Files.isRegularFile(candidate) || !Files.isExecutable(candidate)) {
            throw new IOException("Native Image executable is missing or not executable: " + candidate);
        }
        return candidate;
    }

    private static String executableSuffix() {
        return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win") ? ".exe" : "";
    }
}
