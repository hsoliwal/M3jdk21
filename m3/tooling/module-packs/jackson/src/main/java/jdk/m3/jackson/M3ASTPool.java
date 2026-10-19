// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Adapted from com.synexia.mindex.MIndexASTPool, Synexia revision
// 50f48cbbe11f17fb8dc209bac3812f8940a1e267. Receiving changes: document lifetime,
// exact String-reference coordinates, bounded growth, UTF-16 hash domain and sealing.
package jdk.m3.jackson;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

/**
 * Document-scoped hash-consing over primitive kind/name/value and ordered-child lanes.
 *
 * <p>The temporary dictionary retains existing immutable String references and uses exact
 * String equality. It never creates a spelling buffer or another String implementation.
 * Hashes select candidates; exact coordinates and ordered children decide atom identity.
 * Children must exist before their parent, which prevents forward references and cycles.
 *
 * <p>The receiving hash domain preserves the donor's ordered mix and avalanche operations.
 * A text hash starts at {@code 0x4D33555446313601}, mixes its UTF-16 length, then each unsigned
 * UTF-16 code unit in order, and avalanches. Atom hashing mixes the kind, name and value text
 * hashes, explicit donor null sentinels, child count and ordered child structural hashes into
 * {@code 0x243F6A8885A308D3L}, then avalanches. Unpaired surrogates remain exact code units.
 * This format does not claim compatibility with the donor's UTF-8 content hashes.
 */
final class M3ASTPool {
    private static final int NO_STRING = -1;
    private static final int UNKNOWN_STRING = -2;
    private static final int MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;
    private static final int MAX_TABLE_LENGTH = 1 << 30;
    private static final long HASH_SEED = 0x243F6A8885A308D3L;
    private static final long TEXT_HASH_SEED = 0x4D33555446313601L;
    private static final long NULL_NAME_HASH = 0x13198A2E03707344L;
    private static final long NULL_VALUE_HASH = 0xA4093822299F31D0L;

    private final M3JsonLimits limits;
    private int[] kindIndexes = new int[0];
    private int[] nameIndexes = new int[0];
    private int[] valueIndexes = new int[0];
    private int[] childOffsets = new int[1];
    private int[] childCounts = new int[0];
    private long[] structuralHashes = new long[0];
    private int[] childArena = new int[0];
    private int[] slots = new int[0];
    private String[] strings = new String[0];
    private long[] textHashes = new long[0];
    private HashMap<String, Integer> stringsByValue = new HashMap<>();
    private int size;
    private int childArenaSize;
    private int stringCount;
    private long textUnits;
    private boolean sealed;

    M3ASTPool(M3JsonLimits limits) {
        this.limits = Objects.requireNonNull(limits, "limits");
        if (limits.maxAtoms() < 1 || limits.maxChildReferences() < 0
                || limits.maxTextUnits() < 0) {
            throw new IllegalArgumentException("invalid M3 AST storage limits");
        }
    }

    synchronized int intern(String kind, String name, String value, int[] children) {
        requireOpen();
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(children, "children");
        for (int child : children) {
            Objects.checkIndex(child, size);
        }

        int kindIndex = lookupString(kind);
        int nameIndex = lookupString(name);
        int valueIndex = lookupString(value);
        int newStrings = 0;
        long addedTextUnits = 0;
        if (kindIndex == UNKNOWN_STRING) {
            kindIndex = nextStringIndex(newStrings++);
            addedTextUnits = kind.length();
        }
        if (nameIndex == UNKNOWN_STRING) {
            if (name.equals(kind)) {
                nameIndex = kindIndex;
            } else {
                nameIndex = nextStringIndex(newStrings++);
                addedTextUnits += name.length();
            }
        }
        if (valueIndex == UNKNOWN_STRING) {
            if (value.equals(kind)) {
                valueIndex = kindIndex;
            } else if (value.equals(name)) {
                valueIndex = nameIndex;
            } else {
                valueIndex = nextStringIndex(newStrings++);
                addedTextUnits += value.length();
            }
        }
        if (addedTextUnits > limits.maxTextUnits() - textUnits) {
            throw new IllegalArgumentException("M3 AST retained text limit exceeded");
        }

        long kindHash = textHash(kindIndex, kind);
        long nameHash = name == null ? NULL_NAME_HASH
                : nameIndex == kindIndex ? kindHash : textHash(nameIndex, name);
        long valueHash = value == null ? NULL_VALUE_HASH
                : valueIndex == kindIndex ? kindHash
                : valueIndex == nameIndex ? nameHash : textHash(valueIndex, value);
        long hash = structuralHash(kindHash, nameHash, valueHash, children);
        if (newStrings == 0) {
            int existing = find(kindIndex, nameIndex, valueIndex, children, hash);
            if (existing >= 0) {
                return existing;
            }
        }

        if (size >= limits.maxAtoms() || size >= MAX_ARRAY_LENGTH - 1) {
            throw new IllegalArgumentException("M3 AST atom limit exceeded");
        }
        long requiredChildren = (long) childArenaSize + children.length;
        if (requiredChildren > limits.maxChildReferences()
                || requiredChildren > MAX_ARRAY_LENGTH) {
            throw new IllegalArgumentException("M3 AST child reference limit exceeded");
        }
        int requiredStrings = nextStringIndex(newStrings);
        ensureNodeCapacity(size + 1);
        ensureChildCapacity((int) requiredChildren);
        ensureStringCapacity(requiredStrings);
        ensureTableCapacity(size + 1);

        admitString(kind, kindIndex, kindHash);
        if (name != null) admitString(name, nameIndex, nameHash);
        if (value != null) admitString(value, valueIndex, valueHash);
        textUnits += addedTextUnits;

        int atomIndex = size;
        int childOffset = childArenaSize;
        System.arraycopy(children, 0, childArena, childOffset, children.length);
        childArenaSize = (int) requiredChildren;
        kindIndexes[atomIndex] = kindIndex;
        nameIndexes[atomIndex] = nameIndex;
        valueIndexes[atomIndex] = valueIndex;
        childOffsets[atomIndex] = childOffset;
        childCounts[atomIndex] = children.length;
        structuralHashes[atomIndex] = hash;
        childOffsets[atomIndex + 1] = childArenaSize;
        size++;
        insert(atomIndex);
        return atomIndex;
    }

    /** Drop construction-only indexes and trim retained lanes before immutable publication. */
    synchronized void seal(int rootAtomIndex) {
        requireOpen();
        Objects.checkIndex(rootAtomIndex, size);
        kindIndexes = Arrays.copyOf(kindIndexes, size);
        nameIndexes = Arrays.copyOf(nameIndexes, size);
        valueIndexes = Arrays.copyOf(valueIndexes, size);
        childOffsets = Arrays.copyOf(childOffsets, size + 1);
        childCounts = Arrays.copyOf(childCounts, size);
        structuralHashes = Arrays.copyOf(structuralHashes, size);
        childArena = Arrays.copyOf(childArena, childArenaSize);
        strings = Arrays.copyOf(strings, stringCount);
        stringsByValue = null;
        slots = new int[0];
        textHashes = new long[0];
        sealed = true;
    }

    synchronized M3ASTAtom atom(int atomIndex) {
        checkAtomIndex(atomIndex);
        return new M3ASTAtom(this, atomIndex);
    }

    synchronized String kind(int atomIndex) {
        checkAtomIndex(atomIndex);
        return strings[kindIndexes[atomIndex]];
    }

    synchronized String name(int atomIndex) {
        checkAtomIndex(atomIndex);
        return stringOrNull(nameIndexes[atomIndex]);
    }

    synchronized String value(int atomIndex) {
        checkAtomIndex(atomIndex);
        return stringOrNull(valueIndexes[atomIndex]);
    }

    synchronized int childCount(int atomIndex) {
        checkAtomIndex(atomIndex);
        return childCounts[atomIndex];
    }

    synchronized int childAt(int atomIndex, int childIndex) {
        checkAtomIndex(atomIndex);
        Objects.checkIndex(childIndex, childCounts[atomIndex]);
        return childArena[childOffsets[atomIndex] + childIndex];
    }

    synchronized long structuralHash64(int atomIndex) {
        checkAtomIndex(atomIndex);
        return structuralHashes[atomIndex];
    }

    synchronized int size() {
        requireSealed();
        return size;
    }

    synchronized int childReferenceCount() {
        requireSealed();
        return childArenaSize;
    }

    synchronized long retainedTextUnits() {
        requireSealed();
        return textUnits;
    }

    synchronized void checkAtomIndex(int atomIndex) {
        requireSealed();
        Objects.checkIndex(atomIndex, size);
    }

    private int lookupString(String value) {
        if (value == null) return NO_STRING;
        Integer index = stringsByValue.get(value);
        return index == null ? UNKNOWN_STRING : index;
    }

    private int nextStringIndex(int additional) {
        long index = (long) stringCount + additional;
        if (index > MAX_ARRAY_LENGTH) {
            throw new IllegalArgumentException("M3 AST string coordinate limit exceeded");
        }
        return (int) index;
    }

    private long textHash(int index, String value) {
        if (index < stringCount) return textHashes[index];
        long hash = mix(TEXT_HASH_SEED, value.length());
        for (int unit = 0; unit < value.length(); unit++) {
            hash = mix(hash, value.charAt(unit));
        }
        return avalanche(hash);
    }

    private void admitString(String value, int index, long hash) {
        if (index < stringCount) return;
        if (index != stringCount) throw new AssertionError("M3 AST string coordinate order");
        stringsByValue.put(value, index);
        strings[index] = value;
        textHashes[index] = hash;
        stringCount++;
    }

    private String stringOrNull(int index) {
        return index == NO_STRING ? null : strings[index];
    }

    private int find(int kind, int name, int value, int[] children, long hash) {
        if (slots.length == 0) return -1;
        int mask = slots.length - 1;
        int slot = slot(hash, mask);
        while (true) {
            int stored = slots[slot];
            if (stored == 0) return -1;
            int index = stored - 1;
            if (structuralHashes[index] == hash && kindIndexes[index] == kind
                    && nameIndexes[index] == name && valueIndexes[index] == value
                    && sameChildren(index, children)) {
                return index;
            }
            slot = (slot + 1) & mask;
        }
    }

    private boolean sameChildren(int atomIndex, int[] query) {
        if (childCounts[atomIndex] != query.length) return false;
        int offset = childOffsets[atomIndex];
        for (int index = 0; index < query.length; index++) {
            if (childArena[offset + index] != query[index]) return false;
        }
        return true;
    }

    private long structuralHash(long kind, long name, long value, int[] children) {
        long hash = mix(HASH_SEED, kind);
        hash = mix(hash, name);
        hash = mix(hash, value);
        hash = mix(hash, children.length);
        for (int child : children) hash = mix(hash, structuralHashes[child]);
        return avalanche(hash);
    }

    private void insert(int atomIndex) {
        int mask = slots.length - 1;
        int slot = slot(structuralHashes[atomIndex], mask);
        while (slots[slot] != 0) slot = (slot + 1) & mask;
        slots[slot] = atomIndex + 1;
    }

    private void ensureTableCapacity(int required) {
        if (required <= (long) slots.length * 65 / 100) return;
        int capacity = Math.max(16, slots.length);
        while (required > (long) capacity * 65 / 100) {
            if (capacity == MAX_TABLE_LENGTH) {
                throw new IllegalArgumentException("M3 AST hash table limit exceeded");
            }
            capacity *= 2;
        }
        slots = new int[capacity];
        for (int index = 0; index < size; index++) insert(index);
    }

    private void ensureNodeCapacity(int required) {
        if (required <= structuralHashes.length) return;
        int capacity = capacity(structuralHashes.length, required,
                Math.min(limits.maxAtoms(), MAX_ARRAY_LENGTH - 1));
        kindIndexes = Arrays.copyOf(kindIndexes, capacity);
        nameIndexes = Arrays.copyOf(nameIndexes, capacity);
        valueIndexes = Arrays.copyOf(valueIndexes, capacity);
        childOffsets = Arrays.copyOf(childOffsets, capacity + 1);
        childCounts = Arrays.copyOf(childCounts, capacity);
        structuralHashes = Arrays.copyOf(structuralHashes, capacity);
    }

    private void ensureChildCapacity(int required) {
        if (required <= childArena.length) return;
        int capacity = capacity(childArena.length, required,
                (int) Math.min(limits.maxChildReferences(), MAX_ARRAY_LENGTH));
        childArena = Arrays.copyOf(childArena, capacity);
    }

    private void ensureStringCapacity(int required) {
        if (required <= strings.length) return;
        int capacity = capacity(strings.length, required,
                (int) Math.min((long) limits.maxAtoms() * 3, MAX_ARRAY_LENGTH));
        strings = Arrays.copyOf(strings, capacity);
        textHashes = Arrays.copyOf(textHashes, capacity);
    }

    private static int capacity(int current, int required, int limit) {
        if (required < 0 || required > limit) {
            throw new IllegalArgumentException("M3 AST array capacity limit exceeded");
        }
        int capacity = Math.min(limit, Math.max(16, current));
        while (capacity < required) capacity = (int) Math.min(limit, (long) capacity * 2);
        return capacity;
    }

    private void requireOpen() {
        if (sealed) throw new IllegalStateException("M3 AST document is sealed");
    }

    private void requireSealed() {
        if (!sealed) throw new IllegalStateException("M3 AST document is not sealed");
    }

    private static int slot(long hash, int mask) {
        long mixed = avalanche(hash);
        return (int) (mixed ^ (mixed >>> 32)) & mask;
    }

    private static long mix(long hash, long value) {
        long next = value + 0x9E3779B97F4A7C15L + (hash << 6) + (hash >>> 2);
        return hash ^ next;
    }

    private static long avalanche(long value) {
        long mixed = value;
        mixed ^= mixed >>> 33;
        mixed *= 0xFF51AFD7ED558CCDL;
        mixed ^= mixed >>> 33;
        mixed *= 0xC4CEB9FE1A85EC53L;
        mixed ^= mixed >>> 33;
        return mixed;
    }
}
