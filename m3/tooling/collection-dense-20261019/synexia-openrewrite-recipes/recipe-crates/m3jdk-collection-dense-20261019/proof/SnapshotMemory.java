// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import proof.MemoryAgent;

/** Actual reachable snapshot structure, excluding caller-owned payload identities. */
public final class SnapshotMemory {
    private SnapshotMemory() { }
    public static void main(String[] args) throws Exception {
        for (int size : new int[] {0, 65, 1024, 65536}) {
            List<Integer> source = new ArrayList<>();
            for (int i = 0; i < size; i++) source.add(i);
            for (int stride : new int[] {1, 2, 16}) {
                var payload = new IdentityHashMap<Object, Boolean>();
                for (Object value : source) payload.put(value, Boolean.TRUE);
                List<Integer> result = Collections.filteredSnapshot(source, value -> value % stride == 0);
                var seen = new IdentityHashMap<Object, Boolean>();
                var pending = new ArrayList<Object>();
                pending.add(result);
                long bytes = 0;
                int arrays = 0;
                while (!pending.isEmpty()) {
                    Object item = pending.removeLast();
                    if (item == null || payload.containsKey(item) || seen.put(item, Boolean.TRUE) != null) continue;
                    bytes += MemoryAgent.sizeOf(item);
                    Class<?> type = item.getClass();
                    if (type.isArray()) {
                        arrays++;
                        if (item instanceof Object[] refs) Collections.addAll(pending, refs);
                    } else {
                        if (!type.getName().startsWith("java.util.")) throw new AssertionError(type);
                        for (Class<?> owner = type; owner != Object.class; owner = owner.getSuperclass()) {
                            for (Field field : owner.getDeclaredFields()) {
                                if (!Modifier.isStatic(field.getModifiers()) && !field.getType().isPrimitive()) {
                                    field.setAccessible(true);
                                    pending.add(field.get(item));
                                }
                            }
                        }
                    }
                }
                if (arrays != 1 || seen.size() != 3) throw new AssertionError("unexpected retained structure");
                System.out.println("{\"size\":" + size + ",\"stride\":" + stride
                        + ",\"selected\":" + result.size() + ",\"structural_bytes\":" + bytes
                        + ",\"structural_objects\":" + seen.size() + ",\"arrays\":" + arrays + "}");
            }
        }
    }
}
