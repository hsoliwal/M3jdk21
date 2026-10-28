// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.core.resources.IResourceDelta;
import org.junit.jupiter.api.Test;

/** Oracle for Eclipse node reference identity during M3-backed delta snapshots. */
class M3EclipseDeltaIdentitySetTest {
    @Test
    void equalsAndHashCodeMustNeverBeInvokedForDeltaIdentity() {
        IResourceDelta first = delta(IResourceDelta.CHANGED);
        IResourceDelta second = delta(IResourceDelta.CHANGED);
        IResourceDelta root = delta(IResourceDelta.NO_CHANGE, first, second);
        M3EclipseResourceDeltaSnapshot snapshot = M3EclipseResourceDeltaSnapshot.capture(root, 3);
        assertEquals(3, snapshot.size());
        assertEquals(0, snapshot.capturedFlags(0));
        assertEquals(0, snapshot.capturedFlags(1));
        assertEquals(0, snapshot.capturedFlags(2));
        assertSame(root, snapshot.at(0));
        assertSame(first, snapshot.at(1));
        assertSame(second, snapshot.at(2));
        assertEquals(0, snapshot.parentOrdinal(1));
        assertEquals(0, snapshot.parentOrdinal(2));
    }

    @Test
    void resizingIdentityPlaneRetainsAllDistinctReferencesAndBreadthFirstOrder() {
        IResourceDelta[] children = new IResourceDelta[513];
        for (int i = 0; i < children.length; i++) {
            children[i] = delta(IResourceDelta.CHANGED);
        }
        IResourceDelta root = delta(IResourceDelta.ADDED, children);
        M3EclipseResourceDeltaSnapshot snapshot = M3EclipseResourceDeltaSnapshot.capture(root, 514);
        assertEquals(514, snapshot.size());
        for (int i = 0; i < children.length; i++) {
            assertSame(children[i], snapshot.at(i + 1));
            assertEquals(0, snapshot.parentOrdinal(i + 1));
        }
    }

    @Test
    void repeatedReferenceCycleAndBudgetStillRefuse() {
        IResourceDelta child = delta(IResourceDelta.REMOVED);
        IResourceDelta duplicate = delta(IResourceDelta.ADDED, child, child);
        assertThrows(IllegalStateException.class,
                () -> M3EclipseResourceDeltaSnapshot.capture(duplicate, 4));
        IResourceDelta shallow = delta(IResourceDelta.ADDED, child);
        assertThrows(IllegalStateException.class,
                () -> M3EclipseResourceDeltaSnapshot.capture(shallow, 1));

        AtomicReference<IResourceDelta[]> descendants = new AtomicReference<>(new IResourceDelta[0]);
        IResourceDelta cyclic = dynamicDelta(IResourceDelta.CHANGED, descendants);
        descendants.set(new IResourceDelta[] {cyclic});
        assertThrows(IllegalStateException.class,
                () -> M3EclipseResourceDeltaSnapshot.capture(cyclic, 3));
    }

    @Test
    void nullChildStillRefusesBeforePublishingAResult() {
        IResourceDelta root = delta(IResourceDelta.CHANGED, (IResourceDelta) null);
        assertThrows(NullPointerException.class,
                () -> M3EclipseResourceDeltaSnapshot.capture(root, 3));
    }

    private static IResourceDelta delta(int kind, IResourceDelta... children) {
        return dynamicDelta(kind, new AtomicReference<>(children.clone()));
    }

    private static IResourceDelta dynamicDelta(
            int kind, AtomicReference<IResourceDelta[]> children) {
        return (IResourceDelta) Proxy.newProxyInstance(
                IResourceDelta.class.getClassLoader(), new Class<?>[] {IResourceDelta.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getKind" -> kind;
                    case "getFlags" -> 0; // A no-flags delta still has the real Eclipse API.
                    case "getAffectedChildren" -> children.get().clone();
                    case "equals", "hashCode" ->
                            throw new AssertionError("Eclipse delta identity must never call "
                                    + method.getName());
                    default -> throw new AssertionError("Unexpected delta API: " + method.getName());
                });
    }
}
