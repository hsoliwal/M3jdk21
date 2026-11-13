/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Resolves and identifies one Java toolchain. */
public record M3JdkTools(Path javaHome, Path java, Path javac, Path jar, Path jdeps, Path jlink,
                         String runtimeIdentity) {

    /** Resolves tools from an explicit home or the current JVM. */
    public static M3JdkTools resolve(Path requestedHome) throws IOException, InterruptedException {
        Path home = requestedHome == null ? Path.of(System.getProperty("java.home")) : requestedHome;
        home = home.toAbsolutePath().normalize();
        Path java = executable(home, "java");
        Path javac = executable(home, "javac");
        Path jar = executable(home, "jar");
        Path jdeps = executable(home, "jdeps");
        Path jlink = executable(home, "jlink");
        M3Process.Result version = M3Process.requireSuccess(
                List.of(java.toString(), "-XshowSettings:properties", "-version"),
                home, Duration.ofSeconds(30), Map.of());
        String identityText = version.stdout() + version.stderr();
        String identity = M3Hashing.sha256(identityText);
        return new M3JdkTools(home, java, javac, jar, jdeps, jlink, identity);
    }

    private static Path executable(Path home, String name) throws IOException {
        String suffix = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win") ? ".exe" : "";
        Path path = home.resolve("bin").resolve(name + suffix);
        if (!Files.isRegularFile(path)) {
            throw new IOException("Missing JDK tool: " + path);
        }
        return path;
    }
}
