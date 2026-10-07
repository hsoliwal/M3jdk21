// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class M3BackportPacketBatchMainTest {
    @TempDir Path temp;

    @Test
    void validatesMultiplePacketsThroughCanonicalComposer() throws Exception {
        Files.writeString(
                temp.resolve("jep-423.tsv"),
                M3BackportPacketLoader.HEADER
                        + "\n"
                        + "jep-423\tinventory\tMULTI_MODULE\ttrue\tPROOF:JEP-423:INVENTORY\t\n"
                        + "jep-423\trecipe\tMULTI_MODULE\ttrue\tm3/backports/recipes/jep-423-region-pinning\tinventory\n"
                        + "jep-423\tcompile\tMULTI_MODULE\ttrue\tPROOF:JEP-423:COMPILE\trecipe\n"
                        + "jep-423\ttest\tMULTI_MODULE\ttrue\tPROOF:JEP-423:TEST\tcompile\n"
                        + "jep-423\truntime-parity\tMULTI_MODULE\ttrue\tPROOF:JEP-423:RUNTIME_PARITY\ttest\n"
                        + "jep-423\tfixed-point\tMULTI_MODULE\ttrue\tPROOF:JEP-423:FIXED_POINT\truntime-parity\n");
        Files.writeString(
                temp.resolve("jdk-8357439.tsv"),
                M3BackportPacketLoader.HEADER
                        + "\n"
                        + "jdk-8357439\tinventory\tMODULE\ttrue\tPROOF:JDK-8357439:INVENTORY\t\n"
                        + "jdk-8357439\trecipe\tMODULE\ttrue\tm3/backports/recipes/jdk-8357439\tinventory\n"
                        + "jdk-8357439\tcompile\tMODULE\ttrue\tPROOF:JDK-8357439:COMPILE\trecipe\n"
                        + "jdk-8357439\ttest\tMODULE\ttrue\tPROOF:JDK-8357439:TEST\tcompile\n"
                        + "jdk-8357439\tfixed-point\tMODULE\ttrue\tPROOF:JDK-8357439:FIXED_POINT\ttest\n");

        M3BackportPacketBatchMain.main(new String[] {temp.toString()});
    }

    @Test
    void rejectsMissingOrEmptyPacketDirectory() throws Exception {
        assertThrows(
                IllegalArgumentException.class,
                () -> M3BackportPacketBatchMain.main(new String[] {temp.resolve("missing").toString()}));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3BackportPacketBatchMain.main(new String[] {temp.toString()}));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3BackportPacketBatchMain.main(new String[0]));
    }
}
