/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import jdk.m3.arrays.M3ArrayNative;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/** Run the independent Java 21 differential oracle under the owning Maven test gate. */
public final class M3GenericArrayListTest {
    /** Required output/input shared by the normal JVM and its separate missing-provider discriminator. */
    private static final String SERIALIZATION_FIXTURE = "m3.arraylist.serialization.fixture";

    @Test public void objectArrayListAndIndexedSnapshot() throws Exception {
        M3GenericArrayListSelfTest.main(new String[0]);
    }

    /** Each storage benchmark runs as its own JUnit case; timings remain observations without speed thresholds. */
    @TestFactory @Tag("performance")
    Stream<DynamicTest> storageAtomBenchmarksRunTheExistingMeasuredWorkloads() {
        return List.of("allocate", "allocate-empty", "bulk-copy", "grow", "reserve-noop", "trim", "trim-noop",
                "clone", "object-toArray", "deserialize").stream()
                .map(mode -> DynamicTest.dynamicTest("storage/" + mode,
                        () -> M3CollectionAllocationBenchmark.main(new String[] {
                                mode, mode.equals("allocate-empty") ? "0" : "64", "7"})));
    }

    private static Object[] backing(M3ArrayList<?> list) throws ReflectiveOperationException {
        Field field = M3ArrayList.class.getDeclaredField("elements");
        field.setAccessible(true);
        return (Object[]) field.get(list);
    }

    private static void zeroTail(M3ArrayList<?> list) throws ReflectiveOperationException {
        Object[] storage = backing(list);
        assertEquals(Object[].class, storage.getClass(), "backing has the exact Object component type");
        for (int slot = list.size(); slot < storage.length; slot++) {
            assertNull(storage[slot], "unused backing slot " + slot);
        }
    }

    @Test void backingAllocationIsTypedZeroedAndDetachedAcrossConstructors() throws Exception {
        List<M3ArrayList<Object>> owners = List.of(new M3ArrayList<>(), new M3ArrayList<>(0),
                new M3ArrayList<>(1), new M3ArrayList<>(257), new M3ArrayList<>(List.of()));
        int[] capacities = {0, 0, 1, 257, 0};
        for (int owner = 0; owner < owners.size(); owner++) {
            M3ArrayList<Object> list = owners.get(owner);
            assertTrue(list.isEmpty());
            assertEquals(capacities[owner], list.capacity());
            zeroTail(list);
            for (int earlier = 0; earlier < owner; earlier++) {
                assertNotSame(backing(owners.get(earlier)), backing(list), "even empty native storage is detached");
            }
        }
        assertTrue(M3ArrayNative.isLoaded(), "required backing creation needs the actual JNI provider");
    }

    @Test void collectionImportNormalizesCovariantArraysAndCallsToArrayOnce() throws Exception {
        String first = new String("first");
        String[] foreign = {first, null, first};
        List<String> calls = new ArrayList<>();
        Collection<String> source = new AbstractCollection<>() {
            @Override public int size() { calls.add("size"); return foreign.length; }
            @Override public Iterator<String> iterator() {
                calls.add("iterator");
                return Arrays.asList(foreign).iterator();
            }
            @Override public Object[] toArray() { calls.add("toArray"); return foreign; }
        };
        M3ArrayList<Object> owner = new M3ArrayList<>(source);
        assertEquals(List.of("toArray"), calls, "source evaluation remains exactly one toArray call");
        assertEquals(Object[].class, backing(owner).getClass());
        assertNotSame(foreign, backing(owner));
        assertSame(first, owner.get(0));
        assertSame(first, owner.get(2));
        owner.add(Integer.valueOf(7)); // fails if the foreign String[] was retained as backing
        foreign[0] = "foreign mutation";
        assertSame(first, owner.get(0));
        assertEquals(Arrays.asList(first, null, first, 7), owner);
    }

    @Test void copyBackingKeepsPrefixIdentityAndZeroesTheExpandedTail() throws Exception {
        Method copy = M3ArrayList.class.getDeclaredMethod("copyBacking", Object[].class, int.class);
        copy.setAccessible(true);
        String same = new String("same");
        Object[] source = new String[] {same, null, same};
        Object[] before = source.clone();
        for (int length : new int[] {0, 1, 3, 7}) {
            Object[] result = (Object[]) copy.invoke(null, source, length);
            assertEquals(Object[].class, result.getClass());
            assertEquals(length, result.length);
            assertNotSame(source, result);
            for (int slot = 0; slot < Math.min(source.length, length); slot++) {
                assertSame(source[slot], result[slot]);
            }
            for (int slot = source.length; slot < length; slot++) {
                assertNull(result[slot]);
            }
        }
        InvocationTargetException refusal = assertThrows(InvocationTargetException.class,
                () -> copy.invoke(null, source, -1));
        assertTrue(refusal.getCause() instanceof NegativeArraySizeException);
        assertArrayEquals(before, source, "copy and refusal never mutate the supplied source");
    }

    @Test void capacityArithmeticRefusesOverflowWithoutAllocatingLargeArrays() throws Exception {
        Method required = M3ArrayList.class.getDeclaredMethod("required", int.class, int.class);
        required.setAccessible(true);
        assertEquals(0, required.invoke(null, 0, 0));
        assertEquals(Integer.MAX_VALUE, required.invoke(null, Integer.MAX_VALUE - 1, 1));
        for (int[] input : new int[][] {{Integer.MAX_VALUE, 1}, {0, -1}, {Integer.MAX_VALUE, Integer.MAX_VALUE}}) {
            InvocationTargetException refusal = assertThrows(InvocationTargetException.class,
                    () -> required.invoke(null, input[0], input[1]));
            assertTrue(refusal.getCause() instanceof OutOfMemoryError);
            assertEquals("M3 generic list capacity exhausted", refusal.getCause().getMessage());
        }
    }

    @Test void growthPreservesReferenceIdentityAndInitializesEveryUnusedTail() throws Exception {
        for (int initial : new int[] {0, 1, 2, 9, 10, 63, 64, 65, 127, 128, 129, 257}) {
            M3ArrayList<Object> owner = new M3ArrayList<>(initial);
            Object shared = new Object();
            for (int slot = 0; slot < initial; slot++) {
                owner.add(slot % 3 == 0 ? null : shared);
            }
            Object[] previous = backing(owner);
            Object[] before = previous.clone();
            int epoch = owner.m3StructureEpoch();
            Object appended = new Object();
            owner.add(appended);
            Object[] grown = backing(owner);
            assertNotSame(previous, grown);
            assertEquals(initial == 0 ? 10 : initial + (initial >>> 1) + 1, owner.capacity());
            assertEquals(initial + 1, owner.size());
            assertEquals(epoch + 1, owner.m3StructureEpoch());
            for (int slot = 0; slot < previous.length; slot++) {
                assertSame(previous[slot], grown[slot]);
            }
            assertSame(appended, owner.get(initial));
            assertArrayEquals(before, previous, "growing the owner leaves the old image intact");
            zeroTail(owner);
        }
    }

    @Test void ensureCapacityPreservesNoOpIdentityAndPublishesOnlyTheGrowthEpoch() throws Exception {
        M3ArrayList<String> owner = new M3ArrayList<>(8);
        String shared = new String("payload");
        owner.add(shared);
        owner.add(null);
        Iterator<String> stable = owner.iterator();
        Object[] previous = backing(owner);
        int epoch = owner.m3StructureEpoch();
        owner.ensureCapacity(0);
        owner.ensureCapacity(8);
        assertThrows(IllegalArgumentException.class, () -> owner.ensureCapacity(-1));
        assertSame(previous, backing(owner));
        assertEquals(epoch, owner.m3StructureEpoch());
        assertSame(shared, stable.next());
        Iterator<String> invalidated = owner.iterator();
        owner.ensureCapacity(65);
        assertNotSame(previous, backing(owner));
        assertEquals(65, owner.capacity());
        assertEquals(epoch + 1, owner.m3StructureEpoch());
        assertThrows(ConcurrentModificationException.class, invalidated::next);
        assertSame(shared, owner.get(0));
        assertNull(owner.get(1));
        zeroTail(owner);
    }

    @Test void cloneDetachesAtLogicalSizeAndPreservesCallerPayloadIdentity() throws Exception {
        for (int size : new int[] {0, 1, 3, 65}) {
            M3ArrayList<Object> owner = new M3ArrayList<>(size + 17);
            Object shared = new Object();
            for (int slot = 0; slot < size; slot++) {
                owner.add(slot % 3 == 0 ? null : shared);
            }
            M3ArrayList<Object> copy = owner.clone();
            assertNotSame(backing(owner), backing(copy));
            assertEquals(size, copy.capacity(), "clone retains logical size, not source spare capacity");
            assertEquals(0, copy.m3StructureEpoch());
            for (int slot = 0; slot < size; slot++) {
                assertSame(owner.get(slot), copy.get(slot));
            }
            copy.add(shared);
            assertEquals(size, owner.size());
            if (size != 0) {
                copy.set(0, shared);
                assertNull(owner.get(0));
            }
            zeroTail(owner);
        }
    }

    @Test void objectToArrayCreatesDetachedExactShallowNativeStorage() throws Exception {
        M3ArrayList<Object> owner = new M3ArrayList<>(17);
        Object first = new Object();
        owner.add(first);
        owner.add(null);
        owner.add(first);
        Object[] output = owner.toArray();
        assertEquals(Object[].class, output.getClass());
        assertEquals(owner.size(), output.length);
        assertNotSame(backing(owner), output);
        assertSame(first, output[0]);
        assertSame(first, output[2]);
        output[0] = null;
        assertSame(first, owner.get(0));
        M3ArrayList<Object> empty = new M3ArrayList<>(0);
        assertNotSame(backing(empty), empty.toArray());
        assertNotSame(empty.toArray(), empty.toArray());
    }

    @Test void typedToArrayKeepsCovarianceTargetIdentityAndPartialFailureEffects() {
        String shared = new String("value");
        M3ArrayList<String> owner = new M3ArrayList<>(Arrays.asList(shared, null, shared));
        String[] small = owner.toArray(new String[0]);
        assertEquals(String[].class, small.getClass());
        assertSame(shared, small[0]);
        String[] exact = new String[3];
        assertSame(exact, owner.toArray(exact));
        String[] longer = {"old", "old", "old", "terminator", "untouched"};
        assertSame(longer, owner.toArray(longer));
        assertArrayEquals(new String[] {shared, null, shared, null, "untouched"}, longer);
        assertThrows(NullPointerException.class, () -> owner.toArray((String[]) null));
        assertThrows(ArrayStoreException.class, () -> owner.toArray(new Integer[0]));
        M3ArrayList<Object> mixed = new M3ArrayList<>(List.of("first", 7, "last"));
        ArrayList<Object> oracle = new ArrayList<>(mixed);
        String[] actual = {"a", "b", "c", "d", "e"};
        String[] expected = actual.clone();
        assertThrows(ArrayStoreException.class, () -> mixed.toArray(actual));
        assertThrows(ArrayStoreException.class, () -> oracle.toArray(expected));
        assertArrayEquals(expected, actual, "System.arraycopy retains its donor-compatible partial-write behavior");
        assertEquals(List.of("first", 7, "last"), mixed);
    }

    @Test void aliasedBulkAddsKeepSelfAndSublistSnapshotsInEncounterOrder() throws Exception {
        M3ArrayList<Integer> self = new M3ArrayList<>(List.of(0, 1, 2, 3, 4, 5));
        self.addAll(2, self);
        assertEquals(List.of(0, 1, 0, 1, 2, 3, 4, 5, 2, 3, 4, 5), self);
        M3ArrayList<Integer> view = new M3ArrayList<>(List.of(0, 1, 2, 3, 4, 5));
        view.addAll(1, view.subList(2, 5));
        assertEquals(List.of(0, 2, 3, 4, 1, 2, 3, 4, 5), view);
        zeroTail(self);
        zeroTail(view);
    }

    private static byte[] serialize(Object object) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(object);
        }
        return bytes.toByteArray();
    }

    private static Object deserialize(byte[] bytes) throws Exception {
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return input.readObject();
        }
    }

    private static Path serializationFixture() {
        String configured = System.getProperty(SERIALIZATION_FIXTURE);
        assertNotNull(configured, "explicit serialization fixture path is required");
        assertFalse(configured.isBlank(), "serialization fixture path cannot be blank");
        Path fixture = Path.of(configured);
        assertTrue(fixture.isAbsolute(), "caller must supply an admitted absolute fixture output/input path");
        return fixture.normalize();
    }

    private static Path negativeFixture(Path positive) {
        return positive.resolveSibling(positive.getFileName() + ".negative");
    }

    private static void writeBoundFixture(Path path, byte[] bytes) throws Exception {
        Files.createDirectories(path.getParent());
        assertFalse(Files.isSymbolicLink(path), "fixture cannot redirect its output");
        if (!Files.exists(path)) {
            Files.write(path, bytes, StandardOpenOption.CREATE_NEW);
        }
        assertArrayEquals(bytes, Files.readAllBytes(path), "existing fixture must match the actual serialized owner");
    }

    @Test void serializationRecreatesIndependentNativeBackingAndBindsBothForkFixtures() throws Exception {
        M3ArrayList<String> owner = new M3ArrayList<>(17);
        String shared = new String("shared serialized payload");
        owner.add(shared);
        owner.add(null);
        owner.add(shared);
        byte[] bytes = serialize(owner);
        Object decoded = deserialize(bytes);
        assertTrue(decoded instanceof M3ArrayList<?>);
        M3ArrayList<?> back = (M3ArrayList<?>) decoded;
        assertEquals(owner, back);
        assertEquals(owner.size(), back.capacity());
        assertNotSame(backing(owner), backing(back));
        assertSame(back.get(0), back.get(2), "shared serialized payload aliases remain shared");
        assertNotSame(shared, back.get(0));
        assertNull(back.get(1));
        assertEquals(0, back.m3StructureEpoch());
        assertEquals(0L, back.m3ValueEpoch());
        back.clear();
        assertEquals(3, owner.size());
        zeroTail(back);
        Path positive = serializationFixture();
        writeBoundFixture(positive, bytes);
        M3ArrayList<String> invalid = owner.clone();
        Field size = M3ArrayList.class.getDeclaredField("size");
        size.setAccessible(true);
        size.setInt(invalid, -1); // serialize an actual owner with an invalid size, without manufacturing a protocol
        byte[] negative = serialize(invalid);
        assertThrows(InvalidObjectException.class, () -> deserialize(negative));
        writeBoundFixture(negativeFixture(positive), negative);
    }

    /** Run in a fresh JVM without m3.arrays.native.path after the normal run has bound the two fixtures. */
    @Test @Tag("native-missing")
    void missingNativeProviderPreservesClassInitializationAndValidationOrder() throws Exception {
        String configured = System.getProperty("m3.arrays.native.path");
        assertTrue(configured == null || configured.isBlank(), "missing-provider discriminator needs no library path");
        assertFalse(M3ArrayNative.isLoaded(), "missing-provider discriminator needs a fresh JVM");
        Class.forName(M3ArrayList.class.getName(), true, M3ArrayList.class.getClassLoader());
        assertFalse(M3ArrayNative.isLoaded(), "class initialization alone cannot allocate or load JNI");
        assertThrows(IllegalArgumentException.class, () -> new M3ArrayList<>(-1));
        assertThrows(NullPointerException.class, () -> new M3ArrayList<>((Collection<Object>) null));
        List<String> calls = new ArrayList<>();
        RuntimeException callbackFailure = new IllegalStateException("foreign toArray failed");
        Collection<Object> failing = new AbstractCollection<>() {
            @Override public int size() { calls.add("size"); return 0; }
            @Override public Iterator<Object> iterator() { calls.add("iterator"); return List.of().iterator(); }
            @Override public Object[] toArray() { calls.add("toArray"); throw callbackFailure; }
        };
        assertSame(callbackFailure, assertThrows(IllegalStateException.class, () -> new M3ArrayList<>(failing)));
        assertEquals(List.of("toArray"), calls);
        assertFalse(M3ArrayNative.isLoaded(), "argument and source callback failures precede native lookup");
        Path positive = serializationFixture();
        byte[] negative = Files.readAllBytes(negativeFixture(positive));
        assertThrows(InvalidObjectException.class, () -> deserialize(negative));
        assertFalse(M3ArrayNative.isLoaded(), "negative serialized size is rejected before native lookup");
        assertThrows(UnsatisfiedLinkError.class, () -> new M3ArrayList<>());
        assertThrows(UnsatisfiedLinkError.class, () -> new M3ArrayList<>(0));
        assertThrows(UnsatisfiedLinkError.class, () -> new M3ArrayList<>(1));
        assertThrows(UnsatisfiedLinkError.class, () -> new M3ArrayList<>(List.of()));
        assertThrows(UnsatisfiedLinkError.class, () -> new M3ArrayList<>(List.of("value")));
        assertThrows(UnsatisfiedLinkError.class, () -> deserialize(Files.readAllBytes(positive)));
        assertFalse(M3ArrayNative.isLoaded(), "no missing-provider path silently falls back");
    }
}
