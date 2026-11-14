// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Parser-neutral, optional typed facts over ONE original immutable M3ASTIndex.
 * One occurrence may have repeated typed keys; this is evidence, not semantic
 * binding proof. Immutable primitive column lanes plus two string dictionaries.
 */
public final class M3ASTFacts {
    public enum Kind { SIGNED_LONG, IEEE_DOUBLE_BITS, BOOLEAN, TEXT, NODE_REF }
    private static final Kind[] KINDS = Kind.values();
    public static final String SCHEMA = "M3_AST_TYPED_FACTS_V1";
    private final M3ASTIndex index;
    private final int[] offsets, owners, keyIds, textIds;
    private final long[] rawValues;
    private final byte[] tags;
    private final String[] keys, strings;

    private M3ASTFacts(Builder b) {
        index = b.index;
        owners = Arrays.copyOf(b.owners, b.size);
        keyIds = Arrays.copyOf(b.keyIds, b.size);
        textIds = Arrays.copyOf(b.textIds, b.size);
        rawValues = Arrays.copyOf(b.rawValues, b.size);
        tags = Arrays.copyOf(b.tags, b.size);
        keys = Arrays.copyOf(b.keys, b.keyCount);
        strings = Arrays.copyOf(b.strings, b.stringCount);
        offsets = new int[index.nodeCount() + 1];
        for (int owner : owners) offsets[owner + 1]++;
        for (int i = 1; i < offsets.length; i++) offsets[i] += offsets[i - 1];
    }
    public static Builder builder(M3ASTIndex index, int maxFacts, int maxDictionaryEntries,
                                  long maxDictionaryUtf16Units) {
        return new Builder(index, maxFacts, maxDictionaryEntries, maxDictionaryUtf16Units);
    }
    public M3ASTIndex index() { return index; }
    public int factCount() { return owners.length; }
    public int firstFact(int row) { return offsets[Objects.checkIndex(row, index.nodeCount())]; }
    public int endFact(int row) { return offsets[Objects.checkIndex(row, index.nodeCount()) + 1]; }
    public int owner(int fact) { return owners[check(fact)]; }
    public String key(int fact) { return keys[keyIds[check(fact)]]; }
    public Kind kind(int fact) { return KINDS[tags[check(fact)]]; }
    public long signedLong(int fact) { require(fact, Kind.SIGNED_LONG); return rawValues[fact]; }
    public double ieeeDouble(int fact) { require(fact, Kind.IEEE_DOUBLE_BITS); return Double.longBitsToDouble(rawValues[fact]); }
    public long ieeeDoubleRawBits(int fact) { require(fact, Kind.IEEE_DOUBLE_BITS); return rawValues[fact]; }
    public boolean booleanValue(int fact) { require(fact, Kind.BOOLEAN); return rawValues[fact] != 0; }
    public String text(int fact) { require(fact, Kind.TEXT); return strings[textIds[fact]]; }
    public int nodeRef(int fact) { require(fact, Kind.NODE_REF); return (int) rawValues[fact]; }
    public long retainedPrimitiveBytes() {
        return (long) offsets.length * Integer.BYTES + (long) factCount()
                * (Integer.BYTES * 3L + Long.BYTES + Byte.BYTES);
    }
    public void each(int row, String key, IntConsumer consumer) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(consumer, "consumer");
        int first = firstFact(row), end = endFact(row);
        for (int i = first; i < end; i++) if (keys[keyIds[i]].equals(key)) consumer.accept(i);
    }
    private int check(int fact) { return Objects.checkIndex(fact, owners.length); }
    private void require(int fact, Kind expected) {
        if (kind(fact) != expected) throw new IllegalStateException("different AST fact kind");
    }

    /** One owner, monotonic occurrence rows and bounded dictionary admission. */
    public static final class Builder {
        private final M3ASTIndex index;
        private final int maxFacts, maxDictionaryEntries;
        private final long maxDictionaryUtf16Units;
        private int[] owners, keyIds, textIds;
        private long[] rawValues;
        private byte[] tags;
        private String[] keys = new String[8], strings = new String[8];
        private int keyCount, stringCount, size, previousOwner = -1;
        private long dictionaryUnits;
        private boolean sealed;
        private Builder(M3ASTIndex index, int maxFacts, int maxDictionaryEntries, long maxUnits) {
            this.index = Objects.requireNonNull(index, "original AST index");
            if (maxFacts < 0 || maxFacts > 20_000_000 || maxDictionaryEntries < 0
                    || maxDictionaryEntries > 1_000_000 || maxUnits < 0)
                throw new IllegalArgumentException("invalid fact budgets");
            this.maxFacts = maxFacts;
            this.maxDictionaryEntries = maxDictionaryEntries;
            maxDictionaryUtf16Units = maxUnits;
            int initial = Math.max(1, Math.min(16, maxFacts));
            owners = new int[initial]; keyIds = new int[initial]; textIds = new int[initial];
            rawValues = new long[initial]; tags = new byte[initial];
        }
        public Builder signedLong(int row, String key, long value) {
            add(row, key, Kind.SIGNED_LONG, value, null); return this;
        }
        public Builder ieeeDouble(int row, String key, double value) {
            add(row, key, Kind.IEEE_DOUBLE_BITS, Double.doubleToRawLongBits(value), null); return this;
        }
        public Builder booleanValue(int row, String key, boolean value) {
            add(row, key, Kind.BOOLEAN, value ? 1 : 0, null); return this;
        }
        public Builder text(int row, String key, String value) {
            add(row, key, Kind.TEXT, 0, Objects.requireNonNull(value, "text value")); return this;
        }
        public Builder nodeRef(int row, String key, int target) {
            Objects.checkIndex(target, index.nodeCount());
            add(row, key, Kind.NODE_REF, target, null); return this;
        }
        private void add(int row, String key, Kind kind, long raw, String text) {
            if (sealed) throw new IllegalStateException("facts already sealed");
            Objects.checkIndex(row, index.nodeCount());
            if (row < previousOwner) throw new IllegalArgumentException("facts must follow index occurrence order");
            if (size == maxFacts) throw new IllegalArgumentException("AST fact budget exceeded");
            if (key == null || key.isBlank() || key.length() > 256)
                throw new IllegalArgumentException("invalid parser-neutral fact key");
            int keyId = dictionaryId(keys, keyCount, key);
            int txtId = text == null ? -1 : dictionaryId(strings, stringCount, text);
            int newKey = keyId < 0 ? 1 : 0;
            int newText = text != null && txtId < 0 ? 1 : 0;
            long extraUnits = (newKey != 0 ? key.length() : 0L)
                    + (newText != 0 ? text.length() : 0L);
            if ((long) keyCount + stringCount + newKey + newText > maxDictionaryEntries
                    || extraUnits > maxDictionaryUtf16Units - dictionaryUnits) {
                throw new IllegalArgumentException("fact dictionary budget exceeded");
            }
            if (newKey != 0) keyId = appendKey(key);
            if (newText != 0) txtId = appendText(text);
            if (size == owners.length) grow();
            owners[size] = row;
            keyIds[size] = keyId;
            textIds[size] = txtId;
            rawValues[size] = raw;
            tags[size] = (byte) kind.ordinal();
            size++;
            previousOwner = row;
        }
        private int appendKey(String text) {
            ensureDictionaryCapacity();
            budget(text);
            if (keyCount == keys.length) keys = Arrays.copyOf(keys, keys.length * 2);
            keys[keyCount] = text;
            return keyCount++;
        }
        private int appendText(String text) {
            ensureDictionaryCapacity();
            budget(text);
            if (stringCount == strings.length) strings = Arrays.copyOf(strings, strings.length * 2);
            strings[stringCount] = text;
            return stringCount++;
        }
        private void ensureDictionaryCapacity() {
            if ((long) keyCount + stringCount >= maxDictionaryEntries)
                throw new IllegalArgumentException("fact dictionary entry budget exceeded");
        }
        private void budget(String value) {
            if (value.length() > maxDictionaryUtf16Units - dictionaryUnits)
                throw new IllegalArgumentException("fact dictionary UTF-16 budget exceeded");
            dictionaryUnits += value.length();
        }
        private static int dictionaryId(String[] values, int count, String value) {
            for (int i = 0; i < count; i++) if (values[i].equals(value)) return i;
            return -1;
        }
        private void grow() {
            int capacity = (int) Math.min(maxFacts, Math.max((long) size + 1, size + (long) (size >>> 1) + 1));
            owners = Arrays.copyOf(owners, capacity);
            keyIds = Arrays.copyOf(keyIds, capacity);
            textIds = Arrays.copyOf(textIds, capacity);
            rawValues = Arrays.copyOf(rawValues, capacity);
            tags = Arrays.copyOf(tags, capacity);
        }
        public M3ASTFacts seal() {
            if (sealed) throw new IllegalStateException("facts already sealed");
            sealed = true;
            return new M3ASTFacts(this);
        }
    }
}
