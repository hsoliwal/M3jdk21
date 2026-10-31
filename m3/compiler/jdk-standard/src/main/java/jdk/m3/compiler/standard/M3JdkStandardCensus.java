// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package jdk.m3.compiler.standard;

import com.sun.source.util.JavacTask;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * Parse-only census for M3-owned Java source names.
 *
 * <p>The census deliberately stops after javac parsing. Source files can therefore be checked
 * without resolving their module or dependency closure. Syntax errors still fail closed.
 */
public final class M3JdkStandardCensus {
    private static final int BATCH_SIZE = 128;
    private static final Set<String> EXCLUDED_PARTS = Set.of(
            ".git", "archive-maven-projects", "evidence", "generated-sources", "out",
            "qualification", "retained-variants", "target", "third_party", "vendor");

    private M3JdkStandardCensus() {
    }

    /**
     * Runs a repository census.
     *
     * @param args command-line arguments
     * @throws Exception if source discovery, parsing, or report writing fails
     */
    public static void main(String[] args) throws Exception {
        Configuration configuration = parseArguments(args);
        List<Path> files = configuration.pathsFile() == null
                ? discoverFiles(configuration.root())
                : loadFiles(configuration.root(), configuration.pathsFile());
        Result result = run(configuration.root(), files, configuration.report(),
                configuration.m3Scheme());
        System.out.println("M3JDK_STANDARD_CENSUS_PASS files=" + result.fileCount()
                + " findings=" + result.findingCount() + " report="
                + configuration.report());
    }

    static Result run(Path root, List<Path> files, Path report, boolean m3Scheme)
            throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        List<Path> normalizedFiles = normalizeFiles(normalizedRoot, files);
        List<String> findings = new ArrayList<>();
        List<String> parseErrors = new ArrayList<>();
        for (int start = 0; start < normalizedFiles.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, normalizedFiles.size());
            parseBatch(normalizedRoot, normalizedFiles.subList(start, end), m3Scheme,
                    findings, parseErrors);
        }
        if (!parseErrors.isEmpty()) {
            throw new IOException("M3JDK21 source census parse errors:\n"
                    + String.join("\n", parseErrors));
        }
        findings.sort(M3JdkStandardCensus::compareFindingRows);
        Path parent = report.toAbsolutePath().normalize().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        StringBuilder body = new StringBuilder("file\tline\tcolumn\trule\tmessage\n");
        findings.forEach(row -> body.append(row).append('\n'));
        Files.writeString(report, body, StandardCharsets.UTF_8);
        return new Result(normalizedFiles.size(), findings.size(), List.copyOf(findings));
    }

    static List<Path> discoverFiles(Path root) throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        try (Stream<Path> stream = Files.walk(normalizedRoot)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .filter(path -> !Files.isSymbolicLink(path))
                    .map(normalizedRoot::relativize)
                    .filter(M3JdkStandardCensus::isM3OwnedSource)
                    .sorted()
                    .toList();
        }
    }

    static boolean isM3OwnedSource(Path relative) {
        String portable = portable(relative);
        if (!portable.endsWith(".java") || isExcluded(relative)) {
            return false;
        }
        String file = relative.getFileName().toString();
        return portable.contains("/com/m3/")
                || portable.contains("/jdk/m3/")
                || portable.contains("/jdk/internal/m3/")
                || portable.contains("/com/sun/tools/javac/m3/")
                || file.startsWith("M3");
    }

    private static boolean isExcluded(Path path) {
        for (Path part : path) {
            if (EXCLUDED_PARTS.contains(part.toString())) {
                return true;
            }
        }
        return false;
    }

    private static List<Path> loadFiles(Path root, Path pathsFile) throws IOException {
        TreeSet<Path> result = new TreeSet<>();
        for (String line : Files.readAllLines(pathsFile, StandardCharsets.UTF_8)) {
            String value = line.strip();
            if (value.isEmpty() || value.startsWith("#")) {
                continue;
            }
            Path relative = Path.of(value);
            if (relative.isAbsolute() || relative.normalize().startsWith("..")) {
                throw new IOException("noncanonical census path: " + value);
            }
            if (isM3OwnedSource(relative)) {
                result.add(relative.normalize());
            }
        }
        return List.copyOf(result);
    }

    private static List<Path> normalizeFiles(Path root, List<Path> files) throws IOException {
        TreeSet<Path> result = new TreeSet<>();
        Set<Path> seen = new HashSet<>();
        for (Path file : files) {
            Path candidate = file.isAbsolute() ? file.normalize() : root.resolve(file).normalize();
            if (!candidate.startsWith(root)) {
                throw new IOException("source path escapes repository: " + file);
            }
            if (!Files.isRegularFile(candidate) || Files.isSymbolicLink(candidate)) {
                throw new IOException("source path is not a regular file: " + file);
            }
            if (!seen.add(candidate)) {
                throw new IOException("duplicate source path: " + file);
            }
            result.add(candidate);
        }
        return List.copyOf(result);
    }

    private static void parseBatch(Path root, List<Path> files, boolean m3Scheme,
            List<String> findings, List<String> parseErrors) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IOException("JDK compiler unavailable");
        }
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(
                diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
            Iterable<? extends JavaFileObject> units =
                    fileManager.getJavaFileObjectsFromPaths(files);
            List<String> options = List.of("--release", "21", "-proc:none", "-Xlint:none");
            JavacTask task = (JavacTask) compiler.getTask(
                    null, fileManager, diagnostics, options, null, units);
            M3JdkStandardPlugin plugin = new M3JdkStandardPlugin();
            if (m3Scheme) {
                plugin.init(task, "mode=warning", "scheme=m3");
            } else {
                plugin.init(task, "mode=warning");
            }
            task.parse();
            plugin.findings().stream()
                    .map(row -> normalizeFinding(root, row))
                    .forEach(findings::add);
        }
        diagnostics.getDiagnostics().stream()
                .filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
                .map(M3JdkStandardCensus::formatDiagnostic)
                .forEach(parseErrors::add);
    }

    static int compareFindingRows(String left, String right) {
        String[] first = left.split("\t", 5);
        String[] second = right.split("\t", 5);
        if (first.length != 5 || second.length != 5) {
            throw new IllegalArgumentException("invalid finding row");
        }
        int comparison = first[0].compareTo(second[0]);
        if (comparison != 0) {
            return comparison;
        }
        comparison = Long.compare(Long.parseLong(first[1]), Long.parseLong(second[1]));
        if (comparison != 0) {
            return comparison;
        }
        comparison = Long.compare(Long.parseLong(first[2]), Long.parseLong(second[2]));
        if (comparison != 0) {
            return comparison;
        }
        comparison = first[3].compareTo(second[3]);
        return comparison != 0 ? comparison : first[4].compareTo(second[4]);
    }

    private static String normalizeFinding(Path root, String row) {
        String[] fields = row.split("\\t", 5);
        if (fields.length != 5) {
            throw new IllegalArgumentException("invalid plugin finding row: " + row);
        }
        Path source = Path.of(fields[0]).toAbsolutePath().normalize();
        if (!source.startsWith(root)) {
            throw new IllegalArgumentException("finding source escapes repository: " + source);
        }
        fields[0] = portable(root.relativize(source));
        return String.join("\t", fields);
    }

    private static String formatDiagnostic(Diagnostic<? extends JavaFileObject> diagnostic) {
        String source = diagnostic.getSource() == null
                ? "<unknown>" : diagnostic.getSource().getName();
        return source + ":" + diagnostic.getLineNumber() + ":"
                + diagnostic.getColumnNumber() + ": " + diagnostic.getMessage(Locale.ROOT);
    }

    private static String portable(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static Configuration parseArguments(String[] args) {
        Path root = null;
        Path pathsFile = null;
        Path report = null;
        boolean m3Scheme = false;
        for (int index = 0; index < args.length; index++) {
            String argument = args[index];
            switch (argument) {
                case "--root" -> root = Path.of(requireValue(args, ++index, argument));
                case "--paths" -> pathsFile = Path.of(requireValue(args, ++index, argument));
                case "--report" -> report = Path.of(requireValue(args, ++index, argument));
                case "--scheme=m3" -> m3Scheme = true;
                default -> throw new IllegalArgumentException("unknown argument: " + argument);
            }
        }
        if (root == null || report == null) {
            throw new IllegalArgumentException(
                    "usage: M3JdkStandardCensus --root <repo> [--paths <file>] "
                            + "--report <tsv> [--scheme=m3]");
        }
        return new Configuration(root.toAbsolutePath().normalize(), pathsFile,
                report.toAbsolutePath().normalize(), m3Scheme);
    }

    private static String requireValue(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new IllegalArgumentException("missing value for " + option);
        }
        return args[index];
    }

    record Result(int fileCount, int findingCount, List<String> findings) {
    }

    private record Configuration(Path root, Path pathsFile, Path report, boolean m3Scheme) {
    }
}
