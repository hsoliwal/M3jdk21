// SPDX-License-Identifier: Apache-2.0
package com.m3.cb;

import static org.junit.jupiter.api.Assertions.*;
import com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe;
import com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

/** Native text replay is followed by the actual HotSpot build, not a Java-parser claim. */
final class NativeTest {
    private static final Path CRATE = Path.of(System.getProperty("m3.crate"));
    private static final Path TARGET = Path.of("src/hotspot/share/prims/jni.cpp");

    @Test void nativeReplayFixedPointAndUnrelatedInput() throws Exception {
        var recipe = new M3Jdk21HashPinnedTextSnapshotRecipe("m3-jni");
        var context = new InMemoryExecutionContext(error -> { throw new IllegalStateException(error); });
        var before = PlainText.builder().sourcePath(TARGET).text(Files.readString(CRATE.resolve("jni/jni.cpp.before"))).build();
        var unrelated = PlainText.builder().sourcePath(Path.of("unrelated.txt")).text("keep").build();
        var changes = recipe.run(new InMemoryLargeSourceSet(List.of(before, unrelated)), context, 1).getChangeset().getAllResults();
        assertEquals(2, changes.size());
        var after = new java.util.ArrayList<org.openrewrite.SourceFile>();
        for (var change : changes) {
            var file = change.getAfter(); assertNotNull(file); after.add(file);
            Path output = CRATE.resolve("target/jni/generated").resolve(file.getSourcePath());
            Files.createDirectories(output.getParent()); Files.writeString(output, file.printAll());
        }
        after.add(unrelated);
        assertTrue(recipe.run(new InMemoryLargeSourceSet(after), context, 1).getChangeset().getAllResults().isEmpty());
        var javaRecipe = new M3HashPinnedJavaSnapshotRecipe("m3-jni-null");
        var javaChanges = javaRecipe.run(new InMemoryLargeSourceSet(List.of()), context, 1).getChangeset().getAllResults();
        assertEquals(1, javaChanges.size());
        var probe = javaChanges.get(0).getAfter(); assertNotNull(probe);
        assertTrue(javaRecipe.run(new InMemoryLargeSourceSet(List.of(probe)), context, 1).getChangeset().getAllResults().isEmpty());
        Path output = CRATE.resolve("target/jni/generated").resolve(probe.getSourcePath());
        Files.createDirectories(output.getParent()); Files.writeString(output, probe.printAll());
    }

    @Test void nativeMissingDriftOrJavaParserRefused() {
        var recipe = new M3Jdk21HashPinnedTextSnapshotRecipe("m3-jni");
        var context = new InMemoryExecutionContext(error -> { throw new IllegalStateException(error); });
        assertThrows(IllegalStateException.class, () -> recipe.run(new InMemoryLargeSourceSet(List.of()), context, 1));
        var drift = PlainText.builder().sourcePath(TARGET).text("changed").build();
        assertThrows(RuntimeException.class, () -> recipe.run(new InMemoryLargeSourceSet(List.of(drift)), context, 1));
        var java = JavaParser.fromJavaVersion().build().parse("class WrongParser {}").findFirst().orElseThrow().withSourcePath(TARGET);
        assertThrows(RuntimeException.class, () -> recipe.run(new InMemoryLargeSourceSet(List.of(java)), context, 1));
    }
}
