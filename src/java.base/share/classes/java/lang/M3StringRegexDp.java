/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import jdk.internal.mindex.M3RX;

/**
 * Bounded full-region dynamic programming for the conservative dot/star regex subset.
 *
 * <p>Only {@link String#matches(String)} with an existing canonical M3 String owner
 * may use this internal decision. It does not implement the stateful Pattern/Matcher
 * API, regex flags, Unicode code-point syntax, captures, or search operations.
 * An unknown decision must delegate to the ordinary JDK regex engine.</p>
 *
 * <p>Each compiled atom is one lowercase/uppercase ASCII letter, digit, or dot;
 * a following star marks zero-or-more repetition. The temporary token/DP arrays
 * never contain or own text payload, and each text unit is read directly from
 * the canonical M3 owner rather than a flattened String shadow.</p>
 */
final class M3StringRegexDp {
    static final int UNKNOWN = -1;
    static final int NO_MATCH = 0;
    static final int MATCH = 1;

    private static final int STAR = 1 << 16;
    private static final int CODE_UNIT = 0xffff;
    private static final int MAX_PATTERN_UNITS = 128;
    private static final int MAX_TEXT_UNITS = 8_192;
    private static final long MAX_DP_CELLS = 1_000_000L;

    private static final int PLAN_SLOTS = 32;
    private static final int EFFECT_SLOTS = 128;
    private static volatile Cache cache;

    private M3StringRegexDp() { }

    /** Explicit eviction changes cost only; no canonical owner or text storage is cleared. */
    static void clearPrecompute() { cache = null; }

    /** Retained primitive-array payload bound, excluding object/scalar/VM overhead; includes all plans. */
    static long maximumRetainedPrimitiveBytes() {
        return (PLAN_SLOTS + (long) EFFECT_SLOTS) * (63L * 4 + 134L * 8)
                + EFFECT_SLOTS * 64L * 8;
    }

    /**
     * A three-state, contract-preserving decision: UNKNOWN means use Pattern.matches.
     * No stored per-String state is introduced. Optional metadata uses weak exact owner/range
     * keys and exact compiled instructions. No canonical payload or String API is added.
     */
    static int tryMatch(M3String source, String regex) {
        return tryMatch(source, regex, false);
    }

    /** Explicit internal native-effect candidate; normal String.matches keeps Java selection. */
    static int tryMatch(M3String source, String regex, boolean nativeComposition) {
        int n = regex.length();
        if (n < 2 || n > MAX_PATTERN_UNITS) return UNKNOWN;

        // Compile once per candidate. The compact int lane is a temporary pattern plan,
        // not an owned spelling payload. Every accepted pattern is syntactically valid.
        int[] atoms = new int[n];
        int count = 0;
        int stars = 0;
        for (int index = 0; index < n; index++) {
            char unit = regex.charAt(index);
            if (unit == '*') {
                if (count == 0 || (atoms[count - 1] & STAR) != 0) return UNKNOWN;
                atoms[count - 1] |= STAR;
                stars++;
            } else if (unit == '.' || isAsciiAlphanumeric(unit)) {
                atoms[count++] = unit;
            } else {
                return UNKNOWN;
            }
        }
        // Literal-only expressions already have an exact M3 equality fast path.
        if (stars == 0) return UNKNOWN;

        int m = source.length();
        if (m > MAX_TEXT_UNITS || ((long) m + 1) * (count + 1) > MAX_DP_CELLS) {
            return UNKNOWN;
        }

        // Preserve the measured short-input baseline. Warm cache benefits do not justify
        // charging all small one-shot matches for plan/cache admission.
        if (m >= 256 && count <= M3RX.MAX_ATOMS) {
            try {
                return preparedMatch(source, atoms, count, nativeComposition);
            } catch (OutOfMemoryError unavailable) {
                // Only optional precompute is abandoned. Original DP remains the compatibility lane.
                cache = null;
            }
        }

        // Nullable prefix: every atom of an empty-text-matching prefix must be starred.
        boolean[] previous = new boolean[count + 1];
        boolean[] current = new boolean[count + 1];
        previous[0] = true;
        for (int j = 1; j <= count; j++) {
            previous[j] = (atoms[j - 1] & STAR) != 0 && previous[j - 1];
        }

        for (int i = 0; i < m; i++) {
            char value = source.charAt(i);
            // java.util.regex matches Unicode code points, not arbitrary halves of
            // surrogate pairs. Preserve that contract through the JDK fallback.
            if (Character.isSurrogate(value)) return UNKNOWN;
            current[0] = false;
            for (int j = 1; j <= count; j++) {
                int atom = atoms[j - 1];
                char required = (char) (atom & CODE_UNIT);
                boolean accepts = required == '.'
                        ? !isJdkLineTerminator(value)
                        : required == value;
                current[j] = (atom & STAR) != 0
                        ? current[j - 1] || (accepts && previous[j])
                        : accepts && previous[j - 1];
            }
            boolean[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[count] ? MATCH : NO_MATCH;
    }

    private static int preparedMatch(M3String source, int[] atoms, int count,
                                     boolean nativeComposition) {
        Cache current = cache;
        if (current == null) {
            synchronized (M3StringRegexDp.class) {
                current = cache;
                if (current == null) cache = current = new Cache();
            }
        }
        M3RX.Plan plan = current.plan(atoms, count);
        Entry entry = current.entry(source, plan);
        if (entry != null && entry.effect != null) return plan.matches(entry.effect) ? MATCH : NO_MATCH;
        // Admission/setup is not amortized for a cold one-shot string. Only a repeated exact
        // owner/range/plan key warms all-entry effects, with independent work/shape limits.
        if (entry != null && source.length() <= 512 && count <= 31) {
            M3RX.Effect effect = prepareTree(source, plan, current, new Budget(), 0, nativeComposition);
            if (effect != null) return plan.matches(effect) ? MATCH : NO_MATCH;
        }
        int result = plan.tryMatch(source.length(), source::charAt, null);
        if (result != UNKNOWN) current.put(source, plan, null);
        return result;
    }

    /** Reuses the existing tuple DAG and coordinate rules; slices are handles, never text copies. */
    private static M3RX.Effect prepareTree(M3String source, M3RX.Plan plan, Cache current,
                                          Budget budget, int depth, boolean nativeComposition) {
        if (++budget.nodes > 128 || depth > 48) return null;
        Entry cached = current.entry(source, plan);
        if (cached != null && cached.effect != null) return cached.effect;
        M3RX.Effect result;
        if (source.owner() instanceof M3StringTuple tuple && source.length() != 0) {
            int split = tuple.left.length();
            int begin = source.start(), end = source.end();
            if (end <= split) {
                result = prepareTree(tuple.left.slice(begin, end), plan, current, budget,
                        depth + 1, nativeComposition);
            } else if (begin >= split) {
                result = prepareTree(tuple.right.slice(begin - split, end - split), plan, current,
                        budget, depth + 1, nativeComposition);
            } else {
                M3RX.Effect left = prepareTree(tuple.left.slice(begin, split), plan, current,
                        budget, depth + 1, nativeComposition);
                if (left == null) return null;
                M3RX.Effect right = prepareTree(tuple.right.slice(0, end - split), plan, current,
                        budget, depth + 1, nativeComposition);
                if (right == null) return null;
                result = nativeComposition ? plan.composeNative(left, right, null)
                        : plan.compose(left, right, null);
            }
        } else {
            long work = (long) source.length() * plan.states();
            if (work > budget.steps) return null;
            budget.steps -= work;
            result = plan.summarize(source.length(), source::charAt, work, null);
        }
        if (result != null) current.put(source, plan, result);
        return result;
    }

    private static final class Budget {
        long steps = 1_000_000;
        int nodes;
    }

    /** Follows M3StringSearchPrecompute's bounded weak-owner/exact-coordinate cache pattern. */
    private static final class Cache {
        final AtomicReferenceArray<M3RX.Plan> plans = new AtomicReferenceArray<>(PLAN_SLOTS);
        final AtomicReferenceArray<Entry> effects = new AtomicReferenceArray<>(EFFECT_SLOTS);

        M3RX.Plan plan(int[] atoms, int count) {
            int hash = count;
            for (int i = 0; i < count; i++) hash = 31 * hash + atoms[i];
            int slot = hash & (PLAN_SLOTS - 1);
            M3RX.Plan plan = plans.get(slot);
            if (plan != null && plan.sameInstructions(atoms, count)) return plan;
            plan = M3RX.compile(atoms, count, null);
            plans.set(slot, plan);
            return plan;
        }

        Entry entry(M3String source, M3RX.Plan plan) {
            Entry entry = effects.get(slot(source, plan));
            return entry != null && entry.owner.get() == source.owner()
                    && entry.coordinate == source.coordinate() && entry.plan == plan ? entry : null;
        }

        void put(M3String source, M3RX.Plan plan, M3RX.Effect effect) {
            effects.set(slot(source, plan), new Entry(source, plan, effect));
        }

        private int slot(M3String source, M3RX.Plan plan) {
            long coordinate = source.coordinate();
            int hash = System.identityHashCode(source.owner()) ^ System.identityHashCode(plan)
                    ^ (int) coordinate ^ (int) (coordinate >>> 32);
            hash ^= hash >>> 16;
            return hash & (EFFECT_SLOTS - 1);
        }
    }

    private static final class Entry {
        final WeakReference<M3StringOwner> owner;
        final long coordinate;
        final M3RX.Plan plan;
        final M3RX.Effect effect;
        Entry(M3String source, M3RX.Plan plan, M3RX.Effect effect) {
            owner = new WeakReference<>(source.owner());
            coordinate = source.coordinate();
            this.plan = plan;
            this.effect = effect;
        }
    }

    private static boolean isAsciiAlphanumeric(char value) {
        return (value >= 'a' && value <= 'z')
                || (value >= 'A' && value <= 'Z')
                || (value >= '0' && value <= '9');
    }

    /** JDK default dot semantics (no DOTALL, no UNIX_LINES). */
    private static boolean isJdkLineTerminator(char value) {
        return value == '\n' || value == '\r' || value == '\u0085'
                || value == '\u2028' || value == '\u2029';
    }
}
