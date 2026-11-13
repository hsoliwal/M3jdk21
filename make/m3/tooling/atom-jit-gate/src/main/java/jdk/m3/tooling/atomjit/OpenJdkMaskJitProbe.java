// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tooling.atomjit;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Test-only, fail-closed JIT differential for the actual installed M3JDK 21
 * java.base/jdk.internal.mindex.M3CollectionMaskCompactor.compact implementation.
 * Does not install a recipe into a product or substitute for jtreg/JNI native proof.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/b65df53330e3d82f6b2d5509ded971f19883f4ca/synexia-openrewrite-recipes/crates/m3-openjdk-atom-jit-pattern-20261031/src/main/java/com/synexia/m3/qualification/M3OpenJdkMaskJitProbe.java blob f6bb7481007a; recipe m3/tooling/atom-jit-gate/recipe/atom-jit-gate-20261106/relocate.py v4
public final class OpenJdkMaskJitProbe {
    private static final String TARGET = "jdk.internal.mindex.M3CollectionMaskCompactor";
    private static final String METHOD = "M3CollectionMaskCompactor::compact";
    private static final String WITNESS = "M3_JIT_WITNESS_SHA256=";
    private static final int[] LENGTHS = {0, 1, 2, 63, 64, 65, 511, 512, 513, 1024, 2048};

    private OpenJdkMaskJitProbe() { }

    public static void main(String[] arguments) throws Throwable {
        if (arguments.length == 1 && "--child".equals(arguments[0])) {
            System.out.println(WITNESS + runTargetOracle());
        } else if (arguments.length == 2 && "--verify".equals(arguments[0])) {
            verify(Path.of(arguments[1]));
        } else {
            throw new IllegalArgumentException("Usage: --verify <M3JDK21-java> | --child");
        }
    }

    private static void verify(Path javaBinary) throws Exception {
        Path executable = javaBinary.toRealPath();
        if (!Files.isRegularFile(executable) || !Files.isExecutable(executable)) {
            throw new IllegalArgumentException("M3JDK21 Java executable required");
        }
        // A launcher hash alone does not identify the JVM: identical bin/java
        // launchers can run different HotSpot libraries and java.base modules.
        String sha = runtimeImageRoot(executable);
        Result interpreted = child(executable, true);
        requireStableImage(executable, sha);
        Result compiled = child(executable, false);
        requireStableImage(executable, sha);
        if (!interpreted.witness().equals(compiled.witness())) {
            throw new IllegalStateException("-Xint/JIT semantic digest mismatch");
        }
        if (!compiled.compilationObserved()) {
            throw new IllegalStateException("HotSpot did not report compilation of " + METHOD);
        }
        // Retain the existing receipt field name for the canonical gate.
        // Its digest domain is now the complete V2 runtime-image component set.
        System.out.println("M3_JIT_RUNTIME_BINARY_SHA256=" + sha);
        System.out.println("M3_JIT_RUNTIME_ROOT_VERSION=V2_LAUNCHER_HOTSPOT_MODULES_RELEASE");
        System.out.println(WITNESS + compiled.witness());
        System.out.println("M3_JIT_COMPILED_METHOD_OBSERVED=true");
        System.out.println("M3_JIT_STATUS=REFERENCE_DIFFERENTIAL_ONLY_TARGET_JTREG_JNI_PENDING");
    }

    /**
     * Bind the exact executable, VM implementation and module source together.
     * A source-built JDK without these required components is not qualified.
     * Each component is streamed, not loaded into an unbounded byte array.
     */
    static String runtimeImageRoot(Path launcher) throws Exception {
        Path executable = launcher.toRealPath();
        Path bin = executable.getParent();
        if (bin == null || !bin.getFileName().toString().equals("bin")) {
            throw new IllegalArgumentException("expected canonical JDK bin/java executable");
        }
        Path home = bin.getParent();
        if (home == null) throw new IllegalArgumentException("missing JDK image root");
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String vmRelative = os.contains("win") ? "bin/server/jvm.dll"
                : os.contains("mac") || os.contains("darwin")
                    ? "lib/server/libjvm.dylib" : "lib/server/libjvm.so";
        String launcherRelative = os.contains("win") ? "bin/java.exe" : "bin/java";
        if (!executable.equals(home.resolve(launcherRelative))) {
            throw new IllegalArgumentException("unexpected JDK launcher path");
        }

        String[] labels = {launcherRelative, vmRelative, "lib/modules", "release"};
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        frame(digest, "M3JDK_BINARY_IMAGE_V2_LAUNCHER_HOTSPOT_MODULES_RELEASE");
        byte[] buffer = new byte[64 * 1024];
        for (String label : labels) {
            Path component = home.resolve(label).toRealPath();
            if (!component.startsWith(home) || !Files.isRegularFile(component)
                    || !Files.isReadable(component)) {
                throw new IllegalStateException("missing or external JDK component: " + label);
            }
            frame(digest, label);
            long size = Files.size(component);
            digest.update(ByteBuffer.allocate(Long.BYTES).putLong(size).array());
            try (InputStream stream = Files.newInputStream(component)) {
                int bytes;
                while ((bytes = stream.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytes);
                }
            }
            if (Files.size(component) != size) {
                throw new IllegalStateException("JDK component mutated while hashing: " + label);
            }
        }
        return hex(digest.digest());
    }

    /** Same JDK-image identity for JNI parity in the current target VM. */
    static String currentRuntimeRoot() throws Exception {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String launcher = os.contains("win") ? "java.exe" : "java";
        return runtimeImageRoot(Path.of(System.getProperty("java.home"), "bin", launcher));
    }

    private static void requireStableImage(Path executable, String root) throws Exception {
        if (!root.equals(runtimeImageRoot(executable))) {
            throw new IllegalStateException("JDK image drift between interpreted/C2 phases");
        }
    }

    private static void frame(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    private record Result(String witness, boolean compilationObserved) { }

    private static Result child(Path binary, boolean interpreter) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(binary.toString());
        if (interpreter) {
            command.add("-Xint");
        } else {
            command.add("-XX:-TieredCompilation");
            command.add("-XX:CompileThreshold=200");
            command.add("-XX:+PrintCompilation");
            command.add("-Xbatch");
        }
        command.add("--add-exports=java.base/jdk.internal.mindex=ALL-UNNAMED");
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(OpenJdkMaskJitProbe.class.getName());
        command.add("--child");
        Path log = Files.createTempFile("m3-openjdk-jit-probe-", ".log");
        String transcript;
        Process process = null;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true)
                    .redirectOutput(log.toFile()).start();
            if (!process.waitFor(120, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("target JVM probe timeout");
            }
            if (Files.size(log) > 2_000_000) {
                throw new IOException("excessive target JVM compilation transcript");
            }
            transcript = Files.readString(log, StandardCharsets.UTF_8);
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
            Files.deleteIfExists(log);
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException("target JVM probe failed (" + process.exitValue()
                    + "): " + transcript.substring(0, Math.min(1500, transcript.length())));
        }
        String witness = null;
        for (String line : transcript.split("\\R")) {
            if (line.startsWith(WITNESS)) {
                if (witness != null) throw new IllegalStateException("duplicate JIT digest");
                witness = line.substring(WITNESS.length()).strip();
            }
        }
        if (witness == null || !witness.matches("[0-9a-f]{64}")) {
            throw new IllegalStateException("target JVM emitted no sealed witness");
        }
        return new Result(witness, !interpreter && transcript.contains(METHOD));
    }

    private record Token(int id) { }

    private static String runTargetOracle() throws Throwable {
        Class<?> target = Class.forName(TARGET);
        MethodHandle compact = MethodHandles.publicLookup().findStatic(target, "compact",
                MethodType.methodType(int.class, Object[].class, int.class, int.class, long[].class));
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        Object[] tokens = new Object[4096 + 32];
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = i % 19 == 0 ? null : new Token(i);
        }
        for (int repetition = 0; repetition < 13_200; repetition++) {
            int count = LENGTHS[repetition % LENGTHS.length];
            int from = repetition % 17;
            int to = from + count;
            Object[] actual = Arrays.copyOf(tokens, tokens.length);
            Object[] expected = Arrays.copyOf(tokens, tokens.length);
            long[] deleted = new long[(count + 63) >>> 6];
            for (int i = 0; i < count; i++) {
                if (discard(repetition % 8, i, count)) deleted[i >>> 6] |= 1L << (i & 63);
            }
            int end = from;
            for (int i = 0; i < count; i++) {
                if ((deleted[i >>> 6] & (1L << (i & 63))) == 0L) {
                    expected[end++] = expected[from + i];
                }
            }
            int result = (int) compact.invokeExact(actual, from, to, deleted);
            if (result != end) throw new AssertionError("wrong survivor end at " + repetition);
            for (int i = 0; i < actual.length; i++) {
                if (actual[i] != expected[i]) {
                    throw new AssertionError("identity/tail mismatch at " + repetition + "/" + i);
                }
            }
            digest.update(ByteBuffer.allocate(Integer.BYTES * 2).putInt(repetition)
                    .putInt(result).array());
            for (int i = from; i < result; i++) {
                digest.update(ByteBuffer.allocate(Integer.BYTES)
                        .putInt(actual[i] == null ? -1 : ((Token) actual[i]).id()).array());
            }
        }
        return hex(digest.digest());
    }

    private static boolean discard(int pattern, int i, int length) {
        return switch (pattern) {
            case 0 -> false;
            case 1 -> true;
            case 2 -> i == length - 1;
            case 3 -> (i & 1) == 0;
            case 4 -> i < length / 2;
            case 5 -> i % 64 == 0;
            case 6 -> (i * 1103515245 + length * 31) >>> 28 == 0;
            default -> i > 0 && (i % 7 == 3);
        };
    }

    private static String hex(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }
}
