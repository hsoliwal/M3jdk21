/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import jdk.internal.mindex.M3DotStarRelations;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Bounded Boolean full-match specialization behind String's unchanged Pattern fallback.
 * Pattern opcodes are derived metadata, never a second canonical spelling owner. Effects
 * memoize (program, canonical owner, exact range, incoming frontier) -> outgoing frontier.
 * Tuple children consume one continuous frontier; a component boundary is not a regex boundary.
 * Captures, flags, search, supplementary characters and unsupported syntax stay with Pattern.
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
    private static volatile Tables tables;
    private static volatile boolean nativeUnavailable;
    // Stateless access geometry only; optional allocation stays outside class initialization.
    private static volatile M3DotStarRelations.Access<M3String> RELATION_ACCESS;

    private M3StringRegexDp() { }

    /** UNKNOWN always means that the existing caller must execute Pattern.matches. */
    static int tryMatch(M3String source, String regex) {
        int n = regex.length();
        if (n < 2 || n > MAX_PATTERN_UNITS) return UNKNOWN;
        int m = source.length();
        if (m > MAX_TEXT_UNITS) return UNKNOWN;
        try {
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
                    // Relation summaries are optional; keep the existing bit/rolling/native path.
                }
            }
            if (plan.closure == null) return rolling(source, plan);
            long state = evaluate(source.owner(), source.start(), m,
                    plan, plan.closure[0], cache, 0);
            return state == UNKNOWN ? UNKNOWN
                    : (state & (1L << plan.atoms.length)) != 0 ? MATCH : NO_MATCH;
        } catch (OutOfMemoryError unavailable) {
            // Derived-state allocation is optional; the ordinary JDK path remains authoritative.
            return UNKNOWN;
        }
    }

    private static Tables tables() {
        Tables current = tables;
        if (current != null) return current;
        synchronized (M3StringRegexDp.class) {
            current = tables;
            if (current == null) tables = current = new Tables();
        }
        return current;
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
            outgoing = advance((M3StringAtom) owner, start, length, plan, incoming);
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
