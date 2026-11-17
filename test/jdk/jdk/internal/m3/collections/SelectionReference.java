// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import java.util.*;

/**
 * WindowRuntime.Selection as on master before the represented-length leaf: the reference of
 * SelectionCountTest; test only.
 */
final class SelectionReference {
	private int logicalCount;
	private boolean allSelected;
	private int [] starts = new int [4];
	private int [] ends = new int [4];
	private int rangeCount;

SelectionReference () {
}

	public int logicalCount () {
		return logicalCount;
	}

	public void setLogicalCount (int count) {
		if (count < 0) throw new IllegalArgumentException ("negative logical count");
		if (count == logicalCount) return;
		if (count < logicalCount) {
			remove (count, logicalCount - count);
			return;
		}
		int oldCount = logicalCount;
		logicalCount = count;
		if (allSelected && oldCount < count) addRange (oldCount, count);
	}

	public boolean isSelected (int index) {
		checkIndex (index);
		boolean represented = contains (index);
		return allSelected ? !represented : represented;
	}

	public void setSelected (int index, boolean selected) {
		checkIndex (index);
		if (selected == isSelected (index)) return;
		if (allSelected) {
			if (selected) removeRange (index, index + 1);
			else addRange (index, index + 1);
		} else {
			if (selected) addRange (index, index + 1);
			else removeRange (index, index + 1);
		}
	}

	public void selectRange (int start, int endExclusive) {
		checkRange (start, endExclusive);
		if (allSelected) removeRange (start, endExclusive);
		else addRange (start, endExclusive);
	}

	public void deselectRange (int start, int endExclusive) {
		checkRange (start, endExclusive);
		if (allSelected) addRange (start, endExclusive);
		else removeRange (start, endExclusive);
	}

	public void clear () {
		allSelected = false;
		rangeCount = 0;
	}

	public void selectAll () {
		allSelected = true;
		rangeCount = 0;
	}

	/** Inverts every row in O(1): the represented ranges flip meaning. */
	public void invert () {
		allSelected = !allSelected;
	}

	public int selectedCount () {
		long represented = 0;
		for (int i = 0; i < rangeCount; i++) represented += ends [i] - starts [i];
		long selected = allSelected ? (long)logicalCount - represented : represented;
		return Math.toIntExact (selected);
	}

	public int [] toArray () {
		int count = selectedCount ();
		int [] result = new int [count];
		int offset = 0;
		if (!allSelected) {
			for (int i = 0; i < rangeCount; i++) {
				offset = appendRange (result, offset, starts [i], ends [i]);
			}
			return result;
		}
		// Excluded ranges are already normalized; enumerate only the selected gaps.
		int start = 0;
		for (int range = 0; range < rangeCount; range++) {
			offset = appendRange (result, offset, start, starts [range]);
			start = ends [range];
		}
		appendRange (result, offset, start, logicalCount);
		return result;
	}

	private static int appendRange (int [] result, int offset, int start, int endExclusive) {
		for (int value = start; value < endExclusive; value++) {
			result [offset++] = value;
		}
		return offset;
	}

	public void insert (int index, int count) {
		if (count < 0 || index < 0 || index > logicalCount) {
			throw new IllegalArgumentException ("invalid insert");
		}
		if (count == 0) return;
		int nextCount = Math.addExact (logicalCount, count);
		int first = firstEndingAtOrAfter (index);
		if (!allSelected && first < rangeCount && ends [first] == index) first++;
		boolean crossing = first < rangeCount && starts [first] < index;
		boolean touches = first < rangeCount && starts [first] <= index;
		int extra = allSelected ? (touches ? 0 : 1) : (crossing ? 1 : 0);
		int capacity = compactCapacity (rangeCount + extra);
		if (capacity != starts.length || capacity != ends.length) {
			int [] nextStarts = Arrays.copyOf (starts, capacity);
			int [] nextEnds = Arrays.copyOf (ends, capacity);
			starts = nextStarts;
			ends = nextEnds;
		}
		if (allSelected) {
			// New rows are unselected. Extend a touching exception, or insert one.
			if (touches) {
				ends [first] += count;
				shiftRanges (first + 1, count);
			} else {
				for (int i = rangeCount; i > first; i--) {
					starts [i] = starts [i - 1] + count;
					ends [i] = ends [i - 1] + count;
				}
				starts [first] = index;
				ends [first] = index + count;
				rangeCount++;
			}
		} else {
			if (crossing) {
				// Split the only selected interval crossing the insertion point.
				int end = ends [first];
				for (int i = rangeCount; i > first + 1; i--) {
					starts [i] = starts [i - 1] + count;
					ends [i] = ends [i - 1] + count;
				}
				ends [first] = index;
				starts [first + 1] = index + count;
				ends [first + 1] = end + count;
				rangeCount++;
			} else {
				shiftRanges (first, count);
			}
		}
		logicalCount = nextCount;
	}

	private void shiftRanges (int first, int count) {
		for (int i = first; i < rangeCount; i++) {
			starts [i] += count;
			ends [i] += count;
		}
	}

	public void remove (int index, int count) {
		if (count < 0 || index < 0 || index > logicalCount - count) {
			throw new IllegalArgumentException ("invalid remove");
		}
		if (count == 0) return;
		int nextCount = projectRemoval (index, count, null, null);
		// Match the historical rebuild's compact geometric capacity. Reuse when
		// already compact, but actually release large backing after collapsing runs.
		int capacity = compactCapacity (nextCount);
		int [] nextStarts = starts, nextEnds = ends;
		if (capacity != starts.length || capacity != ends.length) {
			nextStarts = new int [capacity];
			nextEnds = new int [capacity];
		}
		projectRemoval (index, count, nextStarts, nextEnds);
		starts = nextStarts;
		ends = nextEnds;
		rangeCount = nextCount;
		logicalCount -= count;
	}

	/** Count first, then compact left-to-right; no temporary Selection or per-run object. */
	private int projectRemoval (int index, int count, int [] outStarts, int [] outEnds) {
		int endRemoved = index + count;
		int written = 0, previousEnd = -1;
		for (int i = 0; i < rangeCount; i++) {
			int start = starts [i], end = ends [i];
			start = start >= endRemoved ? start - count : Math.min (start, index);
			end = end >= endRemoved ? end - count : Math.min (end, index);
			if (start >= end) continue;
			if (written != 0 && start <= previousEnd) {
				previousEnd = Math.max (previousEnd, end);
				if (outEnds != null) outEnds [written - 1] = previousEnd;
			} else {
				if (outStarts != null) {
					outStarts [written] = start;
					outEnds [written] = end;
				}
				previousEnd = end;
				written++;
			}
		}
		return written;
	}

	private static int compactCapacity (int required) {
		int capacity = 4;
		while (capacity < required) capacity += capacity / 2;
		return capacity;
	}

	public int rangeCount () {
		return rangeCount;
	}

	public int rangeStart (int range) {
		return starts [rangeIndex (range)];
	}

	public int rangeEndExclusive (int range) {
		return ends [rangeIndex (range)];
	}

	public boolean complementMode () {
		return allSelected;
	}

	private void addRange (int start, int endExclusive) {
		if (start >= endExclusive) return;
		int first = firstEndingAtOrAfter (start);
		int mergedStart = start, mergedEnd = endExclusive;
		int last = first;
		while (last < rangeCount && starts [last] <= mergedEnd) {
			mergedStart = Math.min (mergedStart, starts [last]);
			mergedEnd = Math.max (mergedEnd, ends [last]);
			last++;
		}
		int removed = last - first;
		ensureCapacity (rangeCount - removed + 1);
		System.arraycopy (starts, last, starts, first + 1, rangeCount - last);
		System.arraycopy (ends, last, ends, first + 1, rangeCount - last);
		starts [first] = mergedStart;
		ends [first] = mergedEnd;
		rangeCount = rangeCount - removed + 1;
	}

	private void removeRange (int start, int endExclusive) {
		if (start >= endExclusive || rangeCount == 0) return;
		int first = firstEndingAtOrAfter (start + 1);
		if (first == rangeCount || starts [first] >= endExclusive) return;
		int last = first;
		while (last < rangeCount && starts [last] < endExclusive) last++;
		int leftStart = starts [first];
		int rightEnd = ends [last - 1];
		boolean left = leftStart < start;
		boolean right = rightEnd > endExclusive;
		int pieces = (left ? 1 : 0) + (right ? 1 : 0);
		int newCount = rangeCount - (last - first) + pieces;
		ensureCapacity (newCount);
		System.arraycopy (starts, last, starts, first + pieces, rangeCount - last);
		System.arraycopy (ends, last, ends, first + pieces, rangeCount - last);
		int write = first;
		if (left) {
			starts [write] = leftStart;
			ends [write++] = start;
		}
		if (right) {
			starts [write] = endExclusive;
			ends [write] = rightEnd;
		}
		rangeCount = newCount;
	}

	/** First range whose end is at or after <code>position</code> (sorted, disjoint ranges). */
	private int firstEndingAtOrAfter (int position) {
		int low = 0, high = rangeCount;
		return LaneSearch.lowerBound(ends, low, high, position);
	}

	private boolean contains (int index) {
		int low = 0, high = rangeCount - 1;
		while (low <= high) {
			int mid = (low + high) >>> 1;
			if (index < starts [mid]) high = mid - 1;
			else if (index >= ends [mid]) low = mid + 1;
			else return true;
		}
		return false;
	}

	private void ensureCapacity (int required) {
		if (required <= starts.length) return;
		int next = Math.max (required, Math.max (4, starts.length * 3 / 2));
		int [] nextStarts = Arrays.copyOf (starts, next);
		int [] nextEnds = Arrays.copyOf (ends, next);
		starts = nextStarts;
		ends = nextEnds;
	}

	private int rangeIndex (int range) {
		if (range < 0 || range >= rangeCount) {
			throw new IndexOutOfBoundsException ("Index " + range
					+ " out of bounds for length " + rangeCount);
		}
		return range;
	}

	private void checkIndex (int index) {
		if (index < 0 || index >= logicalCount) {
			throw new IllegalArgumentException ("index outside logical model");
		}
	}

	private void checkRange (int start, int endExclusive) {
		if (start < 0 || endExclusive < start || endExclusive > logicalCount) {
			throw new IllegalArgumentException ("range outside logical model");
		}
	}
}
