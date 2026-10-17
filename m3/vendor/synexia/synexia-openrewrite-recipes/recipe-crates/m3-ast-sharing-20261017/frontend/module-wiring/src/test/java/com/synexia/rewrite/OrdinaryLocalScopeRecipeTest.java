// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;

import static org.junit.jupiter.api.Assertions.*;

class OrdinaryLocalScopeRecipeTest {
    private static final Path OWNER = Path.of("m3/ports/compiletime/src/main/java/com/m3/compiletime/M3GvasLiteralHarvester.java");
    private static final String RESOURCE = "/com/synexia/rewrite/hash-pinned-java/m3-ast-ordinary-local-scope-20261017/";

    private static Recipe recipe() throws Exception {
        var resource = OrdinaryLocalScopeRecipeTest.class.getResource("/META-INF/rewrite/m3-ast-ordinary-local-scope.yml");
        try (var stream = resource.openStream()) {
            return Environment.builder().load(new YamlResourceLoader(stream, resource.toURI(), new Properties()))
                    .build().activateRecipes("com.synexia.m3.ASTOrdinaryLocalScope20261017");
        }
    }

    @Test
    void actualTypedRenameIsSealedFixedPointAndRefusesForeignPreimages() throws Exception {
        String before = Files.readString(Path.of("local-scope-fix/preimages").resolve(OWNER));
        String after = Files.readString(Path.of("local-scope-fix/templates/M3GvasLiteralHarvester.java.txt"));
        assertEquals("e4d1a2bced4ca3f33c743aa4be386ae546f5266f68473fd6e0f3fbba68f335f4", WiringSupport.hash(before));
        assertEquals("0e4212f3adf1ca4c7bc7077d507810b42a342964b563f7ee81fdad469e11b865", WiringSupport.hash(after));
        assertEquals(before.replace("Path destination = out.toAbsolutePath().normalize();", "Path publishedDirectory = out.toAbsolutePath().normalize();")
                .replace("Files.createTempDirectory(destination.getParent(),", "Files.createTempDirectory(publishedDirectory.getParent(),")
                .replace("Files.move(staging, destination);", "Files.move(staging, publishedDirectory);"), after);
        var original = WiringSupport.parse(OWNER, before);
        var unrelated = WiringSupport.parse(Path.of("m3/ports/compiletime/src/main/java/com/m3/compiletime/Unrelated.java"),
                "package com.m3.compiletime; class Unrelated {}\n");
        var changes = WiringSupport.run(recipe(), List.of(original, unrelated));
        assertEquals(1, changes.size());
        assertEquals(OWNER, changes.getFirst().getSourcePath());
        assertEquals(after, changes.getFirst().printAll());
        assertTrue(WiringSupport.run(recipe(), List.of(unrelated, changes.getFirst())).isEmpty());
        assertThrows(IllegalStateException.class, () -> WiringSupport.run(recipe(), List.of()));
        assertThrows(IllegalStateException.class, () -> WiringSupport.run(recipe(), List.of(original, original)));
        assertThrows(IllegalStateException.class, () -> WiringSupport.run(recipe(),
                List.of(WiringSupport.parse(OWNER, before + "// foreign source drift\n"))));
        WiringSupport.write(Path.of("target/ordinary-local-scope"), changes.getFirst());

        String receiver = System.getProperty("m3.wiring.ordinary", "");
        if (!receiver.isBlank()) {
            Path root = Path.of(receiver);
            var current = WiringSupport.parse(OWNER, Files.readString(root.resolve(OWNER)));
            var emitted = WiringSupport.run(recipe(), List.of(current));
            var admitted = WiringSupport.merge(List.of(current), emitted);
            assertEquals(1, admitted.size());
            assertEquals(after, admitted.getFirst().printAll());
            assertTrue(WiringSupport.run(recipe(), admitted).isEmpty());
            for (var source : emitted) WiringSupport.write(root, source);
            assertEquals(after, Files.readString(root.resolve(OWNER)));
        }
        Files.writeString(Path.of("target/ORDINARY_LOCAL_SCOPE_SDK_RECEIPT.json"),
                "{\"status\":\"PASSED\",\"recipe_results\":1,\"fixed_point_results\":0,\"target_postimage_sha256\":\""
                        + WiringSupport.hash(after) + "\"}\n");
    }

    @Test
    void templateDriftIsRejected() throws Exception {
        Path template = Path.of(getClass().getResource(RESOURCE + "M3GvasLiteralHarvester.java.txt").toURI());
        byte[] original = Files.readAllBytes(template);
        try {
            Files.writeString(template, "package com.m3.compiletime; final class Wrong {}\n");
            assertThrows(IllegalStateException.class, () -> WiringSupport.run(recipe(), List.of()));
        } finally {
            Files.write(template, original);
        }
    }
}
