// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Executes retained first-party observations against the installed receiver. */
final class M3WrapperReconciliationContractsTest {
    @Test void recoveredMapCompositionContracts() throws Exception {
        com.m3.collections.M3FilteredWrapperSelfTest.main(new String[0]);
    }

    @Test void allNineTypedMapFactoriesAreAvailable() throws Exception {
        M3FilteredMapApiProof.main(new String[]{"present"});
    }

    @Test void mapSizeSaturatesAndStillPropagatesLatePredicateFailure() {
        M3FilteredMapSizeProof.main(new String[0]);
    }

    @Test void laterProjectionContractIsRetained() {
        com.m3.collections.M3FilteredMapsCursorProof.main(new String[0]);
    }

    @Test void exactBaselineCallbackTapeIsPreserved() throws Exception {
        var bytes = new ByteArrayOutputStream();
        PrintStream original = System.out;
        try (var capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            QueryReadProof.main(new String[0]);
        } finally {
            System.setOut(original);
        }
        assertEquals("QUERY_TRACE sha256=3f88ccf87b350d935459e21f0697e8d5f4e0532a65560b3ad3cae40f56338c24 events_chars=252336",
                bytes.toString(StandardCharsets.UTF_8).strip());
    }
}
