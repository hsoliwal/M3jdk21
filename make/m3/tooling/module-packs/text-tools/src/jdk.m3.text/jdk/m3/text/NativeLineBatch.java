// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.util.concurrent.ProgressMonitor;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Immutable, primitive-only native line window. The source is one packed UTF-8
 * byte array, with n+1 byte offsets and a selection mask. Native operations
 * return a new mask; they do not retain addresses or construct line objects.
 *
 * <p>Line endings are deliberately outside this kernel. The calling M3
 * TextFrame owns Unicode validation, the original M3 String owners and exact
 * line endings; this kernel only selects indices of those immutable lines.
 * This byte-level literal subset is NOT a replacement for Unix4j regex/flags.
 */
public final class NativeLineBatch {
    private static final int ABI = 0x00010002;
    private static final int GREP_LITERAL = 1;
    private static final int UNIQUE_ADJACENT = 2;
    private static final int HEAD = 3;
    private static final int TAIL = 4;

    private final byte[] packed;
    private final ByteBuffer direct;
    private final Object m3Owner;
    private final int[] offsets;
    private final long[] selected;

    private NativeLineBatch(byte[] packed, ByteBuffer direct, Object m3Owner,
                            int[] offsets, long[] selected) {
        this.packed = packed;
        this.direct = direct;
        this.m3Owner = m3Owner;
        this.offsets = offsets;
        this.selected = selected;
    }

    /** Create a defensive snapshot over packed UTF-8 line CONTENTS (no endings). */
    public static NativeLineBatch fromPacked(byte[] content, int[] lineOffsets) {
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(lineOffsets, "lineOffsets");
        if (lineOffsets.length == 0 || lineOffsets[0] != 0
                || lineOffsets[lineOffsets.length - 1] != content.length) {
            throw new IllegalArgumentException("invalid line offsets");
        }
        for (int i = 1; i < lineOffsets.length; i++) {
            if (lineOffsets[i] < lineOffsets[i - 1]) {
                throw new IllegalArgumentException("nonmonotone line offsets");
            }
        }
        return owned(content.clone(), lineOffsets.clone());
    }

    /**
     * Zero-copy native input window for an admitted immutable M3 String backing.
     * The caller must guarantee backing lifetime and immutability through m3Owner;
     * a read-only ByteBuffer by itself does not freeze another writable alias.
     * The direct-buffer address is used during JNI only, never stored natively.
     */
    public static NativeLineBatch fromReadOnlyM3Bytes(ByteBuffer source, int[] lineOffsets,
                                                      Object m3Owner) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(m3Owner, "m3Owner");
        Objects.requireNonNull(lineOffsets, "lineOffsets");
        if (!source.isDirect() || !source.isReadOnly()) {
            throw new IllegalArgumentException("immutable direct M3 backing required");
        }
        ByteBuffer window = source.slice().asReadOnlyBuffer();
        if (lineOffsets.length == 0 || lineOffsets[0] != 0
                || lineOffsets[lineOffsets.length - 1] != window.remaining()) {
            throw new IllegalArgumentException("invalid M3 direct window offsets");
        }
        for (int i = 1; i < lineOffsets.length; i++) {
            if (lineOffsets[i] < lineOffsets[i - 1]) {
                throw new IllegalArgumentException("nonmonotone line offsets");
            }
        }
        return new NativeLineBatch(null, window, m3Owner,
                lineOffsets.clone(), allSelected(lineOffsets.length - 1));
    }

    /** Exact-size, owner-private primitive UTF-8 snapshot; no per-row byte arrays. */
    public static NativeLineBatch fromUtf8Lines(List<String> lines) {
        M3Utf8PackedRows.Packed result = M3Utf8PackedRows.pack(lines);
        return owned(result.bytes(), result.offsets());
    }

    private static void validUnicode(String text) {
        for (int i = 0; i < text.length(); i++) {
            char unit = text.charAt(i);
            if (Character.isHighSurrogate(unit)) {
                if (i + 1 == text.length() || !Character.isLowSurrogate(text.charAt(i + 1))) {
                    throw new IllegalArgumentException("unpaired high surrogate");
                }
                i++;
            } else if (Character.isLowSurrogate(unit)) {
                throw new IllegalArgumentException("unpaired low surrogate");
            }
        }
    }

    private static NativeLineBatch owned(byte[] content, int[] offsets) {
        return new NativeLineBatch(content, null, null, offsets, allSelected(offsets.length - 1));
    }

    private static long[] allSelected(int n) {
        long[] mask = new long[(int) ((((long) n) + 63) >>> 6)];
        if (n > 0) {
            Arrays.fill(mask, -1L);
            int last = n & 63;
            if (last != 0) mask[mask.length - 1] = (1L << last) - 1;
        }
        return mask;
    }

    public int lineCount() { return offsets.length - 1; }

    public int selectedCount() {
        int count = 0;
        for (long word : selected) count = Math.addExact(count, Long.bitCount(word));
        return count;
    }

    public boolean containsIndex(int index) {
        Objects.checkIndex(index, lineCount());
        return (selected[index >>> 6] & (1L << (index & 63))) != 0;
    }

    /** Indexed primitive callback: callers need not allocate boxed row IDs. */
    public void forEachSelectedIndex(IntConsumer action) {
        M3MaskIndexCursor.forEach(selected, lineCount(), action);
    }

    /** Preserve the original public boxed List<Integer> compatibility API. */
    public List<Integer> selectedIndices() {
        List<Integer> indices = new ArrayList<>(selectedCount());
        forEachSelectedIndex(indices::add);
        return List.copyOf(indices);
    }

    /** Exact UTF-8 literal substring, not POSIX basic/extended regular expression. */
    public NativeLineBatch grepLiteral(String literal) {
        String pattern = Objects.requireNonNull(literal, "literal");
        validUnicode(pattern);
        return execute(GREP_LITERAL, pattern.getBytes(StandardCharsets.UTF_8), 0);
    }

    /** Compare adjacent *selected* lines by exact bytes; keep the first. */
    public NativeLineBatch uniqAdjacent() {
        return execute(UNIQUE_ADJACENT, new byte[0], 0);
    }

    /** Keep the first count selected lines; zero selects none. */
    public NativeLineBatch head(int count) {
        if (count < 0) throw new IllegalArgumentException("negative head count");
        return execute(HEAD, new byte[0], count);
    }

    /** Keep the last count selected lines in original order. */
    public NativeLineBatch tail(int count) {
        if (count < 0) throw new IllegalArgumentException("negative tail count");
        return execute(TAIL, new byte[0], count);
    }

    public String utf8Content(int index) {
        Objects.checkIndex(index, lineCount());
        if (packed != null) {
            return new String(packed, offsets[index], offsets[index + 1] - offsets[index],
                    StandardCharsets.UTF_8);
        }
        ByteBuffer view = direct.asReadOnlyBuffer();
        int length = offsets[index + 1] - offsets[index];
        byte[] snapshot = new byte[length];
        view.position(offsets[index]);
        view.get(snapshot);
        return new String(snapshot, StandardCharsets.UTF_8);
    }

    /**
     * Stable row-index ordering by unsigned content bytes, optionally retaining only
     * the first selected occurrence of each distinct content. This is a byte-order
     * kernel (C-locale UTF-8 order for valid UTF-8), not TextTools' UTF-16 order.
     * The returned array is a fresh permutation/subset of original row indices;
     * packed/direct content, offsets, selection and owner are never changed.
     *
     * <p>comparisonBudget charges one unit per row comparison plus one per byte
     * pair examined, including equal prefixes and distinct filtering. Exhaustion
     * throws IllegalArgumentException and publishes no partial result. Native
     * work polls cancellation at least every 4096 comparison units, and during
     * index preparation/merge copying; the nullable monitor stays caller-owned.
     */
    public int[] stableOrder(boolean reverse, boolean distinct, long comparisonBudget,
                             ProgressMonitor monitor) {
        if (comparisonBudget < 0) throw new IllegalArgumentException("negative comparison budget");
        if (monitor != null) monitor.checkCancelled();
        Native.require();
        int[] result;
        try {
            result = order0(packed, direct, offsets, selected, reverse, distinct,
                    comparisonBudget, monitor);
        } finally {
            java.lang.ref.Reference.reachabilityFence(m3Owner);
        }
        if (result == null || result.length > selectedCount())
            throw new IllegalStateException("invalid native order result");
        if (monitor != null) {
            monitor.checkCancelled();
            monitor.worked(result.length);
            monitor.checkCancelled();
        }
        return result;
    }

    /** Caller may omit monitoring while retaining an explicit comparison budget. */
    public int[] stableOrder(boolean reverse, boolean distinct, long comparisonBudget) {
        return stableOrder(reverse, distinct, comparisonBudget, null);
    }

    public static int nativeAbi() {
        Native.require();
        return abi0();
    }

    private NativeLineBatch execute(int operation, byte[] needle, int count) {
        Native.require();
        // Keep the backing owner reachable even across a native call.
        Objects.requireNonNull(m3Owner == null ? packed : m3Owner);
        long[] result = packed != null
                ? select0(packed, offsets, selected, needle, operation, count)
                : selectDirect0(direct, offsets, selected, needle, operation, count);
        if (result == null || result.length != selected.length) {
            throw new IllegalStateException("invalid native selection result");
        }
        M3MaskIndexCursor.requireSubset(result, selected, lineCount());
        java.lang.ref.Reference.reachabilityFence(m3Owner);
        return new NativeLineBatch(packed, direct, m3Owner, offsets, result);
    }

    private static final class Native {
        private static final boolean READY = load();
        private static boolean load() {
            String explicit = System.getProperty("m3.unix4j.native.library");
            if (explicit != null && !explicit.isBlank()) {
                System.load(Path.of(explicit).toAbsolutePath().toString());
            } else {
                System.loadLibrary("m3unixtext");
            }
            if (abi0() != ABI) throw new UnsatisfiedLinkError("M3 Unix4j JNI ABI mismatch");
            return true;
        }
        static void require() { if (!READY) throw new UnsatisfiedLinkError("M3 Unix4j JNI unavailable"); }
    }

    private static native int abi0();
    private static native int[] order0(byte[] packed, ByteBuffer direct, int[] offsets,
                                       long[] selected, boolean reverse, boolean distinct,
                                       long comparisonBudget, ProgressMonitor monitor);
    private static native long[] select0(byte[] packed, int[] offsets, long[] selected,
                                         byte[] needle, int operation, int count);
    private static native long[] selectDirect0(ByteBuffer direct, int[] offsets, long[] selected,
                                               byte[] needle, int operation, int count);
}
