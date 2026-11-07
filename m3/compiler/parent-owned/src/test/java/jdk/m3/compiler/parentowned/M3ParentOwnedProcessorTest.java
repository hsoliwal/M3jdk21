// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler.parentowned;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;

//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/eaacff5931ef16a1a52425043bb71b821eda3515/synexia-primitives/src/test/java/com/synexia/primitives/M3ParentOwnedProcessorTest.java blob 3bb0d6528b7b; recipe m3/compiler/parent-owned/recipe/parent-owned-processor-20261107/relocate.py v1
class M3ParentOwnedProcessorTest {
    @Test void emitsDeterministicConservativeCompilerMetadata() throws Exception {
        Path root = Files.createTempDirectory("m3-parent-owned-proof-");
        Path source = root.resolve("Foo.java");
        String code = "package proof; import jdk.m3.collections.M3ParentOwned;"
                + "@M3ParentOwned(owner=\"tree\",flags=13) public class Foo {"
                + "@M3ParentOwned(owner=\"rows\") int bits;"
                + "@M3ParentOwned void work(long id) {} }";
        Files.writeString(source, code, StandardCharsets.UTF_8);
        Path output = root.resolve("classes");
        Files.createDirectories(output);
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler);
        int result = compiler.run(null, null, null, "--release", "21",
                "-classpath", System.getProperty("java.class.path"),
                "-processor", M3ParentOwnedProcessor.class.getName(),
                "-d", output.toString(), source.toString());
        assertEquals(0, result);
        String data = Files.readString(output.resolve("META-INF/m3/parent-owned-v1.tsv"));
        List<String> lines = data.lines().toList();
        assertEquals(4, lines.size());
        assertTrue(lines.get(0).contains("staticReferences"));
        assertTrue(lines.get(1).startsWith("proof.Foo\tCLASS\t13\ttree"));
        assertTrue(lines.get(2).contains("#bits\tFIELD\t"));
        assertTrue(lines.get(3).contains("#work(long)\tMETHOD\t"));
        assertTrue(lines.stream().skip(1).allMatch(s -> s.endsWith("UNKNOWN\tUNVERIFIED")));
    }
}
