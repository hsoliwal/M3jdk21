import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.SequencedSet;
import java.util.SortedSet;
import java.util.SplittableRandom;
import java.util.TreeSet;

/**
 * RevSetProbe : REVERSE_SORTED_SUBSET_DIFFERENTIAL v1 for java.util.ReverseOrderSortedSetView. A non-navigable
 * SortedSet (Collections.synchronizedSortedSet over a TreeSet) is reversed; subSet/headSet/tailSet views of the
 * reversed view (and views of views) are iterated, sized and probed (first/last/contains) and compared with a reference
 * built from a reversed list. Prints a digest so the stock, live and restored classes can be compared.
 */
public final class RevSetProbe {
    public static void main(String[] args) {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 20261107L;
        int rounds = args.length > 1 ? Integer.parseInt(args[1]) : 40_000;
        SplittableRandom r = new SplittableRandom(seed);
        long digest = 11L;
        int[] bad = new int[4], seen = new int[4];
        for (int round = 0; round < rounds; round++) {
            TreeSet<Integer> tree = new TreeSet<>();
            int n = r.nextInt(60);
            for (int i = 0; i < n; i++) tree.add(r.nextInt(100));
            SortedSet<Integer> base = Collections.synchronizedSortedSet(tree);
            SequencedSet<Integer> rev = base.reversed();
            SortedSet<Integer> view = (SortedSet<Integer>) rev;
            int a = r.nextInt(110) - 5, b = r.nextInt(110) - 5;
            SortedSet<Integer> v;
            List<Integer> ref = new ArrayList<>(tree.descendingSet());
            int kase = r.nextInt(4);
            seen[kase]++;
            switch (kase) {
                case 0 -> { v = view.headSet(a); ref.removeIf(x -> x <= a); }
                case 1 -> { v = view.tailSet(a); ref.removeIf(x -> x > a); }
                case 2 -> {
                    int hi = Math.max(a, b), lo = Math.min(a, b);
                    v = view.subSet(hi, lo);            // reversed order: from hi down to (exclusive) lo
                    ref.removeIf(x -> x > hi || x <= lo);
                }
                default -> {
                    int hi = Math.max(a, b), lo = Math.min(a, b);
                    try {
                        v = view.headSet(lo).tailSet(hi);   // a view of a view
                    } catch (IllegalArgumentException outOfRange) {
                        digest = digest * 31 + 7;         // same refusal must be seen on every class
                        continue;
                    }
                    ref.removeIf(x -> x > hi || x <= lo);
                }
            }
            List<Integer> got = new ArrayList<>();
            for (Iterator<Integer> it = v.iterator(); it.hasNext(); ) got.add(it.next());
            if (!got.equals(ref) || v.size() != ref.size()) bad[kase]++;
            digest = digest * 31 + got.hashCode();
            digest = digest * 31 + probe(v::first) * 7 + probe(v::last);
            int q = r.nextInt(110) - 5;
            digest = digest * 31 + (v.contains(q) == ref.contains(q) ? 1 : -1);
        }
        System.out.println("RevSetProbe rounds=" + rounds + " mismatches head/tail/sub/viewOfView=" + bad[0] + "/" + bad[1] + "/" + bad[2] + "/" + bad[3]
                + " of " + seen[0] + "/" + seen[1] + "/" + seen[2] + "/" + seen[3] + " digest=" + Long.toHexString(digest));
    }

    interface Get { Integer get(); }

    static long probe(Get g) {
        try {
            return g.get();
        } catch (NoSuchElementException e) {
            return -999;
        }
    }
}
