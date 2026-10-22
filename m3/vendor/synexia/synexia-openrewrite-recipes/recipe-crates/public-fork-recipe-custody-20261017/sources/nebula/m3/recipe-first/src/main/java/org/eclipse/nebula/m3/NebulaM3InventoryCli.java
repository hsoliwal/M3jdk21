// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

/** Whole-checkout, deterministic CLI over the same file-local facts used by the OpenRewrite recipe. */
public final class NebulaM3InventoryCli {
    private NebulaM3InventoryCli() {
        throw new AssertionError("No instances");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "usage: NebulaM3InventoryCli <nebula-root> <output-directory>");
        }
        run(Path.of(args[0]), Path.of(args[1]));
    }

    static Summary run(Path repositoryRoot, Path outputDirectory) throws IOException {
        Path root = requireDirectory(repositoryRoot, "repositoryRoot");
        Path output = outputDirectory.toAbsolutePath().normalize();
        Files.createDirectories(output);

        List<Path> javaFiles;
        try (Stream<Path> stream = Files.walk(root)) {
            javaFiles =
                    stream.filter(Files::isRegularFile)
                            .filter(path -> path.getFileName().toString().endsWith(".java"))
                            .filter(path -> target(root, path))
                            .sorted(Comparator.comparing(path -> relative(root, path)))
                            .toList();
        }

        List<NebulaM3InventoryRecipe.SourceFacts> rows = new ArrayList<>();
        List<NebulaM3InventoryRecipe.MethodFacts> methodRows = new ArrayList<>();
        List<Failure> failures = new ArrayList<>();
        JavaParser parser = JavaParser.fromJavaVersion().build();

        for (Path file : javaFiles) {
            String relative = relative(root, file);
            List<Throwable> errors = new ArrayList<>();
            InMemoryExecutionContext context = new InMemoryExecutionContext(errors::add);
            String source;
            try {
                source = Files.readString(file, StandardCharsets.UTF_8);
            } catch (IOException failure) {
                failures.add(new Failure(relative, failure.getClass().getSimpleName()));
                continue;
            }
            try (Stream<SourceFile> parsed =
                    parser.parseInputs(
                            List.of(Parser.Input.fromString(Path.of(relative), source)),
                            null,
                            context)) {
                List<SourceFile> sources = parsed.toList();
                if (sources.size() == 1 && sources.getFirst() instanceof J.CompilationUnit unit) {
                    rows.add(NebulaM3InventoryRecipe.analyze(unit));
                    methodRows.addAll(NebulaM3InventoryRecipe.analyzeMethods(unit));
                } else {
                    failures.add(new Failure(relative, "not-one-java-compilation-unit"));
                }
            } catch (RuntimeException failure) {
                failures.add(new Failure(relative, failure.getClass().getSimpleName()));
            }
            for (Throwable error : errors) {
                failures.add(new Failure(relative, error.getClass().getSimpleName()));
            }
        }

        writeInventory(output.resolve("nebula-m3-java-inventory.tsv"), rows);
        writeMethods(output.resolve("nebula-m3-java-methods.tsv"), methodRows);
        writeFailures(output.resolve("nebula-m3-parse-failures.tsv"), failures);
        Summary summary = new Summary(javaFiles.size(), rows.size(), failures.size());
        Files.writeString(
                output.resolve("nebula-m3-summary.tsv"),
                "key\tvalue\n"
                        + "javaFiles\t" + summary.javaFiles() + "\n"
                        + "parsedCompilationUnits\t" + summary.parsedCompilationUnits() + "\n"
                        + "methodRows\t" + methodRows.size() + "\n"
                        + "failureRows\t" + summary.failureRows() + "\n",
                StandardCharsets.UTF_8);
        return summary;
    }

    private static void writeInventory(
            Path file, List<NebulaM3InventoryRecipe.SourceFacts> rows) throws IOException {
        StringBuilder out =
                new StringBuilder(
                        "sourcePath\tmodule\ttestSource\tlines\ttypes\tvisibleTypes\tmethods"
                                + "\tvisibleMethods\tnativeMethods\tloops\tindexOfCalls"
                                + "\tcontainsCalls\tsortCalls\tbinarySearchCalls\ttodoMarkers"
                                + "\tmethodDensityX1000\tpublicSurfaceDensityX1000"
                                + "\tfastSearchSignal\trecommendedNextPass\n");
        for (NebulaM3InventoryRecipe.SourceFacts row : rows) {
            cells(
                    out,
                    row.sourcePath(),
                    row.module(),
                    Boolean.toString(row.testSource()),
                    Integer.toString(row.lineCount()),
                    Integer.toString(row.typeCount()),
                    Integer.toString(row.publicProtectedTypeCount()),
                    Integer.toString(row.methodCount()),
                    Integer.toString(row.publicProtectedMethodCount()),
                    Integer.toString(row.nativeMethodCount()),
                    Integer.toString(row.loopCount()),
                    Integer.toString(row.indexOfCalls()),
                    Integer.toString(row.containsCalls()),
                    Integer.toString(row.sortCalls()),
                    Integer.toString(row.binarySearchCalls()),
                    Integer.toString(row.todoMarkers()),
                    Integer.toString(row.methodDensityX1000()),
                    Integer.toString(row.publicSurfaceDensityX1000()),
                    row.fastSearchSignal(),
                    row.recommendedNextPass());
        }
        Files.writeString(file, out, StandardCharsets.UTF_8);
    }

    private static void writeMethods(
            Path file, List<NebulaM3InventoryRecipe.MethodFacts> rows) throws IOException {
        StringBuilder out =
                new StringBuilder(
                        "sourcePath\townerType\tmethodKey\tmethodName\tcontractSurface"
                                + "\tnativeMethod\tdirectStatements\tloops\tbranches"
                                + "\texceptionBoundaries\tsynchronized\tlambdas\tinvocations"
                                + "\tindexOfCalls\tcontainsCalls\tsortCalls\tbinarySearchCalls"
                                + "\tstructuralPatterns\tfastSearchSignal\trecommendedNextPass\n");
        for (NebulaM3InventoryRecipe.MethodFacts row : rows) {
            cells(
                    out,
                    row.sourcePath(),
                    row.ownerType(),
                    row.methodKey(),
                    row.methodName(),
                    row.contractSurface(),
                    Boolean.toString(row.nativeMethod()),
                    Integer.toString(row.directStatementCount()),
                    Integer.toString(row.loopCount()),
                    Integer.toString(row.branchCount()),
                    Integer.toString(row.exceptionBoundaryCount()),
                    Integer.toString(row.synchronizedCount()),
                    Integer.toString(row.lambdaCount()),
                    Integer.toString(row.invocationCount()),
                    Integer.toString(row.indexOfCalls()),
                    Integer.toString(row.containsCalls()),
                    Integer.toString(row.sortCalls()),
                    Integer.toString(row.binarySearchCalls()),
                    row.structuralPatterns(),
                    row.fastSearchSignal(),
                    row.recommendedNextPass());
        }
        Files.writeString(file, out, StandardCharsets.UTF_8);
    }

    private static void writeFailures(Path file, List<Failure> failures) throws IOException {
        StringBuilder out = new StringBuilder("sourcePath\treason\n");
        for (Failure failure : failures) {
            cells(out, failure.sourcePath(), failure.reason());
        }
        Files.writeString(file, out, StandardCharsets.UTF_8);
    }

    private static void cells(StringBuilder out, String... cells) {
        for (int index = 0; index < cells.length; index++) {
            if (index > 0) out.append('\t');
            out.append(cell(cells[index]));
        }
        out.append('\n');
    }

    private static String cell(String value) {
        return Objects.toString(value, "")
                .replace('\t', ' ')
                .replace('\n', ' ')
                .replace('\r', ' ');
    }

    private static boolean target(Path root, Path path) {
        String relative = relative(root, path);
        return !relative.startsWith(".git/")
                && !relative.startsWith("m3/")
                && !relative.startsWith("target/")
                && !relative.contains("/target/");
    }

    private static Path requireDirectory(Path path, String field) throws IOException {
        Path checked = Objects.requireNonNull(path, field).toAbsolutePath().normalize();
        if (!Files.isDirectory(checked)) {
            throw new IOException(field + " is not a directory: " + checked);
        }
        return checked;
    }

    private static String relative(Path root, Path path) {
        return root.relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    record Summary(int javaFiles, int parsedCompilationUnits, int failureRows) {}

    private record Failure(String sourcePath, String reason) {}
}
