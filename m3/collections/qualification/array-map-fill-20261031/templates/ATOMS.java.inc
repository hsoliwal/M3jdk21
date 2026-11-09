    /**
     * The pure kernels of the ARRAY_MAP and ID_MAP templates (recipe ARRAY_MAP_FILL): arrays in, values out, no
     * instance state; both templates include this class, so every member carries the same atoms.
     * Every atom states its contract in its Javadoc (Requires, Ensures, Effects, Cost), word for word its row of
     * ATOMS.tsv. M3ArrayMapAtomContractTest fails when an atom lacks the contract, the row, its own JUnit test
     * (M3ArrayMapAtomsTest) or its own performance row (M3ArrayMapAtomsPerformanceTest).
     */
    static final class Atoms {
        private Atoms() {
        }

        /**
         * Index table length for SLOTS entries.
         * Requires: SLOTS is between 0 and 2^29 inclusive.
         * Ensures: the smallest power of two that is at least max(2 * SLOTS, MIN_SLOTS), so the load stays at or below
         * one half.
         * Effects: none.
         * Cost: O(1).
         */
        static int tableSize(int slots) {
            return Integer.highestOneBit(Math.max(2 * slots, MIN_SLOTS) - 1) << 1;
        }

        /**
         * The hash code of a key as java.util.Map defines it.
         * Requires: nothing; KEY may be null.
         * Ensures: 0 for a null KEY, KEY.hashCode() otherwise.
         * Effects: calls KEY.hashCode() once when KEY is not null.
         * Cost: O(1) plus the hashCode call.
         */
        static int hash(Object key) {
            return key == null ? 0 : key.hashCode();
        }

        /**
         * Spreads a hash code over the index bits: Fibonacci hashing with the high half folded down.
         * Requires: nothing.
         * Ensures: a bijection on int, so distinct codes stay distinct, and sequential codes spread over the low bits.
         * Effects: none.
         * Cost: O(1).
         */
        static int mix(int h) {
            int m = h * GOLDEN_RATIO_32;
            return m ^ m >>> FOLD_SHIFT;
        }

        /**
         * Key equality as java.util.Map defines it, with an identity short cut.
         * Requires: nothing; either argument may be null.
         * Ensures: true when KEY and STORED are the same reference, false when exactly one is null, KEY.equals(STORED)
         * otherwise.
         * Effects: calls KEY.equals(STORED) at most once, never STORED.equals(KEY), and not at all for the same
         * reference.
         * Cost: O(1) plus the equals call.
         */
        static boolean sameKey(Object key, Object stored) {
            return key == stored || key != null && key.equals(stored);
        }

        /**
         * Linear probe for KEY.
         * Requires: INDEX.length is a power of two and INDEX holds an empty (0) entry; each positive entry e names slot
         * e - 1, filled in HASHES and KEYS; H is hash(KEY).
         * Ensures: the slot of a key with hash H that sameKey accepts, or -1; tombstones (-1) are passed and the first
         * empty entry ends the probe.
         * Effects: none; reads only.
         * Cost: expected O(1) at a load of at most one half.
         */
        static int find(int[] index, int[] hashes, Object[] keys, Object key, int h) {
            int mask = index.length - 1;
            for (int i = mix(h) & mask; ; i = i + 1 & mask) {
                int e = index[i];
                if (e == 0) {
                    return -1;
                }
                if (e > 0 && hashes[e - 1] == h && sameKey(key, keys[e - 1])) {
                    return e - 1;
                }
            }
        }

        /**
         * The index position of an indexed slot.
         * Requires: SLOT + 1 is stored on the probe of H, that is, the slot is indexed under H.
         * Ensures: the position i on the probe of H with INDEX[i] == SLOT + 1.
         * Effects: none; reads only, and does not return when the requirement is broken.
         * Cost: expected O(1) at a load of at most one half.
         */
        static int positionOf(int[] index, int h, int slot) {
            int mask = index.length - 1;
            for (int i = mix(h) & mask; ; i = i + 1 & mask) {
                if (index[i] == slot + 1) {
                    return i;
                }
            }
        }

        /**
         * Indexes SLOT under H.
         * Requires: the probe of H holds an empty (0) or tombstone (-1) entry, and SLOT is not indexed yet.
         * Ensures: SLOT + 1 is stored at the first empty or tombstone entry of the probe of H; returns true when that
         * entry was a tombstone.
         * Effects: writes exactly one INDEX entry.
         * Cost: expected O(1) at a load of at most one half.
         */
        static boolean insert(int[] index, int slot, int h) {
            int mask = index.length - 1;
            for (int i = mix(h) & mask; ; i = i + 1 & mask) {
                int e = index[i];
                if (e <= 0) {
                    index[i] = slot + 1;
                    return e < 0;
                }
            }
        }

        /**
         * Forward scan of the live mask.
         * Requires: FROM is not negative; MASK holds at least (USED + 63) / 64 words and no set bit at or above USED.
         * Ensures: the first set bit at or after FROM and below USED, or -1.
         * Effects: none; reads only.
         * Cost: O(1) plus one step per word skipped.
         */
        static int nextLive(long[] mask, int from, int used) {
            if (from >= used) {
                return -1;
            }
            int w = from >>> WORD_SHIFT;
            long word = mask[w] & -1L << from;
            for (int words = used + WORD_MASK >>> WORD_SHIFT; ; ) {
                if (word != 0) {
                    return (w << WORD_SHIFT) + Long.numberOfTrailingZeros(word);
                }
                if (++w >= words) {
                    return -1;
                }
                word = mask[w];
            }
        }

        /**
         * Backward scan of the live mask.
         * Requires: FROM is below 64 * MASK.length.
         * Ensures: the last set bit at or before FROM, or -1, which is also the answer for a negative FROM.
         * Effects: none; reads only.
         * Cost: O(1) plus one step per word skipped.
         */
        static int prevLive(long[] mask, int from) {
            if (from < 0) {
                return -1;
            }
            int w = from >>> WORD_SHIFT;
            long word = mask[w] & -1L >>> WORD_MASK - (from & WORD_MASK);
            while (true) {
                if (word != 0) {
                    return (w << WORD_SHIFT) + WORD_MASK - Long.numberOfLeadingZeros(word);
                }
                if (--w < 0) {
                    return -1;
                }
                word = mask[w];
            }
        }

        /**
         * The live slot of a given rank.
         * Requires: RANK is not negative and is below the number of set bits of MASK.
         * Ensures: the set bit with exactly RANK set bits below it.
         * Effects: none; reads only.
         * Cost: one popcount per word before the answer, plus at most 63 bit clears in its word.
         */
        static int select(long[] mask, int rank) {
            for (int w = 0; ; w++) {
                long word = mask[w];
                int count = Long.bitCount(word);
                if (rank < count) {
                    for (; rank > 0; rank--) {
                        word &= word - 1;
                    }
                    return (w << WORD_SHIFT) + Long.numberOfTrailingZeros(word);
                }
                rank -= count;
            }
        }

        /**
         * The rank of a slot.
         * Requires: SLOT is not negative and is below 64 * MASK.length.
         * Ensures: the number of set bits of MASK below SLOT.
         * Effects: none; reads only.
         * Cost: one popcount per word up to SLOT.
         */
        static int rank(long[] mask, int slot) {
            int rank = 0;
            int w = slot >>> WORD_SHIFT;
            for (int i = 0; i < w; i++) {
                rank += Long.bitCount(mask[i]);
            }
            return rank + Long.bitCount(mask[w] & (1L << slot) - 1);
        }

        /**
         * Stable two-pointer compaction of the slot lanes.
         * Requires: TO_LIVE is all zero and, like the destination lanes, has room for every live slot; LIVE has no set
         * bit at or above USED.
         * Ensures: the live slots, in ascending order, are copied to destination slots 0 to n - 1 with their bits set
         * in TO_LIVE; returns n.
         * Effects: writes the destination lanes only; the source lanes and LIVE are unchanged.
         * Cost: one step per word of LIVE below USED plus one per live slot.
         */
        static int compact(Object[] keys, Object[] values, int[] hashes, long[] live, int used, Object[] toKeys,
                Object[] toValues, int[] toHashes, long[] toLive) {
            int write = 0;
            for (int w = 0, words = used + WORD_MASK >>> WORD_SHIFT; w < words; w++) {
                for (long word = live[w]; word != 0; word &= word - 1) {
                    int read = (w << WORD_SHIFT) + Long.numberOfTrailingZeros(word);
                    toKeys[write] = keys[read];
                    toValues[write] = values[read];
                    toHashes[write] = hashes[read];
                    toLive[write >>> WORD_SHIFT] |= 1L << write;
                    write++;
                }
            }
            return write;
        }

        /**
         * The hash code of an M3 id (an M3String canonical id, an M3AST node id, any M3 key id).
         * Requires: nothing.
         * Ensures: Long.hashCode(ID), so equal ids fold equally.
         * Effects: none.
         * Cost: O(1).
         */
        static int foldId(long id) {
            return Long.hashCode(id);
        }

        /**
         * Linear probe for an M3 id: one id lane read per occupied entry, no hash lane and no equals call.
         * Requires: INDEX.length is a power of two and INDEX holds an empty (0) entry; each positive entry e names slot
         * e - 1, filled in IDS; H is foldId(ID).
         * Ensures: the slot holding ID, or -1; tombstones (-1) are passed and the first empty entry ends the probe.
         * Effects: none; reads only.
         * Cost: expected O(1) at a load of at most one half.
         */
        static int findId(int[] index, long[] ids, long id, int h) {
            int mask = index.length - 1;
            for (int i = mix(h) & mask; ; i = i + 1 & mask) {
                int e = index[i];
                if (e == 0) {
                    return -1;
                }
                if (e > 0 && ids[e - 1] == id) {
                    return e - 1;
                }
            }
        }

        /**
         * Stable two-pointer compaction of the id lanes.
         * Requires: TO_LIVE is all zero and, like TO_IDS and TO_VALUES, has room for every live slot; LIVE has no set
         * bit at or above USED.
         * Ensures: the live slots, in ascending order, are copied to destination slots 0 to n - 1 with their bits set
         * in TO_LIVE; returns n.
         * Effects: writes the destination lanes only; the source lanes and LIVE are unchanged.
         * Cost: one step per word of LIVE below USED plus one per live slot.
         */
        static int compactIds(long[] ids, Object[] values, long[] live, int used, long[] toIds, Object[] toValues,
                long[] toLive) {
            int write = 0;
            for (int w = 0, words = used + WORD_MASK >>> WORD_SHIFT; w < words; w++) {
                for (long word = live[w]; word != 0; word &= word - 1) {
                    int read = (w << WORD_SHIFT) + Long.numberOfTrailingZeros(word);
                    toIds[write] = ids[read];
                    toValues[write] = values[read];
                    toLive[write >>> WORD_SHIFT] |= 1L << write;
                    write++;
                }
            }
            return write;
        }
    }
