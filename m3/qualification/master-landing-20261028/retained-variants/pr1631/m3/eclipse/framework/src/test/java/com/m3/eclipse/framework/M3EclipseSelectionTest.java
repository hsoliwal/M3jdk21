// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.framework;

import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Independent scalar-mask oracle; payload callbacks are forbidden. */
class M3EclipseSelectionTest {
    private static long checks;

    @Test void allByteMaskPairsPreserveOriginalReferencesAndOrder() {
        M3EclipseIndexedSnapshot<Token> snapshot = new M3EclipseIndexedSnapshot<>();
        Token[] values = new Token[256];
        long[] handles = new long[values.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = new Token(i);
            handles[i] = snapshot.add(values[i], i);
        }
        for (int required = 0; required < 256; required++) {
            for (int excluded = 0; excluded < 256; excluded++) {
                List<Token> selected = snapshot.selected(required, excluded);
                int at = 0;
                for (int i = 0; i < values.length; i++) {
                    if ((i & required) == required && (i & excluded) == 0) {
                        check(at < selected.size(), "missing result");
                        check(selected.get(at++) == values[i], "identity/order");
                    }
                }
                check(at == selected.size(), "extra result");
            }
        }
        for (int i = 0; i < values.length; i++) {
            check(snapshot.atHandle(handles[i]) == values[i], "generation handle");
            check(snapshot.state(i) == i, "captured state");
        }
    }

    @Test void segmentBoundariesAndImmutableDetachedResults() {
        for (int size : new int[]{0, 1, 2, 63, 64, 65, 255, 256, 257, 4095, 4096, 8209}) {
            M3EclipseIndexedSnapshot<Token> snapshot = new M3EclipseIndexedSnapshot<>();
            Token[] values = new Token[size];
            for (int i = 0; i < size; i++) {
                values[i] = new Token(i);
                snapshot.add(values[i], (i * 73 + 19) & 255);
            }
            for (int[] masks : new int[][]{{0,0},{1,0},{0,1},{255,0},{0,255},{128,64},{1,1}}) {
                List<Token> result = snapshot.selected(masks[0], masks[1]);
                int position = 0;
                for (int i = 0; i < size; i++) {
                    int flags = (i * 73 + 19) & 255;
                    if ((flags & masks[0]) == masks[0] && (flags & masks[1]) == 0) {
                        check(result.get(position++) == values[i], "boundary identity");
                    }
                }
                check(result.size() == position, "boundary size");
                expect(UnsupportedOperationException.class, () -> result.add(new Token(-1)));
            }
            List<Token> frozen = snapshot.selected(0, 0);
            snapshot.add(new Token(size), 0);
            check(frozen.size() == size, "detached result");
        }
    }

    @Test void validationOccursForEmptyAndOverlappingSelections() {
        for (boolean populated : new boolean[]{false, true}) {
            M3EclipseIndexedSnapshot<Token> snapshot = new M3EclipseIndexedSnapshot<>();
            if (populated) snapshot.add(new Token(0), 1);
            for (int invalid : new int[]{-1, 256, 257, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
                expect(IllegalArgumentException.class, () -> snapshot.selected(invalid, 0));
                expect(IllegalArgumentException.class, () -> snapshot.selected(0, invalid));
                expect(IllegalArgumentException.class, () -> snapshot.selected(invalid, invalid));
            }
            int size = snapshot.size();
            expect(NullPointerException.class, () -> snapshot.add(null, 0));
            expect(IllegalArgumentException.class, () -> snapshot.add(new Token(0), 256));
            check(snapshot.size() == size, "failed append changed size");
            check(snapshot.selected(1, 1).isEmpty(), "overlap result");
            expect(IndexOutOfBoundsException.class, () -> snapshot.at(size));
        }
    }

    @Test void selectionDoesNotAddRetainedState() {
        long fields = java.util.Arrays.stream(M3EclipseIndexedSnapshot.class.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers())).count();
        check(fields == 3, "retained snapshot field count");
    }

    public static void main(String[] args) {
        M3EclipseSelectionTest test = new M3EclipseSelectionTest();
        test.allByteMaskPairsPreserveOriginalReferencesAndOrder();
        test.segmentBoundariesAndImmutableDetachedResults();
        test.validationOccursForEmptyAndOverlappingSelections();
        test.selectionDoesNotAddRetainedState();
        System.out.println("PASS snapshot checks=" + checks + " maskPairs=65536");
    }

    private static void check(boolean result, String failure) {
        checks++;
        if (!result) throw new AssertionError(failure);
    }

    private static void expect(Class<? extends Throwable> type, Runnable body) {
        checks++;
        try { body.run(); }
        catch (Throwable thrown) {
            if (type.isInstance(thrown)) return;
            throw new AssertionError("wrong exception", thrown);
        }
        throw new AssertionError("missing " + type.getName());
    }

    private record Token(int ordinal) {
        @Override public boolean equals(Object other) { throw new AssertionError("payload equals"); }
        @Override public int hashCode() { throw new AssertionError("payload hashCode"); }
        @Override public String toString() { throw new AssertionError("payload toString"); }
    }
}
