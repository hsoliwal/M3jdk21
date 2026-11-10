/* *****************************************************************************
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
/*
 * Derived by recipe (m3/docs/viewport-everywhere/recipe) from hsoliwal/nebula 79fdab533b25bd228e96a70feed55c905ab9aefb,
 * GridCellSelectionList.java; Apache-2.0 elected from its Apache-2.0 OR EPL-2.0 publication
 * (m3/docs/viewport-everywhere/LICENSE_ELECTION.tsv).
 */
package jdk.internal.m3.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * The selected cells of a Grid in the viewport mode: the stock list (same order, same entries)
 * plus a count per cell value, so {@code contains} (asked for every painted cell and for every
 * cell a range selection adds) is O(1) instead of a scan of the selection. The list keeps its
 * own points: added points are copied and {@link #toArray(Object[])} hands out copies, so no
 * caller can change a cell behind the index; the Grid that shifts cells in place calls
 * {@link #reindex()}.
 */
public final class CellSelectionCounts extends ArrayList<CellPoint> {
	private static final long serialVersionUID = 1L;

	private final transient Map<Long, int[]> counts = new HashMap<>();

	public CellSelectionCounts () {
	}

	private static Long key(final Object o) {
		final CellPoint p = (CellPoint) o;
		return Long.valueOf(((long) p.x << 32) ^ (p.y & 0xFFFFFFFFL));
	}

	private void count(final CellPoint p, final int delta) {
		final Long key = key(p);
		final int[] count = counts.get(key);
		if (count == null) {
			if (delta > 0) {
				counts.put(key, new int[] { delta });
			}
		} else if ((count[0] += delta) <= 0) {
			counts.remove(key);
		}
	}

	/** Rebuilds the counts after the Grid changed cells in place. */
	public void reindex() {
		counts.clear();
		for (final CellPoint p : this) {
			count(p, 1);
		}
	}

	@Override
	public boolean contains(final Object o) {
		return o instanceof CellPoint && counts.containsKey(key(o));
	}

	@Override
	public boolean containsAll(final Collection<?> c) {
		for (final Object o : c) {
			if (!contains(o)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean add(final CellPoint p) {
		final CellPoint copy = new CellPoint(p.x, p.y);
		count(copy, 1);
		return super.add(copy);
	}

	@Override
	public void add(final int index, final CellPoint p) {
		final CellPoint copy = new CellPoint(p.x, p.y);
		super.add(index, copy);
		count(copy, 1);
	}

	@Override
	public boolean addAll(final Collection<? extends CellPoint> c) {
		ensureCapacity(size() + c.size());
		for (final CellPoint p : c) {
			add(p);
		}
		return !c.isEmpty();
	}

	@Override
	public boolean addAll(final int index, final Collection<? extends CellPoint> c) {
		int at = index;
		for (final CellPoint p : c) {
			add(at++, p);
		}
		return !c.isEmpty();
	}

	@Override
	public CellPoint set(final int index, final CellPoint p) {
		final CellPoint copy = new CellPoint(p.x, p.y);
		final CellPoint old = super.set(index, copy);
		count(old, -1);
		count(copy, 1);
		return old;
	}

	@Override
	public CellPoint remove(final int index) {
		final CellPoint old = super.remove(index);
		count(old, -1);
		return old;
	}

	@Override
	public boolean remove(final Object o) {
		if (!contains(o)) {
			return false;
		}
		final int index = indexOf(o);
		remove(index);
		return true;
	}

	@Override
	public boolean removeAll(final Collection<?> c) {
		if (c.isEmpty() || isEmpty()) {
			return false;
		}
		final CellSelectionCounts doomed = new CellSelectionCounts();
		for (final Object o : c) {
			if (o instanceof CellPoint) {
				doomed.count((CellPoint) o, 1);
			}
		}
		return removeIf(doomed::contains);
	}

	@Override
	public boolean retainAll(final Collection<?> c) {
		final CellSelectionCounts kept = new CellSelectionCounts();
		for (final Object o : c) {
			if (o instanceof CellPoint) {
				kept.count((CellPoint) o, 1);
			}
		}
		return removeIf(p -> !kept.contains(p));
	}

	@Override
	public boolean removeIf(final Predicate<? super CellPoint> filter) {
		final boolean removed = super.removeIf(filter);
		if (removed) {
			reindex();
		}
		return removed;
	}

	@Override
	public void clear() {
		super.clear();
		counts.clear();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T[] toArray(final T[] a) {
		final T[] array = super.toArray(a);
		for (int i = 0; i < size(); i++) {
			final CellPoint p = (CellPoint) array[i];
			array[i] = (T) new CellPoint(p.x, p.y);
		}
		return array;
	}
}
