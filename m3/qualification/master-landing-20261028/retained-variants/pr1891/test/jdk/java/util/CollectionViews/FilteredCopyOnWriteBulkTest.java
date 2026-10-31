/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
/*
 * @test
 * @summary M3JDK21 filtered CopyOnWrite bulk operations preserve backing membership and order
 * @run main/othervm FilteredCopyOnWriteBulkTest
 */
import java.util.ArrayList;
import java.util.Collection;
import java.util.CollectionViews;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

public class FilteredCopyOnWriteBulkTest {
    private record Item(int id, boolean visible) { }

    private static long checks;

    private static void check(boolean pass, String why) {
        checks++;
        if (!pass) throw new AssertionError(why + " check=" + checks);
    }

    private static void checkSameEntries(List<Item> expected, List<Item> actual, String why) {
        check(expected.size() == actual.size(), why + " length");
        for (int i = 0; i < expected.size(); i++) {
            check(expected.get(i) == actual.get(i), why + " item " + i);
        }
    }

    private static void listBulkOperations() {
        Item hidden = new Item(1, false);
        Item selected = new Item(1, true);
        Item other = new Item(2, true);
        Item hiddenOther = new Item(3, false);
        CopyOnWriteArrayList<Item> owner = new CopyOnWriteArrayList<>(
                List.of(hidden, selected, other, hiddenOther));
        Collection<Item> view = CollectionViews.filtered(owner, Item::visible);
        check(view.size() == 2, "initial visible size");
        check(view.removeIf(e -> e.id() == 2), "removeIf");
        checkSameEntries(List.of(hidden, selected, hiddenOther), owner, "removeIf state");
        check(view.removeAll(List.of(selected)), "removeAll");
        checkSameEntries(List.of(hidden, hiddenOther), owner, "removeAll state");
        owner.add(other);
        check(view.retainAll(List.of(selected)), "retainAll");
        checkSameEntries(List.of(hidden, hiddenOther), owner, "retainAll state");
        owner.add(selected);
        view.clear();
        checkSameEntries(List.of(hidden, hiddenOther), owner, "clear only visible");
        check(!view.removeIf(e -> true), "empty selected view no-op");

        owner.add(selected);
        view.removeIf(e -> {
            if (!e.visible()) throw new AssertionError("hidden condition evaluated");
            return true;
        });
        checkSameEntries(List.of(hidden, hiddenOther), owner, "hidden condition skipped");
    }

    private static void setBulkOperations() {
        Item hidden = new Item(3, false);
        Item first = new Item(4, true);
        Item second = new Item(5, true);
        CopyOnWriteArraySet<Item> owner = new CopyOnWriteArraySet<>(
                List.of(hidden, first, second));
        Set<Item> view = CollectionViews.filtered((Set<Item>) owner, Item::visible);
        check(view.removeIf(e -> e.id() == 4), "set removeIf");
        check(owner.contains(hidden) && owner.contains(second) && !owner.contains(first),
                "set removeIf state");
        view.clear();
        check(owner.size() == 1 && owner.contains(hidden), "set clear hidden retention");
        check(!view.removeAll(List.of(first)), "set no-op");
    }

    private static void exactTypeOnly() {
        // Ordinary mutable iterator owners retain their original removal path.
        ArrayList<Item> source = new ArrayList<>(
                List.of(new Item(1, false), new Item(2, true), new Item(3, true)));
        Collection<Item> view = CollectionViews.filtered(source, Item::visible);
        check(view.removeIf(e -> e.id() == 2), "ArrayList iterator removal");
        check(source.size() == 2 && !source.get(0).visible()
                && source.get(1).id() == 3, "ArrayList order");

        CopyOnWriteArrayList<Item> own = new CopyOnWriteArrayList<>(
                List.of(new Item(5, false), new Item(6, true)));
        Collection<Item> protectedView = CollectionViews.filtered(own, Item::visible);
        Iterator<Item> cursor = protectedView.iterator();
        cursor.next();
        try {
            cursor.remove();
            throw new AssertionError("filtered iterator removal unexpectedly allowed");
        } catch (UnsupportedOperationException expected) {
            check(own.size() == 2, "iterator remains read-only");
        }
    }

    private static void randomizedDifferential() {
        Random random = new Random(0x6d3c08L);
        for (int trial = 0; trial < 10_000; trial++) {
            List<Item> oracle = new ArrayList<>();
            int length = random.nextInt(65);
            for (int i = 0; i < length; i++) {
                oracle.add(new Item(random.nextInt(12), random.nextBoolean()));
            }
            CopyOnWriteArrayList<Item> owner = new CopyOnWriteArrayList<>(oracle);
            Collection<Item> view = CollectionViews.filtered(owner, Item::visible);
            int mode = random.nextInt(4);
            int chosen = random.nextInt(12);
            boolean expected = false;
            Iterator<Item> it = oracle.iterator();
            while (it.hasNext()) {
                Item row = it.next();
                boolean match = switch (mode) {
                    case 0 -> row.id() == chosen;
                    case 1 -> row.id() != chosen;
                    case 2 -> true;
                    default -> false;
                };
                if (row.visible() && match) {
                    it.remove();
                    expected = true;
                }
            }
            boolean actual = switch (mode) {
                case 0 -> view.removeIf(row -> row.id() == chosen);
                case 1 -> view.retainAll(List.of(new Item(chosen, true)));
                case 2 -> view.removeIf(row -> true);
                default -> view.removeAll(List.of());
            };
            check(actual == expected, "random mutation flag trial=" + trial);
            checkSameEntries(oracle, owner, "random trial=" + trial);
        }
    }

    public static void main(String[] args) {
        listBulkOperations();
        setBulkOperations();
        exactTypeOnly();
        randomizedDifferential();
        System.out.println("PASS FilteredCopyOnWriteBulkTest checks=" + checks);
    }
}
