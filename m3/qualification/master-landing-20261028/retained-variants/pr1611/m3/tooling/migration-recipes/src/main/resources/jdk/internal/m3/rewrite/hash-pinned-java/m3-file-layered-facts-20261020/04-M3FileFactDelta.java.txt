// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Arrays;
import java.util.Objects;

/**
 * Sorted compact delta over two M3FileFacts tables.
 *
 * <p>State is one byte per changed name; unchanged rows are omitted. Merge construction is linear
 * because both inputs are already sorted.</p>
 */
public final class M3FileFactDelta {
    public static final byte ADDED = 1;
    public static final byte REMOVED = 2;
    public static final byte CHANGED = 3;

    private static final M3FileFactDelta EMPTY =
            new M3FileFactDelta(new String[0], new byte[0]);

    private final String[] names;
    private final byte[] states;

    private M3FileFactDelta(String[] names, byte[] states) {
        this.names = names;
        this.states = states;
    }

    static M3FileFactDelta between(M3FileFacts before, M3FileFacts after) {
        int left = 0;
        int right = 0;
        String[] names = new String[Math.addExact(before.size(), after.size())];
        byte[] states = new byte[names.length];
        int count = 0;

        while (left < before.size() || right < after.size()) {
            if (left == before.size()) {
                names[count] = after.nameAt(right++);
                states[count++] = ADDED;
                continue;
            }
            if (right == after.size()) {
                names[count] = before.nameAt(left++);
                states[count++] = REMOVED;
                continue;
            }

            String leftName = before.nameAt(left);
            String rightName = after.nameAt(right);
            int compared = leftName.compareTo(rightName);
            if (compared < 0) {
                names[count] = leftName;
                states[count++] = REMOVED;
                left++;
            } else if (compared > 0) {
                names[count] = rightName;
                states[count++] = ADDED;
                right++;
            } else {
                if (!before.sameDigest(left, after, right)) {
                    names[count] = leftName;
                    states[count++] = CHANGED;
                }
                left++;
                right++;
            }
        }
        if (count == 0) return EMPTY;
        return new M3FileFactDelta(Arrays.copyOf(names, count), Arrays.copyOf(states, count));
    }

    public int size() {
        return names.length;
    }

    public boolean isEmpty() {
        return names.length == 0;
    }

    public String nameAt(int index) {
        return names[Objects.checkIndex(index, names.length)];
    }

    public byte stateAt(int index) {
        return states[Objects.checkIndex(index, states.length)];
    }

    public byte state(String name) {
        int index = Arrays.binarySearch(names, Objects.requireNonNull(name, "name"));
        return index < 0 ? 0 : states[index];
    }
}
