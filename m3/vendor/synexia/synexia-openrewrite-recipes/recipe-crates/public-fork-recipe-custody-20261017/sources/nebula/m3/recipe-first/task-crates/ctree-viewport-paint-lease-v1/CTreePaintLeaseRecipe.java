// SPDX-License-Identifier: Apache-2.0
package org.eclipse.nebula.m3.recipe;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

/**
 * Executable, fail-closed FILE-atom transformation recipe.
 *
 * <p>Uses the original pinned EPL-2.0 CTree source from Git, applies the
 * associated EPL-2.0 unified patch to a temporary worktree, and compares the
 * exact output to the currently reviewed target. Only the real CTree target
 * remains EPL-2.0; this first-party orchestration changes no upstream license.
 * It never edits the caller's current Git working tree.</p>
 */
public final class CTreePaintLeaseRecipe {
    private static final String PATH =
            "widgets/ctree/org.eclipse.nebula.widgets.ctree/src/"
            + "org/eclipse/nebula/widgets/ctree/CTree.java";
    private static final String BASE = "9943511a112e25c9fdaeb1e92442674fa0d19443";
    private static final String BEFORE =
            "5f97a62f5a3582c5a5428768fe3214e3c55be2a4665c8561ffc1cc6bbd2b9ad7";
    private static final String AFTER =
            "19c1196540552a095b648a52706c87dea66f465953fa9e24f45f1eb950486e6b";
    private static final String PATCH =
            "m3/recipe-first/task-crates/ctree-viewport-paint-lease-v1/CTreePaintLease.patch";

    private CTreePaintLeaseRecipe() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 2 || !args[0].equals("--verify")) {
            throw new IllegalArgumentException("usage: --verify path-to-nebula-repository");
        }
        Path root = Path.of(args[1]).toRealPath();
        Path patch = root.resolve(PATCH).toRealPath();
        Path current = root.resolve(PATH).toRealPath();
        require(current.startsWith(root) && patch.startsWith(root), "source path escapes repository");

        byte[] before = gitBytes(root, List.of("show", BASE + ":" + PATH));
        require(sha(before).equals(BEFORE), "base CTree image drift");
        byte[] expected = Files.readAllBytes(current);
        require(sha(expected).equals(AFTER), "current CTree image drift");
        // Fail closed on unexpected hunk targets; no wildcard or module-wide rewrite.
        String patchText = Files.readString(patch, StandardCharsets.UTF_8);
        require(patchText.startsWith("diff --git a/" + PATH + " b/" + PATH + "\n"),
                "patch must start with the single expected target");
        require(patchText.indexOf("\ndiff --git ", 1) < 0,
                "recipe must change precisely one source file");

        Path scratch = Files.createTempDirectory("m3-ctree-atom-replay-");
        try {
            Path target = scratch.resolve(PATH);
            Files.createDirectories(target.getParent());
            Files.write(target, before);
            gitBytes(scratch, List.of("apply", "--check", patch.toString()));
            gitBytes(scratch, List.of("apply", patch.toString()));
            byte[] replayed = Files.readAllBytes(target);
            require(sha(replayed).equals(AFTER), "recipe postimage SHA-256 mismatch");
            require(java.util.Arrays.equals(replayed, expected), "recipe differs from approved CTree");
            // Fixed point: patch must not reapply to the approved result.
            boolean reapplicable = gitExit(scratch, List.of(
                    "apply", "--check", patch.toString())) == 0;
            require(!reapplicable, "recipe is not fixed-point stable");
            // But the exact reverse patch must still recover the original atom.
            gitBytes(scratch, List.of("apply", "--reverse", "--check", patch.toString()));
            gitBytes(scratch, List.of("apply", "--reverse", patch.toString()));
            require(sha(Files.readAllBytes(target)).equals(BEFORE),
                    "reverse recipe does not recover original source");
            System.out.println("PASS CTreePaintLeaseRecipe: exact EPL-2.0 FILE atom, "
                    + "forward/reverse SHA-256, fixed-point and drift refusal");
        } finally {
            try (var walk = Files.walk(scratch)) {
                for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private static byte[] gitBytes(Path cwd, List<String> args) throws Exception {
        byte[] result = git(cwd, args);
        return result;
    }

    private static int gitExit(Path cwd, List<String> args) throws Exception {
        List<String> command = new java.util.ArrayList<>(args.size() + 3);
        command.add("git");
        command.add("-C");
        command.add(cwd.toString());
        command.addAll(args);
        Process child = new ProcessBuilder(command).redirectErrorStream(true).start();
        try (InputStream output = child.getInputStream()) {
            output.transferTo(java.io.OutputStream.nullOutputStream());
        }
        return child.waitFor();
    }

    private static byte[] git(Path cwd, List<String> args) throws Exception {
        List<String> command = new java.util.ArrayList<>(args.size() + 3);
        command.add("git");
        command.add("-C");
        command.add(cwd.toString());
        command.addAll(args);
        Process child = new ProcessBuilder(command).redirectErrorStream(true).start();
        byte[] output;
        try (InputStream stream = child.getInputStream()) {
            output = stream.readAllBytes();
        }
        if (child.waitFor() != 0) {
            throw new IOException("source-sealed Git recipe failed: "
                    + String.join(" ", args) + ": "
                    + new String(output, StandardCharsets.UTF_8));
        }
        return output;
    }

    private static String sha(byte[] input) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input));
    }

    private static void require(boolean success, String cause) {
        if (!success) throw new IllegalStateException(cause);
    }

}
