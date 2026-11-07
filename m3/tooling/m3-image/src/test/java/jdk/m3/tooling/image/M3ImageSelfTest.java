/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;

/** Dependency-free executable regression suite. */
public final class M3ImageSelfTest {
    private int checks;

    /** Runs the complete suite and returns the number of checks. */
    public static int runAll() throws Exception {
        M3ImageSelfTest suite = new M3ImageSelfTest();
        suite.profileRoundTrip();
        suite.endToEndPipeline();
        return suite.checks;
    }

    /** Command-line test entry. */
    public static void main(String[] args) throws Exception {
        int checks = runAll();
        System.out.println("M3_IMAGE_SELF_TEST_OK checks=" + checks);
    }

    private void profileRoundTrip() throws Exception {
        Path root = Files.createTempDirectory("m3-profile-test-");
        try {
            M3Profile profile = new M3Profile(Map.of("sample.Main", 0L),
                    Map.of("sample.Main", 7L), Map.of("sample.Main#main([Ljava/lang/String;)V", 7L));
            Path file = root.resolve("profile.tsv");
            M3ProfileIO.write(file, profile);
            M3Profile reread = M3ProfileIO.read(file);
            check(reread.loadedClassRanks().equals(profile.loadedClassRanks()), "class ranks round trip");
            check(reread.classSamples().equals(profile.classSamples()), "class samples round trip");
            check(reread.methodSamples().equals(profile.methodSamples()), "method samples round trip");
            String first = Files.readString(file, StandardCharsets.UTF_8);
            M3ProfileIO.write(file, reread);
            check(first.equals(Files.readString(file, StandardCharsets.UTF_8)), "profile fixed point");
            expectFailure(() -> new M3Profile(Map.of(), Map.of("sample.Main", 0L), Map.of()),
                    "zero sample count refused");
            expectFailure(() -> M3Arguments.parse(new String[0], 0).requirePath("out"),
                    "required path refusal");
            check(M3Names.isBinaryName("δοκιμή.Κλάση"), "Unicode binary name admitted");
            check(M3Names.isBinaryName("Main"), "default-package binary name admitted");
            check(!M3Names.isBinaryName("invalid-name.Main"), "invalid binary name refused");
        } finally {
            M3Files.deleteTree(root);
        }
    }

    private void endToEndPipeline() throws Exception {
        Path root = Files.createTempDirectory("m3-image-test-");
        try {
            Path classes = compileFixture(root);
            Path jar = root.resolve("fixture.jar");
            M3DeterministicJar.pack(classes, jar);
            String firstJarHash = M3Hashing.sha256(jar);
            Path jarAgain = root.resolve("fixture-again.jar");
            M3DeterministicJar.pack(classes, jarAgain);
            check(firstJarHash.equals(M3Hashing.sha256(jarAgain)), "deterministic directory jar");

            M3ClassPathScanner.Result scan = M3ClassPathScanner.scan(List.of(jar));
            check(scan.classes().keySet().containsAll(Set.of(
                    "sample.Main", "sample.Helper", "sample.DynamicProbe", "sample.Provider", "sample.Unused")),
                    "class path inventory");
            check(scan.serviceProviders().contains("sample.Provider"), "service provider inventory");
            M3ClassInfo main = scan.classes().get("sample.Main");
            check(!main.features().contains(M3FeatureKind.INVOKEDYNAMIC),
                    "standard string-concat bootstrap is resolved");
            check(!main.features().contains(M3FeatureKind.METHOD_HANDLES),
                    "bootstrap implementation handle is not an application boundary");
            M3ClassInfo dynamic = scan.classes().get("sample.DynamicProbe");
            check(dynamic.features().contains(M3FeatureKind.REFLECTION), "reflection detection");
            check(dynamic.features().contains(M3FeatureKind.SERVICE_LOADER), "service loader detection");
            check(dynamic.features().contains(M3FeatureKind.NATIVE_LIBRARY_LOAD), "native library detection");
            check(dynamic.features().contains(M3FeatureKind.JNI_NATIVE_METHOD), "native method detection");
            check(dynamic.features().contains(M3FeatureKind.METHOD_HANDLES),
                    "explicit method-handle API detection");

            M3JdkTools tools = M3JdkTools.resolve(null);
            Path trainingDir = root.resolve("training");
            M3Trainer.Result training = M3Trainer.train(tools, jar.toString(), "sample.Main",
                    List.of(), List.of("CHECK"), trainingDir, Duration.ofMinutes(2));
            check(training.process().stdout().contains("HELLO-CHECK"), "training runtime output");
            check(training.normalizedProfile().loadedClassRanks().containsKey("sample.Main"),
                    "training class-load profile");
            check(M3TrainingManifest.verify(trainingDir).matches("[0-9a-f]{64}"),
                    "training manifest verification");

            Path planAPath = root.resolve("plan-a");
            M3ImagePlan planA = M3ImagePlanner.plan(tools, List.of(jar), "sample.Main", M3Mode.HYBRID,
                    training.normalizedProfile(), Set.of(), Set.of(), planAPath);
            Path planBPath = root.resolve("plan-b");
            M3ImagePlan planB = M3ImagePlanner.plan(tools, List.of(jar), "sample.Main", M3Mode.HYBRID,
                    training.normalizedProfile(), Set.of(), Set.of(), planBPath);
            check(planA.planSha256().equals(planB.planSha256()), "plan fixed point");
            check(Files.readString(planAPath.resolve("semantic-manifest.tsv"), StandardCharsets.UTF_8)
                    .equals(Files.readString(planBPath.resolve("semantic-manifest.tsv"), StandardCharsets.UTF_8)),
                    "semantic manifest fixed point");
            check(planA.roles().get("sample.Main") == M3ClassRole.AOT_CANDIDATE, "main candidate role");
            check(planA.roles().get("sample.Provider") == M3ClassRole.AOT_CANDIDATE,
                    "service provider becomes root");
            check(planA.roles().get("sample.Unused") == M3ClassRole.BYTECODE_FALLBACK,
                    "hybrid retains unreachable bytecode fallback");
            check(planA.features().stream().anyMatch(feature -> feature.ownerClass().equals("sample.DynamicProbe")),
                    "reachable dynamic feature rows");

            expectFailure(() -> M3ImagePlanner.plan(tools, List.of(jar), "sample.Main", M3Mode.CLOSED_WORLD,
                    training.normalizedProfile(), Set.of(), Set.of(), root.resolve("closed-refused")),
                    "closed world refuses dynamic constraints");
            M3ImagePlan closed = M3ImagePlanner.plan(tools, List.of(jar), "sample.Main", M3Mode.CLOSED_WORLD,
                    training.normalizedProfile(), Set.of(), EnumSet.allOf(M3FeatureKind.class),
                    root.resolve("closed-admitted"));
            check(closed.features().stream().allMatch(M3ReachabilityAnalyzer.Feature::resolved),
                    "closed world explicit feature admission");
            check(closed.roles().get("sample.Unused") == M3ClassRole.UNREACHABLE,
                    "closed world marks unreachable class");

            Path partialPlan = root.resolve("partial-plan");
            M3Files.copyTree(planAPath, partialPlan);
            List<String> classRows = Files.readAllLines(partialPlan.resolve("classes.tsv"), StandardCharsets.UTF_8);
            Files.write(partialPlan.resolve("classes.tsv"), classRows.subList(0, classRows.size() - 1),
                    StandardCharsets.UTF_8);
            resealPlan(partialPlan);
            expectFailure(() -> M3PlanIO.read(partialPlan), "partial class inventory refused");

            Path cache = root.resolve("cache");
            M3BuildOptions options = new M3BuildOptions("fixture", cache, null, Duration.ofMinutes(3), List.of());
            M3ImageBuildResult first = M3ImageBuilder.build(planA, tools, M3BackendKind.JDK21_SHIFTED,
                    options, root.resolve("image-a"));
            check(!first.cacheHit(), "first build is not cache hit");
            check(first.backendStatus().contains("AppCDS=PASS"), "AppCDS generation");
            M3ImageVerifier.Result run = M3ImageVerifier.verify(first.directory(), true,
                    List.of("IMAGE"), Duration.ofMinutes(1));
            check(run.runtimeExitCode() == 0 && run.stdout().contains("HELLO-IMAGE"),
                    "packaged runtime execution");
            check(!Files.readString(first.directory().resolve("bin/fixture"), StandardCharsets.UTF_8).contains("*"),
                    "launcher preserves explicit classpath order");

            M3BuildOptions closedOptions = new M3BuildOptions(
                    "closed", null, null, Duration.ofMinutes(3), List.of());
            M3ImageBuildResult closedImage = M3ImageBuilder.build(closed, tools, M3BackendKind.JDK21_SHIFTED,
                    closedOptions, root.resolve("image-closed"));
            try (ZipFile packagedJar = new ZipFile(closedImage.directory()
                    .resolve("app/lib/cp0000-fixture.jar").toFile())) {
                check(packagedJar.getEntry("sample/Unused.class") == null,
                        "closed image prunes unreachable class");
                check(packagedJar.getEntry("sample/Provider.class") != null,
                        "closed image retains service provider root");
                check(packagedJar.getEntry("META-INF/services/java.lang.Runnable") != null,
                        "closed image retains service metadata");
            }
            M3ImageVerifier.Result closedRun = M3ImageVerifier.verify(closedImage.directory(), true,
                    List.of("CLOSED"), Duration.ofMinutes(1));
            check(closedRun.runtimeExitCode() == 0 && closedRun.stdout().contains("HELLO-CLOSED"),
                    "closed-world shifted image execution");

            if (!isWindows()) {
                Path fakeNativeImage = root.resolve("fake-native-image");
                Files.writeString(fakeNativeImage, """
                        #!/bin/sh
                        set -eu
                        out=
                        while [ "$#" -gt 0 ]; do
                          case "$1" in
                            -o) out=$2; shift 2 ;;
                            *) shift ;;
                          esac
                        done
                        test -n "$out"
                        cat > "$out" <<'M3EOF'
                        #!/bin/sh
                        printf 'FAKE-NATIVE:%s\n' "$*"
                        M3EOF
                        chmod +x "$out"
                        """, StandardCharsets.UTF_8);
                M3Files.makeExecutable(fakeNativeImage);
                M3BuildOptions nativeOptions = new M3BuildOptions("native-fixture", null, fakeNativeImage,
                        Duration.ofMinutes(1), List.of());
                M3ImageBuildResult nativeFixture = M3ImageBuilder.build(planA, tools, M3BackendKind.NATIVE_IMAGE,
                        nativeOptions, root.resolve("image-native-fixture"));
                M3ImageVerifier.Result nativeRun = M3ImageVerifier.verify(nativeFixture.directory(), true,
                        List.of("ADAPTER"), Duration.ofMinutes(1));
                check(nativeRun.runtimeExitCode() == 0 && nativeRun.stdout().contains("FAKE-NATIVE:ADAPTER"),
                        "native-image process adapter fixture");
            }

            M3ImageBuildResult second = M3ImageBuilder.build(planA, tools, M3BackendKind.JDK21_SHIFTED,
                    options, root.resolve("image-b"));
            check(second.cacheHit(), "second build cache hit");
            check(first.imageSha256().equals(second.imageSha256()), "cache preserves image identity");

            Path tampered = second.directory().resolve("M3_BUILD_RECEIPT.properties");
            Files.writeString(tampered, "tampered\n", StandardCharsets.UTF_8);
            expectFailure(() -> M3ImageManifest.verify(second.directory()), "manifest rejects tampering");

            Files.writeString(trainingDir.resolve("application.stdout.log"), "tampered\n", StandardCharsets.UTF_8);
            expectFailure(() -> M3TrainingManifest.verify(trainingDir), "training manifest rejects tampering");

            Path signedClasses = root.resolve("signed-classes");
            M3Files.copyTree(classes, signedClasses);
            Files.createDirectories(signedClasses.resolve("META-INF"));
            Files.writeString(signedClasses.resolve("META-INF/TEST.SF"), "Signature-Version: 1.0\n",
                    StandardCharsets.UTF_8);
            Path signedJar = root.resolve("signed.jar");
            M3DeterministicJar.pack(signedClasses, signedJar);
            M3ImagePlan signedPlan = M3ImagePlanner.plan(tools, List.of(signedJar), "sample.Main",
                    M3Mode.CLOSED_WORLD, M3Profile.empty(), Set.of(), EnumSet.allOf(M3FeatureKind.class),
                    root.resolve("signed-plan"));
            expectFailure(() -> M3ImageBuilder.build(signedPlan, tools, M3BackendKind.JDK21_SHIFTED,
                    closedOptions, root.resolve("signed-image")), "signed archive pruning refused");
        } finally {
            M3Files.deleteTree(root);
        }
    }

    private Path compileFixture(Path root) throws IOException {
        Path source = root.resolve("source/sample");
        Path classes = root.resolve("classes");
        Files.createDirectories(source);
        Files.createDirectories(classes);
        Files.writeString(source.resolve("Main.java"), """
                package sample;
                public final class Main {
                    private Main() {}
                    public static void main(String[] args) {
                        Class<?> retained = DynamicProbe.class;
                        String value = args.length == 0 ? "M3" : args[0];
                        System.out.println("HELLO-" + Helper.upper(value) + retained.getSimpleName().substring(0, 0));
                    }
                }
                """, StandardCharsets.UTF_8);
        Files.writeString(source.resolve("Helper.java"), """
                package sample;
                final class Helper {
                    private Helper() {}
                    static String upper(String value) { return value.toUpperCase(java.util.Locale.ROOT); }
                }
                """, StandardCharsets.UTF_8);
        Files.writeString(source.resolve("DynamicProbe.java"), """
                package sample;
                final class DynamicProbe {
                    private DynamicProbe() {}
                    native void nativeEntry();
                    static void boundaries(String name) throws Exception {
                        Class.forName(name);
                        java.util.ServiceLoader.load(Runnable.class);
                        java.lang.invoke.MethodHandles.lookup();
                        System.loadLibrary("m3-not-executed");
                    }
                }
                """, StandardCharsets.UTF_8);
        Files.writeString(source.resolve("Provider.java"), """
                package sample;
                public final class Provider implements Runnable {
                    public Provider() {}
                    @Override public void run() {}
                }
                """, StandardCharsets.UTF_8);
        Files.writeString(source.resolve("Unused.java"), """
                package sample;
                final class Unused {
                    private Unused() {}
                    static String value() { return "UNUSED"; }
                }
                """, StandardCharsets.UTF_8);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IOException("A full JDK is required for tests");
        }
        int exit = compiler.run(null, null, null, "--release", "21", "-d", classes.toString(),
                source.resolve("Main.java").toString(), source.resolve("Helper.java").toString(),
                source.resolve("DynamicProbe.java").toString(), source.resolve("Provider.java").toString(),
                source.resolve("Unused.java").toString());
        if (exit != 0) {
            throw new IOException("Fixture compilation failed with exit " + exit);
        }
        Path services = classes.resolve("META-INF/services");
        Files.createDirectories(services);
        Files.writeString(services.resolve("java.lang.Runnable"), "sample.Provider\n", StandardCharsets.UTF_8);
        return classes;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win");
    }

    private static void resealPlan(Path plan) throws IOException {
        List<String> rows = M3Files.manifest(plan, Set.of(
                "bindings.properties", "semantic-manifest.tsv", "PLAN_SHA256",
                "jdeps.stdout.log", "jdeps.stderr.log"));
        M3Files.writeUtf8(plan.resolve("semantic-manifest.tsv"), String.join("", rows));
        M3Files.writeUtf8(plan.resolve("PLAN_SHA256"), M3Hashing.hashRows(rows) + "\n");
    }

    private void expectFailure(CheckedRunnable runnable, String name) throws Exception {
        try {
            runnable.run();
            throw new AssertionError("Expected failure: " + name);
        } catch (IOException | IllegalArgumentException expected) {
            checks++;
        }
    }

    private void check(boolean condition, String name) {
        if (!condition) {
            throw new AssertionError("Check failed: " + name);
        }
        checks++;
    }

    @FunctionalInterface
    private interface CheckedRunnable {
        void run() throws Exception;
    }
}
