/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Command-line entry point for the complete M3 image workflow. */
public final class M3ImageTool {
    private M3ImageTool() {
    }

    /** Runs one command. */
    public static void main(String[] args) throws Exception {
        if (args.length == 0 || args[0].equals("help") || args[0].equals("--help")) {
            usage();
            return;
        }
        String command = args[0].toLowerCase(java.util.Locale.ROOT);
        M3Arguments options = M3Arguments.parse(args, 1);
        switch (command) {
            case "train" -> train(options);
            case "plan" -> plan(options);
            case "build" -> build(options);
            case "all" -> all(options);
            case "verify" -> verify(options);
            case "inspect" -> inspect(options);
            default -> throw new IllegalArgumentException("Unknown command: " + args[0]);
        }
    }

    private static void train(M3Arguments options) throws Exception {
        options.requireOnly("classpath", "main", "out", "java-home", "vm-arg", "timeout-seconds");
        M3JdkTools tools = M3JdkTools.resolve(options.path("java-home", null));
        M3Trainer.Result result = M3Trainer.train(tools, options.require("classpath"),
                requireClassName(options.require("main")), options.all("vm-arg"), options.trailing(),
                options.requirePath("out"), timeout(options));
        System.out.println("TRAINING_DIR=" + result.directory());
        System.out.println("PROFILE_SHA256=" + M3Hashing.sha256(result.profile()));
    }

    private static void plan(M3Arguments options) throws Exception {
        options.requireOnly("classpath", "main", "training", "out", "java-home", "mode",
                "dynamic-root", "allow-feature");
        M3JdkTools tools = M3JdkTools.resolve(options.path("java-home", null));
        M3Profile profile = trainingProfile(options.path("training", null));
        M3ImagePlan plan = M3ImagePlanner.plan(tools,
                M3ClassPathScanner.parseClassPath(options.require("classpath")),
                requireClassName(options.require("main")),
                M3Mode.parse(options.single("mode", "hybrid")), profile,
                classNames(options.all("dynamic-root")), features(options.all("allow-feature")),
                options.requirePath("out"));
        System.out.println("PLAN_DIR=" + plan.directory());
        System.out.println("PLAN_SHA256=" + plan.planSha256());
        System.out.println("CLASSES=" + plan.classes().size());
        System.out.println("FEATURES=" + plan.features().size());
    }

    private static void build(M3Arguments options) throws Exception {
        options.requireOnly("plan", "out", "java-home", "backend", "image-name", "cache",
                "native-image", "timeout-seconds", "backend-arg", "allow-jdk-drift");
        M3JdkTools tools = M3JdkTools.resolve(options.path("java-home", null));
        M3ImagePlan plan = M3PlanIO.read(options.requirePath("plan"));
        requireJdkIdentity(plan, tools, options.flag("allow-jdk-drift"));
        M3BuildOptions buildOptions = buildOptions(options);
        M3ImageBuildResult result = M3ImageBuilder.build(plan, tools,
                M3BackendKind.parse(options.single("backend", "jdk21-shifted")), buildOptions,
                options.requirePath("out"));
        printBuild(result);
    }

    private static void all(M3Arguments options) throws Exception {
        options.requireOnly("classpath", "main", "out", "java-home", "vm-arg", "timeout-seconds",
                "mode", "dynamic-root", "allow-feature", "backend", "image-name", "cache",
                "native-image", "backend-arg", "no-run");
        Path root = options.requirePath("out");
        M3JdkTools tools = M3JdkTools.resolve(options.path("java-home", null));
        String classPath = options.require("classpath");
        String mainClass = requireClassName(options.require("main"));
        Duration timeout = timeout(options);
        M3Trainer.Result training = M3Trainer.train(tools, classPath, mainClass,
                options.all("vm-arg"), options.trailing(), root.resolve("training"), timeout);
        M3ImagePlan plan = M3ImagePlanner.plan(tools, M3ClassPathScanner.parseClassPath(classPath),
                mainClass, M3Mode.parse(options.single("mode", "hybrid")), training.normalizedProfile(),
                classNames(options.all("dynamic-root")), features(options.all("allow-feature")),
                root.resolve("plan"));
        M3ImageBuildResult built = M3ImageBuilder.build(plan, tools,
                M3BackendKind.parse(options.single("backend", "jdk21-shifted")),
                buildOptions(options), root.resolve("image"));
        M3ImageVerifier.Result verified = M3ImageVerifier.verify(built.directory(), !options.flag("no-run"),
                options.trailing(), timeout);
        if (verified.runtimeExecuted() && verified.runtimeExitCode() != 0) {
            throw new IOException("Packaged application exited " + verified.runtimeExitCode()
                    + "\nstdout:\n" + verified.stdout() + "\nstderr:\n" + verified.stderr());
        }
        System.out.println("TRAINING_DIR=" + training.directory());
        System.out.println("PLAN_SHA256=" + plan.planSha256());
        printBuild(built);
        System.out.println("VERIFY_RUNTIME_EXECUTED=" + verified.runtimeExecuted());
        if (verified.runtimeExecuted()) {
            System.out.print(verified.stdout());
            System.err.print(verified.stderr());
        }
    }

    private static void verify(M3Arguments options) throws Exception {
        options.requireOnly("image", "run", "timeout-seconds");
        M3ImageVerifier.Result result = M3ImageVerifier.verify(options.requirePath("image"),
                options.flag("run"), options.trailing(), timeout(options));
        System.out.println("IMAGE_SHA256=" + result.imageSha256());
        System.out.println("RUNTIME_EXECUTED=" + result.runtimeExecuted());
        if (result.runtimeExecuted()) {
            System.out.println("RUNTIME_EXIT=" + result.runtimeExitCode());
            System.out.print(result.stdout());
            System.err.print(result.stderr());
            if (result.runtimeExitCode() != 0) {
                throw new IOException("Image runtime verification failed");
            }
        }
    }

    private static void inspect(M3Arguments options) throws Exception {
        options.requireOnly("plan");
        M3ImagePlan plan = M3PlanIO.read(options.requirePath("plan"));
        long candidates = plan.roles().values().stream().filter(role -> role == M3ClassRole.AOT_CANDIDATE).count();
        long fallback = plan.roles().values().stream().filter(role -> role == M3ClassRole.BYTECODE_FALLBACK).count();
        long unreachable = plan.roles().values().stream().filter(role -> role == M3ClassRole.UNREACHABLE).count();
        long unresolved = plan.features().stream().filter(feature -> !feature.resolved()).count();
        System.out.println("PLAN_SHA256=" + plan.planSha256());
        System.out.println("MODE=" + plan.mode());
        System.out.println("INPUTS=" + plan.inputs().size());
        System.out.println("CLASSES=" + plan.classes().size());
        System.out.println("AOT_CANDIDATES=" + candidates);
        System.out.println("BYTECODE_FALLBACK=" + fallback);
        System.out.println("UNREACHABLE=" + unreachable);
        System.out.println("DYNAMIC_FEATURES=" + plan.features().size());
        System.out.println("UNRESOLVED_FEATURES=" + unresolved);
        System.out.println("MODULES=" + String.join(",", plan.modules()));
    }

    private static M3BuildOptions buildOptions(M3Arguments options) {
        return new M3BuildOptions(options.single("image-name", "application"),
                options.path("cache", null), options.path("native-image", null),
                timeout(options), options.all("backend-arg"));
    }

    private static M3Profile trainingProfile(Path training) throws IOException {
        if (training == null) {
            return M3Profile.empty();
        }
        M3TrainingManifest.verify(training);
        return M3ProfileIO.read(training.resolve("profile.tsv"));
    }

    private static Set<String> classNames(List<String> values) {
        Set<String> result = new HashSet<>();
        values.forEach(value -> result.add(requireClassName(value)));
        return result;
    }

    private static Set<M3FeatureKind> features(List<String> values) {
        Set<M3FeatureKind> result = EnumSet.noneOf(M3FeatureKind.class);
        values.forEach(value -> result.add(M3FeatureKind.valueOf(
                value.trim().toUpperCase(java.util.Locale.ROOT).replace('-', '_'))));
        return result;
    }

    private static String requireClassName(String value) {
        return M3Names.requireBinaryName(value, "binary class name");
    }

    private static Duration timeout(M3Arguments options) {
        String value = options.single("timeout-seconds", "300");
        try {
            long seconds = Long.parseLong(value);
            if (seconds < 1 || seconds > 86_400) {
                throw new IllegalArgumentException("timeout-seconds must be 1..86400");
            }
            return Duration.ofSeconds(seconds);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid timeout-seconds: " + value, exception);
        }
    }

    private static void requireJdkIdentity(M3ImagePlan plan, M3JdkTools tools, boolean allowDrift) {
        if (!allowDrift && !plan.javaRuntimeIdentity().equals(tools.runtimeIdentity())) {
            throw new IllegalStateException("Plan JDK identity differs from build JDK; use --allow-jdk-drift only with independent qualification");
        }
    }

    private static void printBuild(M3ImageBuildResult result) {
        System.out.println("IMAGE_DIR=" + result.directory());
        System.out.println("IMAGE_SHA256=" + result.imageSha256());
        System.out.println("BACKEND=" + result.backend());
        System.out.println("BACKEND_STATUS=" + result.backendStatus());
        System.out.println("CACHE_HIT=" + result.cacheHit());
    }

    private static void usage() {
        System.out.println("M3 Image Pipeline (Java 21)\n"
                + "  train   --classpath=<cp> --main=<class> --out=<dir> [--vm-arg=<arg>] -- [app args]\n"
                + "  plan    --classpath=<cp> --main=<class> --training=<dir> --out=<dir> [--mode=hybrid]\n"
                + "  build   --plan=<dir> --out=<dir> [--backend=jdk21-shifted] [--cache=<dir>]\n"
                + "  all     --classpath=<cp> --main=<class> --out=<dir> [options] -- [app args]\n"
                + "  verify  --image=<dir> [--run] -- [app args]\n"
                + "  inspect --plan=<dir>");
    }
}
