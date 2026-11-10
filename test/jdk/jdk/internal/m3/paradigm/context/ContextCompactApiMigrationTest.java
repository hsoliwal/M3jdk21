// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.paradigm.context;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

final class ContextCompactApiMigrationTest {
    enum Mode { ZERO, ONE, TWO }
    enum Flag {
        F00,F01,F02,F03,F04,F05,F06,F07,F08,F09,F10,F11,F12,F13,F14,F15,
        F16,F17,F18,F19,F20,F21,F22,F23,F24,F25,F26,F27,F28,F29,F30,F31,
        F32,F33,F34,F35,F36,F37,F38,F39,F40,F41,F42,F43,F44,F45,F46,F47,
        F48,F49,F50,F51,F52,F53,F54,F55,F56,F57,F58,F59,F60,F61,F62,F63
    }

    @Test void staticBitsMatchEnumSetIncludingSignBitAndIdempotence() {
        long bits = 0;
        EnumSet<Flag> expected = EnumSet.noneOf(Flag.class);
        for (Flag flag : Flag.values()) {
            long next = EnumBits.add(bits, flag);
            assertEquals(expected.add(flag), bits != next);
            assertEquals(next, EnumBits.add(next, flag));
            bits = next;
        }
        assertEquals(-1L, bits);
        assertEquals(expected, EnumBits.toSet(Flag.class, bits));
        for (Flag flag : Flag.values()) {
            long next = EnumBits.remove(bits, flag);
            assertEquals(expected.remove(flag), bits != next);
            bits = next;
        }
        assertEquals(0, bits);
        assertThrows(IllegalArgumentException.class, () -> EnumBits.toSet(Mode.class, 8L));
    }

    @Test void optionalVectorsPreserveZeroCodesAcrossPrimitiveWidthsAndSnapshots() {
        for (int maximum : new int[] {2, 255, 256, 65535, 65536, Integer.MAX_VALUE}) {
            EnumCodec<Mode> codec = EnumCodec.stable(Mode.class,
                    value -> value == Mode.TWO ? maximum : value.ordinal());
            var empty = EnumVector.empty(codec, 64);
            var values = empty.with(0, Mode.ZERO).with(63, Mode.TWO);
            assertTrue(empty.get(0).isEmpty());
            assertEquals(Mode.ZERO, values.get(0).orElseThrow());
            assertEquals(Mode.TWO, values.get(63).orElseThrow());
            assertEquals(2, values.size());
            assertSame(values, values.with(63, Mode.TWO));
            var removed = values.without(63);
            assertTrue(removed.get(63).isEmpty());
            assertEquals(Mode.TWO, values.get(63).orElseThrow());
            assertSame(removed, removed.without(63));
        }
        assertThrows(IllegalArgumentException.class,
                () -> EnumVector.empty(EnumCodec.of(Mode.class), 65));
        assertThrows(IndexOutOfBoundsException.class,
                () -> EnumVector.empty(EnumCodec.of(Mode.class), 0).get(0));
    }

    @Test void existingOrdinalVectorPreservesLargeNullableEnumCodeLane() {
        var schema = new EnumSchema<>(Mode.class);
        var empty = OrdinalVector.filled(schema.size() + 1, 10000, 0);
        var values = empty.with(9999, schema.code(Mode.TWO));
        assertNull(schema.decode(empty.get(9999)));
        assertEquals(Mode.TWO, schema.decode(values.get(9999)));
        assertNull(schema.decode(values.with(9999, 0).get(9999)));
        assertEquals(10000L, values.logicalPayloadBytes());
        assertThrows(IndexOutOfBoundsException.class, () -> values.get(10000));
    }

    @Test void schemaSeparatesMissingLookupFromRequiredLookupAndDefendsNames() {
        String[] names = {"Aa", "BB", "Role.Speaker", "role.speaker"};
        var schema = new OrdinalSchema(names);
        names[0] = "changed";
        assertEquals(0, schema.ordinal("Aa"));
        assertEquals(1, schema.ordinal("BB"));
        assertEquals(2, schema.requireOrdinal("Role.Speaker"));
        assertEquals(-1, schema.ordinal("ROLE.SPEAKER"));
        assertThrows(IllegalArgumentException.class, () -> schema.requireOrdinal("missing"));
        schema.names()[0] = "also changed";
        assertEquals("Aa", schema.name(0));
        assertThrows(UnsupportedOperationException.class, () -> schema.namesView().set(0, "changed"));
        assertThrows(IllegalArgumentException.class, () -> new OrdinalSchema(" name"));
        assertThrows(IllegalArgumentException.class, () -> new OrdinalSchema("n".repeat(257)));
        assertThrows(IllegalArgumentException.class, () -> new OrdinalSchema("same", "same"));
    }

    @Test void generalSchemaRetains65535SlotsAndHotLanesReject65() {
        String[] names = java.util.stream.IntStream.range(0, 65535)
                .mapToObj(i -> "k" + i).toArray(String[]::new);
        var general = new OrdinalSchema(names);
        assertEquals(65534, general.requireOrdinal("k65534"));
        assertThrows(IllegalArgumentException.class, general::fullMask);
        assertThrows(IllegalArgumentException.class, () -> OrdinalSchema.hot(Arrays.copyOf(names, 65)));
        assertThrows(IllegalArgumentException.class, () -> new OrdinalSchema(new String[65536]));
        assertThrows(IllegalArgumentException.class, () -> OrdinalLongMap.empty(general));
        assertEquals(-1L, OrdinalSchema.hot(Arrays.copyOf(names, 64)).fullMask());
    }

    @Test void sparseLongMapsPreserveRankOrderSignBitAndDefensiveSnapshots() {
        String[] names = java.util.stream.IntStream.range(0, 64).mapToObj(i -> "k" + i).toArray(String[]::new);
        var schema = OrdinalSchema.hot(names);
        var empty = OrdinalLongMap.empty(schema);
        var values = empty.with(63, Long.MIN_VALUE).with(0, 0).with(17, -1);
        assertEquals(0, empty.size());
        assertEquals(Long.MIN_VALUE | (1L << 17) | 1L, values.presentBits());
        assertArrayEquals(new long[] {0, -1, Long.MIN_VALUE}, values.valuesCopy());
        values.valuesCopy()[0] = 999;
        assertEquals(0, values.getOrDefault(0, 9));
        assertEquals(9, values.getOrDefault("missing", 9));
        assertSame(values, values.with(63, Long.MIN_VALUE));
        assertSame(values, values.without(1));
        assertEquals(2, values.without(17).size());
        assertEquals(3, values.size());
        assertThrows(IllegalArgumentException.class, () -> values.with("missing", 1));
        assertThrows(IndexOutOfBoundsException.class, () -> values.getOrDefault(64, 0));
    }

    @Test void sparseLongMapMatchesIndependentMapAcrossPersistentUpdates() {
        String[] names = java.util.stream.IntStream.range(0, 64).mapToObj(i -> "k" + i).toArray(String[]::new);
        var actual = OrdinalLongMap.empty(OrdinalSchema.hot(names));
        Map<Integer, Long> expected = new HashMap<>();
        Random random = new Random(137);
        for (int step = 0; step < 2000; step++) {
            int ordinal = random.nextInt(64);
            var prior = actual;
            long previous = actual.getOrDefault(ordinal, 91);
            if (random.nextBoolean()) {
                long value = random.nextLong();
                actual = actual.with(ordinal, value); expected.put(ordinal, value);
            } else {
                actual = actual.without(ordinal); expected.remove(ordinal);
            }
            assertEquals(previous, prior.getOrDefault(ordinal, 91));
            assertEquals(expected.size(), actual.size());
            for (int i = 0; i < 64; i++) assertEquals(expected.getOrDefault(i, 91L).longValue(), actual.getOrDefault(i, 91));
        }
    }

    @Test void strictMutableConsumersUseRequireOrdinalWithSentinelSchema() {
        var schema = new OrdinalSchema("known");
        var bytes = new OrdinalByteMap(schema);
        var refs = new OrdinalRefMap<String>(schema);
        assertThrows(IllegalArgumentException.class, () -> bytes.put("missing", 1));
        assertThrows(IllegalArgumentException.class, () -> bytes.getOrDefault("missing", 1));
        assertThrows(IllegalArgumentException.class, () -> refs.put("missing", "x"));
        assertThrows(IllegalArgumentException.class, () -> refs.get("missing"));
        bytes.put("known", 0); refs.put("known", "v");
        assertEquals(0, bytes.getOrDefault("known", 1));
        assertEquals("v", refs.get("known"));
    }

    @Test void packedFrameUsesExistingMutableLongSlots() {
        var frame = new PackedFrame(OrdinalSchema.hot("counter"));
        frame.longs().set("counter", Long.MIN_VALUE);
        assertEquals(Long.MIN_VALUE, frame.longs().get("counter").orElseThrow());
        assertTrue(frame.longs().remove(0));
        assertTrue(frame.longs().get("counter").isEmpty());
        String[] names = java.util.stream.IntStream.range(0, 65)
                .mapToObj(i -> "k" + i).toArray(String[]::new);
        assertThrows(IllegalArgumentException.class,
                () -> new PackedFrame(new OrdinalSchema(names)));
        var general = new OrdinalSchema(names);
        var bytes = new OrdinalByteMap(general);
        var refs = new OrdinalRefMap<String>(general);
        bytes.put("k64", 255); refs.put("k64", "large");
        assertEquals(255, bytes.getOrDefault("k64", 0));
        assertEquals("large", refs.get("k64"));
    }

    @Test void compactOwnersKeepTheirPrimitiveLayouts() {
        assertEquals(List.of(EnumCodec.class, Object.class, long.class), fields(EnumVector.class));
        assertEquals(List.of(OrdinalSchema.class, long.class, long[].class), fields(OrdinalLongMap.class));
        assertEquals(List.of(String[].class, int[].class, int.class), fields(OrdinalSchema.class));
        assertTrue(fields(EnumBits.class).isEmpty());
    }

    private static List<Class<?>> fields(Class<?> owner) {
        return Arrays.stream(owner.getDeclaredFields()).filter(f -> !Modifier.isStatic(f.getModifiers()))
                .map(java.lang.reflect.Field::getType).toList();
    }
}
