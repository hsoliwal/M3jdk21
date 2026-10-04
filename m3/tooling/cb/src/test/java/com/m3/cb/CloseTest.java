// SPDX-License-Identifier: Apache-2.0
package com.m3.cb;

import static org.junit.jupiter.api.Assertions.*;
import com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

/** Strict full-image build exposed an inherited, unnecessarily broad close contract. */
final class CloseTest {
    private static final Path CRATE = Path.of(System.getProperty("m3.crate"));
    private static final String TARGET = "src/java.base/share/classes/jdk/internal/mindex/MIndexStringBacking.java";

    private SourceFile before() throws Exception {
        return JavaParser.fromJavaVersion().build()
                .parse(Files.readString(CRATE.resolve("close/MIndexStringBacking.java.before")))
                .findFirst().orElseThrow().withSourcePath(Path.of(TARGET));
    }

    @Test void exactRewriteFixedPointAndStrictCompile() throws Exception {
        var recipe = new M3HashPinnedJavaSnapshotRecipe("m3-close");
        var context = new InMemoryExecutionContext(error -> { throw new IllegalStateException(error); });
        var changes = recipe.run(new InMemoryLargeSourceSet(List.of(before())), context, 1).getChangeset().getAllResults();
        assertEquals(1, changes.size());
        var after = changes.get(0).getAfter();
        assertNotNull(changes.get(0).getBefore()); assertNotNull(after);
        assertTrue(recipe.run(new InMemoryLargeSourceSet(List.of(after)), context, 1).getChangeset().getAllResults().isEmpty());
        Path generated = CRATE.resolve("target/close/generated");
        Path output = generated.resolve(TARGET); Files.createDirectories(output.getParent());
        Files.writeString(output, after.printAll());
        Path classes = CRATE.resolve("target/close/classes"); Files.createDirectories(classes);
        Path log = CRATE.resolve("target/close/compile.log");
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin/javac").toString(),
                "-source", "21", "-target", "21", "-Xlint:all", "-Werror", "-proc:none",
                "--patch-module", "java.base=" + generated.resolve("src/java.base/share/classes"),
                "-d", classes.toString(), output.toString()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if (!process.waitFor(60, TimeUnit.SECONDS)) { process.destroyForcibly(); fail("close compile timeout"); }
        assertEquals(0, process.exitValue(), Files.readString(log));
    }

    @Test void absentOrDriftedContractRefused() throws Exception {
        var recipe = new M3HashPinnedJavaSnapshotRecipe("m3-close");
        var context = new InMemoryExecutionContext(error -> { throw new IllegalStateException(error); });
        assertThrows(IllegalStateException.class, () -> recipe.run(new InMemoryLargeSourceSet(List.of()), context, 1));
        var drift = JavaParser.fromJavaVersion().build().parse("interface Changed {}").findFirst().orElseThrow()
                .withSourcePath(Path.of(TARGET));
        assertThrows(RuntimeException.class, () -> recipe.run(new InMemoryLargeSourceSet(List.of(drift)), context, 1));
    }
}
