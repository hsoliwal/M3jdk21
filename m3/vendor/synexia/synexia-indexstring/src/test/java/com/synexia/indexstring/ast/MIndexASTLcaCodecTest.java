// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.synexia.indexstring.IndexDictionary;
import com.synexia.indexstring.IndexRuntime;
import com.synexia.indexstring.IndexSpace;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class MIndexASTLcaCodecTest {
  @Test
  void authenticatedRoundTripPreservesEveryLcaAndRejectsTampering() throws Exception {
    String source =
        """
        class Sample {
          int f(int x) { return x + 1; }
          int g(int x) { return x * (x - 1); }
        }
        """;
    Fixture fixture = fixture();
    MIndexASTDocument document = fixture.parser.parse("Sample.java", source);
    MIndexASTLcaSparseTable original = MIndexASTLcaSparseTable.build(document);
    MIndexASTPartialCache.Key key =
        new MIndexASTPartialCache.Key(
            document.path().materialize(),
            document.sourceUtf16Sha256Hex(),
            document.root().astSpec().fingerprint());

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    MIndexASTLcaCodec.write(key, 1, original, output);
    byte[] image = output.toByteArray();
    MIndexASTLcaSparseTable restored =
        MIndexASTLcaCodec.read(key, 1, document, new ByteArrayInputStream(image));

    for (int left = 0; left < document.occurrenceCount(); left++) {
      for (int right = 0; right < document.occurrenceCount(); right++) {
        assertEquals(
            original.lowestCommonAncestor(left, right),
            restored.lowestCommonAncestor(left, right),
            "LCA mismatch for " + left + "," + right);
      }
    }

    byte[] tampered = image.clone();
    tampered[tampered.length - 33] ^= 0x01;
    assertThrows(
        IOException.class,
        () -> MIndexASTLcaCodec.read(
            key, 1, document, new ByteArrayInputStream(tampered)));
  }

  private static Fixture fixture() {
    IndexDictionary java =
        IndexDictionary.of(
            1,
            "java",
            "v1",
            List.of("Sample.java", "Sample", "f", "g", "x", "int", "1"));
    IndexRuntime runtime = new IndexRuntime(IndexSpace.builder().add(java).build());
    MIndexASTPool pool = new MIndexASTPool(runtime, 1);
    return new Fixture(pool, new MIndexASTParser(pool));
  }

  private record Fixture(MIndexASTPool pool, MIndexASTParser parser) {}
}
