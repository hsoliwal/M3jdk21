// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.framework;

import com.m3.collections.M3IndexMaskCompactor;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/** Real OSGi API oracle; the framework owns the references and the snapshot owns only observations. */
public final class M3EclipseBundleOrderContract {
    private static int checks;

    private M3EclipseBundleOrderContract() { }

    private static final class Observation {
        final long id;
        final int state;
        final Bundle bundle;
        boolean captured;

        Observation(long id, int state) {
            this.id = id;
            this.state = state;
            bundle = (Bundle) Proxy.newProxyInstance(Bundle.class.getClassLoader(),
                    new Class<?>[] {Bundle.class}, (proxy, method, args) -> {
                        if (captured) throw new AssertionError("bundle accessed after capture: " + method);
                        return switch (method.getName()) {
                            case "getBundleId" -> id;
                            case "getState" -> state;
                            default -> throw new AssertionError("lifecycle/other bundle call: " + method);
                        };
                    });
        }
    }

    private static BundleContext context(Bundle[] bundles) {
        return (BundleContext) Proxy.newProxyInstance(BundleContext.class.getClassLoader(),
                new Class<?>[] {BundleContext.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("getBundles")) {
                        throw new AssertionError("lifecycle/registry call: " + method);
                    }
                    return bundles == null ? null : bundles.clone();
                });
    }

    private static void check(boolean valid, String message) {
        checks++;
        if (!valid) throw new AssertionError(message);
    }

    private static void sameReferences(List<Bundle> expected, List<Bundle> actual, String message) {
        check(expected.size() == actual.size(), message + ": size");
        for (int i = 0; i < expected.size(); i++) {
            check(expected.get(i) == actual.get(i), message + ": reference " + i);
        }
    }

    private static void refuses(Class<? extends Throwable> type, Runnable action, String message) {
        try {
            action.run();
        } catch (Throwable failure) {
            check(type.isInstance(failure), message + ": wrong failure " + failure);
            return;
        }
        throw new AssertionError(message + ": no failure");
    }

    private static List<Observation> observations() {
        return List.of(new Observation(91, Bundle.ACTIVE),
                new Observation(0, Bundle.INSTALLED),
                new Observation(Long.MAX_VALUE, Bundle.RESOLVED),
                new Observation(7, Bundle.STARTING),
                new Observation(1L << 40, Bundle.STOPPING),
                new Observation(4, Bundle.UNINSTALLED));
    }

    private static M3EclipseBundleSnapshot capture(List<Observation> source) {
        Bundle[] references = source.stream().map(row -> row.bundle).toArray(Bundle[]::new);
        M3EclipseBundleSnapshot snapshot = M3EclipseBundleSnapshot.capture(context(references));
        source.forEach(row -> row.captured = true);
        return snapshot;
    }

    private static List<Bundle> expected(List<Observation> source, int required, int excluded,
            boolean descending) {
        List<Observation> selected = new ArrayList<>();
        for (Observation row : source) {
            if ((row.state & required) == required && (row.state & excluded) == 0) selected.add(row);
        }
        Comparator<Observation> comparison = Comparator.comparingLong(row -> row.id);
        selected.sort(descending ? comparison.reversed() : comparison);
        return selected.stream().map(row -> row.bundle).toList();
    }

    static void capturedIdentityAndMasks(M3IndexMaskCompactor.Backend backend) {
        List<Observation> source = observations();
        M3EclipseBundleSnapshot snapshot = capture(source);
        check(snapshot.size() == source.size(), "captured size");
        for (int i = 0; i < source.size(); i++) {
            Observation row = source.get(i);
            check(snapshot.at(i) == row.bundle, "capture encounter order");
            check(snapshot.capturedState(i) == row.state, "capture state");
            check(snapshot.byId(row.id) == row.bundle, "ID owner resolves original framework reference");
        }
        check(snapshot.byId(-1) == null && snapshot.byId(2) == null, "absent IDs");
        for (boolean descending : new boolean[] {false, true}) {
            sameReferences(expected(source, 0, 0, descending), snapshot.sortedById(descending),
                    "convenience Java ordering");
            for (int required = 0; required < 256; required++) {
                for (int excluded = 0; excluded < 256; excluded++) {
                    sameReferences(expected(source, required, excluded, descending),
                            snapshot.sortedById(required, excluded, descending, backend),
                            "unsigned required/excluded masks " + required + "/" + excluded);
                }
            }
        }
        sameReferences(source.stream().map(row -> row.bundle).toList(), snapshot.all(),
                "sorting leaves capture order untouched");
        sameReferences(List.of(source.get(0).bundle), snapshot.select(Bundle.ACTIVE, 0),
                "existing state selector remains unchanged");
    }

    static void windowsAndIsolation(M3IndexMaskCompactor.Backend backend) {
        List<Observation> source = observations();
        M3EclipseBundleSnapshot snapshot = capture(source);
        for (int[] masks : List.of(new int[] {0, 0}, new int[] {0, Bundle.ACTIVE},
                new int[] {Bundle.ACTIVE, 0}, new int[] {Bundle.ACTIVE, Bundle.ACTIVE},
                new int[] {0, 0xff}, new int[] {0x80, 0})) {
            for (boolean descending : new boolean[] {false, true}) {
                List<Bundle> expected = expected(source, masks[0], masks[1], descending);
                for (int offset = 0; offset <= expected.size(); offset++) {
                    for (int length = 0; length <= expected.size() - offset; length++) {
                        List<Bundle> actual = snapshot.sortedWindowById(masks[0], masks[1],
                                descending, offset, length, backend);
                        sameReferences(expected.subList(offset, offset + length), actual,
                                "window follows filter and sort");
                        refuses(UnsupportedOperationException.class,
                                () -> actual.add(source.get(0).bundle), "window is read-only");
                    }
                }
            }
        }
        List<Bundle> ordered = snapshot.sortedById(0, 0, false, backend);
        refuses(UnsupportedOperationException.class, ordered::clear, "sorted result is read-only");
        refuses(UnsupportedOperationException.class,
                () -> ordered.set(0, source.get(0).bundle), "sorted set is read-only");
        sameReferences(expected(source, 0, 0, false), snapshot.sortedById(false),
                "failed writes leave captured order valid");
    }

    static void validationAndEmpty(M3IndexMaskCompactor.Backend backend) {
        List<Observation> source = observations();
        M3EclipseBundleSnapshot snapshot = capture(source);
        for (int invalid : new int[] {-1, 256, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            refuses(IllegalArgumentException.class,
                    () -> snapshot.sortedById(invalid, 0, false, backend), "invalid required mask");
            refuses(IllegalArgumentException.class,
                    () -> snapshot.sortedById(0, invalid, false, backend), "invalid excluded mask");
        }
        refuses(NullPointerException.class,
                () -> snapshot.sortedById(0, 0, false, null), "null backend");
        for (int[] bounds : List.of(new int[] {-1, 0}, new int[] {0, -1},
                new int[] {1, Integer.MAX_VALUE}, new int[] {7, 0}, new int[] {0, 7})) {
            refuses(IndexOutOfBoundsException.class, () -> snapshot.sortedWindowById(
                    0, 0, false, bounds[0], bounds[1], backend), "invalid window");
        }
        refuses(IndexOutOfBoundsException.class, () -> snapshot.sortedWindowById(
                Bundle.ACTIVE, Bundle.ACTIVE, false, 1, 0, backend), "bounds use filtered size");
        for (Bundle[] empty : new Bundle[][] {null, new Bundle[0]}) {
            M3EclipseBundleSnapshot snapshotEmpty = M3EclipseBundleSnapshot.capture(context(empty));
            check(snapshotEmpty.sortedById(0, 0, false, backend).isEmpty(), "empty order");
            check(snapshotEmpty.sortedWindowById(0, 0, true, 0, 0, backend).isEmpty(), "empty window");
            refuses(IllegalArgumentException.class,
                    () -> snapshotEmpty.sortedById(0, 256, false, backend), "empty validates masks");
            refuses(NullPointerException.class,
                    () -> snapshotEmpty.sortedById(0, 0, false, null), "empty validates backend");
        }
        refuses(NullPointerException.class, () -> M3EclipseBundleSnapshot.capture(null), "null context");
        refuses(NullPointerException.class,
                () -> M3EclipseBundleSnapshot.capture(context(new Bundle[] {null})), "null bundle");
        for (long invalid : new long[] {-1L, Long.MIN_VALUE}) {
            Observation row = new Observation(invalid, Bundle.ACTIVE);
            refuses(IllegalArgumentException.class,
                    () -> M3EclipseBundleSnapshot.capture(context(new Bundle[] {row.bundle})), "negative ID");
        }
        Observation first = new Observation(8, Bundle.ACTIVE);
        Observation duplicate = new Observation(8, Bundle.RESOLVED);
        refuses(IllegalArgumentException.class, () -> M3EclipseBundleSnapshot.capture(
                context(new Bundle[] {first.bundle, duplicate.bundle})), "duplicate ID");
        for (int invalid : new int[] {0, 3, 64, -1}) {
            Observation row = new Observation(4, invalid);
            refuses(IllegalArgumentException.class,
                    () -> M3EclipseBundleSnapshot.capture(context(new Bundle[] {row.bundle})), "invalid state");
        }
    }

    static void nativeRefusal() {
        if (M3IndexMaskCompactor.nativeAvailable()) return;
        M3EclipseBundleSnapshot snapshot = capture(observations());
        refuses(UnsatisfiedLinkError.class,
                () -> snapshot.sortedById(0, 0, false, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED),
                "required native never silently falls back");
        refuses(UnsatisfiedLinkError.class, () -> snapshot.sortedById(
                0, Bundle.ACTIVE, false, M3IndexMaskCompactor.Backend.NATIVE_REQUIRED),
                "required/excluded native never silently falls back");
    }

    public static void main(String[] args) {
        checks = 0;
        if (args.length > 1 || (args.length == 1 && !args[0].equals("--native")
                && !args[0].equals("--java"))) throw new IllegalArgumentException("--java | --native");
        boolean nativeMode = args.length == 1 && args[0].equals("--native");
        if (nativeMode) check(M3IndexMaskCompactor.nativeAvailable(), "actual JNI library required");
        M3IndexMaskCompactor.Backend backend = nativeMode
                ? M3IndexMaskCompactor.Backend.NATIVE_REQUIRED : M3IndexMaskCompactor.Backend.JAVA;
        capturedIdentityAndMasks(backend);
        windowsAndIsolation(backend);
        validationAndEmpty(backend);
        nativeRefusal();
        System.out.println("M3EclipseBundleOrderContract PASS native=" + nativeMode
                + " maskPairsPerDirection=65536 checks=" + checks);
    }
}
