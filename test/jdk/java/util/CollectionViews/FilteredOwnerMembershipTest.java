/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * @test
 * @summary A filtered Collection preserves its owning Collection's membership relation
 * @run main FilteredOwnerMembershipTest
 */
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.CollectionViews;
import java.util.Iterator;

public final class FilteredOwnerMembershipTest {
    private static int checks;
    private static void check(boolean accepted) {
        checks++;
        if (!accepted) throw new AssertionError("check=" + checks);
    }
    private record Item(int key, boolean selected) {
        @Override public boolean equals(Object other) {
            return other instanceof Item item && key == item.key;
        }
        @Override public int hashCode() { return key; }
    }
    private static final class IdentityCollection extends AbstractCollection<Item> {
        final ArrayList<Item> elements = new ArrayList<>();
        @Override public Iterator<Item> iterator() { return elements.iterator(); }
        @Override public int size() { return elements.size(); }
        @Override public boolean add(Item item) { return elements.add(item); }
        @Override public boolean contains(Object query) {
            for (Item value : elements) if (value == query) return true;
            return false;
        }
        @Override public boolean remove(Object query) {
            for (int i = 0; i < elements.size(); i++) {
                if (elements.get(i) == query) {
                    elements.remove(i);
                    return true;
                }
            }
            return false;
        }
    }
    public static void main(String[] ignored) {
        IdentityCollection owner = new IdentityCollection();
        Item hidden = new Item(5, false);
        Item visible = new Item(5, true);
        Item alias = new Item(5, true);
        owner.add(hidden);
        owner.add(visible);
        Collection<Item> view = CollectionViews.filtered((Collection<Item>) owner, Item::selected);
        check(!owner.contains(alias));
        check(!view.contains(alias));
        check(!view.remove(alias));
        check(owner.size() == 2);
        check(view.contains(visible));
        check(view.remove(visible));
        check(owner.size() == 1 && owner.contains(hidden));
        check(!view.contains(hidden));
        check(!view.remove(hidden));
        owner.add(visible);
        check(view.contains(visible));
        view.clear();
        check(owner.size() == 1 && owner.contains(hidden));
        System.out.println("FilteredOwnerMembershipTest PASS checks=" + checks);
    }
}
