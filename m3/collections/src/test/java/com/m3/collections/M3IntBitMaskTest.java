/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.BitSet;
import java.util.Random;

import org.junit.jupiter.api.Test;

/** Oracle test of the generated int-word mask (M3IntBitMask, from the M3BitMask template) against java.util.BitSet. */
public class M3IntBitMaskTest {
	@Test
	public void intMaskMatchesBitSet () {
		Random random = new Random (17L);
		for (int round = 0; round < 200; round++) {
			int size = random.nextInt (200);
			int [] mask = M3IntBitMask.ensure (new int [0], size);
			assertTrue (mask.length >= M3IntBitMask.words (size));
			BitSet oracle = new BitSet ();
			for (int i = 0; i < size; i++) if (random.nextBoolean ()) {
				M3IntBitMask.set (mask, i, true);
				oracle.set (i);
			}
			if (size > 0 && random.nextBoolean ()) {
				int i = random.nextInt (size);
				M3IntBitMask.set (mask, i, false);
				oracle.clear (i);
			}
			for (int i = 0; i < size; i++) assertEquals (oracle.get (i), M3IntBitMask.get (mask, i));
			int words = M3IntBitMask.words (size);
			assertEquals (oracle.cardinality (), M3IntBitMask.count (mask, words));
			int [] kept = new int [size], rank = new int [Math.max (1, words)];
			int count = M3IntBitMask.compact (mask, words, kept, rank);
			assertArrayEquals (oracle.stream ().toArray (), java.util.Arrays.copyOf (kept, count));
			for (int i = 0; i < size; i++) {
				int expected = oracle.get (i) ? oracle.get (0, i).cardinality () : -1;
				assertEquals (expected, M3IntBitMask.rank (mask, rank, i));
			}
			if (size > 0) {
				int start = random.nextInt (size), n = random.nextInt (40);
				int [] grown = M3IntBitMask.ensure (mask, size + n);
				M3IntBitMask.insert (grown, size, start, n);
				BitSet shifted = new BitSet ();
				for (int i = 0; i < size; i++) if (oracle.get (i)) shifted.set (i < start ? i : i + n);
				for (int i = 0; i < size + n; i++) assertEquals (shifted.get (i), M3IntBitMask.get (grown, i), "insert round " + round);
				M3IntBitMask.remove (grown, size + n, start, n);
				for (int i = 0; i < size; i++) assertEquals (oracle.get (i), M3IntBitMask.get (grown, i), "remove round " + round);
			}
		}
	}

	@Test
	public void intWordEdges () {
		int [] mask = new int [3];
		M3IntBitMask.fill (mask, 66);
		assertEquals (66, M3IntBitMask.count (mask, 3));
		M3IntBitMask.fill (mask, 64);
		assertEquals (64, M3IntBitMask.count (mask, 3), "a full last word");
		M3IntBitMask.write (mask, 28, 0, 10);
		assertEquals (54, M3IntBitMask.count (mask, 3), "a write across a word boundary");
		M3IntBitMask.write (mask, 32, -1, 32);
		assertEquals (-1, M3IntBitMask.read (mask, 32));
		assertEquals (0, M3IntBitMask.read (mask, 96), "past the lane reads 0");
		assertEquals (mask [2], M3IntBitMask.read (mask, 64));
		M3IntBitMask.clear (mask, 0, 96);
		assertEquals (0, M3IntBitMask.count (mask, 3));
		assertSame (mask, M3IntBitMask.ensure (mask, 50));
	}
}
