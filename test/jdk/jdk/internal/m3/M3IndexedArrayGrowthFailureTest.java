/*
 * Copyright (c) 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */

/*
 * @test
 * @summary Array growth keeps all committed slots usable after a real allocation failure
 * @modules java.base/jdk.internal.m3:open
 * @run main/othervm -Xmx20m M3IndexedArrayGrowthFailureTest
 */

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import jdk.internal.m3.M3IndexedArray;

public class M3IndexedArrayGrowthFailureTest {
    private static final Object VALUE = new Object();
    private static byte[] reserve;
    private static final String[] LANES = {
        "states", "generations", "leaseCounts", "freeLinks", "positions", "order"
    };

    private static Field field(String name) throws ReflectiveOperationException {
        Field result = M3IndexedArray.class.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }

    private static void coherent(M3IndexedArray<?> owner, Field values, Field allocated,
            Field[] metadata) throws IllegalAccessException {
        int committed = Array.getLength(values.get(owner));
        check(allocated.getInt(owner) <= committed, "allocated exceeds committed capacity");
        for (int index = 0; index < metadata.length; index++) {
            int actual = Array.getLength(metadata[index].get(owner));
            check(actual >= committed, LANES[index] + " capacity " + actual
                    + " is below committed value capacity " + committed);
        }
    }

    public static void main(String[] args) throws ReflectiveOperationException {
        Field values = field("values"), allocated = field("allocated");
        Field[] metadata = new Field[LANES.length];
        for (int i = 0; i < metadata.length; i++) metadata[i] = field(LANES[i]);
        // This reserve is solely for post-failure assertions and diagnostics.
        reserve = new byte[512 * 1024];
        M3IndexedArray<Object> owner = new M3IndexedArray<>();
        long first = owner.append(VALUE), last = first;
        int successful = 1;
        boolean failed = false;
        try {
            while (successful < 10_000_000) {
                last = owner.append(VALUE);
                successful++;
            }
        } catch (OutOfMemoryError expected) {
            reserve = null;
            failed = true;
        }
        check(failed, "run this regression with -Xmx20m to exhaust real array growth");
        check(owner.size() == successful, "failed append changed size");
        check(owner.version() == successful, "failed append changed version");
        check(allocated.getInt(owner) == successful, "failed append allocated a slot");
        coherent(owner, values, allocated, metadata);
        check(owner.get(first) == VALUE && owner.get(last) == VALUE,
                "allocation failure damaged existing handles");
        check(owner.at(successful / 2) == VALUE, "allocation failure damaged middle slot");
        owner.retain(first);
        check(!owner.remove(first), "allocation failure damaged lease protection");
        owner.release(first);
        check(owner.remove(first), "unleased slot could not be removed after failure");
        long replacement = owner.append(VALUE);
        check(replacement != first && owner.get(replacement) == VALUE,
                "reused slot lost its generation after failure");
        try {
            owner.get(first);
            throw new AssertionError("stale handle was resurrected");
        } catch (IllegalArgumentException expected) { }
        int beforeSize = owner.size();
        long beforeVersion = owner.version();
        try {
            long retry = owner.append(VALUE);
            check(owner.get(retry) == VALUE, "successful growth retry lost its value");
            check(owner.size() == beforeSize + 1 && owner.version() == beforeVersion + 1,
                    "successful retry did not commit once");
        } catch (OutOfMemoryError expected) {
            check(owner.size() == beforeSize && owner.version() == beforeVersion,
                    "failed retry changed logical state");
        }
        coherent(owner, values, allocated, metadata);
        System.out.println("PASS M3IndexedArrayGrowthFailureTest successful=" + successful);
    }
}
