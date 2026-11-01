// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import javax.swing.text.GapContent;
import javax.swing.text.Position;
import proof.MemoryAgent;

/** Measures target-owned fields/arrays; opaque reference-queue internals and weak referents excluded. */
public final class GapStorageMemory {
    private GapStorageMemory() { }
    private static volatile Object keep;

    public static void main(String[] arguments) throws Exception {
        for (int size : new int[] {0, 64, 4096, 65536}) {
            GapContent content = new GapContent(2);
            content.insertString(0, "x".repeat(size));
            List<Position> positions = new ArrayList<>();
            for (int i = 0; i <= size; i++) positions.add(content.createPosition(i));
            if (size > 2) content.remove(1, size - 2);
            content.insertString(Math.min(1, content.length() - 1), "abc");
            var seen = new IdentityHashMap<Object, Boolean>();
            var pending = new ArrayList<Object>();
            pending.add(content);
            long bytes = 0;
            int arrays = 0;
            while (!pending.isEmpty()) {
                Object item = pending.removeLast();
                if (item == null || seen.put(item, Boolean.TRUE) != null) continue;
                bytes += MemoryAgent.sizeOf(item);
                if (item.getClass().isArray()) {
                    arrays++;
                    if (item instanceof Object[] references) Collections.addAll(pending, references);
                } else {
                    for (Class<?> owner = item.getClass(); owner != null
                            && owner.getName().startsWith("javax.swing.text."); owner = owner.getSuperclass()) {
                        for (Field field : owner.getDeclaredFields()) {
                            if (!Modifier.isStatic(field.getModifiers()) && !field.getType().isPrimitive()) {
                                field.setAccessible(true);
                                pending.add(field.get(item));
                            }
                        }
                    }
                }
            }
            keep = positions;
            System.out.println("owned-storage," + size + "," + seen.size() + "," + arrays + "," + bytes);
        }
    }
}
