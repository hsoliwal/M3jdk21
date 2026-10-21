/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.collections.interop.M3ApacheBag;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;

/** Independent review probe: exact current owner versus pinned donor and occurrence oracle. */
public final class ApacheBagContractProbe {
    private static void same(Object expected, Object actual, String label) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
        }
    }

    private static boolean operation(Bag<Integer> bag, int code, Integer key,
                                     int count, List<Integer> source) {
        return switch (code) {
            case 0 -> bag.add(key, count);
            case 1 -> bag.remove(key, count);
            case 2 -> bag.remove(key);
            case 3 -> bag.addAll(source);
            case 4 -> bag.removeAll(source);
            case 5 -> bag.retainAll(source);
            default -> throw new AssertionError(code);
        };
    }

    /** Uses occurrence consumption, independent of the M3 counted-directory algorithm. */
    private static boolean operation(List<Integer> occurrences, int code, Integer key,
                                     int count, List<Integer> source) {
        return switch (code) {
            case 0 -> {
                boolean distinct = count > 0 && !occurrences.contains(key);
                for (int i = 0; i < count; i++) occurrences.add(key);
                yield distinct;
            }
            case 1 -> {
                boolean changed = false;
                for (int i = 0; i < count; i++) changed |= occurrences.remove(key);
                yield changed;
            }
            case 2 -> occurrences.removeIf(value -> Objects.equals(key, value));
            case 3 -> {
                boolean distinct = false;
                for (Integer value : source) {
                    distinct |= !occurrences.contains(value);
                    occurrences.add(value);
                }
                yield distinct;
            }
            case 4 -> {
                boolean changed = false;
                for (Integer value : source) changed |= occurrences.remove(value);
                yield changed;
            }
            case 5 -> {
                List<Integer> budget = new ArrayList<>(source);
                yield occurrences.removeIf(value -> !budget.remove(value));
            }
            default -> throw new AssertionError(code);
        };
    }

    private static void retainMatrix() {
        int cases = 0, divergences = 0;
        System.out.println("owned\tincoming\tm3_count\tdonor_count\tm3_changed\tdonor_changed");
        for (String key : Arrays.asList("x", null)) {
            for (int owned = 0; owned <= 6; owned++) {
                for (int incoming = 0; incoming <= 6; incoming++) {
                    for (int inputType = 0; inputType < 3; inputType++) {
                        M3ApacheBag<String> m3 = new M3ApacheBag<>();
                        HashBag<String> donor = new HashBag<>();
                        m3.add(key, owned); donor.add(key, owned);
                        Collection<String> input = switch (inputType) {
                            case 0 -> new ArrayList<>();
                            case 1 -> new HashBag<>();
                            case 2 -> new M3ApacheBag<>();
                            default -> throw new AssertionError(inputType);
                        };
                        for (int i = 0; i < incoming; i++) input.add(key);
                        boolean m3Changed = m3.retainAll(input);
                        boolean donorChanged = donor.retainAll(input);
                        int contractCount = Math.min(owned, incoming);
                        int donorCount = incoming > 0 && incoming <= owned ? incoming : 0;
                        same(contractCount, m3.getCount(key), "M3 declared contract");
                        same(contractCount, m3.size(), "M3 size");
                        same(owned != contractCount, m3Changed, "M3 modification result");
                        same(donorCount, donor.getCount(key), "pinned donor branch");
                        same(donorCount, donor.size(), "donor size");
                        same(owned != donorCount, donorChanged, "donor modification result");
                        if (contractCount != donorCount) divergences++;
                        if (key != null && inputType == 0 && owned == 2 && incoming >= 1 && incoming <= 3) {
                            System.out.println(owned + "\t" + incoming + "\t" + contractCount + "\t"
                                + donorCount + "\t" + m3Changed + "\t" + donorChanged);
                        }
                        cases++;
                    }
                }
            }
        }
        same(294, cases, "matrix case count");
        same(90, divergences, "exact donor divergence count");
        System.out.println("APACHE_RETAIN_MATRIX_PASS cases=" + cases + " explicitDonorDivergences=" + divergences);
    }

    private static void originalSeedFailure() {
        Bag<Integer> donor = new HashBag<>(), m3 = new M3ApacheBag<>();
        Random random = new Random(20261018L);
        for (int step = 0; step < 6000; step++) {
            Integer key = random.nextInt(17) == 0 ? null : random.nextInt(43) - 21;
            int count = random.nextInt(6), code = random.nextInt(6);
            List<Integer> source = code >= 3 ? Arrays.asList(key, key, random.nextInt(11)) : List.of();
            String before = donor.toString();
            boolean donorReturn = operation(donor, code, key, count, source);
            boolean m3Return = operation(m3, code, key, count, source);
            if (donorReturn != m3Return || !donor.equals(m3)) {
                same(702, step, "original seeded first divergence");
                same(5, code, "first divergence operation");
                same(0, donor.size(), "original donor size");
                same(2, m3.size(), "original M3 size");
                System.out.println("APACHE_ORIGINAL_SEED_DIVERGENCE step=" + step + " op=" + code
                    + " key=" + key + " input=" + source + " before=" + before
                    + " donorAfter=" + donor + " m3After=" + m3);
                return;
            }
        }
        throw new AssertionError("original donor oracle no longer exhibits the pinned divergence");
    }

    private static void fullBagContractSeed() {
        List<Integer> occurrences = new ArrayList<>();
        M3ApacheBag<Integer> m3 = new M3ApacheBag<>();
        Random random = new Random(20261018L);
        int[] operations = new int[6];
        int checks = 0;
        for (int step = 0; step < 6000; step++) {
            Integer key = random.nextInt(17) == 0 ? null : random.nextInt(43) - 21;
            int count = random.nextInt(6), code = random.nextInt(6);
            List<Integer> source = code >= 3 ? Arrays.asList(key, key, random.nextInt(11)) : List.of();
            same(operation(occurrences, code, key, count, source), operation(m3, code, key, count, source),
                "return step=" + step);
            same(occurrences.size(), m3.size(), "size step=" + step);
            same(new HashSet<>(occurrences), m3.uniqueSet(), "distinct step=" + step);
            for (Object item : new HashSet<>(occurrences)) {
                int frequency = 0;
                for (Integer value : occurrences) if (Objects.equals(item, value)) frequency++;
                same(frequency, m3.getCount(item), "count step=" + step);
                checks++;
            }
            HashBag<Integer> expectedValue = new HashBag<>(occurrences);
            same(expectedValue, m3, "equality step=" + step);
            same(m3, expectedValue, "reverse equality step=" + step);
            same(expectedValue.hashCode(), m3.hashCode(), "hash step=" + step);
            checks += 6;
            operations[code]++;
        }
        System.out.println("APACHE_BAG_CONTRACT_SEED_PASS operations=6000 checks=" + checks
            + " perOperation=" + Arrays.toString(operations));
    }

    public static void main(String[] args) {
        retainMatrix();
        originalSeedFailure();
        fullBagContractSeed();
    }
}
