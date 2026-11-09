/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 */

/*
 * @test
 * @summary M3AST relationship transport publishes canonical source-sealed facts only after success
 * @modules jdk.compiler/com.sun.source.util
 *          jdk.compiler/com.sun.tools.javac.m3
 * @run main M3ASTRelationshipTransportTest
 */

import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import com.sun.tools.javac.m3.M3ASTPC;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

public class M3ASTRelationshipTransportTest {
    private static int checks;
    private static final String CONTEXT = "0123456789abcdef".repeat(4);
    private static final String SOURCE = """
            package transport;
            interface Role { int run(int n); }
            class Impl implements Role { public int run(int n) { return n; } }
            class Uses { Role role; int invoke(int n) { return role.run(n); } }
            """;

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("m3-relation-wire-");
        Path first = root.resolve("first.tsv");
        check(compile(root.resolve("a"), first, SOURCE, CONTEXT, false), "successful javac task");
        String wire = Files.readString(first, StandardCharsets.UTF_8);
        check(wire.startsWith("M3AST_RELATIONS_V1\nC\t" + CONTEXT + "\n"), "schema and supplied context");
        int trailer = wire.lastIndexOf("END\t");
        check(trailer > 0 && wire.endsWith("\n"), "terminated sealed receipt");
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(wire.substring(0, trailer).getBytes(StandardCharsets.UTF_8)));
        check(wire.substring(trailer).equals("END\t" + digest + "\n"), "whole payload hash");
        String[] lines = wire.substring(0, trailer).split("\n");
        String previous = null;
        int group = 0;
        boolean foundSource = false, foundCall = false;
        for (int n = 2; n < lines.length; n++) {
            String[] fields = lines[n].split("\t", -1);
            int current = switch (fields[0]) { case "S" -> 1; case "Y" -> 2; case "F" -> 3; case "U" -> 4; default -> throw new AssertionError(lines[n]); };
            check(current >= group, "canonical group order");
            if (current == group) check(previous.compareTo(lines[n]) < 0, "sorted unique group");
            group = current; previous = lines[n];
            if (fields[0].equals("S")) {
                check(fields[1].equals(encoded("string:///transport/Uses.java")), "original source URI");
                check(fields[2].equals(utf16(SOURCE)), "exact original UTF16 source seal");
                check(fields[3].equals(Integer.toString(SOURCE.length())), "exact source length");
                foundSource = true;
            }
            if (fields[0].equals("F") && fields[1].equals("INVOKES")
                    && fields[2].equals(encoded("transport.Uses#invoke(I)I"))
                    && fields[3].equals(encoded("transport.Role#run(I)I"))) foundCall = true;
            check(!fields[0].equals("U"), "complete fixture has no unresolved observations");
        }
        check(foundSource && foundCall, "wire contains real attributed call and source seal");
        Path second = root.resolve("second.tsv");
        check(compile(root.resolve("b"), second, SOURCE, CONTEXT, false), "repeat javac task");
        check(Files.readString(second).equals(wire), "deterministic wire independent of output path");
        Path unknown = root.resolve("unknown.tsv");
        check(compile(root.resolve("c"), unknown, SOURCE, null, false), "unsealed semantic context still observes");
        check(Files.readString(unknown).startsWith("M3AST_RELATIONS_V1\nC\tUNKNOWN\n"),
                "missing semantic seal is explicit and cannot imply reuse");
        Path broken = root.resolve("broken.tsv");
        check(!compile(root.resolve("d"), broken, SOURCE.replace("return role.run(n)", "return unknown.run(n)"),
                CONTEXT, false), "erroneous source is rejected");
        check(!Files.exists(broken), "compiler errors must not publish a relationship receipt");
        Path failed = root.resolve("failed.tsv");
        try {
            compile(root.resolve("e"), failed, SOURCE, CONTEXT, true);
            throw new AssertionError("failing consumer must fail compilation");
        } catch (RuntimeException expected) { checks++; }
        check(!Files.exists(failed), "late consumer failure must prevent publication");
        Path sentinel = root.resolve("existing.tsv");
        Files.writeString(sentinel, wire);
        check(!compile(root.resolve("f"), sentinel, "class Broken { invalid }", CONTEXT, false),
                "failed recompilation");
        check(!Files.exists(sentinel), "failed recompilation must invalidate a previous owned export");
        Files.writeString(failed, wire);
        try {
            compile(root.resolve("failed-again"), failed, SOURCE, CONTEXT, true);
            throw new AssertionError("failing consumer must fail recompilation");
        } catch (RuntimeException expected) { checks++; }
        check(!Files.exists(failed), "late failure must not leave a previous owned export");
        Path unrelated = root.resolve("unrelated.tsv");
        Files.writeString(unrelated, "user-owned unrelated content");
        try {
            compile(root.resolve("unrelated"), unrelated, SOURCE, CONTEXT, false);
            throw new AssertionError("unrecognized output must be refused");
        } catch (RuntimeException expected) { checks++; }
        check(Files.readString(unrelated).equals("user-owned unrelated content"),
                "freshness invalidation must preserve unrelated content");
        Path tampered = root.resolve("tampered.tsv");
        String changed = wire.replace("M3AST_RELATIONS_V1", "M3AST_RELATIONS_V1 ");
        Files.writeString(tampered, changed);
        try {
            compile(root.resolve("tampered"), tampered, SOURCE, CONTEXT, false);
            throw new AssertionError("tampered prior export must be refused intact");
        } catch (RuntimeException expected) { checks++; }
        check(Files.readString(tampered).equals(changed), "unsealed prior bytes must survive refusal");
        Path partial = root.resolve("partial.tsv");
        Files.writeString(partial, wire);
        partialAndAliasChecks(root, partial, wire);
        check(!Files.exists(partial), "parse/analyze-only must invalidate old export without publishing");
        failedEarlierTransport(root, wire);
        failedRelationshipTransport(root);
        prospectiveOutputAlias(root);
        sourceModuleScope(root, wire);
        check(!compile(root.resolve("g"), root.resolve("no-output.tsv"),
                "class Uses { Missing value; }", CONTEXT, false), "unresolved type diagnostics remain errors");
        System.out.println("M3_AST_RELATIONSHIP_TRANSPORT checks=" + checks);
    }

    private static void sourceModuleScope(Path root, String priorWire) throws Exception {
        Path sources = root.resolve("module-sources");
        var inputs = new ArrayList<Path>();
        for (String module : List.of("alpha", "beta")) {
            Path directory = Files.createDirectories(sources.resolve(module).resolve("p"));
            Path descriptor = directory.getParent().resolve("module-info.java");
            Files.writeString(descriptor, "module " + module + " { exports p; }");
            Path role = directory.resolve("Role.java");
            Files.writeString(role, "package p; public interface Role { int run(int n); }");
            inputs.add(descriptor); inputs.add(role);
            Path implementation = directory.resolve(module.equals("alpha") ? "Context.java" : "Concrete.java");
            Files.writeString(implementation, module.equals("alpha")
                    ? "package p; public class Context { Role role; int execute(int n) { return role.run(n); } }"
                    : "package p; public class Concrete implements Role { public int run(int n) { return n; } }");
            inputs.add(implementation);
        }
        Path output = root.resolve("multiple-source-modules.tsv");
        var compiler = ToolProvider.getSystemJavaCompiler();
        for (boolean export : List.of(false, true)) {
            if (export) Files.writeString(output, priorWire);
            try (var files = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
                Path classes = Files.createDirectories(root.resolve(export ? "export-modules" : "legacy-modules"));
                var task = (JavacTask) compiler.getTask(null, files, null,
                        List.of("-proc:none", "--module-source-path", sources.toString(), "-d", classes.toString()),
                        null, files.getJavaFileObjectsFromPaths(inputs));
                new M3ASTPC().init(task, export ? new String[] {"relationOut=" + output} : new String[0]);
                try (var session = M3AST.open(task)) {
                    if (!export) check(task.call(), "legacy compiler plugin must retain multi-module compilation");
                    else {
                        try {
                            task.call();
                            throw new AssertionError("binary identities from distinct source modules must not be merged");
                        } catch (RuntimeException expected) {
                            Throwable cause = expected;
                            while (cause.getCause() != null) cause = cause.getCause();
                            check(cause instanceof java.io.IOException
                                            && cause.getMessage().contains("one source module"),
                                    "multi-module export must fail with an explicit identity-scope refusal");
                        }
                    }
                }
            }
        }
        check(!Files.exists(output), "unsupported module aggregation must leave no relationship export");
    }

    private static void failedRelationshipTransport(Path root) throws Exception {
        Path blocked = root.resolve("late-relation-parent");
        Path relation = blocked.resolve("relations.tsv");
        Path facts = root.resolve("facts-after-relation-failure.tsv");
        var compiler = ToolProvider.getSystemJavaCompiler();
        try (var files = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            var unit = new SimpleJavaFileObject(URI.create("string:///RelationFailure.java"), javax.tools.JavaFileObject.Kind.SOURCE) {
                @Override public CharSequence getCharContent(boolean ignored) { return "class RelationFailure { int value; }"; }
            };
            var task = (JavacTask) compiler.getTask(null, files, null,
                    List.of("-proc:none", "-d", root.toString()), null, List.of(unit));
            new M3ASTPC().init(task, "relationOut=" + relation,
                    "facts64=" + encoded(facts.toString()), "factsid=" + java.util.UUID.randomUUID());
            try (var session = M3AST.open(task)) {
                session.addConsumer(new M3AST.Consumer() {
                    @Override public void finished(M3AST.Summary summary) throws Exception {
                        Files.writeString(blocked, "late destination failure");
                    }
                });
                try {
                    task.call();
                    throw new AssertionError("relationship publication failure must fail compilation");
                } catch (RuntimeException expected) { checks++; }
            }
        }
        check(!Files.exists(facts), "relationship failure must preserve legacy facts-last success semantics");
    }

    private static void failedEarlierTransport(Path root, String priorWire) throws Exception {
        Path relation = root.resolve("after-facts-failure.tsv");
        Files.writeString(relation, priorWire);
        Path blocked = root.resolve("facts-parent-is-a-file");
        var compiler = ToolProvider.getSystemJavaCompiler();
        var errors = new StringWriter();
        try (var files = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            var unit = new SimpleJavaFileObject(URI.create("string:///TransportFailure.java"), javax.tools.JavaFileObject.Kind.SOURCE) {
                @Override public CharSequence getCharContent(boolean ignored) { return "class TransportFailure { int value; }"; }
            };
            var task = (JavacTask) compiler.getTask(errors, files, null,
                    List.of("-proc:none", "-d", root.toString()), null, List.of(unit));
            new M3ASTPC().init(task, "relationOut=" + relation,
                    "facts64=" + encoded(blocked.resolve("facts.tsv").toString()),
                    "factsid=" + java.util.UUID.randomUUID());
            try (var session = M3AST.open(task)) {
                session.addConsumer(new M3AST.Consumer() {
                    @Override public void finished(M3AST.Summary summary) throws Exception {
                        Files.writeString(blocked, "ordinary file");
                    }
                });
                try {
                    task.call();
                    throw new AssertionError("build-facts publication failure must fail compilation");
                } catch (RuntimeException expected) { checks++; }
            }
        }
        check(!Files.exists(relation), "earlier owned transport failure must prevent relationship publication");
    }

    private static void prospectiveOutputAlias(Path root) throws Exception {
        Path real = Files.createDirectory(root.resolve("real-output-parent"));
        Path link = root.resolve("linked-output-parent");
        try { Files.createSymbolicLink(link, real); }
        catch (UnsupportedOperationException | java.nio.file.FileSystemException unavailable) {
            System.out.println("M3_AST_RELATIONSHIP_SYMLINK_CHECK_NOT_AVAILABLE " + unavailable.getClass().getSimpleName());
            return;
        }
        Path relation = real.resolve("shared.tsv");
        Path trace = link.resolve("shared.tsv");
        var compiler = ToolProvider.getSystemJavaCompiler();
        try (var files = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            var unit = new SimpleJavaFileObject(URI.create("string:///Alias.java"), javax.tools.JavaFileObject.Kind.SOURCE) {
                @Override public CharSequence getCharContent(boolean ignored) { return "class Alias {}"; }
            };
            var task = (JavacTask) compiler.getTask(null, files, null, List.of("-proc:none"), null, List.of(unit));
            try {
                new M3ASTPC().init(task, "relationOut=" + relation, "trace=" + trace);
                try (var session = M3AST.open(task)) { task.call(); }
                throw new AssertionError("prospective aliased outputs must be refused before either write");
            } catch (RuntimeException expected) { checks++; }
        }
        check(!Files.exists(relation), "prospective output alias must not create either output");
    }

    private static void partialAndAliasChecks(Path root, Path output, String wire) throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        try (var files = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            var unit = new SimpleJavaFileObject(URI.create("string:///Partial.java"), javax.tools.JavaFileObject.Kind.SOURCE) {
                @Override public CharSequence getCharContent(boolean ignored) { return "class Partial { int value; }"; }
            };
            var task = (JavacTask) compiler.getTask(null, files, null, List.of("-proc:none"), null, List.of(unit));
            new M3ASTPC().init(task, "relationOut=" + output);
            try (var session = M3AST.open(task)) { task.parse(); task.analyze(); }
        }
        Path source = root.resolve("InputAlias.java");
        // A known input remains an input even if its current bytes resemble an owned wire.
        Files.writeString(source, wire);
        try (var files = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            var task = (JavacTask) compiler.getTask(null, files, null, List.of("-proc:none"), null,
                    files.getJavaFileObjects(source));
            try {
                new M3ASTPC().init(task, "relationOut=" + source);
                throw new AssertionError("source/output alias must be refused before invalidation");
            } catch (RuntimeException expected) { checks++; }
        }
        check(Files.readString(source).equals(wire), "input alias bytes must remain intact");
        Path both = root.resolve("BothOutputs.tsv");
        Files.writeString(both, wire);
        try (var files = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            var task = (JavacTask) compiler.getTask(null, files, null, List.of("-proc:none"), null,
                    files.getJavaFileObjects(source));
            try {
                new M3ASTPC().init(task, "relationOut=" + both, "trace=" + both);
                throw new AssertionError("output alias must be refused before invalidation");
            } catch (IllegalArgumentException expected) { checks++; }
        }
        check(Files.readString(both).equals(wire), "other output bytes must remain intact");
    }

    private static boolean compile(Path classes, Path output, String source, String context,
                                   boolean failingConsumer) throws Exception {
        Files.createDirectories(classes);
        var compiler = ToolProvider.getSystemJavaCompiler();
        var errors = new StringWriter();
        try (var files = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            var unit = new SimpleJavaFileObject(URI.create("string:///transport/Uses.java"), javax.tools.JavaFileObject.Kind.SOURCE) {
                @Override public CharSequence getCharContent(boolean ignored) { return source; }
            };
            var task = (JavacTask) compiler.getTask(errors, files, null,
                    List.of("-proc:none", "-d", classes.toString()), null, List.of(unit));
            var options = new ArrayList<String>();
            options.add("relationOut=" + output);
            if (context != null) options.add("relationContext=" + context);
            new M3ASTPC().init(task, options.toArray(String[]::new));
            try (var session = M3AST.open(task)) {
                if (failingConsumer) session.addConsumer(new M3AST.Consumer() {
                    @Override public void finished(M3AST.Summary summary) { throw new IllegalStateException("test late failure"); }
                });
                return task.call();
            }
        }
    }

    private static String encoded(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String utf16(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (int i = 0; i < value.length(); i++) {
            digest.update((byte) (value.charAt(i) >>> 8));
            digest.update((byte) value.charAt(i));
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
