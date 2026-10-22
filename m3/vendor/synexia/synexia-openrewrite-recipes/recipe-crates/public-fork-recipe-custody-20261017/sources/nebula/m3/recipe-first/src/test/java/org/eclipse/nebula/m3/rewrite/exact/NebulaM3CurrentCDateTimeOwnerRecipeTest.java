// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

final class NebulaM3CurrentCDateTimeOwnerRecipeTest {
    private static final String ROOT =
            "/org/eclipse/nebula/m3/rewrite/exact/cdatetime-current-owner/";

    private record Case(String path, String before, String after, boolean java) {}

    private static final List<Case> CASES = List.of(
            new Case(
                    "releng/org.eclipse.nebula.nebula-parent/pom.xml",
                    "pre-00-nebula-parent-pom.xml.txt",
                    "00-nebula-parent-pom.xml.txt",
                    false),
            new Case(
                    "widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.css/src/org/eclipse/nebula/widgets/cdatetime/css/CDateTimePropertyHandler.java",
                    "pre-01-CDateTimePropertyHandler.java.txt",
                    "01-CDateTimePropertyHandler.java.txt",
                    true),
            new Case(
                    "widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.example.e4/META-INF/MANIFEST.MF",
                    "pre-02-MANIFEST.MF.txt",
                    "02-MANIFEST.MF.txt",
                    false),
            new Case(
                    "widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.example.e4/org.eclipse.nebula.widgets.cdatetime.example.e4.product",
                    "pre-03-cdatetime-e4.product.txt",
                    "03-cdatetime-e4.product.txt",
                    false),
            new Case(
                    "widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.example.e4/src/org/eclipse/nebula/widgets/cdatetime/example/e4/parts/BigWidgetsPart.java",
                    "pre-04-BigWidgetsPart.java.txt",
                    "04-BigWidgetsPart.java.txt",
                    true),
            new Case(
                    "widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.example.e4/src/org/eclipse/nebula/widgets/cdatetime/example/e4/parts/SimpleWidgetsPart.java",
                    "pre-05-SimpleWidgetsPart.java.txt",
                    "05-SimpleWidgetsPart.java.txt",
                    true),
            new Case(
                    "widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.tests/src/org/eclipse/nebula/widgets/cdatetime/css/BaseCSSThemingTest.java",
                    "pre-06-BaseCSSThemingTest.java.txt",
                    "06-BaseCSSThemingTest.java.txt",
                    true));

    @Test
    void everyExistingTargetReplaysExactlyAndReachesFixedPoint() {
        List<Recipe> steps = new NebulaM3CurrentCDateTimeOwnerRecipe().getRecipeList();
        assertEquals(8, steps.size());

        for (int index = 0; index < CASES.size(); index++) {
            Case fixture = CASES.get(index);
            String beforeText = resource(fixture.before());
            String afterText = resource(fixture.after());

            var firstErrors = new ArrayList<Throwable>();
            var firstContext = new InMemoryExecutionContext(firstErrors::add);
            SourceFile before = source(fixture, beforeText, firstContext);
            String expectedAfter =
                    fixture.java()
                            ? javaSource(fixture.path(), afterText, firstContext).printAll()
                            : afterText;
            var first = steps.get(index).run(
                    new InMemoryLargeSourceSet(List.of(before)), firstContext);
            assertTrue(firstErrors.isEmpty(), fixture.path() + ": " + firstErrors);
            var changes = first.getChangeset().getAllResults();
            assertEquals(1, changes.size(), fixture.path());
            SourceFile after = changes.getFirst().getAfter();
            assertEquals(expectedAfter, after.printAll(), fixture.path());

            var secondErrors = new ArrayList<Throwable>();
            var secondContext = new InMemoryExecutionContext(secondErrors::add);
            var second = steps.get(index).run(
                    new InMemoryLargeSourceSet(List.of(after)), secondContext);
            assertTrue(secondErrors.isEmpty(), fixture.path() + ": " + secondErrors);
            assertTrue(second.getChangeset().getAllResults().isEmpty(), fixture.path());
        }
    }

    @Test
    void providerIsGeneratedOnlyBesideReviewedTestAnchorAndThenStops() {
        List<Recipe> steps = new NebulaM3CurrentCDateTimeOwnerRecipe().getRecipeList();
        Recipe materializer = steps.get(7);
        Case anchorCase = CASES.get(6);

        var context = new InMemoryExecutionContext(failure -> {
            throw new AssertionError(failure);
        });
        SourceFile anchor = source(anchorCase, resource(anchorCase.after()), context);
        var first = materializer.run(
                new InMemoryLargeSourceSet(List.of(anchor)), context, 1);
        var changes = first.getChangeset().getAllResults();
        assertEquals(1, changes.size());
        SourceFile provider = changes.getFirst().getAfter();
        assertEquals(
                "widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.tests/src/org/eclipse/nebula/widgets/cdatetime/tests/css/CSSPropertyHandlerSimpleProviderImpl.java",
                normalized(provider.getSourcePath()));
        assertEquals(
                javaSource(
                                "widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.tests/src/org/eclipse/nebula/widgets/cdatetime/tests/css/CSSPropertyHandlerSimpleProviderImpl.java",
                                resource("07-CSSPropertyHandlerSimpleProviderImpl.java.txt"),
                                context)
                        .printAll(),
                provider.printAll());

        var second = materializer.run(
                new InMemoryLargeSourceSet(List.of(anchor, provider)), context, 1);
        assertTrue(second.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void sourceDriftIsRefusedWithoutCandidateMutation() {
        Recipe javaStep = new NebulaM3CurrentCDateTimeOwnerRecipe().getRecipeList().get(1);
        Case fixture = CASES.get(1);
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        String driftText = resource(fixture.before()) + "// drift\n";
        SourceFile drift = source(fixture, driftText, context);
        String original = drift.printAll();

        var run = javaStep.run(new InMemoryLargeSourceSet(List.of(drift)), context);
        assertTrue(run.getChangeset().getAllResults().isEmpty());
        assertFalse(errors.isEmpty());
        assertEquals(original, drift.printAll());
    }

    @Test
    void reviewedFontClassifierPreservesLegacyEffectsAndNullFailure(
            @org.junit.jupiter.api.io.TempDir Path root) throws Exception {
        String after = resource("01-CDateTimePropertyHandler.java.txt");
        int begin = after.indexOf("private static String legacyFontProperty");
        int end = after.indexOf("\n\t// CSS Font", begin);
        assertTrue(begin >= 0 && end > begin);
        String helper = after.substring(begin, end).replace("private static", "public static");
        Path source = root.resolve("ReviewedCssHelper.java");
        java.nio.file.Files.writeString(
                source,
                "import org.w3c.dom.css.CSSPrimitiveValue; public class ReviewedCssHelper {"
                        + helper + "}",
                StandardCharsets.UTF_8);
        var compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
        assertFalse(compiler == null, "full JDK required");
        try (var manager = compiler.getStandardFileManager(null, null, null)) {
            assertTrue(
                    compiler.getTask(
                                    null,
                                    manager,
                                    null,
                                    List.of("--release", "21", "-d", root.toString()),
                                    null,
                                    manager.getJavaFileObjectsFromPaths(List.of(source)))
                            .call());
        }
        try (var loader =
                new java.net.URLClassLoader(
                        new java.net.URL[] {root.toUri().toURL()},
                        getClass().getClassLoader())) {
            var method = Class.forName("ReviewedCssHelper", true, loader)
                    .getMethod("legacyFontProperty", org.w3c.dom.css.CSSPrimitiveValue.class);
            for (short type : new short[] {
                    org.w3c.dom.css.CSSPrimitiveValue.CSS_IDENT,
                    org.w3c.dom.css.CSSPrimitiveValue.CSS_STRING}) {
                var failure = assertThrows(
                        java.lang.reflect.InvocationTargetException.class,
                        () -> method.invoke(null, primitive(type, null)));
                assertEquals(NullPointerException.class, failure.getCause().getClass());
                assertEquals("font-style", method.invoke(null, primitive(type, "italic")));
                assertEquals("font-style", method.invoke(null, primitive(type, "oblique")));
                assertEquals("font-weight", method.invoke(null, primitive(type, "normal")));
                assertEquals("font-weight", method.invoke(null, primitive(type, "bold")));
                assertEquals("font-family", method.invoke(null, primitive(type, "ITALIC")));
                assertEquals("font-family", method.invoke(null, primitive(type, "")));
            }
            assertEquals(
                    null,
                    method.invoke(
                            null,
                            primitive(
                                    org.w3c.dom.css.CSSPrimitiveValue.CSS_PERCENTAGE,
                                    "25%")));
        }
    }

    @Test
    void reviewedProviderRegistrationRetainsAllTwentyThreeCDateTimeProperties() {
        String after = resource("06-BaseCSSThemingTest.java.txt");
        List<String> properties = List.of(
                "cdt-background-color",
                "cdt-color",
                "cdt-font",
                "cdt-font-style",
                "cdt-font-size",
                "cdt-font-weight",
                "cdt-font-family",
                "cdt-picker-background-color",
                "cdt-picker-color",
                "cdt-picker-font",
                "cdt-picker-font-style",
                "cdt-picker-font-size",
                "cdt-picker-font-weight",
                "cdt-picker-font-family",
                "cdt-picker-active-day-color",
                "cdt-picker-inactive-day-color",
                "cdt-picker-today-color",
                "cdt-picker-minutes-color",
                "cdt-picker-minutes-background-color",
                "cdt-button-hover-border-color",
                "cdt-button-hover-background-color",
                "cdt-button-selected-border-color",
                "cdt-button-selected-background-color");
        assertEquals(23, properties.size());
        for (String property : properties) {
            assertTrue(after.contains("\"" + property + "\""), property);
        }
        assertTrue(after.contains("registerCSSPropertyHandlerProvider(handlerProvider)"));
    }

    @Test
    void reviewedE4PostsUseJakartaAnnotationsWithoutChangingWidgetCodeShape() {
        String manifest = resource("02-MANIFEST.MF.txt");
        String product = resource("03-cdatetime-e4.product.txt");
        String big = resource("04-BigWidgetsPart.java.txt");
        String simple = resource("05-SimpleWidgetsPart.java.txt");

        assertTrue(manifest.contains("jakarta.annotation;version=\"[3.0.0,4.0.0)\""));
        assertFalse(manifest.contains("javax.annotation;"));
        assertTrue(product.contains("<plugin id=\"jakarta.annotation-api\"/>"));
        assertTrue(product.contains("<plugin id=\"jakarta.inject.jakarta.inject-api\"/>"));
        assertFalse(product.contains("<plugin id=\"javax.annotation\"/>"));
        assertFalse(product.contains("<plugin id=\"javax.inject\"/>"));
        assertTrue(big.contains("import jakarta.annotation.PostConstruct;"));
        assertTrue(big.contains("import jakarta.annotation.PreDestroy;"));
        assertTrue(simple.contains("import jakarta.annotation.PostConstruct;"));
        assertTrue(simple.contains("import jakarta.annotation.PreDestroy;"));
    }

    @Test
    void recipeRemainsAnExplicitEightStepCurrentOwnerPacket() {
        NebulaM3CurrentCDateTimeOwnerRecipe recipe =
                new NebulaM3CurrentCDateTimeOwnerRecipe();
        assertEquals(8, recipe.getRecipeList().size());
        assertTrue(recipe.getTags().contains("recipe-first"));
        assertTrue(recipe.getTags().contains("current-master"));
    }

    private static org.w3c.dom.css.CSSPrimitiveValue primitive(short type, String text) {
        return (org.w3c.dom.css.CSSPrimitiveValue)
                java.lang.reflect.Proxy.newProxyInstance(
                        NebulaM3CurrentCDateTimeOwnerRecipeTest.class.getClassLoader(),
                        new Class<?>[] {org.w3c.dom.css.CSSPrimitiveValue.class},
                        (proxy, method, args) -> switch (method.getName()) {
                            case "getPrimitiveType" -> type;
                            case "getStringValue", "getCssText" -> text;
                            default -> throw new UnsupportedOperationException(method.getName());
                        });
    }

    private static SourceFile source(Case fixture, String text, InMemoryExecutionContext context) {
        if (!fixture.java()) {
            return PlainText.builder()
                    .sourcePath(Path.of(fixture.path()))
                    .text(text)
                    .build();
        }
        return javaSource(fixture.path(), text, context);
    }

    private static SourceFile javaSource(
            String path, String text, InMemoryExecutionContext context) {
        List<SourceFile> parsed = JavaParser.fromJavaVersion().build()
                .parseInputs(
                        List.of(Parser.Input.fromString(Path.of(path), text)),
                        null,
                        context)
                .toList();
        assertEquals(1, parsed.size(), path);
        return parsed.getFirst();
    }

    private static String resource(String name) {
        try (var stream =
                NebulaM3CurrentCDateTimeOwnerRecipeTest.class.getResourceAsStream(ROOT + name)) {
            if (stream == null) throw new IllegalStateException("missing resource " + name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static String normalized(Path path) {
        return path.toString().replace('\\', '/');
    }
}
