// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import java.util.Random;
import java.util.regex.Pattern;
import jdk.internal.mindex.M3RegexDpPlan;

public class M3CharacterPlanPreservation {
    public static void main(String[] args) {
        Random random = new Random(0x3C0A_2026L);
        String[] units = {"a", "b", "3", "\n", "\r", "\u0085", "\u2028", "\ud83d", "\ude00", "\ud83d\ude00"};
        int checks = 0;
        for (int trial = 0; trial < 4000; trial++) {
            StringBuilder expression = new StringBuilder("a*");
            for (int n = random.nextInt(6); n > 0; n--) {
                expression.append("ab3.".charAt(random.nextInt(4)));
                if (random.nextBoolean()) expression.append('*');
            }
            StringBuilder text = new StringBuilder();
            for (int n = random.nextInt(14); n > 0; n--) text.append(units[random.nextInt(units.length)]);
            String pattern = expression.toString(), input = text.toString();
            M3RegexDpPlan plan = M3RegexDpPlan.prepare(pattern);
            if (plan == null) throw new AssertionError("admitted shape");
            boolean expected = Pattern.matches(pattern, input);
            var whole = plan.summarizeUtf16(input, null);
            if (plan.accepts(whole.apply(plan.initialState())) != expected) throw new AssertionError("UTF16 oracle");
            checks++;
            int split = random.nextInt(input.length() + 1);
            var joined = plan.summarizeUtf16(input.substring(0, split), null)
                    .then(plan.summarizeUtf16(input.substring(split), null), null);
            if (joined.apply(plan.initialState()) != whole.apply(plan.initialState())) throw new AssertionError("seam");
            checks++;
            var legacy = plan.summarize(input, null);
            boolean surrogate = input.chars().anyMatch(c -> Character.isSurrogate((char) c));
            if (surrogate ? legacy != null : legacy == null || plan.accepts(legacy.apply(plan.initialState())) != expected) {
                throw new AssertionError("legacy surrogate/character contract");
            }
            checks++;
        }
        StringBuilder tape = new StringBuilder();
        CharSequence observed = new CharSequence() {
            public int length() { tape.append("L;"); return 3; }
            public char charAt(int i) { tape.append(i).append(';'); return "aaa".charAt(i); }
            public CharSequence subSequence(int a, int b) { throw new AssertionError("slice"); }
            public String toString() { throw new AssertionError("flatten"); }
        };
        M3RegexDpPlan.prepare("a*").summarize(observed, () -> tape.append("C;"));
        if (!tape.toString().equals("L;C;0;1;2;C;")) throw new AssertionError(tape);
        System.out.println("M3CharacterPlanPreservation PASS checks=" + (checks + 1) + " tape=" + tape);
    }
}
