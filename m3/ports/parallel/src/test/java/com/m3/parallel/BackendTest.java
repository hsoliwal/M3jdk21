/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.parallel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.parallel.spi.Probe;
import com.m3.parallel.testing.TestKernels;

import java.util.Map;

import org.junit.jupiter.api.Test;

/** Discovery and selection, with the test providers of {@code META-INF/services} on the class path. */
final class BackendTest {

    @Test
    void referenceIsAlwaysPresentAndSelectable() {
        Backend reference = Backend.reference();
        assertEquals(Backend.REFERENCE_ID, reference.id());
        assertTrue(reference.isReference());
        assertTrue(reference.capabilities().contains(Backend.Capability.REFERENCE));
        assertEquals(0, reference.score());
        assertSame(reference, Backend.of("java"));
        assertSame(reference, Backend.select(Backend.Policy.REFERENCE));
        assertSame(reference, Backend.select(Backend.Policy.BEST), "negative-score providers never win BEST");
        assertTrue(reference.supports(TestKernels.ADD));
        assertFalse(reference.supports(TestKernels.MISSING));
        assertEquals("Backend[java]", reference.toString());
    }

    @Test
    void acceleratorPolicyPicksTheBestAvailableNonReferenceProvider() {
        Backend accelerator = Backend.select(Backend.Policy.ACCELERATOR);
        assertEquals("test-faulty", accelerator.id());
        assertFalse(accelerator.isReference());
        assertSame(accelerator, Backend.of("test-faulty"));
        assertEquals(-10, accelerator.score());
    }

    @Test
    void faultyProvidersAreRecordedNeverCrashDiscoveryAndNeverSelected() {
        Map<String, Probe> probes = Backend.probeAll();
        assertEquals(Probe.ready(), probes.get("java"));
        assertEquals(Probe.ready(), probes.get("test-faulty"));
        assertEquals("no driver", probes.get("test-absent").reason());
        assertTrue(probes.get("test-throwing").reason().contains("driver crashed"));
        assertTrue(probes.get("test-dup").reason().startsWith("ambiguous: 2 providers"));
        assertTrue(probes.get("invalid:com.m3.parallel.testing.TestProviders$BadId").reason().contains("invalid"));
        assertTrue(probes.get("invalid:com.m3.parallel.testing.TestProviders$Reserved").reason().contains("reserved"));
        assertTrue(probes.keySet().stream().anyMatch(key -> key.startsWith("unloadable:")),
                "missing provider class is recorded: " + probes.keySet());
        assertFalse(probes.get("test-absent").available());

        IllegalStateException absent = assertThrows(IllegalStateException.class, () -> Backend.of("test-absent"));
        assertTrue(absent.getMessage().contains("no driver"));
        assertThrows(IllegalStateException.class, () -> Backend.of("test-throwing"));
        assertThrows(IllegalStateException.class, () -> Backend.of("test-dup"));
        assertThrows(IllegalArgumentException.class, () -> Backend.of("cuda-not-installed"));
        assertThrows(UnsupportedOperationException.class, () -> probes.put("x", Probe.ready()));
    }
}
