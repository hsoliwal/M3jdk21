// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.rewrite;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
class EngineCustodyTest {
    @Test void existingEngineSourcesRemainByteExact() throws IOException {
        Path base = Path.of(System.getProperty("recipe.base"));
        assertEquals("c874b5d4b5fe8886d53763ab91d050f431c4b7c09d3fe34d58680231ccb1cf7e", M3GpuToolJoinManifest.sha256(Files.readString(base.resolve("src/main/java/jdk/internal/m3/rewrite/M3GpuToolJoinManifest.java"))));
        assertEquals("300a520ec9d273922d1edbdf5442e5faf825c47e8e3df7febf4baa2f39945401", M3GpuToolJoinManifest.sha256(Files.readString(base.resolve("src/main/java/jdk/internal/m3/rewrite/M3GpuToolJoinRecipe.java"))));
    }
}
