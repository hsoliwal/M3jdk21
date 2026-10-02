// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Weak process-wide interner for immutable joined character/byte descriptor bodies.
 * Payload remains owned by FrozenChars/FrozenBytes and their MIndexString/precomputation owner.
 * Backing kind is part of the descriptor key: a heap tuple cannot displace a mapped/direct one.
 * Weak values keep concatenation bodies from becoming permanent process retention.
 */
final class MIndexJoinedStorageIntern {
  private static final long HASH_SEED = 0x9e3779b97f4a7c15L;
  private static final AtomicLong NEXT_CHAR_BODY_ID = new AtomicLong(1L);
  private static final AtomicLong NEXT_BYTE_BODY_ID = new AtomicLong(1L);
  private static final ConcurrentHashMap<Fingerprint, CharBucket> CHAR_BUCKETS = new ConcurrentHashMap<>();
  private static final ConcurrentHashMap<Fingerprint, ByteBucket> BYTE_BUCKETS = new ConcurrentHashMap<>();
  private static final ReferenceQueue<CharBody> CHAR_QUEUE = new ReferenceQueue<>();
  private static final ReferenceQueue<ByteBody> BYTE_QUEUE = new ReferenceQueue<>();
  private MIndexJoinedStorageIntern() {}

  static CharBody internChars(FrozenChars[] parts) {
    return internChars(parts, false);
  }
  private static CharBody internChars(FrozenChars[] parts, boolean retained) {
    FrozenChars[] checked = Objects.requireNonNull(parts, "parts");
    expungeChars();
    Fingerprint base = charFingerprint(checked);
    Fingerprint fingerprint = retained
        ? new Fingerprint(base.segmentCount(), base.totalLength(), base.hash32(), base.hash64(), true)
        : base;
    for (;;) {
      CharBucket bucket = CHAR_BUCKETS.computeIfAbsent(fingerprint, ignored -> new CharBucket());
      synchronized (bucket) {
        if (bucket.retired) continue;
        for (Iterator<CharRef> iterator = bucket.values.iterator(); iterator.hasNext(); ) {
          CharRef reference = iterator.next();
          CharBody body = reference.get();
          if (body == null) iterator.remove();
          else if (retained ? body.matchesRetained(checked) : body.matches(checked)) return body;
        }
        CharBody created = new CharBody(checked, fingerprint, nextId(NEXT_CHAR_BODY_ID));
        bucket.values.add(new CharRef(created, fingerprint, bucket));
        return created;
      }
    }
  }
  static CharBody internRetainedChars(FrozenChars[] parts) {
    Objects.requireNonNull(parts, "parts");
    charFingerprint(parts); // Check logical length before allocating the normalized directory.
    FrozenChars[] snapshot = parts.clone();
    charFingerprint(snapshot);
    ArrayList<FrozenChars> normalized = new ArrayList<>();
    for (FrozenChars part : snapshot) {
      if (part.length() == 0) continue;
      if (!normalized.isEmpty()) {
        int last = normalized.size() - 1;
        FrozenChars merged = normalized.get(last).coalesceAdjacent(part);
        if (merged != null) { normalized.set(last, merged); continue; }
      }
      normalized.add(part);
    }
    return internChars(normalized.toArray(FrozenChars[]::new), true);
  }
  static ByteBody internBytes(FrozenBytes[] parts) {
    FrozenBytes[] checked = Objects.requireNonNull(parts, "parts");
    expungeBytes();
    Fingerprint fingerprint = byteFingerprint(checked);
    for (;;) {
      ByteBucket bucket = BYTE_BUCKETS.computeIfAbsent(fingerprint, ignored -> new ByteBucket());
      synchronized (bucket) {
        if (bucket.retired) continue;
        for (Iterator<ByteRef> iterator = bucket.values.iterator(); iterator.hasNext(); ) {
          ByteRef reference = iterator.next();
          ByteBody body = reference.get();
          if (body == null) iterator.remove();
          else if (body.matches(checked)) return body;
        }
        ByteBody created = new ByteBody(checked, fingerprint, nextId(NEXT_BYTE_BODY_ID));
        bucket.values.add(new ByteRef(created, fingerprint, bucket));
        return created;
      }
    }
  }
  private static Fingerprint charFingerprint(FrozenChars[] parts) {
    int segmentCount = 0, totalLength = 0, hash32 = 1;
    long hash64 = HASH_SEED;
    for (FrozenChars part : parts) {
      FrozenChars checked = Objects.requireNonNull(part, "part");
      if (checked.length() == 0) continue;
      segmentCount++;
      totalLength = Math.addExact(totalLength, checked.length());
      hash32 = 31 * hash32 + checked.length();
      hash32 = 31 * hash32 + checked.hash32();
      hash32 = 31 * hash32 + (checked.isDirect() ? 1 : 0);
      hash64 = mix64(hash64 ^ Integer.toUnsignedLong(checked.length()));
      hash64 = mix64(hash64 ^ Integer.toUnsignedLong(checked.hash32()));
    }
    return new Fingerprint(segmentCount, totalLength, hash32, hash64);
  }
  private static Fingerprint byteFingerprint(FrozenBytes[] parts) {
    int segmentCount = 0, totalLength = 0, hash32 = 1;
    long hash64 = HASH_SEED;
    for (FrozenBytes part : parts) {
      FrozenBytes checked = Objects.requireNonNull(part, "part");
      if (checked.length() == 0) continue;
      segmentCount++;
      totalLength = Math.addExact(totalLength, checked.length());
      hash32 = 31 * hash32 + checked.length();
      hash32 = 31 * hash32 + checked.hash32();
      hash32 = 31 * hash32 + (checked.isDirect() ? 1 : 0);
      hash64 = mix64(hash64 ^ Integer.toUnsignedLong(checked.length()));
      hash64 = mix64(hash64 ^ Integer.toUnsignedLong(checked.hash32()));
    }
    return new Fingerprint(segmentCount, totalLength, hash32, hash64);
  }
  private static void expungeChars() {
    CharRef reference;
    while ((reference = (CharRef) CHAR_QUEUE.poll()) != null) {
      CharBucket bucket = reference.bucket;
      synchronized (bucket) {
        bucket.values.remove(reference);
        if (bucket.values.isEmpty() && CHAR_BUCKETS.remove(reference.fingerprint, bucket)) bucket.retired = true;
      }
    }
  }
  private static void expungeBytes() {
    ByteRef reference;
    while ((reference = (ByteRef) BYTE_QUEUE.poll()) != null) {
      ByteBucket bucket = reference.bucket;
      synchronized (bucket) {
        bucket.values.remove(reference);
        if (bucket.values.isEmpty() && BYTE_BUCKETS.remove(reference.fingerprint, bucket)) bucket.retired = true;
      }
    }
  }
  private static long nextId(AtomicLong sequence) {
    long id = sequence.getAndUpdate(current -> current > 0L && current < Long.MAX_VALUE ? current + 1L : 0L);
    if (id <= 0L) throw new IllegalStateException("joined backing ID space exhausted");
    return id;
  }
  private static long mix64(long value) {
    long mixed = value;
    mixed ^= mixed >>> 30; mixed *= 0xbf58476d1ce4e5b9L;
    mixed ^= mixed >>> 27; mixed *= 0x94d049bb133111ebL;
    return mixed ^ (mixed >>> 31);
  }
  private record Fingerprint(int segmentCount, int totalLength, int hash32, long hash64, boolean retained) {
    Fingerprint(int segmentCount, int totalLength, int hash32, long hash64) {
      this(segmentCount, totalLength, hash32, hash64, false);
    }
  }

  static final class CharBody {
    final FrozenChars[] segments;
    final int[] ends;
    final int length;
    final long canonicalId;
    private volatile MIndexJoinedChars canonicalView;
    CharBody(FrozenChars[] parts, Fingerprint fingerprint, long canonicalId) {
      segments = new FrozenChars[fingerprint.segmentCount()];
      ends = new int[segments.length];
      int segment = 0, total = 0;
      for (FrozenChars part : parts) {
        if (part.length() == 0) continue;
        segments[segment] = part;
        total = Math.addExact(total, part.length()); ends[segment++] = total;
      }
      length = total; this.canonicalId = canonicalId;
    }
    boolean matches(FrozenChars[] parts) {
      int segment = 0;
      for (FrozenChars part : parts) {
        FrozenChars checked = Objects.requireNonNull(part, "part");
        if (checked.length() == 0) continue;
        if (segment >= segments.length || !sameChars(segments[segment], checked)) return false;
        segment++;
      }
      return segment == segments.length;
    }
    boolean matchesRetained(FrozenChars[] parts) {
      int segment = 0;
      for (FrozenChars part : parts) {
        if (part.length() == 0) continue;
        if (segment >= segments.length || !segments[segment].sameOwnerAndRange(part)) return false;
        segment++;
      }
      return segment == segments.length;
    }
    MIndexJoinedChars canonicalView() {
      MIndexJoinedChars result = canonicalView;
      if (result != null) return result;
      synchronized (this) {
        result = canonicalView;
        if (result == null) { result = MIndexJoinedChars.full(this); canonicalView = result; }
        return result;
      }
    }
  }
  static final class ByteBody {
    final FrozenBytes[] segments;
    final int[] ends;
    final int length;
    final long canonicalId;
    private volatile MIndexJoinedBytes canonicalView;
    ByteBody(FrozenBytes[] parts, Fingerprint fingerprint, long canonicalId) {
      segments = new FrozenBytes[fingerprint.segmentCount()];
      ends = new int[segments.length];
      int segment = 0, total = 0;
      for (FrozenBytes part : parts) {
        if (part.length() == 0) continue;
        segments[segment] = part;
        total = Math.addExact(total, part.length()); ends[segment++] = total;
      }
      length = total; this.canonicalId = canonicalId;
    }
    boolean matches(FrozenBytes[] parts) {
      int segment = 0;
      for (FrozenBytes part : parts) {
        FrozenBytes checked = Objects.requireNonNull(part, "part");
        if (checked.length() == 0) continue;
        if (segment >= segments.length || !sameBytes(segments[segment], checked)) return false;
        segment++;
      }
      return segment == segments.length;
    }
    MIndexJoinedBytes canonicalView() {
      MIndexJoinedBytes result = canonicalView;
      if (result != null) return result;
      synchronized (this) {
        result = canonicalView;
        if (result == null) { result = MIndexJoinedBytes.full(this); canonicalView = result; }
        return result;
      }
    }
  }
  private static boolean sameChars(FrozenChars left, FrozenChars right) {
    if (left == right) return true;
    int length = left.length();
    if (left.isDirect() != right.isDirect() || length != right.length() || left.hash32() != right.hash32()) return false;
    for (int index = 0; index < length; index++) if (left.charAt(index) != right.charAt(index)) return false;
    return true;
  }
  private static boolean sameBytes(FrozenBytes left, FrozenBytes right) {
    if (left == right) return true;
    int length = left.length();
    if (left.isDirect() != right.isDirect() || length != right.length() || left.hash32() != right.hash32()) return false;
    for (int index = 0; index < length; index++) if (left.byteAt(index) != right.byteAt(index)) return false;
    return true;
  }
  private static final class CharBucket {
    final ArrayList<CharRef> values = new ArrayList<>(); boolean retired;
  }
  private static final class ByteBucket {
    final ArrayList<ByteRef> values = new ArrayList<>(); boolean retired;
  }
  private static final class CharRef extends WeakReference<CharBody> {
    final Fingerprint fingerprint; final CharBucket bucket;
    CharRef(CharBody value, Fingerprint fingerprint, CharBucket bucket) {
      super(value, CHAR_QUEUE); this.fingerprint = fingerprint; this.bucket = bucket;
    }
  }
  private static final class ByteRef extends WeakReference<ByteBody> {
    final Fingerprint fingerprint; final ByteBucket bucket;
    ByteRef(ByteBody value, Fingerprint fingerprint, ByteBucket bucket) {
      super(value, BYTE_QUEUE); this.fingerprint = fingerprint; this.bucket = bucket;
    }
  }
}
