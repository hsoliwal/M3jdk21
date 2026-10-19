/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

/**
 * One sort key of an {@link M3PermutationIndex}: it writes an unsigned radix key per row into the index's
 * {@code keys} lane and answers how many low bytes of the key are significant (1 to 8).
 */
public abstract class M3RadixKey {
	/** For subclasses. */
	protected M3RadixKey () {
	}

	/**
	 * Writes {@code lanes.keys [i]} for every row {@code rows [i]}, {@code i < n} (unsigned order of the keys is the
	 * row order this key wants) and answers the number of significant low bytes.
	 */
	public abstract int radixKeys (int [] rows, int n, M3PermutationIndex lanes);
}
