// SPDX-License-Identifier: Apache-2.0
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Hash-pinned package rename of the SWT residency engine into M3jdk21 {@code m3/ports/swing-viewport}.
 *
 * <pre>java ResidencyRename.java RENAME_MANIFEST.tsv SWT_WORKTREE M3JDK21_ROOT</pre>
 *
 * The SWT worktree must be checked out at the manifest's source commit with
 * {@code core.autocrlf=false} (blob bytes). For every row: the source must hash to
 * {@code source_sha256}; the transform (ordered literal replacements, identical to
 * {@code gen_rename_manifest.py}) must hash to {@code post_sha256}; a target already equal to the
 * postimage is left alone (fixed point); a different existing target is drift. Every row is
 * validated before any write. Exit codes: 0 applied or converged, 3 drift, 4 source mismatch,
 * 5 transform mismatch.
 */
public final class ResidencyRename {
    static final String[][] RULES = {
        {"org.eclipse.swt.internal.residency", "com.m3.swing.viewport.engine.residency"},
        {"org.eclipse.swt.internal.swap", "com.m3.swing.viewport.engine.swap"},
        {"org.eclipse.swt.internal.PackedHeapGrowth", "com.m3.swing.viewport.engine.PackedHeapGrowth"},
        {"import org.eclipse.swt.internal.*;", "import com.m3.swing.viewport.engine.*;"},
        {"package org.eclipse.swt.internal;", "package com.m3.swing.viewport.engine;"},
    };

    public static void main(String[] args) throws Exception {
        Path manifest = Path.of(args[0]), swt = Path.of(args[1]), m3 = Path.of(args[2]);
        List<String[]> rows = new ArrayList<>();
        for (String line : Files.readAllLines(manifest, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#") || line.startsWith("source_path\t")) continue;
            rows.add(line.split("\t"));
        }
        List<Object[]> writes = new ArrayList<>();
        for (String[] r : rows) {
            byte[] source = Files.readAllBytes(swt.resolve(r[0]));
            if (!sha(source).equals(r[1])) exit(4, "SOURCE_MISMATCH\t" + r[0]);
            String text = new String(source, StandardCharsets.UTF_8);
            for (String[] rule : RULES) text = text.replace(rule[0], rule[1]);
            byte[] post = text.getBytes(StandardCharsets.UTF_8);
            if (!sha(post).equals(r[4])) exit(5, "TRANSFORM_MISMATCH\t" + r[0]);
            Path target = m3.resolve(r[2]);
            if (Files.exists(target)) {
                String now = sha(Files.readAllBytes(target));
                if (now.equals(r[4])) continue;
                exit(3, "DRIFT\t" + r[2] + "\tfound " + now);
            }
            writes.add(new Object[] {target, post});
        }
        for (Object[] w : writes) {
            Path target = (Path) w[0];
            Files.createDirectories(target.getParent());
            Files.write(target, (byte[]) w[1]);
            System.out.println("WROTE\t" + target);
        }
        System.out.println("CHANGED\t" + writes.size() + "\tOF\t" + rows.size());
    }

    static void exit(int code, String message) {
        System.out.println(message);
        System.exit(code);
    }

    static String sha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private ResidencyRename() {
    }
}
