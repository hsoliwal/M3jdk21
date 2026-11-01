/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;

/** Fork separately under -Xms16m -Xmx16m: induce genuine allocation failure in tombstone compaction. */
public final class M3LongMapRealOomeTest {
    private static int assertions;
    private M3LongMapRealOomeTest() { }
    private static void ok(boolean condition, String what) {
        assertions++;
        if (!condition) throw new AssertionError(what);
    }
    private static Field field(String name) {
        try { Field f = M3LongLongHashMap.class.getDeclaredField(name); f.setAccessible(true); return f; }
        catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }
    private static int asInt(Field field, Object owner) {
        try { return field.getInt(owner); } catch (IllegalAccessException error) { throw new AssertionError(error); }
    }
    private static Object get(Field field, Object owner) {
        try { return field.get(owner); } catch (IllegalAccessException error) { throw new AssertionError(error); }
    }

    public static void main(String[] arguments) {
        if (arguments.length != 1 || (!arguments[0].equals("direct") && !arguments[0].equals("conditional")))
            throw new IllegalArgumentException("direct or conditional");
        M3LongLongHashMap map = new M3LongLongHashMap(20_000);
        for (int i = 0; i < 6_000; i++) map.put(i, 11L * i + 7);
        for (int i = 0; i < 3_000; i++) ok(map.remove(i), "setup deletion");
        Field keysField = field("keys"), valuesField = field("values"), statesField = field("states");
        Field occupiedField = field("occupied"), sizeField = field("size"), modField = field("modCount");
        long[] keys = (long[]) get(keysField, map), values = (long[]) get(valuesField, map);
        byte[] states = (byte[]) get(statesField, map);
        long[] oldKeys = keys.clone(), oldValues = values.clone();
        byte[] oldStates = states.clone();
        int size = asInt(sizeField, map), occupied = asInt(occupiedField, map), epoch = asInt(modField, map);
        ok(size == 3_000 && occupied == 6_000 && keys.length == 32768, "compaction precondition");
        M3LongIterator before = map.keyIterator();
        ArrayList<byte[]> ballast = new ArrayList<>();
        boolean ballastOome = false;
        try {
            while (ballast.size() < 1000) ballast.add(new byte[64 * 1024]);
        } catch (OutOfMemoryError expected) { ballastOome = true; }
        boolean mutationOome = false;
        try {
            if (arguments[0].equals("direct")) map.remove(3_000);
            else map.remove(3_000, 33_007);
        } catch (OutOfMemoryError expected) { mutationOome = true; }
        int retained = ballast.size();
        ballast.clear();
        ballast = null;
        System.gc();
        ok(ballastOome && mutationOome, "must trigger actual JVM OOME at staging allocation");
        ok(get(keysField,map)==keys && get(valuesField,map)==values && get(statesField,map)==states,
                "primitive lane identity on failed compaction");
        ok(Arrays.equals(keys,oldKeys) && Arrays.equals(values,oldValues) && Arrays.equals(states,oldStates),
                "primitive lane contents on failed compaction");
        ok(asInt(sizeField,map)==size && asInt(occupiedField,map)==occupied && asInt(modField,map)==epoch,
                "size, occupied and modCount on failed compaction");
        ok(map.containsKey(3_000) && map.getOrDefault(3_000,-1)==33_007,
                "failed removed key still present");
        try { ok(before.hasNext(), "pre-existing iterator still valid"); }
        catch (ConcurrentModificationException failure) { throw new AssertionError("iterator invalidated",failure); }
        System.out.printf("M3_LONGMAP_REAL_OOME_PASS mode=%s assertions=%d ballast=%d%n",arguments[0],assertions,retained);
    }
}
