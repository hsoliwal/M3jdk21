// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.chrome;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jdk.m3.llm.chrome.aix.AiXCompletionBoundarySelfTest;
import jdk.m3.llm.chrome.aix.AiXReverseSourceCoverage;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/** Executes the received donor oracles through the product's ordinary Maven test phase. */
class ReceivedContractTest {
    @Test void advisor() throws Exception { ChromeMlContextAdvisorSelfTest.main(new String[0]); }
    @Test void indexedEvidence() throws Exception { ChromeMlReferenceSelectionSelfTest.main(new String[0]); }
    @Test void capturedCounts() throws Exception { ChromeMlEvidenceCountProbe.main(new String[0]); }
    @Test void typedCaller() throws Exception { ChromeMlTypedSelectionExample.main(new String[0]); }
    @Test void completeStatus() throws Exception { AiXCompletionBoundarySelfTest.main(new String[0]); }

    @Test @EnabledOnOs(OS.LINUX)
    void realProcessLifecycleWithSyntheticChild() throws Exception {
        ChromeMlProcessLifecycleTestMain.main(new String[0]);
    }

    @Test void legacyPacketAndPromptIdentity() throws Exception {
        assertEquals("fa360176781d2fa725cbdfb4ad87e8fd8e68bea88c9a84af862d829906628bec",
                stdoutHash(() -> ChromeMlCompatibilityProbe.main(new String[0])));
    }

    @Test void successfulTreeIdentity() throws Exception {
        assertEquals("56ac5322617124dbce38c2f290f7f86ef169146ed6fe476d5ec12ffc87b15efc",
                stdoutHash(() -> AiXCompletionBoundarySelfTest.main(new String[]{"compatibility"})));
    }

    @Test void exactSourceReconstruction() throws Exception {
        Path file = Files.createTempFile("m3-llm-source-", ".txt");
        try {
            Files.writeString(file, "MUST retain every source range. हिन्दी 界 😀\r\n".repeat(137));
            AiXReverseSourceCoverage.main(new String[]{file.toString()});
            ChromeMlCorpusReferenceProbe.main(new String[]{file.toString()});
        } finally { Files.deleteIfExists(file); }
    }

    @FunctionalInterface private interface Checked { void run() throws Exception; }

    private static String stdoutHash(Checked operation) throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            operation.run();
        } finally { System.setOut(original); }
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toString(StandardCharsets.UTF_8).replace("\r\n", "\n").getBytes(StandardCharsets.UTF_8)));
    }
}
