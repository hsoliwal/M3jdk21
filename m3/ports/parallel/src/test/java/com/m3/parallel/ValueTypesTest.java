/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.parallel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.parallel.spi.Probe;
import com.m3.parallel.testing.TestKernels;

import java.util.List;

import org.junit.jupiter.api.Test;

final class ValueTypesTest {

    @Test
    void gridRoundsGlobalUpToLocal() {
        assertEquals(320, new Grid1D(300, 64).launchSize());
        assertEquals(256, Grid1D.of(256, 64).launchSize());
        assertEquals(1, new Grid1D(1, 1).launchSize());
        assertEquals(7, new Grid1D(1, 7).launchSize());
        assertEquals(Integer.MAX_VALUE, new Grid1D(Integer.MAX_VALUE, 1).launchSize());

        assertThrows(IllegalArgumentException.class, () -> new Grid1D(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new Grid1D(1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Grid1D(Integer.MAX_VALUE, 64));
    }

    @Test
    void kernelIdsAreValidatedIdentifiers() {
        assertEquals("m3.rv32im.slice", KernelId.of("m3.rv32im.slice").name());
        assertEquals(KernelId.of("a-b_c.1"), new KernelId("a-b_c.1"));
        assertEquals("x", KernelId.of("x").toString());

        assertThrows(IllegalArgumentException.class, () -> KernelId.of(""));
        assertThrows(IllegalArgumentException.class, () -> KernelId.of("has space"));
        assertThrows(IllegalArgumentException.class, () -> KernelId.of("x".repeat(129)));
        assertThrows(NullPointerException.class, () -> KernelId.of(null));
    }

    @Test
    void javaKernelRegistrationIsIdempotentAndNeverReplaces() {
        assertSame(TestKernels.ADD, JavaKernels.register(TestKernels.ADD, TestKernels.ADD_BODY));
        assertThrows(IllegalStateException.class,
                () -> JavaKernels.register(TestKernels.ADD, (index, buffers) -> { }));
        assertTrue(JavaKernels.lookup(TestKernels.MISSING).isEmpty());

        List<KernelId> registered = JavaKernels.registered();
        assertTrue(registered.containsAll(List.of(TestKernels.ADD, TestKernels.ACCUMULATE, TestKernels.TOUCH)));
        assertEquals(registered.stream().map(KernelId::name).sorted().toList(),
                registered.stream().map(KernelId::name).toList());
    }

    @Test
    void probesAndBindingsValidateTheirFields() {
        assertTrue(Probe.ready().available());
        assertEquals("no driver", Probe.unavailable("no driver").reason());
        assertThrows(IllegalArgumentException.class, () -> Probe.unavailable(" "));
        assertThrows(NullPointerException.class, () -> new Plan.Binding(null, Transfer.ONCE));
        assertThrows(NullPointerException.class, () -> new Plan.Binding(IntBuffer.allocate(1), null));
    }
}
