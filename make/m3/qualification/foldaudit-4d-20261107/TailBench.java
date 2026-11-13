import java.util.function.Predicate;
import java.util.regex.Pattern;
/** TailBench : asMatchPredicate on inputs whose ASCII run starts beyond the 64-unit lookahead (3 forks by the caller). */
public final class TailBench {
    public static void main(String[] a) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++) sb.append('\u0416');
        for (int i = 0; i < 4000; i++) sb.append("abcx".charAt(i % 4));
        String text = sb.toString();
        String[] patterns = {".*a.*b.*c.*x.*a.*b.*c.*", "\u0416*[ab]*x*c*a*b*.*a*b*c*x"};
        long sink = 0;
        for (String p : patterns) {
            Predicate<String> pr = Pattern.compile(p).asMatchPredicate();
            for (int w = 0; w < 300; w++) sink += pr.test(text) ? 1 : 0;
            long t = System.nanoTime();
            for (int k = 0; k < 2000; k++) sink += pr.test(text) ? 1 : 0;
            System.out.printf("%s %.1f us/op ", p.substring(0, 10), (System.nanoTime() - t) / 2000 / 1000.0);
        }
        System.out.println("sink=" + sink);
    }
}
