// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package jdk.internal.mindex;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Random;
import java.util.concurrent.CancellationException;

/** Full mapped codec and existing numeric runtime; not a full String/HotSpot integration test. */
public final class RelationImageProof {
  private static final String SCOPE_A = "a".repeat(64);
  private static final String SCOPE_B = "b".repeat(64);
  private static long checks;
  private static long images;
  private static boolean nativeMode;
  private static final MessageDigest observations = digest();
  private RelationImageProof() { }

  private static MessageDigest digest() {
    try { return MessageDigest.getInstance("SHA-256"); }
    catch (Exception impossible) { throw new AssertionError(impossible); }
  }
  private static String hash(byte[] image) { return HexFormat.of().formatHex(digest().digest(image)); }
  private static void check(boolean ok, String why) {
    checks++;
    if (!ok) throw new AssertionError(why);
  }
  private static void same(long[] a, long[] b, String why) {
    check(Arrays.equals(a,b), why);
    for (long word : b) for (int shift = 56; shift >= 0; shift -= 8)
      observations.update((byte)(word >>> shift));
  }
  private static void refuse(Class<? extends Throwable> expected, Runnable action) {
    try { action.run(); }
    catch (Throwable failure) {
      check(expected.isInstance(failure), "wrong exception " + failure); return;
    }
    throw new AssertionError("did not refuse " + expected.getSimpleName());
  }
  private static long[] slow(long[] left, long[] right) {
    long[] result = new long[left.length];
    for (int i = 0; i < left.length; i++) for (int j = 0; j < left.length; j++)
      if ((left[i] & (1L << j)) != 0L) result[i] |= right[j];
    return result;
  }
  private static long[] compose(long[] a, long[] b) {
    long[] result = new long[a.length];
    M3StateRelation.compose(a, 0, b, 0, result, 0, a.length);
    return result;
  }
  private static long[] restored(long[] rows, String scope) {
    byte[] image = M3RelationImage.encode(rows, scope, null);
    images++;
    check(image.length == 44 + 8 * rows.length, "bounded wire size");
    byte[] before = image.clone();
    long[] result = M3RelationImage.decode(image, hash(image), scope, rows.length, null);
    check(Arrays.equals(image, before), "decode altered input");
    same(rows, result, "round trip");
    return result;
  }
  private static void campaign() throws Exception {
    Random random = new Random(0x4d3352493031L);
    for (int states = 1; states <= 64; states++) for (int trial = 0; trial < 128; trial++) {
      long mask = states == 64 ? -1L : (1L << states) - 1;
      long[] a = new long[states], b = new long[states];
      for (int row = 0; row < states; row++) {
        a[row] = random.nextLong() & mask;
        b[row] = random.nextLong() & mask;
      }
      if (trial % 4 == 0) Arrays.fill(a, a[0]);
      if (trial % 8 == 0) Arrays.fill(b, 0);
      long[] oldA = a.clone(), oldB = b.clone();
      long[] aa = restored(a, SCOPE_A), bb = restored(b, SCOPE_B);
      long[] expected = slow(a, b);
      same(expected, compose(aa,bb), "Java restored composition");
      long[] parent = restored(expected, SCOPE_A);
      same(slow(expected,a), compose(parent,aa), "DP composed on restored DP");
      if (nativeMode) {
        same(expected, M3StateRelation.composeNative(aa,bb), "JNI restored composition");
        same(slow(a,a), M3StateRelation.composeNative(aa,aa), "JNI input alias");
        long[] power = new long[states];
        for (int row = 0; row < states; row++) power[row] = 1L << row;
        int count = trial % 9;
        for (int n = 0; n < count; n++) power = slow(power,a);
        same(power, M3StateRelation.repeatNative(aa,count), "JNI restored repetition");
      }
      same(oldA,a,"capture input mutation"); same(oldB,b,"compose input mutation");
      same(a,aa,"restored input mutation"); same(b,bb,"restored right mutation");
    }
    // Independently parse the wire using DataInput, not the codec's ByteBuffer operations.
    byte[] known = M3RelationImage.encode(new long[]{2,1},SCOPE_A,null);
    try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(known))) {
      check(input.readLong() == 0x4d33524930303031L, "wire version");
      check(input.readInt() == 2, "wire state count");
      check(Arrays.equals(input.readNBytes(32),HexFormat.of().parseHex(SCOPE_A)),"wire scope");
      check(input.readLong()==2 && input.readLong()==1 && input.read()==-1,"wire rows and EOF");
    }
  }
  private static void boundaries() {
    long[] rows = {2,1}; byte[] image = M3RelationImage.encode(rows,SCOPE_A,null);
    String sha = hash(image);
    refuse(NullPointerException.class, () -> M3RelationImage.encode(null,SCOPE_A,null));
    refuse(NullPointerException.class, () -> M3RelationImage.decode(null,sha,SCOPE_A,2,null));
    for (int count : new int[]{0,65}) {
      refuse(IllegalArgumentException.class, () -> M3RelationImage.encode(new long[count],SCOPE_A,null));
      refuse(IllegalArgumentException.class, () -> M3RelationImage.decode(image,sha,SCOPE_A,count,null));
    }
    refuse(IllegalArgumentException.class, () -> M3RelationImage.encode(new long[]{2},SCOPE_A,null));
    refuse(IllegalArgumentException.class, () -> M3RelationImage.decode(image,sha,SCOPE_B,2,null));
    refuse(IllegalArgumentException.class, () -> M3RelationImage.decode(image,SCOPE_B,SCOPE_A,2,null));
    refuse(IllegalArgumentException.class, () -> M3RelationImage.decode(image,"x".repeat(64),SCOPE_A,2,null));
    refuse(IllegalArgumentException.class, () -> M3RelationImage.decode(new byte[557],sha,SCOPE_A,2,null));
    for (int length = 0; length < image.length; length++) {
      byte[] shortImage = Arrays.copyOf(image,length);
      refuse(IllegalArgumentException.class, () -> M3RelationImage.decode(shortImage,hash(shortImage),SCOPE_A,2,null));
    }
    byte[] extra = Arrays.copyOf(image,image.length+1);
    refuse(IllegalArgumentException.class, () -> M3RelationImage.decode(extra,hash(extra),SCOPE_A,2,null));
    for (int offset = 0; offset < image.length; offset++) for (int bit = 0; bit < 8; bit++) {
      byte[] corrupt = image.clone(); corrupt[offset] ^= (byte)(1 << bit);
      refuse(IllegalArgumentException.class, () -> M3RelationImage.decode(corrupt,sha,SCOPE_A,2,null));
    }
    // Even a rehashed packet cannot bypass geometry, version, expected scope or state domain.
    for (int offset : new int[]{0,11,12,44}) {
      byte[] corrupt = image.clone(); corrupt[offset] ^= (byte)0x40;
      refuse(IllegalArgumentException.class, () -> M3RelationImage.decode(corrupt,hash(corrupt),SCOPE_A,2,null));
    }
    long[] source = {2,1}; final int[] count = {0};
    byte[] captured = M3RelationImage.encode(source,SCOPE_A,() -> {
      if (++count[0] == 2) Arrays.fill(source,0);
    });
    same(rows,M3RelationImage.decode(captured,hash(captured),SCOPE_A,2,null),"capture snapshot");
    byte[] mutable = image.clone(); count[0]=0;
    same(rows,M3RelationImage.decode(mutable,sha,SCOPE_A,2,() -> {
      if (++count[0] == 2) Arrays.fill(mutable,(byte)0);
    }),"decode checked snapshot");
    long[] output = M3RelationImage.decode(image,sha,SCOPE_A,2,null); output[0]=0;
    same(rows,M3RelationImage.decode(image,sha,SCOPE_A,2,null),"caller mutation isolation");
    for (int cancel = 1; cancel <= 5; cancel++) {
      final int at = cancel; count[0]=0;
      refuse(CancellationException.class, () -> M3RelationImage.encode(rows,SCOPE_A,() -> {
        if (++count[0]==at) throw new CancellationException("checkpoint");
      }));
      count[0]=0;
      refuse(CancellationException.class, () -> M3RelationImage.decode(image,sha,SCOPE_A,2,() -> {
        if (++count[0]==at) throw new CancellationException("checkpoint");
      }));
    }
    Thread.currentThread().interrupt();
    try {
      same(rows,M3RelationImage.decode(image,sha,SCOPE_A,2,null),"null checkpoint");
      check(Thread.currentThread().isInterrupted(),"interrupt retained");
    } finally { Thread.interrupted(); }
    if (nativeMode) {
      refuse(NullPointerException.class, () -> M3StateRelation.composeNative(null,new long[]{1}));
      refuse(NullPointerException.class, () -> M3StateRelation.composeNative(new long[]{1},null));
      for (int size : new int[]{0,65}) {
        refuse(IllegalArgumentException.class, () -> M3StateRelation.composeNative(new long[size],new long[size]));
      }
      refuse(IllegalArgumentException.class, () -> M3StateRelation.composeNative(new long[]{1},new long[]{1,2}));
      refuse(IllegalArgumentException.class, () -> M3StateRelation.composeNative(new long[]{2},new long[]{1}));
      refuse(IllegalArgumentException.class, () -> M3StateRelation.composeNative(new long[]{1},new long[]{2}));
      refuse(IllegalArgumentException.class, () -> M3StateRelation.repeatNative(new long[]{1},-1));
    }
  }
  private static void emit(Path directory) throws Exception {
    Files.createDirectories(directory);
    byte[] a=M3RelationImage.encode(new long[]{1,4,4},SCOPE_A,null);
    byte[] b=M3RelationImage.encode(new long[]{2,4,4},SCOPE_B,null);
    Files.write(directory.resolve("a.blob"),a);Files.write(directory.resolve("b.blob"),b);
    // This receipt must be trusted separately from blobs read later, never reconstructed from them.
    Files.writeString(directory.resolve("receipt.txt"),hash(a)+"\n"+hash(b)+"\n");
    System.out.println("IMAGE_EMIT records=2 bytes="+(a.length+b.length)+" PASS");
  }
  private static void replay(Path directory, String hashA, String hashB) throws Exception {
    long[] a=M3RelationImage.decode(Files.readAllBytes(directory.resolve("a.blob")),hashA,SCOPE_A,3,null);
    long[] b=M3RelationImage.decode(Files.readAllBytes(directory.resolve("b.blob")),hashB,SCOPE_B,3,null);
    same(new long[]{2,4,4},compose(a,b),"cross-fragment acceptance");
    check(compose(b,a)[0]==4,"occurrence order retained");
    if (nativeMode) {
      long[] repeated=M3StateRelation.repeatNative(a,1_000_000L);
      same(a,repeated,"million numeric repetitions");
      same(compose(a,b),M3StateRelation.composeNative(repeated,b),"fresh JVM JNI composition");
    }
    System.out.println("IMAGE_REPLAY records=2 sourceCharacterReads=0 native="+nativeMode+" PASS");
  }
  public static void main(String[] args) throws Exception {
    if (args.length>0 && args[0].equals("emit")) { emit(Path.of(args[1])); return; }
    if (args.length>0 && args[0].equals("replay")) {
      nativeMode=args.length==5;
      if (nativeMode) System.load(args[4]);
      replay(Path.of(args[1]),args[2],args[3]);return;
    }
    nativeMode=args.length==1;
    if (nativeMode) System.load(args[0]);
    campaign(); boundaries();
    System.out.println("RELATION_IMAGE_PROOF images="+images+" checks="+checks+" native="+nativeMode+
        " observations="+HexFormat.of().formatHex(observations.digest())+" PASS");
  }
}
