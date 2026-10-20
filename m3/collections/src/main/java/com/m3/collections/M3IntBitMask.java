/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * Derived by recipe (m3/docs/viewport-everywhere/recipe) from hsoliwal/eclipse.platform.swt 5593cc43baad179097f1fc3f72c9e5d8b6f8fb8a,
 * M3RowMask.java; Apache-2.0 elected from its Apache-2.0 OR EPL-2.0 publication
 * (m3/docs/viewport-everywhere/LICENSE_ELECTION.tsv).
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 */
package com.m3.collections;

/*
 * int-word variant generated from the long-word template M3BitMask by M3WordWidthVariant (TmOP; GWT/RAP builds);
 * in the comments below read long as int, 64 as 32, 63 as 31 and >>> 6 as >>> 5.
 * Row masks: one bit per source row in a long[] lane (bit i of word i >>> 6). Bit runs are moved
 * 64 bits at a time for insertions and removals of source rows; the kept lane of a mask is its
 * compaction (trailing-zero walk) and a view row is found back by a rank query (per-word prefix
 * popcounts + bitCount of the masked word).
 */
public final class M3IntBitMask {

	private M3IntBitMask () {
	}

	public static int words (int bits) {
		return (bits + 31) >>> 5;
	}

	public static int [] ensure (int [] mask, int bits) {
		int words = words (bits);
		if (mask.length >= words) return mask;
		int [] grown = new int [Math.max (words, mask.length + (mask.length >> 1))];
		System.arraycopy (mask, 0, grown, 0, mask.length);
		return grown;
	}

	public static boolean get (int [] mask, int bit) {
		return (mask [bit >>> 5] & 1 << bit) != 0;
	}

	public static void set (int [] mask, int bit, boolean value) {
		if (value) mask [bit >>> 5] |= 1 << bit; else mask [bit >>> 5] &= ~(1 << bit);
	}

	/* sets bits [0, size) and clears the rest of the lane */
	public static void fill (int [] mask, int size) {
		int full = size >>> 5;
		java.util.Arrays.fill (mask, 0, full, -1);
		java.util.Arrays.fill (mask, full, mask.length, 0);
		if ((size & 31) != 0) mask [full] = (1 << size) - 1;
	}

	/* bits [start, size) move up by count; the count bits from start are cleared (capacity size + count) */
	public static void insert (int [] mask, int size, int start, int count) {
		for (int remaining = size - start; remaining > 0;) {
			int length = Math.min (32, remaining);
			remaining -= length;
			write (mask, start + remaining + count, read (mask, start + remaining), length);
		}
		clear (mask, start, count);
	}

	/* bits [start + count, size) move down by count; the count bits at the end are cleared */
	public static void remove (int [] mask, int size, int start, int count) {
		for (int done = 0, total = size - start - count; done < total;) {
			int length = Math.min (32, total - done);
			write (mask, start + done, read (mask, start + count + done), length);
			done += length;
		}
		clear (mask, size - count, count);
	}

	public static void clear (int [] mask, int start, int count) {
		for (int done = 0; done < count;) {
			int length = Math.min (32, count - done);
			write (mask, start + done, 0, length);
			done += length;
		}
	}

	/* the 64 bits from position (bits past the lane read as 0) */
	public static int read (int [] mask, int position) {
		int word = position >>> 5, bit = position & 31;
		int low = word < mask.length ? mask [word] >>> bit : 0;
		if (bit == 0) return low;
		int high = word + 1 < mask.length ? mask [word + 1] << (32 - bit) : 0;
		return low | high;
	}

	/* writes the low length (1 .. 64) bits of value at position */
	public static void write (int [] mask, int position, int value, int length) {
		int word = position >>> 5, bit = position & 31;
		int ones = length == 32 ? -1 : (1 << length) - 1;
		value &= ones;
		mask [word] = mask [word] & ~(ones << bit) | value << bit;
		if (bit + length > 32) {
			int shift = 32 - bit;
			mask [word + 1] = mask [word + 1] & ~(ones >>> shift) | value >>> shift;
		}
	}

	/* writes the positions of the set bits of mask [0, words) into kept (capacity: their count); returns the count */
	public static int compact (int [] mask, int words, int [] kept, int [] rank) {
		int count = 0;
		for (int w = 0; w < words; w++) {
			rank [w] = count;
			for (int x = mask [w]; x != 0; x &= x - 1) kept [count++] = w << 5 | Integer.numberOfTrailingZeros (x);
		}
		return count;
	}

	/* the number of set bits of mask [0, words) */
	public static int count (int [] mask, int words) {
		int count = 0;
		for (int w = 0; w < words; w++) count += Integer.bitCount (mask [w]);
		return count;
	}

	/* the view row of a set bit, -1 for a clear bit */
	public static int rank (int [] mask, int [] rank, int bit) {
		int word = mask [bit >>> 5], b = 1 << bit;
		return (word & b) == 0 ? -1 : rank [bit >>> 5] + Integer.bitCount (word & b - 1);
	}
}
