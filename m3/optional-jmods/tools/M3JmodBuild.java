// SPDX-License-Identifier: Apache-2.0
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

/**
 * JDK21-only module compiler, exact one-JMOD-per-tool packager and executable
 * source/descriptor/self-test receipt. Uses javac/jmod/java from java.home,
 * never Maven's ambient launcher, Unix tools, WSL, or the public network.
 */
public final class M3JmodBuild {
    private M3JmodBuild() { }
    private record Tool(String module, String main, String family) { }
    private static final String PASS = "M3_JMOD_TEST_PASS ";

    public static void main(String[] args) throws Exception {
        if (args.length != 2 || !List.of("verify", "build", "catalog").contains(args[0]))
            throw new IllegalArgumentException("usage: verify|build|catalog moduleRoot");
        if (Runtime.version().feature() != 21)
            throw new IllegalStateException("Use JDK 21 for Java 21 target JMODs");
        Path root = Path.of(args[1]).toAbsolutePath().normalize();
        List<Tool> tools = load(root.resolve("tool-modules.tsv"));
        if (args[0].equals("catalog")) {
            System.out.println("M3_JMOD_CATALOGUE_COUNT=" + tools.size());
            return;
        }
        Path classes = root.resolve("target/classes"), jmods = root.resolve("target/jmods");
        Files.createDirectories(classes);
        Files.createDirectories(jmods);
        Set<String> actual = new HashSet<>();
        try (var folders = Files.list(root.resolve("modules"))) {
            folders.filter(Files::isDirectory).forEach(p -> actual.add(p.getFileName().toString()));
        }
        Set<String> expected = new HashSet<>();
        for (Tool tool : tools) expected.add(tool.module());
        if (!actual.equals(expected))
            throw new IllegalStateException("unadmitted module directories " + actual);
        StringBuilder report = new StringBuilder(
                "module\tmain\tsource-sha256\tjmod-sha256\tjmod-bytes\tselftest\n");
        for (Tool tool : tools) {
            Path source = root.resolve("modules").resolve(tool.module()).resolve("src/main/java");
            Path descriptor = source.resolve("module-info.java");
            Path implementation = source.resolve(tool.main().replace('.', '/') + ".java");
            requireRegular(descriptor, source);
            requireRegular(implementation, source);
            String text = Files.readString(descriptor, StandardCharsets.UTF_8);
            if (!text.contains("module " + tool.module() + " {")
                    || !text.contains("exports " + tool.module() + ";"))
                throw new IllegalStateException("module descriptor drift " + tool.module());
            Path compiled = classes.resolve(tool.module());
            Files.createDirectories(compiled);
            execute(List.of(binary("javac"), "--release", "21", "-Xlint:all,-module", "-Werror",
                    "-d", compiled.toString(), descriptor.toString(), implementation.toString()));
            Path image = jmods.resolve(tool.module() + ".jmod");
            Files.deleteIfExists(image);
            packageModule(compiled, tool.main(), image);
            String details = execute(List.of(binary("jmod"), "describe", image.toString()));
            if (!details.lines().findFirst().orElse("").equals(tool.module())
                    || !details.contains("exports " + tool.module()))
                throw new IllegalStateException("invalid JMOD contents " + tool.module());
            String output = execute(List.of(binary("java"), "-Djava.awt.headless=true",
                    "--module-path", classes.toString(), "-m",
                    tool.module() + "/" + tool.main(), "--self-test"));
            String expectedOutput = PASS + tool.module().substring("com.synexia.m3.".length());
            if (!output.strip().equals(expectedOutput))
                throw new IllegalStateException("failed exact selftest: " + output);
            String digest = sha256(Files.readAllBytes(image));
            if (args[0].equals("verify")) {
                Path repeated = jmods.resolve(tool.module() + ".repeat.jmod");
                Files.deleteIfExists(repeated);
                packageModule(compiled, tool.main(), repeated);
                if (!digest.equals(sha256(Files.readAllBytes(repeated))))
                    throw new IllegalStateException("JMOD not byte-reproducible " + tool.module());
                Files.delete(repeated);
            }
            report.append(tool.module()).append('\t').append(tool.main()).append('\t')
                    .append(sha256(Files.readAllBytes(descriptor), Files.readAllBytes(implementation)))
                    .append('\t').append(digest).append('\t').append(Files.size(image))
                    .append("\tPASS\n");
            System.out.println(expectedOutput);
        }
        Files.writeString(root.resolve("target/JMOD_BUILD.tsv"), report.toString(), StandardCharsets.UTF_8);
        System.out.println("M3_JMOD_BUILD_PASS modules=" + tools.size());
    }

    private static void packageModule(Path compiled, String main, Path output)
            throws IOException, InterruptedException {
        execute(List.of(binary("jmod"), "create", "--date", "2026-10-14T00:00:00Z",
                "--class-path", compiled.toString(), "--main-class", main, output.toString()));
    }
    private static void requireRegular(Path path, Path base) throws IOException {
        if (!path.normalize().startsWith(base) || !Files.isRegularFile(path)
                || Files.isSymbolicLink(path))
            throw new IOException("untrusted source path " + path);
    }
    private static String binary(String name) {
        String suffix = System.getProperty("os.name").toLowerCase().contains("win") ? ".exe" : "";
        return Path.of(System.getProperty("java.home"), "bin", name + suffix).toString();
    }
    private static String execute(List<String> command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        byte[] data = process.getInputStream().readAllBytes();
        int status = process.waitFor();
        String result = new String(data, StandardCharsets.UTF_8);
        if (status != 0) throw new IOException(
                "JDK tool rc=" + status + " command=" + command + " output=" + result);
        return result;
    }
    private static String sha256(byte[]... content) throws Exception {
        MessageDigest hash = MessageDigest.getInstance("SHA-256");
        for (byte[] part : content) hash.update(part);
        return HexFormat.of().formatHex(hash.digest());
    }
    private static List<Tool> load(Path path) throws IOException {
        List<Tool> tools = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#") || line.startsWith("module\t")) continue;
            String[] cells = line.split("\t", -1);
            if (cells.length != 5 || !cells[0].matches("com[.]synexia[.]m3[.][a-z]+([.][a-z]+)+")
                    || !cells[1].matches(cells[0].replace(".", "[.]") + "[.][A-Za-z][A-Za-z0-9]*")
                    || !Set.of("IMPLEMENTED", "ADAPTER_PENDING").contains(cells[3])
                    || !cells[4].equals("Apache-2.0") || !seen.add(cells[0]))
                throw new IOException("invalid/duplicate tool module: " + line);
            tools.add(new Tool(cells[0], cells[1], cells[2]));
        }
        int admitted;
        try {
            admitted = Integer.parseInt(Files.readString(
                    path.resolveSibling("tool-count.txt"), StandardCharsets.UTF_8).strip());
        } catch (NumberFormatException invalid) {
            throw new IOException("invalid admitted module count", invalid);
        }
        if (admitted < 1 || tools.size() != admitted)
            throw new IOException("expected " + admitted + " admitted independent module implementations");
        return List.copyOf(tools);
    }
}
