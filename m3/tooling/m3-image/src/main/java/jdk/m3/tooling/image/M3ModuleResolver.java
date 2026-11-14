/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Resolves the JDK module closure using the selected toolchain's jdeps. */
public final class M3ModuleResolver {
    /** Module-resolution result. */
    public record Result(Set<String> modules, boolean fallback, String stdout, String stderr) {
        /** Normalizes modules. */
        public Result {
            modules = java.util.Collections.unmodifiableSet(new TreeSet<>(modules));
        }
    }

    private M3ModuleResolver() {
    }

    /** Resolves modules, falling back conservatively outside closed-world mode. */
    public static Result resolve(M3JdkTools tools, List<Path> classPath, M3Mode mode, Path work)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(tools.jdeps().toString());
        if (mode != M3Mode.CLOSED_WORLD) {
            command.add("--ignore-missing-deps");
        }
        command.add("--multi-release");
        command.add("21");
        command.add("--print-module-deps");
        command.add("--class-path");
        command.add(join(classPath));
        classPath.forEach(path -> command.add(path.toString()));
        M3Process.Result result = M3Process.run(command, work, Duration.ofMinutes(2), Map.of());
        Set<String> modules = parseModules(result.stdout());
        if (result.exitCode() == 0 && !modules.isEmpty()) {
            modules.add("java.base");
            return new Result(modules, false, result.stdout(), result.stderr());
        }
        if (mode == M3Mode.CLOSED_WORLD) {
            throw new IOException("jdeps module resolution failed in CLOSED_WORLD mode\nstdout:\n"
                    + result.stdout() + "\nstderr:\n" + result.stderr());
        }
        return new Result(Set.of("java.se", "jdk.unsupported"), true, result.stdout(), result.stderr());
    }

    private static Set<String> parseModules(String stdout) {
        Set<String> modules = new TreeSet<>();
        for (String line : stdout.lines().toList()) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.contains(" ")) {
                continue;
            }
            for (String module : trimmed.split(",")) {
                if (module.matches("[A-Za-z0-9_.]+")) {
                    modules.add(module);
                }
            }
        }
        return modules;
    }

    private static String join(List<Path> paths) {
        return paths.stream().map(Path::toString).collect(java.util.stream.Collectors.joining(File.pathSeparator));
    }
}
