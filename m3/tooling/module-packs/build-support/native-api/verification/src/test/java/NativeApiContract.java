// Copyright 2026 Hitesh Soliwal and contributors.
// SPDX-License-Identifier: Apache-2.0

import com.m3.file.M3File;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.tools.ToolProvider;
import org.codehaus.mojo.natives.NativeBuildException;
import org.codehaus.mojo.natives.c.CCompiler;
import org.codehaus.mojo.natives.c.CLinker;
import org.codehaus.mojo.natives.compiler.Compiler;
import org.codehaus.mojo.natives.compiler.CompilerConfiguration;
import org.codehaus.mojo.natives.linker.ArchiveLinker;
import org.codehaus.mojo.natives.linker.Linker;
import org.codehaus.mojo.natives.linker.LinkerConfiguration;
import org.codehaus.mojo.natives.parser.Parser;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.cli.Commandline;

/** Independent finite fixture; its one known header dependency is not a general build planner. */
public final class NativeApiContract {
    private static int checks;
    private static final long INPUT_LIMIT = 1_048_576;

    private NativeApiContract() {}

    public static void main(String[] arguments) throws Exception {
        if (arguments.length == 4 && arguments[0].equals("--concurrent-failure")) {
            concurrentFailureChild(arguments[1], Path.of(arguments[2]), arguments[3]);
            return;
        }
        if (arguments.length != 6) {
            throw new IllegalArgumentException("work include gcc g++ ar expectedJavaHome");
        }
        Path work = Path.of(arguments[0]).toAbsolutePath();
        Files.createDirectory(work);
        Path include = Path.of(arguments[1]).toRealPath();
        String gcc = Path.of(arguments[2]).toRealPath().toString();
        String gpp = Path.of(arguments[3]).toRealPath().toString();
        String ar = Path.of(arguments[4]).toRealPath().toString();
        check(Path.of(System.getProperty("java.home")).toRealPath()
                .equals(Path.of(arguments[5]).toRealPath()), "selected SDK executes the fixture");
        check(System.getProperty("os.name").equals("Linux"), "qualified native platform is Linux");
        check(Files.isRegularFile(include.resolve("jni.h")), "retained JNI header");
        check(Files.isRegularFile(include.resolve("linux/jni_md.h")), "retained Linux JNI header");
        expectMissingClass("org.apache.maven.execution.MavenSession");
        expectMissingClass("org.apache.maven.project.MavenProject");
        check(Compiler.class.getMethod("compile", CompilerConfiguration.class, File[].class)
                .getReturnType() == List.class, "original Compiler API signature");
        check(Linker.class.getMethod("link", LinkerConfiguration.class, List.class)
                .getReturnType() == File.class, "original Linker API signature");
        check(new CompilerConfiguration().isCheckStaleCompilation(), "default stale analysis is compatible");
        check(M3File.class.getModule().isNamed()
                && M3File.class.getModule().getName().equals("com.m3.file"), "original M3File is a selected module");
        check(Path.of(CCompiler.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath()
                .startsWith(Path.of(arguments[5]).toRealPath().resolve("lib/m3-tools/native-api")),
                "original native provider loads from installed selected assets");

        Path sources = Files.createDirectory(work.resolve("sources"));
        Path headers = Files.createDirectory(work.resolve("generated-headers"));
        Path javaClasses = Files.createDirectory(work.resolve("java-classes"));
        Path objects = Files.createDirectory(work.resolve("objects"));
        Path nativeOutput = Files.createDirectory(work.resolve("native"));
        Path javaSource = sources.resolve("NativeApiProbe.java");
        Files.writeString(javaSource, """
                public final class NativeApiProbe {
                    private NativeApiProbe() {}
                    private static native int add(int left, int right);
                    public static int call(String absoluteLibrary, int left, int right) {
                        System.load(absoluteLibrary);
                        return add(left, right);
                    }
                }
                """, StandardCharsets.UTF_8);
        var javaCompiler = ToolProvider.getSystemJavaCompiler();
        check(javaCompiler != null, "selected SDK exposes JavaCompiler");
        int javaResult = javaCompiler.run(null, null, null, "--release", "21", "-Xlint:all", "-Werror",
                "-h", headers.toString(), "-d", javaClasses.toString(), javaSource.toString());
        check(javaResult == 0, "JavaCompiler generates JNI header and class");
        check(Files.isRegularFile(headers.resolve("NativeApiProbe.h")), "javac -h output exists");

        Path jniSource = sources.resolve("native_entry.c");
        Files.writeString(jniSource, """
                #include "NativeApiProbe.h"
                extern int native_helper(int left, int right);
                JNIEXPORT jint JNICALL Java_NativeApiProbe_add
                  (JNIEnv *env, jclass type, jint left, jint right) {
                    (void)env; (void)type;
                    return (jint)native_helper((int)left, (int)right);
                }
                """, StandardCharsets.UTF_8);
        Path helperSource = sources.resolve("native_helper.cpp");
        Files.writeString(helperSource, """
                #include "native_config.h"
                extern "C" int native_helper(int left, int right) {
                    return left + right + NATIVE_BIAS + 0;
                }
                """, StandardCharsets.UTF_8);
        Path configHeader = sources.resolve("native_config.h");
        Files.writeString(configHeader, "#define NATIVE_BIAS 0\n", StandardCharsets.UTF_8);

        TrackingCompiler c = new TrackingCompiler("native-c");
        CompilerConfiguration cConfig = compileConfiguration(work, objects, gcc,
                new File[] {headers.toFile(), include.toFile(), include.resolve("linux").toFile()}, true);
        Compiler cApi = c;
        List<File> jniObjects = cApi.compile(cConfig, new File[] {jniSource.toFile()});
        check(jniObjects.size() == 1 && Files.size(jniObjects.get(0).toPath()) > 0, "C API produces object");
        check(c.parsers == 1 && c.commands == 1, "default compiler retains original parser path");
        M3File.Fingerprint originalDefaultObject = fingerprint(jniObjects.get(0).toPath());
        FileTime originalDefaultTime = Files.getLastModifiedTime(jniObjects.get(0).toPath());
        check(cApi.compile(cConfig, new File[] {jniSource.toFile()}).equals(jniObjects),
                "default unchanged compilation returns its original object");
        check(c.parsers == 2 && c.commands == 1, "default unchanged object uses parser and skips native compilation");
        check(fingerprint(jniObjects.get(0).toPath()).equals(originalDefaultObject)
                && Files.getLastModifiedTime(jniObjects.get(0).toPath()).equals(originalDefaultTime),
                "default unchanged compilation preserves object bytes and timestamp");

        TrackingCompiler cpp = new TrackingCompiler("native-cpp");
        CompilerConfiguration cppConfig = compileConfiguration(work, objects, gpp,
                new File[] {sources.toFile()}, false);
        Compiler cppApi = cpp;
        List<File> helperObjects = cppApi.compile(cppConfig, new File[] {helperSource.toFile()});
        check(helperObjects.size() == 1 && Files.size(helperObjects.get(0).toPath()) > 0,
                "C++ API produces object");
        check(cpp.parsers == 0 && cpp.commands == 1, "admitted C++ workset bypasses parsing");
        Path jniObject = jniObjects.get(0).toPath();
        Path helperObject = helperObjects.get(0).toPath();
        M3File.Fingerprint originalJniObject = fingerprint(jniObject);
        FileTime originalJniTime = Files.getLastModifiedTime(jniObject);
        M3File.Fingerprint previousSource = fingerprint(helperSource);
        M3File.Fingerprint previousHeader = fingerprint(configHeader);
        File firstLibrary = archiveAndLink(work, nativeOutput, "baseline", ar, gpp, helperObjects, jniObjects);
        check(callNative(javaClasses, firstLibrary, 17, 25) == 42, "real JNI result after C/C++ archive/link");

        // Exactly one known dependency edge: native_helper.cpp includes native_config.h.
        M3File.Fingerprint currentSource = fingerprint(helperSource);
        M3File.Fingerprint currentHeader = fingerprint(configHeader);
        File[] unchanged = selected(previousSource, currentSource, previousHeader, currentHeader, helperSource);
        check(unchanged.length == 0, "M3File identical content selects no native translation unit");
        int commandsBeforeEmpty = cpp.commands;
        M3File.Fingerprint helperBeforeEmpty = fingerprint(helperObject);
        cppConfig.setExecutable(work.resolve("must-not-execute-missing-compiler").toString());
        check(cppApi.compile(cppConfig, unchanged).isEmpty(), "empty admitted workset returns no objects");
        check(cpp.commands == commandsBeforeEmpty && cpp.parsers == 0, "empty workset neither commands nor parses");
        check(fingerprint(helperObject).equals(helperBeforeEmpty), "empty workset preserves output");
        cppConfig.setExecutable(gpp);

        FileTime sourceTime = Files.getLastModifiedTime(helperSource);
        String editedSource = Files.readString(helperSource, StandardCharsets.UTF_8).replace(" + 0;", " + 7;");
        Files.writeString(helperSource, editedSource, StandardCharsets.UTF_8);
        Files.setLastModifiedTime(helperSource, sourceTime);
        check(Files.getLastModifiedTime(helperSource).equals(sourceTime), "source timestamp restored");
        currentSource = fingerprint(helperSource);
        currentHeader = fingerprint(configHeader);
        check(M3File.contentDelta(previousSource, currentSource) == M3File.CONTENT_CHANGED,
                "M3File sees same-timestamp source-byte change");
        File[] sourceWorkset = selected(previousSource, currentSource, previousHeader, currentHeader, helperSource);
        check(sourceWorkset.length == 1, "source delta selects one known translation unit");
        helperObjects = cppApi.compile(cppConfig, sourceWorkset);
        check(cpp.commands == commandsBeforeEmpty + 1 && cpp.parsers == 0, "source workset compiles once without parsing");
        check(fingerprint(jniObject).equals(originalJniObject)
                && Files.getLastModifiedTime(jniObject).equals(originalJniTime), "unselected JNI object is untouched");
        File sourceLibrary = archiveAndLink(work, nativeOutput, "source", ar, gpp, helperObjects, jniObjects);
        check(callNative(javaClasses, sourceLibrary, 17, 25) == 49, "same-timestamp source edit changes JNI result");
        previousSource = currentSource;
        previousHeader = currentHeader;

        FileTime headerTime = Files.getLastModifiedTime(configHeader);
        Files.writeString(configHeader, "#define NATIVE_BIAS 3\n", StandardCharsets.UTF_8);
        Files.setLastModifiedTime(configHeader, headerTime);
        check(Files.getLastModifiedTime(configHeader).equals(headerTime), "header timestamp restored");
        currentSource = fingerprint(helperSource);
        currentHeader = fingerprint(configHeader);
        check(M3File.contentDelta(previousSource, currentSource) == 0, "source content stays fixed during header edit");
        check(M3File.contentDelta(previousHeader, currentHeader) == M3File.CONTENT_CHANGED,
                "M3File sees same-timestamp header-byte change");
        File[] headerWorkset = selected(previousSource, currentSource, previousHeader, currentHeader, helperSource);
        check(headerWorkset.length == 1, "known header edge selects one dependent translation unit");
        helperObjects = cppApi.compile(cppConfig, headerWorkset);
        check(cpp.commands == commandsBeforeEmpty + 2 && cpp.parsers == 0, "header workset compiles once without parsing");
        check(fingerprint(jniObject).equals(originalJniObject)
                && Files.getLastModifiedTime(jniObject).equals(originalJniTime), "header workset preserves unrelated object");
        File headerLibrary = archiveAndLink(work, nativeOutput, "header", ar, gpp, helperObjects, jniObjects);
        check(callNative(javaClasses, headerLibrary, 17, 25) == 52, "header delta changes JNI result");

        int commandsBeforeMissing = cpp.commands;
        expectNativeFailure(() -> cppApi.compile(cppConfig, new File[] {sources.resolve("absent.cpp").toFile()}),
                "not found", "missing admitted source is refused");
        check(cpp.commands == commandsBeforeMissing && cpp.parsers == 0, "missing source fails before parsing or commands");
        for (String mode : List.of("missing", "object", "command", "environment")) {
            checkConcurrentFailureExit(work, gcc, mode);
        }

        TrackingCompiler failures = new TrackingCompiler("native-failures");
        CompilerConfiguration failureConfig = compileConfiguration(work, work.resolve("failure-objects"),
                gcc, new File[0], false);
        Path invalid = sources.resolve("invalid.c");
        Files.writeString(invalid, "this is deliberately invalid C;\n", StandardCharsets.UTF_8);
        expectNativeFailure(() -> failures.compile(failureConfig, new File[] {invalid.toFile()}),
                "Exit code", "real C syntax failure propagates");
        failureConfig.setExecutable(work.resolve("missing-native-compiler").toString());
        expectNativeFailure(() -> failures.compile(failureConfig, new File[] {invalid.toFile()}),
                "Error executing command line", "missing native executable propagates");
        check(failures.parsers == 0, "failure paths do not start dependency parsing in admitted mode");

        CLinker brokenLinker = new CLinker();
        brokenLinker.enableLogging(logger("native-link-failure"));
        LinkerConfiguration brokenConfig = linkConfiguration(work, nativeOutput, "libbroken", "so", gpp);
        brokenConfig.setStartOptions(new String[] {"-shared", "-Wl,--no-undefined"});
        expectNativeFailure(() -> brokenLinker.link(brokenConfig, List.of(work.resolve("absent.o").toFile())),
                "Exit code", "real linker failure propagates");
        check(!Files.exists(brokenConfig.getOutputFile().toPath()), "failed link has no successful output");
        check(c.parsers == 2 && c.commands == 1, "JNI source was compiled exactly once");
        check(cpp.parsers == 0 && cpp.commands == 3, "three admitted C++ generations compiled with no parser calls");
        System.out.println("NativeApiContract: PASS; checks=" + checks
                + "; c_compiles=" + c.commands + "; cpp_compiles=" + cpp.commands
                + "; admitted_parser_calls=" + cpp.parsers + "; empty_workset_compiles=0"
                + "; native_results=42,49,52; runtime=" + Runtime.version());
    }

    private static void checkConcurrentFailureExit(Path work, String gcc, String mode) throws Exception {
        Path childLog = work.resolve("concurrent-" + mode + ".log");
        Process child = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "--add-modules", "com.m3.file", "-cp", System.getProperty("java.class.path"),
                NativeApiContract.class.getName(), "--concurrent-failure", mode,
                work.resolve("concurrent-" + mode).toString(), gcc)
                .redirectErrorStream(true).redirectOutput(childLog.toFile()).start();
        boolean exited = child.waitFor(15, TimeUnit.SECONDS);
        if (!exited) {
            child.destroyForcibly();
            child.waitFor(5, TimeUnit.SECONDS);
            throw new AssertionError("Admitted worker leak after " + mode + " failure; see " + childLog);
        }
        check(child.exitValue() == 0, "concurrent " + mode + " failure exits normally");
        check(Files.readString(childLog).contains("ConcurrentFailure: PASS; mode=" + mode),
                "concurrent " + mode + " failure was caught by caller");
    }

    private static void concurrentFailureChild(String mode, Path work, String gcc) throws Exception {
        Files.createDirectory(work);
        Path first = work.resolve("first.c");
        Path second = work.resolve("second.c");
        Files.writeString(first, "int first(void) { return 1; }\n");
        Files.writeString(second, "int second(void) { return 2; }\n");
        FailureCompiler compiler = new FailureCompiler(mode);
        CompilerConfiguration config = compileConfiguration(work, work.resolve("objects"), gcc, new File[0], false);
        config.setNumberOfConcurrentCompilation(2);
        File later = second.toFile();
        String expected = "fixture preparation failure";
        if (mode.equals("missing")) {
            later = work.resolve("missing.c").toFile();
            expected = "not found";
        } else if (mode.equals("object")) {
            later = new File(second.toString()) {
                private static final long serialVersionUID = 1L;

                @Override
                public String getCanonicalPath() throws IOException {
                    throw new IOException("fixture preparation failure");
                }
            };
        } else if (mode.equals("environment")) {
            int[] calls = {0};
            config.setEnvFactory(() -> {
                if (++calls[0] == 2) throw new NativeBuildException("fixture preparation failure");
                return Map.of();
            });
        }
        File[] selected = {first.toFile(), later};
        expectNativeFailure(() -> compiler.compile(config, selected), expected,
                "concurrent admitted " + mode + " failure propagates");
        check(compiler.parsers == 0, "concurrent admitted failure never obtains parser");
        if (mode.equals("missing")) {
            check(compiler.commands == 0, "complete missing-source preflight precedes native commands");
        }
        System.out.println("ConcurrentFailure: PASS; mode=" + mode + "; commands=" + compiler.commands);
        // Return normally: System.exit would conceal abandoned non-daemon worker threads.
    }

    private static CompilerConfiguration compileConfiguration(Path work, Path output, String executable,
            File[] includes, boolean checkStale) {
        CompilerConfiguration config = new CompilerConfiguration();
        config.setWorkingDirectory(work.toFile());
        config.setOutputDirectory(output.toFile());
        config.setExecutable(executable);
        config.setObjectFileExtension("o");
        config.setStartOptions(new String[] {"-fPIC", "-O0", "-Wall", "-Wextra", "-Werror"});
        config.setIncludePaths(includes);
        config.setNumberOfConcurrentCompilation(1);
        config.setCheckStaleCompilation(checkStale);
        return config;
    }

    private static LinkerConfiguration linkConfiguration(Path work, Path output, String name,
            String extension, String executable) {
        LinkerConfiguration config = new LinkerConfiguration();
        config.setWorkingDirectory(work.toFile());
        config.setOutputDirectory(output.toFile());
        config.setOutputFileName(name);
        config.setOutputFileExtension(extension);
        config.setExecutable(executable);
        config.setStartOptions(new String[0]);
        config.setMiddleOptions(new String[0]);
        config.setEndOptions(new String[0]);
        config.setCheckStaleLinkage(false);
        return config;
    }

    private static File archiveAndLink(Path work, Path output, String generation, String ar, String gpp,
            List<File> helperObjects, List<File> jniObjects) throws IOException {
        ArchiveLinker archive = new ArchiveLinker();
        archive.enableLogging(logger("native-archive"));
        LinkerConfiguration archiveConfig = linkConfiguration(work, output, "libhelper_" + generation, "a", ar);
        archiveConfig.setStartOptions(new String[] {"rcs"});
        File library = ((Linker) archive).link(archiveConfig, helperObjects);
        check(Files.isRegularFile(library.toPath()) && Files.size(library.toPath()) > 0,
                "original ArchiveLinker creates " + generation + " static library");
        CLinker linker = new CLinker();
        linker.enableLogging(logger("native-shared-link"));
        LinkerConfiguration linkConfig = linkConfiguration(work, output, "libnative_" + generation, "so", gpp);
        linkConfig.setStartOptions(new String[] {"-shared", "-Wl,--no-undefined"});
        linkConfig.setExternalLibDirectory(output.toFile());
        linkConfig.setExternalLibFileNames(List.of(library.getName()));
        File shared = ((Linker) linker).link(linkConfig, jniObjects);
        check(Files.isRegularFile(shared.toPath()) && Files.size(shared.toPath()) > 0,
                "original CLinker creates " + generation + " JNI shared library");
        return shared;
    }

    private static File[] selected(M3File.Fingerprint previousSource, M3File.Fingerprint currentSource,
            M3File.Fingerprint previousHeader, M3File.Fingerprint currentHeader, Path dependentSource) {
        boolean changed = M3File.contentDelta(previousSource, currentSource) != 0
                || M3File.contentDelta(previousHeader, currentHeader) != 0;
        return changed ? new File[] {dependentSource.toFile()} : new File[0];
    }

    private static int callNative(Path javaClasses, File library, int left, int right) throws Exception {
        try (URLClassLoader loader = new URLClassLoader(new URL[] {javaClasses.toUri().toURL()},
                ClassLoader.getPlatformClassLoader())) {
            Class<?> probe = Class.forName("NativeApiProbe", true, loader);
            return (Integer) probe.getMethod("call", String.class, int.class, int.class)
                    .invoke(null, library.getAbsolutePath(), left, right);
        }
    }

    private static M3File.Fingerprint fingerprint(Path file) throws IOException {
        return M3File.of(file).fingerprint(INPUT_LIMIT);
    }

    private static Logger logger(String name) {
        return new ConsoleLogger(Logger.LEVEL_INFO, name);
    }

    private static void expectMissingClass(String name) throws ClassNotFoundException {
        try {
            Class.forName(name);
            throw new AssertionError("Maven runtime unexpectedly present: " + name);
        } catch (ClassNotFoundException expected) {
            check(true, "Maven runtime is absent: " + name);
        }
    }

    private static void expectNativeFailure(Checked action, String fragment, String label) throws Exception {
        try {
            action.run();
            throw new AssertionError(label + " did not fail");
        } catch (NativeBuildException expected) {
            check(expected.getMessage().contains(fragment), label);
        }
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
    }

    @FunctionalInterface
    private interface Checked {
        void run() throws Exception;
    }

    private static final class TrackingCompiler extends CCompiler {
        private int parsers;
        private int commands;

        private TrackingCompiler(String name) {
            enableLogging(logger(name));
        }

        @Override
        protected Parser getParser() {
            parsers++;
            return super.getParser();
        }

        @Override
        protected Commandline getCommandLine(File source, File destination, CompilerConfiguration config) {
            commands++;
            return super.getCommandLine(source, destination, config);
        }
    }

    private static final class FailureCompiler extends CCompiler {
        private final String mode;
        private int parsers;
        private int commands;

        private FailureCompiler(String mode) {
            this.mode = mode;
            enableLogging(logger("native-concurrent-failure"));
        }

        @Override
        protected Parser getParser() {
            parsers++;
            return super.getParser();
        }

        @Override
        protected Commandline getCommandLine(File source, File destination, CompilerConfiguration config) {
            if (++commands == 2 && mode.equals("command")) {
                throw new NativeBuildException("fixture preparation failure");
            }
            return super.getCommandLine(source, destination, config);
        }
    }
}
