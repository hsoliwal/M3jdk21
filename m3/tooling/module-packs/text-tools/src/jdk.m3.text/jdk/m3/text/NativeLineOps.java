/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.text;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Optional native kernels over the JDK's canonical M3-backed String facade.
 * Native code returns only primitive permutations or bit masks; Java retains
 * ownership of every String and TextLine and performs output framing.
 */
final class NativeLineOps {
    private static final boolean LOADED = load();

    private NativeLineOps() { }

    static boolean available() { return LOADED; }

    static int[] sortOrder(List<TextLine> lines, boolean reverse) {
        String[] contents = contents(lines);
        int[] order = new int[contents.length];
        sort0(contents, order, reverse);
        return order;
    }

    static long[] adjacentKeep(List<TextLine> lines) {
        String[] contents = contents(lines);
        long[] mask = new long[(contents.length >>> 6) + ((contents.length & 63) == 0 ? 0 : 1)];
        uniq0(contents, mask);
        return mask;
    }

    private static String[] contents(List<TextLine> lines) {
        String[] strings = new String[lines.size()];
        for (int i = 0; i < strings.length; i++) strings[i] = lines.get(i).content();
        return strings;
    }

    private static boolean load() {
        try {
            Path installed = Path.of(System.getProperty("java.home"), "lib",
                    System.mapLibraryName("m3textops"));
            if (Files.isRegularFile(installed)) System.load(installed.toAbsolutePath().toString());
            else System.loadLibrary("m3textops");
            return true;
        } catch (UnsatisfiedLinkError missingOptionalLibrary) {
            return false;
        }
    }

    private static native void sort0(String[] contents, int[] order, boolean reverse);
    private static native void uniq0(String[] contents, long[] keepBits);
}
