// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Whole-string compatibility handle for the reversible compiler experiment.
 * Equality and Java-compatible hashCode are O(1) after interning; character
 * operations materialize unless a direct pooled-byte operation is available.
 */
public final class MIndexString
        implements MIndexText, Comparable<MIndexString>, MIndexSymbolReference {
    public static final Comparator<MIndexString> CASE_INSENSITIVE_ORDER = MIndexString::compareToIgnoreCase;
    private static final MIndexStringPrecomputeCache PRECOMPUTE =
            new MIndexStringPrecomputeCache();

    private final MIndexStringPool pool;
    private final int id;

    public MIndexString() { this(""); }
    public MIndexString(String value) {
        this(MIndexStringPool.GLOBAL, MIndexStringPool.GLOBAL.intern(Objects.requireNonNull(value, "value")));
    }
    public MIndexString(char[] value) { this(new String(Objects.requireNonNull(value, "value"))); }
    public MIndexString(byte[] value) { this(new String(Objects.requireNonNull(value, "value"), Charset.defaultCharset())); }
    public MIndexString(byte[] value, Charset charset) {
        this(new String(Objects.requireNonNull(value, "value"), Objects.requireNonNull(charset, "charset")));
    }

    private MIndexString(MIndexStringPool pool, int id) { this.pool = pool; this.id = id; }
    static MIndexString wrap(MIndexStringPool pool, int id) { return new MIndexString(pool, id); }

    public static MIndexString literal(String value) {
        int id = MIndexStringPool.GLOBAL.intern(Objects.requireNonNull(value, "value"));
        return MIndexStringPool.GLOBAL.canonical(id);
    }

    public static MIndexString of(String value) { return literal(value); }

    /**
     * Canonicalize a strict UTF-8 slice directly without constructing an intermediate String.
     */
    public static MIndexString literalUtf8(byte[] utf8, int offset, int length) {
        int id = MIndexStringPool.GLOBAL.internUtf8(
                Objects.requireNonNull(utf8, "utf8"), offset, length);
        return MIndexStringPool.GLOBAL.canonical(id);
    }

    public static MIndexString literalUtf8(byte[] utf8) {
        Objects.requireNonNull(utf8, "utf8");
        return literalUtf8(utf8, 0, utf8.length);
    }

    /** Returns the canonical handle for a process-local interned content index. */
    public static MIndexString fromContentIndex(int contentIndex) {
        return MIndexStringPool.GLOBAL.canonical(contentIndex);
    }
    /**
     * Finds an admitted spelling without growing the pool; returns -1 for an unknown token.
     * Reading and hashing k UTF-8 bytes is O(k), followed by expected O(1) table probes.
     */
    public static int findContentIndex(CharSequence value) {
        Objects.requireNonNull(value, "value");
        return value instanceof MIndexString indexed && indexed.pool == MIndexStringPool.GLOBAL
                ? indexed.id : MIndexStringPool.GLOBAL.findExisting(value.toString());
    }

    /**
     * Find a previously admitted strict UTF-8 slice without growing the global pool.
     */
    public static int findContentIndexUtf8(byte[] utf8, int offset, int length) {
        return MIndexStringPool.GLOBAL.findExistingUtf8(
                Objects.requireNonNull(utf8, "utf8"), offset, length);
    }

    public static int findContentIndexUtf8(byte[] utf8) {
        Objects.requireNonNull(utf8, "utf8");
        return findContentIndexUtf8(utf8, 0, utf8.length);
    }

    public static MIndexString from(CharSequence value) {
        return value instanceof MIndexString indexed
                ? indexed : literal(Objects.requireNonNull(value, "value").toString());
    }

    public int contentIndex() { return id; }

    /** Stable semantic identity plus logical storage coordinate for this canonical value. */
    public MIndexRef ref() { return pool.ref(id); }

    /**
     * Stable logical UTF-8 storage coordinate.
     *
     * <p>This is not a JVM heap address and remains valid when GC or Java arena growth moves or
     * replaces backing objects.</p>
     */
    public MIndexAddress storageAddress() { return pool.storageAddress(id); }

    @Override
    public MIndexSymbolRef symbolRef() {
        MIndexAddress address = storageAddress();
        return new MIndexSymbolRef(
                MIndexReferenceDomain.STRING,
                address.regionId(),
                Integer.toUnsignedLong(id));
    }

    @Override public MIndexString materialize() { return this; }
    @Override public int segmentCount() { return 1; }
    @Override public MIndexString segment(int index) {
        Objects.checkIndex(index, 1);
        return this;
    }
    public long contentHash64() { return pool.hash64(id); }
    public int utf8Length() { return pool.utf8Length(id); }

    /** True when exact persistence requires the pool's UTF-16 sidecar for an unpaired surrogate. */
    public boolean requiresUtf16Sidecar() { return pool.hasExceptionalUtf16(id); }

    @Override public int length() { return pool.length(id); }
    @Override public char charAt(int index) { return pool.charAt(id, index); }
    @Override public MIndexString subSequence(int start, int end) { return substring(start, end); }

    /**
     * Historical bounded immutable code-point-safe view.
     *
     * <p>This method retains its existing strict boundary behavior for compatibility. Use
     * {@link #subViewUtf16(int, int)} for exact java.lang.String UTF-16 coordinate semantics.</p>
     */
    public SubMIndexString subView(int start, int end) {
        return SubMIndexString.ofCodePointSafe(this, start, end);
    }

    /** Zero-copy UTF-16 range that may split a surrogate pair exactly like String.substring. */
    public SubMIndexString subViewUtf16(int start, int end) {
        return SubMIndexString.ofUtf16(this, start, end);
    }

    /** Explicit alias for the historical strict view contract. */
    public SubMIndexString subViewCodePointSafe(int start, int end) {
        return SubMIndexString.ofCodePointSafe(this, start, end);
    }

    public MIndexString substring(int beginIndex) {
        return pool.canonical(pool.substring(id, beginIndex, length()));
    }

    public MIndexString substring(int beginIndex, int endIndex) {
        return pool.canonical(pool.substring(id, beginIndex, endIndex));
    }
    public boolean isEmpty() { return length() == 0; }
    public boolean isBlank() { return pool.isBlank(id); }

    public boolean contains(CharSequence value) {
        CharSequence checked = Objects.requireNonNull(value, "value");
        if (checked instanceof MIndexString indexed) return contains(indexed);
        return pool.indexOf(id, checked.toString()) >= 0;
    }

    public boolean contains(MIndexString value) {
        Objects.requireNonNull(value, "value");
        return pool == value.pool ? pool.contains(id, value.id) : string().contains(value.string());
    }

    public boolean startsWith(CharSequence prefix) {
        CharSequence checked = Objects.requireNonNull(prefix, "prefix");
        if (checked instanceof MIndexString indexed) return startsWith(indexed);
        return pool.startsWith(id, checked.toString());
    }

    public boolean startsWith(MIndexString prefix) {
        Objects.requireNonNull(prefix, "prefix");
        return pool == prefix.pool ? pool.startsWith(id, prefix.id) : string().startsWith(prefix.string());
    }

    public boolean endsWith(CharSequence suffix) {
        CharSequence checked = Objects.requireNonNull(suffix, "suffix");
        if (checked instanceof MIndexString indexed) return endsWith(indexed);
        return pool.endsWith(id, checked.toString());
    }

    public boolean endsWith(MIndexString suffix) {
        Objects.requireNonNull(suffix, "suffix");
        return pool == suffix.pool ? pool.endsWith(id, suffix.id) : string().endsWith(suffix.string());
    }
    public int indexOf(CharSequence value) {
        CharSequence checked = Objects.requireNonNull(value, "value");
        if (checked instanceof MIndexString indexed) {
            return pool == indexed.pool
                    ? pool.indexOfPooled(id, indexed.id)
                    : string().indexOf(indexed.string());
        }
        return pool.indexOf(id, checked.toString());
    }

    public int indexOf(String value) { return pool.indexOf(id, value); }
    public int indexOf(int ch) { return pool.indexOf(id, ch); }

    public MIndexString concat(CharSequence other) {
        CharSequence checked = Objects.requireNonNull(other, "other");
        if (checked instanceof MIndexString indexed && pool == indexed.pool) {
            return pool.canonical(pool.concat(id, indexed.id));
        }
        return pool.canonical(pool.concat(id, checked.toString()));
    }
    public MIndexString replace(char oldChar, char newChar) {
        return pool.canonical(pool.replaceChar(id, oldChar, newChar));
    }
    public MIndexString replace(CharSequence target, CharSequence replacement) {
        return literal(string().replace(target, replacement));
    }
    public MIndexString toLowerCase() { return literal(string().toLowerCase()); }
    public MIndexString toLowerCase(Locale locale) { return literal(string().toLowerCase(locale)); }
    public MIndexString toUpperCase() { return literal(string().toUpperCase()); }
    public MIndexString toUpperCase(Locale locale) { return literal(string().toUpperCase(locale)); }
    public MIndexString trim() { return pool.canonical(pool.trim(id)); }
    public MIndexString strip() { return pool.canonical(pool.strip(id)); }
    public MIndexString repeat(int count) { return pool.canonical(pool.repeat(id, count)); }
    public byte[] getBytes() { return getBytes(Charset.defaultCharset()); }

    /**
     * Read-only zero-copy view of this canonical pooled UTF-8 spelling.
     *
     * <p>For spellings containing unpaired UTF-16 surrogates, UTF-8 is only the compatibility
     * encoding; callers requiring exact Java String semantics must honor
     * {@link #requiresUtf16Sidecar()} and use the UTF-16 path.</p>
     */
    public ByteBuffer utf8View() { return pool.utf8View(id); }

    /** Immutable logical UTF-8 view backed by this canonical interned value. */
    public MIndexByteSequence utf8Sequence() { return MIndexByteSequence.of(this); }

    /** Immutable logical UTF-16 view backed by this canonical interned value. */
    public MIndexCharSequence charSequenceView() { return MIndexCharSequence.of(this); }

    public byte[] getBytes(Charset charset) {
        return StandardCharsets.UTF_8.equals(charset) ? pool.utf8(id) : string().getBytes(charset);
    }
    public IntStream chars() { return string().chars(); }
    public IntStream codePoints() { return string().codePoints(); }
    public Stream<MIndexString> lines() { return string().lines().map(MIndexString::literal); }

    public boolean equalsIgnoreCase(MIndexString other) {
        return other != null && string().equalsIgnoreCase(other.string());
    }

    public int compareToIgnoreCase(MIndexString other) {
        return string().compareToIgnoreCase(Objects.requireNonNull(other, "other").string());
    }

    @Override public int compareTo(MIndexString other) {
        Objects.requireNonNull(other, "other");
        return pool == other.pool ? pool.compare(id, other.id) : string().compareTo(other.string());
    }

    public MIndexString intern() { return pool.canonical(id); }

    @Override public boolean equals(Object other) {
        return this == other || (other instanceof MIndexString value && pool == value.pool && id == value.id);
    }
    @Override public int hashCode() { return pool.javaHash(id); }
    @Override public String toString() { return string(); }

    public static MIndexString valueOf(Object value) {
        return value instanceof MIndexString indexed ? indexed : literal(String.valueOf(value));
    }
    public static MIndexString valueOf(int value) { return literal(String.valueOf(value)); }
    public static MIndexString valueOf(long value) { return literal(String.valueOf(value)); }
    public static MIndexString valueOf(boolean value) { return literal(String.valueOf(value)); }
    public static MIndexString valueOf(char value) { return literal(String.valueOf(value)); }
    public static MIndexString format(String format, Object... args) { return literal(String.format(format, args)); }
    public static MIndexString join(CharSequence delimiter, CharSequence... elements) {
        return literal(String.join(delimiter, elements));
    }

    /** Immutable lightweight/heavy derived geometry retained in a pressure-bounded side cache. */
    public MIndexStringMetadata metadata() {
        return precompute(MIndexStringPrecomputeProfile.CORE).metadata();
    }

    public MIndexStringPrecomputeImage precompute() {
        return precompute(MIndexStringPrecomputeProfile.SEARCH);
    }

    public MIndexStringPrecomputeImage precompute(MIndexStringPrecomputeProfile profile) {
        return PRECOMPUTE.get(this, Objects.requireNonNull(profile, "profile"));
    }

    /**
     * Existing precompute knowledge for bridge/runtime code, or {@code null} when the requested
     * lanes have not already been prepared. This never triggers compilation or cache admission.
     */
    MIndexStringPrecomputeImage cachedPrecompute(MIndexStringPrecomputeProfile profile) {
        MIndexStringPrecomputeProfile wanted = Objects.requireNonNull(profile, "profile");
        MIndexStringPrecomputeImage image = PRECOMPUTE.peek(id);
        return image != null && image.profile().includes(wanted) ? image : null;
    }

    public MIndexStringPrecomputeImage.Fingerprint substringFingerprint(int start, int end) {
        return precompute(MIndexStringPrecomputeProfile.SEARCH).substringFingerprint(start, end);
    }

    public int minimalPeriod() {
        return precompute(MIndexStringPrecomputeProfile.SEARCH).minimalPeriod();
    }

    public MIndexString longestPalindrome() {
        MIndexStringPrecomputeImage.Span span =
                precompute(MIndexStringPrecomputeProfile.SEARCH).longestPalindrome();
        return substring(span.startInclusive(), span.endExclusive());
    }

    public MIndexString longestRepeatedSubstring() {
        MIndexStringPrecomputeImage.Span span =
                precompute(MIndexStringPrecomputeProfile.INDEXED).longestRepeatedSubstring();
        return substring(span.startInclusive(), span.endExclusive());
    }

    public static void setPrecomputeCacheBudgetBytes(long bytes) {
        PRECOMPUTE.setBudgetBytes(bytes);
    }

    public static long precomputeCacheBudgetBytes() { return PRECOMPUTE.budgetBytes(); }
    public static long precomputeCacheRetainedBytes() { return PRECOMPUTE.retainedBytes(); }
    public static int precomputeCacheEntries() { return PRECOMPUTE.retainedEntries(); }
    public static void clearPrecomputeCache() { PRECOMPUTE.clear(); }

    public static int pooledValues() { return MIndexStringPool.GLOBAL.size(); }
    public static long pooledUtf8Bytes() { return MIndexStringPool.GLOBAL.retainedUtf8Bytes(); }


    /**
     * Returns an immutable hierarchical lexicon view while preserving this
     * value as the canonical String-compatible content identity.
     */
    public MIndexLexicalView lexicalView(MIndexLexiconSpace space) {
        return MIndexHybridCodec.encode(Objects.requireNonNull(space, "space"), this);
    }

    public MIndexLexicalView lexicalView(MIndexLexiconSpace space, int maxJoinedSegments) {
        return MIndexHybridCodec.encode(
                Objects.requireNonNull(space, "space"), this, maxJoinedSegments);
    }

    private String string() { return pool.materialize(id); }

    void copyChars(int sourceStart, char[] target, int targetStart, int count) {
        pool.copyChars(id, sourceStart, target, targetStart, count);
    }
}
