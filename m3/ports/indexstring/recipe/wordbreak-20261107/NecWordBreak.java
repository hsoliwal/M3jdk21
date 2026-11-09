import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
/**
 * NecWordBreak : NEC row of census2 P2-056 (feasibility only). Relocated MIndexWordBreakPlan.canSegment vs the existing
 * jdk.m3.algorithm.M3DynamicProgrammingAlgorithms.wordBreak (LC139, boolean) over random vocabularies and inputs.
 * Counts agreements and classifies each disagreement by its shape (empty input, empty word in the vocabulary, other).
 */
public final class NecWordBreak {
    public static void main(String[] a) {
        SplittableRandom r = new SplittableRandom(20261107L);
        long agree = 0, emptyInput = 0, emptyWord = 0, other = 0, total = 0;
        String units = "abc";
        for (int round = 0; round < 50_000; round++) {
            List<String> words = new ArrayList<>();
            for (int i = r.nextInt(8); i > 0; i--) { StringBuilder b = new StringBuilder(); for (int k = r.nextInt(4); k > 0; k--) b.append(units.charAt(r.nextInt(3))); words.add(b.toString()); }
            var plan = jdk.internal.m3.indexstring.MIndexWordBreakPlan.compile(jdk.internal.m3.indexstring.MIndexVocabularySearchTree.compile(words));
            for (int q = 0; q < 6; q++) {
                StringBuilder b = new StringBuilder();
                for (int k = r.nextInt(5); k > 0; k--) b.append(!words.isEmpty() && r.nextInt(3) != 0 ? words.get(r.nextInt(words.size())) : String.valueOf(units.charAt(r.nextInt(3))));
                String in = b.toString();
                boolean x = plan.canSegment(in), y = jdk.m3.algorithm.M3DynamicProgrammingAlgorithms.wordBreak(in, words, jdk.m3.algorithm.M3ProgressMonitor.none());
                total++;
                if (x == y) agree++; else if (in.isEmpty()) emptyInput++; else if (words.contains("")) emptyWord++; else { if (other++ < 5) System.out.println("OTHER in=" + in + " words=" + words + " plan=" + x + " lc139=" + y); }
            }
        }
        System.out.println("NEC_WORDBREAK total=" + total + " agree=" + agree + " differ_empty_input=" + emptyInput + " differ_empty_word=" + emptyWord + " differ_other=" + other);
    }
}
