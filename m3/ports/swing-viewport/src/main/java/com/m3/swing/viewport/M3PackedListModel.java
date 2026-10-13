/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.swing.viewport;

import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import javax.swing.AbstractListModel;
import javax.swing.event.ListDataEvent;

import com.m3.swing.viewport.engine.PackedHeapGrowth;

/**
 * {@link javax.swing.ListModel} with the {@link javax.swing.DefaultListModel} mutation API over one
 * {@code Object[]} grown through {@link PackedHeapGrowth} (no {@code Vector}, no per-call lock).
 * Elements are the caller's own objects: {@code getElementAt} returns the identical object that was
 * stored. Events are the ones {@code DefaultListModel} fires for the same call (type, index0,
 * index1), which the differential test checks.
 *
 * <p><b>Batches.</b> Between {@link #beginBatch()} and {@link #endBatch()} events are coalesced
 * only where the merged event is equivalent: an add adjacent to or inside the pending added interval
 * extends it, and a contents change extends the pending change to the hull. Anything else flushes the
 * pending event first. Listeners therefore see the same final state and the same index shifts with
 * fewer events (the debounce rule for data storms).</p>
 *
 * <p>Not thread-safe; like every Swing model it belongs to the event dispatch thread.</p>
 */
//FLLDCIM: L2 — data in an Xmx-aware dynamic array; objects are the caller's
public class M3PackedListModel<E> extends AbstractListModel<E> implements M3PackedCells {
	private static final long serialVersionUID = 1L;
	private static final int NONE = -1;

	private Object [] items = new Object [0];
	private int size;
	private int batchDepth, pendingType = NONE, pending0, pending1;

	public M3PackedListModel () {
	}

	// ---- ListModel -------------------------------------------------------------------------------

	@Override
	public int getSize () {
		return size;
	}

	@Override
	public E getElementAt (int index) {
		return get (index);
	}

	/** Paint read: a {@code String} element is copied without allocation; others use {@code toString}. */
	@Override
	public int cellChars (int row, int column, char [] dst) {
		Object e = items [row];
		return e == null ? 0 : M3Chars.copy (e instanceof String s ? s : e.toString (), dst);
	}

	// ---- DefaultListModel read API ---------------------------------------------------------------

	public int size () {
		return size;
	}

	public boolean isEmpty () {
		return size == 0;
	}

	public int capacity () {
		return items.length;
	}

	@SuppressWarnings ("unchecked")
	public E get (int index) {
		check (index, size);
		return (E) items [index];
	}

	public E elementAt (int index) {
		return get (index);
	}

	public E firstElement () {
		if (size == 0) throw new NoSuchElementException ();
		return get (0);
	}

	public E lastElement () {
		if (size == 0) throw new NoSuchElementException ();
		return get (size - 1);
	}

	public boolean contains (Object elem) {
		return indexOf (elem, 0) >= 0;
	}

	public int indexOf (Object elem) {
		return indexOf (elem, 0);
	}

	public int indexOf (Object elem, int index) {
		for (int i = Math.max (0, index); i < size; i++) {
			if (elem == null ? items [i] == null : elem.equals (items [i])) return i;
		}
		return -1;
	}

	public int lastIndexOf (Object elem) {
		return lastIndexOf (elem, size - 1);
	}

	public int lastIndexOf (Object elem, int index) {
		if (index >= size) throw new IndexOutOfBoundsException (index + " >= " + size);
		for (int i = index; i >= 0; i--) {
			if (elem == null ? items [i] == null : elem.equals (items [i])) return i;
		}
		return -1;
	}

	public void copyInto (Object [] anArray) {
		System.arraycopy (items, 0, anArray, 0, size);
	}

	public Object [] toArray () {
		return Arrays.copyOf (items, size);
	}

	public Enumeration<E> elements () {
		return new Enumeration<E> () {
			int next;

			@Override
			public boolean hasMoreElements () {
				return next < size;
			}

			@Override
			public E nextElement () {
				if (next >= size) throw new NoSuchElementException ();
				return get (next++);
			}
		};
	}

	@Override
	public String toString () {
		StringBuilder b = new StringBuilder ("[");
		for (int i = 0; i < size; i++) {
			if (i > 0) b.append (", ");
			b.append (items [i] == this ? "(this Collection)" : String.valueOf (items [i]));
		}
		return b.append (']').toString ();
	}

	// ---- DefaultListModel mutation API -----------------------------------------------------------

	public void trimToSize () {
		if (items.length != size) items = Arrays.copyOf (items, size);
	}

	public void ensureCapacity (int minCapacity) {
		if (minCapacity > items.length) items = Arrays.copyOf (items, PackedHeapGrowth.capacity (items.length, minCapacity, 4));
	}

	public void setSize (int newSize) {
		if (newSize < 0) throw new ArrayIndexOutOfBoundsException (newSize);
		int oldSize = size;
		if (newSize > items.length) ensureCapacity (newSize);
		if (newSize < oldSize) Arrays.fill (items, newSize, oldSize, null);
		size = newSize;
		if (oldSize > newSize) fire (ListDataEvent.INTERVAL_REMOVED, newSize, oldSize - 1);
		else if (oldSize < newSize) fire (ListDataEvent.INTERVAL_ADDED, oldSize, newSize - 1);
	}

	public void setElementAt (E element, int index) {
		set (index, element);
	}

	public E set (int index, E element) {
		E old = get (index);
		items [index] = element;
		fire (ListDataEvent.CONTENTS_CHANGED, index, index);
		return old;
	}

	public void addElement (E element) {
		add (size, element);
	}

	public void insertElementAt (E element, int index) {
		add (index, element);
	}

	public void add (int index, E element) {
		if (index < 0 || index > size) throw new ArrayIndexOutOfBoundsException (index + " > " + size);
		open (index, 1);
		items [index] = element;
		fire (ListDataEvent.INTERVAL_ADDED, index, index);
	}

	public void addAll (Collection<? extends E> c) {
		if (c.isEmpty ()) return;
		addAll (size, c);
	}

	public void addAll (int index, Collection<? extends E> c) {
		if (index < 0 || index > size) throw new ArrayIndexOutOfBoundsException ("index out of range: " + index);
		if (c.isEmpty ()) return;
		Object [] source = c.toArray ();
		open (index, source.length);
		System.arraycopy (source, 0, items, index, source.length);
		fire (ListDataEvent.INTERVAL_ADDED, index, index + source.length - 1);
	}

	public void removeElementAt (int index) {
		remove (index);
	}

	public E remove (int index) {
		E old = get (index);
		close (index, 1);
		fire (ListDataEvent.INTERVAL_REMOVED, index, index);
		return old;
	}

	public boolean removeElement (Object obj) {
		int index = indexOf (obj);
		if (index < 0) return false;
		remove (index);
		return true;
	}

	public void removeRange (int fromIndex, int toIndex) {
		if (fromIndex > toIndex) throw new IllegalArgumentException ("fromIndex must be <= toIndex");
		check (fromIndex, size);
		check (toIndex, size);
		close (fromIndex, toIndex - fromIndex + 1);
		fire (ListDataEvent.INTERVAL_REMOVED, fromIndex, toIndex);
	}

	public void removeAllElements () {
		clear ();
	}

	public void clear () {
		int last = size - 1;
		Arrays.fill (items, 0, size, null);
		size = 0;
		if (last >= 0) fire (ListDataEvent.INTERVAL_REMOVED, 0, last);
	}

	// ---- batches ---------------------------------------------------------------------------------

	/** Starts (or nests) a batch; events are coalesced where the merge is exact. */
	public void beginBatch () {
		batchDepth++;
	}

	/** Ends one nesting level; the outermost end fires the pending event. */
	public void endBatch () {
		if (batchDepth == 0) throw new IllegalStateException ("endBatch without beginBatch");
		if (--batchDepth == 0) flush ();
	}

	// ---- internals -------------------------------------------------------------------------------

	private static void check (int index, int size) {
		if (index < 0 || index >= size) throw new ArrayIndexOutOfBoundsException (index + " >= " + size);
	}

	private void open (int index, int count) {
		if (size + count > items.length) {
			items = Arrays.copyOf (items, PackedHeapGrowth.capacity (items.length, size + count, 4));
		}
		System.arraycopy (items, index, items, index + count, size - index);
		size += count;
	}

	private void close (int index, int count) {
		System.arraycopy (items, index + count, items, index, size - index - count);
		Arrays.fill (items, size - count, size, null);
		size -= count;
	}

	private void fire (int type, int index0, int index1) {
		if (batchDepth == 0) {
			emit (type, index0, index1);
			return;
		}
		if (pendingType == type && type == ListDataEvent.INTERVAL_ADDED && index0 >= pending0 && index0 <= pending1 + 1) {
			pending1 += index1 - index0 + 1;
			return;
		}
		if (pendingType == type && type == ListDataEvent.CONTENTS_CHANGED) {
			pending0 = Math.min (pending0, index0);
			pending1 = Math.max (pending1, index1);
			return;
		}
		flush ();
		pendingType = type;
		pending0 = index0;
		pending1 = index1;
	}

	private void flush () {
		if (pendingType == NONE) return;
		int type = pendingType;
		pendingType = NONE;
		emit (type, pending0, pending1);
	}

	private void emit (int type, int index0, int index1) {
		if (type == ListDataEvent.INTERVAL_ADDED) fireIntervalAdded (this, index0, index1);
		else if (type == ListDataEvent.INTERVAL_REMOVED) fireIntervalRemoved (this, index0, index1);
		else fireContentsChanged (this, index0, index1);
	}
}
