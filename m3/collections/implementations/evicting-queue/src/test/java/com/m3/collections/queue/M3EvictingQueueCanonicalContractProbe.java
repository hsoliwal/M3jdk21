// SPDX-License-Identifier: Apache-2.0
package com.m3.collections.queue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * Differential proof for the historical binary/serialized owner and the
 * first-party capability owner. No Guava runtime is present or required.
 */
public final class M3EvictingQueueCanonicalContractProbe {
    private static final String LEGACY = "com.m3.collections.guava.M3EvictingQueue";
    private static final String CANONICAL = "com.m3.collections.queue.M3EvictingQueue";

    private M3EvictingQueueCanonicalContractProbe() { }

    private record Evidence(String transcript, List<String> methods, byte[] wire) { }

    public static void main(String[] ignored) throws Exception {
        Class<?> legacy = Class.forName(LEGACY);
        Class<?> canonical = Class.forName(CANONICAL);
        require(legacy != canonical, "old and new Java binary identities must remain distinct");
        require(Modifier.isFinal(legacy.getModifiers()), "legacy final ABI changed");
        require(Modifier.isFinal(canonical.getModifiers()), "canonical owner must remain final");
        require(ObjectStreamClass.lookup(legacy).getSerialVersionUID() == 0L,
                "legacy serialization ID changed");
        require(ObjectStreamClass.lookup(canonical).getSerialVersionUID() == 0L,
                "canonical serialization ID changed");

        Evidence before = examine(legacy);
        Evidence after = examine(canonical);
        require(before.transcript().equals(after.transcript()), "queue behavior changed");
        require(before.methods().equals(after.methods()), "normalized public methods changed");
        require(LEGACY.length() == CANONICAL.length(), "descriptor normalization needs equal width");
        String wire = new String(before.wire(), StandardCharsets.ISO_8859_1);
        require(wire.indexOf(LEGACY) >= 0 && wire.indexOf(LEGACY) == wire.lastIndexOf(LEGACY),
                "legacy descriptor must be unique");
        byte[] normalized = wire.replace(LEGACY, CANONICAL)
                .getBytes(StandardCharsets.ISO_8859_1);
        require(Arrays.equals(normalized, after.wire()),
                "serialized bytes differ beyond the deliberately new class descriptor");
        require(!new String(after.wire(), StandardCharsets.ISO_8859_1).contains(LEGACY),
                "canonical stream unexpectedly contains legacy Java class name");

        System.out.println("M3_EVICTING_CANONICAL_NAME_PASS normalized_wire_sha256="
                + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(normalized)));
    }

    private static Evidence examine(Class<?> owner) throws Exception {
        StringBuilder observed = new StringBuilder();
        Queue<String> queue = create(owner, 4);
        require(queue.addAll(List.of("a", "b", "c", "d", "e", "f")), "batch insertion refused");
        observed.append(queue).append('|').append(Arrays.toString(queue.toArray())).append('|');
        String[] extra = new String[6];
        Arrays.fill(extra, "sentinel");
        observed.append(Arrays.toString(queue.toArray(extra))).append('|');

        Iterator<String> cursor = queue.iterator();
        observed.append(cursor.next()).append('|');
        cursor.remove();
        observed.append(queue).append('|');
        observed.append(queue.removeIf("e"::equals)).append('|');
        observed.append(queue.retainAll(List.of("d", "f"))).append('|');
        observed.append(queue).append('|');
        observed.append(owner.getMethod("remainingCapacity").invoke(queue)).append('|');

        queue.add("g");
        queue.add("h");
        observed.append(queue).append('|');
        observed.append(queue.removeAll(List.of("d", "g"))).append('|');
        observed.append(queue).append('|');
        observed.append(queue.contains("h")).append('/').append(queue.contains(null)).append('|');

        Queue<String> zero = create(owner, 0);
        observed.append(zero.add("discard")).append('/').append(zero.size()).append('|');
        try { zero.add(null); throw new AssertionError("null was accepted"); }
        catch (NullPointerException expected) { observed.append("null-rejected|"); }
        try { zero.remove(); throw new AssertionError("empty remove succeeded"); }
        catch (NoSuchElementException expected) { observed.append("empty-rejected|"); }
        try { create(owner, -1); throw new AssertionError("negative capacity accepted"); }
        catch (java.lang.reflect.InvocationTargetException expected) {
            require(expected.getCause() instanceof IllegalArgumentException,
                    "wrong capacity exception type");
            observed.append("capacity-rejected|");
        }

        byte[] image = serialize(queue);
        Object recovered = deserialize(image);
        require(recovered.getClass() == owner, "serialization class identity changed");
        require(Arrays.equals(((Queue<?>) recovered).toArray(), queue.toArray()),
                "serialized queue ordering changed");
        observed.append(Arrays.toString(((Queue<?>) recovered).toArray())).append('|');

        List<String> methods = Arrays.stream(owner.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .map(Method::toGenericString)
                .map(signature -> signature.replace(owner.getName(), "<queue-owner>"))
                .sorted().toList();
        require(!methods.isEmpty(), "no public queue surface");
        return new Evidence(observed.toString(), methods, image);
    }

    @SuppressWarnings("unchecked")
    private static Queue<String> create(Class<?> owner, int capacity) throws Exception {
        return (Queue<String>) owner.getMethod("create", int.class).invoke(null, capacity);
    }

    private static byte[] serialize(Object value) throws Exception {
        try (var bytes = new ByteArrayOutputStream();
                var output = new ObjectOutputStream(bytes)) {
            output.writeObject(value);
            output.flush();
            return bytes.toByteArray();
        }
    }

    private static Object deserialize(byte[] image) throws Exception {
        try (var input = new ObjectInputStream(new ByteArrayInputStream(image))) {
            return input.readObject();
        }
    }

    private static void require(boolean condition, String reason) {
        if (!condition) throw new AssertionError(reason);
    }
}
