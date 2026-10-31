/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 * @test
 * @summary Filtered Set removal retains owner overrides for identity and equal probes
 * @run main FilteredSetMutationDispatchTest
 */
import java.util.CollectionViews;
import java.util.HashSet;
import java.util.Set;

public final class FilteredSetMutationDispatchTest {
    private record Item(int key, boolean visible) {
        @Override public boolean equals(Object value) {
            return value instanceof Item other && key == other.key;
        }
        @Override public int hashCode() { return key; }
    }

    private static final class GuardedSet extends HashSet<Item> {
        private static final long serialVersionUID = 1L;
        int containsCalls;
        int removeCalls;
        final int mode;
        GuardedSet(int mode) { this.mode = mode; }
        @Override public boolean contains(Object value) {
            containsCalls++;
            return super.contains(value);
        }
        @Override public boolean remove(Object value) {
            removeCalls++;
            if (mode == 0) return false;
            if (mode == 1) throw new UnsupportedOperationException("owner denies mutation");
            return super.remove(value);
        }
    }

    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        for (int mode = 0; mode < 3; mode++) {
            for (boolean identity : new boolean[]{true, false}) {
                GuardedSet owner = new GuardedSet(mode);
                Item stored = new Item(7, true);
                owner.add(stored);
                Item probe = identity ? stored : new Item(7, false);
                Set<Item> view = CollectionViews.filtered(owner, Item::visible);
                check(view.contains(probe), "stored visibility determines membership");
                check(owner.containsCalls == 1, "owner admission must not be duplicated");
                owner.containsCalls = 0;
                if (mode == 1) {
                    try { view.remove(probe); throw new AssertionError("owner rejection bypassed"); }
                    catch (UnsupportedOperationException expected) {
                        check(expected.getMessage().equals("owner denies mutation"), "same owner exception");
                    }
                } else {
                    check(view.remove(probe) == (mode == 2), "owner removal result bypassed");
                }
                check(owner.removeCalls == 1, "owner remove override must run exactly once");
                check(owner.containsCalls == 1, "remove owner admission must not be duplicated");
                check(owner.size() == (mode == 2 ? 0 : 1), "owner retains denied occurrence");
            }
        }
        System.out.println("PASS FilteredSetMutationDispatchTest checks=" + checks);
    }
}
