// SPDX-License-Identifier: Apache-2.0
// Copyright © 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.llm.model;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Target tests exercise the ordinary String boundary, including all UTF-16 code units. */
class M3ModelInputTest {
  private static final String A = "a".repeat(64);
  private static final String B = "b".repeat(64);
  private static final M3ModelInput.Namespace NS = new M3ModelInput.Namespace(A, A, A, A);

  @Test
  void preservesTheActualStringAndUsesNoSecondTextStore() {
    String text = new String(new char[] {'x', '\ud800', 'y', '\udfff', '\u0000'});
    M3ModelInput input = M3ModelInput.of(text, NS);
    assertSame(text, input.text());
    assertSame(NS, input.namespace());
    assertEquals(0L, input.retainedBytes());
    M3ModelInput copy = M3ModelInput.of(new String(text.toCharArray()), NS);
    assertTrue(input.matches(copy));
    assertEquals(input, copy);
    assertEquals(input.hashCode(), copy.hashCode());
    assertFalse(input.matches(null));
    assertFalse(input.sameText(null));
    assertNotEquals(input, text);
  }

  @Test
  void exactUtf16HashMatchesIndependentDataOutputOracle() throws Exception {
    for (String text : List.of("", "plain", "\u0000", "हिन्दी", "\ud83d\ude00", "\ud800",
        "\udfff", "?", "a\ud800b\udfff")) {
      M3ModelInput input = M3ModelInput.of(text, NS);
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      try (DataOutputStream out = new DataOutputStream(bytes)) {
        framed(out, "SYNEXIA_MINDEX_MODEL_TEXT_UTF16_V1");
        framed(out, text);
      }
      assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
          .digest(bytes.toByteArray())), input.textRoot());
    }
    assertNotEquals(M3ModelInput.of("\ud800", NS).textRoot(), M3ModelInput.of("?", NS).textRoot());
    assertNotEquals(M3ModelInput.of("\ud800", NS).textRoot(), M3ModelInput.of("\udfff", NS).textRoot());
  }

  @Test
  void everyDependencyVersionParticipatesInIdentity() {
    M3ModelInput base = M3ModelInput.of("source", NS);
    for (M3ModelInput.Namespace changed : List.of(
        new M3ModelInput.Namespace(B, A, A, A), new M3ModelInput.Namespace(A, B, A, A),
        new M3ModelInput.Namespace(A, A, B, A), new M3ModelInput.Namespace(A, A, A, B))) {
      M3ModelInput next = M3ModelInput.of("source", changed);
      assertTrue(base.sameText(next));
      assertFalse(base.matches(next));
      assertNotEquals(base.rootHash(), next.rootHash());
    }
    assertFalse(base.matches(M3ModelInput.of("source!", NS)));
    assertFalse(base.sameText(M3ModelInput.of("source!", NS)));
    assertEquals(base, M3ModelInput.of("source", A, A, A, A));
  }

  @Test
  void floatingAliasesAndMalformedVersionRootsAreRefused() {
    for (String invalid : new String[] {null, "", "jev-latest", "a".repeat(63), "a".repeat(65),
        "A".repeat(64), "g".repeat(64), "0".repeat(63) + "\n"}) {
      assertThrows(IllegalArgumentException.class, () -> new M3ModelInput.Namespace(invalid, A, A, A));
      assertThrows(IllegalArgumentException.class, () -> new M3ModelInput.Namespace(A, invalid, A, A));
      assertThrows(IllegalArgumentException.class, () -> new M3ModelInput.Namespace(A, A, invalid, A));
      assertThrows(IllegalArgumentException.class, () -> new M3ModelInput.Namespace(A, A, A, invalid));
    }
    assertThrows(NullPointerException.class, () -> M3ModelInput.of(null, NS));
    assertThrows(NullPointerException.class, () -> M3ModelInput.of("text", null));
  }

  private static void framed(DataOutputStream out, String value) throws Exception {
    out.writeInt(value.length());
    for (int index = 0; index < value.length(); index++) out.writeChar(value.charAt(index));
  }
}
