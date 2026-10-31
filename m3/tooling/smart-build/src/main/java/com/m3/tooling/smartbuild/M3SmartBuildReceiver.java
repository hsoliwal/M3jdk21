// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.smartbuild;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Opt-in M3JDK21 receiver for the independently source-sealed Synexia Maven SmartBuild CLI.
 * No Maven, compiler, JNI, cache, source mutation, or model authority is implemented here.
 * All command tokens are explicit; the receiver never invokes a command shell.
 */
public final class M3SmartBuildReceiver {
  private static final long MAX_JAR_BYTES = 128L * 1024L * 1024L;
  private static final Duration MAX_RUNTIME = Duration.ofMinutes(90);
  private M3SmartBuildReceiver() {}

  public record Request(Path jdkRoot, Path smartBuildJar, String jarSha256,
      String baselineCommit, Path outsideEvidence, String project,
      String mavenExecutable, Path activeModules, String remoteCacheUrl,
      String chromeOrigin, boolean run) {
    public Request {
      jdkRoot = realDirectory(jdkRoot, "jdkRoot");
      smartBuildJar = regularFile(smartBuildJar, "smartBuildJar");
      jarSha256 = sha(jarSha256);
      baselineCommit = Objects.requireNonNull(baselineCommit, "baselineCommit");
      if (!baselineCommit.matches("[0-9a-f]{40}|[0-9a-f]{64}")) {
        throw new IllegalArgumentException("baseline commit must be a pinned Git object ID");
      }
      outsideEvidence = Objects.requireNonNull(outsideEvidence, "outsideEvidence")
          .toAbsolutePath().normalize();
      if (outsideEvidence.startsWith(jdkRoot) || smartBuildJar.startsWith(jdkRoot)) {
        throw new IllegalArgumentException("tool jar and evidence must live outside JDK checkout");
      }
      project = projectPath(project, jdkRoot);
      mavenExecutable = Objects.requireNonNull(mavenExecutable, "mavenExecutable");
      if (!mavenExecutable.equals("mvn") && !mavenExecutable.equals("mvn.cmd")) {
        if (!Path.of(mavenExecutable).isAbsolute()) {
          throw new IllegalArgumentException("Maven binary must be absolute");
        }
        Path binary = regularFile(Path.of(mavenExecutable), "mavenExecutable");
        mavenExecutable = binary.toString();
      }
      if (activeModules != null) {
        activeModules = regularFile(activeModules, "activeModules");
        if (activeModules.startsWith(jdkRoot)) {
          throw new IllegalArgumentException("reactor inventory must live outside checkout");
        }
      }
      if (remoteCacheUrl != null) {
        java.net.URI uri = java.net.URI.create(remoteCacheUrl);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
            || uri.getRawUserInfo() != null || uri.getRawQuery() != null
            || uri.getRawFragment() != null) {
          throw new IllegalArgumentException("remote cache requires HTTPS without credentials");
        }
      }
      if (chromeOrigin != null) {
        java.net.URI uri = java.net.URI.create(chromeOrigin);
        if (!"http".equalsIgnoreCase(uri.getScheme()) || uri.getPort() < 1
            || !Set.of("127.0.0.1", "[::1]", "::1").contains(uri.getHost())
            || uri.getRawUserInfo() != null || uri.getRawQuery() != null
            || uri.getRawFragment() != null) {
          throw new IllegalArgumentException("ChromeML must use explicit numeric loopback");
        }
      }
    }
  }

  /** Validates every admitted path and artifact hash, then returns command arguments only. */
  public static List<String> command(Request request) throws IOException {
    Request checked = Objects.requireNonNull(request, "request");
    if (!contentHash(checked.smartBuildJar()).equals(checked.jarSha256())) {
      throw new IOException("unapproved SmartBuild jar SHA-256");
    }
    Path java = Path.of(System.getProperty("java.home"), "bin", windows() ? "java.exe" : "java");
    if (!Files.isRegularFile(java, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("Java 21 executable unavailable");
    }
    ArrayList<String> args = new ArrayList<>();
    args.add(java.toString());
    args.add("-cp");
    args.add(checked.smartBuildJar().toString());
    args.add("com.synexia.build.inventory.RepositorySmartBuildCli");
    args.add("--root"); args.add(checked.jdkRoot().toString());
    args.add("--maven-project"); args.add(checked.project());
    args.add("--maven-executable"); args.add(checked.mavenExecutable());
    args.add("--base"); args.add(checked.baselineCommit());
    args.add("--out"); args.add(checked.outsideEvidence().toString());
    if (checked.activeModules() != null) {
      args.add("--active-modules"); args.add(checked.activeModules().toString());
    }
    if (checked.remoteCacheUrl() != null) {
      args.add("--remote-cache-url"); args.add(checked.remoteCacheUrl());
    }
    if (checked.chromeOrigin() != null) {
      args.add("--chrome-llm-origin"); args.add(checked.chromeOrigin());
    }
    if (checked.run()) args.add("--run");
    return List.copyOf(args);
  }

  /** Runs the reviewed canonical tool; its Maven exit code remains authoritative. */
  public static int run(Request request) throws IOException, InterruptedException {
    List<String> args = command(request);
    Process process = new ProcessBuilder(args).inheritIO().start();
    try {
      if (!process.waitFor(MAX_RUNTIME.toSeconds(), TimeUnit.SECONDS)) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
        throw new IOException("M3_SMART_BUILD_RECEIVER_TIMEOUT");
      }
      return process.exitValue();
    } catch (InterruptedException interrupted) {
      process.descendants().forEach(ProcessHandle::destroyForcibly);
      process.destroyForcibly();
      Thread.currentThread().interrupt();
      throw interrupted;
    }
  }

  public static void main(String[] arguments) throws Exception {
    Map<String, String> flags = parse(arguments);
    Request request = new Request(
        Path.of(required(flags, "--jdk-root")),
        Path.of(required(flags, "--smartbuild-jar")),
        required(flags, "--jar-sha256"),
        required(flags, "--baseline"),
        Path.of(required(flags, "--evidence")),
        flags.getOrDefault("--project", "m3"),
        required(flags, "--maven-executable"),
        flags.containsKey("--active-modules") ? Path.of(flags.get("--active-modules")) : null,
        flags.get("--remote-cache-url"),
        flags.get("--chrome-origin"),
        flags.containsKey("--run"));
    int exit = run(request);
    if (exit != 0) System.exit(exit);
  }

  private static Map<String, String> parse(String[] arguments) {
    Set<String> known = Set.of("--jdk-root", "--smartbuild-jar", "--jar-sha256",
        "--baseline", "--evidence", "--project", "--maven-executable",
        "--active-modules", "--remote-cache-url", "--chrome-origin", "--run");
    LinkedHashMap<String, String> result = new LinkedHashMap<>();
    for (int i = 0; i < arguments.length; i++) {
      String key = arguments[i];
      if (!known.contains(key) || result.containsKey(key)) {
        throw new IllegalArgumentException("unknown/duplicate argument: " + key);
      }
      if ("--run".equals(key)) { result.put(key, "true"); continue; }
      if (++i >= arguments.length || arguments[i].isBlank()) {
        throw new IllegalArgumentException("missing argument: " + key);
      }
      result.put(key, arguments[i]);
    }
    return Map.copyOf(result);
  }

  private static String required(Map<String, String> flags, String name) {
    String value = flags.get(name);
    if (value == null || value.isBlank()) throw new IllegalArgumentException("missing " + name);
    return value;
  }

  private static String projectPath(String project, Path root) {
    String value = Objects.requireNonNull(project, "project");
    if (!value.equals("m3") && !value.startsWith("m3/")) {
      throw new IllegalArgumentException("only m3 Maven project receivers are admitted");
    }
    Path candidate = root;
    for (String segment : value.split("/", -1)) {
      if (!segment.matches("[A-Za-z0-9_.-]+") || segment.equals(".")
          || segment.equals("..")) {
        throw new IllegalArgumentException("invalid Maven project segment");
      }
      candidate = candidate.resolve(segment);
      if (Files.isSymbolicLink(candidate)) {
        throw new IllegalArgumentException("Maven project symlink");
      }
    }
    if (!Files.isDirectory(candidate, LinkOption.NOFOLLOW_LINKS)
        || !Files.isRegularFile(candidate.resolve("pom.xml"), LinkOption.NOFOLLOW_LINKS)
        || Files.isSymbolicLink(candidate.resolve("pom.xml"))) {
      throw new IllegalArgumentException("unavailable Maven project root");
    }
    return value;
  }

  private static Path regularFile(Path input, String label) {
    Path path = Objects.requireNonNull(input, label).toAbsolutePath().normalize();
    if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
        || Files.isSymbolicLink(path)) throw new IllegalArgumentException(label + " must be a real file");
    return path;
  }

  private static Path realDirectory(Path input, String label) {
    Path path = Objects.requireNonNull(input, label).toAbsolutePath().normalize();
    if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)
        || Files.isSymbolicLink(path)) throw new IllegalArgumentException(label + " must be a real dir");
    return path;
  }

  private static String sha(String value) {
    String checked = Objects.requireNonNull(value, "jarSha256");
    if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("SHA-256 required");
    return checked;
  }

  private static String contentHash(Path file) throws IOException {
    MessageDigest digest;
    try { digest = MessageDigest.getInstance("SHA-256"); }
    catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    if (Files.size(file) > MAX_JAR_BYTES) throw new IOException("SmartBuild jar exceeds limit");
    try (InputStream input = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) {
      byte[] scratch = new byte[8192];
      long read = 0;
      int n;
      while ((n = input.read(scratch)) != -1) {
        if (n > MAX_JAR_BYTES - read) throw new IOException("SmartBuild jar exceeds limit");
        digest.update(scratch, 0, n);
        read += n;
      }
    }
    return HexFormat.of().formatHex(digest.digest());
  }

  private static boolean windows() {
    return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win");
  }
}
