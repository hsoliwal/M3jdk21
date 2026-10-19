// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

/** Executable, dependency-free differential proof of the actual TextMetadata owner. */
public final class TextMetadataProof {
    private static final char[] ALPHABET = {'\0', 'A', 'e', '\ud83d', '\ude00'};
    private long checks;
    private long layouts;
    private boolean nativeEnabled;

    public static void main(String[] args) {
        TextMetadataProof proof = new TextMetadataProof();
        if (args.length == 1) {
            System.load(Path.of(args[0]).toAbsolutePath().toString());
            proof.nativeEnabled = true;
        } else if (args.length != 0) {
            throw new IllegalArgumentException("zero arguments or absolute proof library path");
        }
        proof.execute();
        System.out.println("TEXT_METADATA_PASS native=" + proof.nativeEnabled
                + " layouts=" + proof.layouts + " checks=" + proof.checks);
    }

    private void execute() {
        for (int width = 0; width <= 5; width++) {
            int count = (int) Math.pow(ALPHABET.length, width);
            for (int code = 0; code < count; code++) {
                char[] units = new char[width];
                int rest = code;
                for (int i = 0; i < width; i++) {
                    units[i] = ALPHABET[rest % ALPHABET.length];
                    rest /= ALPHABET.length;
                }
                verify(new String(units));
            }
        }
        Random random = new Random(0x4d335554463136L);
        for (int i = 0; i < 400; i++) {
            char[] units = new char[32 + random.nextInt(128)];
            for (int j = 0; j < units.length; j++) units[j] = (char) random.nextInt(65536);
            String text = new String(units);
            int cut = random.nextInt(text.length() + 1);
            check(oracle(text), join(summary(text, 0, cut), TextMetadata.EMPTY,
                    summary(text, cut, text.length())));
        }
        // Removing a region creates new adjacency; an annotation-only mask would not.
        String source = "\ud83dignored\ude00";
        check(oracle("\ud83d\ude00"), join(summary(source, 0, 1), summary(source, 8, 9)));
        check(oracle("customer=**"), join(oracle("cust"), oracle("omer="), oracle("**")));
        // A hash/endpoints/count tuple is NOT exact content identity.
        check(oracle("xAaY"), oracle("xBBY"));
        check(false, "xAaY".equals("xBBY"));
        failures();
    }

    private void verify(String text) {
        check(oracle(text), summary(text, 0, text.length()));
        for (int left = 0; left <= text.length(); left++) {
            for (int right = left; right <= text.length(); right++) {
                layouts++;
                TextMetadata.Summary a = summary(text, 0, left);
                TextMetadata.Summary b = summary(text, left, right);
                TextMetadata.Summary c = summary(text, right, text.length());
                check(oracle(text.substring(left, right)), b);
                check(oracle(text), TextMetadata.concat(TextMetadata.concat(a, b), c));
                check(oracle(text), TextMetadata.concat(a, TextMetadata.concat(b, c)));
                check(oracle(text), join(a, TextMetadata.EMPTY, b, TextMetadata.EMPTY, c));
            }
        }
    }

    private TextMetadata.Summary summary(String text, int start, int end) {
        // toString/subSequence are hostile to detect accidental flattening.
        CharSequence opaque = new CharSequence() {
            @Override public int length() { return text.length(); }
            @Override public char charAt(int index) { return text.charAt(index); }
            @Override public CharSequence subSequence(int a, int b) { throw new AssertionError("slice"); }
            @Override public String toString() { throw new AssertionError("flatten"); }
        };
        return TextMetadata.summarize(opaque, start, end);
    }

    private TextMetadata.Summary join(TextMetadata.Summary... rows) {
        TextMetadata.Summary result = TextMetadata.concatAll(rows);
        if (nativeEnabled) check(result, TextMetadata.concatNativeRequired(rows));
        return result;
    }

    private static TextMetadata.Summary oracle(String text) {
        return text.isEmpty() ? TextMetadata.EMPTY : new TextMetadata.Summary(text.length(),
                text.hashCode(), text.codePointCount(0, text.length()), text.charAt(0),
                text.charAt(text.length() - 1));
    }

    private void failures() {
        expect(NullPointerException.class, () -> TextMetadata.summarize(null, 0, 0));
        expect(IndexOutOfBoundsException.class, () -> summary("a", -1, 0));
        expect(IndexOutOfBoundsException.class, () -> summary("a", 1, 0));
        expect(IndexOutOfBoundsException.class, () -> summary("a", 0, 2));
        expect(IllegalArgumentException.class, () -> TextMetadata.pow31(-1));
        expect(IllegalArgumentException.class, () -> new TextMetadata.Summary(0, 1, 0, '\0', '\0'));
        expect(IllegalArgumentException.class, () -> new TextMetadata.Summary(1, 65, 1, 'A', 'B'));
        expect(IllegalArgumentException.class, () -> new TextMetadata.Summary(3, 0, 1, 'A', 'B'));
        expect(NullPointerException.class, () -> TextMetadata.concatAll((TextMetadata.Summary[]) null));
        expect(NullPointerException.class, () -> TextMetadata.concatAll(TextMetadata.EMPTY, null));
        TextMetadata.Summary huge = new TextMetadata.Summary(Integer.MAX_VALUE, 0,
                Integer.MAX_VALUE, 'A', 'A');
        expect(ArithmeticException.class, () -> TextMetadata.concatAll(huge, oracle("A")));
        if (!nativeEnabled) {
            expect(UnsatisfiedLinkError.class, () -> TextMetadata.concatNativeRequired(TextMetadata.EMPTY));
            return;
        }
        check(TextMetadata.EMPTY, TextMetadata.concatNativeRequired());
        expect(ArithmeticException.class, () -> TextMetadata.concatNativeRequired(huge, oracle("A")));
        expect(NullPointerException.class, () -> TextMetadata.nativeConcat(null));
        expect(IllegalArgumentException.class, () -> TextMetadata.nativeConcat(new int[1]));
        int[][] invalid = {{-1,0,0,0,0}, {2,0,3,65,65}, {3,0,1,65,65},
                {0,1,0,0,0}, {0,0,0,1,0}, {1,65,1,65,66}, {1,66,1,65,65},
                {2,0,2,-1,65}, {2,0,2,65,65536}};
        for (int[] row : invalid) {
            expect(IllegalArgumentException.class, () -> TextMetadata.nativeConcat(row));
        }
        int[] input = {1,65,1,65,65,0,0,0,0,0,1,66,1,66,66};
        int[] copy = input.clone();
        check(true, Arrays.equals(new int[] {2,2081,2,65,66}, TextMetadata.nativeConcat(input)));
        check(true, Arrays.equals(copy, input));
        // A valid prefix followed by malformed metadata must be rejected as a whole.
        int[] badTail = Arrays.copyOf(input, input.length + 5);
        badTail[input.length] = -1;
        expect(IllegalArgumentException.class, () -> TextMetadata.nativeConcat(badTail));
    }

    private void check(Object expected, Object actual) {
        checks++;
        if (!Objects.equals(expected, actual)) throw new AssertionError(expected + " != " + actual);
    }

    private void expect(Class<? extends Throwable> type, Runnable operation) {
        checks++;
        try { operation.run(); }
        catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("expected " + type.getName(), error);
        }
        throw new AssertionError("missing " + type.getName());
    }
}
