// SPDX-License-Identifier: Apache-2.0
package com.m3.collections.guava;

import java.io.ByteArrayOutputStream;
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

/** Executable semantic, public-method, and serialized-byte comparison witness. */
public final class M3EvictingQueueContractProbe {
    private static final String BASELINE_SHA256 =
            "93698f686b7fb024404d5c2a807b32137c72bc2c03a9feb99e1240b104cd66ff";
    private M3EvictingQueueContractProbe() { }

    public static void main(String[] ignored) throws Exception {
        StringBuilder output = new StringBuilder();
        M3EvictingQueue<String> queue = M3EvictingQueue.create(4);
        queue.addAll(List.of("a", "b", "c", "d", "e", "f"));
        output.append(queue).append('|').append(Arrays.toString(queue.toArray())).append('|');
        String[] typed = queue.toArray(new String[7]);
        output.append(Arrays.toString(typed)).append('|');
        Iterator<String> iterator = queue.iterator();
        output.append(iterator.next()).append('|');
        iterator.remove();
        output.append(queue).append('|');
        output.append(queue.removeIf(s -> s.equals("e"))).append('|');
        output.append(queue.retainAll(List.of("d", "f"))).append('|');
        output.append(queue).append('|').append(queue.remainingCapacity()).append('|');
        queue.add("g");
        queue.add("h");
        output.append(queue).append('|');
        output.append(queue.removeAll(List.of("d", "g"))).append('|');
        output.append(queue).append('|');
        M3EvictingQueue<String> zero = M3EvictingQueue.create(0);
        output.append(zero.add("discard")).append('/').append(zero.size()).append('|');
        try { zero.add(null); throw new AssertionError("null accepted"); }
        catch (NullPointerException expected) { output.append("reject-null|"); }
        output.append(ObjectStreamClass.lookup(M3EvictingQueue.class).getSerialVersionUID()).append('|');

        Method[] methods = M3EvictingQueue.class.getDeclaredMethods();
        Arrays.stream(methods).filter(method -> Modifier.isPublic(method.getModifiers()))
                .map(Method::toGenericString).sorted().forEach(sig -> output.append(sig).append('|'));
        output.append("serial=").append(sha(serialize(queue)));
        String current = sha(output.toString().getBytes(StandardCharsets.UTF_8));
        if (!BASELINE_SHA256.equals(current)) {
            throw new AssertionError("observable or serialized queue contract drift: " + current);
        }
        System.out.println("M3_EVICTING_RING_PRIVATE_NAME_PASS sha256=" + current);
    }

    private static byte[] serialize(Object source) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream objectStream = new ObjectOutputStream(bytes)) {
            objectStream.writeObject(source);
        }
        return bytes.toByteArray();
    }

    private static String sha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
