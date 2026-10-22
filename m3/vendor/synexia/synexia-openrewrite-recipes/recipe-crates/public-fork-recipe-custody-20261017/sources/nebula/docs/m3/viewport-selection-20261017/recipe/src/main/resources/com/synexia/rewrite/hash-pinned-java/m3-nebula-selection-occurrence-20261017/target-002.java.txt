/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.RandomAccess;

/**
 * The selected items of a Grid in the viewport mode, in the order they were selected (as the
 * stock list keeps them). An item's position identifies its first occurrence; two primitive
 * lanes link its occurrences in list order, so duplicate selections preserve the stock list's
 * first-removal and membership semantics without scanning the selection during painting.
 * Membership, appending and removal by item are O(1), with hole compaction amortized over
 * removals. Indexed insertion and replacement rebuild the occurrence lanes after moving rows.
 */
final class GridSelectionList extends AbstractList<GridItem> implements RandomAccess {
	private GridItem[] order = new GridItem[16];
	/* Circular occurrence links; the previous link of the first occurrence is the last. */
	private int[] previousOccurrence = new int[16];
	private int[] nextOccurrence = new int[16];
	/* positions [head, end) hold the items and the holes */
	private int head;
	private int end;
	private int live;

	@Override
	public int size() {
		return live;
	}

	@Override
	public boolean contains(final Object o) {
		return positionOf(o) >= 0;
	}

	@Override
	public int indexOf(final Object o) {
		if (positionOf(o) < 0) {
			return -1;
		}
		compact();
		return ((GridItem) o).selectionPosition();
	}

	@Override
	public int lastIndexOf(final Object o) {
		if (positionOf(o) < 0) {
			return -1;
		}
		compact();
		return previousOccurrence[((GridItem) o).selectionPosition()];
	}

	@Override
	public GridItem get(final int index) {
		if (index < 0 || index >= live) {
			throw new IndexOutOfBoundsException("index " + index + ", size " + live);
		}
		if (index == 0) {
			skipHoles();
			return order[head];
		}
		compact();
		return order[index];
	}

	@Override
	public boolean add(final GridItem item) {
		add(live, item);
		return true;
	}

	@Override
	public void add(final int index, final GridItem item) {
		if (index < 0 || index > live) {
			throw new IndexOutOfBoundsException("index " + index + ", size " + live);
		}
		java.util.Objects.requireNonNull(item, "item");
		if (index != live) {
			// the Grid appends; an insertion elsewhere keeps the list semantics at O(n)
			compact();
			ensureCapacity(end + 1);
			System.arraycopy(order, index, order, index + 1, end - index);
			order[index] = item;
			end++;
			live++;
			rebuildOccurrences();
		} else {
			ensureCapacity(end + 1);
			appendOccurrence(end, item);
			end++;
			live++;
		}
		modCount++;
	}

	@Override
	public boolean addAll(final Collection<? extends GridItem> items) {
		if (items == this) {
			return addAll(Arrays.asList(toArray(new GridItem[0])));
		}
		if (items.isEmpty()) {
			return false;
		}
		ensureCapacity(end + items.size());
		for (final GridItem item : items) {
			add(item);
		}
		return true;
	}

	@Override
	public boolean remove(final Object o) {
		final int position = positionOf(o);
		if (position < 0) {
			return false;
		}
		removeOccurrence(position);
		return true;
	}

	@Override
	public GridItem remove(final int index) {
		final GridItem item = get(index);
		// get(0) skips leading holes; other indices compact before returning.
		removeOccurrence(index == 0 ? head : index);
		return item;
	}

	@Override
	public GridItem set(final int index, final GridItem item) {
		final GridItem old = get(index);
		java.util.Objects.requireNonNull(item, "item");
		if (old == item) {
			return old;
		}
		compact();
		old.markSelectionPosition(-1);
		order[index] = item;
		rebuildOccurrences();
		return old;
	}

	@Override
	public void clear() {
		for (int i = head; i < end; i++) {
			if (order[i] != null) {
				order[i].markSelectionPosition(-1);
				order[i] = null;
			}
		}
		head = end = live = 0;
		modCount++;
	}

	@Override
	public Object[] toArray() {
		return toArray(new GridItem[live]);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T[] toArray(final T[] a) {
		compact();
		final T[] result = a.length >= live ? a : (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), live);
		System.arraycopy(order, 0, result, 0, live);
		if (result.length > live) {
			result[live] = null;
		}
		return result;
	}

	@Override
	public Iterator<GridItem> iterator() {
		// AbstractList tracks logical indices: read-triggered compaction cannot move its cursor.
		// It also retains the stock list's iterator.remove/removeIf/retainAll contracts.
		return super.iterator();
	}

	private int advance(final int from) {
		int position = from;
		while (position < end && order[position] == null) {
			position++;
		}
		return position;
	}

	private int positionOf(final Object o) {
		if (!(o instanceof GridItem)) {
			return -1;
		}
		final int position = ((GridItem) o).selectionPosition();
		return position >= head && position < end && order[position] == o ? position : -1;
	}

	private void skipHoles() {
		head = advance(head);
	}

	/* Appends to an item's circular chain, keeping its first list occurrence indexed. */
	private void appendOccurrence(final int position, final GridItem item) {
		final int first = positionOf(item);
		order[position] = item;
		if (first < 0) {
			previousOccurrence[position] = nextOccurrence[position] = position;
			item.markSelectionPosition(position);
		} else {
			final int last = previousOccurrence[first];
			previousOccurrence[position] = last;
			nextOccurrence[position] = first;
			nextOccurrence[last] = position;
			previousOccurrence[first] = position;
		}
	}

	/* Unlinks exactly this occurrence without losing membership of other occurrences. */
	private void removeOccurrence(final int position) {
		final GridItem item = order[position];
		final int next = nextOccurrence[position];
		if (next == position) {
			item.markSelectionPosition(-1);
		} else {
			final int previous = previousOccurrence[position];
			nextOccurrence[previous] = next;
			previousOccurrence[next] = previous;
			if (item.selectionPosition() == position) {
				item.markSelectionPosition(next);
			}
		}
		order[position] = null;
		live--;
		modCount++;
		if (live == 0) {
			head = end = 0;
		} else if ((long) end - head > 2L * live + 16) {
			compact();
		}
	}

	/* Rebuild after moving positions. The first pass discards all old position identities. */
	private void rebuildOccurrences() {
		for (int i = head; i < end; i++) {
			if (order[i] != null) {
				order[i].markSelectionPosition(-1);
			}
		}
		for (int i = head; i < end; i++) {
			if (order[i] != null) {
				appendOccurrence(i, order[i]);
			}
		}
	}

	/* closes the holes: every occurrence moves to its list index */
	private void compact() {
		if (end - head == live) {
			if (head > 0) {
				System.arraycopy(order, head, order, 0, live);
				Arrays.fill(order, live, end, null);
				head = 0;
				end = live;
				rebuildOccurrences();
			}
			return;
		}
		int to = 0;
		for (int from = head; from < end; from++) {
			final GridItem item = order[from];
			if (item != null) {
				order[to++] = item;
			}
		}
		Arrays.fill(order, to, end, null);
		head = 0;
		end = to;
		rebuildOccurrences();
	}

	private void ensureCapacity(final int capacity) {
		if (capacity > order.length) {
			final int grown = Math.max(capacity, order.length + (order.length >> 1));
			// Stage every parent-owned lane before publishing any new storage.
			final GridItem[] nextOrder = Arrays.copyOf(order, grown);
			final int[] nextPrevious = Arrays.copyOf(previousOccurrence, grown);
			final int[] nextNext = Arrays.copyOf(nextOccurrence, grown);
			order = nextOrder;
			previousOccurrence = nextPrevious;
			nextOccurrence = nextNext;
		}
	}
}
