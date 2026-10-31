// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.smartbuild;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class M3SmartBuildReceiverTest {
  @TempDir Path temp;

  @Test void verifiedJarBuildsAnExactNoShellCommand() throws Exception {
    Path root = Files.createDirectory(temp.resolve("jdk"));
    Path module = Files.createDirectories(root.resolve("m3/file"));
    Files.writeString(module.resolve("pom.xml"), "<project/>");
    Path jar = temp.resolve("smart-build.jar");
    Files.writeString(jar, "reviewed-java21-build-tool");
    Path modules = temp.resolve("reactor.txt");
    Files.writeString(modules, ".\n");
    M3SmartBuildReceiver.Request input = new M3SmartBuildReceiver.Request(
        root, jar, hash(jar), "a".repeat(40), temp.resolve("evidence"), "m3/file",
        "mvn", modules, null, "http://127.0.0.1:11435", false);
    List<String> command = M3SmartBuildReceiver.command(input);
    assertTrue(command.contains("--maven-project"));
    assertTrue(command.contains("m3/file"));
    assertTrue(command.contains("--active-modules"));
    assertTrue(command.contains("--chrome-llm-origin"));
    assertFalse(command.contains("--run"));
    assertFalse(command.contains("-c"));
  }

  @Test void hashAndJdkScopeFailClosed() throws Exception {
    Path root = Files.createDirectory(temp.resolve("jdk"));
    Path m3 = Files.createDirectories(root.resolve("m3"));
    Files.writeString(m3.resolve("pom.xml"), "<project/>");
    Path jar = temp.resolve("smart-build.jar");
    Files.writeString(jar, "the-file");
    M3SmartBuildReceiver.Request candidate = new M3SmartBuildReceiver.Request(
        root, jar, "0".repeat(64), "a".repeat(40), temp.resolve("outside"),
        "m3", "mvn", null, null, null, true);
    assertThrows(java.io.IOException.class, () -> M3SmartBuildReceiver.command(candidate));
    assertThrows(IllegalArgumentException.class, () -> new M3SmartBuildReceiver.Request(
        root, jar, hash(jar), "a".repeat(40), temp.resolve("outside"),
        "src/hotspot", "mvn", null, null, null, true));
    assertThrows(IllegalArgumentException.class, () -> new M3SmartBuildReceiver.Request(
        root, jar, hash(jar), "a".repeat(40), root.resolve("m3/out"),
        "m3", "mvn", null, null, null, true));
    assertThrows(IllegalArgumentException.class, () -> new M3SmartBuildReceiver.Request(
        root, jar, hash(jar), "a".repeat(40), temp.resolve("outside"),
        "m3", "mvn", null, "http://remote.example/cache", null, true));
  }

  @Test void optionalCacheAndMavenExecutableRemainExplicit() throws Exception {
    Path root = Files.createDirectory(temp.resolve("jdk"));
    Path m3 = Files.createDirectories(root.resolve("m3"));
    Files.writeString(m3.resolve("pom.xml"), "<project/>");
    Path jar = temp.resolve("smart-build.jar");
    Files.writeString(jar, "correct-jar-bytes");
    M3SmartBuildReceiver.Request request = new M3SmartBuildReceiver.Request(
        root, jar, hash(jar), "b".repeat(40), temp.resolve("outside"), "m3",
        "mvn", null, "https://cache.example.invalid/reviewed", null, true);
    List<String> args = M3SmartBuildReceiver.command(request);
    assertTrue(args.stream().filter("--remote-cache-url"::equals).count() == 1);
    assertEquals("https://cache.example.invalid/reviewed",
        args.get(args.indexOf("--remote-cache-url") + 1));
    assertEquals("--run", args.getLast());
  }

  private static String hash(Path path) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
        .digest(Files.readAllBytes(path)));
  }
}
