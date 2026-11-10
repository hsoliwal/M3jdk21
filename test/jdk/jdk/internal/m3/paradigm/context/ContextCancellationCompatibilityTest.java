// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.paradigm.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.Test;

class ContextCancellationCompatibilityTest {

    @Test
    void monitorCancellationSupportsBothExistingCatchFamilies() {
        AtomicProgressMonitor monitor = new AtomicProgressMonitor();
        monitor.cancel();

        CancellationException failure = assertThrows(
                CancellationException.class, monitor::checkCancelled);
        assertInstanceOf(CancelledException.class, failure);
        assertEquals("Context operation cancelled", failure.getMessage());
        assertTrue(monitor.isCancelled());
    }

    @Test
    void threadInterruptionRetainsItsFlagAndLegacyContextSignal() {
        Thread.currentThread().interrupt();
        try {
            CancellationException failure = assertThrows(
                    CancellationException.class, ContextProgressMonitor.NONE::checkCancelled);
            assertInstanceOf(CancelledException.class, failure);
            assertEquals("Context operation cancelled", failure.getMessage());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void explicitlyConstructedContextSignalKeepsItsMessageAndRuntimeCatchCompatibility() {
        CancelledException failure = new CancelledException();
        assertEquals("context execution cancelled", failure.getMessage());
        assertInstanceOf(RuntimeException.class, failure);
        assertInstanceOf(CancellationException.class, failure);
    }
}
