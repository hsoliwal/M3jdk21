// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.synexia.mindex.compiler.ast;

import static com.synexia.mindex.compiler.ast.M3FrontendTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import com.synexia.mindex.ast.MIndexASTLimits;
import com.synexia.mindex.ast.MIndexASTPool;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class M3ASTCodecTest {
    @TempDir Path temporary;

    @Test void roundTripPreservesExactSourceStructuralIdentityAndEveryOccurrence() throws Exception {
        String source = "// exact CRLF, surrogate \ud800, and supplementary \ud83d\ude80\r\n"
                + "class CodecRound { String value = \"a\\n\"; int n = 2 + 3; }\r\n";
        M3AST original = capture(temporary, input("CodecRound", source));
        M3ASTCodec codec = new M3ASTCodec(LIMITS);
        byte[] image = codec.encode(original);
        M3AST recovered = codec.decode(image);
        assertEquals(source, recovered.source());
        assertEquals(original.sourceName(), recovered.sourceName());
        assertEquals(rawUtf16Sha256(source), recovered.sourceSha256());
        assertEquals(original.fingerprint(), recovered.fingerprint());
        assertEquals(original.occurrenceCount(), recovered.occurrenceCount());
        assertNotSame(original.asMIndex().root(), recovered.asMIndex().root());
        for (int i = 0; i < original.occurrenceCount(); i++) {
            assertEquals(original.kind(i), recovered.kind(i));
            assertEquals(original.start(i), recovered.start(i));
            assertEquals(original.end(i), recovered.end(i));
            var old = original.asMIndex().occurrences().get(i);
            var restored = recovered.asMIndex().occurrences().get(i);
            assertEquals(old.atom().fingerprint(), restored.atom().fingerprint());
            if (old.hasSourceSpan()) assertEquals(original.asMIndex().text(old), recovered.asMIndex().text(restored));
        }
        assertArrayEquals(image, codec.encode(recovered));
        image[0] ^= 1;
        assertEquals(source, recovered.source());
        assertEquals(original.fingerprint(), recovered.fingerprint());
        assertTrue(codec.weight(recovered) > 0);
    }

    @Test void staleSourceDigestAndForeignAtomWithCoincidentOrdinalAreRefused() throws Exception {
        M3AST original = capture(temporary, input("Identity", "class Identity { int n = 1; }\n"));
        MIndexASTDocument document = original.asMIndex();
        M3ASTCodec codec = new M3ASTCodec(LIMITS);
        M3AST stale = new M3AST(new MIndexASTDocument(document.sourceName(), document.source(),
                "f".repeat(64), document.root(), document.occurrences()));
        assertNotEquals(document.sourceUtf16Sha256(), stale.sourceSha256());
        assertThrows(IOException.class, () -> codec.encode(stale));

        MIndexASTOccurrence local = document.occurrences().stream()
                .filter(occurrence -> occurrence.atom().contentIndex() == 0).findFirst().orElseThrow();
        var foreignPool = new MIndexASTPool(LIMITS);
        var foreignAtom = foreignPool.leaf("FOREIGN", "different owner at the same numeric index");
        assertEquals(local.atom().contentIndex(), foreignAtom.contentIndex());
        assertNotEquals(local.atom(), foreignAtom);
        var foreign = new MIndexASTOccurrence(foreignAtom, local.start(), local.end());
        M3AST wrongOwner = new M3AST(new MIndexASTDocument(document.sourceName(), document.source(),
                document.sourceUtf16Sha256(), document.root(), List.of(foreign)));
        assertThrows(IOException.class, () -> codec.encode(wrongOwner));
        assertEquals(original.fingerprint(), codec.decode(codec.encode(original)).fingerprint());
    }

    @Test void malformedChecksummedFramesRefuseVersionCountsSpansAndTrailingData() throws Exception {
        M3AST value = capture(temporary, input("Frames", "class Frames { int n = 7; }\n"));
        M3ASTCodec codec = new M3ASTCodec(LIMITS);
        byte[] valid = codec.encode(value);
        int nameLength = ByteBuffer.wrap(valid).getInt(8);
        int sourceLengthOffset = 12 + 2 * nameLength;
        int sourceLength = ByteBuffer.wrap(valid).getInt(sourceLengthOffset);
        int imageLengthOffset = sourceLengthOffset + 4 + 2 * sourceLength;
        int imageLength = ByteBuffer.wrap(valid).getInt(imageLengthOffset);
        int occurrenceCountOffset = imageLengthOffset + 4 + imageLength;
        int occurrenceOffset = occurrenceCountOffset + 4;
        List<byte[]> corruptions = new ArrayList<>();
        corruptions.add(new byte[0]);
        corruptions.add(Arrays.copyOf(valid, 51));
        corruptions.add(Arrays.copyOf(valid, valid.length - 1));
        byte[] badChecksum = valid.clone();
        badChecksum[badChecksum.length - 1] ^= 1;
        corruptions.add(badChecksum);
        byte[] badMagic = valid.clone();
        badMagic[0] ^= 1;
        corruptions.add(reseal(badMagic));
        for (int offset : List.of(8, sourceLengthOffset, imageLengthOffset, occurrenceCountOffset, occurrenceOffset)) {
            byte[] bad = valid.clone();
            ByteBuffer.wrap(bad).putInt(offset, Integer.MAX_VALUE);
            corruptions.add(reseal(bad));
        }
        for (int[] invalid : List.of(new int[] {occurrenceCountOffset, -1},
                new int[] {occurrenceOffset + 4, -2},
                new int[] {occurrenceOffset + 8, sourceLength + 1})) {
            byte[] bad = valid.clone();
            ByteBuffer.wrap(bad).putInt(invalid[0], invalid[1]);
            corruptions.add(reseal(bad));
        }
        byte[] trailing = new byte[valid.length + 1];
        System.arraycopy(valid, 0, trailing, 0, valid.length - 32);
        trailing[valid.length - 32] = 42;
        corruptions.add(reseal(trailing));
        for (int i = 0; i < corruptions.size(); i++) {
            byte[] malformed = corruptions.get(i);
            assertThrows(IOException.class, () -> codec.decode(malformed), "corruption case " + i);
        }
        assertEquals(value.fingerprint(), codec.decode(valid).fingerprint());
    }

    @Test void independentSourceNodeImageAndOuterRecordBudgetsRefuseOversizedValues() throws Exception {
        M3AST value = capture(temporary, input("Budgets", "class Budgets { int first = 1; int second = 2; }\n"));
        byte[] valid = new M3ASTCodec(LIMITS).encode(value);
        List<MIndexASTLimits> envelopes = List.of(
                new MIndexASTLimits(100, 500, 500, 500, 500, 10_000, 100, 10_000, 1),
                new MIndexASTLimits(1, 500, 500, 500, 500, 10_000, 100, 10_000, 10_000),
                new MIndexASTLimits(100, 500, 500, 500, 500, 10_000, 100, 32, 10_000));
        for (MIndexASTLimits limits : envelopes) {
            M3ASTCodec bounded = new M3ASTCodec(limits);
            assertThrows(IOException.class, () -> bounded.encode(value));
            assertThrows(IOException.class, () -> bounded.decode(valid));
        }
        M3ASTCodec tiny = new M3ASTCodec(new MIndexASTLimits(1, 0, 0, 1, 1, 1, 1, 32, 1));
        assertThrows(IOException.class, () -> tiny.decode(new byte[tiny.maxBytes() + 1]));
    }

    private static byte[] reseal(byte[] frame) throws Exception {
        int length = frame.length - 32;
        byte[] checksum = MessageDigest.getInstance("SHA-256").digest(Arrays.copyOf(frame, length));
        System.arraycopy(checksum, 0, frame, length, checksum.length);
        return frame;
    }
}
