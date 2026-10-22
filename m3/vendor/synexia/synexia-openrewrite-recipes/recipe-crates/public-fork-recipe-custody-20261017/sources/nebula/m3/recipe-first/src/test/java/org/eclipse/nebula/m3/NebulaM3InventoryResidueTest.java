// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NebulaM3InventoryResidueTest {
    @Test void malformedSourceIsAccountedForAndLaterFilesStillParse(@TempDir Path temp) throws Exception {
        Path root = Files.createDirectory(temp.resolve("repo"));
        byte[] malformed = {(byte) 0xc3, (byte) 0x28};
        Path invalid = root.resolve("AInvalid.java");
        Files.write(invalid, malformed);
        Path valid = root.resolve("ZValid.java");
        byte[] source = "public class ZValid { public int answer() { return 42; } }\n".getBytes(StandardCharsets.UTF_8);
        Files.write(valid, source);
        for (String excluded : new String[]{"m3/Excluded.java", "target/Excluded.java", ".git/Excluded.java"}) {
            Path file = root.resolve(excluded);
            Files.createDirectories(file.getParent());
            Files.write(file, malformed);
        }
        Path first = temp.resolve("first"), second = temp.resolve("second");
        var summary = NebulaM3InventoryCli.run(root, first);
        assertEquals(2, summary.javaFiles());
        assertEquals(1, summary.parsedCompilationUnits());
        assertEquals(1, summary.failureRows());
        assertEquals(summary, NebulaM3InventoryCli.run(root, second));
        assertEquals("sourcePath\treason\nAInvalid.java\tMalformedInputException\n",
                Files.readString(first.resolve("nebula-m3-parse-failures.tsv")));
        assertTrue(Files.readString(first.resolve("nebula-m3-java-inventory.tsv")).contains("ZValid.java"));
        for (String artifact : new String[]{"nebula-m3-java-inventory.tsv", "nebula-m3-parse-failures.tsv", "nebula-m3-summary.tsv"}) {
            assertArrayEquals(Files.readAllBytes(first.resolve(artifact)), Files.readAllBytes(second.resolve(artifact)));
        }
        assertArrayEquals(malformed, Files.readAllBytes(invalid));
        assertArrayEquals(source, Files.readAllBytes(valid));
    }
}
