import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import jdk.internal.mindex.M3TQ;
/**
 * TqProbe : M3TQ_SCAN_DIFFERENTIAL v1. Random flat (fromExact) and compound (and/or of fromExact) trigram queries over
 * random texts and windows: hasScanPlan, testScanning and the facts path testPrecomputed(precompute(window)). The digest
 * is compared between the master and leaf M3TQ; within one class, a scan answer must equal the facts answer.
 */
public final class TqProbe {
    static final String[] WORDS = {"abc", "abcd", "bcd", "the", "then", "hen", "xyz", "zzz", "qqqq", "ab", "a"};
    public static void main(String[] a) {
        SplittableRandom r = new SplittableRandom(Long.parseLong(a[0]));
        int rounds = Integer.parseInt(a[1]);
        long digest = 1, scans = 0, disagree = 0, plans = 0;
        for (int round = 0; round < rounds; round++) {
            M3TQ q = query(r, 0);
            StringBuilder b = new StringBuilder();
            for (int i = r.nextInt(30); i > 0; i--) b.append(WORDS[r.nextInt(WORDS.length)]);
            String s = b.toString();
            int from = r.nextInt(s.length() + 1), to = from + r.nextInt(s.length() - from + 1);
            boolean plan = q.hasScanPlan();
            boolean facts = q.testPrecomputed(M3TQ.precompute(s, from, to, 1 << 20));
            digest = digest * 31 + (plan ? 3 : 5) + (facts ? 7 : 11);
            if (plan) {
                plans++;
                boolean scan = q.testScanning(s, from, to, 1 << 20);
                scans++;
                if (scan != facts) disagree++;
                digest = digest * 31 + (scan ? 13 : 17);
            }
        }
        System.out.println("M3TQ_SCAN_DIFFERENTIAL rounds=" + rounds + " plans=" + plans + " scan_vs_facts_disagree=" + disagree + " digest=" + Long.toHexString(digest));
    }
    static M3TQ query(SplittableRandom r, int depth) {
        int kind = depth > 1 ? 0 : r.nextInt(4);
        if (kind <= 1) {
            List<String> exact = new ArrayList<>();
            for (int i = 1 + r.nextInt(3); i > 0; i--) exact.add(WORDS[r.nextInt(WORDS.length)] + WORDS[r.nextInt(WORDS.length)]);
            return M3TQ.fromExact(exact);
        }
        List<M3TQ> terms = new ArrayList<>();
        for (int i = 2 + r.nextInt(2); i > 0; i--) terms.add(query(r, depth + 1));
        return kind == 2 ? M3TQ.and(terms) : M3TQ.or(terms);
    }
}
