/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.text;

import java.util.ArrayList;
import java.util.List;

/** Ordered line snapshot; frame hashing distinguishes equal text with different line framing. */
public record TextFrame(List<TextLine> lines) {
  public TextFrame { lines = List.copyOf(lines); }
  public static TextFrame fromString(String text) {
    Canonical.validUnicode(text);
    List<TextLine> result = new ArrayList<>();
    int start = 0;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c != '\r' && c != '\n') continue;
      String content = text.substring(start, i);
      String ending = String.valueOf(c);
      if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
        ending = "\r\n";
        i++;
      }
      result.add(new TextLine(content, ending));
      start = i + 1;
    }
    if (start < text.length()) result.add(new TextLine(text.substring(start), ""));
    return new TextFrame(result);
  }
  public long byteCount() {
    long n = 0;
    for (TextLine line : lines) n = Math.addExact(n, line.byteCount());
    return n;
  }
  public String text() {
    StringBuilder result = new StringBuilder();
    for (TextLine line : lines) result.append(line.content()).append(line.ending());
    return result.toString();
  }
  public String sha256() {
    Canonical hash = new Canonical("m3-unix4j-frame-v1").number(lines.size());
    for (TextLine line : lines) hash.text(line.content()).text(line.ending());
    return hash.finish();
  }

  /**
   * Index an immutable String/M3 view without copying its spelling. Recognizes CR,
   * LF and CRLF exactly as fromString; rejects malformed UTF-16. The owner must
   * remain immutable for the lifetime of the result and every derived view.
   * Admission is bounded to 64 Mi UTF-16 units and at most one million rows.
   * An empty input has zero rows; a final terminator adds no phantom empty row.
   */
  public static LineIndex index(CharSequence source, int maxLines) {
    java.util.Objects.requireNonNull(source, "source");
    LineIndex.checkLimit(maxLines);
    int length = source.length();
    if (length < 0 || length > 64 * 1024 * 1024)
      throw new IllegalArgumentException("source UTF-16 budget");
    Utf8Segments.length(source); // Existing strict scalar/UTF-8 owner, no projection.
    int rows = 0, start = 0;
    for (int i = 0; i < length; i++) {
      char unit = source.charAt(i);
      if (unit != '\r' && unit != '\n') continue;
      if (++rows > maxLines) throw new IllegalArgumentException("line budget");
      if (unit == '\r' && i + 1 < length && source.charAt(i + 1) == '\n') i++;
      start = i + 1;
    }
    if (start < length && ++rows > maxLines) throw new IllegalArgumentException("line budget");
    int[] starts = new int[rows], ends = new int[rows];
    byte[] endings = new byte[rows];
    int row = 0; start = 0;
    for (int i = 0; i < length; i++) {
      char unit = source.charAt(i);
      if (unit != '\r' && unit != '\n') continue;
      starts[row] = start; ends[row] = i;
      byte ending = unit == '\n' ? (byte) 1 : (byte) 2;
      if (unit == '\r' && i + 1 < length && source.charAt(i + 1) == '\n') {
        ending = 3; i++;
      }
      endings[row++] = ending; start = i + 1;
    }
    if (start < length) { starts[row] = start; ends[row] = length; }
    return new LineIndex(source, starts, ends, endings);
  }

  /**
   * Immutable logical frame over one borrowed owner and packed coordinate lanes.
   * Small derived frames intentionally retain their entire source owner. No
   * content objects are stored per row. Public view/array returns are detached
   * projections; only an explicit materialize/toString/writeTo exports spelling.
   */
  public static final class LineIndex {
    private static final int MAX_LINES = 1_000_000;
    private static final String[] ENDINGS = {"", "\n", "\r", "\r\n"};
    private final CharSequence source;
    private final int[] starts;
    private final int[] ends;
    private final byte[] endings;

    private LineIndex(CharSequence source, int[] starts, int[] ends, byte[] endings) {
      this.source = source; this.starts = starts; this.ends = ends; this.endings = endings;
    }
    private static void checkLimit(int limit) {
      if (limit < 0 || limit > MAX_LINES) throw new IllegalArgumentException("line budget");
    }
    public int size() { return starts.length; }
    public int start(int row) { return starts[java.util.Objects.checkIndex(row, size())]; }
    public int end(int row) { return ends[java.util.Objects.checkIndex(row, size())]; }
    public String ending(int row) { return ENDINGS[endings[java.util.Objects.checkIndex(row, size())]]; }
    /** Primitive descriptor bytes only, excluding object/array headers and the source. */
    public long descriptorBytes() { return 9L * size(); }
    public int[] starts() { return starts.clone(); }
    public int[] ends() { return ends.clone(); }

    /** Transient range view; neither source.subSequence nor source.toString is called. */
    public CharSequence contentView(int row) {
      java.util.Objects.checkIndex(row, size());
      return new Range(source, starts[row], ends[row]);
    }
    /** Explicit caller-owned compatibility projection for existing prepared tools. */
    public CharSequence[] contentViews() {
      CharSequence[] result = new CharSequence[size()];
      for (int row = 0; row < result.length; row++) result[row] = contentView(row);
      return result;
    }
    public LineIndex sliceRows(int from, int to) {
      java.util.Objects.checkFromToIndex(from, to, size());
      if (from == 0 && to == size()) return this;
      return new LineIndex(source, java.util.Arrays.copyOfRange(starts, from, to),
          java.util.Arrays.copyOfRange(ends, from, to), java.util.Arrays.copyOfRange(endings, from, to));
    }
    public LineIndex head(int count) {
      if (count < 0) throw new IllegalArgumentException("negative head count");
      return sliceRows(0, Math.min(size(), count));
    }
    public LineIndex tail(int count) {
      if (count < 0) throw new IllegalArgumentException("negative tail count");
      return sliceRows(size() - Math.min(size(), count), size());
    }
    public LineIndex reverse() {
      int[] order = new int[size()];
      for (int i = 0; i < order.length; i++) order[i] = size() - i - 1;
      return selectRows(order);
    }
    /** Ordered multiset selection; repeats are permitted, every index is validated. */
    public LineIndex selectRows(int[] rows) {
      java.util.Objects.requireNonNull(rows, "rows"); checkLimit(rows.length);
      for (int row : rows) java.util.Objects.checkIndex(row, size());
      int[] a = new int[rows.length], b = new int[rows.length]; byte[] e = new byte[rows.length];
      for (int i = 0; i < rows.length; i++) { a[i] = starts[rows[i]]; b[i] = ends[rows[i]]; e[i] = endings[rows[i]]; }
      return new LineIndex(source, a, b, e);
    }
    /** Exact ascending-row selection. Wrong length and high out-of-domain bits reject. */
    public LineIndex selectMask(long[] mask) {
      java.util.Objects.requireNonNull(mask, "mask");
      if (mask.length != (size() + 63) / 64) throw new IllegalArgumentException("mask length");
      if ((size() & 63) != 0 && (mask[mask.length - 1] >>> (size() & 63)) != 0)
        throw new IllegalArgumentException("mask outside frame");
      int count = 0; for (long bits : mask) count += Long.bitCount(bits);
      int[] rows = new int[count]; int at = 0;
      for (int row = 0; row < size(); row++)
        if ((mask[row >>> 6] & (1L << (row & 63))) != 0) rows[at++] = row;
      return selectRows(rows);
    }
    /** Predicate invoked exactly once per row in order; exceptions propagate. */
    public LineIndex filter(java.util.function.Predicate<? super CharSequence> predicate) {
      java.util.Objects.requireNonNull(predicate, "predicate");
      int[] rows = new int[size()]; int count = 0;
      for (int i = 0; i < size(); i++) if (predicate.test(contentView(i))) rows[count++] = i;
      return selectRows(java.util.Arrays.copyOf(rows, count));
    }
    /** Clipped half-open Unicode code-point columns; endings remain attached to rows. */
    public LineIndex cutCodePoints(int from, int to) {
      if (from < 0 || to < from) throw new IllegalArgumentException("code-point range");
      int[] a = new int[size()], b = new int[size()];
      for (int row = 0; row < size(); row++) {
        a[row] = advance(starts[row], ends[row], from);
        b[row] = advance(a[row], ends[row], to - from);
      }
      return new LineIndex(source, a, b, endings.clone());
    }
    /** Character.isWhitespace code-point semantics, equivalent to String.strip. */
    public LineIndex strip() {
      int[] a = starts.clone(), b = ends.clone();
      for (int row = 0; row < size(); row++) {
        while (a[row] < b[row]) {
          int cp = Character.codePointAt(source, a[row]);
          if (!Character.isWhitespace(cp)) break;
          a[row] += Character.charCount(cp);
        }
        while (a[row] < b[row]) {
          int cp = Character.codePointBefore(source, b[row]);
          if (!Character.isWhitespace(cp)) break;
          b[row] -= Character.charCount(cp);
        }
      }
      return new LineIndex(source, a, b, endings.clone());
    }
    /**
     * Zero-based scalar-delimited field, with empty/trailing fields preserved.
     * Missing fields are empty at the content's end. Not a CSV quoting parser.
     */
    public LineIndex field(int delimiter, int ordinal) {
      if (!Character.isValidCodePoint(delimiter) || delimiter >= 0xd800 && delimiter <= 0xdfff
          || delimiter == '\r' || delimiter == '\n' || ordinal < 0)
        throw new IllegalArgumentException("field delimiter/ordinal");
      int[] a = new int[size()], b = new int[size()];
      for (int row = 0; row < size(); row++) {
        int from = starts[row], field = 0; boolean found = false;
        for (int at = from; at < ends[row];) {
          int cp = Character.codePointAt(source, at), width = Character.charCount(cp);
          if (cp == delimiter) {
            if (field == ordinal) { a[row] = from; b[row] = at; found = true; break; }
            field++; from = at + width;
          }
          at += width;
        }
        if (!found) { a[row] = field == ordinal ? from : ends[row]; b[row] = ends[row]; }
      }
      return new LineIndex(source, a, b, endings.clone());
    }
    /**
     * Fold by code points, not display width. Intermediate pieces have LF; each
     * last piece retains its original ending. Exact size admission precedes output
     * allocation; empty rows stay one row and over-budget output rejects.
     */
    public LineIndex fold(int width, int maxLines) {
      if (width <= 0) throw new IllegalArgumentException("fold width"); checkLimit(maxLines);
      long count = 0;
      for (int row = 0; row < size(); row++) {
        int points = Character.codePointCount(source, starts[row], ends[row]);
        count += Math.max(1L, ((long) points + width - 1) / width);
        if (count > maxLines) throw new IllegalArgumentException("fold line budget");
      }
      int[] a = new int[(int) count], b = new int[(int) count]; byte[] e = new byte[(int) count];
      int out = 0;
      for (int row = 0; row < size(); row++) {
        int at = starts[row];
        do {
          int next = advance(at, ends[row], width);
          a[out] = at; b[out] = next; e[out++] = next == ends[row] ? endings[row] : (byte) 1;
          at = next;
        } while (at < ends[row]);
      }
      return new LineIndex(source, a, b, e);
    }
    /** Normalize only terminator metadata; optionally terminate formerly unterminated rows. */
    public LineIndex withEndings(String ending, boolean includeUnterminated) {
      java.util.Objects.requireNonNull(ending, "ending");
      int code = -1; for (int i = 0; i < ENDINGS.length; i++) if (ENDINGS[i].equals(ending)) code = i;
      if (code < 0) throw new IllegalArgumentException("unsupported line ending");
      byte[] e = endings.clone();
      for (int i = 0; i < e.length; i++) if (includeUnterminated || e[i] != 0) e[i] = (byte) code;
      return new LineIndex(source, starts.clone(), ends.clone(), e);
    }
    /** Fixed-size row partitions; each partition borrows the same immutable owner. */
    public List<LineIndex> splitRows(int rowsPerPart) {
      if (rowsPerPart <= 0) throw new IllegalArgumentException("partition size");
      List<LineIndex> parts = new ArrayList<>();
      for (int start = 0; start < size();) {
        int next = start + Math.min(rowsPerPart, size() - start);
        parts.add(sliceRows(start, next)); start = next;
      }
      return List.copyOf(parts);
    }
    public long utf16Units() {
      long units = 0;
      for (int row = 0; row < size(); row++) units += (long) ends[row] - starts[row] + ending(row).length();
      return units;
    }
    /** Content-only code-point counts; terminators are excluded. */
    public int[] codePointCounts() {
      int[] counts = new int[size()];
      for (int row = 0; row < size(); row++) counts[row] = Character.codePointCount(source, starts[row], ends[row]);
      return counts;
    }
    public long byteCount() {
      long bytes = 0;
      for (int row = 0; row < size(); row++) bytes += Utf8Segments.length(contentView(row)) + ending(row).length();
      return bytes;
    }
    /** Identical domain/length framing to the legacy TextFrame hash; no text flattening. */
    public String sha256() {
      Canonical digest = new Canonical("m3-unix4j-frame-v1").number(size());
      for (int row = 0; row < size(); row++) digest.textView(contentView(row)).text(ending(row));
      return digest.finish();
    }
    /** Explicit export to the existing String/TextLine API. */
    public TextFrame materialize() {
      List<TextLine> lines = new ArrayList<>(size());
      for (int row = 0; row < size(); row++) lines.add(new TextLine(contentView(row).toString(), ending(row)));
      return new TextFrame(lines);
    }
    /** Stream the exact framed sequence; do not close or flush the caller's sink. */
    public void writeTo(Appendable sink) throws java.io.IOException {
      java.util.Objects.requireNonNull(sink, "sink");
      for (int row = 0; row < size(); row++) {
        for (int at = starts[row]; at < ends[row]; at++) sink.append(source.charAt(at));
        sink.append(ending(row));
      }
    }
    private int advance(int at, int end, int points) {
      for (int i = 0; i < points && at < end; i++) at += Character.charCount(Character.codePointAt(source, at));
      return at;
    }
  }

  /** On-demand bounded facade; storage remains with the original owner. */
  private static final class Range implements CharSequence {
    private final CharSequence owner;
    private final int from;
    private final int to;
    Range(CharSequence owner, int from, int to) { this.owner = owner; this.from = from; this.to = to; }
    @Override public int length() { return to - from; }
    @Override public char charAt(int index) { return owner.charAt(from + java.util.Objects.checkIndex(index, length())); }
    @Override public CharSequence subSequence(int start, int end) {
      java.util.Objects.checkFromToIndex(start, end, length());
      return start == 0 && end == length() ? this : new Range(owner, from + start, from + end);
    }
    @Override public String toString() {
      StringBuilder text = new StringBuilder(length());
      for (int i = from; i < to; i++) text.append(owner.charAt(i));
      return text.toString();
    }
  }
}
