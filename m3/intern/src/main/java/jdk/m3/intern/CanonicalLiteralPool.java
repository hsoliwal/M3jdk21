// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.intern;

import java.util.Objects;

/**
 * Typed, owner-scoped, bit-exact literal admission into the existing M3 CONSTANT domain.
 *
 * <p>This is a canonical-data candidate, not a replacement for Java constant-pool
 * resolution or for {@code java.lang.String} literal identity. The supplied space
 * owns the backing primitive array/mask store and its lifetime. Source/JVM
 * rewriting must separately prove Java 21 constant-expression, class loader,
 * bootstrap, serialization and JNI compatibility before using these IDs.
 *
 * <p>String payloads retain every UTF-16 code unit, including isolated
 * surrogates; floating-point payloads retain raw IEEE-754 bits, including
 * signed zeros and distinct NaN payloads. No String.intern(), new global
 * object pool, boxed scalar key or retained mutable caller array is used.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/b259ec9f5606b9066231874ef06a3d27b4073e89/synexia-indexstring/src/main/java/com/synexia/indexstring/M3CanonicalLiteralPool.java blob 08cd5e81412f; recipe m3/intern/recipe/canonical-literals-20261106/relocate.py v5
public final class CanonicalLiteralPool {
  /** Version-one literal-kind identifiers, append-only for portable archives. */
  private enum Kind {
    STRING_UTF16(1), BOOLEAN(2), CHARACTER_UTF16(3), BYTE(4), SHORT(5),
    INT(6), LONG(7), FLOAT_RAW_BITS(8), DOUBLE_RAW_BITS(9);

    private final int id;

    Kind(int id) {
      this.id = id;
    }
  }

  private static final byte FALSE_VALUE = 0;
  private static final byte TRUE_VALUE = 1;

  /**
   * M3JDK adapter: tuple kind of the CONSTANT domain in {@code space.shapes()}; members (literal kind, blob
   * handle). The space archive writes it, so the value is a wire-format constant.
   */
  static final int CONSTANT = 0x4d33434c; // M3CL

  private final InternSpace space;

  public CanonicalLiteralPool(InternSpace space) {
    this.space = Objects.requireNonNull(space, "space");
  }

  public int stringLiteral(String value) {
    Objects.requireNonNull(value, "value");
    byte[] bytes = new byte[Math.multiplyExact(value.length(), Character.BYTES)];
    for (int i = 0; i < value.length(); i++) {
      int offset = i * Character.BYTES;
      char codeUnit = value.charAt(i);
      bytes[offset] = (byte) codeUnit;
      bytes[offset + Byte.BYTES] = (byte) (codeUnit >>> Byte.SIZE);
    }
    return constantBytes(Kind.STRING_UTF16.id, bytes);
  }

  public int booleanLiteral(boolean value) {
    return constantBytes(Kind.BOOLEAN.id,
        new byte[] {value ? TRUE_VALUE : FALSE_VALUE});
  }

  public int charLiteral(char value) {
    byte[] bytes = new byte[Character.BYTES];
    bytes[0] = (byte) value;
    bytes[Byte.BYTES] = (byte) (value >>> Byte.SIZE);
    return constantBytes(Kind.CHARACTER_UTF16.id, bytes);
  }

  public int byteLiteral(byte value) {
    return constantBytes(Kind.BYTE.id, new byte[] {value});
  }

  public int shortLiteral(short value) {
    byte[] bytes = new byte[Short.BYTES];
    for (int offset = 0; offset < bytes.length; offset++) {
      bytes[offset] = (byte) (value >>> (offset * Byte.SIZE));
    }
    return constantBytes(Kind.SHORT.id, bytes);
  }

  public int intLiteral(int value) {
    return constantBytes(Kind.INT.id, littleEndianInt(value));
  }

  public int longLiteral(long value) {
    return constantBytes(Kind.LONG.id, littleEndianLong(value));
  }

  public int floatLiteral(float value) {
    return constantBytes(Kind.FLOAT_RAW_BITS.id,
        littleEndianInt(Float.floatToRawIntBits(value)));
  }

  public int doubleLiteral(double value) {
    return constantBytes(Kind.DOUBLE_RAW_BITS.id,
        littleEndianLong(Double.doubleToRawLongBits(value)));
  }

  /**
   * M3JDK adapter onto the owner {@code InternSpace} (the donor admitted into its CONSTANT domain): the payload
   * bytes are interned in {@code space.blobs()}, the literal is the tuple (kind, blob handle) of kind
   * {@code CONSTANT} in {@code space.shapes()}, and that tuple handle is the literal's handle. Equal kind and
   * payload give the same handle; another kind, another payload or another tuple kind never does. Tuple handles
   * are bounded by the store's 2^24 entries, so the int conversion never throws.
   */
  private int constantBytes(int kind, byte[] value) {
    return Math.toIntExact(space.shapes().intern(CONSTANT, kind, space.blobs().intern(value)));
  }

  private static byte[] littleEndianInt(int value) {
    byte[] bytes = new byte[Integer.BYTES];
    for (int offset = 0; offset < bytes.length; offset++) {
      bytes[offset] = (byte) (value >>> (offset * Byte.SIZE));
    }
    return bytes;
  }

  private static byte[] littleEndianLong(long value) {
    byte[] bytes = new byte[Long.BYTES];
    for (int offset = 0; offset < bytes.length; offset++) {
      bytes[offset] = (byte) (value >>> (offset * Byte.SIZE));
    }
    return bytes;
  }
}
