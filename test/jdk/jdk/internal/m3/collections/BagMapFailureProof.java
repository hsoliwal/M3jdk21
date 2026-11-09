// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import jdk.internal.m3.collections.packed.PackedLongBag;
import jdk.internal.m3.collections.packed.PackedLongLongHashMap;

/** Real constrained-heap allocation failure, not a mocked allocator. Run in an isolated JVM. */
//FLLDCIM: RELOCATE - donor blob 1f50696a49e4 at commit d5ad5090d87e (repository, path, URL and digests: m3/collections/recipe/four-library-bags-20261106/PROVENANCE.tsv); recipe m3/collections/recipe/four-library-bags-20261106/relocate.py v1
public final class BagMapFailureProof {
    private BagMapFailureProof() { }
    /** @param args rehash under -Xmx32m, or remove under -Xmx16m @throws Exception on a failed check */
    public static void main(String[] args) throws Exception {
        if (args[0].equals("rehash")) rehash();
        else if (args[0].equals("remove")) remove();
        else throw new IllegalArgumentException("rehash | remove");
    }
    private static Object field(Object owner, String name) throws Exception {
        Field f = owner.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(owner);
    }
    private static void rehash() throws Exception {
        PackedLongLongHashMap map = new PackedLongLongHashMap(); map.put(37, 91);
        Object keys = field(map, "keys"), values = field(map, "values"), states = field(map, "states");
        int capacity = map.capacity();
        var rehash = PackedLongLongHashMap.class.getDeclaredMethod("rehash", int.class); rehash.setAccessible(true);
        boolean failed = false;
        try { rehash.invoke(map, 1 << 21); }
        catch (InvocationTargetException e) { if (!(e.getCause() instanceof OutOfMemoryError)) throw e; failed = true; }
        if (!failed) throw new AssertionError("allocation failure not exercised; use -Xmx32m");
        if (keys != field(map, "keys") || values != field(map, "values") || states != field(map, "states"))
            throw new AssertionError("failed rehash published partial lanes");
        if (map.capacity() != capacity || map.size() != 1 || map.getOrDefault(37, -1) != 91)
            throw new AssertionError("failed rehash changed logical map");
        map.put(12, 25); map.remove(37);
        if (map.size() != 1 || map.getOrDefault(12, -1) != 25) throw new AssertionError("map not reusable");
        System.out.println("PASS real rehash OOME preserves all lanes and logical contents; map reusable");
    }
    private static void remove() throws Exception {
        PackedLongBag bag = new PackedLongBag(260000);
        for (int i = 0; i < 10; i++) bag.add(i);
        Object map = field(bag, "counts"); Object lanes = field(map, "keys");
        if (((long[]) lanes).length != (1 << 19)) throw new AssertionError("expected large sparse table");
        for (int i = 0; i < 6; i++) if (bag.removeAll(i) != 1) throw new AssertionError("remove");
        if (field(map, "keys") != lanes) throw new AssertionError("compaction succeeded; failure was not exercised");
        if (bag.size() != 4 || bag.distinctSize() != 4) throw new AssertionError("bag/map totals diverged");
        for (int i = 0; i < 10; i++) if (bag.count(i) != (i < 6 ? 0 : 1)) throw new AssertionError("wrong key");
        System.out.println("PASS successful removal survives optional compaction OOME; bag/map counts agree");
    }
}
