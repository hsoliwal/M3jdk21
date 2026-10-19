// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable one-language dictionary snapshot backed by UTF-8 arenas and primitive metadata arrays.
 *
 * <p>Frozen UTF-8 byte lookup first uses a precomputed raw multidimensional direct plane:
 * leading bytes + exact bounded length, optional byte2..byte7 refinement and inline exact
 * 64-bit key lanes. Ordinary <=32-byte direct hits and direct misses therefore avoid hashing.
 * Residual prefix ambiguity and longer tokens fall through to the fixed collision-free exact
 * lookup. Runtime lookup never linearly probes and its candidate count does not grow with
 * dictionary cardinality. Prefix and suffix lookup return frozen coordinate ranges without
 * materializing dictionary spellings. Ref equality/order, length/hash metadata, concept tests
 * and frequency reads are O(1).
 */
public final class MIndexDictionary implements MIndexLexicon {
    public static final String SPACE = " ";
    public static final int SPACE_RANK = 0;
    private static final long HASH_SEED = 0x4d494e4458444943L;

    public record TokenEntry(String surface, long frequency, SemanticRef semantic) {
        public TokenEntry {
            Objects.requireNonNull(surface, "surface");
            semantic = semantic == null ? SemanticRef.NONE : semantic;
            if (frequency < 0) throw new IllegalArgumentException("frequency must be >= 0");
        }

        public static TokenEntry of(String surface, long frequency, long conceptId, int flags) {
            return new TokenEntry(surface, frequency, SemanticRef.of(conceptId, flags));
        }
    }

    private final int dictionaryVersion;
    private final int languageId;
    private final String languageTag;
    private final DictionaryProvenance provenance;
    private final byte[] arena;
    private final int[] offsets;
    private final byte[] reverseArena;
    private final int[] reverseOffsets;
    private final int[] prefixRanks;
    private final int[] suffixRanks;
    private final int[] prefixCoordinatesByRank;
    private final int[] suffixCoordinatesByRank;
    private final int[] prefixEndsExclusiveByRank;
    private final int[] suffixEndsExclusiveByRank;
    private final long[] frequencies;
    private final long[] semanticBits;
    private final long[] hashes;
    private final int[] utf16Lengths;
    private final int[] codePointLengths;
    private final int[] javaHashes;
    private final int[] javaHashMultipliers;
    private final MIndexDirectInlineKey32 directLookup;
    private final MIndexExactLookup exactLookup;
    private final MIndexPackedLexemeIndex packedLexemes;

    private MIndexDictionary(
            int dictionaryVersion,
            int languageId,
            String languageTag,
            DictionaryProvenance provenance,
            byte[] arena,
            int[] offsets,
            byte[] reverseArena,
            int[] reverseOffsets,
            int[] prefixRanks,
            int[] suffixRanks,
            int[] prefixCoordinatesByRank,
            int[] suffixCoordinatesByRank,
            int[] prefixEndsExclusiveByRank,
            int[] suffixEndsExclusiveByRank,
            long[] frequencies,
            long[] semanticBits,
            long[] hashes,
            int[] utf16Lengths,
            int[] codePointLengths,
            int[] javaHashes,
            int[] javaHashMultipliers,
            MIndexDirectInlineKey32 directLookup,
            MIndexExactLookup exactLookup,
            MIndexPackedLexemeIndex packedLexemes) {
        this.dictionaryVersion = dictionaryVersion;
        this.languageId = languageId;
        this.languageTag = languageTag;
        this.provenance = provenance;
        this.arena = arena;
        this.offsets = offsets;
        this.reverseArena = reverseArena;
        this.reverseOffsets = reverseOffsets;
        this.prefixRanks = prefixRanks;
        this.suffixRanks = suffixRanks;
        this.prefixCoordinatesByRank = prefixCoordinatesByRank;
        this.suffixCoordinatesByRank = suffixCoordinatesByRank;
        this.prefixEndsExclusiveByRank = prefixEndsExclusiveByRank;
        this.suffixEndsExclusiveByRank = suffixEndsExclusiveByRank;
        this.frequencies = frequencies;
        this.semanticBits = semanticBits;
        this.hashes = hashes;
        this.utf16Lengths = utf16Lengths;
        this.codePointLengths = codePointLengths;
        this.javaHashes = javaHashes;
        this.javaHashMultipliers = javaHashMultipliers;
        this.directLookup = Objects.requireNonNull(directLookup, "directLookup");
        this.exactLookup = Objects.requireNonNull(exactLookup, "exactLookup");
        this.packedLexemes = Objects.requireNonNull(packedLexemes, "packedLexemes");
    }

    public static MIndexDictionary freeze(
            int dictionaryVersion,
            int languageId,
            String languageTag,
            DictionaryProvenance provenance,
            Collection<TokenEntry> supplied,
            MIndexProgressMonitor monitor) {
        Objects.requireNonNull(provenance, "provenance");
        Objects.requireNonNull(supplied, "supplied");
        monitor = monitor == null ? MIndexProgressMonitor.none() : monitor;
        LexicalRef.of(dictionaryVersion, languageId, 0L);
        String normalizedTag = Objects.requireNonNull(languageTag, "languageTag").strip();
        if (normalizedTag.isEmpty()) throw new IllegalArgumentException("languageTag must not be blank");

        monitor.begin("freeze dictionary " + normalizedTag, supplied.size());
        try {
            ArrayList<TokenEntry> staged = new ArrayList<>(
                    Math.addExact(supplied.size(), 1));
            staged.add(new TokenEntry(SPACE, Long.MAX_VALUE, SemanticRef.NONE));
            for (TokenEntry entry : supplied) {
                monitor.checkCancelled();
                Objects.requireNonNull(entry, "token entry");
                if (entry.surface().isEmpty()) {
                    throw new IllegalArgumentException(
                            "empty token is not allowed; SPACE is explicit rank 0");
                }
                staged.add(entry);
                monitor.worked(1);
            }

            /*
             * Sort once, then fold adjacent equal spellings. List.sort is stable, so duplicate
             * semantic selection preserves the previous insertion-order merge behavior without a
             * LinkedHashMap or one node per spelling.
             */
            staged.sort(Comparator.comparing(
                    TokenEntry::surface, MIndexDictionary::compareCodePoint));
            List<TokenEntry> ordered = foldEntries(staged);

            int count = ordered.size();
            int byteCount = 0;
            for (TokenEntry entry : ordered) {
                byteCount = Math.addExact(byteCount, entry.surface().getBytes(StandardCharsets.UTF_8).length);
            }

            byte[] arena = new byte[byteCount];
            int[] offsets = new int[count + 1];
            byte[] reverseArena = new byte[byteCount];
            int[] reverseOffsets = new int[count + 1];
            int[] prefixRanks = new int[count];
            int[] suffixRanks = new int[count];
            int[] prefixCoordinatesByRank = new int[count];
            int[] suffixCoordinatesByRank = new int[count];
            int[] prefixEndsExclusiveByRank = new int[count];
            int[] suffixEndsExclusiveByRank = new int[count];
            long[] frequencies = new long[count];
            long[] semanticBits = new long[count];
            long[] hashes = new long[count];
            int[] utf16Lengths = new int[count];
            int[] codePointLengths = new int[count];
            int[] javaHashes = new int[count];
            int[] javaHashMultipliers = new int[count];

            int cursor = 0;
            int reverseCursor = 0;
            for (int rank = 0; rank < count; rank++) {
                TokenEntry entry = ordered.get(rank);
                String surface = entry.surface();
                byte[] utf8 = surface.getBytes(StandardCharsets.UTF_8);
                offsets[rank] = cursor;
                System.arraycopy(utf8, 0, arena, cursor, utf8.length);
                cursor += utf8.length;
                offsets[rank + 1] = cursor;

                byte[] reverseUtf8 = reverseCodePoints(surface).getBytes(StandardCharsets.UTF_8);
                reverseOffsets[rank] = reverseCursor;
                System.arraycopy(reverseUtf8, 0, reverseArena, reverseCursor, reverseUtf8.length);
                reverseCursor += reverseUtf8.length;
                reverseOffsets[rank + 1] = reverseCursor;
                prefixRanks[rank] = rank;
                suffixRanks[rank] = rank;

                frequencies[rank] = entry.frequency();
                semanticBits[rank] = entry.semantic().bits();
                hashes[rank] = hash64(utf8);
                utf16Lengths[rank] = surface.length();
                codePointLengths[rank] = surface.codePointCount(0, surface.length());
                javaHashes[rank] = surface.hashCode();
                javaHashMultipliers[rank] = TextMetadata.pow31(surface.length());
            }
            sortRanksByBytes(prefixRanks, arena, offsets);
            sortRanksByBytes(suffixRanks, reverseArena, reverseOffsets);
            invertCoordinates(prefixRanks, prefixCoordinatesByRank);
            invertCoordinates(suffixRanks, suffixCoordinatesByRank);
            compileRelationEnds(
                    arena, offsets, prefixRanks, prefixCoordinatesByRank, prefixEndsExclusiveByRank);
            compileRelationEnds(
                    reverseArena,
                    reverseOffsets,
                    suffixRanks,
                    suffixCoordinatesByRank,
                    suffixEndsExclusiveByRank);

            MIndexDirectInlineKey32 directLookup =
                    MIndexDirectInlineKey32.build(arena, offsets);
            MIndexExactLookup exactLookup = MIndexExactLookup.build(hashes, arena, offsets);
            List<String> packedSurfaces =
                    ordered.stream().map(TokenEntry::surface).toList();
            MIndexPackedLexemeIndex packedLexemes =
                    MIndexPackedLexemeIndex.compile(
                            packedSurfaces,
                            MIndexLexemeCodec.derive(
                                    "dictionary:" + normalizedTag, packedSurfaces));

            return new MIndexDictionary(
                    dictionaryVersion, languageId, normalizedTag, provenance,
                    arena, offsets, reverseArena, reverseOffsets, prefixRanks, suffixRanks,
                    prefixCoordinatesByRank, suffixCoordinatesByRank,
                    prefixEndsExclusiveByRank, suffixEndsExclusiveByRank,
                    frequencies, semanticBits, hashes,
                    utf16Lengths, codePointLengths, javaHashes, javaHashMultipliers,
                    directLookup, exactLookup, packedLexemes);
        } finally {
            monitor.done();
        }
    }

    public static MIndexDictionary freeze(
            int dictionaryVersion,
            int languageId,
            String languageTag,
            DictionaryProvenance provenance,
            Collection<TokenEntry> supplied) {
        return freeze(dictionaryVersion, languageId, languageTag, provenance, supplied, MIndexProgressMonitor.none());
    }

    /** Old snapshots remain immutable; a new version is re-frozen canonically. */
    public MIndexDictionary evolve(
            int newVersion,
            Collection<TokenEntry> additions,
            DictionaryProvenance newProvenance,
            MIndexProgressMonitor monitor) {
        if (newVersion == dictionaryVersion) {
            throw new IllegalArgumentException("evolution requires a new dictionary version");
        }
        List<TokenEntry> all = new ArrayList<>(entries());
        all.addAll(additions);
        return freeze(newVersion, languageId, languageTag, newProvenance, all, monitor);
    }

    @Override public int dictionaryVersion() { return dictionaryVersion; }
    @Override public int languageId() { return languageId; }
    @Override public String languageTag() { return languageTag; }
    @Override public DictionaryProvenance provenance() { return provenance; }
    @Override public int size() { return frequencies.length; }
    @Override public LexicalRef spaceRef() { return ref(SPACE_RANK); }

    @Override
    public LexicalRef ref(int rank) {
        Objects.checkIndex(rank, size());
        return LexicalRef.of(dictionaryVersion, languageId, rank);
    }

    @Override
    public Optional<LexicalRef> lookup(String surface) {
        Objects.requireNonNull(surface, "surface");
        int packedRank = packedLexemes.lookup(surface);
        if (packedRank != MIndexPackedLexemeIndex.UNENCODABLE) {
            return packedRank < 0 ? Optional.empty() : Optional.of(ref(packedRank));
        }
        byte[] query = surface.getBytes(StandardCharsets.UTF_8);
        int rank = lookupRankUtf8(query, 0, query.length);
        return rank < 0 ? Optional.empty() : Optional.of(ref(rank));
    }

    /** Number of dictionary spellings addressable by the bounded 128-bit coordinate plane. */
    public int packedLexemeCount() {
        return packedLexemes.packedEntryCount();
    }

    public long packedLexemePayloadBytes() {
        return packedLexemes.primitivePayloadBytes();
    }

    public long packedLexemeCodecFingerprint64() {
        return packedLexemes.codec().fingerprint64();
    }

    /**
     * Precompute sparse position/symbol and exact-length filters over this dictionary's packed
     * lexeme ranks. Returned value IDs are dictionary ranks.
     */
    public MIndexLexemeMatrixIndex compilePackedLexemeMatrix() {
        return packedLexemes.compileMatrix();
    }

    @Override
    public Optional<LexicalRef> lookupUtf8(byte[] utf8, int offset, int length) {
        int rank = lookupRankUtf8(utf8, offset, length);
        return rank < 0 ? Optional.empty() : Optional.of(ref(rank));
    }

    /**
     * Primitive exact lookup for parser/compiler hot paths.
     *
     * <p>The returned value is the lexical rank or {@code -1}. Known-token resolution performs one
     * perfect-layout candidate read followed by exact verification and never allocates an Optional
     * or LexicalRef.</p>
     */
    @Override
    public int lookupRankUtf8(byte[] utf8, int offset, int length) {
        Objects.requireNonNull(utf8, "utf8");
        Objects.checkFromIndexSize(offset, length, utf8.length);

        int direct = directLookup.candidate(utf8, offset, length);
        if (direct >= 0) return direct;
        if (direct == MIndexDirectInlineKey32.MISS) return -1;

        long contentHash64 = hash64(utf8, offset, length);
        return exactLookupRankUtf8Prehashed(
                utf8, offset, length, contentHash64);
    }

    /**
     * Exact lookup when the caller already computed the canonical MIndex content hash.
     *
     * <p>This is intended for byte scanners that retain token hashes while building source tapes.
     * A wrong supplied hash fails closed because both the frozen hash and source bytes are verified.
     */
    @Override
    public int lookupRankUtf8Prehashed(
            byte[] utf8, int offset, int length, long contentHash64) {
        Objects.requireNonNull(utf8, "utf8");
        Objects.checkFromIndexSize(offset, length, utf8.length);

        int direct = directLookup.candidate(utf8, offset, length);
        if (direct >= 0) {
            return hashes[direct] == contentHash64 ? direct : -1;
        }
        if (direct == MIndexDirectInlineKey32.MISS) return -1;

        return exactLookupRankUtf8Prehashed(
                utf8, offset, length, contentHash64);
    }

    private int exactLookupRankUtf8Prehashed(
            byte[] utf8, int offset, int length, long contentHash64) {
        int rank = exactLookup.candidateRank(utf8, offset, length, contentHash64);
        if (rank < 0
                || hashes[rank] != contentHash64
                || !bytesEqual(arena, offsets, rank, utf8, offset, length)) {
            return -1;
        }
        return rank;
    }

    @Override
    public LexicalRange prefixRange(String prefix) {
        byte[] query = Objects.requireNonNull(prefix, "prefix").getBytes(StandardCharsets.UTF_8);
        return rangeForPrefix(arena, offsets, prefixRanks, query);
    }

    @Override
    public LexicalRange suffixRange(String suffix) {
        byte[] query = reverseCodePoints(Objects.requireNonNull(suffix, "suffix"))
                .getBytes(StandardCharsets.UTF_8);
        return rangeForPrefix(reverseArena, reverseOffsets, suffixRanks, query);
    }

    @Override
    public LexicalRef prefixRefAt(int prefixCoordinate) {
        return ref(prefixRanks[Objects.checkIndex(prefixCoordinate, prefixRanks.length)]);
    }

    @Override
    public LexicalRef suffixRefAt(int suffixCoordinate) {
        return ref(suffixRanks[Objects.checkIndex(suffixCoordinate, suffixRanks.length)]);
    }

    @Override
    public int prefixCoordinate(LexicalRef ref) {
        return prefixCoordinatesByRank[rank(ref)];
    }

    @Override
    public int suffixCoordinate(LexicalRef ref) {
        return suffixCoordinatesByRank[rank(ref)];
    }

    @Override
    public LexicalRange prefixRange(LexicalRef prefix) {
        int rank = rank(prefix);
        return new LexicalRange(
                prefixCoordinatesByRank[rank], prefixEndsExclusiveByRank[rank]);
    }

    @Override
    public LexicalRange suffixRange(LexicalRef suffix) {
        int rank = rank(suffix);
        return new LexicalRange(
                suffixCoordinatesByRank[rank], suffixEndsExclusiveByRank[rank]);
    }

    @Override
    public boolean startsWith(LexicalRef value, LexicalRef prefix) {
        int valueCoordinate = prefixCoordinatesByRank[rank(value)];
        int prefixRank = rank(prefix);
        return valueCoordinate >= prefixCoordinatesByRank[prefixRank]
                && valueCoordinate < prefixEndsExclusiveByRank[prefixRank];
    }

    @Override
    public boolean endsWith(LexicalRef value, LexicalRef suffix) {
        int valueCoordinate = suffixCoordinatesByRank[rank(value)];
        int suffixRank = rank(suffix);
        return valueCoordinate >= suffixCoordinatesByRank[suffixRank]
                && valueCoordinate < suffixEndsExclusiveByRank[suffixRank];
    }

    @Override
    public String materialize(LexicalRef ref) {
        int rank = rank(ref);
        return new String(
                arena,
                offsets[rank],
                offsets[rank + 1] - offsets[rank],
                StandardCharsets.UTF_8);
    }

    @Override
    public ByteBuffer utf8View(LexicalRef ref) {
        int rank = rank(ref);
        int start = offsets[rank];
        int end = offsets[rank + 1];
        return ByteBuffer.wrap(arena, start, end - start)
                .slice()
                .asReadOnlyBuffer();
    }

    @Override public long frequency(LexicalRef ref) { return frequencies[rank(ref)]; }
    @Override public SemanticRef semantic(LexicalRef ref) { return new SemanticRef(semanticBits[rank(ref)]); }
    @Override public int utf16Length(LexicalRef ref) { return utf16Lengths[rank(ref)]; }
    @Override public int codePointLength(LexicalRef ref) { return codePointLengths[rank(ref)]; }
    @Override public int javaHashCode(LexicalRef ref) { return javaHashes[rank(ref)]; }
    @Override public int javaHashMultiplier(LexicalRef ref) { return javaHashMultipliers[rank(ref)]; }

    public List<TokenEntry> entries() {
        List<TokenEntry> result = new ArrayList<>(size());
        for (int rank = 0; rank < size(); rank++) {
            LexicalRef ref = ref(rank);
            result.add(new TokenEntry(materialize(ref), frequency(ref), semantic(ref)));
        }
        return List.copyOf(result);
    }

    int rank(LexicalRef ref) {
        requireLocal(ref);
        return Math.toIntExact(ref.lexicalRank());
    }

    /**
     * Package-local zero-allocation verifier path for the native regex automaton.
     *
     * <p>The dictionary retains ownership of the immutable UTF-8 arena; no byte array or String is
     * exposed or materialized.</p>
     */
    boolean matchesUtf8(LexicalRef ref, MIndexRegexAutomaton automaton) {
        Objects.requireNonNull(automaton, "automaton");
        int rank = rank(ref);
        int start = offsets[rank];
        return automaton.matchesUtf8(arena, start, offsets[rank + 1] - start);
    }


    boolean containsUtf8(LexicalRef ref, MIndexTextSearch search) {
        Objects.requireNonNull(search, "search");
        int rank = rank(ref);
        int start = offsets[rank];
        return search.containsUtf8(arena, start, offsets[rank + 1] - start);
    }

    boolean startsWithUtf8(LexicalRef ref, MIndexTextSearch search) {
        Objects.requireNonNull(search, "search");
        int rank = rank(ref);
        int start = offsets[rank];
        return search.startsWithUtf8(arena, start, offsets[rank + 1] - start);
    }

    boolean endsWithUtf8(LexicalRef ref, MIndexTextSearch search) {
        Objects.requireNonNull(search, "search");
        int rank = rank(ref);
        int start = offsets[rank];
        return search.endsWithUtf8(arena, start, offsets[rank + 1] - start);
    }

    byte[] arenaCopy() { return arena.clone(); }
    int[] offsetsCopy() { return offsets.clone(); }
    byte[] reverseArenaCopy() { return reverseArena.clone(); }
    int[] reverseOffsetsCopy() { return reverseOffsets.clone(); }
    int[] prefixRanksCopy() { return prefixRanks.clone(); }
    int[] suffixRanksCopy() { return suffixRanks.clone(); }
    int[] prefixCoordinatesByRankCopy() { return prefixCoordinatesByRank.clone(); }
    int[] suffixCoordinatesByRankCopy() { return suffixCoordinatesByRank.clone(); }
    int[] prefixEndsExclusiveByRankCopy() { return prefixEndsExclusiveByRank.clone(); }
    int[] suffixEndsExclusiveByRankCopy() { return suffixEndsExclusiveByRank.clone(); }
    long[] frequenciesCopy() { return frequencies.clone(); }
    long[] semanticBitsCopy() { return semanticBits.clone(); }
    long[] hashesCopy() { return hashes.clone(); }
    int[] utf16LengthsCopy() { return utf16Lengths.clone(); }
    int[] codePointLengthsCopy() { return codePointLengths.clone(); }
    int[] javaHashesCopy() { return javaHashes.clone(); }
    int[] javaHashMultipliersCopy() { return javaHashMultipliers.clone(); }
    long exactLookupSeed() { return exactLookup.seed(); }
    int[] exactLookupDisplacementsCopy() { return exactLookup.displacementsCopy(); }
    int[] exactLookupSlotsCopy() { return exactLookup.slotsCopy(); }
    int exactLookupRuntimeCandidateBound() { return MIndexExactLookup.FIXED_RUNTIME_CANDIDATES; }
    long exactLookupRetainedBytes() { return exactLookup.retainedBytes(); }
    int directLookupEligibleRankCount() { return directLookup.eligibleRankCount(); }
    int directLookupRankCount() { return directLookup.directRankCount(); }
    int directLookupAmbiguousRankCount() { return directLookup.ambiguousRankCount(); }
    int directLookupUnresolvedRowCount() { return directLookup.unresolvedAmbiguousRowCount(); }
    int directLookupRefinementPageCount() { return directLookup.refinementPageCount(); }
    long directLookupRetainedBytes() { return directLookup.retainedBytes(); }

    static long hash64(byte[] bytes) {
        return hash64(bytes, 0, bytes.length);
    }

    static long hash64(byte[] bytes, int offset, int length) {
        Objects.requireNonNull(bytes, "bytes");
        Objects.checkFromIndexSize(offset, length, bytes.length);
        long hash = beginHash64(length);
        int end = offset + length;
        for (int index = offset; index < end; index++) {
            hash = updateHash64(hash, bytes[index]);
        }
        return finishHash64(hash);
    }

    static long beginHash64(int byteLength) {
        if (byteLength < 0) throw new IllegalArgumentException("byteLength");
        return HASH_SEED ^ ((long) byteLength * 0x9E3779B185EBCA87L);
    }

    static long updateHash64(long hash, int unsignedByte) {
        long value = hash ^ ((unsignedByte & 0xffL) * 0xC2B2AE3D27D4EB4FL);
        return Long.rotateLeft(value, 27) * 0x9E3779B185EBCA87L
                + 0x165667B19E3779F9L;
    }

    static long hash64(ByteBuffer bytes) {
        ByteBuffer view = Objects.requireNonNull(bytes, "bytes").duplicate();
        int length = view.remaining();
        long hash = HASH_SEED ^ ((long) length * 0x9E3779B185EBCA87L);
        while (view.hasRemaining()) {
            hash ^= (view.get() & 0xffL) * 0xC2B2AE3D27D4EB4FL;
            hash = Long.rotateLeft(hash, 27) * 0x9E3779B185EBCA87L + 0x165667B19E3779F9L;
        }
        return finishHash64(hash);
    }

    static long finishHash64(long value) {
        long hash = value;
        hash ^= hash >>> 33;
        hash *= 0xff51afd7ed558ccdL;
        hash ^= hash >>> 33;
        hash *= 0xc4ceb9fe1a85ec53L;
        return hash ^ (hash >>> 33);
    }

    static int mix(long value) { return (int) (value ^ (value >>> 32)); }

    static String reverseCodePoints(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int index = value.length(); index > 0; ) {
            int codePoint = value.codePointBefore(index);
            out.appendCodePoint(codePoint);
            index -= Character.charCount(codePoint);
        }
        return out.toString();
    }

    static LexicalRange rangeForPrefix(
            byte[] sourceArena, int[] sourceOffsets, int[] coordinateToRank, byte[] query) {
        int count = sourceOffsets.length - 1;
        int low = lowerBound(sourceArena, sourceOffsets, coordinateToRank, query, false);
        int high = lowerBound(sourceArena, sourceOffsets, coordinateToRank, query, true);
        if (low > count || high > count) throw new AssertionError("range bounds");
        return new LexicalRange(low, high);
    }

    private static int lowerBound(
            byte[] sourceArena,
            int[] sourceOffsets,
            int[] coordinateToRank,
            byte[] query,
            boolean strictlyGreater) {
        int low = 0;
        int high = sourceOffsets.length - 1;
        while (low < high) {
            int mid = (low + high) >>> 1;
            int rank = coordinateToRank == null ? mid : coordinateToRank[mid];
            int comparison = comparePrefix(sourceArena, sourceOffsets, rank, query);
            if (comparison > 0 || (!strictlyGreater && comparison == 0)) {
                high = mid;
            } else {
                low = mid + 1;
            }
        }
        return low;
    }

    private static int comparePrefix(byte[] bytes, int[] byteOffsets, int rank, byte[] query) {
        int start = byteOffsets[rank];
        int end = byteOffsets[rank + 1];
        int length = end - start;
        int common = Math.min(length, query.length);
        for (int i = 0; i < common; i++) {
            int left = bytes[start + i] & 0xff;
            int right = query[i] & 0xff;
            if (left != right) return Integer.compare(left, right);
        }
        if (query.length <= length) return 0;
        return -1;
    }

    private static boolean bytesEqual(byte[] bytes, int[] byteOffsets, int rank, byte[] query) {
        return bytesEqual(bytes, byteOffsets, rank, query, 0, query.length);
    }

    private static boolean bytesEqual(
            byte[] bytes, int[] byteOffsets, int rank,
            byte[] query, int queryOffset, int queryLength) {
        int start = byteOffsets[rank];
        int length = byteOffsets[rank + 1] - start;
        if (length != queryLength) return false;
        for (int i = 0; i < length; i++) {
            if (bytes[start + i] != query[queryOffset + i]) return false;
        }
        return true;
    }

    private static void invertCoordinates(int[] coordinateToRank, int[] coordinateByRank) {
        if (coordinateToRank.length != coordinateByRank.length) {
            throw new IllegalArgumentException("coordinate lane length mismatch");
        }
        for (int coordinate = 0; coordinate < coordinateToRank.length; coordinate++) {
            coordinateByRank[coordinateToRank[coordinate]] = coordinate;
        }
    }

    private static void compileRelationEnds(
            byte[] bytes,
            int[] byteOffsets,
            int[] coordinateToRank,
            int[] coordinateByRank,
            int[] endExclusiveByRank) {
        if (coordinateToRank.length != coordinateByRank.length
                || coordinateToRank.length != endExclusiveByRank.length) {
            throw new IllegalArgumentException("relation lane length mismatch");
        }
        int count = coordinateToRank.length;
        for (int prefixRank = 0; prefixRank < count; prefixRank++) {
            int low = coordinateByRank[prefixRank] + 1;
            int high = count;
            while (low < high) {
                int middle = (low + high) >>> 1;
                int candidateRank = coordinateToRank[middle];
                if (startsWithRank(bytes, byteOffsets, candidateRank, prefixRank)) {
                    low = middle + 1;
                } else {
                    high = middle;
                }
            }
            endExclusiveByRank[prefixRank] = low;
        }
    }

    private static boolean startsWithRank(
            byte[] bytes, int[] byteOffsets, int candidateRank, int prefixRank) {
        int candidateStart = byteOffsets[candidateRank];
        int candidateLength = byteOffsets[candidateRank + 1] - candidateStart;
        int prefixStart = byteOffsets[prefixRank];
        int prefixLength = byteOffsets[prefixRank + 1] - prefixStart;
        if (prefixLength > candidateLength) return false;
        for (int index = 0; index < prefixLength; index++) {
            if (bytes[candidateStart + index] != bytes[prefixStart + index]) return false;
        }
        return true;
    }

    private static void sortRanksByBytes(int[] ranks, byte[] bytes, int[] byteOffsets) {
        if (ranks.length < 2) return;
        int[] scratch = new int[ranks.length];
        for (int width = 1; width < ranks.length; ) {
            long stride = (long) width << 1;
            for (int start = 0; start < ranks.length; ) {
                int middle = (int) Math.min(ranks.length, (long) start + width);
                int end = (int) Math.min(ranks.length, (long) start + stride);
                int left = start;
                int right = middle;
                int out = start;
                while (left < middle && right < end) {
                    int leftRank = ranks[left];
                    int rightRank = ranks[right];
                    if (compareBytes(bytes, byteOffsets, leftRank, rightRank) <= 0) {
                        scratch[out++] = leftRank;
                        left++;
                    } else {
                        scratch[out++] = rightRank;
                        right++;
                    }
                }
                while (left < middle) scratch[out++] = ranks[left++];
                while (right < end) scratch[out++] = ranks[right++];
                start = end;
            }
            System.arraycopy(scratch, 0, ranks, 0, ranks.length);
            if (width > ranks.length / 2) break;
            width <<= 1;
        }
    }

    private static int compareBytes(byte[] bytes, int[] byteOffsets, int leftRank, int rightRank) {
        int leftStart = byteOffsets[leftRank];
        int leftLength = byteOffsets[leftRank + 1] - leftStart;
        int rightStart = byteOffsets[rightRank];
        int rightLength = byteOffsets[rightRank + 1] - rightStart;
        int common = Math.min(leftLength, rightLength);
        for (int i = 0; i < common; i++) {
            int left = bytes[leftStart + i] & 0xff;
            int right = bytes[rightStart + i] & 0xff;
            if (left != right) return Integer.compare(left, right);
        }
        return Integer.compare(leftLength, rightLength);
    }

    private static List<TokenEntry> foldEntries(List<TokenEntry> staged) {
        ArrayList<TokenEntry> ordered = new ArrayList<>(staged.size());
        TokenEntry space = null;
        // freeze inserts SPACE before this call, so the list is always nonempty.
        TokenEntry current = staged.get(0);
        for (int index = 1; index < staged.size(); index++) {
            TokenEntry next = staged.get(index);
            if (current.surface().equals(next.surface())) {
                current = merge(current, next);
                continue;
            }
            if (current.surface().equals(SPACE)) space = current;
            else ordered.add(current);
            current = next;
        }
        if (current.surface().equals(SPACE)) space = current;
        else ordered.add(current);

        if (space == null) throw new AssertionError("SPACE sentinel disappeared");
        ordered.add(0, space);
        return ordered;
    }

    private static TokenEntry merge(TokenEntry left, TokenEntry right) {
        SemanticRef semantic = left.semantic().bits() != 0L ? left.semantic() : right.semantic();
        return new TokenEntry(left.surface(), Math.max(left.frequency(), right.frequency()), semantic);
    }

    static int compareCodePoint(String left, String right) {
        int li = 0;
        int ri = 0;
        while (li < left.length() && ri < right.length()) {
            int lc = left.codePointAt(li);
            int rc = right.codePointAt(ri);
            if (lc != rc) return Integer.compare(lc, rc);
            li += Character.charCount(lc);
            ri += Character.charCount(rc);
        }
        return Integer.compare(left.length() - li, right.length() - ri);
    }
}
