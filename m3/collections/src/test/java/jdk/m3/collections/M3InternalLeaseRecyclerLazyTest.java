// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Contract-and-lifecycle JUnit bridge; self-test has no external test dependency. */
class M3InternalLeaseRecyclerLazyTest {
    @Test
    void lazyIdleBackingAndAllLifecycleContracts() throws Exception {
        M3InternalLeaseRecyclerLazySelfTest.main(new String[0]);
    }

    @Test
    void publicAndProtectedApiIsUnchanged() {
        var publicNames = Arrays.stream(M3InternalLeaseRecycler.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()) || Modifier.isProtected(m.getModifiers()))
                .map(m -> m.getName() + "/" + m.getParameterCount()).sorted().toList();
        assertEquals(java.util.List.of("close/0", "onPressure/1", "stats/0",
                "trimIdle/1", "withBorrowed/2"), publicNames);
        assertEquals(1, M3InternalLeaseRecycler.class.getDeclaredConstructors().length);
        assertTrue(Modifier.isPublic(M3InternalLeaseRecycler.class.getDeclaredConstructors()[0].getModifiers()));
    }
}
