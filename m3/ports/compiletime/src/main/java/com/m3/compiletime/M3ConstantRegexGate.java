// SPDX-License-Identifier: Apache-2.0
package com.m3.compiletime;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Runtime consumer of the constant-regex image for one compiled {@link Pattern}.
 *
 * <p>The image is looked up once, by content key (pattern UTF-16 + flags), when the gate is
 * created. On a {@code String} input a {@code REQUIRED} entry evaluates its literal formula with
 * {@link String#indexOf(String)}; a false formula proves that no match exists anywhere in the
 * input, so the stock engine is skipped and the stock "no match" result is returned. A
 * {@code PURE_LITERAL} entry is complete: {@code find} is {@code indexOf >= 0} and
 * {@code matches} is {@code equals}. Every other case (miss, refused/unconstrained entry,
 * non-String input, satisfied formula) runs the stock {@link java.util.regex.Matcher}, which
 * remains the oracle. Enabled only when {@value #IMAGE_PROPERTY} names an image that verifies.</p>
 */
public final class M3ConstantRegexGate {
    /** System property naming the image; absent or refused means a pure passthrough gate. */
    public static final String IMAGE_PROPERTY = "m3.precompute.constantRegex.image";

    private static final int PASS = 0;
    private static final int PURE = 1;
    private static final int GATED = 2;

    /*
     * The same build-time image gate may be held in a static field and called
     * from many threads. No execution stack is ever stored in the gate.
     * Typical postfix formulas execute entirely in one invocation-local long.
     * Exceptionally wide formulas borrow thread-confined primitive scratch.
     */
    private static final int PACKED_DEPTH = Long.SIZE;
    private static final int MAX_RETAINED_THREAD_DEPTH = 4_096;
    private static final ThreadLocal<boolean[]> THREAD_SCRATCH =
            ThreadLocal.withInitial(() -> new boolean[0]);

    private final Pattern pattern;
    private final int mode;
    private final String literal;
    private final String[] atoms;
    private final int[] program;
    private final int stackDepth;
    private final boolean packed;

    private M3ConstantRegexGate(Pattern pattern, Optional<M3ConstantRegexImage.Entry> entry) {
        this.pattern = pattern;
        M3RegexRequirement.Kind kind = entry.map(M3ConstantRegexImage.Entry::kind).orElse(null);
        this.mode = kind == M3RegexRequirement.Kind.PURE_LITERAL ? PURE
                : kind == M3RegexRequirement.Kind.REQUIRED ? GATED : PASS;
        this.atoms = mode == PASS ? new String[0] : entry.get().atoms();
        this.program = mode == PASS ? new int[0] : entry.get().program();
        this.literal = mode == PURE ? atoms[0] : null;
        // Fold the *shape* of the already-compiled postfix plan just once,
        // not a regex parser or per-match stack plan. Refuse corrupt images.
        int depth = 0;
        int maxDepth = 0;
        for (int operation : program) {
            if (operation >>> 24 == M3ConstantRegexImage.OP_ATOM) {
                depth++;
            } else {
                int consumed = operation & 0xffffff;
                if (consumed > depth) {
                    throw new IllegalArgumentException("invalid regex precompute stack");
                }
                depth = depth - consumed + 1;
            }
            if (depth > MAX_RETAINED_THREAD_DEPTH * 4) {
                throw new IllegalArgumentException("regex precompute stack too deep");
            }
            maxDepth = Math.max(maxDepth, depth);
        }
        if (program.length != 0 && depth != 1) {
            throw new IllegalArgumentException("incomplete regex precompute stack");
        }
        stackDepth = maxDepth;
        packed = maxDepth <= PACKED_DEPTH;
    }

    /**
     * Compiles {@code regex} with {@code flags} (stock {@link Pattern#compile(String, int)}, same
     * exceptions) and attaches the image entry keyed by exactly that (regex, flags) pair.
     */
    public static M3ConstantRegexGate compile(String regex, int flags, M3ConstantRegexImage image) {
        return of(Pattern.compile(regex, flags), flags, image);
    }

    /**
     * Gate for {@code pattern}, which the caller compiled with {@code compileFlags}. The key is the
     * compile argument, never {@link Pattern#flags()}: embedded flags such as {@code (?m)} change
     * {@code flags()} without changing the source key. A null image yields a passthrough gate.
     */
    public static M3ConstantRegexGate of(Pattern pattern, int compileFlags, M3ConstantRegexImage image) {
        return new M3ConstantRegexGate(pattern,
                image == null ? Optional.empty() : image.lookup(pattern.pattern(), compileFlags));
    }

    /** The image named by {@value #IMAGE_PROPERTY}, or empty when unset or refused. */
    public static Optional<M3ConstantRegexImage> configuredImage() {
        String path = System.getProperty(IMAGE_PROPERTY);
        if (path == null || path.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(M3ConstantRegexImage.open(Path.of(path)));
        } catch (IOException | RuntimeException refused) {
            return Optional.empty();
        }
    }

    public Pattern pattern() {
        return pattern;
    }

    /** True when the image supplied a usable entry (pure literal or literal formula). */
    public boolean hit() {
        return mode != PASS;
    }

    /** Same result as {@code pattern.matcher(input).find()}. */
    public boolean find(CharSequence input) {
        if (input instanceof String s) {
            if (mode == PURE) {
                return s.indexOf(literal) >= 0;
            }
            if (mode == GATED && !holds(s)) {
                return false;
            }
        }
        return pattern.matcher(input).find();
    }

    /** Same result as {@code pattern.matcher(input).matches()}. */
    public boolean matches(CharSequence input) {
        if (input instanceof String s) {
            if (mode == PURE) {
                return s.equals(literal);
            }
            if (mode == GATED && !holds(s)) {
                return false;
            }
        }
        return pattern.matcher(input).matches();
    }

    /** Same result as {@code pattern.matcher(input).replaceAll(replacement)}. */
    public String replaceAll(String input, String replacement) {
        if (mode != PASS && !holds(input)) {
            return input;
        }
        return pattern.matcher(input).replaceAll(replacement);
    }

    /** Same result as {@code pattern.split(input)}. */
    public String[] split(String input) {
        if (mode != PASS && !holds(input)) {
            return new String[] {input};
        }
        return pattern.split(input);
    }

    /**
     * True when the build-time necessary condition holds; false proves absence.
     * All mutable evaluation state is invocation-local or thread-exclusive.
     * Never trust a positive candidate as a complete JDK Pattern result.
     */
    public boolean holds(String s) {
        return packed ? holdsPacked(s) : holdsWide(s);
    }

    /**
     * Evaluate arbitrary-length postfix formulas as a scalar Boolean stack,
     * provided the precomputed *maximum stack depth* fits in 64 bits.
     * AND/OR reductions use one mask/shift instead of an inner Boolean loop.
     * There are no arrays, shared writes, JNI crossings or retained inputs.
     */
    private boolean holdsPacked(String text) {
        long bits = 0L;
        int top = 0;
        for (int op : program) {
            int code = op >>> 24;
            int arg = op & 0xffffff;
            if (code == M3ConstantRegexImage.OP_ATOM) {
                long bit = 1L << top++;
                if (text.indexOf(atoms[arg]) >= 0) bits |= bit;
                else bits &= ~bit;
                continue;
            }
            long mask = arg == Long.SIZE ? -1L : (1L << arg) - 1;
            long selected = (bits >>> (top - arg)) & mask;
            boolean matched = code == M3ConstantRegexImage.OP_AND
                    ? selected == mask : selected != 0L;
            top -= arg;
            long target = 1L << top++;
            if (matched) bits |= target;
            else bits &= ~target;
        }
        return top == 0 || (bits & 1L) != 0;
    }

    /** A single bounded scratch lane per thread, shared across all regex gates. */
    private static boolean[] threadScratch(int required) {
        if (required > MAX_RETAINED_THREAD_DEPTH) {
            // Do not retain pathological arrays on long-lived pooled threads.
            return new boolean[required];
        }
        boolean[] present = THREAD_SCRATCH.get();
        if (present.length < required) {
            present = new boolean[required];
            THREAD_SCRATCH.set(present);
        }
        return present;
    }

    /** Wide formulas retain no mutable state in a globally reusable gate. */
    private boolean holdsWide(String text) {
        int top = 0;
        boolean[] st = threadScratch(stackDepth);
        for (int op : program) {
            int code = op >>> 24;
            int arg = op & 0xffffff;
            if (code == M3ConstantRegexImage.OP_ATOM) {
                st[top++] = text.indexOf(atoms[arg]) >= 0;
                continue;
            }
            boolean and = code == M3ConstantRegexImage.OP_AND;
            boolean value = and;
            for (int i = top - arg; i < top; i++) {
                value = and ? value && st[i] : value || st[i];
            }
            top -= arg;
            st[top++] = value;
        }
        return top == 0 || st[0];
    }
}
