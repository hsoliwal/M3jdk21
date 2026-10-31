// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.internal.mindex;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * RX7: immutable bounded ASCII DFA execution atom derived from a separately verified
 * Synexia MIndexRegexProgram v7 image. This is NOT a parser, image decoder, JDK
 * Matcher replacement, or an authority to decide whether an arbitrary Pattern matches.
 *
 * <p>The caller must prove correspondence between the supplied DFA and its exact
 * upstream v7 pattern/flags/source root before using this candidate. For now,
 * matchesVerified delegates final truth to the authoritative JDK Pattern.
 * All arrays are defensive snapshots; no String/M3String spelling is retained.
 */
public final class M3RX7 {
    private static final int ASCII = 128;
    private static final int MAX_STATES = 4096;
    private static final int MAX_TRANSITION_BYTES = 2 * 1024 * 1024;
    private static final int MAX_TEXT_UNITS = 1_000_000;

    private final String programRoot;
    private final int start;
    private final short[] next;
    private final byte[] accepting;

    private M3RX7(String programRoot, int start, short[] next, byte[] accepting) {
        this.programRoot = programRoot;
        this.start = start;
        this.next = next;
        this.accepting = accepting;
    }

    /**
     * Adapt the exact primitive state lanes of an already-qualified v7
     * MIndexRegexProgram.AsciiDfaImage. The root is the 64-hex-character
     * program-image identity, not an identity of String text or owner ID.
     */
    public static M3RX7 fromAsciiDfa(
            String programRoot, int startState, short[] transitions, byte[] accepting) {
        Objects.requireNonNull(programRoot, "programRoot");
        Objects.requireNonNull(transitions, "transitions");
        Objects.requireNonNull(accepting, "accepting");
        if (!programRoot.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("invalid v7 source-image root");
        }
        int states = accepting.length;
        if (states < 1 || states > MAX_STATES || startState < 0 || startState >= states
                || (long) states * ASCII * Short.BYTES > MAX_TRANSITION_BYTES
                || transitions.length != (long) states * ASCII) {
            throw new IllegalArgumentException("invalid bounded ASCII DFA geometry");
        }
        for (byte flag : accepting) {
            if (flag != 0 && flag != 1) {
                throw new IllegalArgumentException("invalid accepting-state lane");
            }
        }
        for (short state : transitions) {
            if (state < -1 || state >= states) {
                throw new IllegalArgumentException("invalid DFA transition target");
            }
        }
        return new M3RX7(programRoot, startState, transitions.clone(), accepting.clone());
    }

    public String programRoot() { return programRoot; }
    public int stateCount() { return accepting.length; }
    public long primitivePayloadBytes() {
        return (long) next.length * Short.BYTES + accepting.length;
    }

    /**
     * Capture-free full-region ASCII Boolean candidate only: -1 means unsupported,
     * 0 means false and 1 means true. Reads immutable Java String UTF-16 units
     * directly, including M3-backed concatenation/slices; never flattens text.
     * The result is not JDK regex truth until independently verified.
     */
    public int matchesCandidate(String input) {
        Objects.requireNonNull(input, "input");
        int length = input.length();
        if (length > MAX_TEXT_UNITS) return -1;
        int state = start;
        for (int index = 0; index < length; index++) {
            char unit = input.charAt(index);
            if (unit >= ASCII) return -1;
            if (state >= 0) state = next[state * ASCII + unit];
        }
        return state >= 0 && accepting[state] != 0 ? 1 : 0;
    }

    /**
     * Authoritative compatibility oracle, deliberately not an optimization. Refuses
     * a disagreed candidate instead of suppressing a legitimate JDK match.
     */
    public boolean matchesVerified(String input, Pattern authoritativePattern) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(authoritativePattern, "authoritativePattern");
        boolean truth = authoritativePattern.matcher(input).matches();
        int candidate = matchesCandidate(input);
        if (candidate >= 0 && (candidate == 1) != truth) {
            throw new IllegalStateException("M3RX7 DFA disagrees with JDK Pattern");
        }
        return truth;
    }
}
