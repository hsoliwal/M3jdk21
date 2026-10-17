// SPDX-License-Identifier: Apache-2.0
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

/**
 * Narrow offline Java/Maven replay adapter for the existing M3 hash-pinned snapshot manifest.
 * Not a parser, alternate rewrite engine, or automatic promotion authority. The normal owner is
 * M3HashPinnedJavaSnapshotRecipe. Admission/locking follows the existing packed-lazy-tree replay.
 * Entire input/template set is checked before writing; replacement is atomic per file, NOT a
 * repository-wide transaction. An interrupted multi-file application must be reconciled explicitly.
 */
public final class Replay {
    private static final String CRATE = "m3/collections/wrappers/representative-semantics-20261017/replay-inputs/";
    private static final String MANIFEST_SHA256 = "e4aa2cc6d5d735430dc22b1952aa03e23cead8750019d15667331ca132ea359b";
    private record Leaf(String path, String pre, String post, byte[] bytes) {}
    private Replay() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 3 || !(args[0].equals("--apply") || args[0].equals("--check"))) {
            throw new IllegalArgumentException("--apply|--check SOURCE_ROOT TARGET_ROOT");
        }
        Path source = Path.of(args[1]).toRealPath(), target = Path.of(args[2]).toRealPath();
        List<Leaf> leaves = templates(source);
        if (state(target, leaves)) {
            System.out.println("PASS M3 stored representative replay changes=0");
            return;
        }
        if (args[0].equals("--check")) throw new IOException("recipe not materialized");
        Path locks = safe(target, "target/m3-short-flag-replay"); Files.createDirectories(locks);
        Path lock = safe(target, "target/m3-short-flag-replay/ownership.lock");
        try (var channel = FileChannel.open(lock, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                var ownership = channel.tryLock()) {
            if (ownership == null || !ownership.isValid()) throw new IOException("worktree already owned");
            if (state(target, leaves)) { System.out.println("PASS M3 stored representative replay changes=0"); return; }
            // Fully stage every candidate before replacing any production/test leaf.
            List<Path> stages = new ArrayList<>();
            try {
                for (Leaf leaf : leaves) {
                    Path file = safe(target, leaf.path()); Files.createDirectories(file.getParent());
                    Path stage = Files.createTempFile(file.getParent(), ".m3-lazy-", ".stage");
                    stages.add(stage); Files.write(stage, leaf.bytes());
                    if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)
                            && Files.getFileStore(file).supportsFileAttributeView("posix")) {
                        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(file);
                        Files.setPosixFilePermissions(stage, permissions);
                    }
                }
                if (state(target, leaves)) throw new IOException("input changed during staging");
                for (int i = 0; i < leaves.size(); i++) {
                    Leaf leaf = leaves.get(i); Path file = safe(target, leaf.path());
                    if (!leaf.pre().equals(hashAt(file))) throw new IOException("source changed: " + leaf.path());
                    if (leaf.pre().equals("ABSENT")) {
                        // Atomic create-if-absent: publish the prepared inode, never replace an incumbent.
                        Files.createLink(file, stages.get(i));
                        Files.delete(stages.get(i));
                    } else {
                        Files.move(stages.get(i), file, StandardCopyOption.ATOMIC_MOVE,
                                StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                if (!state(target, leaves)) throw new IOException("postimage mismatch");
            } finally { for (Path stage : stages) Files.deleteIfExists(stage); }
        }
        System.out.println("PASS M3 stored representative replay changes=" + leaves.size());
    }

    private static List<Leaf> templates(Path root) throws Exception {
        var result = new ArrayList<Leaf>(); var seen = new HashSet<String>();
        byte[] manifest = Files.readAllBytes(safe(root, CRATE + "manifest.tsv"));
        if (!hash(manifest).equals(MANIFEST_SHA256)) throw new IOException("manifest drift");
        for (String line : new String(manifest, java.nio.charset.StandardCharsets.UTF_8).split("\\R")) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] fields = line.split("\t", -1);
            if (fields.length != 4 || !(fields[0].startsWith("m3/collections/") && (fields[0].endsWith(".java") || fields[0].endsWith(".c"))
                        || fields[0].equals("tools/lazy-columnar-tree-views/verify-inputs.py")
                        || fields[0].equals("tools/lazy-columnar-tree-views/SOURCES.txt")) || !seen.add(fields[0])
                    || !(fields[1].equals("ABSENT") || fields[1].matches("[0-9a-f]{64}"))
                    || !fields[2].matches("[0-9a-f]{64}") || fields[3].contains("/")) {
                throw new IOException("invalid manifest row");
            }
            byte[] bytes = Files.readAllBytes(safe(root, CRATE + fields[3]));
            if (!fields[2].equals(hash(bytes))) throw new IOException("template drift: " + fields[0]);
            result.add(new Leaf(fields[0], fields[1], fields[2], bytes));
        }
        if (result.isEmpty()) throw new IOException("empty manifest");
        return List.copyOf(result);
    }

    /** Return true only for all-POST; all-PRE is false; drift and mixed state are refused. */
    private static boolean state(Path root, List<Leaf> leaves) throws Exception {
        boolean pre = true, post = true;
        for (Leaf leaf : leaves) {
            String actual = hashAt(safe(root, leaf.path()));
            if (!actual.equals(leaf.pre()) && !actual.equals(leaf.post())) {
                throw new IOException("source/absence drift: " + leaf.path());
            }
            pre &= actual.equals(leaf.pre()); post &= actual.equals(leaf.post());
        }
        if (post) return true;
        if (pre) return false;
        throw new IOException("mixed PRE/POST input");
    }
    private static Path safe(Path root, String relative) throws IOException {
        Path name = Path.of(relative);
        if (name.isAbsolute() || !name.normalize().equals(name)) throw new IOException("unsafe path");
        Path cursor = root;
        for (Path part : name) {
            cursor = cursor.resolve(part);
            if (Files.isSymbolicLink(cursor)) throw new IOException("symlink refused: " + relative);
        }
        if (!cursor.startsWith(root)) throw new IOException("path escape");
        return cursor;
    }
    private static String hashAt(Path path) throws Exception {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return "ABSENT";
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw new IOException("not a file");
        return hash(Files.readAllBytes(path));
    }
    private static String hash(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
