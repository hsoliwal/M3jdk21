import java.util.SplittableRandom;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * RegexPredProbe : M3_BOOLEAN_PLAN_DIFFERENTIAL v1. Pattern.asMatchPredicate() (served by M3PatternQuery.BooleanPlan for
 * straight-line atom/slice/greedy-star patterns) must equal Pattern.matcher(s).matches() (the engine authority). Patterns
 * of 1..20 atoms over ASCII letters, a Latin-1 letter, a BMP letter, a supplementary letter, classes, negated classes
 * and '.', each optionally starred; inputs mix ASCII runs of 0..40 units with non-ASCII units and surrogate pairs, so
 * the plan's ASCII-run lanes (runs >= 8) and its rolling lanes are both reached. Prints mismatches and a result digest.
 */
public final class RegexPredProbe {
    static final String[] ATOMS = {"a", "b", "c", "\u00e9", "\u0416", "\ud834\udd1e", "[ab]", "[^a]", ".", "[a-c\u00e9]", "x"};
    static final String[] UNITS = {"a", "b", "c", "x", "\u00e9", "\u0416", "\ud834\udd1e", "\n", "z"};

    static java.lang.reflect.Field planField;

    static boolean planned(Pattern pattern) {
        try {
            if (planField == null) { planField = Pattern.class.getDeclaredField("m3BooleanPlan"); planField.setAccessible(true); }
            return planField.get(pattern) != null;
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("not the M3 Pattern: " + e); }
    }

    /** A string the pattern matches: one unit per plain atom, 0..20 units per starred atom (long ASCII runs). */
    static String sample(String p, SplittableRandom r) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < p.length(); ) {
            String atom = null;
            for (String a : ATOMS) if (p.startsWith(a, i) && (atom == null || a.length() > atom.length())) atom = a;
            i += atom.length();
            boolean star = i < p.length() && p.charAt(i) == '*';
            if (star) i++;
            int n = star ? (r.nextInt(3) == 0 ? 8 + r.nextInt(13) : r.nextInt(3)) : 1;
            for (int k = 0; k < n; k++) out.append(unit(atom, r));
        }
        return out.toString();
    }

    static String unit(String atom, SplittableRandom r) {
        return switch (atom) {
            case "[ab]" -> r.nextBoolean() ? "a" : "b";
            case "[^a]", "." -> r.nextBoolean() ? "x" : "c";
            case "[a-cé]" -> String.valueOf("abcé".charAt(r.nextInt(4)));
            default -> atom;
        };
    }

    public static void main(String[] args) {
        SplittableRandom r = new SplittableRandom(args.length > 0 ? Long.parseLong(args[0]) : 20261107L);
        int rounds = args.length > 1 ? Integer.parseInt(args[1]) : 20000;
        long checks = 0, mismatches = 0, trues = 0, digest = 7, plans = 0;
        for (int round = 0; round < rounds; round++) {
            StringBuilder p = new StringBuilder();
            for (int i = 1 + r.nextInt(r.nextInt(3) == 0 ? 20 : 10); i > 0; i--) {
                p.append(ATOMS[r.nextInt(r.nextInt(3) == 0 ? ATOMS.length : 3)]);
                if (r.nextInt(3) == 0) p.append('*');
            }
            Pattern pattern = Pattern.compile(p.toString());
            if (planned(pattern)) plans++;
            Predicate<String> predicate = pattern.asMatchPredicate();
            for (int q = 0; q < 12; q++) {
                StringBuilder s = new StringBuilder();
                if (q % 2 == 0) { s.append(sample(p.toString(), r)); if (r.nextInt(4) == 0 && s.length() > 0) s.setCharAt(r.nextInt(s.length()), 'q'); }
                else for (int seg = r.nextInt(5); seg >= 0; seg--) {
                    int run = r.nextInt(4) == 0 ? 8 + r.nextInt(33) : r.nextInt(9);
                    for (int k = 0; k < run; k++) s.append("abcx".charAt(r.nextInt(r.nextInt(3) == 0 ? 4 : 2)));
                    if (r.nextBoolean()) s.append(UNITS[r.nextInt(UNITS.length)]);
                }
                String in = s.toString();
                boolean a = predicate.test(in), b = pattern.matcher(in).matches();
                checks++;
                if (a) trues++;
                digest = digest * 31 + (a ? 1 : 2);
                if (a != b && mismatches++ < 5) System.out.println("MISMATCH pattern=" + p + " input=" + in + " predicate=" + a + " matcher=" + b);
            }
        }
        System.out.println("M3_BOOLEAN_PLAN_DIFFERENTIAL checks=" + checks + " true=" + trues + " mismatches=" + mismatches + " patterns_with_plan=" + plans + "/" + rounds + " digest=" + Long.toHexString(digest));
    }
}
