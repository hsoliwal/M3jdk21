/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * Adapted for M3JDK21 from cpullm-java/src/main/java/com/synexia/cpullm/facets/FacetCorpus.java
 * Synexia revision: 2ab439480ecb0643615e85b42def954dbf3b5627.
 * Namespace mapping preserves algorithms and original schema/hash identities.
 */
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.facets;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Immutable source/range authority for faceted projection; candidate scores are not truth scores. */
public final class FacetCorpus {
    public static final int MAX_ATOMS = 65_536;
    public static final int MAX_BYTES = 32 * 1024 * 1024;
    public static final String VERSION = "SX-FACET-1";

    public record Source(String id, byte[] bytes) {
        public Source {
            name(id);
            Objects.requireNonNull(bytes, "bytes");
            if (bytes.length > MAX_BYTES) throw new IllegalArgumentException("source byte limit");
            bytes = bytes.clone();
            decode(bytes, 0, bytes.length);
        }
        @Override public byte[] bytes() { return bytes.clone(); }
        public String sha256() { return HexFormat.of().formatHex(sha().digest(bytes)); }
    }

    /** Byte offsets address the supplied Source, not a guessed original document or token stream. */
    public record Slice(String source, int start, int end, long facets, int priority,
                        boolean mandatory, String conflictGroup) {
        public Slice {
            name(source);
            Objects.requireNonNull(conflictGroup, "conflictGroup");
            if (!conflictGroup.isEmpty()) name(conflictGroup);
            if (start < 0 || end <= start || end - start > 65_536) throw new IllegalArgumentException("slice byte bounds");
            if (priority < 0 || priority > 1_000_000) throw new IllegalArgumentException("candidate priority bounds");
        }
    }

    private final List<Source> sources;
    private final Map<String, Source> bySource;
    private final List<String> vocabulary;
    private final List<Slice> slices;
    private final String[] ids, texts, rendered;
    /** Immutable UTF-8 byte-cost sidecar: at most four bytes per admitted evidence row. */
    private final int[] renderedUtf8ByteLengths;
    private final int[][] facetRows;
    final int[][] groups;
    final int[] rowGroup, groupBytes, groupPriority;
    final long[] groupFacets;
    final boolean[] groupMandatory;
    private final String root;
    private final int payloadBytes;

    public FacetCorpus(List<Source> inputSources, List<String> facetVocabulary,
                       List<Slice> inputSlices, int byteBudget) {
        Objects.requireNonNull(inputSources, "sources");
        Objects.requireNonNull(facetVocabulary, "facets");
        Objects.requireNonNull(inputSlices, "slices");
        if (byteBudget < 0 || byteBudget > MAX_BYTES || inputSources.size() > MAX_ATOMS
                || inputSlices.size() > MAX_ATOMS || facetVocabulary.size() > 64) {
            throw new IllegalArgumentException("corpus admission bounds");
        }
        vocabulary = List.copyOf(facetVocabulary);
        var names = new java.util.HashSet<String>();
        for (String facet : vocabulary) { name(facet); if (!names.add(facet)) throw new IllegalArgumentException("duplicate facet"); }
        var sorted = new TreeMap<String, Source>();
        long bytes = 0;
        for (Source source : inputSources) {
            Objects.requireNonNull(source, "source");
            if (sorted.putIfAbsent(source.id(), source) != null) throw new IllegalArgumentException("duplicate source ID");
            bytes += source.bytes.length;
            if (bytes > byteBudget) throw new IllegalArgumentException("source payload exceeds budget");
        }
        sources = List.copyOf(sorted.values()); bySource = Map.copyOf(sorted);
        var ordered = new ArrayList<>(inputSlices);
        ordered.sort(Comparator.comparing(Slice::source).thenComparingInt(Slice::start).thenComparingInt(Slice::end));
        slices = List.copyOf(ordered);
        ids = new String[slices.size()]; texts = new String[slices.size()]; rendered = new String[slices.size()];
        renderedUtf8ByteLengths = new int[slices.size()];
        int[] counts = new int[vocabulary.size()];
        long validMask = vocabulary.size() == 64 ? -1L : (1L << vocabulary.size()) - 1;
        var seal = new Seal(VERSION).number(byteBudget).number(vocabulary.size());
        vocabulary.forEach(seal::text);
        seal.number(sources.size());
        var sourceHashes = new HashMap<String, String>();
        for (Source source : sources) {
            String identity = source.sha256(); sourceHashes.put(source.id(), identity); seal.text(source.id()).text(identity);
        }
        seal.number(slices.size());
        for (int row = 0; row < slices.size(); row++) {
            cancelled();
            Slice slice = slices.get(row);
            Source source = bySource.get(slice.source());
            if (source == null || slice.end() > source.bytes.length || (slice.facets() & ~validMask) != 0) {
                throw new IllegalArgumentException("unknown source, range, or facet bit");
            }
            if (row > 0) {
                Slice previous = slices.get(row - 1);
                if (previous.source().equals(slice.source()) && previous.start() == slice.start() && previous.end() == slice.end()) {
                    throw new IllegalArgumentException("duplicate evidence range; reconcile annotations explicitly");
                }
            }
            texts[row] = decode(source.bytes, slice.start(), slice.end());
            ids[row] = sliceSeal(new Seal("SX-FACET-ATOM-1").text(sourceHashes.get(source.id())), slice).finish();
            rendered[row] = texts[row] + " [" + ids[row] + "]\n";
            renderedUtf8ByteLengths[row] = utf8(rendered[row]).length;
            bytes += renderedUtf8ByteLengths[row];
            if (bytes > byteBudget) throw new IllegalArgumentException("source plus rendered-payload budget exceeded");
            seal.text(ids[row]);
            for (int bit = 0; bit < counts.length; bit++) if ((slice.facets() & (1L << bit)) != 0) counts[bit]++;
        }
        payloadBytes = (int) bytes; root = seal.finish();
        var grouped = new TreeMap<String, List<Integer>>();
        for (int row = 0; row < slices.size(); row++) {
            Slice slice = slices.get(row);
            String key = slice.conflictGroup().isEmpty() ? "atom:" + ids[row] : "conflict:" + slice.conflictGroup();
            grouped.computeIfAbsent(key, unused -> new ArrayList<>()).add(row);
        }
        groups = new int[grouped.size()][]; rowGroup = new int[slices.size()];
        groupBytes = new int[groups.length]; groupPriority = new int[groups.length];
        groupFacets = new long[groups.length]; groupMandatory = new boolean[groups.length];
        int group = 0;
        for (List<Integer> rows : grouped.values()) {
            groups[group] = rows.stream().mapToInt(Integer::intValue).toArray();
            for (int row : groups[group]) {
                rowGroup[row] = group;
                groupBytes[group] = Math.addExact(groupBytes[group], renderedUtf8ByteLengths[row]);
                groupPriority[group] = Math.max(groupPriority[group], slices.get(row).priority());
                groupFacets[group] |= slices.get(row).facets();
                groupMandatory[group] |= slices.get(row).mandatory();
            }
            group++;
        }
        facetRows = new int[counts.length][];
        for (int bit = 0; bit < counts.length; bit++) facetRows[bit] = new int[counts[bit]];
        Arrays.fill(counts, 0);
        for (int row = 0; row < slices.size(); row++) {
            for (int bit = 0; bit < counts.length; bit++) if ((slices.get(row).facets() & (1L << bit)) != 0) facetRows[bit][counts[bit]++] = row;
        }
    }

    public String root() { return root; }
    public int size() { return slices.size(); }
    public int retainedUtf8PayloadBytes() { return payloadBytes; }
    public List<String> vocabulary() { return vocabulary; }
    public List<Source> sources() { return sources; }
    public List<Slice> slices() { return slices; }
    public String evidenceId(int row) { return ids[Objects.checkIndex(row, size())]; }
    public String expand(int row) { return texts[Objects.checkIndex(row, size())]; }
    public byte[] expandSource(String source) {
        Source value = bySource.get(source); if (value == null) throw new IllegalArgumentException("unknown source"); return value.bytes();
    }
    public long mask(String... facets) {
        long mask = 0;
        for (String facet : facets) {
            int bit = vocabulary.indexOf(facet); if (bit < 0) throw new IllegalArgumentException("unknown facet: " + facet); mask |= 1L << bit;
        }
        return mask;
    }
    public int[] rowsForFacet(String facet) {
        int bit = vocabulary.indexOf(facet); if (bit < 0) throw new IllegalArgumentException("unknown facet"); return facetRows[bit].clone();
    }
    String rendered(int row) { return rendered[row]; }
    int renderedBytes(int row) { return renderedUtf8ByteLengths[row]; }
    void requireMask(long mask) {
        long valid = vocabulary.size() == 64 ? -1L : (1L << vocabulary.size()) - 1;
        if ((mask & ~valid) != 0) throw new IllegalArgumentException("unknown request facet bit");
    }
    static Seal sliceSeal(Seal hash, Slice s) {
        return hash.text(s.source()).number(s.start()).number(s.end()).number(s.facets())
                .number(s.priority()).number(s.mandatory() ? 1 : 0).text(s.conflictGroup());
    }
    static void name(String name) {
        Objects.requireNonNull(name, "name");
        if (name.isBlank() || utf8(name).length > 1024 || name.indexOf('\n') >= 0 || name.indexOf('\r') >= 0 || name.indexOf('\t') >= 0) {
            throw new IllegalArgumentException("identifier length or control character");
        }
    }
    static String decode(byte[] bytes, int start, int end) {
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes, start, end - start)).toString();
        } catch (CharacterCodingException e) { throw new IllegalArgumentException("invalid UTF-8 boundary", e); }
    }
    static byte[] utf8(String text) {
        try {
            ByteBuffer b = StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
                    .encode(java.nio.CharBuffer.wrap(text)); byte[] bytes = new byte[b.remaining()]; b.get(bytes); return bytes;
        } catch (CharacterCodingException e) { throw new IllegalArgumentException("unpaired surrogate", e); }
    }
    static void cancelled() {
        if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException("facet projection interrupted");
    }
    static MessageDigest sha() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    static final class Seal {
        private final MessageDigest hash = sha();
        Seal(String domain) { text(domain); }
        Seal number(long value) { for (int i = 0; i < 8; i++) hash.update((byte) (value >>> (i * 8))); return this; }
        Seal text(String text) { byte[] bytes = utf8(text); number(bytes.length); hash.update(bytes); return this; }
        String finish() { return HexFormat.of().formatHex(hash.digest()); }
    }
}
