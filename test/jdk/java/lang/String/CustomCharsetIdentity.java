/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

/*
 * @test
 * @summary String encoding must use a custom Charset's encoder even when its
 *          canonical name equals a standard charset's name, and preserve
 *          replacement-action callback behavior for empty and strict encoding
 * @modules java.base/java.lang:+open
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage CustomCharsetIdentity
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings CustomCharsetIdentity
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnmappableCharacterException;
import java.util.Arrays;

public class CustomCharsetIdentity {
    private static final Charset[] STANDARD = {
        StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1, StandardCharsets.US_ASCII
    };
    private static final byte EMITTED = '!';
    private static final byte REPLACEMENT = '~';
    private static final Method NO_REPLACEMENT;
    private static long checks;

    static {
        try {
            NO_REPLACEMENT = String.class.getDeclaredMethod(
                    "getBytesNoRepl", String.class, Charset.class);
            NO_REPLACEMENT.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        // The explicit stock mode checks the independent Java 21 oracle only.
        // The jtreg runs above always require and assert canonical M3 storage.
        boolean stock = args.length == 1 && args[0].equals("stock");
        if (args.length != 0 && !stock) throw new IllegalArgumentException("unexpected arguments");
        Access access = stock ? null : new Access();
        char[] ascii = new char[97];
        Arrays.fill(ascii, 'a');
        ascii[31] = 'b';
        ascii[65] = 'c';
        char[] mixed = ascii.clone();
        mixed[0] = '\u0000';
        mixed[16] = '\u00ff';
        mixed[30] = '\u0100';
        mixed[47] = '\ud83d';
        mixed[48] = '\ude00'; // The tuple's central seam bisects this pair.
        mixed[80] = '\ud800'; // Custom encoding may accept unpaired surrogates.
        char[] failing = ascii.clone();
        failing[47] = 'x';

        for (Charset standard : STANDARD) {
            for (char[] units : new char[][] {new char[0], ascii, mixed, failing}) {
                int shapes = stock ? 1 : units.length == 0 ? 2 : 4;
                for (int shape = 0; shape < shapes; shape++) {
                    String source = stock ? new String(units) : access.make(units, shape);
                    Object storage = access == null ? null : access.storage.get(source);
                    Object shadow = access == null ? null : access.value.get(source);
                    for (Failure mode : Failure.values()) {
                        checkCustom(source, units, standard, mode);
                        if (standard == StandardCharsets.UTF_8) {
                            checkCustom(source, units, null, mode);
                        }
                    }
                    ByteBuffer expected = standard.newEncoder()
                            .onMalformedInput(CodingErrorAction.REPLACE)
                            .onUnmappableCharacter(CodingErrorAction.REPLACE)
                            .encode(CharBuffer.wrap(units));
                    byte[] encoded = new byte[expected.remaining()];
                    expected.get(encoded);
                    check(Arrays.equals(source.getBytes(standard), encoded), "standard charset bytes");
                    if (access != null) {
                        check(access.storage.get(source) == storage, "encoding changed canonical storage");
                        check(access.value.get(source) == shadow, "encoding retained a new payload shadow");
                    }
                }
            }
        }
        System.out.println("CUSTOM_CHARSET_IDENTITY_PASS checks=" + checks
                + " canonical_runtime=" + !stock);
    }

    private static void checkCustom(String source, char[] units, Charset standard,
                                    Failure mode) throws Exception {
        String name = standard == null ? "X-Custom-Charset-Identity" : standard.name();
        CustomCharset charset = new CustomCharset(name, mode);
        if (standard == null) {
            for (Charset builtIn : STANDARD) {
                check(!charset.equals(builtIn), "unique fixture must not equal a standard charset");
            }
        } else {
            check(charset.equals(standard), "custom fixture must have the same canonical name");
            check(charset != standard, "custom fixture must have distinct identity");
        }
        byte[] expected = new byte[units.length];
        Arrays.fill(expected, EMITTED);
        boolean error = false;
        for (int index = 0; index < units.length; index++) {
            if (units[index] == 'x' && mode != Failure.NONE) {
                expected[index] = REPLACEMENT;
                error = true;
            }
        }
        charset.rejectActionHooks = units.length == 0;
        byte[] actual = source.getBytes(charset);
        check(Arrays.equals(actual, expected), "custom replacement bytes: " + name + " " + mode);
        check(charset.encoderCalls == 1, "getBytes must request the custom encoder exactly once");
        int expectedActions = units.length == 0 ? 0 : 1;
        String expectedOrder = units.length == 0 ? "" : "MU";
        check(charset.malformedActions == expectedActions, "replacement malformed-action count");
        check(charset.unmappableActions == expectedActions, "replacement unmappable-action count");
        check(charset.actionOrder.toString().equals(expectedOrder), "replacement action order");
        // Strict encoding must not configure either replacement action, even for nonempty input.
        charset.rejectActionHooks = true;
        try {
            byte[] strict = strict(source, charset);
            check(!error, "strict encoding must reject the custom encoder's reported error");
            check(Arrays.equals(strict, expected), "custom strict bytes: " + name);
        } catch (CharacterCodingException failure) {
            check(error, "unexpected strict encoding failure");
            if (mode == Failure.UNMAPPABLE) {
                check(failure instanceof UnmappableCharacterException, "strict unmappable type");
                check(((UnmappableCharacterException) failure).getInputLength() == 1,
                        "strict unmappable input length");
            } else {
                check(failure instanceof MalformedInputException, "strict malformed type");
                check(((MalformedInputException) failure).getInputLength() == 1,
                        "strict malformed input length");
            }
        }
        check(charset.encoderCalls == 2, "strict encoding must request the custom encoder exactly once");
        check(charset.malformedActions == expectedActions, "strict malformed-action count");
        check(charset.unmappableActions == expectedActions, "strict unmappable-action count");
        check(charset.actionOrder.toString().equals(expectedOrder), "strict encoding changed action order");
    }

    private static byte[] strict(String source, Charset charset) throws Exception {
        try {
            return (byte[]) NO_REPLACEMENT.invoke(null, source, charset);
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof Exception exception) throw exception;
            if (cause instanceof Error error) throw error;
            throw new AssertionError(cause);
        }
    }

    private enum Failure { NONE, UNMAPPABLE, MALFORMED }

    private static final class CustomCharset extends Charset {
        private final Failure failure;
        int encoderCalls;
        int malformedActions;
        int unmappableActions;
        boolean rejectActionHooks;
        final StringBuilder actionOrder = new StringBuilder();

        CustomCharset(String name, Failure failure) {
            super(name, new String[0]);
            this.failure = failure;
        }

        @Override
        public boolean contains(Charset other) {
            return other == this;
        }

        @Override
        public CharsetDecoder newDecoder() {
            return StandardCharsets.ISO_8859_1.newDecoder();
        }

        @Override
        public CharsetEncoder newEncoder() {
            encoderCalls++;
            return new CharsetEncoder(this, 1, 1, new byte[] {REPLACEMENT}) {
                // The superclass constructor may invoke overrideable hooks. Only calls made
                // after construction belong to the String encoding operation under test.
                private boolean active = true;

                @Override
                protected void implOnMalformedInput(CodingErrorAction action) {
                    if (!active) return;
                    if (rejectActionHooks) {
                        throw new AssertionError("unexpected malformed-action hook for empty or strict encoding");
                    }
                    check(action == CodingErrorAction.REPLACE, "malformed-action value");
                    malformedActions++;
                    actionOrder.append('M');
                }

                @Override
                protected void implOnUnmappableCharacter(CodingErrorAction action) {
                    if (!active) return;
                    if (rejectActionHooks) {
                        throw new AssertionError("unexpected unmappable-action hook for empty or strict encoding");
                    }
                    check(action == CodingErrorAction.REPLACE, "unmappable-action value");
                    unmappableActions++;
                    actionOrder.append('U');
                }

                @Override
                protected CoderResult encodeLoop(CharBuffer input, ByteBuffer output) {
                    while (input.hasRemaining()) {
                        if (input.get(input.position()) == 'x') {
                            if (failure == Failure.UNMAPPABLE) return CoderResult.unmappableForLength(1);
                            if (failure == Failure.MALFORMED) return CoderResult.malformedForLength(1);
                        }
                        if (!output.hasRemaining()) return CoderResult.OVERFLOW;
                        input.get();
                        output.put(EMITTED);
                    }
                    return CoderResult.UNDERFLOW;
                }
            };
        }
    }

    private static final class Access {
        final Field storage;
        final Field value;
        final Field owner;
        final Method intern;
        final Method concat;
        final Method slice;
        final Constructor<String> constructor;
        final ThreadLocal<Boolean> admitting;

        @SuppressWarnings("unchecked")
        Access() throws ReflectiveOperationException {
            Class<?> type = Class.forName("java.lang.M3String");
            Class<?> pool = Class.forName("java.lang.M3StringPool");
            storage = accessible(String.class.getDeclaredField("m3"));
            value = accessible(String.class.getDeclaredField("value"));
            owner = accessible(type.getDeclaredField("owner"));
            intern = accessible(pool.getDeclaredMethod("internChars", char[].class, int.class, int.class));
            concat = accessible(type.getDeclaredMethod("concat", type));
            slice = accessible(type.getDeclaredMethod("slice", int.class, int.class));
            constructor = String.class.getDeclaredConstructor(type);
            constructor.setAccessible(true);
            admitting = (ThreadLocal<Boolean>) accessible(type.getDeclaredField("ADMITTING")).get(null);
        }

        String make(char[] units, int shape) throws Exception {
            if (shape == 0) {
                Boolean previous = admitting.get();
                admitting.set(Boolean.TRUE);
                String flat;
                try {
                    flat = new String(units);
                } finally {
                    if (previous == null) admitting.remove();
                    else admitting.set(previous);
                }
                check(storage.get(flat) == null, "flat fixture entered canonical storage");
                return flat;
            }
            Object canonical;
            if (shape == 1) {
                canonical = atom(units, 0, units.length);
            } else if (shape == 2) {
                canonical = tuple(units);
            } else {
                char[] padded = new char[units.length + 8];
                Arrays.fill(padded, '\udc00');
                System.arraycopy(units, 0, padded, 4, units.length);
                canonical = slice.invoke(tuple(padded), 4, 4 + units.length);
            }
            check(canonical != null, "canonical fixture was refused");
            String source = constructor.newInstance(canonical);
            check(storage.get(source) == canonical, "canonical encoding route is not active");
            if (units.length != 0) {
                check(owner.get(canonical).getClass().getSimpleName().equals(
                        shape == 1 ? "M3StringAtom" : "M3StringTuple"), "unexpected fixture owner");
            }
            return source;
        }

        private Object atom(char[] units, int start, int length) throws Exception {
            Object result = intern.invoke(null, units, start, length);
            check(result != null, "canonical atom was refused");
            return result;
        }

        private Object tuple(char[] units) throws Exception {
            int middle = units.length / 2;
            return concat.invoke(atom(units, 0, middle),
                    atom(units, middle, units.length - middle));
        }
    }

    private static Field accessible(Field field) {
        field.setAccessible(true);
        return field;
    }

    private static Method accessible(Method method) {
        method.setAccessible(true);
        return method;
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
