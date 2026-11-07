// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Random;

/**
 * Differential dump and reference model (gates 3 and 4 of recipe/lint-storage-20261107/differential.sh). Replays one
 * seeded corpus of long values through the width enum named by args[0] and the 48-bit arithmetic class named by
 * args[1], both reached by reflection so that the donor classes and the relocated classes run the same driver. The
 * corpus is 0, 1, -1, Long.MIN_VALUE, Long.MAX_VALUE and every 2^k - 1, 2^k, 2^k + 1 and their negations for k in
 * 0..62 (383 edge values, each paired with every edge partner 1, -1, 3 and -2^24: 1,532 lines), then CASES values drawn
 * by {@link M3LintStorageReference#randomValue} at a seeded bit length 1..64 and seeded signedness, each with a seeded
 * partner operand (48-bit lint, 16-bit lint or any long).
 *
 * <p>The header prints every width constant (name, bits, bytes, mask, signed and unsigned bounds) and the lint
 * constants. Each value prints one line: its bits and its partner's, the fits bitmask over the five widths x
 * {signed, unsigned}, a 64-bit digest of the ten require outcomes, smallest(value, signed) for both choices, and the
 * check, checkUnsigned, negateExact, absExact, wrap, saturate, addExact, subtractExact, multiplyExact and divideExact
 * results (a number, or the exception class and message). Every result is checked against the BigInteger model of
 * {@link M3LintStorageReference}; a drift aborts the run. The last line totals values, reference comparisons,
 * exceptions and a digest of every line. Two sides agree when their dumps are byte-identical.
 * Usage: {@code java -cp classes;test-classes jdk.m3.arrays.M3LintStorageDump WIDTH_FQN LINT_FQN [CASES]}
 */
//FLLDCIM: TFG - dump driver from the template M3IntRingBufferDump (recipe int-ring-buffer-20261106): reflective, serves donor and relocated classes
public final class M3LintStorageDump {
    private static final long SEED = 0x4C49_4E54_5749_4454L;
    private static final int DEFAULT_CASES = 20_000;
    private static final int SMALL_BITS = 16;
    private static final int PARTNER_KINDS = 8;
    private static final long FNV_PRIME = 0x100000001b3L;
    private static final long FNV_BASIS = 0xcbf29ce484222325L;
    private static final String ARITHMETIC = ArithmeticException.class.getName() + ": ";
    private static final boolean[] SIGNS = {true, false};
    private static final long[] FIXED = {0L, 1L, -1L, Long.MIN_VALUE, Long.MAX_VALUE};
    private static final long[] EDGE_PARTNERS = {1L, -1L, 3L, -(1L << 24)};
    private static final String[] UNARY = {"negateExact", "absExact"};
    private static final String[] BINARY = {"addExact", "subtractExact", "multiplyExact", "divideExact"};

    private final Object[] widths;
    private final Method bits;
    private final Method bytes;
    private final Method mask;
    private final Method minValue;
    private final Method maxValue;
    private final Method fits;
    private final Method require;
    private final Method smallest;
    private final Class<?> lint;
    private final PrintStream out;
    private long references;
    private long exceptions;
    private long digest = FNV_BASIS;

    private M3LintStorageDump(Class<?> width, Class<?> lint, PrintStream out) throws ReflectiveOperationException {
        this.widths = width.getEnumConstants();
        this.bits = width.getMethod("bits");
        this.bytes = width.getMethod("bytes");
        this.mask = width.getMethod("mask");
        this.minValue = width.getMethod("minValue", boolean.class);
        this.maxValue = width.getMethod("maxValue", boolean.class);
        this.fits = width.getMethod("fits", long.class, boolean.class);
        this.require = width.getMethod("require", long.class, boolean.class);
        this.smallest = width.getMethod("smallest", long.class, boolean.class);
        this.lint = lint;
        this.out = out;
    }

    public static void main(String[] args) throws Exception {
        PrintStream out = new PrintStream(new BufferedOutputStream(System.out, 1 << 16), false, StandardCharsets.UTF_8);
        run(Class.forName(args[0]), Class.forName(args[1]), args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_CASES,
                out);
        out.flush();
    }

    /** Runs the corpus; returns {values, reference comparisons, exceptions}. */
    static long[] run(Class<?> width, Class<?> lint, int cases, PrintStream out) throws ReflectiveOperationException {
        M3LintStorageDump dump = new M3LintStorageDump(width, lint, out);
        dump.header();
        long values = dump.edges();
        Random random = new Random(SEED);
        for (int i = 0; i < cases; i++, values++) {
            long value = M3LintStorageReference.randomValue(random, 1 + random.nextInt(Long.SIZE), random.nextBoolean());
            dump.value(value, partner(random));
        }
        dump.out.println("TOTAL values=" + values + " references=" + dump.references + " exceptions=" + dump.exceptions
                + " digest=" + Long.toHexString(dump.digest));
        return new long[] {values, dump.references, dump.exceptions};
    }

    static PrintStream discard() {
        return new PrintStream(OutputStream.nullOutputStream(), false, StandardCharsets.UTF_8);
    }

    private static long partner(Random random) {
        int kind = random.nextInt(PARTNER_KINDS);
        int partnerBits = kind == 0 ? Long.SIZE : kind < 3 ? SMALL_BITS : M3LintStorageReference.LINT_BITS;
        return M3LintStorageReference.randomValue(random, partnerBits, true);
    }

    private long edges() throws ReflectiveOperationException {
        long count = edge(FIXED);
        for (int k = 0; k < Long.SIZE - 1; k++) {
            long power = 1L << k;
            count += edge(new long[] {power - 1, power, power + 1, -power - 1, -power, -power + 1});
        }
        return count;
    }

    /** Every edge value meets every edge partner (so the lint minimum meets -1 in divideExact and multiplyExact). */
    private long edge(long[] values) throws ReflectiveOperationException {
        for (long value : values) {
            for (long partner : EDGE_PARTNERS) {
                value(value, partner);
            }
        }
        return (long) values.length * EDGE_PARTNERS.length;
    }

    private void header() throws ReflectiveOperationException {
        expect(widths.length == M3LintStorageReference.WIDTH_BITS.length, "width count " + widths.length);
        for (int i = 0; i < widths.length; i++) {
            widthHeader(widths[i], M3LintStorageReference.WIDTH_BITS[i]);
        }
        lintConstants();
    }

    private void widthHeader(Object constant, int width) throws ReflectiveOperationException {
        expect((int) bits.invoke(constant) == width && (int) bytes.invoke(constant) == width / Byte.SIZE, "bits " + width);
        expect((long) mask.invoke(constant) == M3LintStorageReference.mask(width), "mask " + width);
        for (boolean signed : SIGNS) {
            expect((long) minValue.invoke(constant, signed) == M3LintStorageReference.min(width, signed).longValue()
                    && (long) maxValue.invoke(constant, signed) == M3LintStorageReference.max(width, signed).longValue(),
                    "bounds " + width + " " + signed);
        }
        emit("WIDTH " + constant + " bits=" + width + " bytes=" + bytes.invoke(constant) + " mask=" + hex(mask.invoke(constant))
                + " signed=[" + minValue.invoke(constant, true) + "," + maxValue.invoke(constant, true) + "] unsigned=["
                + minValue.invoke(constant, false) + "," + hex(maxValue.invoke(constant, false)) + "]");
    }

    private void lintConstants() throws ReflectiveOperationException {
        String[] names = {"BITS", "BYTES", "MIN_VALUE", "MAX_VALUE", "UNSIGNED_MAX_VALUE"};
        BigInteger[] expected = {BigInteger.valueOf(M3LintStorageReference.LINT_BITS),
                BigInteger.valueOf(M3LintStorageReference.LINT_BITS / Byte.SIZE),
                M3LintStorageReference.min(M3LintStorageReference.LINT_BITS, true),
                M3LintStorageReference.max(M3LintStorageReference.LINT_BITS, true),
                M3LintStorageReference.max(M3LintStorageReference.LINT_BITS, false)};
        StringBuilder line = new StringBuilder("LINT");
        for (int i = 0; i < names.length; i++) {
            Object value = lint.getField(names[i]).get(null);
            expect(value.toString().equals(expected[i].toString()), "lint constant " + names[i]);
            line.append(' ').append(names[i]).append('=').append(value);
        }
        emit(line.toString());
    }

    private void value(long value, long partner) throws ReflectiveOperationException {
        StringBuilder line = new StringBuilder("V ").append(Long.toHexString(value)).append(' ')
                .append(Long.toHexString(partner));
        widthFacts(value, line);
        lintFacts(value, partner, line);
        emit(line.toString());
    }

    private void widthFacts(long value, StringBuilder line) throws ReflectiveOperationException {
        int fitMask = 0;
        long requireDigest = FNV_BASIS;
        for (int i = 0; i < widths.length; i++) {
            for (boolean signed : SIGNS) {
                boolean model = M3LintStorageReference.fits(M3LintStorageReference.WIDTH_BITS[i], value, signed);
                expect(outcome(fits, widths[i], value, signed).equals(Boolean.toString(model)), "fits " + i + " " + value);
                String required = outcome(require, widths[i], value, signed);
                expect(model ? required.equals(Long.toString(value)) : required.startsWith(ARITHMETIC), "require " + value);
                fitMask = fitMask << 1 | (model ? 1 : 0);
                requireDigest = fnv(requireDigest, required);
            }
        }
        line.append(" fits=").append(Integer.toHexString(fitMask)).append(" req=").append(Long.toHexString(requireDigest));
        smallestFacts(value, line);
    }

    private void smallestFacts(long value, StringBuilder line) throws ReflectiveOperationException {
        for (boolean signed : SIGNS) {
            String narrowest = outcome(smallest, null, value, signed);
            expect(narrowest.equals("INT" + M3LintStorageReference.smallestBits(value, signed)), "smallest " + value);
            line.append(signed ? " small=" : "/").append(narrowest);
        }
    }

    private void lintFacts(long value, long partner, StringBuilder line) throws ReflectiveOperationException {
        lint(line, "check", M3LintStorageReference.lintCheck(value, true), value);
        lint(line, "checkUnsigned", M3LintStorageReference.lintCheck(value, false), value);
        for (String name : UNARY) {
            lint(line, name, M3LintStorageReference.lintUnary(name, value), value);
        }
        lint(line, "wrap", Optional.of(M3LintStorageReference.wrap(value)), value);
        lint(line, "saturate", Optional.of(M3LintStorageReference.saturate(value)), value);
        for (String name : BINARY) {
            String actual = outcome(lint.getMethod(name, long.class, long.class), null, value, partner);
            compare(actual, M3LintStorageReference.lintBinary(name, value, partner), name + " " + value + " " + partner);
            line.append(' ').append(name, 0, 3).append('=').append(actual);
        }
    }

    private void lint(StringBuilder line, String name, Optional<BigInteger> expected, long value)
            throws ReflectiveOperationException {
        String actual = outcome(lint.getMethod(name, long.class), null, value);
        compare(actual, expected, name + " " + value);
        line.append(' ').append(name).append('=').append(actual);
    }

    private void compare(String actual, Optional<BigInteger> expected, String what) {
        expect(expected.map(value -> actual.equals(value.toString())).orElseGet(() -> actual.startsWith(ARITHMETIC)),
                what + " -> " + actual);
    }

    private String outcome(Method method, Object target, Object... args) throws IllegalAccessException {
        try {
            return String.valueOf(method.invoke(target, args));
        } catch (InvocationTargetException failure) {
            exceptions++;
            Throwable cause = failure.getCause();
            return cause.getClass().getName() + ": " + cause.getMessage();
        }
    }

    private void expect(boolean holds, String what) {
        references++;
        if (!holds) {
            throw new AssertionError("reference drift: " + what);
        }
    }

    private void emit(String line) {
        out.println(line);
        digest = fnv(fnv(digest, line), "\n");
    }

    private static long fnv(long hash, String text) {
        long h = hash;
        for (int i = 0; i < text.length(); i++) {
            h = (h ^ text.charAt(i)) * FNV_PRIME;
        }
        return h;
    }

    private static String hex(Object value) {
        return Long.toHexString((long) value);
    }
}
