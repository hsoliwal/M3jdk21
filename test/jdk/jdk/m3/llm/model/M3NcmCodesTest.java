// SPDX-License-Identifier: Apache-2.0
// Copyright © 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.llm.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class M3NcmCodesTest {
  private static final String A = "a".repeat(64);
  private static final String B = "b".repeat(64);
  private static final M3ModelInput INPUT = M3ModelInput.of("exact\ud800source", A, A, A, A);

  @Test
  void primitiveLanesAreDetachedAndByteBudgetIsExact() {
    int[] offsets = {0, 1, 3};
    int[] sizes = {4, 4};
    int[] values = {1, 2, 3};
    M3NcmCodes codes = M3NcmCodes.of(INPUT, A, offsets, sizes, values, 32);
    assertEquals(32, codes.retainedBytes());
    assertEquals(2, codes.levelCount());
    assertEquals(3, codes.codeCount());
    assertEquals(1, codes.levelLength(0));
    assertEquals(2, codes.levelLength(1));
    assertEquals(4, codes.codebookSize(0));
    offsets[1] = 2;
    sizes[0] = 1;
    values[0] = 0;
    codes.copyCodes()[0] = 0;
    codes.copyLevelOffsets()[1] = 2;
    codes.copyCodebookSizes()[0] = 1;
    assertEquals(1, codes.codeAt(0, 0));
    assertEquals(2, codes.codeAt(1, 0));
    assertEquals(3, codes.codeAt(1, 1));
    assertThrows(IllegalArgumentException.class,
        () -> M3NcmCodes.of(INPUT, A, new int[] {0, 1, 3}, new int[] {4, 4}, new int[] {1, 2, 3}, 31));
    assertThrows(IllegalArgumentException.class,
        () -> M3NcmCodes.of(INPUT, A, new int[] {0, 1}, new int[] {4}, new int[] {1}, -1));
  }

  @Test
  void textNamespacesAndCodebooksRemainDistinct() {
    M3NcmCodes codes = valid();
    assertSame(INPUT, codes.input());
    assertTrue(codes.matches(M3ModelInput.of("exact\ud800source", A, A, A, A), A));
    assertFalse(codes.matches(M3ModelInput.of("exact?source", A, A, A, A), A));
    assertFalse(codes.matches(M3ModelInput.of(INPUT.text(), A, B, A, A), A));
    assertFalse(codes.matches(INPUT, B));
    assertFalse(codes.matches(null, A));
    assertFalse(codes.matches(INPUT, null));
    assertEquals(codes, valid());
    assertEquals(codes.hashCode(), valid().hashCode());
    assertNotEquals(codes, M3NcmCodes.of(INPUT, B, new int[] {0, 1, 3}, new int[] {4, 4}, new int[] {1, 2, 3}));
    assertNotEquals(codes, M3NcmCodes.of(INPUT, A, new int[] {0, 1, 3}, new int[] {4, 4}, new int[] {2, 2, 3}));
  }

  @Test
  void malformedGeometryAndCodeIndicesAreRefused() {
    for (int[] offsets : List.of(new int[] {1, 2, 3}, new int[] {0, 0, 3},
        new int[] {0, 2, 3}, new int[] {0, 1, 4}, new int[] {0, -1, 3}, new int[] {0, 3})) {
      assertThrows(IllegalArgumentException.class,
          () -> M3NcmCodes.of(INPUT, A, offsets, new int[] {4, 4}, new int[] {1, 2, 3}));
    }
    assertThrows(IllegalArgumentException.class,
        () -> M3NcmCodes.of(INPUT, A, new int[] {0}, new int[0], new int[0]));
    assertThrows(IllegalArgumentException.class,
        () -> M3NcmCodes.of(INPUT, A, new int[] {0, 1}, new int[] {0}, new int[] {0}));
    for (int value : new int[] {-1, 4, Integer.MAX_VALUE}) {
      assertThrows(IllegalArgumentException.class,
          () -> M3NcmCodes.of(INPUT, A, new int[] {0, 1}, new int[] {4}, new int[] {value}));
    }
    assertThrows(IndexOutOfBoundsException.class, () -> valid().codeAt(0, 1));
    assertThrows(IndexOutOfBoundsException.class, () -> valid().codeAt(-1, 0));
    assertThrows(IndexOutOfBoundsException.class, () -> valid().levelLength(2));
  }

  private static M3NcmCodes valid() {
    return M3NcmCodes.of(INPUT, A, new int[] {0, 1, 3}, new int[] {4, 4}, new int[] {1, 2, 3});
  }
}
