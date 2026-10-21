// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
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
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Narrow filesystem adapter over the canonical source-seal manifest. The task owns its
 * templates; this class contains no text transformations and grants no M3 promotion authority.
 * Callers must hold exclusive ownership of the target files while applying a reviewed plan.
 */
public final class Replay {
    private static final String RESOURCE = "/m3/qualification/";
    private static final Runnable NONE = () -> { };

    private Replay() { }

    public record Change(String path, String before, String after) { }
    public record Report(String baseRevision, String manifestHash, List<Change> changes,
                         boolean applied) {
        public Report { changes = List.copyOf(changes); }
        public boolean promotionAuthority() { return false; }
    }

    /** Preflight all source and resource seals before any target file is created or replaced. */
    public static Report replay(Path repository, Path templates, boolean apply, Runnable progress)
            throws IOException {
        Runnable checkpoint = progress == null ? NONE : progress;
        M3GpuToolJoinManifest.check(checkpoint);
        Path root = checkedRoot(repository);
        Path payload = checkedRoot(templates);
        String revision = read(checkedPath(payload, "base-revision.txt")).strip();
        if (!revision.matches("[0-9a-f]{40}")) throw new IllegalStateException("RECIPE_BASE_REVISION");
        final byte[] manifestBytes;
        try {
            manifestBytes = readBytes(checkedPath(payload, "manifest.tsv"),
                    M3GpuToolJoinManifest.MAX_MANIFEST_BYTES);
        } catch (IOException failure) {
            // Preserve the canonical loader's resource-read failure boundary.
            throw new IllegalStateException("cannot read recipe resource: " + RESOURCE + "manifest.tsv", failure);
        }
        var manifest = M3GpuToolJoinManifest.load(RESOURCE, resource -> {
            if (!resource.startsWith(RESOURCE)) throw new IOException("RECIPE_RESOURCE_ROOT");
            if (resource.equals(RESOURCE + "manifest.tsv")) return new ByteArrayInputStream(manifestBytes);
            return Files.newInputStream(checkedPath(payload, resource.substring(RESOURCE.length())));
        }, checkpoint);
        var scan = manifest.scan();
        var changes = new ArrayList<Change>();
        for (var target : manifest.targets()) {
            M3GpuToolJoinManifest.check(checkpoint);
            if (target.removal()) throw new IllegalStateException("REMOVAL_NOT_ADMITTED:" + target.path());
            for (int slash = target.path().indexOf('/'); slash >= 0;
                 slash = target.path().indexOf('/', slash + 1)) {
                String ancestor = target.path().substring(0, slash);
                if (manifest.target(ancestor) != null) {
                    throw new IOException("OVERLAPPING_TARGETS:" + ancestor + ":" + target.path());
                }
            }
            Path path = checkedPath(root, target.path());
            String actual = M3GpuToolJoinManifest.ABSENT;
            if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                requireFile(path);
                actual = M3GpuToolJoinManifest.sha256(read(path));
                scan.observe(target.path(), actual);
            }
            if (!actual.equals(target.after())) {
                changes.add(new Change(target.path(), actual, target.after()));
            }
        }
        scan.requireAdmissible();
        String manifestHash;
        try { manifestHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(manifestBytes)); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
        // Only an admitted, immutable manifest can reach the mutation loop.
        if (apply) for (var change : changes) {
            M3GpuToolJoinManifest.check(checkpoint);
            var target = manifest.target(change.path());
            Path path = checkedPath(root, change.path());
            if (change.before().equals(M3GpuToolJoinManifest.ABSENT)) {
                if (!scan.missing(change.path())) throw new IllegalStateException("ADDITION_NOT_ABSENT");
                createParents(root, path.getParent());
                checkedPath(root, change.path());
                Files.writeString(path, target.text(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            } else {
                requireFile(path);
                scan.editing(change.path(), M3GpuToolJoinManifest.sha256(read(path)));
                Path temporary = Files.createTempFile(path.getParent(), ".m3-replay-", ".tmp");
                try {
                    Files.writeString(temporary, target.text(), StandardCharsets.UTF_8);
                    if (Files.getFileStore(path).supportsFileAttributeView("posix")) {
                        Files.setPosixFilePermissions(temporary, Files.getPosixFilePermissions(path));
                    }
                    checkedPath(root, change.path());
                    scan.editing(change.path(), M3GpuToolJoinManifest.sha256(read(path)));
                    Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                } finally {
                    Files.deleteIfExists(temporary);
                }
            }
            if (!M3GpuToolJoinManifest.sha256(read(path)).equals(target.after())) {
                throw new IllegalStateException("POSTIMAGE_MISMATCH:" + change.path());
            }
        }
        M3GpuToolJoinManifest.check(checkpoint);
        return new Report(revision, manifestHash, changes, apply);
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3 || !List.of("--plan", "--check", "--apply").contains(args[0])) {
            throw new IllegalArgumentException("--plan|--check|--apply REPOSITORY TEMPLATES");
        }
        var report = replay(Path.of(args[1]), Path.of(args[2]), args[0].equals("--apply"), null);
        if (args[0].equals("--check") && !report.changes().isEmpty()) {
            throw new IllegalStateException("RECIPE_NOT_APPLIED:" + report.changes().size());
        }
        System.out.println("RECIPE base=" + report.baseRevision() + " manifest=" + report.manifestHash()
                + " changes=" + report.changes().size() + " applied=" + report.applied()
                + " promotionAuthority=false");
        for (var change : report.changes()) {
            System.out.println(change.path() + "\t" + change.before() + "\t" + change.after());
        }
    }

    private static Path checkedRoot(Path root) throws IOException {
        Path absolute = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
        for (Path part = absolute; part != null; part = part.getParent()) {
            if (Files.isSymbolicLink(part)) throw new IOException("SYMLINK_ROOT:" + part);
        }
        if (!Files.isDirectory(absolute, LinkOption.NOFOLLOW_LINKS)) throw new IOException("MISSING_ROOT:" + absolute);
        return absolute.toRealPath(LinkOption.NOFOLLOW_LINKS);
    }

    private static Path checkedPath(Path root, String relative) throws IOException {
        if (relative.isEmpty() || relative.startsWith("/") || relative.indexOf('\\') >= 0
                || relative.indexOf(':') >= 0 || relative.codePoints().anyMatch(Character::isISOControl)) {
            throw new IOException("NONCANONICAL_PATH");
        }
        Path path = root;
        for (String part : relative.split("/", -1)) {
            if (part.isEmpty() || part.equals(".") || part.equals("..") || part.equalsIgnoreCase(".git")) {
                throw new IOException("NONCANONICAL_PATH");
            }
            path = path.resolve(part);
            if (Files.isSymbolicLink(path)) throw new IOException("SYMLINK_TARGET:" + relative);
            if (!path.equals(root.resolve(relative)) && Files.exists(path, LinkOption.NOFOLLOW_LINKS)
                    && !Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("NON_DIRECTORY_PARENT:" + relative);
            }
        }
        if (!path.startsWith(root)) throw new IOException("OUTSIDE_ROOT");
        return path;
    }

    private static void createParents(Path root, Path parent) throws IOException {
        if (parent.equals(root)) return;
        checkedPath(root, root.relativize(parent).toString().replace('\\', '/'));
        Files.createDirectories(parent);
    }

    private static void requireFile(Path path) throws IOException {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw new IOException("NOT_REGULAR_FILE:" + path);
    }

    private static String read(Path path) throws IOException {
        // Strict decoding makes the canonical text hash equal the original UTF-8 byte hash.
        byte[] bytes = readBytes(path, M3GpuToolJoinManifest.MAX_TEMPLATE_BYTES);
        return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
    }

    private static byte[] readBytes(Path path, int maximum) throws IOException {
        requireFile(path);
        byte[] bytes;
        try (var input = Files.newInputStream(path)) {
            bytes = input.readNBytes(maximum + 1);
        }
        if (bytes.length > maximum) throw new IOException("SOURCE_BYTE_BUDGET");
        return bytes;
    }
}
