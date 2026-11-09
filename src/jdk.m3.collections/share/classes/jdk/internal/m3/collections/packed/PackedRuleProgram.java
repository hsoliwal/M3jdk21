/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections.packed;

import java.util.Arrays;

/**
 * Compound semantic rules stored entirely in parallel primitive lanes.
 *
 * <p>Each rule owns five fact masks:
 * <ul>
 *   <li>{@code allOn}: every listed fact must be active;</li>
 *   <li>{@code anyOn}: at least one listed fact must be active (empty means no any-condition);</li>
 *   <li>{@code allOff}: every listed fact must be inactive;</li>
 *   <li>{@code setMask}: facts activated when the rule matches;</li>
 *   <li>{@code clearMask}: facts cleared when the rule matches.</li>
 * </ul>
 *
 * <p>An optional unsigned-byte scalar range can be attached to the same rule.
 * No rule/expression/node object is retained per rule. The representation is:
 *
 * <pre>{@code
 * long[] allOn
 * long[] anyOn
 * long[] allOff
 * long[] setMask
 * long[] clearMask
 * int[]  scalarIndex
 * byte[] scalarMin
 * byte[] scalarMax
 * byte[] flags
 * }</pre>
 *
 * <p>The program is mutable during construction and is not thread-safe. A
 * {@link PackedSemanticMachine} freezes it by convention when the machine is compiled.
 *
 * @since 1.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/semantic/PackedRuleProgram.java}
 * blob {@code cbcba906e791d067ead9e0e32e28d5a695e68423} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedRuleProgram {
    private final int factCount;
    private final int wordCount;
    private final int scalarCount;

    private long[] allOn;
    private long[] anyOn;
    private long[] allOff;
    private long[] setMask;
    private long[] clearMask;

    private int[] scalarIndex;
    private byte[] scalarMin;
    private byte[] scalarMax;
    private byte[] flags;

    private int ruleCount;

    private static final byte HAS_ANY = 1;
    private static final byte HAS_SCALAR = 2;

    /**
     * Creates a rule program with default initial capacity for eight rules.
     *
     * @param factCount semantic fact count
     * @param scalarCount scalar-channel count
     */
    public PackedRuleProgram(int factCount, int scalarCount) {
        this(factCount, scalarCount, 8);
    }

    /**
     * Creates an empty rule program.
     *
     * @param factCount semantic fact count; must be positive
     * @param scalarCount scalar-channel count; may be zero
     * @param expectedRules initial rule capacity; may be zero
     * @throws IllegalArgumentException if a dimension is invalid
     * @throws ArithmeticException if primitive storage sizing overflows
     */
    public PackedRuleProgram(int factCount, int scalarCount, int expectedRules) {
        if (factCount <= 0) {
            throw new IllegalArgumentException("factCount");
        }
        if (scalarCount < 0 || expectedRules < 0) {
            throw new IllegalArgumentException("negative size");
        }
        this.factCount = factCount;
        this.wordCount = PackedSemanticMasks.wordCount(factCount);
        this.scalarCount = scalarCount;
        int capacity = Math.max(1, expectedRules);
        int words = Math.multiplyExact(capacity, wordCount);
        this.allOn = new long[words];
        this.anyOn = new long[words];
        this.allOff = new long[words];
        this.setMask = new long[words];
        this.clearMask = new long[words];
        this.scalarIndex = new int[capacity];
        this.scalarMin = new byte[capacity];
        this.scalarMax = new byte[capacity];
        this.flags = new byte[capacity];
    }

    /**
     * Returns the fact count understood by every rule mask.
     *
     * @return semantic fact count
     */
    public int factCount() {
        return factCount;
    }

    /**
     * Returns the scalar-channel count addressable by scalar rules.
     *
     * @return scalar count
     */
    public int scalarCount() {
        return scalarCount;
    }

    /**
     * Returns the primitive words in each rule mask.
     *
     * @return {@code ceil(factCount / 64)}
     */
    public int wordCount() {
        return wordCount;
    }

    /**
     * Returns the number of configured rules.
     *
     * @return active rule count
     */
    public int ruleCount() {
        return ruleCount;
    }

    /**
     * Returns the currently allocated rule capacity.
     *
     * @return rule slots retained by all parallel lanes
     */
    public int capacity() {
        return flags.length;
    }

    /**
     * Returns retained primitive payload bytes for the allocated capacity.
     *
     * <p>Java object and array headers are excluded.
     *
     * @return primitive payload byte estimate
     */
    public long payloadBytes() {
        long longWords = (long) allOn.length
                + anyOn.length
                + allOff.length
                + setMask.length
                + clearMask.length;
        long longBytes = longWords * Long.BYTES;
        long intBytes = (long) scalarIndex.length * Integer.BYTES;
        long byteBytes = (long) scalarMin.length + scalarMax.length + flags.length;
        return longBytes + intBytes + byteBytes;
    }

    /**
     * Adds a fact-only compound rule.
     *
     * <p>Every input mask must contain exactly {@link #wordCount()} longs.
     * The supplied arrays are copied into primitive program lanes; later caller mutation
     * does not modify the compiled rule.
     *
     * @param requireAllOn facts that must all be active
     * @param requireAnyOn facts where at least one must be active; empty mask disables this test
     * @param requireAllOff facts that must all be inactive
     * @param set facts to activate when the rule matches
     * @param clear facts to clear when the rule matches
     * @return zero-based rule index
     * @throws IllegalArgumentException if any mask has the wrong word length
     */
    public int addRule(
            long[] requireAllOn,
            long[] requireAnyOn,
            long[] requireAllOff,
            long[] set,
            long[] clear) {
        return addInternal(
                requireAllOn,
                requireAnyOn,
                requireAllOff,
                -1,
                0,
                255,
                set,
                clear);
    }

    /**
     * Adds a compound rule with an inclusive unsigned-byte scalar range condition.
     *
     * <p>The fact conditions and scalar range must all match. This supports quantized
     * thresholds and hysteresis without retaining a floating-point rule object.
     *
     * @param requireAllOn facts that must all be active
     * @param requireAnyOn facts where at least one must be active
     * @param requireAllOff facts that must all be inactive
     * @param scalar scalar-channel index
     * @param minInclusive minimum unsigned scalar value in {@code 0..255}
     * @param maxInclusive maximum unsigned scalar value in {@code 0..255}
     * @param set facts to activate
     * @param clear facts to clear
     * @return zero-based rule index
     * @throws IndexOutOfBoundsException if the scalar index is invalid
     * @throws IllegalArgumentException if a scalar bound/mask is invalid
     */
    public int addScalarRule(
            long[] requireAllOn,
            long[] requireAnyOn,
            long[] requireAllOff,
            int scalar,
            int minInclusive,
            int maxInclusive,
            long[] set,
            long[] clear) {
        checkScalar(scalar);
        PackedU8.require(minInclusive);
        PackedU8.require(maxInclusive);
        if (minInclusive > maxInclusive) {
            throw new IllegalArgumentException("min > max");
        }
        return addInternal(
                requireAllOn,
                requireAnyOn,
                requireAllOff,
                scalar,
                minInclusive,
                maxInclusive,
                set,
                clear);
    }

    /**
     * Shrinks all parallel lanes to the configured rule count.
     *
     * <p>The logical program is unchanged. This is intended for the configuration-to-runtime
     * boundary so the hot machine does not retain unused rule capacity.
     */
    public void trimToSize() {
        int capacity = Math.max(1, ruleCount);
        if (capacity == flags.length) {
            return;
        }
        int words = Math.multiplyExact(capacity, wordCount);
        allOn = Arrays.copyOf(allOn, words);
        anyOn = Arrays.copyOf(anyOn, words);
        allOff = Arrays.copyOf(allOff, words);
        setMask = Arrays.copyOf(setMask, words);
        clearMask = Arrays.copyOf(clearMask, words);
        scalarIndex = Arrays.copyOf(scalarIndex, capacity);
        scalarMin = Arrays.copyOf(scalarMin, capacity);
        scalarMax = Arrays.copyOf(scalarMax, capacity);
        flags = Arrays.copyOf(flags, capacity);
    }

    void evaluate(long[] state, byte[] scalars, long[] setOut, long[] clearOut) {
        requireWords(state);
        requireWords(setOut);
        requireWords(clearOut);
        if (scalars.length != scalarCount) {
            throw new IllegalArgumentException("scalar-count mismatch");
        }

        for (int rule = 0; rule < ruleCount; rule++) {
            int offset = ruleOffset(rule);
            if (!matchesAllOn(state, offset)
                    || !matchesAllOff(state, offset)
                    || !matchesAnyOn(state, offset, (flags[rule] & HAS_ANY) != 0)
                    || !matchesScalar(scalars, rule)) {
                continue;
            }

            for (int word = 0; word < wordCount; word++) {
                setOut[word] |= setMask[offset + word];
                clearOut[word] |= clearMask[offset + word];
            }
        }
    }

    private int addInternal(
            long[] requireAllOn,
            long[] requireAnyOn,
            long[] requireAllOff,
            int scalar,
            int minInclusive,
            int maxInclusive,
            long[] set,
            long[] clear) {
        requireWords(requireAllOn);
        requireWords(requireAnyOn);
        requireWords(requireAllOff);
        requireWords(set);
        requireWords(clear);
        ensureRuleCapacity(ruleCount + 1);

        int rule = ruleCount++;
        int offset = ruleOffset(rule);
        System.arraycopy(requireAllOn, 0, allOn, offset, wordCount);
        System.arraycopy(requireAnyOn, 0, anyOn, offset, wordCount);
        System.arraycopy(requireAllOff, 0, allOff, offset, wordCount);
        System.arraycopy(set, 0, setMask, offset, wordCount);
        System.arraycopy(clear, 0, clearMask, offset, wordCount);

        byte ruleFlags = 0;
        if (PackedSemanticMasks.any(requireAnyOn)) {
            ruleFlags |= HAS_ANY;
        }
        if (scalar >= 0) {
            ruleFlags |= HAS_SCALAR;
            scalarIndex[rule] = scalar;
            scalarMin[rule] = (byte) minInclusive;
            scalarMax[rule] = (byte) maxInclusive;
        }
        flags[rule] = ruleFlags;
        return rule;
    }

    private boolean matchesAllOn(long[] state, int offset) {
        for (int word = 0; word < wordCount; word++) {
            long required = allOn[offset + word];
            if ((state[word] & required) != required) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesAllOff(long[] state, int offset) {
        for (int word = 0; word < wordCount; word++) {
            if ((state[word] & allOff[offset + word]) != 0L) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesAnyOn(long[] state, int offset, boolean enabled) {
        if (!enabled) {
            return true;
        }
        for (int word = 0; word < wordCount; word++) {
            if ((state[word] & anyOn[offset + word]) != 0L) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesScalar(byte[] scalars, int rule) {
        if ((flags[rule] & HAS_SCALAR) == 0) {
            return true;
        }
        int value = PackedU8.unsigned(scalars[scalarIndex[rule]]);
        return value >= PackedU8.unsigned(scalarMin[rule])
                && value <= PackedU8.unsigned(scalarMax[rule]);
    }

    private void ensureRuleCapacity(int required) {
        if (required <= flags.length) {
            return;
        }
        int capacity = Math.max(required, flags.length + (flags.length >>> 1) + 1);
        int words = Math.multiplyExact(capacity, wordCount);

        allOn = Arrays.copyOf(allOn, words);
        anyOn = Arrays.copyOf(anyOn, words);
        allOff = Arrays.copyOf(allOff, words);
        setMask = Arrays.copyOf(setMask, words);
        clearMask = Arrays.copyOf(clearMask, words);

        scalarIndex = Arrays.copyOf(scalarIndex, capacity);
        scalarMin = Arrays.copyOf(scalarMin, capacity);
        scalarMax = Arrays.copyOf(scalarMax, capacity);
        flags = Arrays.copyOf(flags, capacity);
    }

    private int ruleOffset(int rule) {
        return Math.multiplyExact(rule, wordCount);
    }

    private void requireWords(long[] words) {
        if (words.length != wordCount) {
            throw new IllegalArgumentException(
                    "word-length mismatch: " + words.length + " != " + wordCount);
        }
    }

    private void checkScalar(int scalar) {
        if (scalar < 0 || scalar >= scalarCount) {
            throw new IndexOutOfBoundsException(
                    "scalar=" + scalar + ", scalarCount=" + scalarCount);
        }
    }
}
