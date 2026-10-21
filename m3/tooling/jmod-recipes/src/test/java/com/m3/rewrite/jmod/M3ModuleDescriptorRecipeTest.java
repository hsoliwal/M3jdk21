// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package com.m3.rewrite.jmod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class M3ModuleDescriptorRecipeTest {

    private static final String DESCRIPTOR = "module demo.lib {\n    exports demo.lib;\n}\n";

    private static Path manifest(Path dir, String content, String pin) throws Exception {
        Path generated = dir.resolve("generated/demo.lib/module-info.java");
        Files.createDirectories(generated.getParent());
        Files.writeString(generated, content, StandardCharsets.UTF_8);
        Path m = dir.resolve("DESCRIPTOR_MANIFEST.tsv");
        Files.writeString(m, "module\tartifacts\tunit_jars\ttarget\tsha256\tgenerated\n"
                + "demo.lib\tdemo-lib\tdemo-lib.jar\tm3/vendor/third_party-m3/descriptors/demo.lib/module-info.java\t" + pin + "\t"
                + generated + "\n", StandardCharsets.UTF_8);
        return m;
    }

    @Test
    void addsTheDescriptorThenReachesAFixedPoint(@TempDir Path dir) throws Exception {
        Path repo = Files.createDirectories(dir.resolve("repo"));
        Path m = manifest(dir, DESCRIPTOR, M3ModuleDescriptorRecipe.sha256(DESCRIPTOR));
        assertEquals(0, M3ModuleDescriptorApply.apply(repo, m, dir.resolve("r1.tsv")));
        Path target = repo.resolve("m3/vendor/third_party-m3/descriptors/demo.lib/module-info.java");
        assertEquals(DESCRIPTOR, Files.readString(target, StandardCharsets.UTF_8));
        String receipt = Files.readString(dir.resolve("r1.tsv"), StandardCharsets.UTF_8);
        assertTrue(receipt.contains("ABSENT"), receipt);
        assertTrue(receipt.contains("FIXED_POINT\ttrue\tfirst_run_changes=1\tsecond_run_changes=0"), receipt);
        assertEquals(0, M3ModuleDescriptorApply.apply(repo, m, dir.resolve("r2.tsv")));
        assertTrue(Files.readString(dir.resolve("r2.tsv"), StandardCharsets.UTF_8)
                .contains("FIXED_POINT\ttrue\tfirst_run_changes=0\tsecond_run_changes=0"));
    }

    @Test
    void refusesToOverwriteADriftedDescriptor(@TempDir Path dir) throws Exception {
        Path repo = Files.createDirectories(dir.resolve("repo"));
        Path target = repo.resolve("m3/vendor/third_party-m3/descriptors/demo.lib/module-info.java");
        Files.createDirectories(target.getParent());
        Files.writeString(target, "module demo.lib { }\n", StandardCharsets.UTF_8);
        Path m = manifest(dir, DESCRIPTOR, M3ModuleDescriptorRecipe.sha256(DESCRIPTOR));
        assertEquals(3, M3ModuleDescriptorApply.apply(repo, m, dir.resolve("r.tsv")));
        assertEquals("module demo.lib { }\n", Files.readString(target, StandardCharsets.UTF_8));
    }

    @Test
    void refusesATamperedGeneratedDescriptor(@TempDir Path dir) throws Exception {
        Path repo = Files.createDirectories(dir.resolve("repo"));
        Path m = manifest(dir, DESCRIPTOR + "// tampered\n", M3ModuleDescriptorRecipe.sha256(DESCRIPTOR));
        assertThrows(IllegalStateException.class, () -> M3ModuleDescriptorApply.apply(repo, m, dir.resolve("r.tsv")));
        assertTrue(Files.notExists(repo.resolve("m3")));
    }
    @Test
    void driftRefusesTheWholePacketBeforeCreatingAnySibling(@TempDir Path dir) throws Exception {
        Path repo = Files.createDirectories(dir.resolve("repo"));
        Path drifted = repo.resolve("m3/vendor/third_party-m3/descriptors/demo.lib/module-info.java");
        Files.createDirectories(drifted.getParent());
        Files.writeString(drifted, "module demo.lib { }\n", StandardCharsets.UTF_8);
        Path m = manifest(dir, DESCRIPTOR, M3ModuleDescriptorRecipe.sha256(DESCRIPTOR));
        String sibling = "module demo.other { }\n";
        Path generated = dir.resolve("generated/demo.other/module-info.java");
        Files.createDirectories(generated.getParent());
        Files.writeString(generated, sibling, StandardCharsets.UTF_8);
        Files.writeString(m, Files.readString(m) + "demo.other\tdemo-other\tdemo-other.jar\t"
                + "m3/vendor/third_party-m3/descriptors/demo.other/module-info.java\t"
                + M3ModuleDescriptorRecipe.sha256(sibling) + "\t" + generated + "\n");
        assertEquals(3, M3ModuleDescriptorApply.apply(repo, m, dir.resolve("refusal.tsv")));
        assertEquals("module demo.lib { }\n", Files.readString(drifted));
        assertTrue(Files.notExists(repo.resolve("m3/vendor/third_party-m3/descriptors/demo.other")));
        assertTrue(Files.readString(dir.resolve("refusal.tsv")).contains("REFUSED\tdescriptor drift:"));
    }

}
