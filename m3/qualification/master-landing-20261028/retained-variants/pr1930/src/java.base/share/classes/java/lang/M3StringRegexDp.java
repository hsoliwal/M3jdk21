/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import jdk.internal.mindex.M3DotStarRelations;
import jdk.internal.mindex.M3RegexDpPlan;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Bounded Boolean full-match specialization behind String's unchanged Pattern fallback.
 * Pattern opcodes are derived metadata, never a second canonical spelling owner. Effects
 * memoize (program, canonical owner, exact range, incoming frontier) -> outgoing frontier.
 * Tuple children consume one continuous frontier; a component boundary is not a regex boundary.
 * Prepared exact class facts also answer standalone class runs, including supplementary text.
 * Captures, flags, search and unsupported syntax stay with Pattern. The dot/star DP lane
 * delegates repeated surrogate input to bounded code-point-correct fragment effects.
 *
 * <p>Synexia MIndexRegexProgram/MIndexRegexEffects supply the finite-state/composition
 * design lineage. This target-owned adaptation uses the existing M3 names and native owners,
 * not Synexia payloads, packages, JNI symbols or a runtime dependency.</p>
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
    private static final int MAX_BIT_ATOMS = 32;
    private static final int PLAN_SLOTS = 64;
    private static final int EFFECT_SLOTS = 256;
    private static final int MIN_EFFECT_UNITS = 16;
    private static final int MIN_NATIVE_UNITS = 256;
    private static final int MAX_TREE_DEPTH = 64;
    // Optional allocation is lazy: an allocation failure does not poison class initialization.
    private static volatile SoftReference<Tables> caches;
    private static volatile boolean preparationSuspended;
    private static volatile boolean nativeUnavailable;
    // Stateless access geometry only; optional allocation stays outside class initialization.
    private static volatile M3DotStarRelations.Access<M3String> RELATION_ACCESS;

    private M3StringRegexDp() { }

    /** UNKNOWN always means that the existing caller must execute Pattern.matches. */
    static int tryMatch(M3String source, String regex) {
        int n = regex.length();
        if (n < 1 || n > MAX_PATTERN_UNITS) return UNKNOWN;
        try {
            if (!heapAllowsPreparation()) return UNKNOWN;
            int prepared = preparedClassMatch(source, regex);
            if (prepared != UNKNOWN) return prepared;
            if (n < 2) return UNKNOWN;
            int m = source.length();
            if (m > MAX_TEXT_UNITS) return UNKNOWN;
            Tables cache = tables();
            Plan plan = prepare(regex, cache);
            if (plan == null || ((long) m + 1) * (plan.atoms.length + 1) > MAX_DP_CELLS) {
                return UNKNOWN;
            }
            // Existing ordered relation algebra can reuse an entire immutable owner/range
            // independently of its incoming frontier. Refusal retains every current lane below.
            if (plan.closure == null && m >= 256 && plan.atoms.length < Long.SIZE
                    && source.owner() instanceof M3StringTuple) {
                try {
                    int decision;
                    try {
                        decision = M3DotStarRelations.evaluate(source, relationAccess(), plan.atoms,
                                plan.atoms.length, 128,
                                Math.min(MAX_DP_CELLS, (long) m * (plan.atoms.length + 1)),
                                null, null).decision();
                    } finally {
                        Reference.reachabilityFence(source);
                    }
                    if (decision != UNKNOWN) return decision;
                } catch (OutOfMemoryError unavailable) {
                    preparationSuspended = true;
                    releaseCachedMetadata();
                    // Pressure refusal delegates to the caller's existing Pattern path.
                    return UNKNOWN;
                }
            }
            if (plan.closure == null) {
                int decision = rolling(source, plan);
                return decision != UNKNOWN ? decision : tryRepeatedUtf16(source, regex);
            }
            long state = evaluate(source.owner(), source.start(), m,
                    plan, plan.closure[0], cache, 0);
            return state == UNKNOWN ? tryRepeatedUtf16(source, regex)
                    : (state & (1L << plan.atoms.length)) != 0 ? MATCH : NO_MATCH;
        } catch (OutOfMemoryError unavailable) {
            preparationSuspended = true;
            releaseCachedMetadata();
            // Derived-state allocation is optional; the ordinary JDK path remains authoritative.
            return UNKNOWN;
        }
    }


    /**
     * Restore the qualified fragment/power mechanics only after the existing DP lane
     * declines. Ordinary class-fact, bit, native, relation and rolling decisions keep
     * their existing selectors. All preparation here is invocation-local and optional;
     * no additional cache, text payload, owner field or native pointer is retained.
     *
     * Synexia's m3jdk21-regex-utf16-20261015 receiver supplies the exact repeated-owner,
     * UTF-16 seam and power lineage. The current target Plan/native owners remain in use.
     */
    private static int tryRepeatedUtf16(M3String source, String regex) {
        if (source.length() < 128 || !worthPreparingRepeated(source)) return UNKNOWN;
        M3RegexDpPlan plan = M3RegexDpPlan.prepare(regex);
        if (plan == null || ((long) source.length() + 1) * plan.stateCount() > MAX_DP_CELLS) {
            return UNKNOWN;
        }
        try {
            M3RegexDpPlan.Effect effect = repeatedEffect(source, plan, new EffectWork(), 0);
            return effect == null ? UNKNOWN
                    : plan.accepts(effect.apply(plan.initialState())) ? MATCH : NO_MATCH;
        } finally {
            Reference.reachabilityFence(source);
        }
    }

    private static boolean worthPreparingRepeated(M3String source) {
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

    private static M3RegexDpPlan.Effect repeatedEffect(M3String source, M3RegexDpPlan plan,
            EffectWork work, int depth) {
        if (depth > 48 || !work.take(1)) return null;
        M3StringOwner owner = source.owner();
        M3String unit = source;
        int levels = 0;
        while (depth + levels < 48 && unit.start() == 0 && unit.length() == unit.owner().length
                && unit.owner() instanceof M3StringTuple tuple
                && tuple.left.owner() == tuple.right.owner()
                && tuple.left.coordinate() == tuple.right.coordinate()) {
            unit = tuple.left;
            levels++;
        }
        if (levels >= 8) {
            long cost = levels + 2L * plan.stateCount() * (plan.stateCount() + 1) * (levels + 1);
            if (!work.take(cost)) return null;
            M3RegexDpPlan.Effect one = repeatedEffect(unit, plan, work, depth + levels);
            return one == null ? null : one.repeated(1L << levels, null);
        }
        if (source.start() == 0 && source.length() == owner.length
                && owner instanceof M3StringTuple tuple) {
            M3RegexDpPlan.Effect first = repeatedEffect(tuple.left, plan, work, depth + 1);
            if (first == null) return null;
            M3RegexDpPlan.Effect second = repeatedEffect(tuple.right, plan, work, depth + 1);
            if (second == null || !work.take(2L * plan.stateCount() * (plan.stateCount() + 1))) return null;
            return first.then(second, null);
        }
        if (!work.take((long) source.length() * plan.stateCount())) return null;
        M3RegexDpPlan.Effect effect = plan.summarize(new EffectCursorView(source), null);
        if (effect == null) {
            // Reread only immutable canonical coordinates after legacy surrogate refusal.
            // Charge both possible UTF-16 relations; a boundary effect never retains text.
            if (!work.take(2L * source.length() * plan.stateCount())) return null;
            effect = plan.summarizeUtf16(new EffectCursorView(source), null);
        }
        return effect;
    }

    private static final class EffectWork {
        private long remaining = 262_144;
        boolean take(long amount) {
            if (amount < 0 || amount > remaining) return false;
            remaining -= amount;
            return true;
        }
    }

    /** Transient cursor only; the resulting effect contains primitive relations. */
    private static final class EffectCursorView implements CharSequence {
        private final M3String.CodeUnitCursor cursor;
        private final int length;
        EffectCursorView(M3String source) {
            cursor = new M3String.CodeUnitCursor(source);
            length = source.length();
        }
        @Override public int length() { return length; }
        @Override public char charAt(int index) {
            return cursor.at(java.util.Objects.checkIndex(index, length));
        }
        @Override public CharSequence subSequence(int start, int end) {
            throw new UnsupportedOperationException("regex effect traverses the complete view");
        }
        @Override public String toString() {
            throw new UnsupportedOperationException("no regex text shadow");
        }
    }


    private static final int NEGATED_CLASS = 1 << 15;

    /**
     * A complete one-class expression is exactly all/none membership plus code-point
     * cardinality. Consume prepared facts only: cold calls do not scan or retain text.
     */
    private static int preparedClassMatch(M3String source, String regex) {
        int token = commonClass(regex);
        if (token == 0) return UNKNOWN;
        long bounds = repetitionBounds(regex, token >>> 16);
        if (bounds < 0) return UNKNOWN;
        M3StringFacts facts = source.factsIfPrepared();
        if (facts == null || facts.utf16Length != source.length()) return UNKNOWN;
        int count = facts.codePointCount;
        if (count < (int) (bounds >>> 32) || count > (int) bounds) return NO_MATCH;
        int mask = token & M3StringFacts.REGEX_CLASSES;
        boolean matches = (token & NEGATED_CLASS) == 0
                ? (facts.regexAllClasses & mask) != 0
                : (facts.regexAnyClasses & mask) == 0;
        return matches ? MATCH : NO_MATCH;
    }

    /** Packed consumed UTF-16 length, negation and one exact class bit; zero refuses. */
    private static int commonClass(String regex) {
        char first = regex.charAt(0);
        if (first == '.') return (1 << 16) | M3StringFacts.REGEX_DOT;
        if (first == '\\' && regex.length() >= 2) {
            char kind = regex.charAt(1);
            int mask = switch (kind) {
                case 'd', 'D' -> M3StringFacts.REGEX_DIGIT;
                case 'w', 'W' -> M3StringFacts.REGEX_WORD;
                case 's', 'S' -> M3StringFacts.REGEX_SPACE;
                case 'h', 'H' -> M3StringFacts.REGEX_HORIZONTAL;
                case 'v', 'V' -> M3StringFacts.REGEX_VERTICAL;
                default -> 0;
            };
            if (mask != 0) {
                return (2 << 16) | mask
                        | (kind >= 'A' && kind <= 'Z' ? NEGATED_CLASS : 0);
            }
            if ((kind != 'p' && kind != 'P') || regex.length() < 4
                    || regex.charAt(2) != '{') return 0;
            int length;
            if (regex.startsWith("Lower}", 3)) {
                mask = M3StringFacts.REGEX_LOWER; length = 9;
            }
            else if (regex.startsWith("Upper}", 3)) {
                mask = M3StringFacts.REGEX_UPPER; length = 9;
            }
            else if (regex.startsWith("ASCII}", 3)) {
                mask = M3StringFacts.REGEX_ASCII; length = 9;
            }
            else if (regex.startsWith("Alpha}", 3)) {
                mask = M3StringFacts.REGEX_ALPHA; length = 9;
            }
            else if (regex.startsWith("Digit}", 3)) {
                mask = M3StringFacts.REGEX_DIGIT; length = 9;
            }
            else if (regex.startsWith("Alnum}", 3)) {
                mask = M3StringFacts.REGEX_ALNUM; length = 9;
            }
            else if (regex.startsWith("XDigit}", 3)) {
                mask = M3StringFacts.REGEX_HEX; length = 10;
            }
            else if (regex.startsWith("Space}", 3)) {
                mask = M3StringFacts.REGEX_SPACE; length = 9;
            }
            else return 0;
            return (length << 16) | mask | (kind == 'P' ? NEGATED_CLASS : 0);
        }
        if (first != '[' || regex.length() < 2) return 0;
        boolean negated = regex.charAt(1) == '^';
        int start = negated ? 2 : 1;
        int mask;
        int length;
        if (regex.startsWith("0-9]", start)) {
            mask = M3StringFacts.REGEX_DIGIT; length = 4;
        }
        else if (regex.startsWith("a-z]", start)) {
            mask = M3StringFacts.REGEX_LOWER; length = 4;
        }
        else if (regex.startsWith("A-Z]", start)) {
            mask = M3StringFacts.REGEX_UPPER; length = 4;
        }
        else if (regex.startsWith("a-zA-Z]", start)) {
            mask = M3StringFacts.REGEX_ALPHA; length = 7;
        }
        else if (regex.startsWith("A-Za-z]", start)) {
            mask = M3StringFacts.REGEX_ALPHA; length = 7;
        }
        else if (regex.startsWith("a-zA-Z0-9]", start)) {
            mask = M3StringFacts.REGEX_ALNUM; length = 10;
        }
        else if (regex.startsWith("A-Za-z0-9]", start)) {
            mask = M3StringFacts.REGEX_ALNUM; length = 10;
        }
        else if (regex.startsWith("a-zA-Z_0-9]", start)) {
            mask = M3StringFacts.REGEX_WORD; length = 11;
        }
        else if (regex.startsWith("a-zA-Z0-9_]", start)) {
            mask = M3StringFacts.REGEX_WORD; length = 11;
        }
        else if (regex.startsWith("A-Za-z0-9_]", start)) {
            mask = M3StringFacts.REGEX_WORD; length = 11;
        }
        else if (regex.startsWith("0-9a-fA-F]", start)) {
            mask = M3StringFacts.REGEX_HEX; length = 10;
        }
        else if (regex.startsWith("0-9A-Fa-f]", start)) {
            mask = M3StringFacts.REGEX_HEX; length = 10;
        }
        else if (regex.startsWith("A-Fa-f0-9]", start)) {
            mask = M3StringFacts.REGEX_HEX; length = 10;
        }
        else if (regex.startsWith("a-fA-F0-9]", start)) {
            mask = M3StringFacts.REGEX_HEX; length = 10;
        }
        else if (regex.startsWith("0-9a-f]", start)) {
            mask = M3StringFacts.REGEX_LOWER_HEX; length = 7;
        }
        else if (regex.startsWith("0-9A-F]", start)) {
            mask = M3StringFacts.REGEX_UPPER_HEX; length = 7;
        }
        else return 0;
        return ((start + length) << 16) | mask | (negated ? NEGATED_CLASS : 0);
    }

    /** Inclusive minimum/maximum counts; -1 refuses syntax without masking Pattern errors. */
    private static long repetitionBounds(String regex, int start) {
        int length = regex.length();
        if (start == length) return (1L << 32) | 1L;
        if (start + 1 == length) {
            return switch (regex.charAt(start)) {
                case '?' -> 1L;
                case '*' -> Integer.MAX_VALUE;
                case '+' -> (1L << 32) | Integer.MAX_VALUE;
                default -> -1L;
            };
        }
        if (regex.charAt(start) != '{') return -1L;
        long first = decimalBound(regex, start + 1);
        if (first < 0) return -1L;
        int minimum = (int) first;
        int end = (int) (first >>> 32);
        if (end == length - 1 && regex.charAt(end) == '}') {
            return ((long) minimum << 32) | minimum;
        }
        if (end >= length || regex.charAt(end) != ',') return -1L;
        end++;
        if (end == length - 1 && regex.charAt(end) == '}') {
            return ((long) minimum << 32) | Integer.MAX_VALUE;
        }
        long last = decimalBound(regex, end);
        if (last < 0 || (int) (last >>> 32) != length - 1
                || regex.charAt(length - 1) != '}') return -1L;
        int maximum = (int) last;
        return minimum > maximum ? -1L : ((long) minimum << 32) | maximum;
    }

    /** Parse only ASCII decimal syntax, refusing overflow before multiplication. */
    private static long decimalBound(String regex, int start) {
        int value = 0;
        int end = start;
        while (end < regex.length()) {
            int digit = regex.charAt(end) - '0';
            if (digit < 0 || digit > 9) break;
            if (value > (Integer.MAX_VALUE - digit) / 10) return -1L;
            value = value * 10 + digit;
            end++;
        }
        return end == start ? -1L : ((long) end << 32) | value;
    }

    private static Tables tables() {
        SoftReference<Tables> reference = caches;
        Tables current = reference == null ? null : reference.get();
        if (current != null) return current;
        synchronized (M3StringRegexDp.class) {
            reference = caches;
            current = reference == null ? null : reference.get();
            if (current == null) {
                current = new Tables();
                caches = new SoftReference<>(current);
            }
        }
        return current;
    }

    /** Recomputable metadata only; never clears or closes canonical text owners. */
    private static void releaseCachedMetadata() {
        SoftReference<Tables> reference = caches;
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

    private static Plan prepare(String regex, Tables cache) {
        int slot = regex.hashCode() & (PLAN_SLOTS - 1);
        Plan found = cache.plans.get(slot);
        // The hash selects a slot, never establishes semantic/program identity.
        if (found != null && found.sameExpression(regex)) return found;
        int n = regex.length();
        int[] atoms = new int[n];
        int count = 0;
        int stars = 0;
        for (int index = 0; index < n; index++) {
            char unit = regex.charAt(index);
            if (unit == '*') {
                if (count == 0 || (atoms[count - 1] & STAR) != 0) return null;
                atoms[count - 1] |= STAR;
                stars++;
            } else if (unit == '.' || isAsciiAlphanumeric(unit)) {
                atoms[count++] = unit;
            } else {
                return null;
            }
        }
        if (stars == 0) return null; // Preserve the existing literal route.
        Plan plan = new Plan(Arrays.copyOf(atoms, count), n);
        cache.plans.set(slot, plan);
        return plan;
    }

    private static long evaluate(M3StringOwner owner, int start, int length,
            Plan plan, long incoming, Tables cache, int depth) {
        if (length == 0) return incoming;
        if (depth > MAX_TREE_DEPTH) return UNKNOWN;
        long coordinate = ((long) start << 32) | Integer.toUnsignedLong(length);
        int slot = (System.identityHashCode(owner) ^ System.identityHashCode(plan)
                ^ Long.hashCode(Long.rotateLeft(coordinate, 13) ^ incoming)) & (EFFECT_SLOTS - 1);
        boolean retain = length >= MIN_EFFECT_UNITS;
        if (retain) {
            Effect effect = cache.effects.get(slot);
            if (effect != null && effect.owner.get() == owner && effect.plan == plan
                    && effect.coordinate == coordinate && effect.incoming == incoming) {
                return effect.outgoing;
            }
        }
        long outgoing;
        if (owner instanceof M3StringTuple tuple) {
            int leftLength = tuple.left.length();
            int leftCount = start < leftLength ? Math.min(length, leftLength - start) : 0;
            outgoing = incoming;
            if (leftCount != 0) {
                outgoing = evaluate(tuple.left.owner(), tuple.left.start() + start,
                        leftCount, plan, outgoing, cache, depth + 1);
            }
            if (outgoing != UNKNOWN && leftCount < length) {
                int rightStart = Math.max(0, start - leftLength);
                outgoing = evaluate(tuple.right.owner(), tuple.right.start() + rightStart,
                        length - leftCount, plan, outgoing, cache, depth + 1);
            }
        } else {
            outgoing = owner instanceof M3StringAtom atom
                    ? advance(atom, start, length, plan, incoming)
                    : advanceJava(owner, start, length, plan, incoming);
        }
        if (retain) cache.effects.set(slot,
                new Effect(new WeakReference<>(owner), coordinate, plan, incoming, outgoing));
        return outgoing;
    }

    private static long advance(M3StringAtom owner, int start, int length,
            Plan plan, long incoming) {
        try {
            if (!nativeUnavailable && length >= MIN_NATIVE_UNITS) {
                try {
                    return advance0(owner.address, owner.length, start, length,
                            owner.storageWidth, owner.bigEndian, incoming, plan.atoms, plan.closure);
                } catch (UnsatisfiedLinkError unavailable) {
                    // An older/unavailable libjava may use the exact Java kernel; proof runs
                    // invoke the required native entry separately and never count this as JNI.
                    nativeUnavailable = true;
                }
            }
            return advanceJava(owner, start, length, plan, incoming);
        } finally {
            // The pool may reclaim native blocks once their canonical owner is unreachable.
            Reference.reachabilityFence(owner);
        }
    }

    private static long advanceJava(M3StringOwner owner, int start, int length,
            Plan plan, long state) {
        for (int offset = 0; offset < length; offset++) {
            char unit = owner.charAt(start + offset);
            // Do not early-return for an empty state: a later surrogate must still refuse.
            if (Character.isSurrogate(unit)) return UNKNOWN;
            long accepts = unit < 128 ? plan.ascii[unit]
                    : isJdkLineTerminator(unit) ? 0L : plan.dot;
            long live = state & accepts;
            long destinations = (live & plan.star) | ((live & ~plan.star) << 1);
            state = close(destinations, plan.star);
        }
        return state;
    }

    private static long close(long bits, long star) {
        if (bits == 0L) return 0L;
        // A carry closes each starred run from its first seed through its exit.
        // At most 32 atom bits keep the sum below 2^33.
        return bits | (star ^ (star + (bits & star)));
    }

    /** Larger admitted patterns use one row and a saved previous-row diagonal. */
    private static int rolling(M3String source, Plan plan) {
        int count = plan.atoms.length;
        boolean[] row = new boolean[count + 1];
        row[0] = true;
        for (int j = 1; j <= count; j++) {
            row[j] = (plan.atoms[j - 1] & STAR) != 0 && row[j - 1];
        }
        // Reuse canonical leaf-local UTF-16 coordinates; no copied text or new owner.
        M3String.CodeUnitCursor units = new M3String.CodeUnitCursor(source);
        for (int i = 0; i < source.length(); i++) {
            char value = units.at(i);
            if (Character.isSurrogate(value)) return UNKNOWN;
            boolean diagonal = row[0];
            row[0] = false;
            for (int j = 1; j <= count; j++) {
                boolean previous = row[j];
                int atom = plan.atoms[j - 1];
                char required = (char) (atom & CODE_UNIT);
                boolean accepts = required == '.' ? !isJdkLineTerminator(value) : required == value;
                // Left is already the current row; previous and diagonal are still the old row.
                row[j] = (atom & STAR) != 0
                        ? row[j - 1] || (accepts && previous)
                        : accepts && diagonal;
                diagonal = previous;
            }
        }
        return row[count] ? MATCH : NO_MATCH;
    }

    private static native long advance0(long address, int capacity, int start, int length,
            int width, boolean bigEndian, long incoming, int[] atoms, long[] closure);

    /** Includes obsolete plans retained by every possible effect entry, not only current plans. */
    private static long maximumRetainedPrimitiveBytes() {
        long perPlan = MAX_PATTERN_UNITS * (long) Integer.BYTES
                + (MAX_BIT_ATOMS + 1L + 128L + 2L) * Long.BYTES + Integer.BYTES;
        return (PLAN_SLOTS + (long) EFFECT_SLOTS) * perPlan
                + EFFECT_SLOTS * 3L * Long.BYTES;
    }

    /** Lazily publish stateless coordinates; never retain a String, owner, range or native handle. */
    private static M3DotStarRelations.Access<M3String> relationAccess() {
        M3DotStarRelations.Access<M3String> current = RELATION_ACCESS;
        if (current != null) return current;
        synchronized (M3StringRegexDp.class) {
            current = RELATION_ACCESS;
            if (current == null) {
                current = new M3DotStarRelations.Access<>() {
                    @Override public Object owner(M3String value) { return value.owner(); }
                    @Override public long coordinate(M3String value) { return value.coordinate(); }
                    @Override public int length(M3String value) { return value.length(); }
                    @Override public int codeUnitAt(M3String value, int index) {
                        return value.charAt(index);
                    }
                    @Override public M3String first(M3String value) {
                        if (!(value.owner() instanceof M3StringTuple tuple)) return null;
                        int start = value.start();
                        int end = value.end();
                        int split = tuple.left.length();
                        return start >= split
                                ? tuple.right.slice(start - split, end - split)
                                : tuple.left.slice(start, Math.min(end, split));
                    }
                    @Override public M3String second(M3String value) {
                        if (!(value.owner() instanceof M3StringTuple tuple)) return null;
                        int split = tuple.left.length();
                        return value.start() < split && value.end() > split
                                ? tuple.right.slice(0, value.end() - split) : null;
                    }
                };
                RELATION_ACCESS = current;
            }
        }
        return current;
    }

    private static boolean isAsciiAlphanumeric(char value) {
        return (value >= 'a' && value <= 'z') || (value >= 'A' && value <= 'Z')
                || (value >= '0' && value <= '9');
    }

    private static boolean isJdkLineTerminator(char value) {
        return value == '\n' || value == '\r' || value == '\u0085'
                || value == '\u2028' || value == '\u2029';
    }

    private static final class Tables {
        final AtomicReferenceArray<Plan> plans = new AtomicReferenceArray<>(PLAN_SLOTS);
        final AtomicReferenceArray<Effect> effects = new AtomicReferenceArray<>(EFFECT_SLOTS);
    }

    private static final class Plan {
        final int[] atoms;
        final int units;
        final long[] closure;
        final long[] ascii;
        final long star;
        final long dot;

        Plan(int[] atoms, int units) {
            this.atoms = atoms;
            this.units = units;
            if (atoms.length > MAX_BIT_ATOMS) {
                closure = null;
                ascii = null;
                star = dot = 0L;
                return;
            }
            closure = new long[atoms.length + 1];
            ascii = new long[128];
            long stars = 0L;
            long dots = 0L;
            closure[atoms.length] = 1L << atoms.length;
            for (int i = atoms.length - 1; i >= 0; i--) {
                long bit = 1L << i;
                boolean repeated = (atoms[i] & STAR) != 0;
                closure[i] = bit | (repeated ? closure[i + 1] : 0L);
                if (repeated) stars |= bit;
                char required = (char) atoms[i];
                if (required == '.') {
                    dots |= bit;
                    for (int c = 0; c < ascii.length; c++) {
                        if (!isJdkLineTerminator((char) c)) ascii[c] |= bit;
                    }
                } else {
                    ascii[required] |= bit;
                }
            }
            star = stars;
            dot = dots;
        }

        boolean sameExpression(String regex) {
            if (regex.length() != units) return false;
            int offset = 0;
            for (int atom : atoms) {
                if (regex.charAt(offset++) != (char) atom) return false;
                if ((atom & STAR) != 0 && regex.charAt(offset++) != '*') return false;
            }
            return true;
        }
    }

    private record Effect(WeakReference<M3StringOwner> owner, long coordinate,
            Plan plan, long incoming, long outgoing) { }
}
