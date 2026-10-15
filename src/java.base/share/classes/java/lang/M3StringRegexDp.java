/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import jdk.internal.mindex.M3RegexDpPlan;

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

    // Lazy allocation avoids poisoning class initialization when optional metadata is refused.
    private static volatile SoftReference<Caches> caches;
    private static volatile boolean preparationSuspended;
    private static final int PLAN_SLOTS = 64;
    private static final int EFFECT_SLOTS = 128;
    private static final long MAX_EFFECT_WORK = 262_144;

    private M3StringRegexDp() { }

    /**
     * A three-state, contract-preserving decision: UNKNOWN means use Pattern.matches.
     * Optional plans/effects are primitive metadata, weak-owner/exact-coordinate keyed.
     * No fields or spelling payload are added to String, M3String or its canonical owners.
     */
    static int tryMatch(M3String source, String regex) {
        int n = regex.length();
        if (n < 2 || n > MAX_PATTERN_UNITS) return UNKNOWN;

        try {
            int prepared = tryPrepared(source, regex);
            if (prepared != UNKNOWN) return prepared;
        } catch (OutOfMemoryError unavailable) {
            preparationSuspended = true;
            releaseCachedMetadata();
            // Preparation is optional; the existing bounded DP/Pattern path remains available.
        }

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

        // Nullable prefix: every atom of an empty-text-matching prefix must be starred.
        boolean[] previous = new boolean[count + 1];
        previous[0] = true;
        for (int j = 1; j <= count; j++) {
            previous[j] = (atoms[j - 1] & STAR) != 0 && previous[j - 1];
        }

        for (int i = 0; i < m; i++) {
            char value = source.charAt(i);
            // java.util.regex matches Unicode code points, not arbitrary halves of
            // surrogate pairs. Preserve that contract through the JDK fallback.
            if (Character.isSurrogate(value)) return UNKNOWN;
            // Save the old diagonal before updating this row in place.
            boolean diagonal = previous[0];
            previous[0] = false;
            for (int j = 1; j <= count; j++) {
                boolean above = previous[j];
                int atom = atoms[j - 1];
                char required = (char) (atom & CODE_UNIT);
                boolean accepts = required == '.'
                        ? !isJdkLineTerminator(value)
                        : required == value;
                previous[j] = (atom & STAR) != 0
                        ? previous[j - 1] || (accepts && above)
                        : accepts && diagonal;
                diagonal = above;
            }
        }
        return previous[count] ? MATCH : NO_MATCH;
    }


    private static int tryPrepared(M3String source, String regex) {
        if (source.length() > MAX_TEXT_UNITS || !worthPreparing(source)) return UNKNOWN;
        if (!heapAllowsPreparation()) return UNKNOWN;
        Caches cache = caches();
        M3RegexDpPlan plan = plan(regex, cache);
        if (plan == null || ((long) source.length() + 1) * plan.stateCount() > MAX_DP_CELLS) {
            return UNKNOWN;
        }
        // Hierarchical preparation is reserved for the admitted repetition cohort.
        // Other inputs retain the original rolling rows; effects cover every entry state.
        if (source.length() >= 128 && source.owner() instanceof M3StringTuple) {
            M3RegexDpPlan.Effect effect = effect(source, plan, cache, new Work(), 0);
            if (effect != null) return plan.accepts(effect.apply(plan.initialState())) ? MATCH : NO_MATCH;
        }
        // Never replace the cheap scalar/ordinary-composite rolling path merely because
        // a plan can be compiled. Cold-plan measurements are a selection gate, not a speed claim.
        return UNKNOWN;
    }

    private static boolean worthPreparing(M3String source) {
        // The selected candidate cohort is a structurally repeated canonical DAG.
        // 256 occurrences is an internal cost gate, not a new semantic/input restriction:
        // every other source still uses the unchanged rolling-DP/Pattern implementation.
        int copies = 1;
        M3String node = source;
        while (copies < 256 && node.start() == 0 && node.length() == node.owner().length
                && node.owner() instanceof M3StringTuple tuple
                && tuple.left.owner() == tuple.right.owner()
                && tuple.left.coordinate() == tuple.right.coordinate()) {
            copies <<= 1;
            node = tuple.left;
        }
        return copies >= 256;
    }

    private static Caches caches() {
        SoftReference<Caches> reference = caches;
        Caches found = reference == null ? null : reference.get();
        if (found != null) return found;
        synchronized (M3StringRegexDp.class) {
            reference = caches;
            found = reference == null ? null : reference.get();
            if (found == null) {
                found = new Caches();
                caches = new SoftReference<>(found);
            }
        }
        return found;
    }

    /** Recomputable metadata only; never clears or closes canonical text owners. */
    private static void releaseCachedMetadata() {
        SoftReference<Caches> reference = caches;
        if (reference != null) reference.clear();
    }

    /** Advisory heap snapshot, not a reservation or an OS/native-memory measure. */
    private static boolean heapAllowsPreparation() {
        Runtime runtime = Runtime.getRuntime();
        boolean allowed = hasHeapHeadroom(runtime.maxMemory(), runtime.totalMemory(),
                runtime.freeMemory(), preparationSuspended);
        preparationSuspended = !allowed;
        if (!allowed) releaseCachedMetadata();
        return allowed;
    }

    /** Overflow-safe normalization and hysteresis; no caller budget or forced GC. */
    private static boolean hasHeapHeadroom(long maximum, long total, long free,
            boolean suspended) {
        if (maximum <= 0) return false;
        long committed = Math.max(0, Math.min(maximum, total));
        long unused = Math.max(0, Math.min(committed, free));
        long headroom = maximum - (committed - unused);
        long reserve = Math.max(suspended ? 1L << 20 : 1L << 19,
                maximum >>> (suspended ? 4 : 5));
        return headroom >= reserve;
    }

    private static M3RegexDpPlan plan(String regex, Caches cache) {
        // Do not admit a flat/bootstrap String into a new spelling pool for this optimization.
        M3String pattern = regex.m3();
        if (pattern == null) return M3RegexDpPlan.prepare(regex);
        M3StringOwner owner = pattern.owner();
        long coordinate = pattern.coordinate();
        int slot = slot(owner, coordinate, PLAN_SLOTS);
        PlanEntry old = cache.plans.get(slot);
        if (old != null && old.owner.get() == owner && old.coordinate == coordinate) return old.plan;
        M3RegexDpPlan made = M3RegexDpPlan.prepare(regex);
        if (made != null) cache.plans.set(slot, new PlanEntry(new WeakReference<>(owner), coordinate, made));
        return made;
    }

    private static M3RegexDpPlan.Effect effect(M3String source, M3RegexDpPlan plan,
            Caches cache, Work work, int depth) {
        if (depth > 48 || !work.take(1)) return null;
        M3StringOwner owner = source.owner();
        long coordinate = source.coordinate();
        int slot = slot(owner, coordinate, EFFECT_SLOTS);
        EffectEntry old = cache.effects.get(slot);
        if (old != null && old.owner.get() == owner && old.coordinate == coordinate && old.plan == plan) {
            return old.effect;
        }
        M3RegexDpPlan.Effect made;
        M3String unit = source;
        int levels = 0;
        // Equality here is exact canonical owner AND coordinate, not a text/hash guess.
        while (depth + levels < 48 && unit.start() == 0 && unit.length() == unit.owner().length
                && unit.owner() instanceof M3StringTuple repeated
                && repeated.left.owner() == repeated.right.owner()
                && repeated.left.coordinate() == repeated.right.coordinate()) {
            unit = repeated.left;
            levels++;
        }
        if (levels >= 8) {
            // A 2^levels power costs levels squares and one identity composition.
            // Charge skipped node visits as well; do not widen the preparation budget.
            long cost = levels + 2L * plan.stateCount() * (plan.stateCount() + 1) * (levels + 1);
            if (!work.take(cost)) return null;
            M3RegexDpPlan.Effect one = effect(unit, plan, cache, work, depth + levels);
            if (one == null) return null;
            made = one.repeated(1L << levels, null);
        } else if (source.start() == 0 && source.length() == owner.length && owner instanceof M3StringTuple tuple) {
            M3RegexDpPlan.Effect left = effect(tuple.left, plan, cache, work, depth + 1);
            if (left == null) return null;
            M3RegexDpPlan.Effect right = effect(tuple.right, plan, cache, work, depth + 1);
            if (right == null || !work.take(2L * plan.stateCount() * (plan.stateCount() + 1))) return null;
            made = left.then(right, null);
        } else {
            if (!work.take((long) source.length() * plan.stateCount())) return null;
            made = plan.summarize(new CursorView(source), null);
            if (made == null) {
                // Immutable canonical input may be reread after legacy surrogate refusal.
                // The boundary-aware lane retains two relations at most, never text.
                if (!work.take(2L * source.length() * plan.stateCount())) return null;
                made = plan.summarizeUtf16(new CursorView(source), null);
            }
        }
        if (made != null) cache.effects.set(slot,
                new EffectEntry(new WeakReference<>(owner), coordinate, plan, made));
        return made;
    }

    private static int slot(M3StringOwner owner, long coordinate, int size) {
        long mix = coordinate ^ (coordinate >>> 32) ^ System.identityHashCode(owner);
        mix ^= mix >>> 17;
        return (int) mix & (size - 1);
    }

    private static final class Work {
        private long remaining = MAX_EFFECT_WORK;
        boolean take(long amount) {
            if (amount > remaining) return false;
            remaining -= amount;
            return true;
        }
    }

    private static final class Caches {
        final AtomicReferenceArray<PlanEntry> plans = new AtomicReferenceArray<>(PLAN_SLOTS);
        final AtomicReferenceArray<EffectEntry> effects = new AtomicReferenceArray<>(EFFECT_SLOTS);
    }
    private record PlanEntry(WeakReference<M3StringOwner> owner, long coordinate, M3RegexDpPlan plan) { }
    private record EffectEntry(WeakReference<M3StringOwner> owner, long coordinate,
                               M3RegexDpPlan plan, M3RegexDpPlan.Effect effect) { }

    /** Transient leaf cursor adapter. An Effect never retains this view or its source. */
    private static final class CursorView implements CharSequence {
        private final M3String.CodeUnitCursor cursor;
        private final int length;
        CursorView(M3String source) { cursor = new M3String.CodeUnitCursor(source); length = source.length(); }
        @Override public int length() { return length; }
        @Override public char charAt(int index) { return cursor.at(java.util.Objects.checkIndex(index, length)); }
        @Override public CharSequence subSequence(int start, int end) {
            throw new UnsupportedOperationException("regex effect traverses the complete view");
        }
        @Override public String toString() { throw new UnsupportedOperationException("no regex text shadow"); }
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
