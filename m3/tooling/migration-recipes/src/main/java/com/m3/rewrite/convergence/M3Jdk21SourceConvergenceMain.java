// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.convergence;

import com.m3.rewrite.M3Java21ConvergenceRecipe;
import com.m3.rewrite.atom.M3AtomizePureIntReturnRecipe;
import com.m3.rewrite.atom.M3DocumentPureIntAtomRecipe;
import com.m3.rewrite.atom.M3InventoryPureIntAtomCandidates;
import com.m3.rewrite.atom.M3PatternizePureIntAtomRecipe;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

/**
 * Maven-launched, whole-tree Java 21 convergence executor.
 *
 * <p>The OpenJDK configure/make build remains authoritative. This control-plane tool walks the
 * original {@code src/**/*.java} tree, runs only bounded JUnit-proven OpenRewrite FILE recipes,
 * and writes candidate postimages plus receipts under the caller-owned output directory. It never
 * writes the OpenJDK source tree.
 *
 * <p>Each file is independent: inventory -> atomization -> patternization/IOP -> documentation ->
 * composite dry-run fixed point. Results are sorted before receipt hashing, so horizontal execution
 * changes throughput but not semantic output.
 */
public final class M3Jdk21SourceConvergenceMain {
    static final String MANIFEST = "SOURCE_CONVERGENCE.tsv";
    static final String SUMMARY = "SOURCE_CONVERGENCE.summary.tsv";

    private M3Jdk21SourceConvergenceMain() {}

    public static void main(String[] args) throws Exception {
        if (args.length < 2 || args.length > 3) {
            throw new IllegalArgumentException(
                    "usage: <jdk-root> <output-dir> [threads]");
        }
        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        Path output = Path.of(args[1]).toAbsolutePath().normalize();
        int threads =
                args.length == 3
                        ? positive(Integer.parseInt(args[2]), "threads")
                        : Math.max(1, Runtime.getRuntime().availableProcessors());
        RunSummary summary = convergeTree(root, output, threads);
        System.out.println(
                "M3JDK21 source convergence: files="
                        + summary.files()
                        + " changed="
                        + summary.changed()
                        + " holds="
                        + summary.holds()
                        + " root="
                        + summary.root());
    }

    public static RunSummary convergeTree(Path repositoryRoot, Path outputDirectory, int threads)
            throws IOException, InterruptedException {
        Path root = checkedRoot(repositoryRoot);
        Path output = checkedOutput(root, outputDirectory);
        positive(threads, "threads");

        List<Path> files;
        try (var stream = Files.walk(root.resolve("src"))) {
            files =
                    stream.filter(Files::isRegularFile)
                            .filter(path -> path.getFileName().toString().endsWith(".java"))
                            .sorted()
                            .toList();
        }

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<FileReceipt>> futures = new ArrayList<>(files.size());
        try {
            for (Path file : files) {
                String relative = normalized(root.relativize(file));
                futures.add(
                        executor.submit(
                                task(relative, file)));
            }

            List<FileReceipt> receipts = new ArrayList<>(files.size());
            for (Future<FileReceipt> future : futures) {
                try {
                    receipts.add(future.get());
                } catch (ExecutionException failure) {
                    Throwable cause = failure.getCause();
                    throw new IllegalStateException(
                            "source convergence worker failed", cause);
                }
            }
            receipts.sort(Comparator.comparing(FileReceipt::path));
            return write(output, receipts);
        } finally {
            executor.shutdownNow();
        }
    }

    static FileReceipt convergeSource(String path, String source) {
        String checkedPath = relativeJavaPath(path);
        String before = Objects.requireNonNull(source, "source");
        String beforeHash = sha256(before);

        try {
                    Stage inventory = apply(new M3InventoryPureIntAtomCandidates(), checkedPath, before, 1);
                    if (inventory.hold()) {
                        return hold(checkedPath, beforeHash, inventory.message());
                    }
                    if (inventory.changed()) {
                        return hold(checkedPath, beforeHash, "inventory recipe mutated source");
                    }
            
                    Stage atomized =
                            apply(new M3AtomizePureIntReturnRecipe(), checkedPath, before, 1);
                    if (atomized.hold()) return hold(checkedPath, beforeHash, atomized.message());
            
                    Stage patternized =
                            apply(
                                    new M3PatternizePureIntAtomRecipe(),
                                    checkedPath,
                                    atomized.source(),
                                    1);
                    if (patternized.hold()) return hold(checkedPath, beforeHash, patternized.message());
            
                    Stage documented =
                            apply(
                                    new M3DocumentPureIntAtomRecipe(),
                                    checkedPath,
                                    patternized.source(),
                                    1);
                    if (documented.hold()) return hold(checkedPath, beforeHash, documented.message());
            
                    Stage fixedPoint =
                            apply(
                                    new M3Java21ConvergenceRecipe(),
                                    checkedPath,
                                    documented.source(),
                                    8);
                    if (fixedPoint.hold()) return hold(checkedPath, beforeHash, fixedPoint.message());
                    if (fixedPoint.changed()) {
                        return hold(
                                checkedPath,
                                beforeHash,
                                "composite convergence changed the staged postimage");
                    }
            
                    String after = documented.source();
                    boolean changed = !before.equals(after);
                    return new FileReceipt(
                            checkedPath,
                            beforeHash,
                            sha256(after),
                            atomized.changed(),
                            patternized.changed(),
                            documented.changed(),
                            true,
                            changed ? "CONVERGED_CHANGED" : "CONVERGED_UNCHANGED",
                            "",
                            changed ? after : "");
            
        } catch (RuntimeException failure) {
            return hold(
                    checkedPath,
                    beforeHash,
                    "runtime hold: " + compact(failure));
        }
    }

    private static Callable<FileReceipt> task(String relative, Path file) {
        return () -> {
            try {
                return convergeSource(relative, Files.readString(file, StandardCharsets.UTF_8));
            } catch (IOException failure) {
                return hold(
                        relative,
                        sha256(Files.readAllBytes(file)),
                        "UTF-8/read hold: " + failure.getClass().getSimpleName());
            } catch (RuntimeException failure) {
                return hold(
                        relative,
                        sha256(Files.readAllBytes(file)),
                        "runtime hold: " + compact(failure));
            }
        };
    }

    private static Stage apply(Recipe recipe, String path, String source, int maxCycles) {
        List<Throwable> errors = new ArrayList<>();
        InMemoryExecutionContext context = new InMemoryExecutionContext(errors::add);
        List<SourceFile> parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(Parser.Input.fromString(Path.of(path), source)),
                                null,
                                context)
                        .toList();
        if (!errors.isEmpty()) {
            return Stage.hold(source, "parse error: " + compact(errors.getFirst()));
        }
        if (parsed.size() != 1 || !(parsed.getFirst() instanceof J.CompilationUnit)) {
            return Stage.hold(source, "not a Java compilation unit");
        }

        var run =
                recipe.run(
                        new InMemoryLargeSourceSet(parsed),
                        context,
                        maxCycles);
        if (!errors.isEmpty()) {
            return Stage.hold(source, "recipe error: " + compact(errors.getFirst()));
        }

        List<Result> results = run.getChangeset().getAllResults();
        if (results.isEmpty()) return Stage.unchanged(source);
        if (results.size() != 1) {
            return Stage.hold(source, "FILE recipe produced " + results.size() + " changes");
        }

        Result result = results.getFirst();
        SourceFile before = result.getBefore();
        SourceFile after = result.getAfter();
        if (before == null || after == null) {
            return Stage.hold(source, "FILE recipe attempted create/delete");
        }
        String beforePath = normalized(before.getSourcePath());
        String afterPath = normalized(after.getSourcePath());
        if (!path.equals(beforePath) || !path.equals(afterPath)) {
            return Stage.hold(source, "FILE recipe escaped target path");
        }
        if (!(after instanceof J.CompilationUnit)) {
            return Stage.hold(source, "FILE recipe downgraded Java LST");
        }
        return Stage.changed(after.printAll());
    }

    private static RunSummary write(Path output, List<FileReceipt> receipts) throws IOException {
        Files.createDirectories(output);
        Path candidates = output.resolve("candidates");
        Files.createDirectories(candidates);

        StringBuilder manifest = new StringBuilder(
                "path\tpreSha256\tpostSha256\tatomizationChanged\tpatternizationChanged"
                        + "\tdocumentationChanged\tfixedPoint\tstatus\tmessage\tcandidate\n");
        int changed = 0;
        int holds = 0;
        for (FileReceipt receipt : receipts) {
            String candidate = "";
            if (receipt.changed()) {
                changed++;
                candidate = "candidates/" + receipt.path();
                Path target = output.resolve(candidate).normalize();
                if (!target.startsWith(candidates)) {
                    throw new IllegalStateException("candidate path escaped output");
                }
                Files.createDirectories(target.getParent());
                Files.writeString(
                        target,
                        receipt.candidateSource(),
                        StandardCharsets.UTF_8);
            }
            if (receipt.hold()) holds++;
            manifest.append(receipt.path()).append('\t')
                    .append(receipt.preSha256()).append('\t')
                    .append(receipt.postSha256()).append('\t')
                    .append(receipt.atomizationChanged()).append('\t')
                    .append(receipt.patternizationChanged()).append('\t')
                    .append(receipt.documentationChanged()).append('\t')
                    .append(receipt.fixedPoint()).append('\t')
                    .append(receipt.status()).append('\t')
                    .append(tsv(receipt.message())).append('\t')
                    .append(candidate).append('\n');
        }

        String root = sha256(manifest.toString());
        Files.writeString(output.resolve(MANIFEST), manifest, StandardCharsets.UTF_8);
        String summary =
                "files\tchanged\tholds\tsemanticRoot\n"
                        + receipts.size()
                        + "\t"
                        + changed
                        + "\t"
                        + holds
                        + "\t"
                        + root
                        + "\n";
        Files.writeString(output.resolve(SUMMARY), summary, StandardCharsets.UTF_8);
        return new RunSummary(receipts.size(), changed, holds, root);
    }

    private static FileReceipt hold(String path, String preSha256, String message) {
        return new FileReceipt(
                relativeJavaPath(path),
                preSha256,
                preSha256,
                false,
                false,
                false,
                false,
                "HOLD",
                compact(message),
                "");
    }

    private static Path checkedRoot(Path value) {
        Path root = Objects.requireNonNull(value, "repositoryRoot").toAbsolutePath().normalize();
        if (!Files.isDirectory(root.resolve("src"))) {
            throw new IllegalArgumentException("OpenJDK src directory missing: " + root);
        }
        return root;
    }

    private static Path checkedOutput(Path root, Path value) {
        Path output = Objects.requireNonNull(value, "outputDirectory").toAbsolutePath().normalize();
        if (output.equals(root) || output.startsWith(root.resolve("src"))) {
            throw new IllegalArgumentException("output may not write the OpenJDK source tree");
        }
        return output;
    }

    private static String relativeJavaPath(String value) {
        String path = Objects.requireNonNull(value, "path").replace('\\', '/');
        if (!path.startsWith("src/")
                || !path.endsWith(".java")
                || path.contains("/../")
                || path.contains("/./")
                || path.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("noncanonical Java source path: " + path);
        }
        return path;
    }

    private static String normalized(Path path) {
        return path.normalize().toString().replace('\\', '/');
    }

    private static int positive(int value, String field) {
        if (value <= 0) throw new IllegalArgumentException(field);
        return value;
    }

    private static String compact(Throwable failure) {
        return compact(
                failure.getClass().getSimpleName()
                        + ": "
                        + Objects.toString(failure.getMessage(), ""));
    }

    private static String compact(String message) {
        return Objects.toString(message, "")
                .replace('\t', ' ')
                .replace('\r', ' ')
                .replace('\n', ' ')
                .strip();
    }

    private static String tsv(String value) {
        return compact(value);
    }

    private static String sha256(String value) {
        return sha256(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256(byte[] value) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    public record FileReceipt(
            String path,
            String preSha256,
            String postSha256,
            boolean atomizationChanged,
            boolean patternizationChanged,
            boolean documentationChanged,
            boolean fixedPoint,
            String status,
            String message,
            String candidateSource) {
        public FileReceipt {
            path = relativeJavaPath(path);
            preSha256 = hash(preSha256, "preSha256");
            postSha256 = hash(postSha256, "postSha256");
            status = token(status, "status");
            message = compact(message);
            candidateSource = Objects.requireNonNull(candidateSource, "candidateSource");
            boolean changedStatus = "CONVERGED_CHANGED".equals(status);
            boolean holdStatus = "HOLD".equals(status);
            if (changedStatus != !candidateSource.isEmpty()) {
                throw new IllegalArgumentException("candidate/source status mismatch");
            }
            if (changedStatus && !fixedPoint) {
                throw new IllegalArgumentException("changed candidate requires fixed point");
            }
            if (holdStatus && fixedPoint) {
                throw new IllegalArgumentException("hold cannot be fixed point");
            }
            if (!changedStatus
                    && !holdStatus
                    && !"CONVERGED_UNCHANGED".equals(status)) {
                throw new IllegalArgumentException("unknown convergence status");
            }
        }

        public boolean changed() {
            return "CONVERGED_CHANGED".equals(status);
        }

        public boolean hold() {
            return "HOLD".equals(status);
        }

        private static String token(String value, String field) {
            String checked = Objects.requireNonNull(value, field).strip();
            if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
                throw new IllegalArgumentException(field);
            }
            return checked;
        }

        private static String hash(String value, String field) {
            String checked = token(value, field);
            if (!checked.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException(field);
            }
            return checked;
        }
    }

    public record RunSummary(int files, int changed, int holds, String root) {
        public RunSummary {
            if (files < 0 || changed < 0 || holds < 0 || changed + holds > files) {
                throw new IllegalArgumentException("summary counts");
            }
            root = FileReceipt.hash(root, "root");
        }
    }

    private record Stage(String source, boolean changed, boolean hold, String message) {
        static Stage unchanged(String source) {
            return new Stage(source, false, false, "");
        }

        static Stage changed(String source) {
            return new Stage(source, true, false, "");
        }

        static Stage hold(String source, String message) {
            return new Stage(source, false, true, compact(message));
        }
    }
}
