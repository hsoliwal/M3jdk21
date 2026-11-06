import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * CowDiff : COW_REMOVEIF_DIFFERENTIAL v1. Seeded random lists (size 0..300) and predicates (density 0..1, plus
 * windows hitting the 64-wide single-word boundary), removeIf on the whole list and on subList windows; oracle =
 * java.util.ArrayList. Also checks the reentrant-modification ConcurrentModificationException path. Prints one
 * digest line so the live and restored classes can be compared byte for byte.
 */
public final class CowDiff {
    public static void main(String[] args) {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 20261106L;
        int rounds = args.length > 1 ? Integer.parseInt(args[1]) : 200_000;
        SplittableRandom rnd = new SplittableRandom(seed);
        long digest = 1125899906842597L;
        int cases = 0;
        for (int r = 0; r < rounds; r++) {
            int n = rnd.nextInt(4) == 0 ? 60 + rnd.nextInt(10) : rnd.nextInt(301);
            List<Integer> base = new ArrayList<>(n);
            for (int i = 0; i < n; i++) base.add(rnd.nextInt(1000));
            int threshold = rnd.nextInt(1001);
            Predicate<Integer> p = v -> v < threshold;
            CopyOnWriteArrayList<Integer> cow = new CopyOnWriteArrayList<>(base);
            List<Integer> oracle = new ArrayList<>(base);
            boolean a, b;
            if (n > 0 && rnd.nextBoolean()) {
                int lo = rnd.nextInt(n + 1), hi = lo + rnd.nextInt(n - lo + 1);
                a = cow.subList(lo, hi).removeIf(p);
                b = oracle.subList(lo, hi).removeIf(p);
            } else {
                a = cow.removeIf(p);
                b = oracle.removeIf(p);
            }
            if (a != b || !cow.equals(oracle)) throw new AssertionError("mismatch round " + r + " n=" + n);
            digest = digest * 31 + (a ? 1 : 0);
            digest = digest * 31 + cow.hashCode();
            cases++;
        }
        cases += reentrant(digest);
        System.out.println("CowDiff PASS cases=" + cases + " digest=" + Long.toHexString(digest));
    }

    private static int reentrant(long digest) {
        int checked = 0;
        for (int n = 1; n <= 130; n++) {
            CopyOnWriteArrayList<Integer> cow = new CopyOnWriteArrayList<>();
            for (int i = 0; i < n; i++) cow.add(i);
            try {
                cow.removeIf(v -> { if (v == 0) cow.add(-1); return v % 2 == 0; });
                throw new AssertionError("no CME at n=" + n);
            } catch (ConcurrentModificationException expected) {
                checked++;
            }
        }
        return checked;
    }
}
