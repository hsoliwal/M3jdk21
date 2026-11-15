/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary Exercise portable process launch, shell fallback, streams and errors
 * @requires (os.family == "linux")
 * @run main/othervm -Djdk.lang.Process.launchMechanism=POSIX_SPAWN AndroidProcessRuntimeChecks
 * @run main/othervm -Djdk.lang.Process.launchMechanism=FORK AndroidProcessRuntimeChecks
 */

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AndroidProcessRuntimeChecks {
    private static final String SHELL = Files.isExecutable(Path.of("/system/bin/sh"))
            ? "/system/bin/sh" : "/bin/sh";
    private static int checks;

    private static void equal(Object actual, Object expected) {
        if (!actual.equals(expected)) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
        checks++;
    }

    private static String text(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static int finish(Process process) throws Exception {
        if (!process.waitFor(20, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new AssertionError("Child process did not finish");
        }
        return process.exitValue();
    }

    private static void failure(ProcessBuilder builder) throws Exception {
        try {
            Process unexpected = builder.start();
            unexpected.destroyForcibly();
            finish(unexpected);
            throw new AssertionError("Launch unexpectedly succeeded: " + builder.command());
        } catch (IOException expected) {
            if (expected.getMessage() == null || expected.getMessage().isEmpty()) {
                throw new AssertionError("Launch failure lost its error message", expected);
            }
            checks++;
        }
    }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("android-process-\u03bb-");
        try {
            ProcessBuilder builder = new ProcessBuilder(SHELL, "-c",
                    "printf '%s\\n' \"$1\" \"$PROCESS_TEST_VALUE\"; printf error >&2; exit 37",
                    "arg-zero", "argument \u03bb \u4e2d");
            builder.environment().put("PROCESS_TEST_VALUE", "environment \u03bb \u4e2d");
            Process child = builder.start();
            child.getOutputStream().close();
            equal(text(child.getInputStream().readAllBytes()),
                    "argument \u03bb \u4e2d\nenvironment \u03bb \u4e2d\n");
            equal(text(child.getErrorStream().readAllBytes()), "error");
            equal(finish(child), 37);

            child = new ProcessBuilder(SHELL, "-c", "pwd").directory(directory.toFile()).start();
            child.getOutputStream().close();
            equal(text(child.getInputStream().readAllBytes()).strip(), directory.toRealPath().toString());
            equal(finish(child), 0);

            child = new ProcessBuilder(SHELL, "-c", "IFS= read -r value; printf '%s' \"$value\"").start();
            child.getOutputStream().write("pipe \u03bb\n".getBytes(StandardCharsets.UTF_8));
            child.getOutputStream().close();
            equal(text(child.getInputStream().readAllBytes()), "pipe \u03bb");
            equal(finish(child), 0);

            child = new ProcessBuilder(SHELL, "-c", "printf out; printf err >&2")
                    .redirectErrorStream(true).start();
            child.getOutputStream().close();
            equal(text(child.getInputStream().readAllBytes()), "outerr");
            equal(text(child.getErrorStream().readAllBytes()), "");
            equal(finish(child), 0);

            Path input = Files.writeString(directory.resolve("input"), "redirect-input\n");
            Path output = directory.resolve("output");
            Path error = directory.resolve("error");
            builder = new ProcessBuilder(SHELL, "-c",
                    "IFS= read -r value; printf '%s' \"$value\"; printf err >&2")
                    .redirectInput(input.toFile()).redirectOutput(output.toFile())
                    .redirectError(error.toFile());
            equal(finish(builder.start()), 0);
            equal(Files.readString(output), "redirect-input");
            equal(Files.readString(error), "err");
            equal(finish(new ProcessBuilder(SHELL, "-c", "printf appended")
                    .redirectOutput(ProcessBuilder.Redirect.appendTo(output.toFile())).start()), 0);
            equal(Files.readString(output), "redirect-inputappended");

            // An executable script without a shebang must use the platform shell.
            Path script = Files.writeString(directory.resolve("script-\u03bb"),
                    "printf '%s' \"$1\"; exit 19\n");
            Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwx------"));
            child = new ProcessBuilder(script.toString(), "script \u03bb").start();
            child.getOutputStream().close();
            equal(text(child.getInputStream().readAllBytes()), "script \u03bb");
            equal(finish(child), 19);

            // Executable lookup uses the launching parent's PATH, not the child's.
            builder = new ProcessBuilder("sh", "-c", "printf path");
            builder.environment().put("PATH", directory.resolve("absent-path").toString());
            child = builder.start();
            child.getOutputStream().close();
            equal(text(child.getInputStream().readAllBytes()), "path");
            equal(finish(child), 0);

            failure(new ProcessBuilder(directory.resolve("missing-command").toString()));
            failure(new ProcessBuilder(SHELL, "-c", "exit 0")
                    .directory(directory.resolve("missing-directory").toFile()));
            Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rw-------"));
            failure(new ProcessBuilder(script.toString()));

            // Mix success and failure to exercise concurrent pipe cleanup.
            try (var executor = Executors.newFixedThreadPool(4)) {
                List<Callable<Boolean>> work = new ArrayList<>();
                for (int i = 0; i < 64; i++) {
                    final int expected = i % 32;
                    work.add(() -> {
                        Process process = new ProcessBuilder(SHELL, "-c", "exit " + expected).start();
                        process.getOutputStream().close();
                        process.getInputStream().close();
                        process.getErrorStream().close();
                        if (finish(process) != expected) return false;
                        try {
                            Process bad = new ProcessBuilder(directory.resolve("absent-" + expected).toString()).start();
                            bad.destroyForcibly();
                            finish(bad);
                            return false;
                        } catch (IOException wanted) {
                            return wanted.getMessage() != null;
                        }
                    });
                }
                for (var result : executor.invokeAll(work)) equal(result.get(), true);
            }
            System.out.println("PASS: " + checks + " process checks, including 64 concurrent success/failure pairs");
        } finally {
            try (var files = Files.walk(directory)) {
                for (Path path : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }
}
