/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

/**
 * Heap measurements for {@link M3WarmHeapGrowth}: the JVM's maximum heap and the heap in use. Written for M3 (the
 * EPL-2.0-only Nebula sampler it replaces was not copied); kept separate so a web build can supply its own variant.
 */
public final class M3HeapSampler {
	private M3HeapSampler () {
	}

	/** The maximum heap the JVM will try to use ({@link Runtime#maxMemory()}). */
	public static long maxMemory () {
		return Runtime.getRuntime ().maxMemory ();
	}

	/** The heap in use: total heap minus free heap. */
	public static long usedMemory () {
		Runtime runtime = Runtime.getRuntime ();
		return runtime.totalMemory () - runtime.freeMemory ();
	}
}
