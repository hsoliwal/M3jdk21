/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package org.m3jdk.tooling.rewrite;

import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import com.sun.source.tree.ClassTree;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.tree.J;

/**
 * Opt-in OpenRewrite Java recipe runner over the selected M3 compiler's actual attributed units.
 * This entry point never invokes a JavaParser or reparses/re-attributes its input compilation units.
 * Recipes may use their normal synthetic-template parsers. The runner emits a review patch; it
 * does not replace source files. Runtime and additional recipe jars retain their original packages.
 */
public final class M3RewriteTool {
    private M3RewriteTool() { }

    private static final String USAGE = """
            Usage: m3-rewrite --source-root DIR --source FILE [--source FILE ...]
                   --recipe QUALIFIED_NAME [--recipe QUALIFIED_NAME ...]
                   [--recipe-config YAML ...] [--recipe-jar JAR ...]
                   [--patch NEW_FILE] [--classes-dir DIR] [-- JAVAC_OPTIONS ...]

            Runs OpenRewrite 8.17.1 Java recipes after one successful, lossless by-file
            M3 javac compilation. Recipe YAML supplies recipe options. Sources are UTF-8;
            relative --source paths resolve against --source-root. Other paths resolve
            against the working directory. The default patch destination is stdout.
            --patch creates a new file and refuses overwrite. Source files are not applied.
            Class output uses a temporary directory unless --classes-dir is supplied.
            Pass normal classpath, release, processor and other compiler options after --.
            Every requested source must produce an attributed LST. Unsupported units
            (including module-info/package-info without an analyzed type) are refused.
            """;

    public static void main(String[] arguments) {
        System.exit(run(arguments, System.out, System.err));
    }

    /** CLI entry with injectable output streams. Returns 0 on success, 1 on compilation failure. */
    public static int run(String[] arguments, PrintStream patchOutput, PrintStream diagnosticsOutput) {
        Objects.requireNonNull(arguments, "arguments");
        Objects.requireNonNull(patchOutput, "patchOutput");
        Objects.requireNonNull(diagnosticsOutput, "diagnosticsOutput");
        if (arguments.length == 1 && arguments[0].equals("--help")) {
            patchOutput.print(USAGE);
            return 0;
        }
        try {
            return execute(Request.parse(arguments), patchOutput, diagnosticsOutput);
        } catch (Exception | LinkageError failure) {
            diagnosticsOutput.println("m3-rewrite: " + failure.getClass().getSimpleName()
                    + ": " + Objects.toString(failure.getMessage(), "operation failed"));
            return 2;
        }
    }

    private static int execute(Request request, PrintStream patchOutput, PrintStream messages)
            throws Exception {
        Path temporaryClasses = request.classesDirectory() == null
                ? Files.createTempDirectory("m3-rewrite-classes-") : null;
        Path classes = temporaryClasses == null ? request.classesDirectory() : temporaryClasses;
        try {
            Files.createDirectories(classes);
            ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();
            ExecutionContext execution = new InMemoryExecutionContext(failures::add);
            Compilation compilation = compile(request, classes, execution, messages);
            if (!compilation.successful()) return 1;
            if (!failures.isEmpty()) throw new IOException("input LST conversion failed", failures.peek());

            // Do not even load caller recipe classes/configuration until input compilation succeeds.
            List<URL> urls = new ArrayList<>();
            for (Path jar : request.recipeJars()) urls.add(jar.toUri().toURL());
            ClassLoader previous = Thread.currentThread().getContextClassLoader();
            try (URLClassLoader recipes = new URLClassLoader(urls.toArray(URL[]::new),
                    M3RewriteTool.class.getClassLoader())) {
                Thread.currentThread().setContextClassLoader(recipes);
                try {
                    Recipe recipe = activate(request, recipes, execution);
                    List<SourceFile> inputs = new ArrayList<>(compilation.units());
                    List<Result> results = new ArrayList<>(recipe.run(
                            new InMemoryLargeSourceSet(inputs), execution).getChangeset().getAllResults());
                    if (!failures.isEmpty()) {
                        throw new IOException("recipe execution failed; no patch emitted", failures.peek());
                    }
                    results.sort(Comparator.comparing(M3RewriteTool::resultPath));
                    StringBuilder patch = new StringBuilder();
                    for (Result result : results) {
                        SourceFile before = reviewPath(result.getBefore(), request.root());
                        SourceFile after = reviewPath(result.getAfter(), request.root());
                        String diff = new Result(before, after, result.getRecipes()).diff();
                        patch.append(diff);
                        if (!diff.isEmpty() && !diff.endsWith("\n")) patch.append('\n');
                    }
                    if (request.patchFile() == null) {
                        patchOutput.print(patch);
                        if (patchOutput.checkError()) throw new IOException("patch output stream failed");
                    } else {
                        Path parent = request.patchFile().getParent();
                        if (parent != null) Files.createDirectories(parent);
                        Files.writeString(request.patchFile(), patch, StandardCharsets.UTF_8,
                                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                    }
                    messages.println("M3Rewrite recipes=" + request.recipeNames().size()
                            + " inputLsts=" + inputs.size() + " changedFiles=" + results.size());
                    return 0;
                } finally {
                    Thread.currentThread().setContextClassLoader(previous);
                }
            }
        } finally {
            if (temporaryClasses != null) {
                try (var paths = Files.walk(temporaryClasses)) {
                    for (Path path : paths.sorted(Comparator.reverseOrder()).toList())
                        Files.deleteIfExists(path);
                }
            }
        }
    }

    private record Compilation(boolean successful, List<J.CompilationUnit> units) { }

    private static Compilation compile(Request request, Path classes, ExecutionContext execution,
                                       PrintStream messages) throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("M3 compiler is unavailable");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        Map<URI, Path> requested = new LinkedHashMap<>();
        for (Path path : request.sources()) requested.put(path.toUri(), request.root().relativize(path));
        Map<URI, J.CompilationUnit> retained = new LinkedHashMap<>();
        List<String> options = new ArrayList<>(List.of("-encoding", "UTF-8", "-d", classes.toString(),
                "-XDcompilePolicy=byfile"));
        options.addAll(request.compilerOptions());
        try (var manager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) compiler.getTask(null, manager, diagnostics, options, null,
                    manager.getJavaFileObjectsFromPaths(request.sources()));
            boolean success;
            RuntimeException invocationFailure = null;
            M3AST.Summary summary;
            try (M3AST shared = M3AST.open(task, true)) {
                var projection = M3RewriteFrontend.projection(execution);
                shared.addConsumer(new M3AST.Consumer() {
                    @Override public void analyzed(M3AST.Unit unit) throws Exception {
                        URI uri = unit.tree().getSourceFile().toUri();
                        // Implicit or processor-generated sources participate in javac normally,
                        // but only explicit --source inputs belong to this recipe source set.
                        if (requested.containsKey(uri) && !retained.containsKey(uri)
                                && unit.tree().getTypeDecls().stream().anyMatch(ClassTree.class::isInstance)
                                && diagnostics.getDiagnostics().stream()
                                    .noneMatch(d -> d.getKind() == Diagnostic.Kind.ERROR)) {
                            retained.put(uri, unit.project(projection).withSourcePath(requested.get(uri)));
                        }
                    }
                });
                try {
                    success = Boolean.TRUE.equals(task.call());
                } catch (RuntimeException failure) {
                    if (diagnostics.getDiagnostics().stream()
                            .noneMatch(d -> d.getKind() == Diagnostic.Kind.ERROR)) invocationFailure = failure;
                    success = false;
                }
                summary = shared.summary();
            }
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics())
                messages.println(diagnostic);
            messages.println("M3RewriteCompiler parsed=" + summary.parsedUnits()
                    + " analyzed=" + summary.analyzedTypes() + " inputLsts=" + retained.size()
                    + " conversions=" + summary.projectionComputations()
                    + " sourceChecks=" + summary.sourceReads() + " errors=" + summary.compilerErrors()
                    + " completed=" + summary.completed());
            if (invocationFailure != null) throw invocationFailure;
            if (!success || summary.compilerErrors() != 0 || summary.consumerFailed()) {
                messages.println("Compilation failed; recipes were not loaded or run; no patch emitted.");
                return new Compilation(false, List.of());
            }
            if (!summary.completed()) throw new IllegalStateException("compiler did not complete");
            if (!retained.keySet().equals(requested.keySet())) {
                Set<URI> missing = new LinkedHashSet<>(requested.keySet());
                missing.removeAll(retained.keySet());
                throw new IllegalStateException("No supported attributed LST for requested input(s): "
                        + missing + "; no recipe run or parser fallback");
            }
            return new Compilation(true, List.copyOf(retained.values()));
        }
    }

    private static Recipe activate(Request request, ClassLoader recipes, ExecutionContext execution)
            throws IOException {
        Environment.Builder environment = Environment.builder().scanRuntimeClasspath("org.openrewrite");
        for (Path jar : request.recipeJars()) environment.scanJar(jar, request.recipeJars(), recipes);
        for (Path yaml : request.recipeConfigs()) {
            try (var input = Files.newInputStream(yaml)) {
                environment.load(new YamlResourceLoader(input, yaml.toUri(), new Properties(), recipes));
            }
        }
        Recipe selected = environment.build().activateRecipes(request.recipeNames());
        var validated = selected.validateAll(execution, new ArrayList<>());
        List<String> invalid = new ArrayList<>();
        for (var validation : validated) {
            for (var failure : validation.failures())
                invalid.add(failure.getProperty() + ": " + failure.getMessage());
        }
        if (!invalid.isEmpty()) throw new IllegalArgumentException("Invalid recipe configuration: " + invalid);
        return selected;
    }

    private static String resultPath(Result result) {
        SourceFile file = result.getBefore() == null ? result.getAfter() : result.getBefore();
        if (file == null) throw new IllegalStateException("recipe returned an empty result");
        return file.getSourcePath().toString();
    }

    private static SourceFile reviewPath(SourceFile file, Path root) {
        if (file == null) return null;
        Path path = file.getSourcePath().normalize();
        if (path.isAbsolute()) {
            if (!path.startsWith(root)) throw new IllegalArgumentException("recipe path escapes source root: " + path);
            path = root.relativize(path);
        }
        if (path.toString().isEmpty() || path.startsWith(".."))
            throw new IllegalArgumentException("invalid recipe patch path: " + path);
        return file.withSourcePath(path);
    }

    private record Request(Path root, List<Path> sources, List<String> recipeNames,
                           List<Path> recipeConfigs, List<Path> recipeJars, Path patchFile,
                           Path classesDirectory, List<String> compilerOptions) {
        static Request parse(String[] arguments) throws IOException {
            Path root = null, patch = null, classes = null;
            List<String> sourceNames = new ArrayList<>(), recipeNames = new ArrayList<>();
            List<Path> configs = new ArrayList<>(), jars = new ArrayList<>();
            List<String> options = new ArrayList<>();
            for (int index = 0; index < arguments.length; index++) {
                String option = arguments[index];
                if (option.equals("--")) {
                    for (++index; index < arguments.length; index++) options.add(arguments[index]);
                    break;
                }
                if (++index >= arguments.length) throw new IllegalArgumentException("missing value for " + option);
                String value = arguments[index];
                switch (option) {
                    case "--source-root" -> {
                        if (root != null) throw new IllegalArgumentException("duplicate --source-root");
                        root = Path.of(value).toRealPath();
                    }
                    case "--source" -> sourceNames.add(value);
                    case "--recipe" -> {
                        if (!value.matches("[A-Za-z_$][\\w$]*(\\.[A-Za-z_$][\\w$]*)+"))
                            throw new IllegalArgumentException("recipe name must be qualified: " + value);
                        if (recipeNames.contains(value)) throw new IllegalArgumentException("duplicate recipe " + value);
                        recipeNames.add(value);
                    }
                    case "--recipe-config" -> configs.add(regularFile(value));
                    case "--recipe-jar" -> jars.add(regularFile(value));
                    case "--patch" -> {
                        if (patch != null) throw new IllegalArgumentException("duplicate --patch");
                        patch = Path.of(value).toAbsolutePath().normalize();
                        if (Files.exists(patch, LinkOption.NOFOLLOW_LINKS))
                            throw new IllegalArgumentException("patch destination already exists: " + patch);
                    }
                    case "--classes-dir" -> {
                        if (classes != null) throw new IllegalArgumentException("duplicate --classes-dir");
                        classes = Path.of(value).toAbsolutePath().normalize();
                    }
                    default -> throw new IllegalArgumentException("unknown option " + option + "\n" + USAGE);
                }
            }
            if (root == null || !Files.isDirectory(root) || sourceNames.isEmpty() || recipeNames.isEmpty())
                throw new IllegalArgumentException("source root, source files and recipe names are required\n" + USAGE);
            List<Path> sources = new ArrayList<>();
            for (String name : sourceNames) {
                Path path = root.resolve(name).toRealPath();
                if (!Files.isRegularFile(path) || !path.startsWith(root) || !path.toString().endsWith(".java"))
                    throw new IllegalArgumentException("source must be a Java file within the source root: " + name);
                if (sources.contains(path)) throw new IllegalArgumentException("duplicate source " + name);
                sources.add(path);
            }
            for (String option : options) {
                if (option.equals("-d") || option.equals("-encoding") || option.equals("-proc:only")
                        || option.startsWith("-XDcompilePolicy") || option.startsWith("-XDallowStringFolding"))
                    throw new IllegalArgumentException("compiler option conflicts with retained-input contract: " + option);
            }
            if (patch != null) directoryAncestor(patch.getParent(), "patch destination");
            if (classes != null) {
                directoryAncestor(classes, "class output");
                if (patch != null && classes.startsWith(patch))
                    throw new IllegalArgumentException("patch destination conflicts with class output: " + patch);
            }
            return new Request(root, List.copyOf(sources), List.copyOf(recipeNames),
                    List.copyOf(configs), List.copyOf(jars), patch, classes, List.copyOf(options));
        }

        private static Path regularFile(String value) throws IOException {
            Path path = Path.of(value).toRealPath();
            if (!Files.isRegularFile(path)) throw new IllegalArgumentException("not a regular file: " + value);
            return path;
        }

        private static void directoryAncestor(Path path, String label) {
            while (path != null && !Files.exists(path, LinkOption.NOFOLLOW_LINKS)) path = path.getParent();
            if (path != null && !Files.isDirectory(path))
                throw new IllegalArgumentException(label + " has a non-directory ancestor: " + path);
        }
    }
}
