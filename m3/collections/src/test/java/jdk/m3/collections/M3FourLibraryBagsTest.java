/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.M3BagAlgebraProof;
import jdk.internal.m3.collections.M3BagMapFailureProof;
import jdk.internal.m3.collections.M3FourLibraryBagsReferenceModel;

/**
 * Owning-module JUnit test of the relocated packed long bag and map (recipe four-library-bags-20261106): the crate's
 * algebra proof, its two constrained-heap failure proofs in isolated JVMs (-Xmx32m rehash, -Xmx16m remove, as the
 * proof's own usage line requires), and a bounded run of the recipe's reference model.
 */
final class M3FourLibraryBagsTest {
    @Test void bagAlgebraProof() {
        M3BagAlgebraProof.main(new String[0]);
    }

    @Test void referenceModel() {
        M3FourLibraryBagsReferenceModel.main(new String[] {"100000", "20261106"});
    }

    @Test void rehashFailurePreservesLanes() throws Exception {
        assertTrue(isolated("-Xmx32m", "rehash").startsWith("PASS real rehash OOME"));
    }

    @Test void removalSurvivesFailedCompaction() throws Exception {
        assertTrue(isolated("-Xmx16m", "remove").startsWith("PASS successful removal survives"));
    }

    private static String isolated(String heap, String mode) throws IOException, InterruptedException {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        List<String> command = List.of(java, heap, "-cp", System.getProperty("java.class.path"),
                M3BagMapFailureProof.class.getName(), mode);
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        assertTrue(process.waitFor(120, TimeUnit.SECONDS), "isolated JVM timed out");
        assertEquals(0, process.exitValue(), output);
        return output;
    }
}
