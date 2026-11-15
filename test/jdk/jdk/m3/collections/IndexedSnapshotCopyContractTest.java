/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.lang.reflect.Field;
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/** Collection-to-array ownership and legacy array component-type compatibility. */
public final class IndexedSnapshotCopyContractTest {
    private static int checks;

    private IndexedSnapshotCopyContractTest() { }

    private static void check(boolean condition, String description) {
        checks++;
        if (!condition) throw new AssertionError(description);
    }

    private static Object[] payload(IndexedListSnapshot<?> snapshot) throws Exception {
        Field field = IndexedListSnapshot.class.getDeclaredField("values");
        field.setAccessible(true);
        return (Object[]) field.get(snapshot);
    }

    private static final class Source extends AbstractCollection<Object> {
        private final Object[] values;
        private final RuntimeException failure;
        private Object[] returned;
        private int calls;

        Source(Object[] values) { this(values, null); }
        Source(Object[] values, RuntimeException failure) {
            this.values = values;
            this.failure = failure;
        }
        @Override public Object[] toArray() {
            calls++;
            if (failure != null) throw failure;
            returned = values == null ? null : values.clone();
            return returned;
        }
        @Override public int size() { throw new AssertionError("size not demanded"); }
        @Override public Iterator<Object> iterator() {
            throw new AssertionError("iterator not demanded");
        }
    }

    private static void exactObjectArrayStillTransfersWithoutAnotherCopy() throws Exception {
        Object token = new Object();
        for (Object[] values : new Object[][] {{}, {null}, {token, null, token}}) {
            Source source = new Source(values);
            IndexedListSnapshot<Object> snapshot = IndexedListSnapshot.copyOf(source);
            check(source.calls == 1, "one toArray callback");
            check(payload(snapshot) == source.returned, "fresh Object[] must transfer directly");
            check(payload(snapshot).getClass() == Object[].class, "exact payload type");
            for (int i = 0; i < values.length; i++) {
                check(snapshot.get(i) == values[i], "identity and encounter order");
                check(snapshot.physicalIndex(i) == i, "dense physical coordinates");
            }
            Arrays.fill(values, null);
            if (snapshot.size() > 1) check(snapshot.get(0) == token, "source state detached");
        }
    }

    private static void legacyCovariantArraysNormalizeAndStayDetached() throws Exception {
        for (String[] values : new String[][] {{}, {null}, {"first", null, "last"}}) {
            Source source = new Source(values);
            IndexedListSnapshot<Object> snapshot = IndexedListSnapshot.copyOf(source);
            check(source.calls == 1, "one legacy toArray callback");
            check(payload(snapshot).getClass() == Object[].class, "legacy runtime type normalized");
            check(payload(snapshot) != source.returned, "covariant array normalization owns output");
            for (int i = 0; i < values.length; i++) {
                check(snapshot.get(i) == values[i], "legacy order and identity");
            }
            if (values.length > 1) {
                source.returned[0] = "changed";
                check("first".equals(snapshot.get(0)), "legacy source output is detached");
            }
            IndexedListSnapshot<Object> second = IndexedListSnapshot.copyOf(snapshot);
            check(second != snapshot, "snapshot copy has distinct identity");
            check(payload(second) != payload(snapshot), "snapshot copy has detached payload");
            check(payload(second).getClass() == Object[].class, "copied payload stays exact");
            check(second.equals(snapshot), "snapshot copy values");
        }
    }

    private static void failuresKeepTheirIdentityAndCallbackCount() {
        RuntimeException signal = new IllegalStateException("toArray failure");
        Source source = new Source(new Object[0], signal);
        try { IndexedListSnapshot.copyOf(source); throw new AssertionError("source accepted"); }
        catch (RuntimeException failure) { check(failure == signal, "source exception identity"); }
        check(source.calls == 1, "one throwing callback");
        source = new Source(null);
        try { IndexedListSnapshot.copyOf(source); throw new AssertionError("null array accepted"); }
        catch (NullPointerException expected) { check(source.calls == 1, "one null-array callback"); }
        try {
            IndexedListSnapshot.copyOf((Collection<Object>) null);
            throw new AssertionError("null source accepted");
        } catch (NullPointerException expected) {
            check("source".equals(expected.getMessage()), "null source message");
        }
    }

    public static void main(String[] arguments) throws Exception {
        exactObjectArrayStillTransfersWithoutAnotherCopy();
        legacyCovariantArraysNormalizeAndStayDetached();
        failuresKeepTheirIdentityAndCallbackCount();
        System.out.println("INDEXED_SNAPSHOT_COPY_CONTRACT_PASS checks=" + checks);
    }
}
