/*
 * Copyright (c) 2026, Hitesh Soliwal (Synexia) and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package sun.swing.table;

import sun.security.action.GetBooleanAction;

/**
 * The view-to-model and model-to-view order of a {@code DefaultRowSorter} as two int lanes
 * ({@code -Dm3.swing.viewport=true}, off by default). Stock keeps one {@code Row} object per visible
 * row (a reference to the sorter and the model index, sorted through {@code Comparable}) plus a
 * reference lane and the int lane: 32 bytes per row at 10^6 rows. Here the view order is the int
 * lane itself, 8 bytes per row with the model-to-view lane, and the sort compares model indices
 * through the sorter's own {@code compare}. Every answer is the stock answer: the sorter's order is
 * a strict total order (ties fall back to the model index), so any comparison sort gives the same
 * permutation, and the insert, delete and update paths are the stock paths with {@code Row}
 * replaced by its model index. The one difference is the sequence of {@code compare} calls, which
 * only a comparator with side effects or one that breaks its contract can observe.
 * <p>
 * The sorter binds a {@link Host} that gives the lanes its comparison, filter, keys, flags and
 * event; the public {@code sort()} runs through the host so that an override still applies. Design
 * and proof: m3/docs/swing-viewport/DESIGN.md section 14.
 */
//FLLDCIM: M3 swing-viewport leaf 7 - DefaultRowSorter order as int lanes, no Row objects
public final class M3SorterLanes {

    /** Whether {@code DefaultRowSorter} keeps its order here; read once from {@code m3.swing.viewport}. */
    public static final boolean ENABLED = GetBooleanAction.privilegedGetProperty("m3.swing.viewport");

    /** What the lanes need from their sorter. Every method maps to one stock private member. */
    public interface Host {
        /** {@code DefaultRowSorter.compare}: the sort keys' order, ties by model index. */
        int compare(int model1, int model2);

        /** {@code DefaultRowSorter.include}: whether the filter shows the row. */
        boolean include(int modelRow);

        /** {@code getModelWrapper().getRowCount()}. */
        int modelRowCount();

        /** {@code isUnsorted()}: no keys, or the first key UNSORTED. */
        boolean unsorted();

        /** {@code getRowFilter() != null}. */
        boolean filtered();

        /** The {@code sorted} flag. */
        boolean sorted();

        /** Sets the {@code sorted} flag. */
        void sorted(boolean sorted);

        /** The start of the stock {@code sort()}: {@code updateUseToString}, then the cached keys. */
        void prepareSort();

        /** The start of the stock {@code sortExistingData()}: {@code updateUseToString}, {@code cacheSortKeys}. */
        void prepareResort();

        /** The public, overridable {@code sort()}. */
        void sort();

        /** {@code fireRowSorterChanged(lastViewToModel)}. */
        void changed(int[] lastViewToModel);
    }

    private static final int[] NONE = new int[0];

    private Host host;
    /** View index to model index; null while the view is neither sorted nor filtered. */
    private int[] viewToModel;
    /** Model index to view index, -1 for a filtered row; null with {@link #viewToModel}. */
    private int[] modelToView;

    /** Creates unbound lanes; the sorter binds its host before the first use. */
    public M3SorterLanes() {
    }

    /** Whether {@link #bind} ran. */
    public boolean bound() {
        return host != null;
    }

    /** Binds the sorter's callbacks. */
    public void bind(Host host) {
        this.host = host;
    }

    /** Whether the view is sorted or filtered (the stock {@code isTransformed}). */
    public boolean transformed() {
        return viewToModel != null;
    }

    /** The stock {@code getViewRowCount} while transformed. */
    public int viewRowCount() {
        return viewToModel.length;
    }

    /** The stock {@code convertRowIndexToModel} while transformed. */
    public int modelIndex(int viewIndex) {
        return viewToModel[viewIndex];
    }

    /** The stock {@code convertRowIndexToView} while transformed. */
    public int viewIndex(int modelIndex) {
        return modelToView[modelIndex];
    }

    /** The stock {@code allChanged}: forgets the order (the sorter then sorts again). */
    public void clear() {
        viewToModel = null;
        modelToView = null;
    }

    /** The model-index lane as the stock {@code getViewToModelAsInts} snapshots it, for tests. */
    public int[] snapshot() {
        return viewToModel != null ? viewToModel.clone() : NONE;
    }

    /** The stock {@code setSortKeys} tail: a first sort through the host, or a re-sort of the data. */
    public void sortKeysChanged() {
        if (viewToModel == null) {
            host.sort();
        } else {
            sortExistingData();
        }
    }

    /** The stock {@code sort()}. */
    public void sort() {
        host.sorted(true);
        int[] last = snapshot();
        host.prepareSort();
        if (host.unsorted()) {
            if (!host.filtered()) {
                if (viewToModel != null) {
                    viewToModel = null;
                    modelToView = null;
                } else {
                    return;
                }
            } else {
                initializeFilteredMapping();
            }
        } else {
            if (host.filtered()) {
                initializeFilteredMapping();
            } else {
                int n = host.modelRowCount();
                createModelToView(n);
                createViewToModel(n);
            }
            sortLane(viewToModel, viewToModel.length);
            setModelToViewFromViewToModel(false);
        }
        host.changed(last);
    }

    /** The stock {@code sortExistingData()}. */
    private void sortExistingData() {
        int[] last = snapshot();
        host.prepareResort();
        if (host.unsorted()) {
            if (!host.filtered()) {
                viewToModel = null;
                modelToView = null;
            } else {
                int included = 0;
                for (int i = 0; i < modelToView.length; i++) {
                    if (modelToView[i] != -1) {
                        viewToModel[included] = i;
                        modelToView[i] = included++;
                    }
                }
            }
        } else {
            sortLane(viewToModel, viewToModel.length);
            setModelToViewFromViewToModel(false);
        }
        host.changed(last);
    }

    private void initializeFilteredMapping() {
        int rowCount = host.modelRowCount();
        int excluded = 0;
        createModelToView(rowCount);
        for (int i = 0; i < rowCount; i++) {
            if (host.include(i)) {
                modelToView[i] = i - excluded;
            } else {
                modelToView[i] = -1;
                excluded++;
            }
        }
        createViewToModel(rowCount - excluded);
        for (int i = 0, j = 0; i < rowCount; i++) {
            if (modelToView[i] != -1) {
                viewToModel[j++] = i;
            }
        }
    }

    private void createModelToView(int rowCount) {
        if (modelToView == null || modelToView.length != rowCount) {
            modelToView = new int[rowCount];
        }
    }

    /** Rows {@code 0 .. rowCount - 1} in model order (what the stock Row objects hold after it). */
    private void createViewToModel(int rowCount) {
        if (viewToModel == null || viewToModel.length != rowCount) {
            viewToModel = new int[rowCount];
        }
        for (int i = 0; i < rowCount; i++) {
            viewToModel[i] = i;
        }
    }

    private void setModelToViewFromViewToModel(boolean unsetFirst) {
        if (unsetFirst) {
            java.util.Arrays.fill(modelToView, -1);
        }
        for (int i = viewToModel.length - 1; i >= 0; i--) {
            modelToView[viewToModel[i]] = i;
        }
    }

    // ---- the stock table change paths ------------------------------------------------------

    /** The stock {@code rowsInserted} tail. */
    public void rowsInserted(int firstRow, int lastRow) {
        if (shouldOptimizeChange(firstRow, lastRow)) {
            rowsInserted0(firstRow, lastRow);
        }
    }

    /** The stock {@code rowsDeleted} tail. */
    public void rowsDeleted(int firstRow, int lastRow) {
        if (shouldOptimizeChange(firstRow, lastRow)) {
            rowsDeleted0(firstRow, lastRow);
        }
    }

    /** The stock {@code rowsUpdated} tail. */
    public void rowsUpdated(int firstRow, int lastRow, boolean sortsOnUpdates) {
        if (sortsOnUpdates) {
            if (shouldOptimizeChange(firstRow, lastRow)) {
                rowsUpdated0(firstRow, lastRow);
            }
        } else {
            host.sorted(false);
        }
    }

    private boolean shouldOptimizeChange(int firstRow, int lastRow) {
        if (viewToModel == null) {
            return false;
        }
        if (!host.sorted() || viewToModel.length == 0 || (lastRow - firstRow) > viewToModel.length / 10) {
            host.sort();
            return false;
        }
        return true;
    }

    private void rowsInserted0(int firstRow, int lastRow) {
        int[] old = snapshot();
        int delta = lastRow - firstRow + 1;
        int[] added = new int[delta];
        int count = 0;
        for (int i = firstRow; i <= lastRow; i++) {
            if (host.include(i)) {
                added[count++] = i;
            }
        }
        for (int i = modelToView.length - 1; i >= firstRow; i--) {
            int viewIndex = modelToView[i];
            if (viewIndex != -1) {
                viewToModel[viewIndex] += delta;
            }
        }
        if (count > 0) {
            sortLane(added, count);
            int[] current = viewToModel;
            viewToModel = new int[current.length + count];
            insertInOrder(added, count, current);
        }
        createModelToView(host.modelRowCount());
        setModelToViewFromViewToModel(true);
        host.changed(old);
    }

    private void rowsDeleted0(int firstRow, int lastRow) {
        int[] old = snapshot();
        int removedFromView = 0;
        for (int i = firstRow; i <= lastRow; i++) {
            int viewIndex = modelToView[i];
            if (viewIndex != -1) {
                removedFromView++;
                viewToModel[viewIndex] = -1;
            }
        }
        int delta = lastRow - firstRow + 1;
        for (int i = modelToView.length - 1; i > lastRow; i--) {
            int viewIndex = modelToView[i];
            if (viewIndex != -1) {
                viewToModel[viewIndex] -= delta;
            }
        }
        if (removedFromView > 0) {
            int[] kept = new int[viewToModel.length - removedFromView];
            for (int i = 0, j = 0; i < viewToModel.length; i++) {
                if (viewToModel[i] != -1) {
                    kept[j++] = viewToModel[i];
                }
            }
            viewToModel = kept;
        }
        createModelToView(host.modelRowCount());
        setModelToViewFromViewToModel(true);
        host.changed(old);
    }

    private void rowsUpdated0(int firstRow, int lastRow) {
        int[] old = snapshot();
        int delta = lastRow - firstRow + 1;
        if (!host.filtered()) {
            int[] updated = new int[delta];
            for (int j = 0, i = firstRow; i <= lastRow; i++, j++) {
                updated[j] = viewToModel[modelToView[i]];
            }
            sortLane(updated, delta);
            int[] intermediary = new int[viewToModel.length - delta];
            for (int i = 0, j = 0; i < viewToModel.length; i++) {
                int modelIndex = viewToModel[i];
                if (modelIndex < firstRow || modelIndex > lastRow) {
                    intermediary[j++] = modelIndex;
                }
            }
            insertInOrder(updated, delta, intermediary);
            setModelToViewFromViewToModel(false);
        } else {
            int[] updated = new int[delta];
            int count = 0;
            int newlyVisible = 0;
            int newlyHidden = 0;
            int effected = 0;
            for (int i = firstRow; i <= lastRow; i++) {
                if (modelToView[i] == -1) {
                    if (host.include(i)) {
                        updated[count++] = i;
                        newlyVisible++;
                    }
                } else {
                    if (!host.include(i)) {
                        newlyHidden++;
                    } else {
                        updated[count++] = viewToModel[modelToView[i]];
                    }
                    modelToView[i] = -2;
                    effected++;
                }
            }
            sortLane(updated, count);
            int[] intermediary = new int[viewToModel.length - effected];
            for (int i = 0, j = 0; i < viewToModel.length; i++) {
                int modelIndex = viewToModel[i];
                if (modelToView[modelIndex] != -2) {
                    intermediary[j++] = modelIndex;
                }
            }
            if (newlyVisible != newlyHidden) {
                viewToModel = new int[viewToModel.length + newlyVisible - newlyHidden];
            }
            insertInOrder(updated, count, intermediary);
            setModelToViewFromViewToModel(true);
        }
        host.changed(old);
    }

    /** The stock {@code insertInOrder}: merges the sorted {@code toAdd} into {@code current}, into the lane. */
    private void insertInOrder(int[] toAdd, int count, int[] current) {
        int last = 0;
        for (int i = 0; i < count; i++) {
            int index = binarySearch(current, toAdd[i]);
            if (index < 0) {
                index = -1 - index;
            }
            System.arraycopy(current, last, viewToModel, last + i, index - last);
            viewToModel[index + i] = toAdd[i];
            last = index;
        }
        System.arraycopy(current, last, viewToModel, last + count, current.length - last);
    }

    /**
     * {@code Arrays.binarySearch} over model indices with the host's order. The key is never in
     * {@code a} (the order is strict and the callers removed it first), so the found case is left
     * out; the insertion point is the same.
     */
    private int binarySearch(int[] a, int key) {
        int low = 0;
        int high = a.length - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (host.compare(a[mid], key) < 0) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return -(low + 1);
    }

    // ---- sorting an int lane with the host's order -----------------------------------------------

    /**
     * Sorts {@code a[0 .. n)} by the host's order: a merge sort that skips a merge whose halves are
     * already in order, so an ordered lane costs n - 1 comparisons, as the stock TimSort does.
     * Public for the unit test.
     */
    public void sortLane(int[] a, int n) {
        if (n > 1) {
            mergeSort(a, new int[n], 0, n);
        }
    }

    private void mergeSort(int[] a, int[] tmp, int lo, int hi) {
        if (hi - lo <= 16) {
            for (int i = lo + 1; i < hi; i++) {
                int v = a[i];
                int j = i - 1;
                while (j >= lo && host.compare(a[j], v) > 0) {
                    a[j + 1] = a[j];
                    j--;
                }
                a[j + 1] = v;
            }
            return;
        }
        int mid = (lo + hi) >>> 1;
        mergeSort(a, tmp, lo, mid);
        mergeSort(a, tmp, mid, hi);
        if (host.compare(a[mid - 1], a[mid]) <= 0) {
            return;
        }
        System.arraycopy(a, lo, tmp, lo, mid - lo);
        int i = lo;
        int j = mid;
        int k = lo;
        while (i < mid && j < hi) {
            a[k++] = host.compare(a[j], tmp[i]) < 0 ? a[j++] : tmp[i++];
        }
        while (i < mid) {
            a[k++] = tmp[i++];
        }
    }
}
