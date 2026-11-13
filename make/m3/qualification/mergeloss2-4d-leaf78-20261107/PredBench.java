import java.util.function.Predicate;
import java.util.regex.Pattern;
/** PredBench : asMatchPredicate throughput on >= 8-atom plans over mixed ASCII / non-ASCII inputs (5 forks by the caller). */
public final class PredBench {
    public static void main(String[] a) {
        String[] patterns = {"a*b*[ab]*c*x*.*a*b*c", "[ab]*x*c*a*b*.*\u0416*a*b*c*x", ".*a.*b.*c.*x.*a.*b.*c.*"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4000; i++) sb.append(i % 97 == 0 ? '\u0416' : "abcx".charAt(i % 4));
        String text = sb.toString();
        long sink = 0;
        for (String p : patterns) {
            Predicate<String> pr = Pattern.compile(p).asMatchPredicate();
            for (int w = 0; w < 300; w++) sink += pr.test(text) ? 1 : 0;
            long t = System.nanoTime();
            for (int k = 0; k < 2000; k++) sink += pr.test(text) ? 1 : 0;
            System.out.printf("%s %.1f us/op%n", p.length() > 12 ? p.substring(0, 12) : p, (System.nanoTime() - t) / 2000 / 1000.0);
        }
        System.out.println("sink=" + sink);
    }
}
