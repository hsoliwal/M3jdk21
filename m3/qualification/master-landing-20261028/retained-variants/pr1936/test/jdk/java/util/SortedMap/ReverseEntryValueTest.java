/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
/* @test
 * @summary Reverse sorted map entries remain coherent after successful setValue
 * @library /java/util/SequencedCollection
 * @run main ReverseEntryValueTest
 * @run main/othervm -Xcheck:jni -XX:+UseSerialGC ReverseEntryValueTest
 */
import java.util.*;
public class ReverseEntryValueTest {
    static int checks;
    static void check(boolean b) { checks++; if (!b) throw new AssertionError("check " + checks); }
    static final RuntimeException REFUSAL = new IllegalStateException("owner refusal");
    static final class Owner extends SimpleSortedMap<String,Integer> {
        boolean refuse;
        Owner(Comparator<String> comparator) { super(comparator); }
        @Override public Integer put(String key, Integer value) {
            if (refuse) throw REFUSAL;
            return super.put(key, value);
        }
    }
    public static void main(String[] args) {
        for (Comparator<String> comparator : List.of(Comparator.<String>naturalOrder(), Comparator.<String>reverseOrder())) {
            Owner owner = new Owner(comparator); owner.put("a", 1); owner.put("b", 2);
            SortedMap<String,Integer> source = owner;
            var iterator = source.reversed().entrySet().iterator();
            Map.Entry<String,Integer> entry = iterator.next();
            check(iterator.hasNext());
            Integer original = entry.getValue();
            check(Objects.equals(entry.setValue(20), original));
            check(Objects.equals(owner.get(entry.getKey()), 20));
            check(Objects.equals(entry.getValue(), 20));
            var expected = new AbstractMap.SimpleEntry<>(entry.getKey(), 20);
            check(entry.equals(expected)); check(expected.equals(entry));
            check(entry.hashCode() == expected.hashCode()); check(entry.toString().equals(expected.toString()));
            check(Objects.equals(entry.setValue(null), 20)); check(entry.getValue() == null);
            check(owner.get(entry.getKey()) == null);
            check(entry.setValue(30) == null); check(Objects.equals(entry.getValue(), 30));
            owner.refuse = true;
            try { entry.setValue(40); throw new AssertionError("missing refusal"); }
            catch (RuntimeException failure) { check(failure == REFUSAL); }
            check(Objects.equals(entry.getValue(), 30)); check(Objects.equals(owner.get(entry.getKey()), 30));
        }
        System.out.println("ReverseEntryValueTest checks=" + checks);
    }
}
