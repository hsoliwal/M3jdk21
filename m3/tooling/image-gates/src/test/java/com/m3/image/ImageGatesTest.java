// SPDX-License-Identifier: Apache-2.0
package com.m3.image;

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

final class ImageGatesTest {
    private static final Path CRATE = Path.of(System.getProperty("m3.crate"));
    private static final Path RES = CRATE.resolve("src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-image-gates");

    private static List<SourceFile> before() throws Exception {
        var files = new ArrayList<SourceFile>();
        for (String line : Files.readAllLines(RES.resolve("manifest.tsv"))) {
            if (line.startsWith("#") || line.isBlank()) continue;
            String[] row = line.split("\t");
            if (row[1].equals("ABSENT")) continue;
            files.add(PlainText.builder().sourcePath(Path.of(row[0]))
                    .text(Files.readString(RES.resolve(row[3]+".before"))).build());
        }
        return files;
    }

    private static List<SourceFile> apply(List<SourceFile> files) {
        var context = new InMemoryExecutionContext(error -> { throw new IllegalStateException(error); });
        return new M3Jdk21HashPinnedTextSnapshotRecipe("m3-image-gates")
                .run(new InMemoryLargeSourceSet(files), context, 1)
                .getChangeset().getAllResults().stream().map(r -> r.getAfter()).toList();
    }

    @Test void exactBytesAndFixedPoint() throws Exception {
        var after = apply(before());
        assertEquals(29, after.size());
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

    @Test void capturedCompilerEvidenceAndRefusals() throws Exception {
        Path script = CRATE.resolve("src/test/python/test_log.py");
        Path checker = CRATE.resolve("src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-image-gates/check-log.py.txt");
        Process process = new ProcessBuilder("python3",script.toString(),checker.toString())
                .redirectErrorStream(true).redirectOutput(CRATE.resolve("target/compiler-evidence.log").toFile()).start();
        assertTrue(process.waitFor(30,java.util.concurrent.TimeUnit.SECONDS));
        assertEquals(0,process.exitValue(),Files.readString(CRATE.resolve("target/compiler-evidence.log")));
    }

    @Test void generatedRegexMatrixCompilesAndExecutes() throws Exception {
        exactBytesAndFixedPoint();
        Path directory=CRATE.resolve("target/matrix-probe"); Files.createDirectories(directory);
        Path probe=directory.resolve("MatrixProbe.java");
        Files.writeString(probe,"""
                package com.m3.a3;
                public class MatrixProbe {
                    public static void main(String[] args) {
                        var first=A3RegexMatrix.evaluate("return a;");
                        var same=A3RegexMatrix.evaluate("return a;");
                        var changed=A3RegexMatrix.evaluate("class Different {}");
                        if(first.patternCount()!=16 || first.subjectCount()!=17
                                || !first.equals(same) || first.root().equals(changed.root())
                                || first.matchedCells()==0 || first.matchedCells()>272)
                            throw new AssertionError("regex matrix contract");
                        System.out.println("MATRIX_PASS patterns=16 subjects=17 cells=272");
                    }
                }
                """);
        Path javaHome=Path.of(System.getProperty("java.home"));
        String source="m3/tooling/a3/src/main/java/com/m3/a3/A3RegexMatrix.java";
        Process compile=new ProcessBuilder(javaHome.resolve("bin/javac").toString(),"--release","21",
                "-Xlint:all","-Werror","-d",directory.toString(),
                CRATE.resolve("target/generated").resolve(source).toString(),probe.toString())
                .redirectErrorStream(true).redirectOutput(directory.resolve("compile.log").toFile()).start();
        assertTrue(compile.waitFor(30,java.util.concurrent.TimeUnit.SECONDS));
        assertEquals(0,compile.exitValue(),Files.readString(directory.resolve("compile.log")));
        Process run=new ProcessBuilder(javaHome.resolve("bin/java").toString(),"-cp",directory.toString(),"com.m3.a3.MatrixProbe")
                .redirectErrorStream(true).redirectOutput(directory.resolve("run.log").toFile()).start();
        assertTrue(run.waitFor(30,java.util.concurrent.TimeUnit.SECONDS));
        assertEquals(0,run.exitValue(),Files.readString(directory.resolve("run.log")));
    }

    @Test void retainsUnrelatedFiles() throws Exception {
        var files = before();
        files.add(PlainText.builder().sourcePath(Path.of("unrelated.txt")).text("retained\n").build());
        assertEquals(29, apply(files).size());
        assertTrue(apply(files).stream().noneMatch(f -> f.getSourcePath().toString().equals("unrelated.txt")));
    }
}
