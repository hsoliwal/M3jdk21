/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.collections.M3CountedBag;
import com.m3.collections.M3IndexMaskCompactor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;

public final class CountedBagIteratorProof {
    private static long checks;
    private static long operations;
    private CountedBagIteratorProof() { }
    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
    private static void equal(Object actual, Object expected, String label) {
        check(Objects.equals(actual, expected), label + ": " + actual + " != " + expected);
    }
    private static String failure(Runnable action) {
        try { action.run(); return "NONE"; }
        catch (RuntimeException failure) { return failure.getClass().getName(); }
    }
    private static void preimage() {
        M3CountedBag<String> bag = new M3CountedBag<>();
        bag.add("A"); bag.add("B");
        Iterator<String> iterator = bag.iterator();
        String returned = iterator.next();
        boolean more = iterator.hasNext();
        iterator.hasNext();
        iterator.remove();
        System.out.println("{\"case\":\"last-delivered-after-lookahead\",\"returned\":\""
            + returned + "\",\"hasNext\":" + more + ",\"A\":" + bag.count("A")
            + ",\"B\":" + bag.count("B") + ",\"contractSatisfied\":"
            + (bag.count("A") == 0 && bag.count("B") == 1) + "}");
    }
    private static void boundaryCases() {
        for (String first : new String[]{null, "A"}) {
            for (int count = 1; count <= 4; count++) {
                M3CountedBag<String> bag = new M3CountedBag<>();
                bag.addOccurrences(first, count); bag.addOccurrences("B", 3);
                Iterator<String> iterator = bag.iterator();
                equal(failure(iterator::remove), IllegalStateException.class.getName(), "remove before next");
                for (int i = 0; i < count; i++) equal(iterator.next(), first, "first group");
                for (int i = 0; i < 7; i++) check(iterator.hasNext(), "repeat lookahead");
                iterator.remove();
                equal(bag.count(first), count - 1, "removed delivered occurrence");
                equal(bag.count("B"), 3, "lookahead group unchanged");
                equal(failure(iterator::remove), IllegalStateException.class.getName(), "double remove");
                for (int i = 0; i < 3; i++) equal(iterator.next(), "B", "lookahead still delivered");
                check(!iterator.hasNext(), "exhaustion");
                equal(failure(iterator::next), NoSuchElementException.class.getName(), "failed next");
                iterator.remove();
                equal(bag.count("B"), 2, "failed next retains remove eligibility");
                equal(bag.totalOccurrences(), (long) count + 1, "total after both removes");
            }
        }
        String canonical = new String("same");
        String equalKey = new String("same");
        M3CountedBag<String> identities = new M3CountedBag<>();
        identities.addOccurrences(canonical, 1); identities.addOccurrences(equalKey, 1); identities.add("other");
        Iterator<String> identityIterator = identities.iterator();
        check(identityIterator.next() == canonical, "first key identity");
        check(identityIterator.next() == canonical, "equal key retains representative");
        check(identityIterator.hasNext(), "identity lookahead");
        identityIterator.remove();
        equal(identities.count(equalKey), 1, "equal key multiplicity removal");
        equal(identities.count("other"), 1, "other identity survives");
        check(identityIterator.next() == "other", "other delivered identity");
        for (int mutation = 0; mutation < 4; mutation++) {
            for (int operation = 0; operation < 3; operation++) {
                M3CountedBag<String> bag = new M3CountedBag<>();
                bag.addOccurrences("A", 2); bag.add("B");
                Iterator<String> iterator = bag.iterator(); iterator.next();
                if (mutation == 0) bag.add("A");
                else if (mutation == 1) bag.remove("A");
                else if (mutation == 2) bag.setOccurrences("B", 3);
                else bag.clear();
                Runnable action = operation == 0 ? () -> iterator.hasNext()
                    : operation == 1 ? () -> iterator.next() : iterator::remove;
                equal(failure(action), ConcurrentModificationException.class.getName(), "count failfast");
            }
        }
        M3CountedBag<String> noOp = new M3CountedBag<>();
        noOp.add("A"); noOp.add("B");
        Iterator<String> iterator = noOp.iterator();
        long revision = noOp.revision();
        noOp.addOccurrences("A", 0); noOp.removeOccurrences("A", 0);
        noOp.setOccurrences("A", 1); noOp.removeOccurrences("absent", 9);
        equal(noOp.revision(), revision, "no-op revision");
        equal(iterator.next(), "A", "no-op iterator valid");
        iterator.hasNext(); iterator.remove(); equal(noOp.count("B"), 1, "no-op lookahead remove");
    }
    private static Object outcome(Iterator<String> iterator, int operation) {
        try {
            if (operation == 0) return iterator.hasNext();
            if (operation == 1) return Arrays.asList("VALUE", iterator.next());
            iterator.remove(); return "REMOVED";
        } catch (RuntimeException failure) { return failure.getClass().getName(); }
    }
    private static void compareOwner(M3CountedBag<String> bag, List<String> reference) {
        equal(new ArrayList<>(bag), reference, "remaining occurrence order");
        equal(bag.totalOccurrences(), (long) reference.size(), "reference total");
        equal(bag.size(), reference.size(), "reference size");
        for (String key : new String[]{null, "A", "B", "C", "D"}) {
            int count = 0;
            for (String element : reference) if (Objects.equals(key, element)) count++;
            equal(bag.count(key), count, "reference count " + key);
        }
    }
    private static void randomized() {
        Random random = new Random(0x434f554e544544L);
        String[] keys = {null, "A", "B", "C", "D"};
        for (int trial = 0; trial < 2000; trial++) {
            M3CountedBag<String> bag = new M3CountedBag<>();
            List<String> reference = new ArrayList<>();
            for (String key : keys) {
                int count = random.nextInt(5);
                bag.addOccurrences(key, count);
                for (int i = 0; i < count; i++) reference.add(key);
            }
            Iterator<String> candidate = bag.iterator();
            Iterator<String> oracle = reference.iterator();
            for (int step = 0; step < 100; step++) {
                int operation = random.nextInt(3);
                equal(outcome(candidate, operation), outcome(oracle, operation), "protocol trial " + trial + " step " + step);
                operations++;
                compareOwner(bag, reference);
            }
        }
    }
    private static void maskIntegration(boolean nativeRequired) {
        M3IndexMaskCompactor.Backend backend = nativeRequired
            ? M3IndexMaskCompactor.Backend.NATIVE_REQUIRED : M3IndexMaskCompactor.Backend.JAVA;
        M3CountedBag<String> bag = new M3CountedBag<>();
        bag.addOccurrences(null, 2); bag.addOccurrences("A", 3); bag.addOccurrences("B", 4); bag.addOccurrences("C", 5);
        byte[] states = {0, 1, (byte) 128, (byte) 255};
        for (int required = 0; required < 256; required++) {
            byte[] original = states.clone();
            M3CountedBag<String> selected = bag.selectedDistinct(states, required, backend);
            String[] keys = {null, "A", "B", "C"};
            long total = 0;
            for (int i = 0; i < keys.length; i++) {
                int expected = ((states[i] & 255) & required) == required ? i + 2 : 0;
                equal(selected.count(keys[i]), expected, "mask key");
                total += expected;
            }
            equal(selected.totalOccurrences(), total, "mask total");
            check(Arrays.equals(states, original), "states unchanged");
            selected.clear();
            equal(bag.totalOccurrences(), 14L, "selected owner detached");
        }
        Iterator<String> iterator = bag.iterator(); iterator.next(); iterator.next();
        check(iterator.hasNext(), "native candidate lookahead");
        iterator.remove();
        M3CountedBag<String> selected = bag.selectedDistinct(states, 128, backend);
        equal(selected.count(null), 0, "masked null");
        equal(selected.count("A"), 0, "masked A");
        equal(selected.count("B"), 4, "masked B");
        equal(selected.count("C"), 5, "masked C");
        equal(bag.count(null), 1, "delivered null removed before mask");
        equal(bag.count("A"), 3, "prefetched A preserved before mask");
    }
    public static void main(String[] arguments) {
        if (arguments.length == 1 && arguments[0].equals("preimage")) { preimage(); return; }
        boundaryCases();
        randomized();
        boolean nativeRequired = arguments.length == 1 && arguments[0].equals("native");
        maskIntegration(nativeRequired);
        System.out.println("{\"status\":\"PASS\",\"checks\":" + checks
            + ",\"protocolOperations\":" + operations + ",\"maskBackend\":\""
            + (nativeRequired ? "NATIVE_REQUIRED" : "JAVA") + "\"}");
    }
}
