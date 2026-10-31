/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.swing.viewport;

import java.beans.PropertyChangeListener;
import java.util.Arrays;

import javax.swing.event.EventListenerList;
import javax.swing.event.SwingPropertyChangeSupport;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.RowMapper;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.m3.swing.viewport.engine.residency.ItemBitSet;
import com.m3.swing.viewport.engine.residency.ItemLanes;

/**
 * {@link TreeSelectionModel} over one engine lane keyed by node key: ranges are RUN containers,
 * sparse picks ARRAY containers, dense pages BITMAP containers, and rules ("every k-th node") are
 * patterns. No {@code TreePath} is stored; paths are made when a caller asks
 * ({@code getSelectionPaths}, events, {@code getLeadSelectionPath}).
 *
 * <p>Behaviour follows {@code DefaultTreeSelectionModel}: modes, contiguity checks through the
 * {@link RowMapper}, which paths an event reports as added or removed, and the old and new lead.
 * Two things differ, and both are unspecified by {@code TreeSelectionModel}:</p>
 * <ul>
 * <li>{@code getSelectionPaths} returns key order, not insertion order;</li>
 * <li>after a removal the lead is the selected node with the highest key, where
 * {@code DefaultTreeSelectionModel} takes the last path in its hash table's order.</li>
 * </ul>
 *
 * <p>Bulk operations ({@link #selectKeyRange}, {@link #selectPattern}, and {@link #clearSelection}
 * over more than {@link #BULK_EVENT_LIMIT} nodes) fire one {@link M3TreeSelectionEvent} with no
 * paths; listeners re-read the model.</p>
 */
//FLLDCIM: L2 — selection as bit lanes and patterns; L4 — TreePath only when asked
public class M3TreeSelectionModel implements TreeSelectionModel {
	public static final int BULK_EVENT_LIMIT = 1 << 16;
	private static final int LANE = 0;

	/** Selection event of a bulk operation: no paths, re-read the model. */
	public static final class M3TreeSelectionEvent extends TreeSelectionEvent {
		private static final long serialVersionUID = 1L;
		private final int from, to;

		M3TreeSelectionEvent (Object source, TreePath oldLead, TreePath newLead, int from, int to) {
			super (source, new TreePath [0], new boolean [0], oldLead, newLead);
			this.from = from;
			this.to = to;
		}

		public boolean isBulk () {
			return true;
		}

		/** Key range the bulk operation touched, {@code [from, to)}. */
		public int keyFrom () {
			return from;
		}

		public int keyTo () {
			return to;
		}

		@Override
		public Object cloneWithSource (Object newSource) {
			return new M3TreeSelectionEvent (newSource, getOldLeadSelectionPath (), getNewLeadSelectionPath (), from, to);
		}
	}

	private final M3TreeModelBase model;
	private final ItemLanes lanes = new ItemLanes (1);
	private final ItemBitSet rowBits = new ItemBitSet ();
	private int space, mode = DISCONTIGUOUS_TREE_SELECTION, leadKey = -1, leadRow = -1, pathKey = -1;
	private TreePath leadPath;
	private RowMapper rowMapper;
	/** Mirrors DefaultTreeSelectionModel's "selection array exists" (null versus empty array). */
	private boolean selectionArray;
	protected final EventListenerList listenerList = new EventListenerList ();
	protected SwingPropertyChangeSupport changeSupport;
	private long pathsMaterialized;

	public M3TreeSelectionModel (M3TreeModelBase model) {
		this.model = model;
		model.addFreedKeyListener (this::keyFreed);
	}

	// ---- keys ----------------------------------------------------------------------------------

	private void ensure (int key) {
		if (key >= space) {
			space = Math.max (key + 1, model.keySpace ());
			lanes.setCount (space);
		}
	}

	private int keyOf (TreePath path) {
		if (path == null) return -1;
		int k = model.keyOf (path.getLastPathComponent ());
		if (k >= 0) ensure (k);
		return k;
	}

	public boolean isKeySelected (int key) {
		return key >= 0 && key < space && lanes.get (LANE, key);
	}

	public int leadKey () {
		return leadKey;
	}

	/** Paths this model created for callers since construction. */
	public long pathsMaterialized () {
		return pathsMaterialized;
	}

	private TreePath path (int key) {
		if (key < 0) return null;
		pathsMaterialized++;
		return model.pathOf (key);
	}

	private int selectedCount () {
		return space == 0 ? 0 : lanes.countRange (LANE, 0, space);
	}

	private int firstSelected () {
		return space == 0 ? -1 : lanes.nextSet (LANE, 0);
	}

	private int lastSelected () {
		int last = -1;
		for (int k = firstSelected (); k >= 0; k = lanes.nextSet (LANE, k + 1)) last = k;
		return last;
	}

	private int [] selectedKeys () {
		int [] keys = new int [selectedCount ()];
		int n = 0;
		for (int k = firstSelected (); k >= 0 && n < keys.length; k = lanes.nextSet (LANE, k + 1)) keys [n++] = k;
		return keys;
	}

	private void keyFreed (int key) {
		if (key < space && lanes.get (LANE, key)) lanes.set (LANE, key, false);
		if (key == leadKey) leadKey = -1;
		if (key == pathKey) {
			pathKey = -1;
			leadPath = null;
		}
	}

	// ---- mode and mapper -----------------------------------------------------------------------

	@Override
	public void setSelectionMode (int newMode) {
		int old = mode;
		mode = newMode != SINGLE_TREE_SELECTION && newMode != CONTIGUOUS_TREE_SELECTION && newMode != DISCONTIGUOUS_TREE_SELECTION
				? DISCONTIGUOUS_TREE_SELECTION : newMode;
		if (old != mode && changeSupport != null) changeSupport.firePropertyChange ("selectionMode", Integer.valueOf (old), Integer.valueOf (mode));
	}

	@Override
	public int getSelectionMode () {
		return mode;
	}

	@Override
	public void setRowMapper (RowMapper newMapper) {
		rowMapper = newMapper;
		resetRowSelection ();
	}

	@Override
	public RowMapper getRowMapper () {
		return rowMapper;
	}

	// ---- set, add, remove, clear ---------------------------------------------------------------

	@Override
	public void setSelectionPath (TreePath path) {
		setSelectionPaths (path == null ? null : new TreePath [] {path});
	}

	@Override
	public void setSelectionPaths (TreePath [] pPaths) {
		TreePath [] paths = pPaths;
		int newCount = paths == null ? 0 : paths.length, oldCount = selectedCount ();
		if (newCount + oldCount == 0) return;
		if (mode == SINGLE_TREE_SELECTION && newCount > 1) {
			paths = new TreePath [] {pPaths [0]};
			newCount = 1;
		} else if (mode == CONTIGUOUS_TREE_SELECTION && newCount > 0 && !arePathsContiguous (paths)) {
			paths = new TreePath [] {pPaths [0]};
			newCount = 1;
		}
		int oldLead = leadKey;
		int [] next = new int [newCount];
		int n = 0;
		ItemBitSet seen = new ItemBitSet ();
		int [] changed = new int [newCount + oldCount];
		boolean [] added = new boolean [newCount + oldCount];
		int c = 0;
		leadKey = -1;
		for (int i = 0; i < newCount; i++) {
			int k = keyOf (paths [i]);
			if (k < 0 || seen.get (k)) continue;
			seen.set (k, true);
			if (!lanes.get (LANE, k)) {
				changed [c] = k;
				added [c++] = true;
			}
			leadKey = k;
			next [n++] = k;
		}
		for (int k = firstSelected (); k >= 0; k = lanes.nextSet (LANE, k + 1)) {
			if (!seen.get (k)) changed [c++] = k;
		}
		if (space > 0) lanes.setRange (LANE, 0, space, false);
		for (int i = 0; i < n; i++) lanes.set (LANE, next [i], true);
		selectionArray = true;
		resetRowSelection ();
		if (c > 0) notify (changed, added, c, oldLead);
	}

	@Override
	public void addSelectionPath (TreePath path) {
		if (path != null) addSelectionPaths (new TreePath [] {path});
	}

	@Override
	public void addSelectionPaths (TreePath [] paths) {
		int length = paths == null ? 0 : paths.length;
		if (length == 0) return;
		if (mode == SINGLE_TREE_SELECTION) {
			setSelectionPaths (paths);
			return;
		}
		if (mode == CONTIGUOUS_TREE_SELECTION && !canPathsBeAdded (paths)) {
			setSelectionPaths (arePathsContiguous (paths) ? paths : new TreePath [] {paths [0]});
			return;
		}
		int oldLead = leadKey, lead = -1, c = 0;
		int [] changed = new int [length];
		boolean [] added = new boolean [length];
		for (int i = 0; i < length; i++) {
			int k = keyOf (paths [i]);
			if (paths [i] == null) continue;
			if (k >= 0 && !lanes.get (LANE, k)) {
				lanes.set (LANE, k, true);
				changed [c] = k;
				added [c++] = true;
			}
			if (k >= 0) lead = k;
		}
		if (c == 0) return;
		leadKey = lead < 0 ? oldLead : lead;
		selectionArray = true;
		resetRowSelection ();
		notify (changed, added, c, oldLead);
	}

	@Override
	public void removeSelectionPath (TreePath path) {
		if (path != null) removeSelectionPaths (new TreePath [] {path});
	}

	@Override
	public void removeSelectionPaths (TreePath [] paths) {
		if (paths == null || paths.length == 0 || selectedCount () == 0) return;
		if (!canPathsBeRemoved (paths)) {
			clearSelection ();
			return;
		}
		int oldLead = leadKey, c = 0;
		int [] changed = new int [paths.length];
		for (int i = paths.length - 1; i >= 0; i--) {
			int k = keyOf (paths [i]);
			if (k >= 0 && lanes.get (LANE, k)) {
				lanes.set (LANE, k, false);
				changed [c++] = k;
			}
		}
		if (c == 0) return;
		leadKey = lastSelected ();
		if (leadKey < 0) selectionArray = false;
		resetRowSelection ();
		notify (changed, new boolean [c], c, oldLead);
	}

	@Override
	public void clearSelection () {
		int count = selectedCount ();
		if (count == 0) return;
		int oldLead = leadKey;
		TreeSelectionEvent e;
		if (count > BULK_EVENT_LIMIT) {
			e = new M3TreeSelectionEvent (this, path (oldLead), null, 0, space);
		} else {
			int [] keys = selectedKeys ();
			TreePath [] p = new TreePath [keys.length];
			for (int i = 0; i < keys.length; i++) p [i] = path (keys [i]);
			e = new TreeSelectionEvent (this, p, new boolean [keys.length], path (oldLead), null);
		}
		leadKey = leadRow = -1;
		lanes.setRange (LANE, 0, space, false);
		selectionArray = false;
		resetRowSelection ();
		fireValueChanged (e);
	}

	// ---- bulk (M3) -----------------------------------------------------------------------------

	/** Selects keys {@code [from, to)} in one RUN; fires one bulk event. */
	public void selectKeyRange (int from, int to) {
		if (from >= to) return;
		ensure (to - 1);
		int oldLead = leadKey;
		lanes.setRange (LANE, from, to, true);
		selectionArray = true;
		resetRowSelection ();
		fireValueChanged (new M3TreeSelectionEvent (this, path (oldLead), leadPathOrNull (), from, to));
	}

	/** Selects by rule: key {@code i} in {@code [from, to)} when bit {@code (i - from) % stride} of {@code mask} is set. */
	public void selectPattern (int from, int to, int stride, int mask) {
		if (from >= to) return;
		ensure (to - 1);
		int oldLead = leadKey;
		lanes.setPattern (LANE, from, to, stride, mask);
		selectionArray = true;
		resetRowSelection ();
		fireValueChanged (new M3TreeSelectionEvent (this, path (oldLead), leadPathOrNull (), from, to));
	}

	/** Selected nodes in {@code [from, to)}: arithmetic over runs and patterns. */
	public int countKeys (int from, int to) {
		ensure (Math.max (0, to - 1));
		return lanes.countRange (LANE, from, Math.min (to, space));
	}

	// ---- reads ---------------------------------------------------------------------------------

	@Override
	public TreePath getSelectionPath () {
		int k = firstSelected ();
		return k < 0 ? null : path (k);
	}

	@Override
	public TreePath [] getSelectionPaths () {
		int [] keys = selectedKeys ();
		TreePath [] p = new TreePath [keys.length];
		for (int i = 0; i < keys.length; i++) p [i] = path (keys [i]);
		return p;
	}

	@Override
	public int getSelectionCount () {
		return selectedCount ();
	}

	@Override
	public boolean isPathSelected (TreePath path) {
		return isKeySelected (keyOf (path));
	}

	@Override
	public boolean isSelectionEmpty () {
		return selectedCount () == 0;
	}

	@Override
	public TreePath getLeadSelectionPath () {
		return leadPathOrNull ();
	}

	private TreePath leadPathOrNull () {
		if (leadKey < 0) return null;
		if (pathKey != leadKey || leadPath == null) {
			leadPath = path (leadKey);
			pathKey = leadKey;
		}
		return leadPath;
	}

	// ---- rows ----------------------------------------------------------------------------------

	private int rowOf (int key) {
		if (rowMapper instanceof M3TreeRows r) return r.rowOf (key);
		int [] rows = rowMapper.getRowsForPaths (new TreePath [] {model.pathOf (key)});
		return rows == null ? -1 : rows [0];
	}

	private int rowOfPath (TreePath p) {
		if (rowMapper instanceof M3TreeRows r) return r.getRowForPath (p);
		int [] rows = rowMapper.getRowsForPaths (new TreePath [] {p});
		return rows == null ? Integer.MIN_VALUE : rows [0];
	}

	@Override
	public boolean isRowSelected (int row) {
		if (rowMapper instanceof M3TreeRows r) return isKeySelected (r.keyAt (row));
		return row >= 0 && rowBits.get (row);
	}

	@Override
	public int [] getSelectionRows () {
		int count = selectedCount ();
		if (rowMapper == null || count == 0) return new int [0];
		int [] rows = new int [count];
		int n = 0;
		for (int k = firstSelected (); k >= 0; k = lanes.nextSet (LANE, k + 1)) {
			int r = rowOf (k);
			if (r != -1) rows [n++] = r;
		}
		return n == 0 ? null : n == count ? rows : Arrays.copyOf (rows, n);
	}

	@Override
	public int getMinSelectionRow () {
		if (rowMapper == null || selectedCount () == 0) return -1;
		if (!(rowMapper instanceof M3TreeRows)) return rowBits.next (0, Integer.MAX_VALUE);
		int min = -1;
		for (int k = firstSelected (); k >= 0; k = lanes.nextSet (LANE, k + 1)) {
			int r = rowOf (k);
			if (r >= 0 && (min < 0 || r < min)) min = r;
		}
		return min;
	}

	@Override
	public int getMaxSelectionRow () {
		if (rowMapper == null || selectedCount () == 0) return -1;
		int max = -1;
		if (!(rowMapper instanceof M3TreeRows)) {
			for (int r = rowBits.next (0, Integer.MAX_VALUE); r >= 0; r = rowBits.next (r + 1, Integer.MAX_VALUE)) max = r;
			return max;
		}
		for (int k = firstSelected (); k >= 0; k = lanes.nextSet (LANE, k + 1)) max = Math.max (max, rowOf (k));
		return max;
	}

	@Override
	public void resetRowSelection () {
		if (!rowBits.isEmpty ()) rowBits.setRange (0, Integer.MAX_VALUE, false);
		if (rowMapper == null || selectedCount () == 0) {
			leadRow = -1;
			return;
		}
		if (!(rowMapper instanceof M3TreeRows)) {
			for (int k = firstSelected (); k >= 0; k = lanes.nextSet (LANE, k + 1)) {
				int r = rowOf (k);
				if (r != -1) rowBits.set (r, true);
			}
		}
		leadRow = leadKey < 0 ? -1 : rowOf (leadKey);
		insureRowContinuity ();
	}

	@Override
	public int getLeadSelectionRow () {
		return leadRow;
	}

	/** {@code DefaultTreeSelectionModel.insureRowContinuity}: contiguous and single modes. */
	protected void insureRowContinuity () {
		if (mode == CONTIGUOUS_TREE_SELECTION && rowMapper != null && selectedCount () > 0) {
			int min = getMinSelectionRow ();
			if (min == -1) return;
			int max = getMaxSelectionRow ();
			for (int row = min; row <= max; row++) {
				if (isRowSelected (row)) continue;
				if (row == min) {
					clearSelection ();
				} else {
					TreePath [] keep = new TreePath [row - min];
					for (int k = firstSelected (); k >= 0; k = lanes.nextSet (LANE, k + 1)) {
						int r = rowOf (k);
						// Same failure as the JDK when an invisible path is selected (its row -1 indexes the array).
						if (r < 0) throw new ArrayIndexOutOfBoundsException ("Index " + (r - min) + " out of bounds for length " + keep.length);
						if (r < row) keep [r - min] = path (k);
					}
					setSelectionPaths (keep);
				}
				return;
			}
		} else if (mode == SINGLE_TREE_SELECTION && selectedCount () > 1) {
			setSelectionPath (getSelectionPath ());
		}
	}

	/** {@code DefaultTreeSelectionModel.arePathsContiguous}. */
	protected boolean arePathsContiguous (TreePath [] paths) {
		if (rowMapper == null || paths.length < 2) return true;
		ItemBitSet bits = new ItemBitSet ();
		int count = paths.length, valid = 0;
		int min = rowOfPath (paths [0]);
		for (TreePath p : paths) {
			if (p == null) continue;
			int row = rowOfPath (p);
			if (row == Integer.MIN_VALUE || row == -1 || row < min - count || row > min + count) return false;
			if (row < min) min = row;
			if (!bits.get (row)) {
				bits.set (row, true);
				valid++;
			}
		}
		for (int row = min; row < min + valid; row++) {
			// Same failure as the JDK (java.util.BitSet) when the first path is null or invisible.
			if (row < 0) throw new IndexOutOfBoundsException ("bitIndex < 0: " + row);
			if (!bits.get (row)) return false;
		}
		return true;
	}

	/** {@code DefaultTreeSelectionModel.canPathsBeAdded}. */
	protected boolean canPathsBeAdded (TreePath [] paths) {
		if (paths == null || paths.length == 0 || rowMapper == null || !selectionArray || mode == DISCONTIGUOUS_TREE_SELECTION) return true;
		ItemBitSet bits = new ItemBitSet ();
		int min = getMinSelectionRow (), max = getMaxSelectionRow ();
		if (min != -1) {
			for (int row = min; row <= max; row++) {
				if (isRowSelected (row)) bits.set (row, true);
			}
		} else {
			min = max = rowOfPath (paths [0]);
		}
		for (int i = paths.length - 1; i >= 0; i--) {
			if (paths [i] == null) continue;
			int row = rowOfPath (paths [i]);
			if (row == Integer.MIN_VALUE) return false;
			min = Math.min (row, min);
			max = Math.max (row, max);
			if (row == -1) return false;
			bits.set (row, true);
		}
		for (int row = min; row <= max; row++) {
			// Same failure as the JDK (java.util.BitSet) for a null first path over an empty selection.
			if (row < 0) throw new IndexOutOfBoundsException ("bitIndex < 0: " + row);
			if (!bits.get (row)) return false;
		}
		return true;
	}

	/** {@code DefaultTreeSelectionModel.canPathsBeRemoved}. */
	protected boolean canPathsBeRemoved (TreePath [] paths) {
		if (rowMapper == null || selectedCount () == 0 || mode == DISCONTIGUOUS_TREE_SELECTION) return true;
		ItemBitSet removing = new ItemBitSet (), bits = new ItemBitSet ();
		for (TreePath p : paths) {
			int k = keyOf (p);
			if (k >= 0) removing.set (k, true);
		}
		int min = -1, valid = 0;
		for (int k = firstSelected (); k >= 0; k = lanes.nextSet (LANE, k + 1)) {
			if (removing.get (k)) continue;
			int row = rowOf (k);
			if (row != -1 && !bits.get (row)) {
				valid++;
				min = min == -1 ? row : Math.min (min, row);
				bits.set (row, true);
			}
		}
		if (valid > 1) {
			for (int row = min + valid - 1; row >= min; row--) {
				if (!bits.get (row)) return false;
			}
		}
		return true;
	}

	// ---- listeners -----------------------------------------------------------------------------

	private void notify (int [] keys, boolean [] added, int n, int oldLead) {
		TreePath [] p = new TreePath [n];
		for (int i = 0; i < n; i++) p [i] = path (keys [i]);
		fireValueChanged (new TreeSelectionEvent (this, p, Arrays.copyOf (added, n), path (oldLead), leadPathOrNull ()));
	}

	@Override
	public void addTreeSelectionListener (TreeSelectionListener l) {
		listenerList.add (TreeSelectionListener.class, l);
	}

	@Override
	public void removeTreeSelectionListener (TreeSelectionListener l) {
		listenerList.remove (TreeSelectionListener.class, l);
	}

	public TreeSelectionListener [] getTreeSelectionListeners () {
		return listenerList.getListeners (TreeSelectionListener.class);
	}

	protected void fireValueChanged (TreeSelectionEvent e) {
		Object [] ls = listenerList.getListenerList ();
		for (int i = ls.length - 2; i >= 0; i -= 2) {
			if (ls [i] == TreeSelectionListener.class) ((TreeSelectionListener) ls [i + 1]).valueChanged (e);
		}
	}

	@Override
	public synchronized void addPropertyChangeListener (PropertyChangeListener listener) {
		if (changeSupport == null) changeSupport = new SwingPropertyChangeSupport (this);
		changeSupport.addPropertyChangeListener (listener);
	}

	@Override
	public synchronized void removePropertyChangeListener (PropertyChangeListener listener) {
		if (changeSupport != null) changeSupport.removePropertyChangeListener (listener);
	}
}
