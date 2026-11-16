/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary Preserve legacy wildcard imports and enforce functional type safety
 * @modules java.compiler jdk.compiler
 * @run main FunctionExtensionCompilation
 */

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class FunctionExtensionCompilation {
    private static final JavaCompiler COMPILER = ToolProvider.getSystemJavaCompiler();
    private static int checks;
    private static Path componentPatch;

    public static void main(String[] args) throws Exception {
        if (args.length > 1 || args.length == 1
                && !args[0].startsWith("--component-patch=")) {
            throw new IllegalArgumentException("Expected no arguments or --component-patch=PATH");
        }
        if (args.length == 1) {
            componentPatch = Path.of(args[0].substring("--component-patch=".length()))
                    .toRealPath();
            if (!Files.isDirectory(componentPatch)) {
                throw new IllegalArgumentException("Component patch must be a directory");
            }
        }
        if (COMPILER == null) {
            throw new AssertionError("A JDK compiler is required");
        }
        Path root = Files.createTempDirectory(Path.of("."), "function-compilation-");
        wildcardImports(root);
        checkedTypes(root);
        genericSam(root);
        System.out.println("FunctionExtensionCompilation "
                + (componentPatch == null ? "INSTALLED" : "COMPONENT")
                + ": " + checks + " checks passed");
    }

    private static void wildcardImports(Path root) throws IOException {
        Path vendor = write(root.resolve("vendor/thirdparty/TriFunction.java"), """
                package thirdparty;
                public interface TriFunction<A, B, C, R> {
                    R apply(A first, B second, C third);
                }
                """);
        Path vendorClasses = root.resolve("vendor-classes");
        success(compile(vendorClasses, List.of(), vendor));
        Path legacy = write(root.resolve("legacy/Legacy.java"), """
                import java.util.function.*;
                import thirdparty.*;
                class Legacy {
                    TriFunction<String, String, String, String> function =
                            (first, second, third) -> first + second + third;
                }
                """);
        success(compile(root.resolve("legacy-classes"),
                List.of("-classpath", vendorClasses.toString()), legacy));
        Path extension = write(root.resolve("extension/Extension.java"), """
                import java.util.function.ext.TriFunction;
                class Extension {
                    TriFunction<String, String, String, String> function =
                            (first, second, third) -> first + second + third;
                }
                """);
        success(compile(root.resolve("extension-classes"), List.of(), extension));
        Path forbiddenRoot = root.resolve("forbidden");
        Path forbidden = write(forbiddenRoot.resolve(
                "java/util/function/TriFunction.java"), """
                package java.util.function;
                public interface TriFunction<A, B, C, R> {
                    R apply(A first, B second, C third);
                }
                """);
        Path forbiddenClasses = root.resolve("forbidden-classes");
        success(compile(forbiddenClasses, List.of("--patch-module",
                "java.base=" + forbiddenRoot), forbidden));
        failure(compile(root.resolve("ambiguous-classes"),
                List.of("--patch-module", "java.base=" + forbiddenClasses,
                        "-classpath", vendorClasses.toString()), legacy),
                "compiler.err.ref.ambiguous");
        // Independent subsequent compilation must remain unaffected by the patch.
        success(compile(root.resolve("legacy-again"),
                List.of("-classpath", vendorClasses.toString()), legacy));
    }

    private static void checkedTypes(Path root) throws IOException {
        Path invalid = write(root.resolve("checked/Invalid.java"), """
                interface Invalid<T, R, E extends Throwable>
                        extends java.util.function.Function<T, R> {
                    R apply(T value) throws E;
                }
                """);
        failure(compile(root.resolve("invalid-checked"), List.of(), invalid),
                "compiler.err.override.meth.doesnt.throw");
        Path valid = write(root.resolve("checked/Valid.java"), """
                class Valid {
                    static final class Signal extends Throwable {
                        private static final long serialVersionUID = 1L;
                    }
                    static String produce(String value) throws Signal {
                        throw new Signal();
                    }
                    void apply() throws Signal {
                        java.util.function.checked.Function<String, String, Signal>
                                function = Valid::produce;
                        function.apply("value");
                    }
                }
                """);
        success(compile(root.resolve("valid-checked"), List.of(), valid));
        Path wrongException = write(root.resolve("checked/WrongException.java"), """
                class WrongException {
                    void apply() {
                        java.util.function.checked.Function<String, String,
                                java.io.FileNotFoundException> function = value -> {
                                    throw new java.io.IOException();
                                };
                    }
                }
                """);
        failure(compile(root.resolve("wrong-exception"), List.of(), wrongException),
                "compiler.err.unreported.exception.need.to.catch.or.throw");
        Path unhandled = write(root.resolve("checked/Unhandled.java"), """
                class Unhandled {
                    void apply(java.util.function.checked.Function<String, String,
                            java.io.IOException> function) {
                        function.apply("value");
                    }
                }
                """);
        failure(compile(root.resolve("unhandled-exception"), List.of(), unhandled),
                "compiler.err.unreported.exception.need.to.catch.or.throw");
    }

    private static void genericSam(Path root) throws IOException {
        Path invalid = write(root.resolve("generic/Generic.java"), """
                @FunctionalInterface
                interface Generic {
                    <T> T apply(T value);
                }
                class GenericUse {
                    Generic function = value -> value;
                }
                """);
        failure(compile(root.resolve("generic-invalid"), List.of(), invalid),
                "compiler.err.prob.found.req");
        Path valid = write(root.resolve("generic/Typed.java"), """
                @FunctionalInterface
                interface Typed<T> {
                    T apply(T value);
                }
                class TypedUse {
                    Typed<String> function = value -> value;
                }
                """);
        success(compile(root.resolve("generic-valid"), List.of(), valid));
    }

    private static Path write(Path path, String source) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, source, StandardCharsets.UTF_8);
        return path;
    }

    private static Result compile(Path output, List<String> extra, Path source)
            throws IOException {
        Files.createDirectories(output);
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        List<String> options = new ArrayList<>(List.of("-d", output.toString(),
                "-proc:none", "-Xlint:all", "-Werror"));
        options.addAll(compilerOptions(extra));
        StringWriter messages = new StringWriter();
        try (StandardJavaFileManager files = COMPILER.getStandardFileManager(
                diagnostics, null, StandardCharsets.UTF_8)) {
            boolean passed = COMPILER.getTask(messages, files, diagnostics, options,
                    null, files.getJavaFileObjectsFromPaths(List.of(source))).call();
            List<String> errors = diagnostics.getDiagnostics().stream()
                    .filter(item -> item.getKind() == Diagnostic.Kind.ERROR)
                    .map(Diagnostic::getCode).toList();
            return new Result(passed, errors, diagnostics.getDiagnostics().toString()
                    + messages);
        }
    }

    private static List<String> compilerOptions(List<String> extra) {
        if (componentPatch == null) {
            return extra;
        }
        List<String> result = new ArrayList<>();
        String patch = componentPatch.toString();
        for (int index = 0; index < extra.size(); index++) {
            if ("--patch-module".equals(extra.get(index))) {
                String value = extra.get(++index);
                if (!value.startsWith("java.base=")) {
                    throw new IllegalArgumentException("Unexpected patch module: " + value);
                }
                patch += java.io.File.pathSeparator + value.substring("java.base=".length());
            } else {
                result.add(extra.get(index));
            }
        }
        result.addAll(List.of("--patch-module", "java.base=" + patch,
                "--add-exports", "java.base/java.util.function.ext=ALL-UNNAMED",
                "--add-exports", "java.base/java.util.function.checked=ALL-UNNAMED"));
        return result;
    }

    private static void success(Result result) {
        checks++;
        if (!result.passed() || !result.errors().isEmpty()) {
            throw new AssertionError("Expected compilation success: " + result.details());
        }
    }

    private static void failure(Result result, String code) {
        checks++;
        if (result.passed() || !result.errors().equals(List.of(code))) {
            throw new AssertionError("Expected only " + code + ": " + result.details());
        }
    }

    private record Result(boolean passed, List<String> errors, String details) { }
}
