import java.util.SplittableRandom;

/**
 * ZDiff : Z_SIMILARITY_DIFFERENTIAL v1. For seeded UTF-16 strings (random over small alphabets, periodic runs,
 * uniform runs, supplementary pairs and unpaired surrogates, lengths 0..600) compares the sum of Z-scores from
 * (a) a brute-force O(n^2) oracle, (b) the donor com.synexia.m3.search.M3ZSimilarity.sumJava over synexia-algo's
 * StringsZAlgorithm, (c) the relocated jdk.m3.search.M3ZSimilarity.sumJava over M3StringAlgorithms.zFunction and,
 * when -Dzdiff.native=true, (d) jdk.m3.search.M3ZSimilarity.sumNativeRequired (the relocated C kernel).
 */
public final class ZDiff {
    private static final char[] ALPHABET = {'a', 'b', 'c', '$', 'é', '\uD83D', '\uDE00', '\uD800', 'a', 'a'};

    public static void main(String[] args) {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 20261106L;
        int cases = args.length > 1 ? Integer.parseInt(args[1]) : 20_000;
        boolean nat = Boolean.getBoolean("zdiff.native");
        SplittableRandom r = new SplittableRandom(seed);
        long digest = 7L;
        for (int c = 0; c < cases; c++) {
            String s = sample(r);
            long brute = brute(s);
            long donor = com.synexia.m3.search.M3ZSimilarity.sumJava(s);
            long m3 = jdk.m3.search.M3ZSimilarity.sumJava(s);
            if (donor != brute || m3 != brute) throw new AssertionError("java mismatch case " + c + " len " + s.length());
            if (nat && jdk.m3.search.M3ZSimilarity.sumNativeRequired(s) != brute) throw new AssertionError("native mismatch case " + c);
            digest = digest * 1_000_003L + brute;
        }
        System.out.println("ZDiff PASS cases=" + cases + " native=" + nat + " digest=" + Long.toHexString(digest));
    }

    static String sample(SplittableRandom r) {
        int n = r.nextInt(601);
        StringBuilder b = new StringBuilder(n);
        switch (r.nextInt(4)) {
            case 0 -> { for (int i = 0; i < n; i++) b.append(ALPHABET[r.nextInt(ALPHABET.length)]); }
            case 1 -> { int p = 1 + r.nextInt(5); String unit = randomUnit(r, p); while (b.length() < n) b.append(unit); b.setLength(n); }
            case 2 -> { char ch = ALPHABET[r.nextInt(ALPHABET.length)]; for (int i = 0; i < n; i++) b.append(ch); if (n > 0 && r.nextBoolean()) b.setCharAt(r.nextInt(n), 'z'); }
            default -> { for (int i = 0; i < n; i++) b.append((char) ('a' + r.nextInt(2))); }
        }
        return b.toString();
    }

    static String randomUnit(SplittableRandom r, int p) {
        StringBuilder u = new StringBuilder();
        for (int i = 0; i < p; i++) u.append(ALPHABET[r.nextInt(ALPHABET.length)]);
        return u.toString();
    }

    static long brute(String s) {
        long sum = 0;
        for (int i = 0; i < s.length(); i++) {
            int k = 0;
            while (i + k < s.length() && s.charAt(k) == s.charAt(i + k)) k++;
            sum += k;
        }
        return sum;
    }
}
