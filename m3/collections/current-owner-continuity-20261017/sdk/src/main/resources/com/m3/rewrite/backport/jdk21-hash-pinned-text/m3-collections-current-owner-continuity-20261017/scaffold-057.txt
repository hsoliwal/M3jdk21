/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
import com.m3.collections.M3CollectionViews;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Same fixture executes against the namespace-only original and recipe output. */
public final class M3WrapperRefinementProbe {
    private M3WrapperRefinementProbe() { }
    public static void main(String[] args) {
        switch (args[0]) {
            case "erased" -> {
                Collection<Integer> owner = new TreeSet<>(List.of(1, 2, 3));
                if (!(M3CollectionViews.filtered(owner, i -> true) instanceof NavigableSet<?>))
                    throw new AssertionError("ERASED_SORTED_CAPABILITY_LOST");
            }
            case "snapshot" -> {
                AtomicInteger calls = new AtomicInteger();
                Collection<Integer> view = M3CollectionViews.filtered(new ArrayList<>(List.of(5, 4, 3, 2, 1)), i -> {
                    calls.incrementAndGet(); return i % 2 != 0;
                });
                if (!M3CollectionViews.sortedSnapshot(view, Comparator.naturalOrder()).equals(List.of(1, 3, 5)))
                    throw new AssertionError("WRONG_SNAPSHOT");
                if (calls.get() != 5) throw new AssertionError("SNAPSHOT_PREDICATE_REPEATED=" + calls);
            }
            case "remove" -> {
                var backing = new ArrayList<>(List.of(1, 2, 3));
                Collection<Integer> owner = new AbstractCollection<>() {
                    @Override public int size() { return backing.size(); }
                    @Override public Iterator<Integer> iterator() { return Collections.unmodifiableCollection(backing).iterator(); }
                    @Override public boolean remove(Object value) { return backing.remove(value); }
                };
                if (!M3CollectionViews.filtered(owner, i -> i % 2 != 0).remove(1) || backing.contains(1))
                    throw new AssertionError("OWNER_REMOVE_NOT_REUSED");
            }
            case "identity" -> {
                Collection<String> owner = Collections.newSetFromMap(new IdentityHashMap<>());
                String stored = new String("same"); owner.add(stored);
                Collection<String> view = M3CollectionViews.filtered(owner, x -> true);
                if (!view.contains(stored) || view.contains(new String("same")))
                    throw new AssertionError("OWNER_IDENTITY_NOT_PRESERVED");
            }
            default -> throw new IllegalArgumentException("mode");
        }
        System.out.println("WRAPPER_REFINEMENT_PASS=" + args[0]);
    }
}
