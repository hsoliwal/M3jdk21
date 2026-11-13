/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Standard JDK 21 backend: minimized runtime, deterministic application JARs, and AppCDS. */
public final class M3Jdk21ShiftedBackend implements M3ImageBackend {
    /** {@inheritDoc} */
    @Override
    public M3BackendKind kind() {
        return M3BackendKind.JDK21_SHIFTED;
    }

    /** {@inheritDoc} */
    @Override
    public M3ImageBuildResult build(M3ImagePlan plan, M3JdkTools tools, M3BuildOptions options, Path output)
            throws IOException, InterruptedException {
        Path image = M3Files.freshDirectory(output);
        Path runtime = image.resolve("runtime");
        Path app = image.resolve("app");
        Path lib = app.resolve("lib");
        Path bin = image.resolve("bin");
        Files.createDirectories(lib);
        Files.createDirectories(bin);

        Map<String, String> packagedNames = packageInputs(plan, lib);
        String packagedClassPath = absoluteClassPath(image, plan, packagedNames);
        String modules = String.join(",", plan.modules());
        List<String> jlinkCommand = new ArrayList<>(List.of(
                tools.jlink().toString(),
                "--add-modules", modules,
                "--strip-debug",
                "--no-header-files",
                "--no-man-pages",
                "--compress=zip-6",
                "--output", runtime.toString()));
        jlinkCommand.addAll(options.backendArguments());
        M3Process.Result jlink = M3Process.requireSuccess(jlinkCommand, image, options.timeout(), Map.of());
        M3Files.writeUtf8(image.resolve("jlink.stdout.log"), jlink.stdout());
        M3Files.writeUtf8(image.resolve("jlink.stderr.log"), jlink.stderr());

        List<String> applicationClasses = plan.profile().loadedClassRanks().entrySet().stream()
                .filter(entry -> plan.classes().containsKey(entry.getKey()))
                .filter(entry -> plan.roles().get(entry.getKey()) != M3ClassRole.UNREACHABLE)
                .sorted(Map.Entry.<String, Long>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey).distinct().toList();
        StringBuilder classListText = new StringBuilder();
        applicationClasses.forEach(name -> classListText.append(name.replace('.', '/')).append('\n'));
        Path classList = app.resolve("application.classlist");
        M3Files.writeUtf8(classList, classListText.toString());

        String appCdsStatus = "NOT_RUN_EMPTY_CLASSLIST";
        Path archive = app.resolve("application.jsa");
        if (!applicationClasses.isEmpty()) {
            Path runtimeJava = runtime.resolve("bin").resolve(executableName("java"));
            List<String> dumpCommand = List.of(
                    runtimeJava.toString(),
                    "-Xshare:dump",
                    "-XX:SharedClassListFile=" + classList,
                    "-XX:SharedArchiveFile=" + archive,
                    "-cp", packagedClassPath);
            M3Process.Result dump = M3Process.run(dumpCommand, image, options.timeout(), Map.of());
            M3Files.writeUtf8(image.resolve("appcds.stdout.log"), dump.stdout());
            M3Files.writeUtf8(image.resolve("appcds.stderr.log"), dump.stderr());
            if (dump.exitCode() == 0 && Files.isRegularFile(archive)) {
                appCdsStatus = "PASS";
            } else {
                Files.deleteIfExists(archive);
                appCdsStatus = "FALLBACK_EXIT_" + dump.exitCode();
                if (plan.mode() == M3Mode.CLOSED_WORLD) {
                    throw new IOException("AppCDS generation failed in CLOSED_WORLD mode\nstdout:\n"
                            + dump.stdout() + "\nstderr:\n" + dump.stderr());
                }
            }
        }

        writeLaunchers(plan, options.imageName(), image, archive, packagedNames);
        StringBuilder packaged = new StringBuilder("input_id\tpackaged_path\n");
        for (M3ClassPathScanner.Input input : plan.inputs()) {
            packaged.append(input.id()).append('\t').append(packagedNames.get(input.id())).append('\n');
        }
        M3Files.writeUtf8(app.resolve("packaged-inputs.tsv"), packaged.toString());
        String pruning = plan.mode() == M3Mode.CLOSED_WORLD ? "PASS_REACHABLE_CLASSES_ONLY" : "NOT_RUN_MODE_" + plan.mode();
        M3Files.writeUtf8(image.resolve("M3_BUILD_RECEIPT.properties"),
                "schema=1\n"
                        + "backend=" + kind().name() + "\n"
                        + "backendStatus=PASS\n"
                        + "imageName=" + options.imageName() + "\n"
                        + "planSha256=" + plan.planSha256() + "\n"
                        + "javaRuntimeIdentity=" + tools.runtimeIdentity() + "\n"
                        + "mainClass=" + propertyEscape(plan.mainClass()) + "\n"
                        + "mode=" + plan.mode().name() + "\n"
                        + "appCdsStatus=" + appCdsStatus + "\n"
                        + "applicationClassPruning=" + pruning + "\n"
                        + "classPathOrder=EXPLICIT_PLAN_ORDER\n"
                        + "moduleFallback=" + plan.moduleFallback() + "\n");
        String imageHash = M3ImageManifest.seal(image);
        M3ImageManifest.verify(image);
        return new M3ImageBuildResult(image, imageHash, kind(), "PASS;AppCDS=" + appCdsStatus, false);
    }

    private static Map<String, String> packageInputs(M3ImagePlan plan, Path lib) throws IOException {
        Map<String, Set<String>> admittedByInput = new LinkedHashMap<>();
        for (M3ClassPathScanner.Input input : plan.inputs()) {
            admittedByInput.put(input.id(), new TreeSet<>());
        }
        plan.classes().forEach((name, info) -> {
            if (plan.roles().get(name) != M3ClassRole.UNREACHABLE) {
                admittedByInput.get(info.inputId()).add(info.entryName());
            }
        });

        Map<String, String> names = new LinkedHashMap<>();
        for (M3ClassPathScanner.Input input : plan.inputs()) {
            String base = sanitize(input.logicalName());
            String fileName;
            Path destination;
            if (Files.isDirectory(input.path())) {
                fileName = input.id() + "-" + base + ".jar";
                destination = lib.resolve(fileName);
                if (plan.mode() == M3Mode.CLOSED_WORLD) {
                    M3DeterministicJar.packFilteredDirectory(input.path(), destination,
                            admittedByInput.get(input.id()));
                } else {
                    M3DeterministicJar.pack(input.path(), destination);
                }
            } else if (plan.mode() == M3Mode.CLOSED_WORLD) {
                fileName = input.id() + "-" + ensureJarSuffix(base);
                destination = lib.resolve(fileName);
                M3DeterministicJar.packFilteredArchive(input.path(), destination,
                        admittedByInput.get(input.id()));
            } else {
                fileName = input.id() + "-" + base;
                destination = lib.resolve(fileName);
                Files.copy(input.path(), destination, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES);
            }
            names.put(input.id(), "app/lib/" + fileName);
        }
        return names;
    }

    private static void writeLaunchers(M3ImagePlan plan, String imageName, Path image, Path archive,
                                       Map<String, String> packagedNames) throws IOException {
        String main = plan.mainClass();
        String archiveOption = Files.isRegularFile(archive)
                ? "-XX:SharedArchiveFile=\"$ROOT/app/application.jsa\" " : "";
        String unixClassPath = plan.inputs().stream()
                .map(input -> "$ROOT/" + packagedNames.get(input.id()))
                .collect(java.util.stream.Collectors.joining(":"));
        String unix = "#!/bin/sh\n"
                + "set -eu\n"
                + "ROOT=$(CDPATH= cd -- \"$(dirname -- \"$0\")/..\" && pwd)\n"
                + "exec \"$ROOT/runtime/bin/java\" ${M3_JAVA_OPTS:-} " + archiveOption
                + "-cp \"" + unixClassPath + "\" " + shellQuote(main) + " \"$@\"\n";
        Path unixLauncher = image.resolve("bin").resolve(imageName);
        M3Files.writeUtf8(unixLauncher, unix);
        M3Files.makeExecutable(unixLauncher);

        String windowsArchive = Files.isRegularFile(archive)
                ? "-XX:SharedArchiveFile=\"%ROOT%\\app\\application.jsa\" " : "";
        String windowsClassPath = plan.inputs().stream()
                .map(input -> "%ROOT%\\" + packagedNames.get(input.id()).replace('/', '\\'))
                .collect(java.util.stream.Collectors.joining(";"));
        String cmd = "@echo off\r\n"
                + "setlocal\r\n"
                + "set \"ROOT=%~dp0..\"\r\n"
                + "\"%ROOT%\\runtime\\bin\\java.exe\" %M3_JAVA_OPTS% " + windowsArchive
                + "-cp \"" + windowsClassPath + "\" " + main + " %*\r\n";
        M3Files.writeUtf8(image.resolve("bin").resolve(imageName + ".cmd"), cmd);
    }

    private static String absoluteClassPath(Path image, M3ImagePlan plan, Map<String, String> packagedNames) {
        return plan.inputs().stream()
                .map(input -> image.resolve(packagedNames.get(input.id())).toString())
                .collect(java.util.stream.Collectors.joining(File.pathSeparator));
    }

    private static String executableName(String base) {
        return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win")
                ? base + ".exe" : base;
    }

    private static String sanitize(String value) {
        String sanitized = value.replaceAll("[^A-Za-z0-9._-]", "_");
        return sanitized.isBlank() ? "input.jar" : sanitized;
    }

    private static String ensureJarSuffix(String value) {
        return value.toLowerCase(java.util.Locale.ROOT).endsWith(".jar") ? value : value + ".jar";
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private static String propertyEscape(String value) {
        return value.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r");
    }
}
