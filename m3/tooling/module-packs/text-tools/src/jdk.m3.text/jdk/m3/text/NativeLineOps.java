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

    /** Older installed libraries retain sort/uniq support without this optional symbol. */
    static boolean structuralAvailable() { return StructuralCapability.AVAILABLE; }

    private static final class StructuralCapability {
        private static final boolean AVAILABLE = probe();

        private static boolean probe() {
            if (!LOADED) return false;
            try {
                if (structural0("", 0, 0, ',', new int[0]) != 0)
                    throw new LinkageError("invalid native structural scan capability");
                return true;
            } catch (UnsatisfiedLinkError missingOptionalSymbol) {
                return false;
            }
        }
    }

    /** Fill one caller-owned window; errors after the capability probe propagate. */
    static int structuralOffsets(String input, int from, int units,
                                 char delimiter, int[] positions) {
        return structural0(input, from, units, delimiter, positions);
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
        } catch (UnsatisfiedLinkError | SecurityException unavailableOptionalLibrary) {
            return false;
        }
    }

    private static native int structural0(String input, int from, int units,
                                          char delimiter, int[] positions);
    private static native void sort0(String[] contents, int[] order, boolean reverse);
    private static native void uniq0(String[] contents, long[] keepBits);
}
