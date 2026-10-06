// SPDX-License-Identifier: Apache-2.0
package com.m3.ci;

import static org.junit.jupiter.api.Assertions.*;

import com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

final class CiRepairTest {
    private static final Path CRATE = Path.of(System.getProperty("m3.crate"));
    private static final Path RES = CRATE.resolve("src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-ci-repair");

    private static List<SourceFile> before() throws Exception {
        var files = new ArrayList<SourceFile>();
        for (String line : Files.readAllLines(RES.resolve("manifest.tsv"))) {
            if (line.startsWith("#") || line.isBlank()) continue;
            String[] row = line.split("\t");
            files.add(PlainText.builder().sourcePath(Path.of(row[0]))
                    .text(Files.readString(RES.resolve(row[3]+".before"))).build());
        }
        return files;
    }

    private static List<SourceFile> apply(List<SourceFile> files) {
        var context = new InMemoryExecutionContext(error -> { throw new IllegalStateException(error); });
        return new M3Jdk21HashPinnedTextSnapshotRecipe("m3-ci-repair")
                .run(new InMemoryLargeSourceSet(files), context, 1)
                .getChangeset().getAllResults().stream().map(r -> r.getAfter()).toList();
    }

    @Test void exactBytesAndFixedPoint() throws Exception {
        var after = apply(before());
        assertEquals(4, after.size());
        assertTrue(apply(after).isEmpty());
        for (String line : Files.readAllLines(RES.resolve("manifest.tsv"))) {
            if (line.startsWith("#") || line.isBlank()) continue;
            String[] row = line.split("\t");
            var file = after.stream().filter(f -> f.getSourcePath().toString().equals(row[0])).findFirst().orElseThrow();
            assertEquals(Files.readString(RES.resolve(row[3])), file.printAll());
            Path target = CRATE.resolve("target/generated").resolve(file.getSourcePath());
            Files.createDirectories(target.getParent());
            Files.writeString(target, file.printAll());
            assertArrayEquals(Files.readAllBytes(RES.resolve(row[3])), Files.readAllBytes(target));
        }
    }

    @Test void refusesDriftMissingAndDuplicate() throws Exception {
        var missing = before(); missing.removeFirst();
        assertThrows(RuntimeException.class, () -> apply(missing));
        var duplicate = before(); duplicate.add(duplicate.getFirst());
        assertThrows(RuntimeException.class, () -> apply(duplicate));
        var drift = before(); var file = drift.getFirst();
        drift.set(0, PlainText.builder().sourcePath(file.getSourcePath()).text(file.printAll()+"drift").build());
        assertThrows(RuntimeException.class, () -> apply(drift));
    }

    @Test void retainsUnrelatedFiles() throws Exception {
        var files = before();
        files.add(PlainText.builder().sourcePath(Path.of("unrelated.txt")).text("retained\n").build());
        assertEquals(4, apply(files).size());
        assertTrue(apply(files).stream().noneMatch(f -> f.getSourcePath().toString().equals("unrelated.txt")));
    }
}
