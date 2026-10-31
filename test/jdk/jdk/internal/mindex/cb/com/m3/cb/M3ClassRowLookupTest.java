// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
/*
 * @test
 * @summary M3Class indexed lookup uses immutable CSR rows without copying; class-loader identity is preserved
 * @library /jdk/internal/mindex/cb
 * @modules java.base/jdk.internal.mindex
 * @build com.m3.cb.VmFixture
 * @run main/othervm -Xint com.m3.cb.M3ClassRowLookupTest
 * @run main/othervm -Xmixed com.m3.cb.M3ClassRowLookupTest
 */
package com.m3.cb;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import jdk.internal.mindex.M3CI;
import jdk.internal.mindex.M3Class;
import jdk.internal.mindex.M3VI;

/** Differential class/loader lookup proof; no test-specific product runtime dependency. */
public final class M3ClassRowLookupTest {
    private static int checks;

    private static final class DefiningLoader extends ClassLoader {
        DefiningLoader() {
            super(M3ClassRowLookupTest.class.getClassLoader());
        }

        Class<?> define(byte[] bytes) {
            return defineClass(null, bytes, 0, bytes.length);
        }
    }

    private static void check(boolean truth, String message) {
        checks++;
        if (!truth) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        byte[] bytes;
        try (InputStream source = VmFixture.class.getResourceAsStream("VmFixture.class")) {
            if (source == null) throw new AssertionError("missing VmFixture classfile");
            bytes = source.readAllBytes();
        }

        Class<?> first = new DefiningLoader().define(bytes);
        Class<?> second = new DefiningLoader().define(bytes);
        check(first != second && first.getName().equals(second.getName()),
                "different defining loaders create different classes");

        List<Class<?>> roots = List.of(first, second, Object.class, String.class);
        M3CI index = M3CI.compile(roots, M3CI.Options.standard(), M3VI.Progress.none());
        M3Class a = index.fromClass(first).orElseThrow();
        M3Class b = index.fromClass(second).orElseThrow();
        check(a.row() != b.row(), "distinct class rows");
        check(a.loaderSpaceId() != b.loaderSpaceId(), "distinct loader spaces");
        check(index.classRows(a.getName()).length == 2, "two same-name rows");
        check(index.classRow(a.loaderSpaceId(), a.name()) == a.row(), "first exact identity");
        check(index.classRow(b.loaderSpaceId(), b.name()) == b.row(), "second exact identity");
        check(index.classRow(-1, a.name()) == -1, "negative loader refused");
        check(index.classRow(Integer.MAX_VALUE, a.name()) == -1, "out-of-range loader refused");
        check(index.classRow(a.loaderSpaceId(), "not.present.M3Class") == -1, "unknown name refused");
        check(index.classRow(a.loaderSpaceId(), Object.class.getName()) == -1,
                "same-loader mismatch refused");
        try {
            index.classRow(0, null);
            throw new AssertionError("valid loader with null name must throw NPE");
        } catch (NullPointerException expected) {
            checks++;
        }
        check(index.classRow(-1, null) == -1, "invalid loader keeps historical return semantics");

        int[] rows = index.classRows(a.name());
        Arrays.fill(rows, -1);
        check(index.classRows(a.name()).length == 2, "classRows returns detached copy");
        check(index.classRow(a.loaderSpaceId(), a.name()) == a.row(),
                "lookup independent of caller mutation");
        check(index.classRow(b.loaderSpaceId(), b.name()) == b.row(),
                "second loader unaffected by copied rows");
        check(a.equals(index.classAt(a.row())), "immutable view equality");
        check(a.hashCode() == index.classAt(a.row()).hashCode(), "stable view hash");
        check(a.toClass() == first && b.toClass() == second, "weak JVM boundary round-trip");

        M3CI compact = M3CI.compile(roots, M3CI.Options.compact(), M3VI.Progress.none());
        int[] compactRows = compact.classRows(a.name());
        check(compactRows.length == 2, "compact snapshot includes both loaders");
        for (int row : compactRows) {
            int loaderSpace = compact.loaderSpaceId(row);
            check(compact.classRow(loaderSpace, a.name()) == row,
                    "compact exact lookup uses owner-local loader identity");
        }
        System.out.println("PASS checks=" + checks);
    }
}
