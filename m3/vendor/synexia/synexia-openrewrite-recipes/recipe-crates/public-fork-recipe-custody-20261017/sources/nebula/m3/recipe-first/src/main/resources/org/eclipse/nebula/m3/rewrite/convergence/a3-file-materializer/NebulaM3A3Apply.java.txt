// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import org.eclipse.nebula.m3.rewrite.convergence.NebulaM3FileConvergenceRecipeDag;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.tree.ParseError;

/**
 * Serial explicit-file A3 candidate materializer for Nebula.
 *
 * <p>Each selected Java file is processed independently through the existing FILE-local convergence
 * DAG. Original repository source is never an output. Candidates and receipts are confined below
 * {@code m3/recipe-first/target/} and every candidate must reach a zero-change second pass.</p>
 */
public final class NebulaM3A3Apply {
    private static final String OUTPUT_ROOT = "m3/recipe-first/target";

    public record Receipt(
            String path,
            String recipe,
            String beforeSha256,
            String afterSha256,
            boolean changed,
            boolean fixedPoint) {
        public Receipt {
            path = text(path, "path");
            recipe = text(recipe, "recipe");
            beforeSha256 = sha(beforeSha256, "beforeSha256");
            afterSha256 = sha(afterSha256, "afterSha256");
            if (!fixedPoint) {
                throw new IllegalArgumentException("fixedPoint");
            }
        }
    }

    private NebulaM3A3Apply() {
        throw new AssertionError("No instances");
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            throw new IllegalArgumentException(
                    "usage: NebulaM3A3Apply <nebula-root> <output> <source.java>...");
        }
        Path root = Path.of(args[0]);
        Path output = Path.of(args[1]);
        List<String> sources =
                java.util.Arrays.stream(args)
                        .skip(2)
                        .toList();
        run(root, output, sources);
    }

    static List<Receipt> run(
            Path repositoryRoot,
            Path output,
            List<String> sources)
            throws IOException {
        Path root = requireDirectory(repositoryRoot, "repositoryRoot");
        Path out = output(root, output);
        List<String> ordered =
                Objects.requireNonNull(sources, "sources").stream()
                        .map(value -> text(value, "source"))
                        .distinct()
                        .sorted()
                        .toList();
        if (ordered.isEmpty()) {
            throw new IllegalArgumentException("sources");
        }

        List<Receipt> receipts = new ArrayList<>(ordered.size());
        for (String source : ordered) {
            receipts.add(applyOne(root, out, source));
        }
        receipts.sort(Comparator.comparing(Receipt::path));
        writeReceipt(out.resolve("receipt.tsv"), receipts);
        return List.copyOf(receipts);
    }

    private static Receipt applyOne(
            Path root,
            Path out,
            String requested)
            throws IOException {
        Path file = source(root, requested);
        String relative = relative(root, file);
        String before = Files.readString(file, StandardCharsets.UTF_8);
        SourceFile parsed = parse(relative, before);
        Recipe recipe = NebulaM3FileConvergenceRecipeDag.fixedPointRecipe();
        String after = apply(recipe, parsed);
        SourceFile converged = parse(relative, after);
        requireFixedPoint(recipe, converged, relative);

        Path candidate = out.resolve("candidate").resolve(relative).normalize();
        if (!candidate.startsWith(out)) {
            throw new IllegalArgumentException("candidate path escaped output");
        }
        Files.createDirectories(candidate.getParent());
        Files.writeString(candidate, after, StandardCharsets.UTF_8);

        return new Receipt(
                relative,
                recipe.getName(),
                sha256(before),
                sha256(after),
                !before.equals(after),
                true);
    }

    private static Path source(Path root, String requested) throws IOException {
        String normalized = text(requested, "source").replace('\\', '/');
        Path relative = Path.of(normalized).normalize();
        if (relative.isAbsolute()
                || normalized.startsWith("/")
                || normalized.startsWith("../")
                || normalized.endsWith("/..")
                || normalized.contains("/../")) {
            throw new IllegalArgumentException("source");
        }

        Path unresolved = root.resolve(relative).normalize();
        if (!unresolved.startsWith(root)
                || Files.isSymbolicLink(unresolved)
                || !Files.isRegularFile(unresolved, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("source is not a regular repository file: " + requested);
        }
        Path real = unresolved.toRealPath();
        if (!real.startsWith(root) || Files.isSymbolicLink(real)) {
            throw new IOException("source escapes repository: " + requested);
        }

        String repositoryPath = relative(root, real);
        if (!repositoryPath.endsWith(".java")
                || repositoryPath.startsWith(".git/")
                || repositoryPath.startsWith("m3/")
                || repositoryPath.startsWith("target/")
                || repositoryPath.contains("/target/")) {
            throw new IllegalArgumentException(
                    "source is not an admitted Nebula Java product/test file: " + repositoryPath);
        }
        return real;
    }

    private static Path output(Path root, Path requested) throws IOException {
        Path candidate =
                Objects.requireNonNull(requested, "output").isAbsolute()
                        ? requested.toAbsolutePath().normalize()
                        : root.resolve(requested).normalize();
        Path allowed = root.resolve(OUTPUT_ROOT).normalize();
        if (!candidate.startsWith(allowed)) {
            throw new IllegalArgumentException(
                    "A3 output must remain below " + OUTPUT_ROOT);
        }
        Files.createDirectories(candidate);
        if (Files.isSymbolicLink(candidate)
                || !candidate.toRealPath().startsWith(allowed.toRealPath())) {
            throw new IOException("A3 output escaped target root");
        }
        return candidate;
    }

    private static SourceFile parse(String path, String source) {
        List<Throwable> errors = new ArrayList<>();
        InMemoryExecutionContext context = new InMemoryExecutionContext(errors::add);
        List<SourceFile> parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(
                                        Parser.Input.fromString(
                                                Path.of(path),
                                                source)),
                                null,
                                context)
                        .toList();
        if (!errors.isEmpty()
                || parsed.size() != 1
                || parsed.getFirst() instanceof ParseError
                || !parsed.getFirst().printAll().equals(source)) {
            throw new IllegalArgumentException(
                    "Nebula A3 could not losslessly parse Java 21 source: " + path);
        }
        return parsed.getFirst();
    }

    private static String apply(Recipe recipe, SourceFile source) {
        InMemoryExecutionContext context =
                new InMemoryExecutionContext(
                        failure -> {
                            throw new IllegalStateException(failure);
                        });
        var run =
                recipe.run(
                        new InMemoryLargeSourceSet(List.of(source)),
                        context,
                        8);
        var results = run.getChangeset().getAllResults();
        if (results.isEmpty()) {
            return source.printAll();
        }
        if (results.size() != 1
                || results.getFirst().getAfter() == null
                || !results.getFirst()
                        .getAfter()
                        .getSourcePath()
                        .equals(source.getSourcePath())) {
            throw new IllegalStateException(
                    "Nebula A3 FILE recipe produced an invalid change shape: "
                            + source.getSourcePath());
        }
        return results.getFirst().getAfter().printAll();
    }

    private static void requireFixedPoint(
            Recipe recipe,
            SourceFile source,
            String path) {
        InMemoryExecutionContext context =
                new InMemoryExecutionContext(
                        failure -> {
                            throw new IllegalStateException(failure);
                        });
        var second =
                recipe.run(
                        new InMemoryLargeSourceSet(List.of(source)),
                        context,
                        8);
        if (!second.getChangeset().getAllResults().isEmpty()) {
            throw new IllegalStateException(
                    "Nebula A3 candidate did not reach fixed point: " + path);
        }
    }

    private static void writeReceipt(
            Path file,
            List<Receipt> receipts)
            throws IOException {
        StringBuilder out =
                new StringBuilder(
                        "path\trecipe\tbeforeSha256\tafterSha256\tchanged\tfixedPoint\n");
        for (Receipt receipt : receipts) {
            out.append(cell(receipt.path()))
                    .append('\t')
                    .append(cell(receipt.recipe()))
                    .append('\t')
                    .append(receipt.beforeSha256())
                    .append('\t')
                    .append(receipt.afterSha256())
                    .append('\t')
                    .append(receipt.changed())
                    .append('\t')
                    .append(receipt.fixedPoint())
                    .append('\n');
        }
        Files.writeString(file, out, StandardCharsets.UTF_8);
    }

    private static Path requireDirectory(Path path, String field) throws IOException {
        Path checked = Objects.requireNonNull(path, field).toAbsolutePath().normalize();
        if (Files.isSymbolicLink(checked)
                || !Files.isDirectory(checked, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException(field + " is not a directory: " + checked);
        }
        return checked.toRealPath();
    }

    private static String relative(Path root, Path path) {
        return root.relativize(path.toAbsolutePath().normalize())
                .toString()
                .replace('\\', '/');
    }

    private static String cell(String value) {
        return Objects.toString(value, "")
                .replace('\t', ' ')
                .replace('\n', ' ')
                .replace('\r', ' ');
    }

    private static String text(String value, String field) {
        String checked = Objects.toString(value, "").strip();
        if (checked.isEmpty()
                || checked.indexOf('\0') >= 0
                || checked.indexOf('\n') >= 0
                || checked.indexOf('\r') >= 0
                || checked.indexOf('\t') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String sha(String value, String field) {
        String checked = text(value, field);
        if (!checked.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
