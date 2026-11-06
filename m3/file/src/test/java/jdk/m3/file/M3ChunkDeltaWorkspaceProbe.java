// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.file;

import java.util.Collections;

/** Actual product probe; launched by JUnit in a separate 48 MiB heap. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/c278513bd458f4c9a69e4f5fe261c9450368be3b/synexia-fileindex/src/test/java/com/synexia/fileindex/content/chunk/DeltaWorkspaceProbe.java blob 4f21f47b00c9; recipe m3/file/recipe/delta-generator-20261106/relocate.py v3
public final class M3ChunkDeltaWorkspaceProbe {
    private M3ChunkDeltaWorkspaceProbe() { }
    public static void main(String[] args) {
        String a = "%064x".formatted(1), b = "%064x".formatted(2);
        var old = Collections.nCopies(5000, a);
        var next = Collections.nCopies(5000, b);
        if (args[0].equals("script")) {
            var ops = new M3ChunkDeltaGenerator().generate(a, old, b, next).operations();
            if (ops.size() != 10000) throw new AssertionError("operation count");
            for (int i = 0; i < 5000; i++) {
                if (!(ops.get(i) instanceof M3ChunkDelta.Delete d) || !a.equals(d.hash())) throw new AssertionError("delete order");
                if (!(ops.get(5000 + i) instanceof M3ChunkDelta.Insert in) || !b.equals(in.hash())) throw new AssertionError("insert order");
            }
        } else if (M3ChunkDeltaGenerator.editDistance(old, next) != 5000) throw new AssertionError("distance");
        System.out.println("PASS " + args[0] + " 5000x5000 heap=" + Runtime.getRuntime().maxMemory());
    }
}
