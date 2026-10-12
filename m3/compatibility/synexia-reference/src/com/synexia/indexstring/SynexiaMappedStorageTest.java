// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/** Exercises only the pinned heap/direct immutable-text contract; not the addressed-view API. */
public final class SynexiaMappedStorageTest {
  private static int checks;

  private static void check(boolean condition, String message) {
    checks++;
    if (!condition) throw new AssertionError(message);
  }

  public static void main(String[] args) {
    char[] input = {0, 'A', (char) 0xD800, (char) 0xDC00, (char) 0xDFFF, (char) 0xFFFF};
    ByteBuffer writable = ByteBuffer.allocateDirect(input.length * Character.BYTES)
        .order(ByteOrder.BIG_ENDIAN);
    for (char unit : input) writable.putChar(unit);
    writable.flip();

    ByteBuffer published = writable.asReadOnlyBuffer().order(ByteOrder.BIG_ENDIAN);
    int byteHash = FrozenBytes.hash(published);
    FrozenBytes owner = FrozenBytes.mapped(published, published.remaining(), byteHash);
    ByteBuffer hashInput = owner.asReadOnlyBuffer().order(ByteOrder.BIG_ENDIAN);
    FrozenChars value = FrozenChars.mapped(owner, FrozenChars.hashUtf16(hashInput));

    check(owner.isDirect(), "mapped byte owner remains direct");
    check(owner.asReadOnlyBuffer().isReadOnly(), "mapped byte view is read-only");
    check(value.isDirect(), "mapped text remains backed by the byte owner");
    check(value.length() == input.length, "mapped text length is UTF-16 units");
    check(value.hash32() == new String(input).hashCode(), "mapped text hash matches String");
    check(value.toString().equals(new String(input)), "mapped text preserves all UTF-16 units");

    for (int i = 0; i < input.length; i++) {
      check(value.charAt(i) == input[i], "mapped charAt preserves UTF-16 unit " + i);
    }

    for (int from = 0; from <= input.length; from++) {
      for (int to = from; to <= input.length; to++) {
        FrozenChars part = value.subSequence(from, to);
        String expected = new String(input, from, to - from);
        check(part.toString().equals(expected), "mapped subsequence " + from + ".." + to);
        check(part.hash32() == expected.hashCode(), "mapped subsequence hash " + from + ".." + to);
        check(part.isDirect(), "mapped subsequence retains immutable byte owner");
      }
    }

    char[] copied = value.copy();
    check(Arrays.equals(copied, input), "mapped char copy is exact");
    copied[0] = 'X';
    check(value.charAt(0) == 0, "returned char array cannot mutate mapped text");

    char[] target = new char[input.length + 2];
    value.copyTo(1, target, 1, input.length - 2);
    check(Arrays.equals(Arrays.copyOfRange(target, 1, input.length - 1),
        Arrays.copyOfRange(input, 1, input.length - 1)), "mapped copyTo respects source and target offsets");

    ByteBuffer utf16 = value.directUtf16Bytes();
    check(utf16.isDirect() && utf16.isReadOnly(), "UTF-16 JNI view remains direct and read-only");
    check(utf16.remaining() == input.length * Character.BYTES, "UTF-16 JNI view length is byte-counted");
    for (int i = 0; i < input.length; i++) {
      check(utf16.getChar(i * Character.BYTES) == input[i], "UTF-16 JNI view preserves unit " + i);
    }

    byte[] allBytes = owner.copy();
    FrozenBytes slice = owner.slice(Character.BYTES, allBytes.length - Character.BYTES);
    check(Arrays.equals(slice.copy(), Arrays.copyOfRange(allBytes, Character.BYTES,
        allBytes.length - Character.BYTES)), "byte slice preserves exact range");
    check(slice.compact().length() == slice.length(), "compacted byte slice preserves length");
    check(!slice.compact().isDirect(), "explicit compact detaches to heap storage");

    System.out.println("SYNEXIA_MAPPED_STORAGE_PASS checks=" + checks
        + " utf16Units=" + input.length + " byteOrder=BIG_ENDIAN");
  }
}
