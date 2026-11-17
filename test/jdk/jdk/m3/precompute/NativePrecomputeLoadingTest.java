// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;

/** Exercises the real optional loader in fresh VMs, without fake native libraries. */
final class NativePrecomputeLoadingTest {
  private static final String PREFIX = "m3.precompute";
  /** NCM batches load from the single jdk.m3.llm NCM native owner, library m3_ncm. */
  private static final String NCM_LIBRARY = "m3.ncm.library";

  @TempDir Path temporary;

  @TestFactory
  List<DynamicTest> optionalConfigurationFailuresKeepJavaAvailable() throws IOException {
    Path search = Files.createDirectory(temporary.resolve("empty-search"));
    Path missing = temporary.resolve("missing-library");
    Path empty = Files.writeString(temporary.resolve("empty.txt"), " \r\n\t");
    Path malformed = Files.write(temporary.resolve("malformed.txt"), new byte[] {(byte) 0xff});
    Path directory = Files.createDirectory(temporary.resolve("not-a-file"));
    return List.of(
        scenario("unconfigured empty library search", "none", search, Map.of(), "none"),
        scenario("disabled ignores missing configuration", "none", search,
            Map.of(property("disableNative"), "true", property("native.path"), missing.toString(),
                property("native.pathFile"), missing.toString()), "none"),
        scenario("missing explicit library uses Java", "none", search,
            Map.of(property("native.path"), missing.toString()), "none"),
        scenario("missing path file uses Java", "none", search,
            Map.of(property("native.pathFile"), missing.toString()), "none"),
        scenario("empty path file uses Java", "none", search,
            Map.of(property("native.pathFile"), empty.toString()), "none"),
        scenario("directory path file uses Java", "none", search,
            Map.of(property("native.pathFile"), directory.toString()), "none"),
        scenario("malformed UTF-8 path file uses Java", "none", search,
            Map.of(property("native.pathFile"), malformed.toString()), "none"),
        scenario("invalid explicit path uses Java", "none", search, Map.of(), "invalid-explicit"),
        scenario("invalid path-file name uses Java", "none", search, Map.of(), "invalid-pointer"));
  }

  @TestFactory
  List<DynamicTest> nativeProfileRequiresConfiguredLibraryAndPrecedence() throws IOException {
    if (!Boolean.getBoolean(property("requireNcmNative"))) return List.of();
    Path library = configuredLibrary();
    assertTrue(Files.isRegularFile(library), "native profile must produce a real library");
    Path ncm = configuredNcmLibrary();
    assertTrue(Files.isRegularFile(ncm), "native profile must produce the NCM owner library");
    Path ncmPointer = Files.writeString(temporary.resolve("ncm library pointer.txt"),
        " \n" + ncm + "\r\n", StandardCharsets.UTF_8);
    Path search = Files.createDirectory(temporary.resolve("isolated-search"));
    Path pointer = Files.writeString(temporary.resolve("native library pointer.txt"),
        " \n" + library + "\r\n", StandardCharsets.UTF_8);
    Path missing = temporary.resolve("missing-library");
    Path empty = Files.writeString(temporary.resolve("empty-pointer.txt"), " \n");
    return List.of(
        scenario("configured path file works outside library search", "current", "current", search,
            withNcm(ncm, property("native.pathFile"), pointer.toString()), "none"),
        scenario("explicit library wins over missing path file", "current", "current", search,
            withNcm(ncm, property("native.path"), library.toString(),
                property("native.pathFile"), missing.toString()), "none"),
        scenario("blank explicit path delegates to path file", "current", "current", search,
            withNcm(ncm, property("native.path"), " \t", property("native.pathFile"), pointer.toString()), "none"),
        scenario("loadLibrary remains the unconfigured default", "current", "none", library.getParent(),
            Map.of(), "none"),
        scenario("blank path-file setting delegates to loadLibrary", "current", "none", library.getParent(),
            Map.of(property("native.pathFile"), " \t"), "none"),
        scenario("disabled dominates a valid configured library", "none", "none", library.getParent(),
            withNcm(ncm, property("disableNative"), "true", property("native.path"), library.toString(),
                property("native.pathFile"), pointer.toString()), "none"),
        scenario("missing explicit library does not fall through to a valid pointer", "none", "current",
            library.getParent(), withNcm(ncm, property("native.path"), missing.toString(),
                property("native.pathFile"), pointer.toString()), "none"),
        scenario("missing configured pointer does not search another library", "none", "none", library.getParent(),
            Map.of(property("native.pathFile"), missing.toString()), "none"),
        scenario("empty configured pointer does not search another library", "none", "none", library.getParent(),
            Map.of(property("native.pathFile"), empty.toString()), "none"),
        scenario("NCM path file locates the owner library", "current", "current", search,
            Map.of(property("native.path"), library.toString(), NCM_LIBRARY + ".pathFile", ncmPointer.toString()),
            "none"),
        scenario("missing NCM owner library keeps the Java oracle", "current", "none", search,
            Map.of(property("native.path"), library.toString(), NCM_LIBRARY, missing.toString()), "none"),
        scenario("precompute library alone does not provide NCM batches", "current", "none", search,
            Map.of(property("native.path"), library.toString(), NCM_LIBRARY, library.toString()), "none"));
  }

  private static Map<String, String> withNcm(Path ncm, String... pairs) {
    Map<String, String> result = new TreeMap<>();
    for (int i = 0; i < pairs.length; i += 2) result.put(pairs[i], pairs[i + 1]);
    result.put(NCM_LIBRARY, ncm.toString());
    return result;
  }

  private static String property(String suffix) { return PREFIX + "." + suffix; }

  private static Path configuredLibrary() throws IOException {
    String explicit = System.getProperty(property("native.path"));
    if (explicit != null && !explicit.isBlank()) return Path.of(explicit).toAbsolutePath().normalize();
    String pointer = System.getProperty(property("native.pathFile"));
    assertTrue(pointer != null && !pointer.isBlank(),
        "required native profile must supply an explicit library or CMake path file");
    String value = Files.readString(Path.of(pointer), StandardCharsets.UTF_8).strip();
    assertTrue(!value.isEmpty(), "CMake native path file must not be empty");
    return Path.of(value).toAbsolutePath().normalize();
  }

  private static Path configuredNcmLibrary() throws IOException {
    String explicit = System.getProperty(NCM_LIBRARY);
    if (explicit != null && !explicit.isBlank()) return Path.of(explicit).toAbsolutePath().normalize();
    String pointer = System.getProperty(NCM_LIBRARY + ".pathFile");
    assertTrue(pointer != null && !pointer.isBlank(),
        "required native profile must supply the NCM owner library or its CMake path file");
    String value = Files.readString(Path.of(pointer), StandardCharsets.UTF_8).strip();
    assertTrue(!value.isEmpty(), "NCM CMake path file must not be empty");
    return Path.of(value).toAbsolutePath().normalize();
  }

  private DynamicTest scenario(String name, String expected, Path search,
                               Map<String, String> properties, String adjustment) {
    return scenario(name, expected, "none", search, properties, adjustment);
  }

  private DynamicTest scenario(String name, String expected, String expectedNcm, Path search,
                               Map<String, String> properties, String adjustment) {
    return DynamicTest.dynamicTest(name, () -> probe(expected, expectedNcm, search, properties, adjustment));
  }

  private void probe(String expected, String expectedNcm, Path search, Map<String, String> properties,
                     String adjustment) throws Exception {
    String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
    String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
    List<String> command = new ArrayList<>(List.of(
        Path.of(System.getProperty("java.home"), "bin", executable).toString(),
        "-ea", "-Xcheck:jni", "-Xmx128m", "-cp", classpath,
        "-Djava.library.path=" + search, "-D" + property("disableNative") + "=false"));
    for (Map.Entry<String, String> entry : new TreeMap<>(properties).entrySet()) {
      command.add("-D" + entry.getKey() + "=" + entry.getValue());
    }
    command.add(Probe.class.getName());
    command.add(expected);
    command.add(adjustment);
    command.add(expectedNcm);
    Path log = Files.createTempFile(temporary, "loader-probe-", ".log");
    Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
    boolean completed = process.waitFor(30, TimeUnit.SECONDS);
    if (!completed) {
      process.destroyForcibly();
      process.waitFor(5, TimeUnit.SECONDS);
    }
    String output = Files.readString(log, StandardCharsets.UTF_8);
    assertTrue(completed, () -> "native loader probe timed out: " + output);
    assertEquals(0, process.exitValue(), () -> "native loader probe failed: " + output);
    assertTrue(output.contains("NATIVE_PRECOMPUTE_LOADING_PASS " + expected), () -> output);
  }

  /** Subprocess entry point, so class initialization and native loading never leak between cases. */
  public static final class Probe {
    private Probe() {}

    public static void main(String[] args) {
      if (args.length != 3) {
        throw new IllegalArgumentException("expected state, property adjustment and NCM state");
      }
      switch (args[1]) {
        case "none" -> { }
        case "invalid-explicit" -> System.setProperty(property("native.path"), "\u0000");
        case "invalid-pointer" -> System.setProperty(property("native.pathFile"), "\u0000");
        default -> throw new IllegalArgumentException("unknown property adjustment");
      }
      boolean current = switch (args[0]) {
        case "none" -> false;
        case "current" -> true;
        default -> throw new IllegalArgumentException("unknown expected native state");
      };
      check(NativePrecomputeOps.available() == current, "pair ABI availability");
      check(NativePrecomputeOps.tuplesAvailable() == current, "tuple ABI availability");
      boolean ncm = switch (args[2]) {
        case "none" -> false;
        case "current" -> true;
        default -> throw new IllegalArgumentException("unknown expected NCM state");
      };
      check(NativePrecomputeOps.ncmCodebookAvailable() == ncm, "NCM owner ABI availability");
      int[] codes = NcmCodebook.nearest(new double[] {1, 2, 3, 4}, 0, 2,
          new double[] {1, 2, 3, 4}, 0, 2, 2, 0);
      check(Arrays.equals(new int[] {0, 1}, codes), "numeric behavior after optional loading");
      System.out.println("NATIVE_PRECOMPUTE_LOADING_PASS " + args[0]);
    }

    private static void check(boolean condition, String message) {
      if (!condition) throw new AssertionError(message);
    }
  }
}
