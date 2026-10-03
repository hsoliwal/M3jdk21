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

final class M3Jdk8359706BackportRecipeTest {
    private static final String PRE =
            "/com/m3/rewrite/backport/jdk27-open-fd-8359706/pre/";

    private static final String AIX = "src/hotspot/os/aix/os_aix.cpp";
    private static final String BSD = "src/hotspot/os/bsd/os_bsd.cpp";
    private static final String BSD_HPP = "src/hotspot/os/bsd/os_bsd.hpp";
    private static final String LINUX = "src/hotspot/os/linux/os_linux.cpp";
    private static final String WINDOWS = "src/hotspot/os/windows/os_windows.cpp";
    private static final String OS_HPP = "src/hotspot/share/runtime/os.hpp";
    private static final String VM_ERROR = "src/hotspot/share/utilities/vmError.cpp";
    private static final String TEST = "test/jdk/sun/tools/jcmd/TestJcmdSanity.java";

    @Test
    void packetOwnsBothDonorCommitsAndTwoRecipeAtoms() {
        Recipe recipe = new M3Jdk8359706BackportRecipe();
        assertEquals("b0831572e2cd9dbff9ee2abcdf81a493ddcecc7e",
                M3Jdk8359706BackportRecipe.PRIMARY_COMMIT);
        assertEquals("3a109f49feb19f313632be6a2aa24ba7d9b7269b",
                M3Jdk8359706BackportRecipe.MACOS_FIX_COMMIT);
        assertEquals(2, recipe.getRecipeList().size());
        assertEquals("jdk27-open-fd-8359706-java",
                assertInstanceOf(M3Jdk21HashPinnedSnapshotRecipe.class,
                        recipe.getRecipeList().get(0)).getCrateName());
        assertEquals("jdk27-open-fd-8359706-text",
                assertInstanceOf(M3Jdk21HashPinnedTextSnapshotRecipe.class,
                        recipe.getRecipeList().get(1)).getCrateName());
        assertTrue(recipe.getTags().contains("jdk-8380236"));
    }

    @Test
    void exactJava21PreimagesReachCumulativePostimageAndFixedPoint() throws Exception {
        Recipe recipe = new M3Jdk8359706BackportRecipe();
        var first = recipe.run(new InMemoryLargeSourceSet(baseline()), context(), 1);
        List<Result> changes = first.getChangeset().getAllResults();
        assertEquals(8, changes.size());

        assertChangedContains(changes, LINUX, "Open File Descriptors:");
        assertChangedContains(changes, BSD, "precond(buflen >= sizeof(struct proc_fdinfo))");
        assertChangedContains(changes, BSD_HPP, "print_open_file_descriptors");
        assertChangedContains(changes, AIX, "File descriptor counting not implemented on AIX");
        assertChangedContains(changes, WINDOWS, "File descriptor counting not supported on Windows");
        assertChangedContains(changes, OS_HPP, "static void print_open_file_descriptors");
        assertChangedContains(changes, VM_ERROR, "os::print_open_file_descriptors(st)");
        assertChangedContains(changes, TEST, "Open File Descriptors: \\d+");

        List<SourceFile> after = changes.stream().map(Result::getAfter).toList();
        assertTrue(recipe.run(new InMemoryLargeSourceSet(after), context(), 1)
                .getChangeset().getAllResults().isEmpty());
    }

    @Test
    void stalePreimageFailsClosed() throws Exception {
        List<SourceFile> sources = new ArrayList<>(baseline());
        sources.set(3, text(LINUX,
                sources.get(3).printAll().replace("OS:", "OS: drift")));
        assertThrows(RuntimeException.class, () ->
                new M3Jdk8359706BackportRecipe()
                        .run(new InMemoryLargeSourceSet(sources), context(), 1)
                        .getChangeset().getAllResults());
    }

    private static void assertChangedContains(List<Result> changes, String path, String needle) {
        assertTrue(changes.stream().anyMatch(result ->
                result.getAfter() != null
                        && path.equals(normalized(result.getAfter().getSourcePath()))
                        && result.getAfter().printAll().contains(needle)),
                path + " should contain " + needle);
    }

    private static List<SourceFile> baseline() throws Exception {
        return List.of(
                text(AIX, resource("os_aix.cpp.before.txt")),
                text(BSD, resource("os_bsd.cpp.before.txt")),
                text(BSD_HPP, resource("os_bsd.hpp.before.txt")),
                text(LINUX, resource("os_linux.cpp.before.txt")),
                text(WINDOWS, resource("os_windows.cpp.before.txt")),
                text(OS_HPP, resource("os.hpp.before.txt")),
                text(VM_ERROR, resource("vmError.cpp.before.txt")),
                java(TEST, resource("TestJcmdSanity.java.before.txt")));
    }

    private static SourceFile java(String path, String source) {
        return JavaParser.fromJavaVersion().build()
                .parseInputs(List.of(Parser.Input.fromString(Path.of(path), source)), null, context())
                .findFirst().orElseThrow();
    }

    private static SourceFile text(String path, String source) {
        return PlainText.builder().sourcePath(Path.of(path)).text(source).build();
    }

    private static String resource(String name) throws IOException {
        try (var input = M3Jdk8359706BackportRecipeTest.class.getResourceAsStream(PRE + name)) {
            if (input == null) throw new IOException("missing preimage resource " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
    }

    private static String normalized(Path path) {
        return path.toString().replace('\\', '/');
    }
}
