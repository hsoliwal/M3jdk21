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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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
