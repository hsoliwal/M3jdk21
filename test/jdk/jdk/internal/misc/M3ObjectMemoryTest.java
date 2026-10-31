/* Copyright (c) 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0 */
/*
 * @test
 * @summary M3JDK21 shallow object size, JVMTI GC-stable tags and observed reference edges
 * @modules java.base/jdk.internal.misc
 * @run main/othervm M3ObjectMemoryTest
 */
import jdk.internal.misc.M3ObjectMemory;

public final class M3ObjectMemoryTest {
    public static void main(String[] args) {
        Object target = new Object();
        Object alias = target;
        Object other = new Object();
        if (!M3ObjectMemory.sizingAvailable()
                || M3ObjectMemory.shallowSize(target) <= 0
                || M3ObjectMemory.shallowSize(new byte[4096])
                        <= M3ObjectMemory.shallowSize(target)) {
            throw new AssertionError("VM-reported shallow sizes");
        }
        if (!M3ObjectMemory.taggingAvailable()) throw new AssertionError("tag capability");
        long initialId = M3ObjectMemory.objectId(target);
        if (initialId <= 0 || initialId != M3ObjectMemory.objectId(alias)
                || initialId == M3ObjectMemory.objectId(other)) {
            throw new AssertionError("tag identity and aliases");
        }
        System.gc();
        if (M3ObjectMemory.objectId(target) != initialId)
            throw new AssertionError("GC-stable tag");
        var snapshot = M3ObjectMemory.referenceSnapshot(target);
        if (snapshot.observedIncomingEdges() < 0
                || snapshot.heapEdges() + snapshot.rootEdges() != snapshot.observedIncomingEdges()
                || snapshot.completedNanos() < snapshot.startedNanos()) {
            throw new AssertionError("untrusted edge categories");
        }
        expect(NullPointerException.class, () -> M3ObjectMemory.shallowSize(null));
        expect(NullPointerException.class, () -> M3ObjectMemory.objectId(null));
        expect(NullPointerException.class, () -> M3ObjectMemory.referenceSnapshot(null));
        expect(UnsupportedOperationException.class, () -> M3ObjectMemory.addressOf(target));
        System.out.println("PASS M3ObjectMemoryTest size/id/roots; address deliberately unavailable");
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("wrong exception", failure);
        }
        throw new AssertionError("missing expected " + type.getName());
    }
}
