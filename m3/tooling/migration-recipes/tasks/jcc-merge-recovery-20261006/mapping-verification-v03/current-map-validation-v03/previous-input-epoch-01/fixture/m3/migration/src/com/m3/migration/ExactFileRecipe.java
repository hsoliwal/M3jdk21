/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.migration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Dependency-free, Maven-executable source-bound additive recipe.
 * Reviewed postimages, not blind renames, define the transformation.
 * Every source, target guard, snapshot and destination is checked before writes.
 * No existing destination is overwritten. Cooperating invocations use a lock.
 *
 * <p>This is NOT a cross-file filesystem transaction. A crash may leave completed
 * files or a partial new file: complete equal files replay safely; partial/drifted
 * files require review. Rollback removes only receipt-owned, still-equal additions.
 * A crash after file creation but before recording ownership may leave an unowned
 * file, which rollback conservatively preserves. Non-cooperating concurrent
 * filesystem mutation is unsupported. No provenance signature is implied.
 */
public final class ExactFileRecipe {
    private static final int MAX_FILE = 8 * 1024 * 1024;
    private static final int MAX_TOTAL_POSTIMAGES = 16 * 1024 * 1024;
    private static final String STATE = "m3/migration/.state";
    private ExactFileRecipe() { }
    public record Result(int changed, int unchanged, boolean dryRun) { }
    private record Pin(String path, String sha) { }
    private record Edit(String path, String sha, byte[] bytes) { }
    private record Plan(String sha, List<Pin> sources, List<Pin> guards, List<Edit> edits) { }

    public static Result apply(Path sourceRoot, Path targetRoot, Path planFile,
                               boolean checkOnly) throws IOException {
        Path source = root(sourceRoot), target = root(targetRoot);
        Plan plan = load(planFile);
        verifyPins(source, plan.sources);
        verifyPins(target, plan.guards);
        int unchanged = preflight(target, plan);
        readReceipt(target, plan, false);
        if (checkOnly) return new Result(plan.edits.size() - unchanged, unchanged, true);
        Path state = safe(target, STATE);
        Files.createDirectories(state);
        Path lockPath = safe(target, STATE + "/lock");
        FileChannel lock = FileChannel.open(lockPath, StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
        try (lock) {
            lock.force(true);
            verifyPins(source, plan.sources);
            verifyPins(target, plan.guards);
            unchanged = preflight(target, plan);
            Set<String> owned = readReceipt(target, plan, false);
            writeReceipt(target, plan, owned);
            int changed = 0;
            for (Edit edit : plan.edits) {
                Path destination = safe(target, edit.path);
                if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
                    verify(target, new Pin(edit.path, edit.sha));
                    continue;
                }
                Files.createDirectories(destination.getParent());
                // CREATE_NEW never overwrites a concurrently created file or symlink.
                try (FileChannel output = FileChannel.open(destination,
                        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE,
                        LinkOption.NOFOLLOW_LINKS)) {
                    ByteBuffer bytes = ByteBuffer.wrap(edit.bytes);
                    while (bytes.hasRemaining()) output.write(bytes);
                    output.force(true);
                }
                owned.add(edit.path);
                writeReceipt(target, plan, owned);
                changed++;
            }
            return new Result(changed, unchanged, false);
        } finally {
            // Close the channel before unlinking; open-file unlink is not portable to Windows.
            Files.delete(lockPath);
        }
    }

    public static Result rollback(Path targetRoot, Path planFile) throws IOException {
        Path target = root(targetRoot);
        Plan plan = load(planFile);
        verifyPins(target, plan.guards);
        Set<String> owned = readReceipt(target, plan, true);
        Path lockPath = safe(target, STATE + "/lock");
        FileChannel lock = FileChannel.open(lockPath, StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
        try (lock) {
            lock.force(true);
            owned = readReceipt(target, plan, true);
            // Validate ALL owned files before removing any; never discard adaptations.
            for (Edit edit : plan.edits) if (owned.contains(edit.path)) {
                verify(target, new Pin(edit.path, edit.sha));
            }
            int changed = 0;
            for (Edit edit : plan.edits) if (owned.contains(edit.path)) {
                Files.delete(safe(target, edit.path));
                owned.remove(edit.path);
                writeReceipt(target, plan, owned);
                changed++;
            }
            Files.delete(receiptPath(target, plan));
            return new Result(changed, plan.edits.size() - changed, false);
        } finally {
            // Close the channel before unlinking; open-file unlink is not portable to Windows.
            Files.delete(lockPath);
        }
    }

    private static int preflight(Path target, Plan plan) throws IOException {
        int unchanged = 0;
        for (Edit edit : plan.edits) {
            Path path = safe(target, edit.path);
            if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                verify(target, new Pin(edit.path, edit.sha));
                unchanged++;
            }
        }
        return unchanged;
    }

    private static Plan load(Path file) throws IOException {
        Path parent = root(file.toAbsolutePath().normalize().getParent());
        byte[] bytes = read(safe(parent, file.getFileName().toString()), 1024 * 1024);
        List<String> lines = decode(bytes).lines().toList();
        if (lines.isEmpty() || !lines.getFirst().equals("M3-EXACT-FILE-RECIPE\t1")) {
            throw new IllegalArgumentException("unsupported recipe format");
        }
        if (lines.size() > 4096) throw new IllegalArgumentException("recipe entry limit");
        var sources = new ArrayList<Pin>();
        var guards = new ArrayList<Pin>();
        var edits = new ArrayList<Edit>();
        var sourcePaths = new HashSet<String>();
        var targetPaths = new HashSet<String>();
        int total = 0;
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] row = line.split("\t", -1);
            if (row.length < 3 || !row[2].matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("invalid pin at line " + (i + 1));
            }
            validateRelative(row[1]);
            switch (row[0]) {
                case "source" -> {
                    if (row.length != 3 || !sourcePaths.add(row[1])) throw new IllegalArgumentException("duplicate/invalid source");
                    sources.add(new Pin(row[1], row[2]));
                }
                case "guard" -> {
                    if (row.length != 3 || !targetPaths.add(row[1])) throw new IllegalArgumentException("duplicate/invalid guard");
                    guards.add(new Pin(row[1], row[2]));
                }
                case "add" -> {
                    if (row.length != 4 || !row[1].startsWith("m3/")
                            || row[1].equals(STATE) || STATE.startsWith(row[1] + "/")
                            || row[1].startsWith(STATE + "/") || !targetPaths.add(row[1])) {
                        throw new IllegalArgumentException("duplicate/unsafe destination");
                    }
                    byte[] postimage = read(safe(parent, row[3]), MAX_FILE);
                    if (!hash(postimage).equals(row[2])) throw new IOException("snapshot pin mismatch: " + row[1]);
                    total = Math.addExact(total, postimage.length);
                    if (total > MAX_TOTAL_POSTIMAGES) throw new IOException("postimage memory budget exceeded");
                    edits.add(new Edit(row[1], row[2], postimage));
                }
                default -> throw new IllegalArgumentException("unknown recipe operation: " + row[0]);
            }
        }
        if (sources.isEmpty() || guards.isEmpty() || edits.isEmpty()) {
            throw new IllegalArgumentException("recipe requires source pins, target guards and additions");
        }
        Set<String> destinations = new HashSet<>();
        for (Edit edit : edits) destinations.add(edit.path);
        for (String destination : destinations) {
            for (int slash = destination.indexOf('/'); slash >= 0;
                    slash = destination.indexOf('/', slash + 1)) {
                if (destinations.contains(destination.substring(0, slash))) {
                    throw new IllegalArgumentException("planned file/directory conflict");
                }
            }
        }
        return new Plan(hash(bytes), List.copyOf(sources), List.copyOf(guards), List.copyOf(edits));
    }

    private static void verifyPins(Path root, List<Pin> pins) throws IOException {
        for (Pin pin : pins) verify(root, pin);
    }
    private static void verify(Path root, Pin pin) throws IOException {
        if (!hash(read(safe(root, pin.path), MAX_FILE)).equals(pin.sha)) {
            throw new IOException("pin mismatch; refusing drift: " + pin.path);
        }
    }
    private static Path root(Path input) throws IOException {
        Path path = input.toAbsolutePath().normalize();
        Path current = path.getRoot();
        for (Path part : path) {
            current = current.resolve(part);
            if (Files.isSymbolicLink(current)) throw new IOException("symlink root component");
        }
        if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) throw new IOException("root is not a directory: " + path);
        return path.toRealPath(LinkOption.NOFOLLOW_LINKS);
    }
    private static void validateRelative(String relative) {
        if (!relative.matches("[A-Za-z0-9_.-]+(/[A-Za-z0-9_.-]+)*")) throw new IllegalArgumentException("unsafe relative path");
        for (String part : relative.split("/")) {
            if (part.equals(".") || part.equals("..")) throw new IllegalArgumentException("path traversal");
        }
    }
    private static Path safe(Path root, String relative) throws IOException {
        validateRelative(relative);
        Path current = root;
        String[] parts = relative.split("/");
        for (int i = 0; i < parts.length; i++) {
            current = current.resolve(parts[i]);
            if (Files.isSymbolicLink(current)) throw new IOException("symlink path component: " + relative);
            if (i + 1 < parts.length && Files.exists(current, LinkOption.NOFOLLOW_LINKS)
                    && !Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("nondirectory path ancestor: " + relative);
            }
        }
        return current;
    }
    private static byte[] read(Path file, int limit) throws IOException {
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) throw new IOException("missing or nonregular file: " + file);
        try (var input = FileChannel.open(file, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);
             var output = new ByteArrayOutputStream()) {
            if (input.size() > limit) throw new IOException("file budget exceeded: " + file);
            ByteBuffer buffer = ByteBuffer.allocate(8192);
            while (input.read(buffer) != -1) {
                buffer.flip();
                if (output.size() > limit - buffer.remaining()) throw new IOException("growing file exceeds budget");
                output.write(buffer.array(), 0, buffer.remaining());
                buffer.clear();
            }
            return output.toByteArray();
        }
    }
    private static String decode(byte[] bytes) throws IOException {
        return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
    }
    private static String hash(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private static Path receiptPath(Path target, Plan plan) throws IOException {
        return safe(target, STATE + "/" + plan.sha + ".receipt");
    }
    private static Set<String> readReceipt(Path target, Plan plan, boolean required) throws IOException {
        Path path = receiptPath(target, plan);
        var owned = new TreeSet<String>();
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            if (required) throw new IOException("no ownership receipt; rollback refused");
            return owned;
        }
        List<String> lines = decode(read(path, 1024 * 1024)).lines().toList();
        String header = "M3-OWNED-ADDITIONS\t1\t" + plan.sha + "\t" + hash(target.toString().getBytes(StandardCharsets.UTF_8));
        if (lines.isEmpty() || !lines.getFirst().equals(header)) throw new IOException("receipt identity mismatch");
        Set<String> allowed = new HashSet<>();
        for (Edit edit : plan.edits) allowed.add(edit.path);
        for (String line : lines.subList(1, lines.size())) {
            if (!allowed.contains(line) || !owned.add(line)) throw new IOException("invalid receipt ownership");
        }
        for (Edit edit : plan.edits) if (owned.contains(edit.path)) {
            verify(target, new Pin(edit.path, edit.sha));
        }
        return owned;
    }
    private static void writeReceipt(Path target, Plan plan, Set<String> owned) throws IOException {
        Path receipt = receiptPath(target, plan);
        StringBuilder text = new StringBuilder("M3-OWNED-ADDITIONS\t1\t").append(plan.sha)
                .append('\t').append(hash(target.toString().getBytes(StandardCharsets.UTF_8))).append('\n');
        for (String path : new TreeSet<>(owned)) text.append(path).append('\n');
        Path temporary = Files.createTempFile(receipt.getParent(), "receipt-", ".tmp");
        try {
            Files.writeString(temporary, text, StandardCharsets.UTF_8);
            Files.move(temporary, receipt, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
    }
    public static void main(String[] args) throws IOException {
        Result result;
        if (args.length == 4 && (args[0].equals("apply") || args[0].equals("check"))) {
            result = apply(Path.of(args[1]), Path.of(args[2]), Path.of(args[3]), args[0].equals("check"));
        } else if (args.length == 3 && args[0].equals("rollback")) {
            result = rollback(Path.of(args[1]), Path.of(args[2]));
        } else {
            throw new IllegalArgumentException("check|apply SOURCE_ROOT TARGET_ROOT PLAN; rollback TARGET_ROOT PLAN");
        }
        System.out.println("EXACT_RECIPE_PASS mode=" + args[0] + " result=" + result);
    }
}
