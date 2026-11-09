// SPDX-License-Identifier: Apache-2.0
/*
 * @test
 * @summary Stored-occurrence membership and visible physical deletion in CollectionViews
 * @run main StoredRepresentativeOracle
 */
import java.util.CollectionViews;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Random;

public class StoredRepresentativeOracle {
  private record Item(int id, boolean selected) {
    @Override public boolean equals(Object other) {
      return other instanceof Item item && id == item.id;
    }
    @Override public int hashCode() { return id; }
  }
  private static int checks;
  private static void check(boolean ok) {
    checks++;
    if (!ok) throw new AssertionError("check=" + checks);
  }
  private static Collection<Item> filter(Collection<Item> src) {
    return CollectionViews.filtered(src, Item::selected);
  }
  public static void main(String[] ignored) {
    Item hidden = new Item(1, false);
    Item shown = new Item(1, true);
    Item probe = new Item(1, false);
    ArrayList<Item> list = new ArrayList<>(Arrays.asList(hidden, shown));
    Collection<Item> filtered = filter(list);
    check(filtered.contains(probe));
    check(filtered.remove(probe));
    check(list.size() == 1 && list.getFirst() == hidden);
    check(!filtered.contains(probe));
    CopyOnWriteArrayList<Item> cow = new CopyOnWriteArrayList<>(Arrays.asList(hidden, shown));
    Collection<Item> cowView = filter(cow);
    check(cowView.contains(probe));
    check(cowView.remove(probe));
    check(cow.size() == 1 && cow.getFirst() == hidden);
    Set<Item> identity = Collections.newSetFromMap(new IdentityHashMap<>());
    check(identity.add(hidden) && identity.add(shown));
    Set<Item> identityView = CollectionViews.filtered(identity, Item::selected);
    check(!identityView.contains(hidden));
    check(identityView.contains(shown));
    check(!identityView.contains(probe));
    check(!identityView.remove(hidden));
    check(identityView.remove(shown));
    check(identity.contains(hidden) && !identity.contains(shown));
    Set<Item> hash = new HashSet<>();
    hash.add(shown);
    Set<Item> hashView = CollectionViews.filtered(hash, Item::selected);
    check(hashView.contains(probe));
    check(hashView.remove(probe));
    check(hash.isEmpty());
    TreeSet<String> sorted = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    sorted.add("ADMIN");
    Set<String> sortedView = CollectionViews.filtered(sorted, s -> s.equals(s.toLowerCase()));
    check(!sortedView.contains("admin"));
    check(!sortedView.remove("admin"));
    check(sorted.contains("ADMIN"));
    ArrayList<Item> nulls = new ArrayList<>();
    nulls.add(null);nulls.add(hidden);nulls.add(shown);
    Collection<Item> nullable = CollectionViews.filtered(nulls, it -> it == null || it.selected());
    check(nullable.contains(null));
    check(nullable.remove(null));
    check(nulls.size() == 2);
    // Compare live List collection observations with a primitive-index oracle.
    Random rng = new Random(0x4d334cL);
    for (int trial = 0; trial < 1800; trial++) {
      List<Item> original = new ArrayList<>();
      for (int i = 0, n = 1 + rng.nextInt(70); i < n; i++) {
        original.add(new Item(rng.nextInt(12), rng.nextBoolean()));
      }
      Collection<Item> view = filter(original);
      for (int id = 0; id < 12; id++) {
        Item query = new Item(id, false);
        boolean expected = false;
        int index = -1;
        for (int i = 0; i < original.size(); i++) {
          Item stored = original.get(i);
          if (stored.id() == id && stored.selected()) {
            expected = true; index = i; break;
          }
        }
        check(view.contains(query) == expected);
        ArrayList<Item> expectedPhysical = new ArrayList<>(original);
        if (expected) expectedPhysical.remove(index);
        check(view.remove(query) == expected);
        check(original.size() == expectedPhysical.size());
        for (int j = 0; j < original.size(); j++) {
          check(original.get(j) == expectedPhysical.get(j));
        }
      }
    }
    System.out.println("StoredRepresentativeOracle PASS checks=" + checks);
  }
}
