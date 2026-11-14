// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
package jdk.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import javax.tools.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

/** Ordinary Maven/Jupiter coverage; runtime fixtures compile entirely in memory. */
final class M3LexiconNormalizationRecipeTest {
    private static final String CRATE = "synexia-instance-normalize-r247";
    private static final String TARGET = "m3/core/src/jdk/m3/text/M3InstanceIndexPrecompute.java";
    private static final String OWNER = "jdk.m3.text.M3InstanceIndexPrecompute";
    private static final String FIXTURE =
            "/jdk/m3/rewrite/backport/lexicon-contracts/r248/instance-before.java.txt";
    private static final String POSTIMAGE =
            "/jdk/m3/rewrite/backport/jdk21-hash-pinned/" + CRATE + "/M3InstanceIndexPrecompute.java.txt";
    static final java.util.concurrent.atomic.AtomicLong comparedCases = new java.util.concurrent.atomic.AtomicLong();
    private static String before;
    private static String after;
    private static Method originalNormalize;
    private static Method generatedNormalize;

    @BeforeAll
    static void sourceBoundRuntimeFixtures() throws Exception {
        before = text(FIXTURE);
        after = text(POSTIMAGE);
        assertEquals("a40be1c52aa7dd71755abe81ca5ad42fd2376569205ad015195b85dca9863d42",
                M3Jdk21HashPinnedSnapshotRecipe.sha256(before));
        assertEquals("2cfed3d012c472927c7afe5f35ad5b456b16a9e38174676ec5f8484ac8ebfe7d",
                M3Jdk21HashPinnedSnapshotRecipe.sha256(after));
        Class<?> original = compile(before);
        Class<?> generated = compile(after);
        assertNotSame(original.getClassLoader(), generated.getClassLoader());
        assertNotSame(M3LexiconNormalizationRecipeTest.class.getClassLoader(), generated.getClassLoader());
        originalNormalize = original.getMethod("normalizedInstanceName", String.class);
        generatedNormalize = generated.getMethod("normalizedInstanceName", String.class);
    }

    @Test
    void installedCrateOwnsOnlyTheReviewedSource() {
        var recipe = new M3Jdk21HashPinnedSnapshotRecipe(CRATE);
        assertEquals(List.of(TARGET), recipe.targetPaths());
        assertEquals(CRATE, recipe.getCrateName());
    }

    @Test
    void replayIsExactPreservesIdentityAndReachesFixedPoint() {
        SourceFile input = parse(before);
        var changes = apply(List.of(input));
        assertEquals(1, changes.size());
        SourceFile result = Objects.requireNonNull(changes.getFirst().getAfter());
        assertEquals(after, result.printAll());
        assertEquals(input.getId(), result.getId());
        assertEquals(input.getSourcePath(), result.getSourcePath());
        assertTrue(apply(List.of(result)).isEmpty());
    }

    @Test
    void missingForeignDuplicateAndWrongTreeInputsAreRefused() {
        assertThrows(RuntimeException.class, () -> apply(List.of()));
        SourceFile foreign = parse("package jdk.m3.text; public class M3InstanceIndexPrecompute {}\n");
        assertThrows(RuntimeException.class, () -> apply(List.of(foreign)));
        SourceFile input = parse(before);
        assertThrows(RuntimeException.class, () -> apply(List.of(input, input.withId(Tree.randomId()))));
        SourceFile wrongTree = PlainText.builder().sourcePath(Path.of(TARGET)).text(before).build();
        assertThrows(RuntimeException.class, () -> apply(List.of(wrongTree)));
    }

    @Test
    void exactCorePathDoesNotOpenNeighboringOwners() {
        assertTrue(M3Jdk21HashPinnedSnapshotRecipe.jdkJavaPath(TARGET));
        assertTrue(M3Jdk21HashPinnedSnapshotRecipe.synexiaReceiverJavaPath(TARGET));
        for (String path : List.of(TARGET.replace("Precompute.java", "Other.java"),
                TARGET.replace("/text/", "/other/"), "../" + TARGET, "/" + TARGET,
                TARGET.replace("/text/", "/text/../text/"), TARGET.replace('/', '\\'),
                TARGET.replace("/src/", "/src//"), TARGET + ".bak")) {
            assertFalse(M3Jdk21HashPinnedSnapshotRecipe.jdkJavaPath(path), path);
            assertFalse(M3Jdk21HashPinnedSnapshotRecipe.synexiaReceiverJavaPath(path), path);
        }
        assertTrue(M3Jdk21HashPinnedSnapshotRecipe.jdkJavaPath(
                "m3/core/src/jdk/m3/text/M3FixedAddressMapping.java"));
        assertFalse(M3Jdk21HashPinnedSnapshotRecipe.jdkJavaPath(
                "m3/core/src/jdk/m3/text/M3FixedAddressMappingOther.java"));
    }

    @Test
    void runtimeNormalizationMatchesOriginalForEveryUtf16Unit() throws Exception {
        same(null);
        same("");
        for (int unit = 0; unit <= 0xffff; unit++) {
            char item = (char) unit;
            same(String.valueOf(item));
            same("A" + item + "Z");
        }
    }

    @Test
    void runtimeFreshnessMultilingualTextAndMixedErrorsRemainExact() throws Exception {
        for (String item : List.of("lower", "UPPER", "aBc", " lower ", "ÀBC", "İstanbul",
                "Straße", "Σίσυφος", "漢字", "e\u0301 É", "\ud83d\ude00aBC", "\udbff\udfff",
                "\t\n", "\u000b", "\u000c", "\u0000\ud800", "\ud800\u0000",
                "\udc00\u0001", "\ud800\udc00\u0001", "\u00a0", "\u2003",
                "\u007f", "\ufffe", "\uffff")) same(item);
        String input = "already-lower";
        String result = (String) generatedNormalize.invoke(null, input);
        assertNotSame(input, result);
        assertSame(input, result.intern());
        Throwable failure = failure(generatedNormalize, "\u0000\ud800");
        assertEquals(IllegalArgumentException.class, failure.getClass());
        assertEquals("name contains an unpaired surrogate", failure.getMessage());
    }

    private static void same(String input) throws Exception {
        comparedCases.incrementAndGet();
        Throwable originalFailure = failure(originalNormalize, input);
        Throwable generatedFailure = failure(generatedNormalize, input);
        if (originalFailure != null || generatedFailure != null) {
            assertNotNull(originalFailure);
            assertNotNull(generatedFailure);
            assertEquals(originalFailure.getClass(), generatedFailure.getClass());
            assertEquals(originalFailure.getMessage(), generatedFailure.getMessage());
            return;
        }
        String original = (String) originalNormalize.invoke(null, (Object) input);
        String generated = (String) generatedNormalize.invoke(null, (Object) input);
        assertEquals(original, generated);
        assertEquals(original.hashCode(), generated.hashCode());
        assertNotSame(input, generated);
        assertNotSame(generated, generatedNormalize.invoke(null, (Object) input));
    }

    private static Throwable failure(Method method, String input) throws Exception {
        try {
            method.invoke(null, (Object) input);
            return null;
        } catch (InvocationTargetException failure) {
            return failure.getCause();
        }
    }

    private static SourceFile parse(String source) {
        var inputs = List.of(Parser.Input.fromString(Path.of(TARGET), source));
        var parsed = JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath())
                .build().parseInputs(inputs, null, context()).toList();
        assertEquals(1, parsed.size());
        return parsed.getFirst();
    }

    private static List<org.openrewrite.Result> apply(List<SourceFile> sources) {
        return new M3Jdk21HashPinnedSnapshotRecipe(CRATE)
                .run(new InMemoryLargeSourceSet(sources), context(), 1)
                .getChangeset().getAllResults();
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(failure -> { throw new AssertionError(failure); });
    }

    private static String text(String resource) throws IOException {
        try (InputStream stream = M3LexiconNormalizationRecipeTest.class.getResourceAsStream(resource)) {
            return new String(Objects.requireNonNull(stream, resource).readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Adapted from the existing M3LexiconCpuProof memory compiler; no disk output. */
    private static Class<?> compile(String source) throws IOException, ClassNotFoundException {
        JavaCompiler compiler = Objects.requireNonNull(ToolProvider.getSystemJavaCompiler(),
                "JDK21 compiler required");
        Map<String, ByteArrayOutputStream> outputs = new HashMap<>();
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var standard = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8);
                var files = new ForwardingJavaFileManager<StandardJavaFileManager>(standard) {
                    @Override
                    public JavaFileObject getJavaFileForOutput(Location location, String name,
                            JavaFileObject.Kind kind, FileObject sibling) {
                        if (kind != JavaFileObject.Kind.CLASS) throw new AssertionError("non-class output");
                        return new SimpleJavaFileObject(URI.create("mem:///" + name.replace('.', '/') + kind.extension), kind) {
                            @Override
                            public OutputStream openOutputStream() {
                                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                                if (outputs.put(name, bytes) != null) throw new AssertionError("duplicate class");
                                return bytes;
                            }
                        };
                    }
                }) {
            JavaFileObject input = new SimpleJavaFileObject(
                    URI.create("string:///" + OWNER.replace('.', '/') + ".java"), JavaFileObject.Kind.SOURCE) {
                @Override
                public CharSequence getCharContent(boolean ignored) { return source; }
            };
            var options = List.of("--release", "21", "-proc:none", "-g:none", "-Xlint:all", "-Werror", "-classpath", "");
            assertTrue(compiler.getTask(null, files, diagnostics, options, null, List.of(input)).call(),
                    () -> diagnostics.getDiagnostics().toString());
        }
        ClassLoader loader = new ClassLoader(null) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                ByteArrayOutputStream bytes = outputs.get(name);
                if (bytes == null) throw new ClassNotFoundException(name);
                return defineClass(name, bytes.toByteArray(), 0, bytes.size());
            }
        };
        Class<?> type = loader.loadClass(OWNER);
        assertSame(loader, type.getClassLoader());
        return type;
    }
}
