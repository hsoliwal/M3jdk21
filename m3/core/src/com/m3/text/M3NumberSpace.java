/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.util.Objects;

/**
 * M3JDK's immutable numeric vocabulary projection for Synexia's
 * {@code dictlang.numbers.0-10000} source.
 *
 * <p>The value views share one UTF-16 backing piece. A number lookup returns
 * the same cached view every time; flattening is explicit and limited to the
 * ordinary {@link String} boundary. Language coordinates are explicit
 * projection metadata and never alter canonical numeric identity.</p>
 */
public final class M3NumberSpace {
    public static final String SOURCE_ID = "dictlang.numbers.0-10000";
    public static final String SOURCE_REVISION = "64a2ea61c73b548413fed6686a9daeeb0b9b0564";
    public static final String RECORD_ID = "number";
    public static final String DEFAULT_LANGUAGE_TAG = "und";
    public static final int MIN_VALUE = 0;
    public static final int MAX_VALUE = 10_000;

    private static final SharedStorage SHARED_STORAGE = SharedStorage.create();

    /** Unscoped numeric vocabulary view; callers with language data must use {@link #forLanguage}. */
    public static final M3NumberSpace INSTANCE = new M3NumberSpace(DEFAULT_LANGUAGE_TAG);

    private final String languageTag;
    private final SharedStorage storage;

    private M3NumberSpace(String languageTag) {
        this.languageTag = requireLanguageTag(languageTag);
        this.storage = SHARED_STORAGE;
    }

    /** Return a language-scoped view while retaining the shared numeric backing. */
    public static M3NumberSpace forLanguage(String languageTag) {
        return new M3NumberSpace(languageTag);
    }

    /** Return the explicit source language coordinate for this projection view. */
    public String languageTag() {
        return languageTag;
    }

    /** Return the canonical cached view for one value. */
    public LocalM3StringPiece number(int value) {
        checkValue(value);
        return storage.values[value];
    }

    /** Materialize the canonical spelling at the ordinary String boundary. */
    public String spelling(int value) {
        return number(value).flatten();
    }

    /** Parse a canonical decimal view without calling {@code toString()}. */
    public LocalM3StringPiece parse(CharSequence spelling) {
        Objects.requireNonNull(spelling, "spelling");
        int length = spelling.length();
        if (length == 0 || (length > 1 && spelling.charAt(0) == '0'))
            throw new NumberFormatException("non-canonical number spelling: " + describe(spelling));
        int value = 0;
        for (int index = 0; index < length; index++) {
            char character = spelling.charAt(index);
            if (character < '0' || character > '9')
                throw new NumberFormatException("non-decimal number spelling: " + describe(spelling));
            int digit = character - '0';
            if (value > (MAX_VALUE - digit) / 10)
                throw new NumberFormatException("number outside 0..10000: " + describe(spelling));
            value = value * 10 + digit;
        }
        return number(value);
    }

    public int precomputedValueCount() {
        return storage.values.length;
    }

    /** All language views retain the same immutable backing owner. */
    public StorageIdentity storageIdentity() {
        return storage.backing.storageIdentity();
    }

    private static String describe(CharSequence spelling) {
        if (spelling instanceof String value) return value;
        StringBuilder result = new StringBuilder(spelling.length());
        for (int index = 0; index < spelling.length(); index++) result.append(spelling.charAt(index));
        return result.toString();
    }

    private static String requireLanguageTag(String value) {
        Objects.requireNonNull(value, "languageTag");
        String tag = value.trim();
        if (tag.isEmpty()) throw new IllegalArgumentException("languageTag is empty");
        for (int index = 0; index < tag.length(); index++) {
            if (Character.isWhitespace(tag.charAt(index)))
                throw new IllegalArgumentException("languageTag contains whitespace");
        }
        return tag;
    }

    private static void checkValue(int value) {
        if (value < MIN_VALUE || value > MAX_VALUE)
            throw new IndexOutOfBoundsException("number value: " + value);
    }

    private static final class SharedStorage {
        private final LocalM3StringPiece[] values;
        private final LocalM3StringPiece backing;

        private SharedStorage(LocalM3StringPiece[] values, LocalM3StringPiece backing) {
            this.values = values;
            this.backing = backing;
        }

        private static SharedStorage create() {
            int totalUnits = 0;
            for (int value = MIN_VALUE; value <= MAX_VALUE; value++)
                totalUnits = Math.addExact(totalUnits, Integer.toString(value).length());
            char[] all = new char[totalUnits];
            int[] offsets = new int[MAX_VALUE + 1];
            int[] lengths = new int[MAX_VALUE + 1];
            int cursor = 0;
            for (int value = MIN_VALUE; value <= MAX_VALUE; value++) {
                String spelling = Integer.toString(value);
                offsets[value] = cursor;
                lengths[value] = spelling.length();
                spelling.getChars(0, spelling.length(), all, cursor);
                cursor += spelling.length();
            }
            LocalM3StringPiece backing = new LocalM3Arena().copyUtf16(all);
            LocalM3StringPiece[] values = new LocalM3StringPiece[MAX_VALUE + 1];
            for (int value = MIN_VALUE; value <= MAX_VALUE; value++)
                values[value] = backing.subSequence(offsets[value], offsets[value] + lengths[value]);
            return new SharedStorage(values, backing);
        }
    }
}
