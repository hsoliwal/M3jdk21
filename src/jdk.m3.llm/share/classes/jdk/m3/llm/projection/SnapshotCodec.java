// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.projection;

import jdk.m3.llm.internal.Sha256;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;

/** Canonical bounded snapshot codec. Digest validation precedes object publication. */
public final class SnapshotCodec {
    public static final int MAX_BYTES = 64 * 1024 * 1024;
    private static final int MAGIC = 0x53454931;
    private SnapshotCodec() {}

    static String payloadHash(InventorySnapshot snapshot) {
        return Sha256.digest(payload(snapshot));
    }

    static byte[] payload(InventorySnapshot snapshot) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(buffer)) {
                out.writeInt(MAGIC);
                out.writeInt(1);
                out.writeInt(snapshot.size());
                for (TextInventoryCell cell : snapshot.cells()) {
                    CellKey key = cell.key();
                    string(out, key.namespace());
                    string(out, key.row());
                    string(out, key.family());
                    string(out, key.qualifier());
                    out.writeLong(key.version());
                    string(out, cell.text());
                    string(out, cell.contentSha256());
                }
            }
            byte[] bytes = buffer.toByteArray();
            if (bytes.length > MAX_BYTES - 32) throw new IllegalArgumentException("SNAPSHOT_LIMIT");
            return bytes;
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    /** Existing trusted directory only; equal content is idempotent, never overwritten. */
    public static Path write(Path directory, InventorySnapshot snapshot) throws IOException {
        Path root = safe(directory);
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) throw new IOException("DIRECTORY_REQUIRED");
        Path target = root.resolve(snapshot.root() + ".sesi");
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            if (!read(target).root().equals(snapshot.root())) throw new IOException("EXISTING_SNAPSHOT_MISMATCH");
            return target;
        }
        byte[] data = payload(snapshot);
        byte[] trailer = HexFormat.of().parseHex(snapshot.root());
        try (FileChannel channel = FileChannel.open(target, StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
            for (byte[] bytes : new byte[][] {data, trailer}) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) channel.write(buffer);
            }
            channel.force(true);
        }
        return target;
    }

    public static InventorySnapshot read(Path path) throws IOException {
        Path source = safe(path);
        if (!Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) throw new IOException("REGULAR_FILE_REQUIRED");
        long length = Files.size(source);
        if (length < 44 || length > MAX_BYTES) throw new IOException("SNAPSHOT_LENGTH");
        byte[] data;
        try (var input = Files.newInputStream(source, LinkOption.NOFOLLOW_LINKS)) {
            data = input.readNBytes(MAX_BYTES + 1);
        }
        if (data.length != length) throw new IOException("SNAPSHOT_CHANGED_OR_OVERSIZED");
        byte[] body = Arrays.copyOf(data, data.length - 32);
        byte[] trailer = Arrays.copyOfRange(data, data.length - 32, data.length);
        if (!MessageDigest.isEqual(HexFormat.of().parseHex(Sha256.digest(body)), trailer)) {
            throw new IOException("SNAPSHOT_HASH_MISMATCH");
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(body))) {
            if (in.readInt() != MAGIC || in.readInt() != 1) throw new IOException("SNAPSHOT_SCHEMA");
            int count = in.readInt();
            if (count < 0 || count > InventorySnapshot.MAX_CELLS) throw new IOException("CELL_LIMIT");
            var cells = new ArrayList<TextInventoryCell>(count);
            CellKey previous = null;
            for (int i = 0; i < count; i++) {
                CellKey key = new CellKey(string(in, 512), string(in, 512), string(in, 512), string(in, 512), in.readLong());
                if (previous != null && previous.compareTo(key) >= 0) throw new IOException("NONCANONICAL_ORDER");
                cells.add(new TextInventoryCell(key, string(in, 1_048_576), string(in, 64)));
                previous = key;
            }
            if (in.available() != 0) throw new IOException("TRAILING_PAYLOAD");
            InventorySnapshot snapshot = new InventorySnapshot(cells);
            if (!snapshot.root().equals(HexFormat.of().formatHex(trailer))) throw new IOException("NONCANONICAL_PAYLOAD");
            return snapshot;
        } catch (IllegalArgumentException invalid) {
            throw new IOException("INVALID_SNAPSHOT", invalid);
        }
    }

    private static Path safe(Path path) throws IOException {
        Path absolute = path.toAbsolutePath().normalize();
        for (Path part = absolute; part != null; part = part.getParent()) {
            if (Files.isSymbolicLink(part)) throw new IOException("SYMLINK_REJECTED");
        }
        return absolute;
    }

    private static void string(DataOutputStream out, String value) throws IOException {
        byte[] bytes = EntropyValues.utf8(value);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static String string(DataInputStream in, int maximum) throws IOException {
        int length = in.readInt();
        if (length < 0 || length > maximum || length > in.available()) throw new IOException("STRING_LENGTH");
        return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(in.readNBytes(length))).toString();
    }
}
