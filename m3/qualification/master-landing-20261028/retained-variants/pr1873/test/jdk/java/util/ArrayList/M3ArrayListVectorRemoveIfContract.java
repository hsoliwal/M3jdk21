/*
 * Copyright (c) 2026, Hitesh Soliwal and Contributors to the Synexia Project.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

/*
 * @test
 * @summary M3JDK21 ArrayList and Vector removeIf preserves large-mask callback and reference contracts
 * @run main M3ArrayListVectorRemoveIfContract
 */
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/** Independent public-contract oracle; full product image required to test M3 dispatch. */
public final class M3ArrayListVectorRemoveIfContract {
    private static long checks;

    private M3ArrayListVectorRemoveIfContract() { }

    public static void main(String[] args) {
        for (boolean vector : new boolean[] {false, true}) {
            for (int length : new int[] {0, 1, 63, 64, 65, 511, 512, 513, 1023, 1024, 2048}) {
                for (int mode = 0; mode < 7; mode++) {
                    verify(vector, length, mode);
                }
            }
            verifyCallbackException(vector);
            verifyNullPredicate(vector);
        }
        System.out.println("M3ArrayListVectorRemoveIfContract checks=" + checks);
    }

    private static List<Object> list(boolean vector) {
        return vector ? new Vector<>() : new ArrayList<>();
    }

    private static boolean deleted(int index, int length, int mode) {
        return switch (mode) {
            case 0 -> false;
            case 1 -> true;
            case 2 -> (index & 1) == 0;
            case 3 -> index == 0;
            case 4 -> index == length - 1;
            case 5 -> index >= length / 3 && index < 2 * length / 3;
            default -> index % 17 == 0 || index % 17 == 16;
        };
    }

    private static void verify(boolean vector, int length, int mode) {
        List<Object> collection = list(vector);
        Object[] originals = new Object[length];
        Object shared = new Object();
        for (int index = 0; index < length; index++) {
            Object value = index % 29 == 0 ? null : index % 13 == 0 ? shared : new Object();
            originals[index] = value;
            collection.add(value);
        }
        Iterator<Object> cursor = collection.iterator();
        int[] invoked = {0};
        boolean expectedChange = false;
        for (int i = 0; i < length; i++) expectedChange |= deleted(i, length, mode);
        boolean actualChange = collection.removeIf(value -> {
            int index = invoked[0]++;
            if (index >= length || value != originals[index]) {
                throw new AssertionError("Predicate encounter order changed");
            }
            checks++;
            return deleted(index, length, mode);
        });
        sameBoolean(expectedChange, actualChange);
        sameInt(length, invoked[0]);
        int survivor = 0;
        for (int index = 0; index < length; index++) {
            if (!deleted(index, length, mode)) {
                if (collection.get(survivor++) != originals[index]) {
                    throw new AssertionError("Survivor identity/order changed");
                }
                checks++;
            }
        }
        sameInt(survivor, collection.size());
        if (expectedChange && length > 0) {
            expect(ConcurrentModificationException.class, cursor::next);
        } else {
            if (length > 0) sameObject(originals[0], cursor.next());
            else sameBoolean(false, cursor.hasNext());
        }
        Object appended = new Object();
        collection.add(appended);
        sameObject(appended, collection.get(survivor));
    }

    private static void verifyCallbackException(boolean vector) {
        List<Object> collection = list(vector);
        for (int i = 0; i < 1024; i++) collection.add(new Object());
        Object[] original = collection.toArray();
        RuntimeException sentinel = new RuntimeException("user callback");
        int[] called = {0};
        try {
            collection.removeIf(value -> {
                if (++called[0] == 515) throw sentinel;
                return true;
            });
            throw new AssertionError("Callback should throw");
        } catch (RuntimeException exception) {
            sameObject(sentinel, exception);
        }
        sameInt(515, called[0]);
        sameInt(1024, collection.size());
        for (int i = 0; i < 1024; i++) sameObject(original[i], collection.get(i));
    }

    private static void verifyNullPredicate(boolean vector) {
        List<Object> collection = list(vector);
        expect(NullPointerException.class, () -> collection.removeIf(null));
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); }
        catch (Throwable exception) {
            if (type.isInstance(exception)) { checks++; return; }
            throw new AssertionError("Unexpected throwable", exception);
        }
        throw new AssertionError("Expected " + type.getName());
    }

    private static void sameObject(Object a, Object b) {
        if (a != b) throw new AssertionError("Reference identity changed");
        checks++;
    }

    private static void sameInt(int a, int b) {
        if (a != b) throw new AssertionError(a + " != " + b);
        checks++;
    }

    private static void sameBoolean(boolean a, boolean b) {
        if (a != b) throw new AssertionError(a + " != " + b);
        checks++;
    }
}
