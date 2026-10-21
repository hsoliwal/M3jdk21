// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.RecordComponent;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.ShortBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Observable owner/consumer contracts; the fixture never accesses private fields. */
class M3FsPreparedViewCursorTest {
  @TempDir Path temp;

  @Test
  void readingSegmentMetadataCannotRetargetAnAdmittedRow() {
    var segments = (M3FsPrecomputeBundle.PreparedByteSegments) segments(plain()).owner();
    assertEquals(2, segments.metadata().get());
    assertEquals(2, segments.size());
    assertEquals(1, segments.segmentStart(1));
    assertEquals(1, segments.segmentLength(1));
  }

  @Test
  void readingPatternBytesCannotRetargetPatternCoordinates() {
    var machine = (M3FsPrecomputeBundle.PreparedBytePatternMachine) machine(plain()).owner();
    assertEquals('a', machine.patternBytes().get());
    assertEquals(2, machine.totalPatternBytes());
    assertEquals('a', machine.patternByteAt(0, 0));
    assertEquals('b', machine.patternByteAt(0, 1));
  }

  @Test
  void allNineteenExportedCursorsPreserveWindowMarkOrderAndOwnerBehavior() throws Exception {
    int checked = 0;
    for (Shape shape : shapes()) {
      for (Fixture fixture : fixtures(shape)) checked += checkProjections(fixture);
    }
    assertEquals(19 * 16, checked);
  }

  @Test
  void constructorInputCursorsRemainIndependentOfCapturedViews() throws Exception {
    for (Shape shape : shapes()) {
      for (Fixture fixture : fixtures(shape)) {
        int hash = fixture.owner().hashCode();
        for (Buffer input : fixture.inputs()) disturb(input);
        fixture.oracle().run();
        assertEquals(hash, fixture.owner().hashCode());
      }
    }
  }

  @Test
  void rawConstructorsContinueSharingTheirSuppliedPayload() throws Exception {
    for (Shape shape : shapes()) {
      if (shape.readOnly()) continue;
      for (Fixture fixture : fixtures(shape)) {
        RecordComponent[] components = fixture.owner().getClass().getRecordComponents();
        for (int i = 0; i < components.length; i++) {
          Buffer input = fixture.inputs()[i];
          int at = input.position();
          long original = readAt(input, at);
          long changed = original ^ 1L;
          writeAt(input, at, changed);
          try {
            Buffer exported = (Buffer) components[i].getAccessor().invoke(fixture.owner());
            assertEquals(changed, readAt(exported, exported.position()), components[i].getName());
          } finally {
            writeAt(input, at, original);
          }
        }
        fixture.oracle().run();
      }
    }
  }

  @Test
  void emptyPreparedSegmentsKeepTheirNonzeroEmptyWindows() throws Exception {
    for (Shape shape : shapes()) {
      Buffer[] inputs = {
          bytes(shape), ints(shape), ints(shape), longs(shape), longs(shape), longs(shape),
          ints(shape, 0, 0, 1, 1, 0)};
      var owner = new M3FsPrecomputeBundle.PreparedByteSegments(
          (ByteBuffer) inputs[0], (IntBuffer) inputs[1], (IntBuffer) inputs[2],
          (LongBuffer) inputs[3], (LongBuffer) inputs[4], (LongBuffer) inputs[5],
          (IntBuffer) inputs[6]);
      checkProjections(new Fixture(owner, inputs, () -> {
        assertEquals(0, owner.size());
        assertEquals(0, owner.packedBytes());
        assertEquals(0, owner.bitmapWords());
        assertEquals(0, owner.maxSegmentLength());
        assertThrows(IndexOutOfBoundsException.class, () -> owner.byteAt(0));
        assertThrows(IndexOutOfBoundsException.class, () -> owner.segmentStart(0));
      }));
    }
  }

  @Test
  void constructorRefusalsAndOverflowPrecedenceRemainUnchanged() {
    assertEquals("bytes", assertThrows(NullPointerException.class, () ->
        new M3FsPrecomputeBundle.PreparedByteSegments(null, null, null, null, null, null, null))
        .getMessage());
    assertEquals("patternBytes", assertThrows(NullPointerException.class, () ->
        new M3FsPrecomputeBundle.PreparedBytePatternMachine(null, null, null, null, null))
        .getMessage());
    assertEquals("asciiClassByValue", assertThrows(NullPointerException.class, () ->
        new M3FsPrecomputeBundle.CompactAsciiDfa(null, null, null, null)).getMessage());
    assertEquals("bytes", assertThrows(NullPointerException.class, () ->
        new M3FsPrecomputeBundle.GpuGrepSegments(null, null, null)).getMessage());

    assertThrows(IllegalArgumentException.class, () -> segmentGeometry(
        new byte[]{'a'}, new int[]{1}, new int[]{1}, new int[]{1, 1, 1, 1, 1}));
    assertThrows(IllegalArgumentException.class, () -> segmentGeometry(
        new byte[]{'a'}, new int[]{0}, new int[]{2}, new int[]{1, 1, 1, 1, 2}));
    assertThrows(ArithmeticException.class, () -> segmentGeometry(
        new byte[0], new int[]{0, Integer.MAX_VALUE}, new int[]{Integer.MAX_VALUE, 1},
        new int[]{2, 1, 1, 1, Integer.MAX_VALUE}));
    assertThrows(IllegalArgumentException.class, () ->
        new M3FsPrecomputeBundle.PreparedBytePatternMachine(ByteBuffer.wrap(new byte[]{1}),
            IntBuffer.wrap(new int[]{0, 0}), IntBuffer.wrap(new int[256]),
            LongBuffer.wrap(new long[1]), IntBuffer.wrap(new int[]{1, 1, 256, 0})));
    assertThrows(ArithmeticException.class, () ->
        new M3FsPrecomputeBundle.PreparedBytePatternMachine(ByteBuffer.wrap(new byte[]{1}),
            IntBuffer.wrap(new int[]{0, 1}), IntBuffer.allocate(0), LongBuffer.allocate(0),
            IntBuffer.wrap(new int[]{1, Integer.MAX_VALUE, 256, 0})));
    assertThrows(IllegalArgumentException.class, () ->
        new M3FsPrecomputeBundle.CompactAsciiDfa(ByteBuffer.wrap(new byte[128]),
            ShortBuffer.wrap(new short[]{1}), ByteBuffer.wrap(new byte[]{0}),
            IntBuffer.wrap(new int[]{0, 1, 1})));
    assertThrows(IllegalArgumentException.class, () ->
        new M3FsPrecomputeBundle.GpuGrepSegments(ByteBuffer.wrap(new byte[]{1}),
            LongBuffer.wrap(new long[]{0, 2}), IntBuffer.wrap(new int[]{2})));
  }

  @Test
  void cursorRepairDoesNotInventAdditionalProducerValidation() {
    var segments = segmentGeometry(new byte[]{'a'}, new int[]{0}, new int[]{1},
        new int[]{1, 1, 1, 1, 0});
    assertEquals(0, segments.maxSegmentLength());
    int[] targets = new int[256];
    java.util.Arrays.fill(targets, -7);
    var machine = new M3FsPrecomputeBundle.PreparedBytePatternMachine(
        ByteBuffer.wrap(new byte[]{'a'}), IntBuffer.wrap(new int[]{0, 1}),
        IntBuffer.wrap(targets), LongBuffer.wrap(new long[]{Long.MIN_VALUE}),
        IntBuffer.wrap(new int[]{1, 1, 256, 0}));
    assertEquals(-7, machine.nextState(0, 0));
    assertEquals(Long.MIN_VALUE, machine.outputMask(0));
  }

  @Test
  void actualMappedBundlesKeepConsumerBehaviorAfterExportedReads() throws Exception {
    M3DirectoryContentStore store = new M3DirectoryContentStore(temp.resolve("mapped"));
    M3PrecomputeKey key = new M3PrecomputeKey("cursor", "views", 1,
        List.of(M3ContentId.digest(new byte[]{7})), Map.of());
    var prepared = (M3FsPrecomputeBundle.PreparedByteSegments) segments(plain()).owner();
    var automaton = (M3FsPrecomputeBundle.PreparedBytePatternMachine) machine(plain()).owner();
    var compact = (M3FsPrecomputeBundle.CompactAsciiDfa) compact(plain()).owner();
    var grep = (M3FsPrecomputeBundle.GpuGrepSegments) grep(plain()).owner();

    var mappedSegments = new M3FsPrecomputeWriter(store, key, "gpu-prepared-byte-segments-v1")
        .addPreparedByteSegments(prepared).bundle().openMapped(store, 16_384, 65_536)
        .preparedByteSegments();
    var mappedMachine = new M3FsPrecomputeWriter(store, key, "gpu-prepared-byte-machine-v1")
        .addPreparedBytePatternMachine(automaton).bundle().openMapped(store, 16_384, 65_536)
        .preparedBytePatternMachine();
    var mappedCompact = new M3FsPrecomputeWriter(store, key, "compact-ascii-dfa-v1")
        .addCompactAsciiDfa(byteArray(compact.asciiClassByValue()), 2,
            shortArray(compact.transitions()), byteArray(compact.accepting()), 0)
        .bundle().openMapped(store, 16_384, 65_536).compactAsciiDfa();
    var mappedGrep = new M3FsPrecomputeWriter(store, key, "gpu-grep-segments-v1")
        .addGpuGrepSegments(byteArray(grep.bytes()), new long[]{0, 1, 2}, new int[]{1, 1})
        .bundle().openMapped(store, 16_384, 65_536).gpuGrepSegments();

    for (Object owner : List.of(mappedSegments, mappedMachine, mappedCompact, mappedGrep)) {
      for (RecordComponent component : owner.getClass().getRecordComponents()) {
        Buffer exported = (Buffer) component.getAccessor().invoke(owner);
        assertTrue(exported.isDirect());
        consume(exported);
        disturb(exported);
      }
    }
    assertSegments(mappedSegments);
    assertMachine(mappedMachine);
    assertCompact(mappedCompact);
    assertGrep(mappedGrep);
    int[] rows = {-1, -1};
    assertEquals(1, mappedGrep.grepSegments(new byte[]{'a'}, rows));
    assertEquals(0, rows[0]);
    assertEquals(-1, rows[1]);
  }

  private static int checkProjections(Fixture fixture) throws Exception {
    Object owner = fixture.owner();
    RecordComponent[] components = owner.getClass().getRecordComponents();
    Object[] arguments = new Object[components.length];
    Class<?>[] types = new Class<?>[components.length];
    for (int i = 0; i < components.length; i++) {
      types[i] = components[i].getType();
      arguments[i] = components[i].getAccessor().invoke(owner);
    }
    Object reconstructed = owner.getClass().getConstructor(types).newInstance(arguments);
    assertEquals(owner, reconstructed);
    int hash = owner.hashCode();
    String description = owner.toString();
    for (int i = 0; i < components.length; i++) {
      Buffer first = (Buffer) components[i].getAccessor().invoke(owner);
      Buffer sibling = (Buffer) components[i].getAccessor().invoke(owner);
      checkShape(fixture.inputs()[i], first);
      consume(first);
      disturb(first);
      checkShape(fixture.inputs()[i], sibling);
      checkShape(fixture.inputs()[i], (Buffer) components[i].getAccessor().invoke(owner));
      fixture.oracle().run();
      assertEquals(hash, owner.hashCode());
      assertEquals(description, owner.toString());
      assertEquals(reconstructed, owner);
    }
    return components.length;
  }

  private static void checkShape(Buffer input, Buffer exported) {
    assertEquals(input.position(), exported.position());
    assertEquals(input.limit(), exported.limit());
    assertEquals(input.capacity(), exported.capacity());
    assertEquals(input.isDirect(), exported.isDirect());
    assertTrue(exported.isReadOnly());
    assertEquals(input instanceof ByteBuffer ? ByteOrder.BIG_ENDIAN : order(input), order(exported));
    for (int i = input.position(); i < input.limit(); i++) {
      assertEquals(readAt(input, i), readAt(exported, i));
    }
    Buffer inputMark = input.duplicate();
    Buffer exportedMark = exported.duplicate();
    inputMark.reset(); exportedMark.reset();
    assertEquals(inputMark.position(), exportedMark.position());
    if (exported.hasRemaining()) {
      assertThrows(ReadOnlyBufferException.class,
          () -> writeAt(exported, exported.position(), 0));
    }
  }

  private static void consume(Buffer buffer) {
    while (buffer.hasRemaining()) {
      if (buffer instanceof ByteBuffer b) b.get();
      else if (buffer instanceof IntBuffer b) b.get();
      else if (buffer instanceof LongBuffer b) b.get();
      else if (buffer instanceof ShortBuffer b) b.get();
      else throw new AssertionError(buffer.getClass());
    }
  }

  private static void disturb(Buffer buffer) {
    buffer.clear();
    buffer.position(buffer.capacity());
    buffer.mark();
    if (buffer instanceof ByteBuffer bytes) {
      bytes.order(bytes.order() == ByteOrder.BIG_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
    }
  }

  private static long readAt(Buffer buffer, int index) {
    if (buffer instanceof ByteBuffer b) return b.get(index);
    if (buffer instanceof IntBuffer b) return b.get(index);
    if (buffer instanceof LongBuffer b) return b.get(index);
    if (buffer instanceof ShortBuffer b) return b.get(index);
    throw new AssertionError(buffer.getClass());
  }

  private static void writeAt(Buffer buffer, int index, long value) {
    if (buffer instanceof ByteBuffer b) b.put(index, (byte) value);
    else if (buffer instanceof IntBuffer b) b.put(index, (int) value);
    else if (buffer instanceof LongBuffer b) b.put(index, value);
    else if (buffer instanceof ShortBuffer b) b.put(index, (short) value);
    else throw new AssertionError(buffer.getClass());
  }

  private static ByteOrder order(Buffer buffer) {
    if (buffer instanceof ByteBuffer b) return b.order();
    if (buffer instanceof IntBuffer b) return b.order();
    if (buffer instanceof LongBuffer b) return b.order();
    if (buffer instanceof ShortBuffer b) return b.order();
    throw new AssertionError(buffer.getClass());
  }

  private record Shape(boolean direct, ByteOrder order, boolean readOnly, int padding) {}
  private record Fixture(Object owner, Buffer[] inputs, Runnable oracle) {}
  private static Shape plain() { return new Shape(false, ByteOrder.BIG_ENDIAN, false, 0); }

  private static List<Shape> shapes() {
    List<Shape> result = new ArrayList<>();
    for (boolean direct : new boolean[]{false, true})
      for (ByteOrder order : new ByteOrder[]{ByteOrder.BIG_ENDIAN, ByteOrder.LITTLE_ENDIAN})
        for (boolean readOnly : new boolean[]{false, true})
          for (int padding : new int[]{0, 1}) result.add(new Shape(direct, order, readOnly, padding));
    return result;
  }

  private static List<Fixture> fixtures(Shape shape) {
    return List.of(segments(shape), machine(shape), compact(shape), grep(shape));
  }

  private static Fixture segments(Shape shape) {
    long[] presence = new long[256]; presence['a'] = 1; presence['b'] = 2;
    Buffer[] inputs = {bytes(shape, 'a', 'b'), ints(shape, 0, 1), ints(shape, 1, 1),
        longs(shape, presence), longs(shape, 0), longs(shape, 0), ints(shape, 2, 1, 1, 1, 1)};
    var owner = new M3FsPrecomputeBundle.PreparedByteSegments((ByteBuffer) inputs[0],
        (IntBuffer) inputs[1], (IntBuffer) inputs[2], (LongBuffer) inputs[3],
        (LongBuffer) inputs[4], (LongBuffer) inputs[5], (IntBuffer) inputs[6]);
    return new Fixture(owner, inputs, () -> assertSegments(owner));
  }

  private static void assertSegments(M3FsPrecomputeBundle.PreparedByteSegments owner) {
    assertEquals(2, owner.size()); assertEquals(2, owner.packedBytes());
    assertEquals(1, owner.bitmapWords()); assertEquals(1, owner.maxSegmentLength());
    assertEquals(1, owner.pairNgramBuckets()); assertEquals(1, owner.trigramNgramBuckets());
    assertEquals(0, owner.segmentStart(0)); assertEquals(1, owner.segmentStart(1));
    assertEquals(1, owner.segmentLength(0)); assertEquals(1, owner.segmentLength(1));
    assertEquals('a', owner.byteAt(0)); assertEquals('b', owner.byteAt(1));
    assertEquals(1L, owner.bytePresenceWord('a', 0));
    assertEquals(2L, owner.bytePresenceWord('b', 0));
    assertEquals(0L, owner.pairNgramPresenceWord(0, 0));
    assertEquals(0L, owner.trigramNgramPresenceWord(0, 0));
  }

  private static Fixture machine(Shape shape) {
    int[] transitions = new int[3 * 256];
    for (int state = 0; state < 3; state++) transitions[state * 256 + 'a'] = 1;
    transitions[256 + 'b'] = 2;
    Buffer[] inputs = {bytes(shape, 'a', 'b'), ints(shape, 0, 2), ints(shape, transitions),
        longs(shape, 0, 0, 1), ints(shape, 1, 3, 256, 0)};
    var owner = new M3FsPrecomputeBundle.PreparedBytePatternMachine((ByteBuffer) inputs[0],
        (IntBuffer) inputs[1], (IntBuffer) inputs[2], (LongBuffer) inputs[3], (IntBuffer) inputs[4]);
    return new Fixture(owner, inputs, () -> assertMachine(owner));
  }

  private static void assertMachine(M3FsPrecomputeBundle.PreparedBytePatternMachine owner) {
    assertEquals(1, owner.patternCount()); assertEquals(3, owner.stateCount());
    assertEquals(0, owner.startState()); assertEquals(2, owner.totalPatternBytes());
    assertEquals(2, owner.patternLength(0)); assertEquals('a', owner.patternByteAt(0, 0));
    assertEquals('b', owner.patternByteAt(0, 1)); assertEquals(3 * 256, owner.transitionCount());
    int state = owner.startState(); long mask = 0;
    for (byte value : new byte[]{'a', 'a', 'b'}) {
      state = owner.nextState(state, value); mask |= owner.outputMask(state);
    }
    assertEquals(2, state); assertEquals(1L, mask);
  }

  private static Fixture compact(Shape shape) {
    int[] classes = new int[128];
    for (int i = 64; i < 128; i++) classes[i] = 1;
    Buffer[] inputs = {bytes(shape, classes), shorts(shape, 0, 1, 1, -1),
        bytes(shape, 0, 1), ints(shape, 0, 2, 2)};
    var owner = new M3FsPrecomputeBundle.CompactAsciiDfa((ByteBuffer) inputs[0],
        (ShortBuffer) inputs[1], (ByteBuffer) inputs[2], (IntBuffer) inputs[3]);
    return new Fixture(owner, inputs, () -> assertCompact(owner));
  }

  private static void assertCompact(M3FsPrecomputeBundle.CompactAsciiDfa owner) {
    assertEquals(0, owner.startState()); assertEquals(2, owner.classCount());
    assertEquals(2, owner.stateCount()); assertEquals(1, owner.nextState(0, 'A'));
    assertEquals(-1, owner.nextState(1, 'a'));
  }

  private static Fixture grep(Shape shape) {
    Buffer[] inputs = {bytes(shape, 'a', 'b'), longs(shape, 0, 1, 2), ints(shape, 1, 1)};
    var owner = new M3FsPrecomputeBundle.GpuGrepSegments((ByteBuffer) inputs[0],
        (LongBuffer) inputs[1], (IntBuffer) inputs[2]);
    return new Fixture(owner, inputs, () -> assertGrep(owner));
  }

  private static void assertGrep(M3FsPrecomputeBundle.GpuGrepSegments owner) {
    assertEquals(2, owner.segmentCount());
    ByteBuffer bytes = owner.bytes(); LongBuffer offsets = owner.offsets(); IntBuffer lengths = owner.lengths();
    assertEquals('a', bytes.get(bytes.position())); assertEquals('b', bytes.get(bytes.position() + 1));
    assertEquals(0L, offsets.get(offsets.position())); assertEquals(2L, offsets.get(offsets.position() + 2));
    assertEquals(1, lengths.get(lengths.position())); assertEquals(1, lengths.get(lengths.position() + 1));
  }

  private static M3FsPrecomputeBundle.PreparedByteSegments segmentGeometry(
      byte[] bytes, int[] starts, int[] lengths, int[] metadata) {
    return new M3FsPrecomputeBundle.PreparedByteSegments(ByteBuffer.wrap(bytes),
        IntBuffer.wrap(starts), IntBuffer.wrap(lengths), LongBuffer.wrap(new long[256]),
        LongBuffer.wrap(new long[1]), LongBuffer.wrap(new long[1]), IntBuffer.wrap(metadata));
  }

  private static ByteBuffer storage(Shape shape, int count, int width) {
    int bytes = (count + 2 * shape.padding()) * width;
    return (shape.direct() ? ByteBuffer.allocateDirect(bytes) : ByteBuffer.allocate(bytes)).order(shape.order());
  }

  private static void window(Buffer buffer, Shape shape, int length) {
    buffer.position(Math.max(0, shape.padding() - 1)); buffer.mark();
    buffer.position(shape.padding()); buffer.limit(shape.padding() + length);
  }

  private static ByteBuffer bytes(Shape shape, int... values) {
    ByteBuffer result = storage(shape, values.length, 1);
    for (int i = 0; i < values.length; i++) result.put(shape.padding() + i, (byte) values[i]);
    window(result, shape, values.length);
    return shape.readOnly() ? result.asReadOnlyBuffer().order(shape.order()) : result;
  }

  private static IntBuffer ints(Shape shape, int... values) {
    IntBuffer result = storage(shape, values.length, Integer.BYTES).asIntBuffer();
    for (int i = 0; i < values.length; i++) result.put(shape.padding() + i, values[i]);
    window(result, shape, values.length);
    return shape.readOnly() ? result.asReadOnlyBuffer() : result;
  }

  private static LongBuffer longs(Shape shape, long... values) {
    LongBuffer result = storage(shape, values.length, Long.BYTES).asLongBuffer();
    for (int i = 0; i < values.length; i++) result.put(shape.padding() + i, values[i]);
    window(result, shape, values.length);
    return shape.readOnly() ? result.asReadOnlyBuffer() : result;
  }

  private static ShortBuffer shorts(Shape shape, int... values) {
    ShortBuffer result = storage(shape, values.length, Short.BYTES).asShortBuffer();
    for (int i = 0; i < values.length; i++) result.put(shape.padding() + i, (short) values[i]);
    window(result, shape, values.length);
    return shape.readOnly() ? result.asReadOnlyBuffer() : result;
  }

  private static byte[] byteArray(ByteBuffer values) {
    byte[] result = new byte[values.remaining()]; values.get(result); return result;
  }

  private static short[] shortArray(ShortBuffer values) {
    short[] result = new short[values.remaining()]; values.get(result); return result;
  }
}
