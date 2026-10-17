// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.framework;

import com.m3.collections.M3Collections;
import com.m3.collections.M3LongArrayList;
import com.m3.collections.M3ObjectStateSlots28;
import com.m3.collections.M3SlotAllocator28;
import java.util.List;
import java.util.Objects;

/**
 * File-local Eclipse reference projection: Eclipse retains payload/lifecycle ownership while
 * the existing M3 slot allocator owns generation-safe handles and eight-bit state masks.
 *
 * <p>The returned references are not cloned: extension points can become invalid, bundles
 * can change state, and resource deltas can become obsolete after a snapshot was built.
 * This single-threaded builder becomes read-only after construction; no native pointer,
 * JNI global reference, Entry object, or replacement Eclipse service is created.</p>
 */
final class M3EclipseIndexedSnapshot<T> {
    private final List<T> payloads = M3Collections.arrayList();
    private final M3LongArrayList handles = M3Collections.longArrayList();
    private final M3ObjectStateSlots28 states = new M3ObjectStateSlots28();

    /** The physical slot equals the appended ordinal because snapshot slots are never reused. */
    long add(T object, int flags) {
        Objects.requireNonNull(object, "object");
        if ((flags & ~0xff) != 0) throw new IllegalArgumentException("state must fit byte");
        long handle = states.allocate(flags);
        int slot = M3SlotAllocator28.slot(handle);
        if (slot != payloads.size()) {
            states.release(handle);
            throw new IllegalStateException("M3 snapshot handle and reference planes diverged");
        }
        try {
            payloads.add(object);
            handles.add(handle);
        } catch (RuntimeException | Error failure) {
            // Construction is all-or-nothing to external readers: no partial snapshot escapes.
            if (payloads.size() > slot) payloads.remove(slot);
            states.release(handle);
            throw failure;
        }
        return handle;
    }

    int size() { return payloads.size(); }

    T at(int ordinal) { return payloads.get(ordinal); }

    int state(int ordinal) { return states.state(handles.get(ordinal)); }

    List<T> selected(int requiredMask, int excludedMask) {
        long[] source = handles.toArray();
        long[] result = states.snapshotHandles(source, 0, source.length,
                requiredMask, excludedMask);
        List<T> selected = M3Collections.arrayList();
        for (long handle : result) {
            selected.add(payloads.get(M3SlotAllocator28.slot(handle)));
        }
        return List.copyOf(selected);
    }

    List<T> all() { return List.copyOf(payloads); }

    T atHandle(long handle) {
        if (!states.isLive(handle)) {
            throw new IllegalStateException("stale M3 Eclipse snapshot handle");
        }
        return payloads.get(M3SlotAllocator28.slot(handle));
    }
}
