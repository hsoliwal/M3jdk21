// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.collections.M3DefaultProgressMonitor;
import org.eclipse.core.runtime.IProgressMonitor;
import org.junit.jupiter.api.Test;

class M3EclipseProgressMonitorTest {
    @Test
    void ownedModePreservesEclipseLifecycleAndFractionalProgress() {
        M3DefaultProgressMonitor m3 = new M3DefaultProgressMonitor();
        IProgressMonitor eclipse = M3EclipseProgressMonitor.owned(m3);
        assertSame(m3, ((M3EclipseProgressMonitor) eclipse).owner());
        eclipse.beginTask("compile", 10);
        eclipse.internalWorked(0.25d);
        eclipse.internalWorked(0.75d);
        eclipse.worked(2);
        eclipse.subTask("bindings");
        assertEquals(3L, m3.state().worked());
        assertEquals("bindings", m3.state().subTaskName());
        eclipse.setTaskName("link");
        assertEquals("link", m3.state().taskName());
        eclipse.done();
        eclipse.done();
        eclipse.worked(2);
        assertEquals(3L, m3.state().worked());
        assertTrue(m3.state().done());
        assertThrows(IllegalStateException.class, () -> eclipse.beginTask("again", 10));
    }

    @Test
    void borrowedModeDoesNotChangeParentLifecycle() {
        M3DefaultProgressMonitor m3 = new M3DefaultProgressMonitor();
        m3.beginTask("outer", 20);
        IProgressMonitor eclipse = M3EclipseProgressMonitor.borrowed(m3);
        eclipse.beginTask("jdt", 10);
        eclipse.internalWorked(0.5d);
        eclipse.internalWorked(0.5d);
        eclipse.setTaskName("parser");
        eclipse.worked(2);
        eclipse.done();
        assertEquals("outer", m3.state().taskName());
        assertEquals("parser", m3.state().subTaskName());
        assertFalse(m3.state().done());
        assertEquals(3L, m3.state().worked());
        eclipse.setCanceled(true);
        assertTrue(eclipse.isCanceled());
        assertTrue(m3.isCanceled());
        eclipse.setCanceled(false);
        assertFalse(m3.isCanceled());
    }

    @Test
    void ignoresNonFiniteAndNegativeWorkAndKeepsUnknownTotal() {
        M3DefaultProgressMonitor m3 = new M3DefaultProgressMonitor();
        IProgressMonitor eclipse = M3EclipseProgressMonitor.owned(m3);
        eclipse.beginTask("unknown", IProgressMonitor.UNKNOWN);
        assertEquals(-1L, m3.state().totalWork());
        eclipse.internalWorked(Double.NaN);
        eclipse.internalWorked(Double.POSITIVE_INFINITY);
        eclipse.internalWorked(-12.0d);
        eclipse.worked(-5);
        assertEquals(0L, m3.state().worked());
        eclipse.internalWorked(0.125d);
        eclipse.internalWorked(0.875d);
        assertEquals(1L, m3.state().worked());
    }

    @Test
    void rejectsIllegalLifecycleAndDoesNotStartOnInvalidWork() {
        M3DefaultProgressMonitor m3 = new M3DefaultProgressMonitor();
        IProgressMonitor eclipse = M3EclipseProgressMonitor.owned(m3);
        assertThrows(IllegalArgumentException.class, () -> eclipse.beginTask("bad", -2));
        eclipse.beginTask("good", 1);
        assertEquals("good", m3.state().taskName());
        assertThrows(IllegalStateException.class, () -> eclipse.beginTask("duplicate", 1));
        eclipse.done();
        assertTrue(m3.state().done());
    }

    @Test
    void preservesM3LongOverflowSaturationAndSingleOwner() {
        M3DefaultProgressMonitor m3 = new M3DefaultProgressMonitor();
        IProgressMonitor eclipse = M3EclipseProgressMonitor.borrowed(m3);
        eclipse.internalWorked(1.0e30d);
        assertEquals(Long.MAX_VALUE, m3.state().worked());
        eclipse.internalWorked(2.0d);
        assertEquals(Long.MAX_VALUE, m3.state().worked());
    }
}
