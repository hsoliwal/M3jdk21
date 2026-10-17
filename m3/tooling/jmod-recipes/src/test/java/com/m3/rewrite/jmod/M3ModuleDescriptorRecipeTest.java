// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.jmod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.io.CleanupMode;

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
    void refusesEveryEscapingTargetBeforeAnyWrite(@TempDir Path dir) throws Exception {
        Path repo = Files.createDirectories(dir.resolve("repo"));
        Path m = manifest(dir, DESCRIPTOR, M3ModuleDescriptorRecipe.sha256(DESCRIPTOR));
        String valid = Files.readString(m, StandardCharsets.UTF_8);
        String target = "m3/vendor/third_party-m3/descriptors/demo.lib/module-info.java";
        String row = valid.substring(valid.indexOf('\n') + 1);
        for (String unsafe : new String[] {"../escape.java", "/escape.java", "C:/escape.java",
                "m3/vendor/third_party-m3/descriptors/../module-info.java", ".git/config", "src/module-info.java"}) {
            Files.writeString(m, valid + row.replace(target, unsafe), StandardCharsets.UTF_8);
            Path receipt = dir.resolve("refused.tsv");
            assertThrows(IllegalArgumentException.class, () -> M3ModuleDescriptorApply.apply(repo, m, receipt));
            assertFalse(Files.exists(repo.resolve("m3")));
            assertFalse(Files.exists(receipt));
            assertFalse(Files.exists(dir.resolve("escape.java")));
        }
    }

    @Test
    void refusesOccupiedOrSourceReceiptWithoutWritingDescriptor(@TempDir Path dir) throws Exception {
        Path repo = Files.createDirectories(dir.resolve("repo"));
        Path m = manifest(dir, DESCRIPTOR, M3ModuleDescriptorRecipe.sha256(DESCRIPTOR));
        Path receipt = dir.resolve("occupied.tsv");
        Files.writeString(receipt, "unique proof", StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> M3ModuleDescriptorApply.apply(repo, m, receipt));
        assertEquals("unique proof", Files.readString(receipt));
        assertThrows(IllegalArgumentException.class, () -> M3ModuleDescriptorApply.apply(repo, m, repo.resolve("receipt.tsv")));
        assertFalse(Files.exists(repo.resolve("m3")));
    }

    @Test
    void refusesWindowsJunctionRootBeforeWriting(@TempDir(cleanup = CleanupMode.NEVER) Path dir) throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(System.getProperty("os.name").startsWith("Windows"));
        Path repo = Files.createDirectories(dir.resolve("repo"));
        Path link = dir.resolve("junction");
        Process process = new ProcessBuilder("cmd.exe", "/c", "mklink", "/J", link.toString(), repo.toString())
                .redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.waitFor(), output);
        Path m = manifest(dir, DESCRIPTOR, M3ModuleDescriptorRecipe.sha256(DESCRIPTOR));
        assertThrows(IllegalArgumentException.class, () -> M3ModuleDescriptorApply.apply(link, m, dir.resolve("r.tsv")));
        assertFalse(Files.exists(repo.resolve("m3")));
        assertFalse(Files.exists(dir.resolve("r.tsv")));
    }

    @Test
    void refusesWindowsJunctionInsideDescriptorLayerBeforeWriting(@TempDir(cleanup = CleanupMode.NEVER) Path dir) throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(System.getProperty("os.name").startsWith("Windows"));
        Path repo = Files.createDirectories(dir.resolve("repo"));
        Path outside = Files.createDirectories(dir.resolve("outside"));
        Path layer = Files.createDirectories(repo.resolve("m3/vendor/third_party-m3/descriptors"));
        Process process = new ProcessBuilder("cmd.exe", "/c", "mklink", "/J", layer.resolve("demo.lib").toString(), outside.toString())
                .redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.waitFor(), output);
        Path m = manifest(dir, DESCRIPTOR, M3ModuleDescriptorRecipe.sha256(DESCRIPTOR));
        assertThrows(IllegalArgumentException.class, () -> M3ModuleDescriptorApply.apply(repo, m, dir.resolve("r.tsv")));
        assertFalse(Files.exists(outside.resolve("module-info.java")));
        assertFalse(Files.exists(dir.resolve("r.tsv")));
    }

    @Test
    void refusesExcessDescriptorPayloadBeforeAnyOutput(@TempDir Path dir) throws Exception {
        Path repo = Files.createDirectories(dir.resolve("repo"));
        String oversized = " ".repeat(512 * 1024 + 1);
        Path m = manifest(dir, oversized, M3ModuleDescriptorRecipe.sha256(oversized));
        assertThrows(IllegalArgumentException.class, () -> M3ModuleDescriptorApply.apply(repo, m, dir.resolve("r.tsv")));
        assertFalse(Files.exists(repo.resolve("m3")));
        assertFalse(Files.exists(dir.resolve("r.tsv")));
    }

    @Test
    void chargesSmallFilesByMeasuredAllocationUnits() {
        assertEquals(1024 * 1024, M3ModuleDescriptorApply.fileAllocation(1, 1024 * 1024));
        assertEquals(8192, M3ModuleDescriptorApply.fileAllocation(4097, 4096));
        assertThrows(IllegalArgumentException.class, () -> M3ModuleDescriptorApply.fileAllocation(1, 0));
    }

    @Test
    void refusesAnOccupiedDirectoryTargetBeforeWritingEarlierValidRows(@TempDir Path dir) throws Exception {
        Path repo = Files.createDirectories(dir.resolve("repo"));
        Path m = manifest(dir, DESCRIPTOR, M3ModuleDescriptorRecipe.sha256(DESCRIPTOR));
        String first = Files.readString(m, StandardCharsets.UTF_8);
        String second = first.substring(first.indexOf('\n') + 1)
                .replace("descriptors/demo.lib/", "descriptors/occupied/");
        Files.writeString(m, first + second, StandardCharsets.UTF_8);
        Files.createDirectories(repo.resolve("m3/vendor/third_party-m3/descriptors/occupied/module-info.java"));
        assertThrows(IllegalArgumentException.class, () -> M3ModuleDescriptorApply.apply(repo, m, dir.resolve("r.tsv")));
        assertFalse(Files.exists(repo.resolve("m3/vendor/third_party-m3/descriptors/demo.lib")));
        assertFalse(Files.exists(dir.resolve("r.tsv")));
    }
}
