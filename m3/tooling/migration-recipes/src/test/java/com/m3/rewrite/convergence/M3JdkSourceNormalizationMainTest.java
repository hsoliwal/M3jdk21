// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.convergence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class M3JdkSourceNormalizationMainTest {
    @TempDir
    Path temp;

    @Test
    void baselineAndDonorUseSameEngineButWriteTypedIdentityReceipts() throws Exception {
        Path root = temp.resolve("jdk");
        Path source = root.resolve("src/java.base/share/classes/p/A.java");
        Files.createDirectories(source.getParent());
        String java = "package p; final class A { private static int value(int a,int b){ return a+b; } }";
        Files.writeString(source, java);

        Path baselineOut = temp.resolve("baseline");
        var baseline = M3JdkSourceNormalizationMain.normalize(
                M3JdkSourceNormalizationMain.ImageRole.BASELINE,
                "jdk-21+35",
                root,
                baselineOut,
                1);
        assertEquals(1, baseline.files());
        assertTrue(Files.readString(baselineOut.resolve(M3JdkSourceNormalizationMain.IDENTITY))
                .contains("BASELINE\tjdk-21+35\t" + baseline.root()));

        Path donorOut = temp.resolve("donor");
        var donor = M3JdkSourceNormalizationMain.normalize(
                M3JdkSourceNormalizationMain.ImageRole.DONOR,
                "jdk-22+36",
                root,
                donorOut,
                1);
        assertEquals(baseline.root(), donor.root());
        assertTrue(Files.readString(donorOut.resolve(M3JdkSourceNormalizationMain.IDENTITY))
                .contains("DONOR\tjdk-22+36\t" + donor.root()));

        assertEquals(java, Files.readString(source), "normalization may not mutate source");
    }

    @Test
    void invalidRoleRevisionAndThreadsFailClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> M3JdkSourceNormalizationMain.main(
                        new String[] {"UNKNOWN", "x", temp.toString(), temp.resolve("o").toString()}));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3JdkSourceNormalizationMain.normalize(
                        M3JdkSourceNormalizationMain.ImageRole.DONOR,
                        "bad\tref",
                        temp,
                        temp.resolve("o2"),
                        1));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3JdkSourceNormalizationMain.normalize(
                        M3JdkSourceNormalizationMain.ImageRole.DONOR,
                        "jdk-22+36",
                        temp,
                        temp.resolve("o3"),
                        0));
    }
}
