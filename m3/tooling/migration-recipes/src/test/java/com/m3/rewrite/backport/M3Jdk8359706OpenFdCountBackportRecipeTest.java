// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

final class M3Jdk8359706OpenFdCountBackportRecipeTest {
    private static final String PRE =
            "/com/m3/rewrite/backport/jdk27-open-fd-8359706/pre/";

    private static final String AIX = "src/hotspot/os/aix/os_aix.cpp";
    private static final String BSD_CPP = "src/hotspot/os/bsd/os_bsd.cpp";
    private static final String BSD_HPP = "src/hotspot/os/bsd/os_bsd.hpp";
    private static final String LINUX = "src/hotspot/os/linux/os_linux.cpp";
    private static final String WINDOWS = "src/hotspot/os/windows/os_windows.cpp";
    private static final String OS_HPP = "src/hotspot/share/runtime/os.hpp";
    private static final String VM_ERROR = "src/hotspot/share/utilities/vmError.cpp";
    private static final String JTREG = "test/jdk/sun/tools/jcmd/TestJcmdSanity.java";

    @Test
    void compositeOwnsEightIndependentFileAtomsAndBothDonorPins() {
        Recipe recipe = new M3Jdk8359706OpenFdCountBackportRecipe();

        assertEquals(
                "b0831572e2cd9dbff9ee2abcdf81a493ddcecc7e",
                M3Jdk8359706OpenFdCountBackportRecipe.PRIMARY_UPSTREAM_COMMIT);
        assertEquals(
                "3a109f49feb19f313632be6a2aa24ba7d9b7269b",
                M3Jdk8359706OpenFdCountBackportRecipe.MACOS_FOLLOWUP_COMMIT);
        assertEquals(8, recipe.getRecipeList().size());
        for (int index = 0; index < 7; index++) {
            assertInstanceOf(
                    M3Jdk21HashPinnedTextSnapshotRecipe.class,
                    recipe.getRecipeList().get(index));
        }
        assertInstanceOf(
                M3Jdk21HashPinnedSnapshotRecipe.class,
                recipe.getRecipeList().get(7));
        assertEquals(1, recipe.maxCycles());
        assertTrue(recipe.getTags().contains("dag-composable"));
        assertTrue(recipe.getTags().contains("module-scope"));
    }

    @Test
    void exactJdk21PreimagesReachAdaptedPostimageAndFixedPoint() throws Exception {
        Recipe recipe = new M3Jdk8359706OpenFdCountBackportRecipe();
        var first = recipe.run(new InMemoryLargeSourceSet(baseline()), context(), 1);
        List<Result> changes = first.getChangeset().getAllResults();

        assertEquals(8, changes.size());
        assertContains(changes, LINUX, "opendir(\"/proc/self/fd\")");
        assertContains(changes, LINUX, "Open File Descriptors: unknown");
        assertContains(changes, BSD_CPP, "#include <libproc.h>");
        assertContains(changes, BSD_CPP, "precond(buflen >= sizeof(struct proc_fdinfo))");
        assertContains(changes, BSD_CPP, "proc_pidinfo(");
        assertContains(changes, BSD_HPP, "print_open_file_descriptors");
        assertContains(changes, OS_HPP, "static void print_open_file_descriptors");
        assertContains(changes, VM_ERROR, "os::Bsd::print_open_file_descriptors");
        assertContains(changes, VM_ERROR, "os::print_open_file_descriptors");
        assertContains(changes, AIX, "File descriptor counting not implemented on AIX");
        assertContains(changes, WINDOWS, "File descriptor counting not supported on Windows");
        assertContains(changes, JTREG, "8359706 8380236");
        assertContains(changes, JTREG, "Open File Descriptors:");
        assertTrue(
                changes.stream()
                        .filter(result -> JTREG.equals(path(result.getAfter())))
                        .noneMatch(result -> result.getAfter().printAll().contains("jdk.test.lib.Utils")));

        List<SourceFile> after = changes.stream().map(Result::getAfter).toList();
        assertTrue(
                recipe.run(new InMemoryLargeSourceSet(after), context(), 1)
                        .getChangeset()
                        .getAllResults()
                        .isEmpty());
    }

    @Test
    void staleLinuxPreimageFailsClosed() throws Exception {
        List<SourceFile> sources = new ArrayList<>(baseline());
        SourceFile linux = sources.get(3);
        sources.set(3, text(LINUX, linux.printAll() + "// drift\n"));

        assertThrows(
                RuntimeException.class,
                () ->
                        new M3Jdk8359706OpenFdCountBackportRecipe()
                                .run(new InMemoryLargeSourceSet(sources), context(), 1)
                                .getChangeset()
                                .getAllResults());
    }

    private static List<SourceFile> baseline() throws Exception {
        return List.of(
                text(AIX, resource("os_aix.cpp.before.txt")),
                text(BSD_CPP, resource("os_bsd.cpp.before.txt")),
                text(BSD_HPP, resource("os_bsd.hpp.before.txt")),
                text(LINUX, resource("os_linux.cpp.before.txt")),
                text(WINDOWS, resource("os_windows.cpp.before.txt")),
                text(OS_HPP, resource("os.hpp.before.txt")),
                text(VM_ERROR, resource("vmError.cpp.before.txt")),
                java(JTREG, resource("TestJcmdSanity.java.before.txt")));
    }

    private static void assertContains(List<Result> changes, String target, String token) {
        assertTrue(
                changes.stream()
                        .anyMatch(
                                result ->
                                        target.equals(path(result.getAfter()))
                                                && result.getAfter().printAll().contains(token)),
                target + " missing " + token);
    }

    private static SourceFile text(String path, String source) {
        return PlainText.builder().sourcePath(Path.of(path)).text(source).build();
    }

    private static SourceFile java(String path, String source) {
        InMemoryExecutionContext context = context();
        return JavaParser.fromJavaVersion()
                .build()
                .parseInputs(
                        List.of(Parser.Input.fromString(Path.of(path), source)),
                        null,
                        context)
                .findFirst()
                .orElseThrow();
    }

    private static String resource(String name) throws IOException {
        try (var input =
                M3Jdk8359706OpenFdCountBackportRecipeTest.class.getResourceAsStream(PRE + name)) {
            if (input == null) {
                throw new IOException("missing preimage " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(
                error -> {
                    throw new AssertionError(error);
                });
    }

    private static String path(SourceFile source) {
        return source.getSourcePath().toString().replace('\\', '/');
    }
}
