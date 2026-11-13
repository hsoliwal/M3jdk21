// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tooling.atomjit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/b65df53330e3d82f6b2d5509ded971f19883f4ca/synexia-openrewrite-recipes/crates/m3-openjdk-atom-jit-pattern-20261031/src/test/java/com/synexia/m3/qualification/M3OpenJdkMaskJitProbeTest.java blob 24a34347b555; recipe make/m3/tooling/atom-jit-gate/recipe/atom-jit-gate-20261106/relocate.py v4
final class OpenJdkMaskJitProbeTest {
    @TempDir Path image;

    /** The old, unsafe bin/java-only witness stays unchanged in this regression. */
    @Test
    void sameLauncherWithDifferentJvmOrModulesMustProduceDifferentImageRoots()
            throws Exception {
        Path launcher = syntheticImage();
        String baseline = OpenJdkMaskJitProbe.runtimeImageRoot(launcher);
        assertTrue(baseline.matches("[0-9a-f]{64}"));
        assertEquals(baseline, OpenJdkMaskJitProbe.runtimeImageRoot(launcher));

        Path modules = image.resolve("lib/modules");
        byte[] bytes = Files.readAllBytes(modules);
        String launcherSha = sha(Files.readAllBytes(launcher));
        bytes[bytes.length - 1] ^= 0x01;
        Files.write(modules, bytes);
        assertEquals(launcherSha, sha(Files.readAllBytes(launcher)));
        String changedModules = OpenJdkMaskJitProbe.runtimeImageRoot(launcher);
        assertNotEquals(baseline, changedModules);

        Files.write(modules, new byte[]{1, 2, 3});
        Path libjvm = image.resolve(vmPath());
        String withModules = OpenJdkMaskJitProbe.runtimeImageRoot(launcher);
        Files.writeString(libjvm, "another VM revision", StandardCharsets.UTF_8);
        String withNewVm = OpenJdkMaskJitProbe.runtimeImageRoot(launcher);
        assertNotEquals(withModules, withNewVm);

        Files.writeString(image.resolve("release"), "JAVA_VERSION=22", StandardCharsets.UTF_8);
        assertNotEquals(withNewVm, OpenJdkMaskJitProbe.runtimeImageRoot(launcher));
    }

    @Test
    void missingVmModulesOrReleaseRefuseAQualifiedRuntimeRoot() throws Exception {
        Path launcher = syntheticImage();
        Path modules = image.resolve("lib/modules");
        Files.delete(modules);
        assertThrows(NoSuchFileException.class,
                () -> OpenJdkMaskJitProbe.runtimeImageRoot(launcher));
        Files.write(modules, new byte[]{3});
        Files.delete(image.resolve(vmPath()));
        assertThrows(NoSuchFileException.class,
                () -> OpenJdkMaskJitProbe.runtimeImageRoot(launcher));
        Files.createDirectories(image.resolve(vmPath()).getParent());
        Files.write(image.resolve(vmPath()), new byte[]{7});
        Files.delete(image.resolve("release"));
        assertThrows(NoSuchFileException.class,
                () -> OpenJdkMaskJitProbe.runtimeImageRoot(launcher));
    }

    @Test
    void rejectsArbitraryExecutableOutsideJdkBin() throws Exception {
        Path arbitrary = image.resolve("not-a-jdk");
        Files.writeString(arbitrary, "launcher", StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class,
                () -> OpenJdkMaskJitProbe.runtimeImageRoot(arbitrary));
    }

    private Path syntheticImage() throws Exception {
        String executable = System.getProperty("os.name", "").toLowerCase(Locale.ROOT)
                .contains("win") ? "java.exe" : "java";
        Path launcher = image.resolve("bin").resolve(executable);
        Path vm = image.resolve(vmPath());
        Files.createDirectories(launcher.getParent());
        Files.createDirectories(vm.getParent());
        Files.createDirectories(image.resolve("lib"));
        Files.writeString(launcher, "same exact Java 21 launcher", StandardCharsets.UTF_8);
        Files.writeString(vm, "original VM revision", StandardCharsets.UTF_8);
        byte[] modules = new byte[2 * 64 * 1024 + 17];
        Arrays.fill(modules, (byte) 5);
        Files.write(image.resolve("lib/modules"), modules);
        Files.writeString(image.resolve("release"), "JAVA_VERSION=21", StandardCharsets.UTF_8);
        return launcher;
    }

    private static String vmPath() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return os.contains("win") ? "bin/server/jvm.dll"
                : os.contains("mac") || os.contains("darwin")
                ? "lib/server/libjvm.dylib" : "lib/server/libjvm.so";
    }

    private static String sha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    @Test
    void nativeAndJitProofsResolveTheSameCurrentFullJdkImageRoot() throws Exception {
        String root = OpenJdkMaskJitProbe.currentRuntimeRoot();
        assertTrue(root.matches("[0-9a-f]{64}"));
        assertEquals(root, OpenJdkMaskJitProbe.currentRuntimeRoot());
    }

    @Test
    void unknownInvocationCannotProduceAPassingJitReceipt() {
        assertThrows(IllegalArgumentException.class,
                () -> OpenJdkMaskJitProbe.main(new String[]{"--child", "extra"}));
    }

    @Test
    void missingTargetJvmCannotProduceAPassingJitReceipt() {
        assertThrows(NoSuchFileException.class,
                () -> OpenJdkMaskJitProbe.main(
                        new String[]{"--verify", "/m3jdk21-not-installed-in-proof-crate/bin/java"}));
    }
}
