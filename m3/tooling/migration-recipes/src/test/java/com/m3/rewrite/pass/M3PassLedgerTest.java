// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.pass;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.indexdb.M3IndexDB;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class M3PassLedgerTest {
    private static final String ZERO = "0".repeat(64);
    private static final String ONE = "1".repeat(64);
    private static final String TWO = "2".repeat(64);
    private static final String THREE = "3".repeat(64);
    private static final String FOUR = "4".repeat(64);

    @Test
    void fileSemanticPassesAdvanceSeriallyBeforeFixedPointPromotion() {
        M3PassLedger ledger = M3PassLedger.empty();
        assertEquals(0, ledger.nextPassOrdinal());
        assertEquals(0, ledger.nextIteration());
        assertEquals(ZERO, ledger.currentStateRoot());
        assertFalse(ledger.complete());

        ledger = ledger.append(receipt(0, 0, "inventory", ZERO, ZERO, false, true));
        assertEquals(1, ledger.nextPassOrdinal());

        ledger = ledger.append(receipt(1, 0, "file-atomization", ZERO, ONE, true, true));
        assertEquals(2, ledger.nextPassOrdinal());
        assertEquals(ONE, ledger.currentStateRoot());

        ledger = ledger.append(receipt(2, 0, "file-patternization", ONE, TWO, true, true));
        assertEquals(3, ledger.nextPassOrdinal());
        assertEquals(TWO, ledger.currentStateRoot());

        ledger = ledger.append(receipt(3, 0, "file-documentation", TWO, THREE, true, true));
        assertEquals(4, ledger.nextPassOrdinal());
        assertEquals(THREE, ledger.currentStateRoot());

        ledger = ledger.append(receipt(4, 0, "file-fixed-point", THREE, THREE, false, true));
        assertEquals(5, ledger.nextPassOrdinal());
        assertEquals(0, ledger.nextIteration());
        assertEquals(THREE, ledger.currentStateRoot());
    }

    @Test
    void ledgerRejectsPassJumpIterationJumpWrongIdAndStaleInput() {
        M3PassLedger ledger = M3PassLedger.empty();

        assertThrows(
                IllegalArgumentException.class,
                () -> ledger.append(receipt(1, 0, "file-atomization", ZERO, ONE, true, false)));
        assertThrows(
                IllegalArgumentException.class,
                () -> ledger.append(receipt(0, 1, "inventory", ZERO, ZERO, false, true)));
        assertThrows(
                IllegalArgumentException.class,
                () -> ledger.append(receipt(0, 0, "wrong", ZERO, ZERO, false, true)));

        M3PassLedger afterInventory =
                ledger.append(receipt(0, 0, "inventory", ZERO, ZERO, false, true));
        assertThrows(
                IllegalArgumentException.class,
                () -> afterInventory.append(
                        receipt(1, 0, "file-atomization", TWO, THREE, true, false)));
    }

    @Test
    void receiptContractRejectsFalseClaims() {
        assertThrows(
                IllegalArgumentException.class,
                () -> M3PassReceipt.create(
                        0,
                        0,
                        "inventory",
                        ZERO,
                        ONE,
                        TWO,
                        THREE,
                        M3PassReceipt.Status.PASSED,
                        false,
                        true));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3PassReceipt.create(
                        0,
                        0,
                        "inventory",
                        ZERO,
                        ZERO,
                        TWO,
                        THREE,
                        M3PassReceipt.Status.FAILED,
                        false,
                        true));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3PassReceipt(
                        0,
                        0,
                        "inventory",
                        ZERO,
                        ZERO,
                        TWO,
                        THREE,
                        M3PassReceipt.Status.PASSED,
                        false,
                        true,
                        FOUR));
    }

    @Test
    void blockedOrFailedPassCannotAdvanceToNextPass() {
        M3PassLedger base =
                M3PassLedger.empty()
                        .append(receipt(0, 0, "inventory", ZERO, ZERO, false, true));

        M3PassReceipt blocked = M3PassReceipt.create(
                1,
                0,
                "file-atomization",
                ZERO,
                ZERO,
                TWO,
                THREE,
                M3PassReceipt.Status.BLOCKED,
                false,
                false);
        M3PassLedger blockedLedger = base.append(blocked);
        assertEquals(1, blockedLedger.nextPassOrdinal());
        assertEquals(1, blockedLedger.nextIteration());

        M3PassReceipt failed = M3PassReceipt.create(
                1,
                0,
                "file-atomization",
                ZERO,
                ZERO,
                TWO,
                THREE,
                M3PassReceipt.Status.FAILED,
                false,
                false);
        M3PassLedger failedLedger = base.append(failed);
        assertEquals(1, failedLedger.nextPassOrdinal());
        assertEquals(1, failedLedger.nextIteration());
    }

    @Test
    void completedLedgerRoundTripsAndReopensFromM3IndexDb() throws Exception {
        M3PassLedger ledger = completeLedger();
        assertTrue(ledger.complete());
        assertEquals(11, ledger.nextPassOrdinal());

        byte[] bytes = ledger.encode();
        M3PassLedger decoded = M3PassLedger.decode(bytes);
        assertArrayEquals(bytes, decoded.encode());
        assertEquals(ledger.receipts(), decoded.receipts());
        assertTrue(decoded.complete());

        Path root = Files.createTempDirectory("m3-pass-ledger-");
        try (M3IndexDB db = M3IndexDB.open(root)) {
            ledger.store(db, "multipass");
            M3PassLedger loaded = M3PassLedger.load(db, "multipass");
            assertEquals(ledger.receipts(), loaded.receipts());
            assertTrue(loaded.complete());
            assertThrows(
                    IllegalStateException.class,
                    () -> loaded.append(receipt(
                            10, 1, "proof", loaded.currentStateRoot(),
                            loaded.currentStateRoot(), false, true)));
        }

        try (M3IndexDB reopened = M3IndexDB.open(root)) {
            assertTrue(M3PassLedger.load(reopened, "multipass").complete());
        }
    }

    @Test
    void decoderAndArtifactContractFailClosed() throws Exception {
        assertThrows(
                IllegalArgumentException.class,
                () -> M3PassLedger.decode(new byte[] {1, 2, 3}));

        M3PassLedger ledger = M3PassLedger.empty();
        byte[] encoded = ledger.encode();
        byte[] trailing = java.util.Arrays.copyOf(encoded, encoded.length + 1);
        assertThrows(IllegalArgumentException.class, () -> M3PassLedger.decode(trailing));

        Path root = Files.createTempDirectory("m3-pass-wrong-");
        try (M3IndexDB db = M3IndexDB.open(root)) {
            db.putArtifact("wrong", "other", 1, encoded);
            assertThrows(java.io.IOException.class, () -> M3PassLedger.load(db, "wrong"));
        }
    }

    private static M3PassLedger completeLedger() {
        M3PassLedger ledger = M3PassLedger.empty();
        String state = ZERO;
        for (M3MultiPassPlan.Pass pass : M3MultiPassPlan.canonical().passes()) {
            boolean changed = pass.ordinal() >= 1 && pass.ordinal() <= 3;
            String output =
                    switch (pass.ordinal()) {
                        case 1 -> ONE;
                        case 2 -> TWO;
                        case 3 -> THREE;
                        default -> state;
                    };
            ledger = ledger.append(M3PassReceipt.create(
                    pass.ordinal(),
                    0,
                    pass.passId(),
                    state,
                    output,
                    TWO,
                    THREE,
                    M3PassReceipt.Status.PASSED,
                    changed,
                    true));
            state = output;
        }
        return ledger;
    }

    private static M3PassReceipt receipt(
            int pass,
            int iteration,
            String passId,
            String input,
            String output,
            boolean changed,
            boolean stop) {
        return M3PassReceipt.create(
                pass,
                iteration,
                passId,
                input,
                output,
                TWO,
                THREE,
                M3PassReceipt.Status.PASSED,
                changed,
                stop);
    }
}
