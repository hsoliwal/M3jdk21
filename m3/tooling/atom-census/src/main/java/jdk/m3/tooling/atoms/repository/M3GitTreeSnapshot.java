/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.atoms.repository;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Captures exact committed Git tree entries and hashes blob bytes without consulting the working tree. */
public final class M3GitTreeSnapshot {
    private static final int COPY_BUFFER_SIZE = 64 * 1024;
    private static final int MAX_HEADER_BYTES = 4096;

    private final Path repositoryRoot;
    private final String revision;
    private final String commitId;
    private final String treeId;
    private final String objectFormat;
    private final List<M3GitTreeEntry> entries;

    private M3GitTreeSnapshot(Path repositoryRoot, String revision, String commitId, String treeId,
            String objectFormat, List<M3GitTreeEntry> entries) {
        this.repositoryRoot = repositoryRoot;
        this.revision = revision;
        this.commitId = commitId;
        this.treeId = treeId;
        this.objectFormat = objectFormat;
        this.entries = List.copyOf(entries);
    }

    /**
     * Captures the exact tree at {@code revision}. Uncommitted and ignored working-tree bytes cannot enter the result.
     *
     * @param repositoryPath repository root or any path inside it
     * @param revision Git revision resolving to a commit
     * @return immutable committed snapshot
     * @throws IOException when Git cannot resolve or read the snapshot
     */
    public static M3GitTreeSnapshot capture(Path repositoryPath, String revision) throws IOException {
        Objects.requireNonNull(repositoryPath, "repositoryPath");
        requireRevision(revision);
        Path requested = repositoryPath.toAbsolutePath().normalize();
        Path root = Path.of(runText(requested, "rev-parse", "--show-toplevel")).toRealPath();
        String commit = runText(root, "rev-parse", "--verify", "--end-of-options", revision + "^{commit}");
        String tree = runText(root, "rev-parse", "--verify", "--end-of-options", commit + "^{tree}");
        String format = objectFormat(root, commit);
        List<RawTreeEntry> rawEntries = parseTree(runBytes(root, "ls-tree", "-r", "-z", "--full-tree", commit));
        if (rawEntries.isEmpty()) {
            throw new IllegalArgumentException("committed tree contains no tracked entries: " + commit);
        }

        List<M3GitTreeEntry> entries = new ArrayList<>(rawEntries.size());
        Map<String, BlobDigest> blobDigests = new HashMap<>();
        try (BlobBatch batch = new BlobBatch(root)) {
            for (RawTreeEntry raw : rawEntries) {
                BlobDigest digest;
                if (raw.objectType().equals("blob")) {
                    digest = blobDigests.get(raw.objectId());
                    if (digest == null) {
                        digest = batch.hash(raw.objectId());
                        blobDigests.put(raw.objectId(), digest);
                    }
                } else if (raw.objectType().equals("commit")) {
                    byte[] pointer = ("gitlink " + raw.objectId() + "\n").getBytes(StandardCharsets.UTF_8);
                    digest = new BlobDigest(pointer.length, sha256(pointer));
                } else {
                    throw new IllegalArgumentException("unsupported ls-tree object type: " + raw.objectType());
                }
                entries.add(new M3GitTreeEntry(raw.mode(), raw.objectType(), raw.objectId(), raw.path(),
                        digest.size(), digest.sha256()));
            }
        }
        entries.sort(Comparator.comparing(M3GitTreeEntry::path));
        return new M3GitTreeSnapshot(root, revision, commit, tree, format, entries);
    }

    /**
     * Returns the resolved repository root.
     *
     * @return resolved repository root
     */
    public Path repositoryRoot() {
        return repositoryRoot;
    }

    /**
     * Returns the requested revision.
     *
     * @return requested revision
     */
    public String revision() {
        return revision;
    }

    /**
     * Returns the exact commit object identity.
     *
     * @return exact commit object identity
     */
    public String commitId() {
        return commitId;
    }

    /**
     * Returns the exact root-tree object identity.
     *
     * @return exact root-tree object identity
     */
    public String treeId() {
        return treeId;
    }

    /**
     * Returns the Git object format.
     *
     * @return Git object format, normally {@code sha1} or {@code sha256}
     */
    public String objectFormat() {
        return objectFormat;
    }

    /**
     * Returns tracked entries in normalized path order.
     *
     * @return tracked entries in normalized path order
     */
    public List<M3GitTreeEntry> entries() {
        return entries;
    }

    /**
     * Reads one exact committed blob from this snapshot.
     *
     * @param entry blob entry belonging to this snapshot
     * @return exact committed blob bytes
     * @throws IOException when Git cannot read the object or its identity no longer matches the snapshot
     */
    public byte[] readBlob(M3GitTreeEntry entry) throws IOException {
        Objects.requireNonNull(entry, "entry");
        if (!entries.contains(entry)) {
            throw new IllegalArgumentException("entry does not belong to this snapshot: " + entry.path());
        }
        if (!entry.objectType().equals("blob")) {
            throw new IllegalArgumentException("entry is not a blob: " + entry.path());
        }
        byte[] bytes = runBytes(repositoryRoot, "cat-file", "blob", entry.objectId());
        if (bytes.length != entry.sourceSize() || !sha256(bytes).equals(entry.sourceSha256())) {
            throw new IOException("Git blob identity changed while reading snapshot entry " + entry.path());
        }
        return bytes;
    }

    private static void requireRevision(String revision) {
        Objects.requireNonNull(revision, "revision");
        if (revision.isBlank() || revision.startsWith("-") || revision.length() > 512) {
            throw new IllegalArgumentException("revision must be a nonblank non-option Git revision");
        }
        for (int index = 0; index < revision.length(); index++) {
            if (Character.isISOControl(revision.charAt(index))) {
                throw new IllegalArgumentException("revision contains a control character");
            }
        }
    }

    private static String objectFormat(Path root, String commit) throws IOException {
        try {
            String format = runText(root, "rev-parse", "--show-object-format");
            if (format.equals("sha1") || format.equals("sha256")) {
                return format;
            }
            throw new IllegalArgumentException("unsupported Git object format: " + format);
        } catch (IOException exception) {
            if (commit.length() == 40) {
                return "sha1";
            }
            if (commit.length() == 64) {
                return "sha256";
            }
            throw exception;
        }
    }

    private static List<RawTreeEntry> parseTree(byte[] bytes) throws CharacterCodingException {
        List<RawTreeEntry> entries = new ArrayList<>();
        int start = 0;
        while (start < bytes.length) {
            int end = indexOf(bytes, (byte) 0, start);
            if (end < 0) {
                throw new IllegalArgumentException("git ls-tree output is missing a NUL terminator");
            }
            int tab = indexOf(bytes, (byte) '\t', start);
            if (tab < 0 || tab >= end) {
                throw new IllegalArgumentException("git ls-tree entry is missing its path separator");
            }
            String metadata = decodeUtf8(bytes, start, tab - start);
            String path = decodeUtf8(bytes, tab + 1, end - tab - 1);
            String[] cells = metadata.split(" ", -1);
            if (cells.length != 3) {
                throw new IllegalArgumentException("unexpected git ls-tree metadata: " + metadata);
            }
            entries.add(new RawTreeEntry(cells[0], cells[1], cells[2], path));
            start = end + 1;
        }
        return entries;
    }

    private static int indexOf(byte[] bytes, byte target, int start) {
        for (int index = start; index < bytes.length; index++) {
            if (bytes[index] == target) {
                return index;
            }
        }
        return -1;
    }

    private static String decodeUtf8(byte[] bytes, int offset, int length) throws CharacterCodingException {
        CharBuffer decoded = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes, offset, length));
        return decoded.toString();
    }

    private static String runText(Path root, String... arguments) throws IOException {
        byte[] bytes = runBytes(root, arguments);
        String text = decodeUtf8(bytes, 0, bytes.length);
        String line;
        if (text.endsWith("\r\n")) {
            line = text.substring(0, text.length() - 2);
        } else if (text.endsWith("\n")) {
            line = text.substring(0, text.length() - 1);
        } else {
            throw new IllegalArgumentException("Git command output is missing its line terminator");
        }
        if (line.isBlank() || line.indexOf('\n') >= 0 || line.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("Git command did not return exactly one nonblank line");
        }
        return line;
    }

    private static byte[] runBytes(Path root, String... arguments) throws IOException {
        List<String> command = new ArrayList<>();
        command.add(System.getProperty("m3.git.executable", "git"));
        command.add("-C");
        command.add(root.toString());
        command.addAll(List.of(arguments));
        ProcessBuilder builder = new ProcessBuilder(command);
        Map<String, String> environment = builder.environment();
        environment.put("LC_ALL", "C");
        environment.put("LANG", "C");
        environment.put("GIT_OPTIONAL_LOCKS", "0");
        Process process = builder.start();
        StreamCapture stderr = new StreamCapture(process.getErrorStream());
        Thread stderrThread = Thread.ofVirtual().name("m3-git-stderr").start(stderr);
        byte[] stdout = process.getInputStream().readAllBytes();
        int exit = waitFor(process);
        join(stderrThread);
        if (exit != 0) {
            throw new IOException("Git command failed (" + exit + "): " + String.join(" ", command)
                    + "\n" + stderr.text());
        }
        return stdout;
    }

    private static int waitFor(Process process) throws IOException {
        try {
            return process.waitFor();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted while waiting for Git", exception);
        }
    }

    private static void join(Thread thread) throws IOException {
        try {
            thread.join();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted while reading Git diagnostics", exception);
        }
    }

    private static String sha256(byte[] bytes) {
        return HexFormat.of().formatHex(sha256Digest().digest(bytes));
    }

    private static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("SHA-256 is required by the JDK", exception);
        }
    }

    private record RawTreeEntry(String mode, String objectType, String objectId, String path) {
    }

    private record BlobDigest(long size, String sha256) {
    }

    private static final class BlobBatch implements AutoCloseable {
        private final Process process;
        private final BufferedOutputStream input;
        private final BufferedInputStream output;
        private final StreamCapture stderr;
        private final Thread stderrThread;
        private boolean closed;

        BlobBatch(Path root) throws IOException {
            List<String> command = List.of(System.getProperty("m3.git.executable", "git"), "-C", root.toString(),
                    "cat-file", "--batch");
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.environment().put("LC_ALL", "C");
            builder.environment().put("LANG", "C");
            builder.environment().put("GIT_OPTIONAL_LOCKS", "0");
            process = builder.start();
            input = new BufferedOutputStream(process.getOutputStream());
            output = new BufferedInputStream(process.getInputStream());
            stderr = new StreamCapture(process.getErrorStream());
            stderrThread = Thread.ofVirtual().name("m3-git-cat-file-stderr").start(stderr);
        }

        BlobDigest hash(String objectId) throws IOException {
            if (closed) {
                throw new IllegalStateException("blob batch is closed");
            }
            input.write(objectId.getBytes(StandardCharsets.US_ASCII));
            input.write('\n');
            input.flush();
            String header = readAsciiLine(output);
            String[] cells = header.split(" ", -1);
            if (cells.length != 3 || !cells[0].equals(objectId) || !cells[1].equals("blob")) {
                throw new IOException("unexpected git cat-file response: " + header);
            }
            long size;
            try {
                size = Long.parseLong(cells[2]);
            } catch (NumberFormatException exception) {
                throw new IOException("invalid git cat-file size: " + header, exception);
            }
            MessageDigest digest = sha256Digest();
            byte[] buffer = new byte[COPY_BUFFER_SIZE];
            long remaining = size;
            while (remaining > 0) {
                int read = output.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read < 0) {
                    throw new IOException("git cat-file ended inside blob " + objectId);
                }
                digest.update(buffer, 0, read);
                remaining -= read;
            }
            if (output.read() != '\n') {
                throw new IOException("git cat-file response lacks trailing LF for " + objectId);
            }
            return new BlobDigest(size, HexFormat.of().formatHex(digest.digest()));
        }

        @Override
        public void close() throws IOException {
            if (closed) {
                return;
            }
            closed = true;
            input.close();
            int exit = waitFor(process);
            join(stderrThread);
            output.close();
            if (exit != 0) {
                throw new IOException("git cat-file --batch failed (" + exit + "): " + stderr.text());
            }
        }

        private static String readAsciiLine(InputStream input) throws IOException {
            ByteArrayOutputStream line = new ByteArrayOutputStream();
            while (line.size() <= MAX_HEADER_BYTES) {
                int value = input.read();
                if (value < 0) {
                    throw new IOException("git cat-file ended before its response header");
                }
                if (value == '\n') {
                    return line.toString(StandardCharsets.US_ASCII);
                }
                line.write(value);
            }
            throw new IOException("git cat-file response header exceeds " + MAX_HEADER_BYTES + " bytes");
        }
    }

    private static final class StreamCapture implements Runnable {
        private final InputStream input;
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private IOException failure;

        StreamCapture(InputStream input) {
            this.input = input;
        }

        @Override
        public void run() {
            try (InputStream stream = input; OutputStream destination = bytes) {
                stream.transferTo(destination);
            } catch (IOException exception) {
                failure = exception;
            }
        }

        String text() throws IOException {
            if (failure != null) {
                throw failure;
            }
            return bytes.toString(StandardCharsets.UTF_8).strip();
        }
    }
}
