// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.intern;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

/** Each public admission atom has its own JUnit test and shared-domain invariants. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/b259ec9f5606b9066231874ef06a3d27b4073e89/synexia-indexstring/src/test/java/com/synexia/indexstring/M3CanonicalLiteralPoolTest.java blob 094348daebc7; recipe m3/intern/recipe/canonical-literals-20261106/relocate.py v5
final class CanonicalLiteralPoolTest {
  private static final int FLOAT_NAN_A = 0x7fc00001;
  private static final int FLOAT_NAN_B = 0x7fc00002;
  private static final long DOUBLE_NAN_A = 0x7ff8000000000001L;
  private static final long DOUBLE_NAN_B = 0x7ff8000000000002L;

  @Test
  void stringLiteralRetainsExactUtf16IncludingUnpairedSurrogates() {
    InternSpace space = new InternSpace();
    CanonicalLiteralPool pool = new CanonicalLiteralPool(space);
    String text = "\ud800\u0000\udc00";
    int id = pool.stringLiteral(text);
    assertEquals(id, pool.stringLiteral(new String(text.toCharArray())));
    assertNotEquals(id, pool.stringLiteral("\ufffd\u0000\ufffd"));
    assertArrayEquals(new byte[] {0, (byte) 0xd8, 0, 0, 0, (byte) 0xdc},
        payload(space, id));
  }

  @Test
  void booleanLiteralDeduplicatesAndKeepsBothValuesSeparate() {
    InternSpace space = new InternSpace();
    CanonicalLiteralPool pool = new CanonicalLiteralPool(space);
    assertEquals(pool.booleanLiteral(true), pool.booleanLiteral(true));
    assertNotEquals(pool.booleanLiteral(true), pool.booleanLiteral(false));
  }

  @Test
  void charLiteralDoesNotAliasSameUtf16String() {
    InternSpace space = new InternSpace();
    CanonicalLiteralPool pool = new CanonicalLiteralPool(space);
    int character = pool.charLiteral('\ud800');
    assertEquals(character, pool.charLiteral('\ud800'));
    assertNotEquals(character, pool.stringLiteral("\ud800"));
    assertArrayEquals(new byte[] {0, (byte) 0xd8}, payload(space, character));
  }

  @Test
  void byteLiteralRetainsExactSignedByteWithoutTypeAliasing() {
    InternSpace space = new InternSpace();
    CanonicalLiteralPool pool = new CanonicalLiteralPool(space);
    assertEquals(pool.byteLiteral(Byte.MIN_VALUE), pool.byteLiteral(Byte.MIN_VALUE));
    assertNotEquals(pool.byteLiteral((byte) 0), pool.booleanLiteral(false));
  }

  @Test
  void shortLiteralPreservesSignedShortAndKind() {
    InternSpace space = new InternSpace();
    CanonicalLiteralPool pool = new CanonicalLiteralPool(space);
    int id = pool.shortLiteral(Short.MIN_VALUE);
    assertEquals(id, pool.shortLiteral(Short.MIN_VALUE));
    assertNotEquals(id, pool.intLiteral(Short.MIN_VALUE));
    assertArrayEquals(new byte[] {0, (byte) 0x80}, payload(space, id));
  }

  @Test
  void intLiteralPreservesRawSignedValueAndKind() {
    InternSpace space = new InternSpace();
    CanonicalLiteralPool pool = new CanonicalLiteralPool(space);
    int id = pool.intLiteral(Integer.MIN_VALUE);
    assertEquals(id, pool.intLiteral(Integer.MIN_VALUE));
    assertNotEquals(id, pool.longLiteral(Integer.MIN_VALUE));
    assertEquals(Integer.MIN_VALUE, ByteBuffer.wrap(payload(space, id))
        .order(ByteOrder.LITTLE_ENDIAN).getInt());
  }

  @Test
  void longLiteralPreservesRawSignedValueAndKind() {
    InternSpace space = new InternSpace();
    CanonicalLiteralPool pool = new CanonicalLiteralPool(space);
    int id = pool.longLiteral(Long.MIN_VALUE);
    assertEquals(id, pool.longLiteral(Long.MIN_VALUE));
    assertNotEquals(id, pool.intLiteral(Integer.MIN_VALUE));
    assertEquals(Long.MIN_VALUE, ByteBuffer.wrap(payload(space, id))
        .order(ByteOrder.LITTLE_ENDIAN).getLong());
  }

  @Test
  void floatLiteralKeepsSignedZeroDistinctNaNsAndRepeatedRawBits() {
    InternSpace space = new InternSpace();
    CanonicalLiteralPool pool = new CanonicalLiteralPool(space);
    assertNotEquals(pool.floatLiteral(Float.intBitsToFloat(0)),
        pool.floatLiteral(Float.intBitsToFloat(Integer.MIN_VALUE)));
    int nanA = pool.floatLiteral(Float.intBitsToFloat(FLOAT_NAN_A));
    int nanB = pool.floatLiteral(Float.intBitsToFloat(FLOAT_NAN_B));
    assertNotEquals(nanA, nanB);
    assertEquals(nanA, pool.floatLiteral(Float.intBitsToFloat(FLOAT_NAN_A)));
    assertEquals(FLOAT_NAN_A, ByteBuffer.wrap(payload(space, nanA))
        .order(ByteOrder.LITTLE_ENDIAN).getInt());
  }

  @Test
  void doubleLiteralKeepsSignedZeroDistinctNaNsAndRepeatedRawBits() {
    InternSpace space = new InternSpace();
    CanonicalLiteralPool pool = new CanonicalLiteralPool(space);
    assertNotEquals(pool.doubleLiteral(Double.longBitsToDouble(0L)),
        pool.doubleLiteral(Double.longBitsToDouble(Long.MIN_VALUE)));
    int nanA = pool.doubleLiteral(Double.longBitsToDouble(DOUBLE_NAN_A));
    int nanB = pool.doubleLiteral(Double.longBitsToDouble(DOUBLE_NAN_B));
    assertNotEquals(nanA, nanB);
    assertEquals(nanA, pool.doubleLiteral(Double.longBitsToDouble(DOUBLE_NAN_A)));
    assertEquals(DOUBLE_NAN_A, ByteBuffer.wrap(payload(space, nanA))
        .order(ByteOrder.LITTLE_ENDIAN).getLong());
  }

  @Test
  void nullStringIsRefusedAndConstantDomainIsSeparateFromSymbol() {
    InternSpace space = new InternSpace();
    CanonicalLiteralPool pool = new CanonicalLiteralPool(space);
    assertThrows(NullPointerException.class, () -> pool.stringLiteral(null));
    int id = pool.stringLiteral("word");
    assertNotEquals(id, space.internType(1, "word"));
    assertEquals(CanonicalLiteralPool.CONSTANT, domain(space, id));
  }

  /** M3JDK adapter: the payload of a CONSTANT tuple (literal kind, blob handle), read after freezing the space. */
  private static byte[] payload(InternSpace space, int id) {
    space.freeze();
    return space.blobs().copy(space.shapes().members(id)[1]);
  }

  /** M3JDK adapter: the domain of a handle is its tuple kind, read after freezing the space. */
  private static int domain(InternSpace space, int id) {
    space.freeze();
    return space.shapes().kind(id);
  }
}
