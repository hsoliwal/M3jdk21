/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.swing.viewport;

/**
 * Allocation-free text into a caller's reusable {@code char[]}. Every method returns the length
 * written, or {@code -(needed) - 1} when the buffer is too small (the caller grows and retries).
 */
//FLLDCIM: HPCPOP — paint-path formatting; no allocation
final class M3Chars {
	private M3Chars () {
	}

	static int copy (String s, char [] dst) {
		int len = s.length ();
		if (len > dst.length) return -len - 1;
		s.getChars (0, len, dst, 0);
		return len;
	}

	static int decimal (int v, char [] dst) {
		return decimal ((long) v, dst);
	}

	static int decimal (long v, char [] dst) {
		if (v == Long.MIN_VALUE) return copy ("-9223372036854775808", dst);
		int digits = 1;
		long a = v < 0 ? -v : v;
		for (long t = a; t >= 10; t /= 10) digits++;
		int len = digits + (v < 0 ? 1 : 0);
		if (len > dst.length) return -len - 1;
		int at = len;
		do {
			dst [--at] = (char) ('0' + (int) (a % 10));
			a /= 10;
		} while (a != 0);
		if (v < 0) dst [0] = '-';
		return len;
	}
}
