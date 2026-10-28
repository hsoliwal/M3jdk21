// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
class EngineCustodyTest {
    @Test void existingEngineSourcesRemainByteExact() throws IOException {
        Path base = Path.of(System.getProperty("recipe.base"));
        assertEquals("56bd61be29eae76b3f468519677424450448751d94ca032956b330d7b3dd1999", M3GpuToolJoinManifest.sha256(Files.readString(base.resolve("src/main/java/com/synexia/rewrite/M3GpuToolJoinManifest.java"))));
        assertEquals("ee90a2b8689699745553c2f4d2d07121b91777f4e9a671fdb33ba8edea0c14ec", M3GpuToolJoinManifest.sha256(Files.readString(base.resolve("src/main/java/com/synexia/rewrite/M3GpuToolJoinRecipe.java"))));
    }
}
