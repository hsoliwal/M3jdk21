/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.regex.Pattern;

/**
 * Reference model of the ASCII literal/dot/star programs of M3RegexDpPlan (DOT-STAR ORACLE v1).
 *
 * <p>The language oracle is java.util.regex: an admitted program is itself a Java regex with the
 * same meaning (letters and digits are literals, {@code .} excludes the five line terminators,
 * {@code *} is the Kleene star of the preceding atom) and {@code Matcher.matches()} is the full-match
 * decision. The state-set oracle is an explicit java.util.BitSet NFA over code points, written from
 * the class contract (state i = first i atoms consumed), independent of the plan's bit arithmetic.</p>
 */
final class DotStarOracle {
    /** Admitted pattern length bounds and token budget. */
    static final int MIN_PATTERN = 2;
    static final int MAX_PATTERN = 128;
    static final int MAX_TOKENS = 63;
    /** The 62 admitted literal code units in the plan's compact index order. */
    static final String LITERALS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    /** Supplementary code point used by the UTF-16 checks (U+1F600). */
    static final String SUPPLEMENTARY = "😀";
    /** Lone surrogate units. */
    static final char HIGH = '\uD83D';
    static final char LOW = '\uDE00';
    /** Probability that a generated atom is starred / is a dot. */
    private static final double STAR_RATE = 0.4;
    private static final double DOT_RATE = 0.2;

    private static final Map<String, Pattern> COMPILED = new HashMap<>();

    private DotStarOracle() {
    }

    /** Parsed program: atoms 1..n, each a literal or '.', with its star flag. */
    record Program(String source, char[] atoms, boolean[] starred) {
        int tokens() {
            return atoms.length - 1;
        }
    }

    /** Admission from the class contract: 2..128 chars, alnum/dot atoms, stars after atoms only, 1..63 atoms, >=1 star. */
    static boolean admitted(String pattern) {
        if (pattern.length() < MIN_PATTERN || pattern.length() > MAX_PATTERN) {
            return false;
        }
        int tokens = 0;
        int stars = 0;
        boolean afterAtom = false;
        for (char c : pattern.toCharArray()) {
            if (c == '*') {
                if (!afterAtom) {
                    return false;
                }
                stars++;
                afterAtom = false;
            } else if (c == '.' || LITERALS.indexOf(c) >= 0) {
                tokens++;
                afterAtom = true;
            } else {
                return false;
            }
        }
        return stars > 0 && tokens <= MAX_TOKENS;
    }

    static Program parse(String pattern) {
        List<Character> atoms = new ArrayList<>();
        List<Boolean> stars = new ArrayList<>();
        atoms.add('\0');
        stars.add(false);
        for (char c : pattern.toCharArray()) {
            if (c == '*') {
                stars.set(stars.size() - 1, true);
            } else {
                atoms.add(c);
                stars.add(false);
            }
        }
        char[] a = new char[atoms.size()];
        boolean[] s = new boolean[atoms.size()];
        for (int i = 0; i < a.length; i++) {
            a[i] = atoms.get(i);
            s[i] = stars.get(i);
        }
        return new Program(pattern, a, s);
    }

    static boolean lineTerminator(int codePoint) {
        return codePoint == '\n' || codePoint == '\r' || codePoint == 0x85 || codePoint == 0x2028 || codePoint == 0x2029;
    }

    static boolean atomMatches(char atom, int codePoint) {
        return atom == '.' ? !lineTerminator(codePoint) : atom == codePoint;
    }

    /** Epsilon closure: an atom with a star may be skipped. */
    static BitSet closure(Program p, BitSet states) {
        BitSet out = (BitSet) states.clone();
        for (int j = 1; j <= p.tokens(); j++) {
            if (p.starred()[j] && out.get(j - 1)) {
                out.set(j);
            }
        }
        return out;
    }

    /** Consumes one code point from a closed state set and closes the result. */
    static BitSet step(Program p, BitSet states, int codePoint) {
        BitSet next = new BitSet();
        for (int i = states.nextSetBit(0); i >= 0; i = states.nextSetBit(i + 1)) {
            if (i + 1 <= p.tokens() && !p.starred()[i + 1] && atomMatches(p.atoms()[i + 1], codePoint)) {
                next.set(i + 1);
            }
            if (i >= 1 && p.starred()[i] && atomMatches(p.atoms()[i], codePoint)) {
                next.set(i);
            }
        }
        return closure(p, next);
    }

    static BitSet initial(Program p) {
        BitSet start = new BitSet();
        start.set(0);
        return closure(p, start);
    }

    /** States reached from {@code from} after every code point of {@code text}. */
    static BitSet run(Program p, BitSet from, CharSequence text) {
        BitSet states = from;
        for (int i = 0; i < text.length(); ) {
            int cp = Character.codePointAt(text, i);
            states = step(p, states, cp);
            i += Character.charCount(cp);
        }
        return states;
    }

    static boolean accepts(Program p, BitSet states) {
        return states.get(p.tokens());
    }

    static long bits(BitSet set) {
        long[] words = set.toLongArray();
        return words.length == 0 ? 0L : words[0];
    }

    static BitSet set(long bits) {
        return BitSet.valueOf(new long[] {bits});
    }

    /** java.util.regex full-match decision of the same program text. */
    static boolean javaMatches(String pattern, CharSequence text) {
        return COMPILED.computeIfAbsent(pattern, Pattern::compile).matcher(text).matches();
    }

    /** Random admitted program of 1..maxAtoms atoms over the given literal alphabet. */
    static String randomPattern(SplittableRandom random, int maxAtoms, String alphabet) {
        for (;;) {
            StringBuilder out = new StringBuilder();
            int atoms = random.nextInt(1, maxAtoms + 1);
            for (int i = 0; i < atoms; i++) {
                out.append(random.nextDouble() < DOT_RATE ? '.' : alphabet.charAt(random.nextInt(alphabet.length())));
                if (random.nextDouble() < STAR_RATE) {
                    out.append('*');
                }
            }
            String pattern = out.toString();
            if (admitted(pattern)) {
                return pattern;
            }
        }
    }

    /** Every string over {@code alphabet} of length 0..maxLength (alphabet entries may be surrogate pairs). */
    static List<String> texts(List<String> alphabet, int maxLength) {
        List<String> out = new ArrayList<>();
        List<String> layer = List.of("");
        out.add("");
        for (int len = 1; len <= maxLength; len++) {
            List<String> next = new ArrayList<>();
            for (String prefix : layer) {
                for (String unit : alphabet) {
                    next.add(prefix + unit);
                }
            }
            out.addAll(next);
            layer = next;
        }
        return out;
    }
}
