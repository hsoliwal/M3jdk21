// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

class M3EclipseFrameworkSnapshotsTest {
    @Test
    void osgiBundleIdsStatesAndReferencesUseM3WithoutChangingBundleLifecycle() {
        Bundle system = bundle(0L, Bundle.ACTIVE);
        Bundle resolved = bundle(99L, Bundle.RESOLVED);
        Bundle starting = bundle(Long.MAX_VALUE, Bundle.STARTING);
        M3EclipseBundleSnapshot snapshot = M3EclipseBundleSnapshot.capture(context(system, resolved, starting));
        assertEquals(3, snapshot.size());
        assertSame(system, snapshot.byId(0L));
        assertSame(starting, snapshot.byId(Long.MAX_VALUE));
        assertNull(snapshot.byId(100L));
        assertEquals(Bundle.RESOLVED, snapshot.capturedState(1));
        assertEquals(List.of(system), snapshot.select(Bundle.ACTIVE, 0));
        assertEquals(List.of(resolved, starting), snapshot.select(0, Bundle.ACTIVE));
        assertEquals(3, snapshot.all().size());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.all().clear());
    }

    @Test
    void osgiRejectsDuplicateAndInvalidState() {
        assertThrows(IllegalArgumentException.class,
                () -> M3EclipseBundleSnapshot.capture(context(bundle(4L, Bundle.RESOLVED), bundle(4L, Bundle.ACTIVE))));
        assertThrows(IllegalArgumentException.class,
                () -> M3EclipseBundleSnapshot.capture(context(bundle(5L, Bundle.ACTIVE | Bundle.RESOLVED))));
        assertEquals(0, M3EclipseBundleSnapshot.capture(context()).size());
    }

    @Test
    void extensionSnapshotIndexesValidityButForwardsLiveLookup() {
        AtomicBoolean valid = new AtomicBoolean(true);
        IExtensionPoint earlier = extension(valid);
        IExtensionPoint newer = extension(new AtomicBoolean(true));
        IExtensionRegistry registry = registry(earlier, newer);
        M3EclipseExtensionSnapshot snapshot = M3EclipseExtensionSnapshot.capture(registry);
        assertEquals(2, snapshot.size());
        assertEquals(M3EclipseExtensionSnapshot.VALID_AT_CAPTURE, snapshot.capturedState(0));
        assertSame(earlier, snapshot.at(0));
        assertEquals(List.of(earlier, newer), snapshot.select(M3EclipseExtensionSnapshot.VALID_AT_CAPTURE, 0));
        assertSame(earlier, snapshot.livePoint("known"));
        valid.set(false);
        assertFalse(snapshot.at(0).isValid());
        assertEquals(2, snapshot.select(M3EclipseExtensionSnapshot.VALID_AT_CAPTURE, 0).size());
        assertEquals(List.of(newer), snapshot.refresh().select(M3EclipseExtensionSnapshot.VALID_AT_CAPTURE, 0));
        assertThrows(NullPointerException.class, () -> snapshot.livePoint(null));
    }

    @Test
    void deltaProjectionHasStableBreadthFirstOrdinalsAndPrimitiveKindMasks() {
        IResourceDelta grandchild = delta(IResourceDelta.REMOVED);
        IResourceDelta left = delta(IResourceDelta.ADDED, grandchild);
        IResourceDelta right = delta(IResourceDelta.CHANGED);
        IResourceDelta root = delta(IResourceDelta.NO_CHANGE, left, right);
        M3EclipseResourceDeltaSnapshot snapshot = M3EclipseResourceDeltaSnapshot.capture(root, 20);
        assertEquals(4, snapshot.size());
        assertSame(root, snapshot.at(0));
        assertSame(left, snapshot.at(1));
        assertSame(right, snapshot.at(2));
        assertSame(grandchild, snapshot.at(3));
        assertEquals(-1, snapshot.parentOrdinal(0));
        assertEquals(0, snapshot.parentOrdinal(1));
        assertEquals(0, snapshot.parentOrdinal(2));
        assertEquals(1, snapshot.parentOrdinal(3));
        assertEquals(IResourceDelta.CHANGED, snapshot.capturedKind(2));
        assertEquals(List.of(left), snapshot.select(IResourceDelta.ADDED, 0));
        assertEquals(List.of(root, left, right), snapshot.select(0, IResourceDelta.REMOVED));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.all().clear());
    }

    @Test
    void deltaBudgetRefusalIdentityDuplicationAndDeepChainAreIterative() {
        IResourceDelta leaf = delta(IResourceDelta.CHANGED);
        IResourceDelta duplicate = delta(IResourceDelta.ADDED, leaf, leaf);
        assertThrows(IllegalStateException.class,
                () -> M3EclipseResourceDeltaSnapshot.capture(duplicate, 10));
        IResourceDelta shallow = delta(IResourceDelta.ADDED, delta(IResourceDelta.REMOVED));
        assertThrows(IllegalStateException.class,
                () -> M3EclipseResourceDeltaSnapshot.capture(shallow, 1));
        IResourceDelta tail = delta(IResourceDelta.ADDED);
        for (int i = 0; i < 2048; i++) tail = delta(IResourceDelta.CHANGED, tail);
        M3EclipseResourceDeltaSnapshot snapshot = M3EclipseResourceDeltaSnapshot.capture(tail, 2050);
        assertEquals(2049, snapshot.size());
        assertEquals(2047, snapshot.parentOrdinal(2048));
        assertEquals(2048, snapshot.select(IResourceDelta.CHANGED, 0).size());
    }

    private static Bundle bundle(long id, int state) {
        return proxy(Bundle.class, (proxy, method, args) -> switch (method.getName()) {
            case "getBundleId" -> id;
            case "getState" -> state;
            default -> throw new AssertionError("Unexpected bundle operation: " + method.getName());
        });
    }

    private static BundleContext context(Bundle... bundles) {
        return proxy(BundleContext.class, (proxy, method, args) -> {
            if (method.getName().equals("getBundles")) return bundles.clone();
            throw new AssertionError("Unexpected framework operation: " + method.getName());
        });
    }

    private static IExtensionPoint extension(AtomicBoolean valid) {
        return proxy(IExtensionPoint.class, (proxy, method, args) -> {
            if (method.getName().equals("isValid")) return valid.get();
            throw new AssertionError("Unexpected extension operation: " + method.getName());
        });
    }

    private static IExtensionRegistry registry(IExtensionPoint a, IExtensionPoint b) {
        return proxy(IExtensionRegistry.class, (proxy, method, args) -> {
            if (method.getName().equals("getExtensionPoints") && (args == null || args.length == 0)) {
                return new IExtensionPoint[]{a, b};
            }
            if (method.getName().equals("getExtensionPoint") && args.length == 1) {
                return "known".equals(args[0]) ? a : null;
            }
            throw new AssertionError("Unexpected registry operation: " + method.getName());
        });
    }

    private static IResourceDelta delta(int kind, IResourceDelta... children) {
        return proxy(IResourceDelta.class, (proxy, method, args) -> switch (method.getName()) {
            case "getKind" -> kind;
            case "getAffectedChildren" -> children.clone();
            default -> throw new AssertionError("Unexpected delta operation: " + method.getName());
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> api, java.lang.reflect.InvocationHandler call) {
        return (T) Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[]{api}, call);
    }
}
