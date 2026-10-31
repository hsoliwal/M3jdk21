/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.collections.interop.M3SpringArrayMultiValueMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;
import org.springframework.util.MultiValueMap;
import org.springframework.util.MultiValueMapAdapter;

/** Independent current-source probe; compares failure class and state as well as success. */
public final class SpringExceptionParityProbe {
    private record Outcome(Object value, Class<? extends RuntimeException> failure) { }

    private static Outcome invoke(Supplier<Object> operation) {
        try {
            return new Outcome(operation.get(), null);
        } catch (RuntimeException failure) {
            return new Outcome(null, failure.getClass());
        }
    }

    private static void same(Object expected, Object actual, String label) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
        }
    }

    private static Object operation(MultiValueMap<Integer, Integer> map, int code,
                                    Integer key, Integer value, List<Integer> putList,
                                    boolean clear) {
        return switch (code) {
            case 0 -> { map.add(key, value); yield null; }
            case 1 -> { map.set(key, value); yield null; }
            case 2 -> { map.addIfAbsent(key, value); yield null; }
            case 3 -> map.remove(key);
            case 4 -> { map.addAll(key, Arrays.asList(value, value)); yield null; }
            case 5 -> map.put(key, putList);
            case 6 -> map.asSingleValueMap().put(key, value);
            case 7 -> map.asSingleValueMap().remove(key);
            case 8 -> map.putIfAbsent(key, Arrays.asList(value));
            case 9 -> { map.setAll(Map.of(5, 9)); yield null; }
            case 10 -> map.containsKey(key);
            case 11 -> { if (clear) map.clear(); yield null; }
            case 12 -> {
                Map<Integer, List<Integer>> copy = new LinkedHashMap<>();
                copy.put(key, Arrays.asList(value));
                map.putAll(copy); yield null;
            }
            default -> throw new AssertionError(code);
        };
    }

    public static void main(String[] args) {
        MultiValueMap<Integer, Integer> actual = new M3SpringArrayMultiValueMap<>();
        MultiValueMap<Integer, Integer> expected = new MultiValueMapAdapter<>(new LinkedHashMap<>());
        List<Integer> actualBorrowed = Arrays.asList(1);
        List<Integer> expectedBorrowed = Arrays.asList(1);
        actual.put(2, actualBorrowed); expected.put(2, expectedBorrowed);
        Outcome actualRefusal = invoke(() -> { actual.addAll(2, List.of(3, 4)); return null; });
        Outcome expectedRefusal = invoke(() -> { expected.addAll(2, List.of(3, 4)); return null; });
        same(expectedRefusal, actualRefusal, "borrowed fixed-size list exception parity");
        same(UnsupportedOperationException.class, actualRefusal.failure(), "expected refusal type");
        same(expected, actual, "borrowed fixed-size list state");
        if (actual.get(2) != actualBorrowed || expected.get(2) != expectedBorrowed) {
            throw new AssertionError("caller list alias lost");
        }
        System.out.println("SPRING_MINIMAL_BORROWED_FIXED_LIST_PASS exception=" + actualRefusal.failure().getName());
        actual.clear(); expected.clear();
        Random random = new Random(0x20261017L);
        int refusals = 0;
        for (int step = 0; step < 4500; step++) {
            Integer key = random.nextInt(7) == 0 ? null : random.nextInt(32);
            Integer value = random.nextInt(8) == 0 ? null : random.nextInt(50);
            int code = random.nextInt(13);
            List<Integer> actualPut = null;
            List<Integer> expectedPut = null;
            if (code == 5 && random.nextInt(6) != 0) {
                actualPut = new ArrayList<>(Arrays.asList(value));
                expectedPut = new ArrayList<>(actualPut);
            }
            final List<Integer> a = actualPut, b = expectedPut;
            final boolean clear = step % 47 == 0;
            String before = expected.toString();
            Outcome donor = invoke(() -> operation(expected, code, key, value, b, clear));
            Outcome m3 = invoke(() -> operation(actual, code, key, value, a, clear));
            same(donor, m3, "operation outcome step=" + step + " op=" + code);
            if (donor.failure() != null) {
                refusals++;
                if (refusals == 1) {
                    System.out.println("SPRING_FIRST_SEEDED_REFUSAL step=" + step + " op=" + code
                        + " key=" + key + " value=" + value + " exception=" + donor.failure().getName()
                        + " before=" + before + " after=" + expected);
                }
            }
            same(expected, actual, "map step=" + step);
            same(expected.size(), actual.size(), "size step=" + step);
            same(expected.getFirst(key), actual.getFirst(key), "first step=" + step);
            same(expected.toSingleValueMap(), actual.toSingleValueMap(), "copy step=" + step);
            same(new ArrayList<>(expected.keySet()), new ArrayList<>(actual.keySet()), "order step=" + step);
            same(expected.asSingleValueMap().get(key), actual.asSingleValueMap().get(key), "view step=" + step);
        }
        System.out.println("SPRING_SEEDED_EXCEPTION_PARITY_PASS operations=4500 refusals=" + refusals
                           + " checksPerOperation=7");
    }
}
