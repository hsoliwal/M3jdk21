/* Copyright 2026 Hitesh Soliwal; SPDX-License-Identifier: Apache-2.0 */
package com.m3.tooling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

final class ExplicitViewConcatRecipeTest {
    private static final String API = """
        package com.m3.indexstring;
        public final class M3String {
            public static M3String fromString(String text) { return null; }
            public static M3String fromConcatOperands(String left, String right) { return null; }
            public String toString() { return null; }
        }
        """;
    @TempDir Path temporary;

    private Recipe recipe(boolean enabled) {
        try {
            return (Recipe)Class.forName("com.m3.tooling.ExplicitViewConcatRecipe")
                    .getConstructor(Boolean.class).newInstance(enabled);
        } catch (ClassNotFoundException missing) {
            fail("typed explicit-view recipe is not implemented");
            return null;
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    private String transform(String source, boolean enabled, String api) {
        var context = new InMemoryExecutionContext(failure -> { throw new AssertionError(failure); });
        J.CompilationUnit unit = assertInstanceOf(J.CompilationUnit.class,
                JavaParser.fromJavaVersion().dependsOn(api).build().parse(context, source).findFirst().orElseThrow());
        return assertInstanceOf(J.CompilationUnit.class,
                recipe(enabled).getVisitor().visit(unit, context)).printAll();
    }
    private String transform(String source) { return transform(source, true, API); }
    private static String admitted(String expression) {
        return "import com.m3.indexstring.M3String; class Sample { "
                + "M3String value(String left, String right) { return M3String.fromString("
                + expression + "); } }";
    }

    @Test void defaultDisabledPreservesInput() {
        String source = admitted("left + right");
        assertEquals(source, transform(source, false, API));
    }
    @Test void noArgumentConstructorAndNullOptionStayDisabled() throws ReflectiveOperationException {
        Class<?> type = Class.forName("com.m3.tooling.ExplicitViewConcatRecipe");
        String source = admitted("left + right");
        var context = new InMemoryExecutionContext(failure -> { throw new AssertionError(failure); });
        J.CompilationUnit unit = assertInstanceOf(J.CompilationUnit.class,
                JavaParser.fromJavaVersion().dependsOn(API).build().parse(context, source).findFirst().orElseThrow());
        for (Recipe disabled : List.of((Recipe)type.getConstructor().newInstance(),
                (Recipe)type.getConstructor(Boolean.class).newInstance(new Object[]{null}))) {
            assertEquals(source, assertInstanceOf(J.CompilationUnit.class,
                    disabled.getVisitor().visit(unit, context)).printAll());
        }
    }
    @Test void typedTwoStringBoundaryReplaysIdempotently() {
        String source = admitted("left + right");
        String expected = source.replace("fromString(left + right)", "fromConcatOperands(left, right)");
        assertEquals(expected, transform(source));
        assertEquals(expected, transform(expected));
    }
    @Test void preservesStaticQualifierExpression() {
        String source = "import com.m3.indexstring.M3String; class Sample { "
                + "M3String receiver() { return null; } M3String value(String left, String right) { "
                + "return receiver().fromString(left + right); } }";
        assertEquals(source.replace("fromString(left + right)", "fromConcatOperands(left, right)"), transform(source));
    }
    @Test void constantsAndFinalStringAliasesRefuse() {
        String literal = admitted("\"left\" + \"right\"");
        assertEquals(literal, transform(literal));
        String field = "import com.m3.indexstring.M3String; class Sample { static final String LEFT=\"left\"; "
                + "M3String value(String right) { return M3String.fromString(LEFT + right); } }";
        assertEquals(field, transform(field));
    }
    @Test void nonStringAndLiteralNullOperandsRefuse() {
        String object = admitted("left + (Object)right");
        assertEquals(object, transform(object));
        String nullLiteral = admitted("left + null");
        assertEquals(nullLiteral, transform(nullLiteral));
    }
    @Test void unresolvedOperandsRefuseWithoutEdits() {
        String source = admitted("left + missing()");
        assertEquals(source, transform(source));
    }
    @Test void missingDestinationAbiRefuses() {
        String api = "package com.m3.indexstring; public final class M3String { "
                + "public static M3String fromString(String text) { return null; } }";
        String source = admitted("left + right");
        assertEquals(source, transform(source, true, api));
        String wrongHelper = "package com.m3.indexstring; public final class M3String { "
                + "public static M3String fromString(String text) { return null; } "
                + "public static String fromConcatOperands(String left, String right) { return null; } }";
        assertEquals(source, transform(source, true, wrongHelper));
    }
    @Test void helperImplementationCannotRewriteItself() {
        String source = "package com.m3.indexstring; public final class M3String { "
                + "public static M3String fromString(String text) { return null; } "
                + "public static M3String fromConcatOperands(String left, String right) { "
                + "return M3String.fromString(left + right); } }";
        assertEquals(source, transform(source));
    }
    @Test void actualRecipeLifecycleProducesOneValidatedChange() {
        String source = admitted("left + right");
        var context = new InMemoryExecutionContext(failure -> { throw new AssertionError(failure); });
        var parsed = JavaParser.fromJavaVersion().dependsOn(API).build().parse(context, source).toList();
        var results = recipe(true).run(new InMemoryLargeSourceSet(parsed), context).getChangeset().getAllResults();
        assertEquals(1, results.size());
        assertEquals(source.replace("fromString(left + right)", "fromConcatOperands(left, right)"),
                results.getFirst().getAfter().printAll());
    }
    @Test void unrelatedOwnerAndOrdinaryPublicConcatenationStayUntouched() {
        String source = "class Sample { static String fromString(String text) { return text; } "
                + "String value(String left, String right) { return fromString(left + right); } }";
        assertEquals(source, transform(source));
        String ordinary = "class Sample { public String value(String left, String right) { return left + right; } }";
        assertEquals(ordinary, transform(ordinary));
    }
    @Test void commentsAndStaticImportedSitesRefuse() {
        String commented = admitted("left /* keep this comment */ + right");
        assertEquals(commented, transform(commented));
        String imported = "import static com.m3.indexstring.M3String.fromString; class Sample { "
                + "Object value(String left, String right) { return fromString(left + right); } }";
        assertEquals(imported, transform(imported));
    }
    @Test void exactCompiledBeforeAfterPreserveQualifierOrderNullAndRightFailure() throws Exception {
        String body = """
            import com.m3.indexstring.M3String;
            public class Behavior {
                static String trace = "";
                static int mode;
                static M3String receiver() { trace += "S"; if (mode == 3) throw new IllegalStateException("select"); return null; }
                static String left() { trace += "L"; if (mode == 2) throw new IllegalStateException("left"); return mode == 4 ? null : "left"; }
                static String right() { trace += "R"; if (mode == 1) throw new IllegalStateException("right"); return null; }
                static M3String value() { return receiver().fromString(left() + right()); }
                public static void main(String[] args) {
                    for (mode = 0; mode <= 4; mode++) {
                        trace = "";
                        try { System.out.println(value().toString() + "|" + trace); }
                        catch (IllegalStateException failure) {
                            System.out.println(failure.getMessage() + "|" + trace);
                        }
                    }
                }
            }
            """;
        String expected = "leftnull|SLR\nright|SLR\nleft|SL\nselect|S\nnullnull|SLR\n";
        assertEquals(expected, compileAndRun(body, temporary.resolve("before")));
        String transformed = transform(body);
        assertEquals(body.replace("fromString(left() + right())", "fromConcatOperands(left(), right())"), transformed);
        assertEquals(expected, compileAndRun(transformed, temporary.resolve("after")));
    }

    private String compileAndRun(String source, Path directory) throws Exception {
        Files.createDirectories(directory);
        Path fixture = directory.resolve("Behavior.java");
        Files.writeString(fixture, source, StandardCharsets.UTF_8);
        List<String> arguments = new ArrayList<>(List.of("--release", "21", "-d", directory.toString()));
        try (var paths = Files.walk(Path.of(System.getProperty("m3.text.sourceRoot")))) {
            paths.filter(path -> path.toString().endsWith(".java")).sorted()
                    .map(Path::toString).forEach(arguments::add);
        }
        arguments.add(fixture.toString());
        var errors = new ByteArrayOutputStream();
        int compileExit = ToolProvider.getSystemJavaCompiler().run(null, null, errors,
                arguments.toArray(String[]::new));
        System.out.println("M3_PROOF_BEGIN=" + directory.getFileName());
        arguments.forEach(argument -> System.out.println("M3_JAVAC_ARG=" + argument));
        System.out.println("M3_JAVAC_EXIT=" + compileExit);
        assertEquals(0, compileExit, errors.toString(StandardCharsets.UTF_8));
        String java = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        List<String> command = List.of(Path.of(System.getProperty("java.home"), "bin", java).toString(),
                "-Xms16m", "-Xmx64m", "-XX:ActiveProcessorCount=2", "-ea", "-cp", directory.toString(), "Behavior");
        command.forEach(argument -> System.out.println("M3_JAVA_ARG=" + argument));
        Process child = new ProcessBuilder(command)
                .redirectErrorStream(true).start();
        String output = new String(child.getInputStream().readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        int runExit = child.waitFor();
        System.out.println("M3_JAVA_EXIT=" + runExit);
        System.out.println("M3_OUTPUT_BASE64=" + Base64.getEncoder().encodeToString(output.getBytes(StandardCharsets.UTF_8)));
        System.out.println("M3_PROOF_END=" + directory.getFileName());
        assertEquals(0, runExit, output);
        return output;
    }
}
