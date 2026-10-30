/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * Adapted for M3JDK21 from cpullm-java/src/main/java/com/synexia/cpullm/facets/FacetSnapshot.java
 * Synexia revision: 2ab439480ecb0643615e85b42def954dbf3b5627.
 * Namespace mapping preserves algorithms and original schema/hash identities.
 */
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.facets;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/** Bounded, source-complete portable snapshot. Indexes are rebuilt and the semantic root is checked. */
public final class FacetSnapshot {
    private static final int MAGIC = 0x53464631;
    private FacetSnapshot() { }
    public static void write(Path path, FacetCorpus corpus, int originalBudget) throws IOException {
        // The source root includes admission policy; require the caller's original exact budget.
        FacetCorpus reconstructed = new FacetCorpus(corpus.sources(), corpus.vocabulary(), corpus.slices(), originalBudget);
        if (!reconstructed.root().equals(corpus.root())) throw new IllegalArgumentException("snapshot policy differs");
        safeParent(path);
        try (var output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path, StandardOpenOption.CREATE_NEW)))) {
            output.writeInt(MAGIC); output.writeInt(originalBudget); string(output, corpus.root());
            output.writeInt(corpus.vocabulary().size()); for (String facet : corpus.vocabulary()) string(output, facet);
            output.writeInt(corpus.sources().size());
            for (FacetCorpus.Source source : corpus.sources()) {
                string(output, source.id()); byte[] bytes = source.bytes(); output.writeInt(bytes.length); output.write(bytes);
            }
            output.writeInt(corpus.size());
            for (FacetCorpus.Slice slice : corpus.slices()) {
                string(output, slice.source()); output.writeInt(slice.start()); output.writeInt(slice.end());
                output.writeLong(slice.facets()); output.writeInt(slice.priority()); output.writeBoolean(slice.mandatory()); string(output, slice.conflictGroup());
            }
        }
    }
    public static FacetCorpus read(Path path) throws IOException {
        safeParent(path);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.size(path) > 128L * 1024 * 1024) throw new IOException("snapshot file limit/type");
        try (var input = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            if (input.readInt() != MAGIC) throw new IOException("snapshot magic");
            int budget = count(input, FacetCorpus.MAX_BYTES); String expected = string(input, 64);
            if (!expected.matches("[0-9a-f]{64}")) throw new IOException("snapshot root");
            int facets = count(input, 64); List<String> vocabulary = new ArrayList<>();
            for (int i = 0; i < facets; i++) vocabulary.add(string(input, 1024));
            int sourceCount = count(input, FacetCorpus.MAX_ATOMS); List<FacetCorpus.Source> sources = new ArrayList<>(); long total = 0;
            for (int i = 0; i < sourceCount; i++) {
                String id = string(input, 1024); int n = count(input, budget); total += n;
                if (total > budget) throw new IOException("snapshot source aggregate budget");
                byte[] bytes = input.readNBytes(n); if (bytes.length != n) throw new EOFException("source bytes");
                sources.add(new FacetCorpus.Source(id, bytes));
            }
            int size = count(input, FacetCorpus.MAX_ATOMS); List<FacetCorpus.Slice> slices = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                String source = string(input, 1024); int start = input.readInt(), end = input.readInt();
                long mask = input.readLong(); int priority = input.readInt(), flag = input.readUnsignedByte();
                if (flag > 1) throw new IOException("noncanonical mandatory flag");
                slices.add(new FacetCorpus.Slice(source, start, end, mask, priority, flag == 1, string(input, 1024)));
            }
            if (input.read() != -1) throw new IOException("trailing snapshot data");
            FacetCorpus corpus = new FacetCorpus(sources, vocabulary, slices, budget);
            if (!expected.equals(corpus.root())) throw new IOException("snapshot reconstruction differs");
            return corpus;
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) { throw new IOException("snapshot rejected", e); }
    }
    static void safeParent(Path path) throws IOException {
        for (Path at = path.toAbsolutePath().normalize(); at != null; at = at.getParent()) {
            if (Files.isSymbolicLink(at)) throw new IOException("symlink path rejected");
        }
        if (!Files.isDirectory(path.toAbsolutePath().getParent())) throw new IOException("parent directory absent");
    }
    private static int count(DataInputStream input, int limit) throws IOException {
        int n = input.readInt(); if (n < 0 || n > limit) throw new IOException("snapshot count limit"); return n;
    }
    private static String string(DataInputStream input, int maximum) throws IOException {
        int n = count(input, maximum); byte[] bytes = input.readNBytes(n); if (bytes.length != n) throw new EOFException("string");
        return FacetCorpus.decode(bytes, 0, bytes.length);
    }
    private static void string(DataOutputStream output, String value) throws IOException {
        byte[] bytes = FacetCorpus.utf8(value); output.writeInt(bytes.length); output.write(bytes);
    }
}
