// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

/**
 * An immutable file text descriptor: a borrowed resolver and packed token IDs.
 * Character payload remains exclusively in the resolver's ordinary JDK Strings.
 * Those Strings are the public M3String owner on the matched M3JDK runtime.
 * This class is the narrow file projection of the donor MIndexString contract;
 * it is not a second general text implementation or a replacement String owner.
 *
 * <p>Leaves retain only primitive IDs and cumulative UTF-16 coordinates. Balanced
 * concatenation shares immutable descriptor nodes; metrics and candidate facts
 * compose from admitted atoms, including surrogate pairs split between atoms.
 * A caller keeps the resolver and its generation valid for the descriptor's
 * lifetime. Existing IDs must never change their String values.</p>
 *
 * <p>{@link #materialize()}, {@link #getBytes(Charset)} and {@link #toString()}
 * are explicit export boundaries. Reads, encoding and hashing traverse borrowed
 * Strings without retaining a joined character or byte payload in this object.</p>
 */
public final class M3FileText implements CharSequence, Comparable<M3FileText> {
  private static final int LEAF_TOKENS = 256;
  private final M3FileResolver resolver;
  private final Node root;

  public M3FileText(M3FileResolver resolver, long... tokenIds) {
    this.resolver = Objects.requireNonNull(resolver, "resolver");
    long[] stable = Objects.requireNonNull(tokenIds, "tokenIds").clone();
    root = build(resolver, stable, 0, stable.length);
  }

  private M3FileText(M3FileResolver resolver, Node root) {
    this.resolver = resolver;
    this.root = root;
  }

  /** Admits one ordinary String atom through the caller's resolver and borrows its ID. */
  public static M3FileText intern(M3FileResolver resolver, int languageId, CharSequence text) {
    M3FileResolver owner = Objects.requireNonNull(resolver, "resolver");
    return new M3FileText(owner, owner.intern(languageId, Objects.requireNonNull(text, "text")));
  }

  public M3FileResolver resolver() { return resolver; }
  public int tokenCount() { return root.tokens; }
  @Override public int length() { return root.metrics.utf16Length(); }
  @Override public boolean isEmpty() { return length() == 0; }
  public int utf8Length() { return root.metrics.utf8Length(); }
  public int codePointCount() { return root.metrics.codePointCount(); }
  public M3TextMetrics metrics() { return root.metrics; }
  public M3TextFacts precomputedFacts() { return root.facts; }

  public long tokenIdAt(int ordinal) {
    int index = Objects.checkIndex(ordinal, root.tokens);
    Node node = root;
    while (node instanceof Branch branch) {
      if (index < branch.left.tokens) node = branch.left;
      else { index -= branch.left.tokens; node = branch.right; }
    }
    return ((Leaf) node).ids[index];
  }

  public long[] tokenIds() {
    long[] result = new long[tokenCount()];
    copyIds(root, result, 0);
    return result;
  }

  @Override public char charAt(int utf16Index) {
    int index = Objects.checkIndex(utf16Index, length());
    Node node = root;
    while (node instanceof Branch branch) {
      int leftLength = branch.left.metrics.utf16Length();
      if (index < leftLength) node = branch.left;
      else { index -= leftLength; node = branch.right; }
    }
    Leaf leaf = (Leaf) node;
    int token = leaf.tokenAtOffset(index);
    return resolver.resolve(leaf.ids[token]).charAt(index - leaf.starts[token]);
  }

  /** Shares immutable ID metadata. Resolver identities must match exactly. */
  public M3FileText concat(M3FileText other) {
    M3FileText value = Objects.requireNonNull(other, "other");
    if (resolver != value.resolver) throw new IllegalArgumentException("resolver mismatch");
    if (value.tokenCount() == 0) return this;
    if (tokenCount() == 0) return value;
    return new M3FileText(resolver, join(root, value.root));
  }

  @Override public M3FileText subSequence(int start, int end) { return substring(start, end); }
  public M3FileText substring(int start) { return substring(start, length()); }

  /**
   * Reuses complete atoms and admits only cut boundary atoms, as the donor does.
   * A frozen resolver refuses a cut spelling absent from its dictionary.
   */
  public M3FileText substring(int start, int end) {
    Objects.checkFromToIndex(start, end, length());
    if (start == 0 && end == length()) return this;
    if (start == end) {
      int language = tokenCount() == 0 ? 0 : resolver.languageId(tokenIdAt(0));
      return new M3FileText(resolver, resolver.intern(language, ""));
    }
    M3FileTextBuilder result = new M3FileTextBuilder(resolver);
    appendRange(root, 0, start, end, result);
    return result.build();
  }

  public void appendTo(StringBuilder target) {
    StringBuilder out = Objects.requireNonNull(target, "target");
    for (int i = 0; i < tokenCount(); i++) resolver.appendTo(tokenIdAt(i), out);
  }

  /** Publishes an ordinary String through the existing JDK composition boundary. */
  public String materialize() {
    if (tokenCount() == 0) return "";
    if (tokenCount() == 1) return resolver.resolve(tokenIdAt(0));
    String[] owners = new String[tokenCount()];
    for (int i = 0; i < owners.length; i++) owners[i] = resolver.resolve(tokenIdAt(i));
    return String.join("", owners);
  }

  public String asString() { return materialize(); }
  @Override public String toString() { return materialize(); }

  /** Explicit byte export with the same malformed-input replacement as String.getBytes. */
  public byte[] getBytes(Charset charset) {
    Objects.requireNonNull(charset, "charset");
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try {
      encode(charset, buffer -> {
        int count = buffer.remaining();
        bytes.write(buffer.array(), buffer.arrayOffset() + buffer.position(), count);
        buffer.position(buffer.limit());
      });
    } catch (IOException impossible) { throw new UncheckedIOException(impossible); }
    return bytes.toByteArray();
  }

  /** Hashes bounded encoding buffers; a surrogate pair may cross any atom boundary. */
  public M3Hash256 sha256(Charset charset) {
    Objects.requireNonNull(charset, "charset");
    MessageDigest digest;
    try { digest = MessageDigest.getInstance("SHA-256"); }
    catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    try { encode(charset, digest::update); }
    catch (IOException impossible) { throw new UncheckedIOException(impossible); }
    return M3Hash256.of(digest.digest());
  }

  /** Writes UTF-8 without closing or retaining the caller's channel. */
  public long writeUtf8To(WritableByteChannel target) throws IOException {
    WritableByteChannel channel = Objects.requireNonNull(target, "target");
    return encode(StandardCharsets.UTF_8, buffer -> {
      while (buffer.hasRemaining()) {
        if (channel.write(buffer) == 0) Thread.onSpinWait();
      }
    });
  }

  @Override public int compareTo(M3FileText other) {
    M3FileText value = Objects.requireNonNull(other, "other");
    if (this == value) return 0;
    Cursor left = new Cursor(this), right = new Cursor(value);
    int common = Math.min(length(), value.length());
    for (int i = 0; i < common; i++) {
      int difference = left.charAt(i) - right.charAt(i);
      if (difference != 0) return difference;
    }
    return length() - value.length();
  }

  @Override public boolean equals(Object other) {
    if (this == other) return true;
    if (!(other instanceof M3FileText value)) return false;
    if (length() != value.length() || hashCode() != value.hashCode()) return false;
    if (root == value.root && resolver == value.resolver) return true;
    return compareTo(value) == 0;
  }

  /** The exact Java String UTF-16 hash, composed from immutable atom metrics. */
  @Override public int hashCode() { return root.metrics.javaHashCode(); }

  private void appendRange(Node node, int base, int start, int end, M3FileTextBuilder out) {
    if (node instanceof Branch branch) {
      int split = Math.addExact(base, branch.left.metrics.utf16Length());
      if (start < split) appendRange(branch.left, base, start, end, out);
      if (end > split) appendRange(branch.right, split, start, end, out);
      return;
    }
    Leaf leaf = (Leaf) node;
    for (int i = 0; i < leaf.ids.length; i++) {
      int from = Math.addExact(base, leaf.starts[i]);
      int to = Math.addExact(base, leaf.starts[i + 1]);
      if (from >= end) break;
      if (to <= start) continue;
      long id = leaf.ids[i];
      if (start <= from && end >= to) out.appendId(id);
      else {
        String owner = resolver.resolve(id);
        String cut = owner.substring(Math.max(start, from) - from, Math.min(end, to) - from);
        out.appendId(resolver.intern(resolver.languageId(id), cut));
      }
    }
  }

  private long encode(Charset charset, M3FileCharsetEncoding.Sink sink) throws IOException {
    return M3FileCharsetEncoding.encode(new Cursor(this)::next, charset, sink);
  }

  /** Ephemeral sequential borrow: caches one existing String reference, never character storage. */
  private static final class Cursor {
    private final M3FileText text;
    private String owner;
    private int start;
    private int end;
    private int next;
    Cursor(M3FileText text) { this.text = text; }
    int length() { return text.length(); }
    int next() { return next == length() ? -1 : charAt(next++); }
    char charAt(int index) {
      Objects.checkIndex(index, length());
      if (owner == null || index < start || index >= end) {
        Node node = text.root;
        int base = 0;
        while (node instanceof Branch branch) {
          int split = Math.addExact(base, branch.left.metrics.utf16Length());
          if (index < split) node = branch.left;
          else { base = split; node = branch.right; }
        }
        Leaf leaf = (Leaf) node;
        int token = leaf.tokenAtOffset(index - base);
        owner = text.resolver.resolve(leaf.ids[token]);
        start = Math.addExact(base, leaf.starts[token]);
        end = Math.addExact(base, leaf.starts[token + 1]);
      }
      return owner.charAt(index - start);
    }
  }

  private abstract static sealed class Node permits Leaf, Branch {
    final int tokens;
    final int height;
    final M3TextMetrics metrics;
    final M3TextFacts facts;
    Node(int tokens, int height, M3TextMetrics metrics, M3TextFacts facts) {
      this.tokens = tokens; this.height = height; this.metrics = metrics; this.facts = facts;
    }
  }

  private static final class Leaf extends Node {
    final long[] ids;
    final int[] starts;
    Leaf(long[] ids, int[] starts, M3TextMetrics metrics, M3TextFacts facts) {
      super(ids.length, 1, metrics, facts); this.ids = ids; this.starts = starts;
    }
    int tokenAtOffset(int offset) {
      int low = 0, high = ids.length;
      while (low < high) {
        int middle = (low + high) >>> 1;
        if (starts[middle + 1] <= offset) low = middle + 1;
        else high = middle;
      }
      return low;
    }
  }

  private static final class Branch extends Node {
    final Node left;
    final Node right;
    Branch(Node left, Node right) {
      super(Math.addExact(left.tokens, right.tokens), Math.max(left.height, right.height) + 1,
          M3TextMetrics.combine(left.metrics, right.metrics),
          M3TextFacts.combine(left.facts, left.metrics, right.facts, right.metrics));
      this.left = left; this.right = right;
    }
  }

  private static Node build(M3FileResolver resolver, long[] ids, int from, int to) {
    int count = to - from;
    if (count > LEAF_TOKENS) {
      int middle = from + (count >>> 1);
      return new Branch(build(resolver, ids, from, middle), build(resolver, ids, middle, to));
    }
    long[] leafIds = Arrays.copyOfRange(ids, from, to);
    int[] starts = new int[count + 1];
    M3TextMetrics metrics = M3TextMetrics.EMPTY;
    M3TextFacts facts = M3TextFacts.EMPTY;
    for (int i = 0; i < count; i++) {
      M3TextMetrics atom = Objects.requireNonNull(resolver.metrics(leafIds[i]), "metrics");
      M3TextFacts atomFacts = Objects.requireNonNull(resolver.facts(leafIds[i]), "facts");
      facts = M3TextFacts.combine(facts, metrics, atomFacts, atom);
      metrics = M3TextMetrics.combine(metrics, atom);
      starts[i + 1] = metrics.utf16Length();
    }
    return new Leaf(leafIds, starts, metrics, facts);
  }

  private static int copyIds(Node node, long[] target, int offset) {
    if (node instanceof Leaf leaf) {
      System.arraycopy(leaf.ids, 0, target, offset, leaf.ids.length);
      return offset + leaf.ids.length;
    }
    Branch branch = (Branch) node;
    return copyIds(branch.right, target, copyIds(branch.left, target, offset));
  }

  private static Node join(Node left, Node right) {
    if (left.tokens == 0) return right;
    if (right.tokens == 0) return left;
    if (left.height > right.height + 1) {
      Branch branch = (Branch) left;
      return balance(branch.left, join(branch.right, right));
    }
    if (right.height > left.height + 1) {
      Branch branch = (Branch) right;
      return balance(join(left, branch.left), branch.right);
    }
    return new Branch(left, right);
  }

  private static Node balance(Node left, Node right) {
    if (left.height > right.height + 1) {
      Branch branch = (Branch) left;
      if (branch.left.height >= branch.right.height) {
        return new Branch(branch.left, new Branch(branch.right, right));
      }
      Branch middle = (Branch) branch.right;
      return new Branch(new Branch(branch.left, middle.left), new Branch(middle.right, right));
    }
    if (right.height > left.height + 1) {
      Branch branch = (Branch) right;
      if (branch.right.height >= branch.left.height) {
        return new Branch(new Branch(left, branch.left), branch.right);
      }
      Branch middle = (Branch) branch.left;
      return new Branch(new Branch(left, middle.left), new Branch(middle.right, branch.right));
    }
    return new Branch(left, right);
  }
}
