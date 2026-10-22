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
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;

/**
 * The selected items of a Grid in the viewport mode, in the order they were selected (as the
 * stock list keeps them): every item marks its own position, so membership, adding and removing
 * are O(1) and the paint asks {@code contains} per row at no cost of the selection's size. A
 * removed item leaves a hole that is closed when holes outnumber items or when a position other
 * than the first is read, so removals cost O(1) amortized.
 */
final class GridSelectionList extends AbstractList<GridItem> implements RandomAccess {
	private GridItem[] order = new GridItem[16];
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
		final int position = positionOf(o);
		if (position < 0) {
			return -1;
		}
		compact();
		return ((GridItem) o).selectionPosition();
	}

	@Override
	public int lastIndexOf(final Object o) {
		return indexOf(o);
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
		if (index != live) {
			// the Grid appends; an insertion elsewhere keeps the list semantics at O(n)
			compact();
			if (index < 0 || index > live) {
				throw new IndexOutOfBoundsException("index " + index + ", size " + live);
			}
			ensureCapacity(end + 1);
			System.arraycopy(order, index, order, index + 1, end - index);
			order[index] = item;
			end++;
			live++;
			for (int i = index; i < end; i++) {
				order[i].markSelectionPosition(i);
			}
			modCount++;
			return;
		}
		ensureCapacity(end + 1);
		order[end] = item;
		item.markSelectionPosition(end);
		end++;
		live++;
		modCount++;
	}

	@Override
	public boolean addAll(final Collection<? extends GridItem> items) {
		ensureCapacity(end + items.size());
		for (final GridItem item : items) {
			order[end] = item;
			item.markSelectionPosition(end);
			end++;
			live++;
		}
		modCount++;
		return !items.isEmpty();
	}

	@Override
	public boolean remove(final Object o) {
		final int position = positionOf(o);
		if (position < 0) {
			return false;
		}
		order[position] = null;
		((GridItem) o).markSelectionPosition(-1);
		live--;
		modCount++;
		if (live == 0) {
			head = end = 0;
		} else if (end - head > 2 * live + 16) {
			compact();
		}
		return true;
	}

	@Override
	public GridItem remove(final int index) {
		final GridItem item = get(index);
		remove(item);
		return item;
	}

	@Override
	public GridItem set(final int index, final GridItem item) {
		final GridItem old = get(index);
		compact();
		old.markSelectionPosition(-1);
		order[index] = item;
		item.markSelectionPosition(index);
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
		return new Iterator<GridItem>() {
			private int next = advance(head);
			private final int expectedModCount = modCount;

			@Override
			public boolean hasNext() {
				return next < end;
			}

			@Override
			public GridItem next() {
				if (modCount != expectedModCount) {
					throw new ConcurrentModificationException();
				}
				if (next >= end) {
					throw new NoSuchElementException();
				}
				final GridItem item = order[next];
				next = advance(next + 1);
				return item;
			}
		};
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

	/* closes the holes: every item moves to its index */
	private void compact() {
		if (end - head == live) {
			if (head > 0) {
				System.arraycopy(order, head, order, 0, live);
				Arrays.fill(order, live, end, null);
				for (int i = 0; i < live; i++) {
					order[i].markSelectionPosition(i);
				}
				head = 0;
				end = live;
			}
			return;
		}
		int to = 0;
		for (int from = head; from < end; from++) {
			final GridItem item = order[from];
			if (item != null) {
				order[to] = item;
				item.markSelectionPosition(to);
				to++;
			}
		}
		Arrays.fill(order, to, end, null);
		head = 0;
		end = to;
	}

	private void ensureCapacity(final int capacity) {
		if (capacity > order.length) {
			order = Arrays.copyOf(order, Math.max(capacity, order.length + (order.length >> 1)));
		}
	}
}
