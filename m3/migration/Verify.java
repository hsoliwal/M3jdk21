// SPDX-License-Identifier: Apache-2.0
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Run with: java m3/migration/Verify.java [repository-root]. No downloads, Maven, or JDK replacement. */
final class Verify {
  private static Path root;
  private static Path work;
  private static final List<String> transcript = new ArrayList<>();
  private Verify() {}
  public static void main(String[] args) throws Exception {
    if (Runtime.version().feature() != 21) throw new IllegalArgumentException("run this acceptance slice with Java 21");
    root = Path.of(args.length == 0 ? "." : args[0]).toRealPath();
    work = Files.createTempDirectory("m3-verification-");
    Path tool = dir("tool"), port = dir("port"), tests = dir("tests"), original = dir("source"), originalClasses = dir("original");
    try {
      compile(tool, null, List.of(root.resolve("m3/migration/src"), root.resolve("m3/migration/tests-tool")));
      run(tool.toString(), "com.m3.migration.JsonTest");
      run(tool.toString(), "com.m3.migration.PortRecipeTest");
      run(tool.toString(), "com.m3.migration.SourceInventoryTest");
      run(tool.toString(), "com.m3.migration.ManifestValidatorTest", root.toString());
      run(tool.toString(), "com.m3.migration.M3PortRecipe", "check", root.toString(), "-", root.toString(), "--snapshot");
      compile(port, null, List.of(root.resolve("m3/ports/indexstring/src")));
      compile(tests, port, List.of(root.resolve("m3/migration/tests")));
      String cp = tests + java.io.File.pathSeparator + port;
      String legacyPort = run(cp, "com.synexia.indexstring.LegacyContractTest");
      run(cp, "com.synexia.indexstring.RequiredApiTest");
      run(cp, "com.synexia.indexstring.M3TextTest");
      run(tool.toString(), "com.m3.migration.M3PortRecipe", "extract", root.toString(), original.toString(), root.toString());
      compile(originalClasses, null, List.of(original));
      Path legacyOnly = dir("legacy-test");
      compile(legacyOnly, originalClasses, List.of(root.resolve("m3/migration/tests/com/synexia/indexstring/LegacyContractTest.java")));
      String legacySource = run(legacyOnly + java.io.File.pathSeparator + originalClasses, "com.synexia.indexstring.LegacyContractTest");
      if (!legacySource.equals(legacyPort)) throw new AssertionError("legacy differential output mismatch");
      Path replay = dir("replay");
      run(tool.toString(), "com.m3.migration.M3PortRecipe", "apply", root.toString(), original.toString(), replay.toString(), "--snapshot");
      run(tool.toString(), "com.m3.migration.M3PortRecipe", "apply", root.toString(), original.toString(), replay.toString(), "--snapshot");
      run(tool.toString(), "com.m3.migration.M3PortRecipe", "check", root.toString(), "-", replay.toString());
      run(tool.toString(), "com.m3.migration.M3PortRecipe", "rollback", root.toString(), "-", replay.toString());
      Path consumer = work.resolve("Consumer.java");
      Files.writeString(consumer, "import com.m3.indexstring.M3Text; class Consumer { public static void main(String[] args) { if (!M3Text.fromString(\"a\").concat(M3Text.fromString(\"b\")).toString().equals(\"ab\")) throw new AssertionError(); System.out.println(\"NAMED_MODULE consumer passed\"); } }\n");
      command(List.of(binary("javac"), "--release", "21", "-Xlint:all", "-Werror", "--module-path", port.toString(), "--add-modules", "com.mthree.indexstring", "-d", dir("consumer").toString(), consumer.toString()));
      command(List.of(binary("java"), "--module-path", port.toString(), "--add-modules", "com.mthree.indexstring", "-cp", work.resolve("consumer").toString(), "Consumer"));
      transcript.add("RESULT pass: exact local file-set acceptance only; not full JDK, compiler route, native, repository-wide or hosted CI acceptance.");
      System.out.println(transcript.get(transcript.size() - 1));
    } finally {
      Files.write(work.resolve("commands-and-results.txt"), transcript, StandardCharsets.UTF_8);
      System.out.println("VERIFICATION_LOG " + work.resolve("commands-and-results.txt"));
    }
  }
  private static Path dir(String name) throws IOException { return Files.createDirectories(work.resolve(name)); }
  private static String binary(String name) { return Path.of(System.getProperty("java.home"), "bin", name).toString(); }
  private static void compile(Path output, Path classpath, List<Path> roots) throws Exception {
    List<String> command = new ArrayList<>(List.of(binary("javac"), "--release", "21", "-Xlint:all", "-Werror", "-d", output.toString()));
    if (classpath != null) command.addAll(List.of("-cp", classpath.toString()));
    for (Path entry : roots) {
      if (Files.isRegularFile(entry)) command.add(entry.toString());
      else try (var files = Files.walk(entry)) { command.addAll(files.filter(p -> p.toString().endsWith(".java")).sorted().map(Path::toString).toList()); }
    }
    command(command);
  }
  private static String run(String classpath, String main, String... args) throws Exception {
    List<String> command = new ArrayList<>(List.of(binary("java"), "-cp", classpath, main)); command.addAll(List.of(args)); return command(command);
  }
  private static String command(List<String> command) throws Exception {
    transcript.add("COMMAND " + String.join(" ", command));
    Process process = new ProcessBuilder(command).directory(root.toFile()).redirectErrorStream(true).start();
    String text = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    int exit = process.waitFor(); transcript.add(text); transcript.add("EXIT " + exit);
    if (!text.isBlank()) System.out.print(text);
    if (exit != 0) throw new IllegalStateException("command failed (" + exit + "): " + command.get(0));
    return text;
  }
}
