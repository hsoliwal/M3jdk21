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
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.RecipeRun;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.Validated;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;
import static org.junit.jupiter.api.Assertions.*;

class M3RewriteToolTest {
    @TempDir Path directory;

    private static final String RENAME = """
            type: specs.openrewrite.org/v1beta/recipe
            name: example.RenameApi
            displayName: Rename typed API
            description: Rename both declaration and typed callers in the supplied source set.
            recipeList:
              - org.openrewrite.java.ChangeMethodName:
                  methodPattern: 'demo.Api oldName(java.lang.String)'
                  newMethodName: renamed
                  matchOverrides: false
                  ignoreDefinition: false
            """;

    @Test void actualCliRunsConfiguredTypedRecipeAcrossFilesAfterOneCompilerPass() throws Exception {
        Path root = Files.createDirectories(directory.resolve("project"));
        String apiText = "package demo;\r\n/* α keep */ public class Api {\r\n"
                + "  public static String oldName(String value) { return value.trim(); }\r\n"
                + "  String joined = \"a\" /* keep */ + \"b\";\r\n}\r\nclass Auxiliary {}\r\n";
        String useText = "package demo; class Use { String f() { return Api.oldName(\" x \" ); } }\n";
        Path api = write(root.resolve("demo/Api.java"), apiText);
        Path use = write(root.resolve("demo/Use.java"), useText);
        Path yaml = write(directory.resolve("rewrite.yml"), RENAME);
        Path patch = directory.resolve("review.patch");
        Path classes = directory.resolve("compiled");
        List<String> arguments = List.of("--source-root", root.toString(), "--source", "demo/Api.java",
                "--source", "demo/Use.java", "--recipe", "example.RenameApi", "--recipe-config",
                yaml.toString(), "--patch", patch.toString(), "--classes-dir", classes.toString(),
                "--", "--release", "21", "-proc:none");
        Outcome result = cli(arguments);
        assertEquals(0, result.exit(), result.error());
        String diff = Files.readString(patch);
        assertTrue(diff.contains("demo/Api.java"), diff);
        assertTrue(diff.contains("demo/Use.java"), diff);
        assertTrue(diff.contains("String renamed(String value)"), diff);
        assertTrue(diff.contains("Api.renamed("), diff);
        assertEquals(apiText, Files.readString(api));
        assertEquals(useText, Files.readString(use));
        assertTrue(Files.isRegularFile(classes.resolve("demo/Api.class")));
        assertTrue(Files.isRegularFile(classes.resolve("demo/Use.class")));
        assertTrue(result.error().contains("parsed=2 analyzed=3 inputLsts=2 conversions=2 sourceChecks=2 errors=0 completed=true"), result.error());
        assertTrue(result.error().contains("inputLsts=2 changedFiles=2"), result.error());
        assertEquals("", result.output());
    }

    @Test void arbitraryUserRecipeJarRunsThroughTheRealEnvironmentAndDefaultPatchStream() throws Exception {
        Path root = Files.createDirectories(directory.resolve("sources"));
        Path input = write(root.resolve("A.java"), "class A { String x = \"old\"; }\n");
        Path marker = directory.resolve("recipe-ran");
        Path jar = recipeJar(marker, false);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Outcome result = inProcess(List.of("--source-root", root.toString(), "--source", "A.java",
                "--recipe", "example.ExternalRecipe", "--recipe-jar", jar.toString(), "--", "-proc:none"));
        assertEquals(0, result.exit(), result.error());
        assertTrue(Files.isRegularFile(marker));
        assertTrue(result.output().contains("\"new\""), result.output());
        assertEquals("class A { String x = \"old\"; }\n", Files.readString(input));
        assertSame(loader, Thread.currentThread().getContextClassLoader());
        assertTrue(result.error().contains("parsed=1 analyzed=1 inputLsts=1 conversions=1 sourceChecks=1 errors=0 completed=true"), result.error());
    }

    @Test void compileErrorsDoNotLoadOrRunRecipesAndDoNotCreatePatch() throws Exception {
        Path root = Files.createDirectories(directory.resolve("broken"));
        write(root.resolve("A.java"), "class A { Missing value; }\n");
        Path marker = directory.resolve("recipe-must-not-run");
        Path jar = recipeJar(marker, false);
        Path invalidYaml = write(directory.resolve("invalid.yml"), "this: [is: broken YAML");
        Path patch = directory.resolve("must-not-exist.patch");
        Outcome result = cli(List.of("--source-root", root.toString(), "--source", "A.java",
                "--recipe", "example.ExternalRecipe", "--recipe-jar", jar.toString(),
                "--recipe-config", invalidYaml.toString(), "--patch", patch.toString(), "--", "-proc:none"));
        assertEquals(1, result.exit(), result.error());
        assertTrue(result.error().contains("Compilation failed; recipes were not loaded or run"), result.error());
        assertFalse(Files.exists(marker));
        assertFalse(Files.exists(patch));
        assertEquals("", result.output());
    }

    @Test void recipeFailuresNeverEmitAPartialPatch() throws Exception {
        Path root = Files.createDirectories(directory.resolve("throwing"));
        String text = "class A { String x = \"old\"; }\n";
        Path source = write(root.resolve("A.java"), text);
        Path jar = recipeJar(directory.resolve("attempted"), true);
        Path patch = directory.resolve("failed.patch");
        Outcome result = inProcess(List.of("--source-root", root.toString(), "--source", "A.java",
                "--recipe", "example.ExternalRecipe", "--recipe-jar", jar.toString(),
                "--patch", patch.toString(), "--", "-proc:none"));
        assertEquals(2, result.exit(), result.error());
        assertFalse(Files.exists(patch));
        assertEquals("", result.output());
        assertEquals(text, Files.readString(source));
    }

    @Test void coverageAndOutputRefusalsDoNotFallBackToParsingOrOverwrite() throws Exception {
        Path root = Files.createDirectories(directory.resolve("coverage"));
        write(root.resolve("package-info.java"), "/** Package documentation. */ package sample;\n");
        Path yaml = write(directory.resolve("rewrite.yml"), RENAME);
        Outcome unsupported = inProcess(List.of("--source-root", root.toString(), "--source", "package-info.java",
                "--recipe", "example.RenameApi", "--recipe-config", yaml.toString(), "--", "-proc:none"));
        assertEquals(2, unsupported.exit(), unsupported.error());
        assertTrue(unsupported.error().contains("No supported attributed LST"), unsupported.error());
        assertTrue(unsupported.error().contains("inputLsts=0 conversions=0 sourceChecks=0"), unsupported.error());
        assertEquals("", unsupported.output());
        Path existing = write(directory.resolve("existing.patch"), "preserve");
        Outcome overwrite = cli(List.of("--source-root", root.toString(), "--source", "package-info.java",
                "--recipe", "example.RenameApi", "--recipe-config", yaml.toString(), "--patch", existing.toString()));
        assertEquals(2, overwrite.exit());
        assertEquals("preserve", Files.readString(existing));
        assertFalse(overwrite.error().contains("M3RewriteCompiler"));
        Outcome blockedParent = cli(List.of("--source-root", root.toString(), "--source", "package-info.java",
                "--recipe", "example.RenameApi", "--recipe-config", yaml.toString(), "--patch",
                existing.resolve("child.patch").toString()));
        assertEquals(2, blockedParent.exit());
        assertTrue(blockedParent.error().contains("non-directory ancestor"), blockedParent.error());
        assertFalse(blockedParent.error().contains("M3RewriteCompiler"));
        Outcome policy = inProcess(List.of("--source-root", root.toString(), "--source", "package-info.java",
                "--recipe", "example.RenameApi", "--recipe-config", yaml.toString(), "--", "-XDcompilePolicy=bytodo"));
        assertEquals(2, policy.exit());
        assertTrue(policy.error().contains("conflicts with retained-input contract"));
    }

    @Test void sourceAndRecipePathTraversalAreRefusedBeforeAnyPatch() throws Exception {
        Path root = Files.createDirectories(directory.resolve("safe"));
        String text = "class A {}\n";
        Path input = write(root.resolve("A.java"), text);
        Path outside = write(directory.resolve("Outside.java"), "class Outside {}\n");
        Outcome sourceEscape = cli(List.of("--source-root", root.toString(), "--source", "../Outside.java",
                "--recipe", "org.openrewrite.java.ChangeMethodName"));
        assertEquals(2, sourceEscape.exit(), sourceEscape.error());
        assertTrue(sourceEscape.error().contains("within the source root"), sourceEscape.error());
        assertFalse(sourceEscape.error().contains("M3RewriteCompiler"));
        assertEquals("class Outside {}\n", Files.readString(outside));

        Path marker = directory.resolve("path-recipe-ran");
        Path jar = recipeJar(marker, """
                return new org.openrewrite.java.JavaIsoVisitor<org.openrewrite.ExecutionContext>() {
                  @Override public org.openrewrite.java.tree.J.CompilationUnit visitCompilationUnit(
                      org.openrewrite.java.tree.J.CompilationUnit unit, org.openrewrite.ExecutionContext ctx) {
                    return super.visitCompilationUnit(unit, ctx)
                        .withSourcePath(java.nio.file.Path.of("../escaped.java"));
                  }
                };
                """);
        Path patch = directory.resolve("path-escape.patch");
        Outcome recipeEscape = cli(List.of("--source-root", root.toString(), "--source", "A.java",
                "--recipe", "example.ExternalRecipe", "--recipe-jar", jar.toString(),
                "--patch", patch.toString(), "--", "-proc:none"));
        assertEquals(2, recipeEscape.exit(), recipeEscape.error());
        assertTrue(Files.isRegularFile(marker));
        assertTrue(recipeEscape.error().contains("invalid recipe patch path"), recipeEscape.error());
        assertFalse(Files.exists(patch));
        assertEquals("", recipeEscape.output());
        assertEquals(text, Files.readString(input));
    }

    @Test void typedApiUsesOriginalCompilerTreesForSemanticChangesAndFixedPoint() throws Exception {
        List<Throwable> errors = new ArrayList<>();
        ExecutionContext execution = new InMemoryExecutionContext(errors::add);
        Captured captured = capture(List.of(
                new CountingSource("demo/Api.java", "package demo;\n"
                        + "/* retained */ public class Api {\n"
                        + "  public static String oldName(String value) { return value.trim(); }\n"
                        + "}\nclass Auxiliary {}\n"),
                new CountingSource("demo/Use.java", "package demo; class Use {\n"
                        + "  String f() { return Api.oldName(\" x \"); }\n}\n")), execution);
        M3AST.Summary beforeExecution = captured.session().summary();
        List<Integer> reads = captured.readCounts();
        assertEquals(2, beforeExecution.parsedUnits());
        assertEquals(3, beforeExecution.analyzedTypes());
        assertEquals(2, beforeExecution.projectionComputations());
        assertTrue(beforeExecution.completed());
        assertFalse(beforeExecution.consumerFailed());
        Map<Path, J.CompilationUnit> firstVisited = new LinkedHashMap<>();
        Recipe observe = new Recipe() {
            @Override public String getDisplayName() { return "Observe original trees"; }
            @Override public String getDescription() { return "Observe the first supplied Java objects."; }
            @Override public TreeVisitor<?, ExecutionContext> getVisitor() {
                return new JavaIsoVisitor<ExecutionContext>() {
                    @Override public J.CompilationUnit visitCompilationUnit(
                            J.CompilationUnit unit, ExecutionContext context) {
                        firstVisited.putIfAbsent(unit.getSourcePath(), unit);
                        return super.visitCompilationUnit(unit, context);
                    }
                };
            }
        };
        Recipe rename = new ChangeMethodName("demo.Api oldName(java.lang.String)", "renamed", false, false);
        Recipe composite = new Recipe() {
            @Override public String getDisplayName() { return "Observe and rename"; }
            @Override public String getDescription() { return "Run original visitors in order."; }
            @Override public List<Recipe> getRecipeList() { return List.of(observe, rename); }
        };
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        RecipeRun run = M3RewriteTool.execute(composite, captured.units(), execution);
        List<Result> results = run.getChangeset().getAllResults();
        assertEquals(2, results.size());
        for (J.CompilationUnit unit : captured.units()) {
            assertSame(unit, firstVisited.get(unit.getSourcePath()), "original LST reached the visitor");
            Result result = results.stream()
                    .filter(value -> value.getBefore().getSourcePath().equals(unit.getSourcePath()))
                    .findFirst().orElseThrow();
            assertSame(unit, result.getBefore(), "native changeset retains original input ownership");
            assertFalse(result.getRecipes().isEmpty(), "native recipe attribution is retained");
        }
        String changed = results.stream().map(result -> result.getAfter().printAll())
                .reduce("", String::concat);
        assertTrue(changed.contains("String renamed(String value)"), changed);
        assertTrue(changed.contains("Api.renamed("), changed);
        assertTrue(changed.contains("/* retained */"), changed);
        assertFalse(run.getDataTables().isEmpty(), "native run data tables are returned");
        List<SourceFile> transformed = results.stream().map(Result::getAfter).toList();
        RecipeRun fixedPoint = M3RewriteTool.execute(rename, transformed, execution);
        assertTrue(fixedPoint.getChangeset().getAllResults().isEmpty(), "transformed typed trees reach fixed point");
        assertEquals(beforeExecution, captured.session().summary(), "execution performs no work on the original compiler");
        assertEquals(reads, captured.readCounts(), "sealed input sources are not read again");
        assertTrue(errors.isEmpty(), errors.toString());
        assertSame(loader, Thread.currentThread().getContextClassLoader());
    }

    @Test void typedApiSnapshotsMembershipAndAcceptsExistingNonJavaTrees() {
        PlainText first = PlainText.builder().sourcePath(Path.of("first.txt")).text("old one\n").build();
        PlainText second = PlainText.builder().sourcePath(Path.of("second.txt")).text("old two\n").build();
        List<SourceFile> supplied = new ArrayList<>(List.of(first, second));
        List<Throwable> errors = new ArrayList<>();
        Recipe replace = new Recipe() {
            @Override public String getDisplayName() { return "Edit admitted text"; }
            @Override public String getDescription() { return "Visit the admitted membership without parsing."; }
            @Override public TreeVisitor<?, ExecutionContext> getVisitor() {
                return new PlainTextVisitor<ExecutionContext>() {
                    @Override public PlainText visitText(PlainText text, ExecutionContext context) {
                        supplied.clear(); // A caller callback changes its own list after admission.
                        return text.withText(text.getText().replace("old", "new"));
                    }
                };
            }
        };
        RecipeRun run = M3RewriteTool.execute(replace, supplied, new InMemoryExecutionContext(errors::add));
        List<Result> results = run.getChangeset().getAllResults();
        assertEquals(2, results.size(), "both admitted members survive later caller-list changes");
        assertSame(first, results.get(0).getBefore());
        assertSame(second, results.get(1).getBefore());
        assertEquals("new one\n", results.get(0).getAfter().printAll());
        assertEquals("new two\n", results.get(1).getAfter().printAll());
        assertEquals("old one\n", first.printAll());
        assertEquals("old two\n", second.printAll());
        assertTrue(errors.isEmpty(), errors.toString());
    }

    @Test void typedApiRefusesInvalidCompositeConfigurationBeforeScheduling() {
        AtomicInteger visits = new AtomicInteger();
        AtomicInteger admissions = new AtomicInteger();
        ExecutionContext execution = new InMemoryExecutionContext();
        Recipe invalid = new Recipe() {
            @Override public String getDisplayName() { return "Invalid child"; }
            @Override public String getDescription() { return "Require an intentionally missing option."; }
            @Override public Validated<Object> validate(ExecutionContext context) {
                assertSame(execution, context);
                return Validated.invalid("required.option", null, "must be provided");
            }
            @Override public TreeVisitor<?, ExecutionContext> getVisitor() {
                visits.incrementAndGet();
                return TreeVisitor.noop();
            }
        };
        Recipe composite = new Recipe() {
            @Override public String getDisplayName() { return "Invalid composite"; }
            @Override public String getDescription() { return "Preserve full child configuration validation."; }
            @Override public List<Recipe> getRecipeList() { return List.of(invalid); }
            @Override public Collection<Validated<Object>> validateAll(
                    ExecutionContext context, Collection<Validated<Object>> validations) {
                admissions.incrementAndGet();
                return super.validateAll(context, validations);
            }
        };
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> M3RewriteTool.execute(composite, List.of(), execution));
        assertTrue(refused.getMessage().startsWith("Invalid recipe configuration:"));
        assertTrue(refused.getMessage().contains("required.option: must be provided"));
        assertEquals(1, admissions.get(), "one tool-level validation admission");
        assertEquals(0, visits.get(), "invalid children are not scheduled");
    }

    @Test void typedApiPreservesCallerContextAndNativeHandledErrorSemantics() {
        PlainText input = PlainText.builder().sourcePath(Path.of("failure.txt")).text("unchanged\n").build();
        Object value = new Object();
        IllegalStateException deliberate = new IllegalStateException("intentional API recipe failure");
        Recipe failing = failingRecipe(value, deliberate);
        List<Throwable> actualErrors = new ArrayList<>(), nativeErrors = new ArrayList<>();
        ExecutionContext actual = new InMemoryExecutionContext(actualErrors::add);
        ExecutionContext nativeContext = new InMemoryExecutionContext(nativeErrors::add);
        actual.putMessage("fixture.value", value);
        nativeContext.putMessage("fixture.value", value);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        RecipeRun actualRun = M3RewriteTool.execute(failing, List.of(input), actual);
        RecipeRun nativeRun = failing.run(new InMemoryLargeSourceSet(List.of(input)), nativeContext);
        assertFalse(actualErrors.isEmpty(), "the caller receives original scheduler errors");
        assertEquals(nativeErrors.size(), actualErrors.size());
        assertTrue(actualErrors.stream().allMatch(error -> causedBy(error, deliberate)));
        assertTrue(nativeErrors.stream().allMatch(error -> causedBy(error, deliberate)));
        assertEquals(nativeRun.getChangeset().getAllResults().size(), actualRun.getChangeset().getAllResults().size());
        assertSame(value, actual.getMessage("fixture.value"));
        assertSame(input, actual.getMessage("fixture.visited"));
        assertSame(loader, Thread.currentThread().getContextClassLoader());
        assertEquals("unchanged\n", input.printAll());
    }

    @Test void typedApiPropagatesExceptionsEscapingTheCallerErrorHandler() {
        PlainText input = PlainText.builder().sourcePath(Path.of("failure.txt")).text("unchanged\n").build();
        Object value = new Object();
        Recipe failing = failingRecipe(value, new IllegalStateException("visitor failure"));
        IllegalStateException handlerFailure = new IllegalStateException("caller rejects recipe errors");
        ExecutionContext actual = new InMemoryExecutionContext(error -> { throw handlerFailure; });
        ExecutionContext nativeContext = new InMemoryExecutionContext(error -> { throw handlerFailure; });
        actual.putMessage("fixture.value", value);
        nativeContext.putMessage("fixture.value", value);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        RuntimeException nativeFailure = assertThrows(RuntimeException.class,
                () -> failing.run(new InMemoryLargeSourceSet(List.of(input)), nativeContext));
        RuntimeException actualFailure = assertThrows(RuntimeException.class,
                () -> M3RewriteTool.execute(failing, List.of(input), actual));
        assertSame(handlerFailure, nativeFailure);
        assertSame(nativeFailure, actualFailure, "the API adds no exception wrapper");
        assertSame(loader, Thread.currentThread().getContextClassLoader());
    }

    @Test void typedApiRejectsNullArgumentsAndElementsBeforeRecipeWork() {
        AtomicInteger visits = new AtomicInteger();
        Recipe recipe = new Recipe() {
            @Override public String getDisplayName() { return "Count work"; }
            @Override public String getDescription() { return "Do not schedule malformed inputs."; }
            @Override public TreeVisitor<?, ExecutionContext> getVisitor() {
                visits.incrementAndGet();
                return TreeVisitor.noop();
            }
        };
        ExecutionContext execution = new InMemoryExecutionContext();
        List<SourceFile> nullElement = new ArrayList<>();
        nullElement.add(null);
        assertThrows(NullPointerException.class, () -> M3RewriteTool.execute(null, List.of(), execution));
        assertThrows(NullPointerException.class, () -> M3RewriteTool.execute(recipe, null, execution));
        assertThrows(NullPointerException.class, () -> M3RewriteTool.execute(recipe, List.of(), null));
        assertThrows(NullPointerException.class, () -> M3RewriteTool.execute(recipe, nullElement, execution));
        assertEquals(0, visits.get());
        assertEquals(1, nullElement.size(), "admission leaves caller membership unchanged");
        assertNull(nullElement.get(0));
    }

    private Captured capture(List<CountingSource> sources, ExecutionContext execution) throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        Path classes = Files.createDirectories(directory.resolve("api-original-classes"));
        Map<URI, J.CompilationUnit> retained = new LinkedHashMap<>();
        M3AST session;
        try (var manager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                    List.of("-proc:none", "-d", classes.toString(), "-XDcompilePolicy=byfile"), null, sources);
            session = M3AST.open(task, true);
            try (session) {
                var projection = M3RewriteFrontend.projection(execution);
                session.addConsumer(new M3AST.Consumer() {
                    @Override public void analyzed(M3AST.Unit unit) throws Exception {
                        URI uri = unit.tree().getSourceFile().toUri();
                        if (!retained.containsKey(uri)) {
                            retained.put(uri, unit.project(projection)
                                    .withSourcePath(Path.of(uri.getPath().substring(1))));
                        }
                    }
                });
                assertTrue(Boolean.TRUE.equals(task.call()), diagnostics.getDiagnostics().toString());
            }
        }
        assertEquals(sources.size(), retained.size(), "all explicit input units were admitted");
        for (CountingSource source : sources) source.sealed = true;
        return new Captured(List.copyOf(retained.values()), session, List.copyOf(sources));
    }

    private static Recipe failingRecipe(Object value, RuntimeException failure) {
        return new Recipe() {
            @Override public String getDisplayName() { return "Fail in original scheduler"; }
            @Override public String getDescription() { return "Expose caller-controlled context and errors."; }
            @Override public TreeVisitor<?, ExecutionContext> getVisitor() {
                return new PlainTextVisitor<ExecutionContext>() {
                    @Override public PlainText visitText(PlainText text, ExecutionContext context) {
                        assertSame(value, context.getMessage("fixture.value"));
                        context.putMessage("fixture.visited", text);
                        throw failure;
                    }
                };
            }
        };
    }

    private static boolean causedBy(Throwable error, Throwable expected) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current == expected) return true;
        }
        return false;
    }

    private record Captured(List<J.CompilationUnit> units, M3AST session, List<CountingSource> sources) {
        List<Integer> readCounts() { return sources.stream().map(source -> source.reads).toList(); }
    }

    private static final class CountingSource extends SimpleJavaFileObject {
        private final String text;
        private int reads;
        private boolean sealed;
        CountingSource(String path, String text) {
            super(URI.create("string:///" + path), JavaFileObject.Kind.SOURCE);
            this.text = text;
        }
        @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            if (sealed) throw new AssertionError("admitted source was read after compilation");
            reads++;
            return text;
        }
    }

    private Outcome cli(List<String> arguments) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.addAll(java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments()
                .stream().filter(arg -> arg.startsWith("--add-exports=")).toList());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(M3RewriteTool.class.getName());
        command.addAll(arguments);
        Path stdout = directory.resolve("cli-" + System.nanoTime() + ".out");
        Path stderr = directory.resolve("cli-" + System.nanoTime() + ".err");
        Process process = new ProcessBuilder(command).redirectOutput(stdout.toFile())
                .redirectError(stderr.toFile()).start();
        assertTrue(process.waitFor(60, java.util.concurrent.TimeUnit.SECONDS), "CLI timed out");
        return new Outcome(process.exitValue(), Files.readString(stdout), Files.readString(stderr));
    }

    private static Outcome inProcess(List<String> arguments) throws Exception {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream(), stderr = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(stdout, true, StandardCharsets.UTF_8);
             PrintStream err = new PrintStream(stderr, true, StandardCharsets.UTF_8)) {
            int exit = M3RewriteTool.run(arguments.toArray(String[]::new), out, err);
            return new Outcome(exit, stdout.toString(StandardCharsets.UTF_8), stderr.toString(StandardCharsets.UTF_8));
        }
    }

    private Path recipeJar(Path marker, boolean fail) throws Exception {
        String body = fail ? "throw new IllegalStateException(\"intentional recipe failure\");" : """
                return new org.openrewrite.java.JavaIsoVisitor<org.openrewrite.ExecutionContext>() {
                  @Override public org.openrewrite.java.tree.J.Literal visitLiteral(
                      org.openrewrite.java.tree.J.Literal literal, org.openrewrite.ExecutionContext ctx) {
                    var value = super.visitLiteral(literal, ctx);
                    return "old".equals(value.getValue())
                        ? value.withValue("new").withValueSource("\\\"new\\\"") : value;
                  }
                };
                """;
        return recipeJar(marker, body);
    }

    private Path recipeJar(Path marker, String body) throws Exception {
        Path work = Files.createDirectories(directory.resolve("recipe-" + System.nanoTime()));
        String markerLiteral = marker.toString().replace("\\", "\\\\").replace("\"", "\\\"");
        Path source = write(work.resolve("example/ExternalRecipe.java"), """
                package example;
                public final class ExternalRecipe extends org.openrewrite.Recipe {
                  public String getDisplayName() { return "External recipe"; }
                  public String getDescription() { return "Test a caller-supplied real recipe jar."; }
                  public org.openrewrite.TreeVisitor<?, org.openrewrite.ExecutionContext> getVisitor() {
                    try { java.nio.file.Files.writeString(java.nio.file.Path.of("%s"), "ran"); }
                    catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
                    %s
                  }
                }
                """.formatted(markerLiteral, body));
        Path classes = Files.createDirectories(work.resolve("classes"));
        int exit = ToolProvider.getSystemJavaCompiler().run(null, null, null, "-proc:none", "-cp",
                System.getProperty("java.class.path"), "-d", classes.toString(), source.toString());
        assertEquals(0, exit, "real user recipe fixture compilation");
        Path jar = work.resolve("external-recipe.jar");
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar));
             var paths = Files.walk(classes)) {
            for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                output.putNextEntry(new JarEntry(classes.relativize(path).toString().replace('\\', '/')));
                Files.copy(path, output);
                output.closeEntry();
            }
        }
        return jar;
    }

    private static Path write(Path path, String text) throws Exception {
        Files.createDirectories(path.getParent());
        return Files.writeString(path, text);
    }

    private record Outcome(int exit, String output, String error) { }
}
