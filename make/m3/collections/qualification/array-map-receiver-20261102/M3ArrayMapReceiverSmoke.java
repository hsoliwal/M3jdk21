// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
package jdk.m3.collections.receiver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import jdk.m3.collections.apache.M3HashedMap;
import jdk.m3.collections.apache.M3LinkedMap;
import jdk.m3.collections.apache.M3LRUMap;
import jdk.m3.collections.id.M3LongObjectLinkedHashMap;

/** Independently executable Java 21 receiver smoke; full donor/JNI/JMH gates remain separate. */
public final class M3ArrayMapReceiverSmoke {
    private static long assertions;

    private M3ArrayMapReceiverSmoke() { }

    private static void check(boolean valid, String message) {
        assertions++;
        if (!valid) {
            throw new AssertionError(message);
        }
    }

    private static void lane(Class<?> owner, String field, Class<?> type) throws Exception {
        Field f = owner.getDeclaredField(field);
        check(f.getType() == type, owner.getSimpleName() + "." + field + " is not " + type);
        check(Modifier.isTransient(f.getModifiers()), owner.getSimpleName() + "." + field + " must be transient");
    }

    private static void storage(Class<?> owner, boolean links) throws Exception {
        lane(owner, "keys", Object[].class);
        lane(owner, "values", Object[].class);
        lane(owner, "hashes", int[].class);
        lane(owner, "index", int[].class);
        lane(owner, "liveMask", long[].class);
        if (links) {
            lane(owner, "before", int[].class);
            lane(owner, "after", int[].class);
        }
        for (Field f : owner.getDeclaredFields()) {
            if (!Modifier.isTransient(f.getModifiers())) {
                check(!Map.Entry.class.isAssignableFrom(f.getType()),
                        owner.getSimpleName() + " persists Entry field " + f.getName());
                check(!f.getType().getSimpleName().equals("Node"),
                        owner.getSimpleName() + " persists Node field " + f.getName());
            }
        }
    }

    private static void roundTrip(Map<Integer, String> map) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(map);
        }
        Object copy;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            copy = in.readObject();
        }
        check(map.equals(copy), "serialization must preserve entries");
        check(map.hashCode() == copy.hashCode(), "serialization hash mismatch");
    }

    private static void randomAccess(Map<Integer, String> candidate) throws Exception {
        Map<Integer, String> oracle = new HashMap<>();
        candidate.put(null, "null");
        oracle.put(null, "null");
        for (int i = 0; i < 320; i++) {
            candidate.put(i, "value-" + i);
            oracle.put(i, "value-" + i);
        }
        for (int i = 0; i < 320; i += 3) {
            check(java.util.Objects.equals(candidate.remove(i), oracle.remove(i)), "removed value " + i);
        }
        for (int i = 320; i < 520; i++) {
            candidate.put(i, "value-" + i);
            oracle.put(i, "value-" + i);
        }
        check(candidate.equals(oracle) && oracle.equals(candidate), "oracle map equality");
        check(candidate.hashCode() == oracle.hashCode(), "hashCode parity");
        Iterator<Map.Entry<Integer, String>> entries = candidate.entrySet().iterator();
        Map.Entry<Integer, String> first = entries.next();
        Integer key = first.getKey();
        entries.remove();
        oracle.remove(key);
        check(candidate.equals(oracle), "entrySet iterator remove");
        check(candidate.keySet().size() == candidate.size(), "live view size");
        roundTrip(candidate);
    }

    private static void primitiveIds() throws Exception {
        M3LongObjectLinkedHashMap<String> candidate = new M3LongObjectLinkedHashMap<>();
        Map<Long, String> oracle = new java.util.LinkedHashMap<>();
        for (long id = -128; id < 320; id++) {
            String value = id % 13 == 0 ? null : "v" + id;
            check(java.util.Objects.equals(candidate.put(id, value), oracle.put(id, value)),
                    "primitive id insertion " + id);
        }
        for (long id = -127; id < 320; id += 3) {
            check(java.util.Objects.equals(candidate.remove(id), oracle.remove(id)),
                    "primitive id removal " + id);
        }
        for (long id = 320; id < 450; id++) {
            String value = "next" + id;
            check(java.util.Objects.equals(candidate.put(id, value), oracle.put(id, value)),
                    "primitive id reuse " + id);
        }
        check(candidate.size() == oracle.size(), "primitive id map size");
        int i = 0;
        for (Map.Entry<Long, String> e : oracle.entrySet()) {
            check(candidate.idAt(i) == e.getKey().longValue(), "primitive id rank " + i);
            check(java.util.Objects.equals(candidate.valueAt(i), e.getValue()), "primitive value at " + i);
            check(candidate.indexOf(e.getKey().longValue()) == i, "primitive indexOf " + i);
            check(candidate.containsKey(e.getKey().longValue()), "primitive membership " + i);
            i++;
        }
        check(candidate.firstId() == oracle.keySet().iterator().next().longValue(), "primitive first id");
        long last = 0L;
        for (Long id : oracle.keySet()) {
            last = id.longValue();
        }
        check(candidate.lastId() == last, "primitive last id");
        check(candidate.ids().length == oracle.size(), "primitive ids span");
        Class<?> owner = M3LongObjectLinkedHashMap.class;
        check(owner.getDeclaredField("ids").getType() == long[].class, "primitive ids must be long[]");
        check(owner.getDeclaredField("values").getType() == Object[].class, "primitive values must be Object[]");
        check(owner.getDeclaredField("index").getType() == int[].class, "primitive index must be int[]");
        check(owner.getDeclaredField("liveMask").getType() == long[].class, "primitive liveness must be long[]");
    }

    public static void main(String[] args) throws Exception {
        storage(M3HashedMap.class, true);
        storage(M3LinkedMap.class, false);
        storage(M3LRUMap.class, true);
        primitiveIds();
        randomAccess(new M3HashedMap<>());
        randomAccess(new M3LinkedMap<>());

        M3LinkedMap<Integer, String> linked = new M3LinkedMap<>();
        linked.put(7, "a");
        linked.put(3, "b");
        linked.put(9, "c");
        check(linked.indexOf(3) == 1, "insertion-order indexed lookup");
        check(linked.get(0) == 7, "insertion-order first index");
        check(linked.firstKey() == 7 && linked.lastKey() == 9, "ordered map ends");

        M3LRUMap<Integer, String> lru = new M3LRUMap<>(3);
        lru.put(1, "one");
        lru.put(2, "two");
        lru.put(3, "three");
        check("one".equals(lru.get(1)), "LRU access");
        lru.put(4, "four");
        check(!lru.containsKey(2) && lru.containsKey(1), "least-recent eviction");
        check(lru.size() == 3, "LRU bound");
        roundTrip(lru);
        System.out.println("M3_ARRAY_MAP_RESULT_ONLY_RECEIVER_PASS assertions=" + assertions);
    }
}
