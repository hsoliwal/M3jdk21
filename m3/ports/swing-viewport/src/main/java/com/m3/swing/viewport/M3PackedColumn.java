/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.swing.viewport;

import java.util.Arrays;

import com.m3.swing.viewport.engine.PackedHeapGrowth;
import com.m3.swing.viewport.engine.residency.ItemBitSet;

/**
 * One table column in packed form. The storage kind follows the values written: the first
 * non-null value picks {@code INT}, {@code LONG}, {@code DOUBLE}, {@code BOOLEAN}, {@code STRING}
 * or {@code OBJECT}; a later value the kind cannot hold exactly promotes the column to
 * {@code OBJECT}, boxing what it holds, so every read returns a value equal to the one written.
 * Nulls are a sparse {@link ItemBitSet}. Strings live in one UTF-16 {@code char[]} pool with
 * per-row start and length, so a string cell costs no object until a caller reads it.
 *
 * <p>Reads for paint go through {@link #chars}: digits and pooled characters are copied into the
 * caller's reusable buffer without creating an object (doubles excepted).</p>
 */
//FLLDCIM: L2 — columnar primitive arrays, Xmx-aware growth, objects only when asked
final class M3PackedColumn {
	static final int AUTO = 0, INT = 1, LONG = 2, DOUBLE = 3, BOOLEAN = 4, STRING = 5, OBJECT = 6;

	private int kind;
	private int size;
	private int [] ints;
	private long [] longs;
	private Object [] objects;
	private ItemBitSet bits;
	private final ItemBitSet nulls = new ItemBitSet ();
	private char [] pool;
	private int poolUsed, poolLive;
	private final int [][] scratch = {new int [16]};

	M3PackedColumn (int size) {
		this.size = size;
		nulls.setRange (0, size, true);
	}

	int kind () {
		return kind;
	}

	int size () {
		return size;
	}

	// ---- reads ---------------------------------------------------------------------------------

	Object get (int row) {
		if (nulls.get (row)) return null;
		switch (kind) {
			case INT: return Integer.valueOf (ints [row]);
			case LONG: return Long.valueOf (longs [row]);
			case DOUBLE: return Double.valueOf (Double.longBitsToDouble (longs [row]));
			case BOOLEAN: return Boolean.valueOf (bits.get (row));
			case STRING: return new String (pool, ints [2 * row], ints [2 * row + 1]);
			default: return objects [row];
		}
	}

	/**
	 * Copies the cell's text into {@code dst}; returns its length, or {@code -(needed) - 1} when
	 * {@code dst} is too small. Null is the empty string. No object is created for int, long,
	 * boolean and string cells.
	 */
	int chars (int row, char [] dst) {
		if (nulls.get (row)) return 0;
		switch (kind) {
			case INT: return M3Chars.decimal (ints [row], dst);
			case LONG: return M3Chars.decimal (longs [row], dst);
			case BOOLEAN: return M3Chars.copy (bits.get (row) ? "true" : "false", dst);
			case STRING: {
				int len = ints [2 * row + 1];
				if (len > dst.length) return -len - 1;
				System.arraycopy (pool, ints [2 * row], dst, 0, len);
				return len;
			}
			case DOUBLE: return M3Chars.copy (Double.toString (Double.longBitsToDouble (longs [row])), dst);
			default: return M3Chars.copy (String.valueOf (objects [row]), dst);
		}
	}

	// ---- writes --------------------------------------------------------------------------------

	void set (int row, Object value) {
		if (value == null) {
			releaseString (row);
			nulls.set (row, true);
			if (kind == OBJECT) objects [row] = null;
			return;
		}
		if (kind == AUTO) adopt (kindOf (value));
		else if (kind != OBJECT && kind != kindOf (value)) promote ();
		releaseString (row);
		nulls.set (row, false);
		switch (kind) {
			case INT: ints [row] = (Integer) value; break;
			case LONG: longs [row] = (Long) value; break;
			case DOUBLE: longs [row] = Double.doubleToRawLongBits ((Double) value); break;
			case BOOLEAN: bits.set (row, (Boolean) value); break;
			case STRING: putString (row, (String) value); break;
			default: objects [row] = value;
		}
	}

	/** Opens {@code count} null rows at {@code row}. */
	void insert (int row, int count) {
		ensure (size + count);
		switch (kind) {
			case INT: System.arraycopy (ints, row, ints, row + count, size - row); break;
			case LONG: case DOUBLE: System.arraycopy (longs, row, longs, row + count, size - row); break;
			case STRING: System.arraycopy (ints, 2 * row, ints, 2 * (row + count), 2 * (size - row)); Arrays.fill (ints, 2 * row, 2 * (row + count), 0); break;
			case OBJECT: System.arraycopy (objects, row, objects, row + count, size - row); Arrays.fill (objects, row, row + count, null); break;
			default: break;
		}
		if (bits != null) bits.shift (row, count, size + count, scratch);
		nulls.shift (row, count, size + count, scratch);
		nulls.setRange (row, row + count, true);
		size += count;
	}

	/** Removes {@code count} rows at {@code row}. */
	void remove (int row, int count) {
		for (int r = row; r < row + count; r++) releaseString (r);
		int tail = size - row - count;
		switch (kind) {
			case INT: System.arraycopy (ints, row + count, ints, row, tail); break;
			case LONG: case DOUBLE: System.arraycopy (longs, row + count, longs, row, tail); break;
			case STRING: System.arraycopy (ints, 2 * (row + count), ints, 2 * row, 2 * tail); break;
			case OBJECT: System.arraycopy (objects, row + count, objects, row, tail); Arrays.fill (objects, size - count, size, null); break;
			default: break;
		}
		if (bits != null) {
			bits.setRange (row, row + count, false);
			bits.shift (row + count, -count, size, scratch);
		}
		nulls.setRange (row, row + count, false);
		nulls.shift (row + count, -count, size, scratch);
		size -= count;
	}

	/** Rotates rows {@code [from, to)} by {@code shift} (positive moves down), as {@code Collections.rotate}. */
	void rotate (int from, int to, int shift) {
		int n = to - from;
		if (n <= 0) return;
		int s = ((shift % n) + n) % n;
		if (s == 0) return;
		Object [] tmp = new Object [n];
		for (int i = 0; i < n; i++) tmp [i] = get (from + i);
		for (int i = 0; i < n; i++) set (from + (i + s) % n, tmp [i]);
	}

	/** Truncates or pads (with nulls) to {@code rows}. */
	void setSize (int rows) {
		if (rows < size) remove (rows, size - rows);
		else if (rows > size) insert (size, rows - size);
	}

	long bytes () {
		long b = nulls.bytes () + (bits == null ? 0 : bits.bytes ());
		if (ints != null) b += 4L * ints.length;
		if (longs != null) b += 8L * longs.length;
		if (objects != null) b += 4L * objects.length;
		if (pool != null) b += 2L * pool.length;
		return b;
	}

	// ---- internals -----------------------------------------------------------------------------

	private static int kindOf (Object value) {
		if (value instanceof Integer) return INT;
		if (value instanceof Long) return LONG;
		if (value instanceof Double) return DOUBLE;
		if (value instanceof Boolean) return BOOLEAN;
		if (value instanceof String) return STRING;
		return OBJECT;
	}

	private void adopt (int newKind) {
		kind = newKind;
		int cap = Math.max (size, 8);
		switch (kind) {
			case INT: ints = new int [cap]; break;
			case LONG: case DOUBLE: longs = new long [cap]; break;
			case BOOLEAN: bits = new ItemBitSet (); break;
			case STRING: ints = new int [2 * cap]; pool = new char [64]; break;
			default: objects = new Object [cap];
		}
	}

	private void promote () {
		Object [] boxed = new Object [Math.max (size, 8)];
		for (int r = 0; r < size; r++) boxed [r] = get (r);
		ints = null;
		longs = null;
		bits = null;
		pool = null;
		poolUsed = poolLive = 0;
		objects = boxed;
		kind = OBJECT;
	}

	private void ensure (int rows) {
		switch (kind) {
			case INT: if (rows > ints.length) ints = Arrays.copyOf (ints, PackedHeapGrowth.capacity (ints.length, rows, 4)); break;
			case LONG: case DOUBLE: if (rows > longs.length) longs = Arrays.copyOf (longs, PackedHeapGrowth.capacity (longs.length, rows, 8)); break;
			case STRING: if (2 * rows > ints.length) ints = Arrays.copyOf (ints, 2 * PackedHeapGrowth.capacity (ints.length / 2, rows, 8)); break;
			case OBJECT: if (rows > objects.length) objects = Arrays.copyOf (objects, PackedHeapGrowth.capacity (objects.length, rows, 4)); break;
			default: break;
		}
	}

	private void releaseString (int row) {
		if (kind == STRING && !nulls.get (row)) poolLive -= ints [2 * row + 1];
	}

	private void putString (int row, String s) {
		int len = s.length ();
		if (poolUsed + len > pool.length) {
			if (poolUsed - poolLive > poolLive) compact ();
			if (poolUsed + len > pool.length) pool = Arrays.copyOf (pool, PackedHeapGrowth.capacity (pool.length, poolUsed + len, 2));
		}
		s.getChars (0, len, pool, poolUsed);
		ints [2 * row] = poolUsed;
		ints [2 * row + 1] = len;
		poolUsed += len;
		poolLive += len;
	}

	/** Rewrites the pool with live strings only (row order). */
	private void compact () {
		char [] next = new char [Math.max (64, PackedHeapGrowth.capacity (0, poolLive + 64, 2))];
		int at = 0;
		for (int r = 0; r < size; r++) {
			if (nulls.get (r)) continue;
			int len = ints [2 * r + 1];
			System.arraycopy (pool, ints [2 * r], next, at, len);
			ints [2 * r] = at;
			at += len;
		}
		pool = next;
		poolUsed = at;
	}
}
