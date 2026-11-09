// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser;

import com.sun.source.util.JavacTask;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * Exactly one original javac parse task per selected source execution.
 *
 * Only the selected sources are provided to javac. Everything outside the
 * requested scope must be supplied as a compiled, caller-pinned dependency.
 * No second AST parser, implicit source compilation, or annotation processing.
 * Attach M3AST observers on the caller-owned task at its original parse event;
 * this convenience executor retains only detached operation diagnostics.
 */
public final class M3ScopedCompilation {
    public enum Scope { FILE, PACKAGE, MODULE, PROJECT, REPOSITORY }
    public enum Phase { SYNTAX, ANALYZE, GENERATE }

    public record Unit(URI uri, String source) {
        public Unit {
            Objects.requireNonNull(uri, "uri");
            Objects.requireNonNull(source, "source");
            if (!"string".equals(uri.getScheme()) && !"file".equals(uri.getScheme())) {
                throw new IllegalArgumentException("unsupported source URI scheme");
            }
            if (!uri.getPath().endsWith(".java")) {
                throw new IllegalArgumentException("Java compilation unit required: " + uri);
            }
        }
    }

    public record Request(Scope scope, Phase phase, List<Unit> units,
                          List<Path> classpath, List<Path> modulepath, Path output,
                          List<String> additionalOptions) {
        public Request {
            Objects.requireNonNull(scope, "scope");
            Objects.requireNonNull(phase, "phase");
            units = List.copyOf(Objects.requireNonNull(units, "units"));
            classpath = List.copyOf(Objects.requireNonNull(classpath, "classpath"));
            modulepath = List.copyOf(Objects.requireNonNull(modulepath, "modulepath"));
            additionalOptions = List.copyOf(Objects.requireNonNull(additionalOptions, "additionalOptions"));
            if (units.isEmpty() || units.size() > 100_000) {
                throw new IllegalArgumentException("explicit bounded source set required");
            }
            if (scope == Scope.FILE && units.size() != 1) {
                throw new IllegalArgumentException("FILE scope requires exactly one source");
            }
            if (phase == Phase.GENERATE && output == null) {
                throw new IllegalArgumentException("GENERATE requires an explicit output directory");
            }
            for (Path path : classpath) requireExistingArtifact(path);
            for (Path path : modulepath) requireExistingArtifact(path);
            for (String option : additionalOptions) {
                if (!List.of("-g:none", "-g:lines", "-g:vars", "-g:source",
                        "-parameters", "-nowarn", "-Xlint:all", "-Xlint:none", "-Werror").contains(option)) {
                    throw new IllegalArgumentException("unsafe/unreviewed javac option: " + option);
                }
            }
            var identities = new java.util.HashSet<URI>();
            for (Unit unit : units) {
                if (!identities.add(unit.uri())) throw new IllegalArgumentException("duplicate source URI");
            }
        }
        private static void requireExistingArtifact(Path path) {
            Objects.requireNonNull(path, "dependency artifact");
            if (!Files.exists(path) || (!Files.isDirectory(path)
                    && !path.toString().endsWith(".jar") && !path.toString().endsWith(".jmod"))) {
                throw new IllegalArgumentException("missing/unsupported binary dependency: " + path);
            }
        }
    }

    public record Result(Scope scope, Phase phase, long suppliedUnits, long parsedUnits,
                         long generatedFiles, boolean success, List<String> diagnostics) {
        public Result {
            diagnostics = List.copyOf(diagnostics);
        }
    }

    private M3ScopedCompilation() { }

    /**
     * The same javac task parses, optionally attributes, then generates the
     * exact source set. Sourcepath is explicitly empty, processors disabled,
     * and dependency source roots are never traversed by this operation.
     */
    public static Result execute(Request request) throws IOException {
        Objects.requireNonNull(request, "request");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("running JDK compiler unavailable");
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        long parsed = 0;
        long generated = 0;
        boolean ok;
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(collector, null, null)) {
            List<String> opts = new ArrayList<>();
            opts.add("-proc:none");
            opts.add("-implicit:none");
            Path emptySource = Files.createTempDirectory("m3-empty-sourcepath-");
            try {
                opts.add("-sourcepath");
                opts.add(emptySource.toString());
                opts.add("-classpath");
                opts.add(request.classpath().isEmpty()
                        ? emptySource.toString() : join(request.classpath()));
                if (!request.modulepath().isEmpty()) {
                    opts.add("--module-path");
                    opts.add(join(request.modulepath()));
                }
                if (request.phase() == Phase.GENERATE) {
                    Files.createDirectories(request.output());
                    opts.add("-d");
                    opts.add(request.output().toString());
                }
                opts.addAll(request.additionalOptions());
                List<JavaFileObject> selected = new ArrayList<>(request.units().size());
                for (Unit unit : request.units()) selected.add(new TextUnit(unit));
                JavacTask task = (JavacTask) compiler.getTask(null, manager, collector,
                        opts, null, selected);
                for (var ignored : task.parse()) parsed++;
                if (request.phase() != Phase.SYNTAX) {
                    task.analyze();
                    if (request.phase() == Phase.GENERATE) {
                        for (var ignored : task.generate()) generated++;
                    }
                }
                ok = collector.getDiagnostics().stream()
                        .noneMatch(d -> d.getKind() == Diagnostic.Kind.ERROR);
            } finally {
                Files.deleteIfExists(emptySource);
            }
        }
        List<String> diagnostics = collector.getDiagnostics().stream()
                .map(d -> d.getKind() + ":" + d.getCode() + ":" + d.getLineNumber())
                .toList();
        return new Result(request.scope(), request.phase(), request.units().size(),
                parsed, generated, ok, diagnostics);
    }

    private static String join(Collection<Path> files) {
        return String.join(java.io.File.pathSeparator,
                files.stream().map(path -> path.toAbsolutePath().normalize().toString()).toList());
    }

    private static final class TextUnit extends SimpleJavaFileObject {
        private final String text;
        TextUnit(Unit original) {
            super(original.uri(), JavaFileObject.Kind.SOURCE);
            text = original.source();
        }
        @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return text;
        }
    }
}
