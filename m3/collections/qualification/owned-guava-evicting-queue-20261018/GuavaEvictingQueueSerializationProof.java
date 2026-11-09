/*
 * Copyright 2026 M3 contributors.
 * Licensed under the Apache License, Version 2.0.
 * Regression fixture: public JDK streams/filters and Queue API only.
 * It checks the private owned wire; it does not assert Guava wire compatibility.
 */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.InvalidObjectException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

public final class GuavaEvictingQueueSerializationProof {
    private static Method factory;
    private static String ownerName;
    private static int cases;
    private static int failures;
    private static int checks;

    private GuavaEvictingQueueSerializationProof() { }

    @FunctionalInterface private interface Action { void run() throws Exception; }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static String quoted(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    private static void run(String name, Action action) {
        cases++;
        try {
            action.run();
            System.out.println("{\"case\":" + quoted(name) + ",\"status\":\"PASS\"}");
        } catch (Exception | AssertionError failure) {
            failures++;
            System.out.println("{\"case\":" + quoted(name) + ",\"status\":\"FAIL\",\"error\":"
                    + quoted(failure.getClass().getName()) + ",\"message\":"
                    + quoted(String.valueOf(failure.getMessage())) + "}");
        }
    }

    @SuppressWarnings("unchecked") private static Queue<Object> queue(int maximum) throws Exception {
        return (Queue<Object>) factory.invoke(null, maximum);
    }

    @SuppressWarnings("unchecked") private static Queue<Object> asQueue(Object object) {
        return (Queue<Object>) object;
    }

    private static byte[] encode(Object object) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream stream = new ObjectOutputStream(bytes)) {
            stream.writeObject(object);
        }
        return bytes.toByteArray();
    }

    private static Object decode(byte[] bytes, String pattern, List<Long> lengths)
            throws IOException, ClassNotFoundException {
        ObjectInputFilter policy = ObjectInputFilter.Config.createFilter(pattern);
        try (ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            stream.setObjectInputFilter(info -> {
                if (info.arrayLength() >= 0) lengths.add(info.arrayLength());
                return policy.checkInput(info);
            });
            return stream.readObject();
        }
    }

    private static Object permitted(byte[] bytes) throws IOException, ClassNotFoundException {
        return decode(bytes, "maxarray=64;maxdepth=32;maxrefs=256;*", new ArrayList<>());
    }

    private static void expectInvalid(byte[] bytes) throws Exception {
        try {
            permitted(bytes);
            throw new AssertionError("malformed image was accepted");
        } catch (InvalidObjectException expected) {
            check(expected.getMessage() != null, "InvalidObjectException needs a diagnostic");
        }
    }

    private static void putInt(byte[] bytes, int offset, int number) {
        bytes[offset] = (byte) (number >>> 24);
        bytes[offset + 1] = (byte) (number >>> 16);
        bytes[offset + 2] = (byte) (number >>> 8);
        bytes[offset + 3] = (byte) number;
    }

    private static int uniqueIndex(byte[] bytes, byte[] needle) {
        int found = -1;
        int occurrences = 0;
        for (int start = 0; start <= bytes.length - needle.length; start++) {
            boolean equal = true;
            for (int offset = 0; offset < needle.length; offset++) {
                if (bytes[start + offset] != needle[offset]) { equal = false; break; }
            }
            if (equal) { found = start; occurrences++; }
        }
        check(occurrences == 1, "expected one unambiguous primitive/class descriptor pattern: " + occurrences);
        return found;
    }

    // The fixture uses a real three-element queue. Its class has one int instance field.
    // Match descriptor end/null superclass + maxSize + four-byte custom-data block.
    // The pattern must be unique; no huge capacity is ever passed to a constructor.
    private static byte[] corruptDeclarations(int capacity, int count) throws Exception {
        Queue<Object> queue = queue(7);
        queue.addAll(List.of("first", "second", "third"));
        byte[] bytes = encode(queue);
        byte[] pattern = {0x78, 0x70, 0, 0, 0, 7, 0x77, 4, 0, 0, 0, 3};
        int start = uniqueIndex(bytes, pattern);
        putInt(bytes, start + 2, capacity);
        putInt(bytes, start + 8, count);
        return bytes;
    }

    // A test-only stream producer with the same public serialization field schema.
    // Its descriptor is mechanically renamed to the owner; OIS resolves the real owner.
    // No readObject override, fake FilterInfo, reflection into JDK internals or Unsafe.
    private static final class WireCarrier implements Serializable {
        @Serial private static final long serialVersionUID = 0L;
        private final int maxSize;
        private transient int count;
        private transient Object image;
        WireCarrier(int maximum, int size, Object payload) {
            maxSize = maximum; count = size; image = payload;
        }
        @Serial private void writeObject(ObjectOutputStream stream) throws IOException {
            stream.defaultWriteObject();
            stream.writeInt(count);
            stream.writeObject(image);
        }
    }

    private static byte[] renameCarrier(byte[] source) throws IOException {
        byte[] before = WireCarrier.class.getName().getBytes(StandardCharsets.UTF_8);
        byte[] after = ownerName.getBytes(StandardCharsets.UTF_8);
        byte[] descriptor = new byte[before.length + 3];
        descriptor[0] = 0x72;
        descriptor[1] = (byte) (before.length >>> 8);
        descriptor[2] = (byte) before.length;
        System.arraycopy(before, 0, descriptor, 3, before.length);
        int offset = uniqueIndex(source, descriptor);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(source, 0, offset + 1);
        output.write(after.length >>> 8);
        output.write(after.length);
        output.write(after);
        output.write(source, offset + descriptor.length, source.length - offset - descriptor.length);
        return output.toByteArray();
    }

    private static byte[] crafted(int count, Object image) throws IOException {
        return renameCarrier(encode(new WireCarrier(7, count, image)));
    }

    private static void arrayImageDetached() throws Exception {
        Object[] image = new Object[2];
        image[0] = image;
        WireCarrier carrier = new WireCarrier(7, 1, image);
        Object[] graph = (Object[]) permitted(renameCarrier(encode(new Object[] {carrier, image})));
        Queue<Object> restored = asQueue(graph[0]);
        Object[] external = (Object[]) graph[1];
        check(restored.size() == 1 && restored.peek() == external, "payload identity must be retained");
        check(external[0] == external, "serialized self array identity must be retained");
        external[0] = "caller-change";
        check(restored.peek() == external, "external image mutation must not replace private queue slot");
        Object[] apiCopy = restored.toArray();
        apiCopy[0] = "snapshot-change";
        check(restored.peek() == external, "toArray must remain detached");
        check(restored.poll() == external && restored.isEmpty(), "detached image must support removal");
        check(external[0].equals("caller-change"), "queue removal must not clear externally visible payload array");
    }

    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1) throw new IllegalArgumentException("one owner class argument required");
        ownerName = arguments[0];
        factory = Class.forName(ownerName).getMethod("create", int.class);

        run("P1-real-array-filter-rejects-three-elements", () -> {
            Queue<Object> original = queue(7);
            original.addAll(List.of("one", "two", "three"));
            List<Long> observed = new ArrayList<>();
            try {
                decode(encode(original), "maxarray=2;*", observed);
                throw new AssertionError("restrictive maxarray filter accepted three-element queue");
            } catch (InvalidClassException expected) {
                check(observed.contains(4L), "real OIS must report the count+1 array length");
            }
        });
        run("permitted-zero-capacity", () -> {
            Queue<Object> restored = asQueue(permitted(encode(queue(0))));
            check(restored.isEmpty() && restored.size() == 0, "zero queue state");
            check(restored.add("ignored") && restored.isEmpty(), "zero capacity remains evicting");
        });
        run("permitted-wrapped-order-and-capacity", () -> {
            Queue<Object> original = queue(3);
            original.addAll(List.of("a", "b", "c", "d", "e"));
            Queue<Object> restored = asQueue(permitted(encode(original)));
            check(Arrays.equals(restored.toArray(), new Object[] {"c", "d", "e"}), "logical wrapped order");
            restored.add("f");
            check(Arrays.equals(restored.toArray(), new Object[] {"d", "e", "f"}), "capacity after roundtrip");
            check(Arrays.equals(original.toArray(), new Object[] {"c", "d", "e"}), "original detached");
        });
        run("permitted-shared-element-graph", () -> {
            Queue<Object> original = queue(3);
            List<String> shared = new ArrayList<>(List.of("payload"));
            original.add(shared); original.add(shared);
            Object[] graph = (Object[]) permitted(encode(new Object[] {original, shared}));
            Object[] values = asQueue(graph[0]).toArray();
            check(values.length == 2 && values[0] == values[1] && values[0] == graph[1], "shared element references");
        });
        run("permitted-owner-self-reference", () -> {
            Queue<Object> original = queue(3);
            original.add(original);
            Queue<Object> restored = asQueue(permitted(encode(original)));
            check(restored.size() == 1 && restored.peek() == restored, "owner self reference");
            check(restored.poll() == restored && restored.isEmpty(), "self removal");
        });
        run("P2-negative-capacity", () -> expectInvalid(corruptDeclarations(-1, 3)));
        run("P2-count-exceeds-capacity", () -> expectInvalid(corruptDeclarations(2, 3)));
        run("P2-negative-count", () -> expectInvalid(corruptDeclarations(7, -1)));
        run("P2-count-plus-one-overflow-without-allocation", () ->
                expectInvalid(corruptDeclarations(Integer.MAX_VALUE, Integer.MAX_VALUE)));
        run("wire-reject-non-array", () -> expectInvalid(crafted(1, "payload")));
        run("wire-reject-array-subtype", () -> expectInvalid(crafted(1, new String[] {"payload", null})));
        run("wire-reject-wrong-length", () -> expectInvalid(crafted(1, new Object[] {"payload"})));
        run("wire-reject-non-null-sentinel", () -> expectInvalid(crafted(1, new Object[] {"payload", "bad"})));
        run("wire-reject-null-element", () -> expectInvalid(crafted(1, new Object[] {null, null})));
        run("wire-detached-self-and-external-image", GuavaEvictingQueueSerializationProof::arrayImageDetached);

        System.out.println("{\"summary\":\"OWNED_PRIVATE_SERIALIZATION\",\"owner\":" + quoted(ownerName)
                + ",\"cases\":" + cases + ",\"checks\":" + checks + ",\"failures\":" + failures + "}");
        if (failures != 0) throw new AssertionError("serialization regression failures: " + failures);
    }
}
