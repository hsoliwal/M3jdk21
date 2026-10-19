// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/** Integration tests against the real canonical MIndex runtime, never a stand-in pool. */
class MIndexCompositeStructuralOpsTest {
    @Test void codePointCountDoesNotAdmitWholeSpelling() throws ReflectiveOperationException {
        var text = MIndexCompositeString.of(MIndexString.literal("left\ud83d"),
                MIndexString.literal(""), MIndexString.literal("\ude00right"));
        var field = MIndexCompositeString.class.getDeclaredField("canonical");
        field.setAccessible(true);
        assertNull(field.get(text));
        String expected = "left\ud83d\ude00right";
        assertEquals(expected.codePointCount(0, expected.length()), text.codePointCount());
        assertNull(field.get(text));
    }

    @Test void appendConcatAndSegmentSlicePreserveParentRangesIncludingSurrogateHalves() {
        var parent = MIndexString.literal("x\ud83d\ude00y");
        var first = MIndexCompositeString.builder()
                .append(SubMIndexString.ofUtf16(parent, 1, 2)).build();
        var second = MIndexCompositeString.builder()
                .append(SubMIndexString.ofUtf16(parent, 2, 3)).build();
        var joined = first.concat(second);
        checkSegment(joined, 0, parent, 1, 2);
        checkSegment(joined, 1, parent, 2, 3);
        assertEquals(1, joined.codePointCount());
        assertEquals("\ud83d\ude00".hashCode(), joined.hashCode());
        var copied = MIndexCompositeString.builder().append(joined).build();
        checkSegment(copied, 0, parent, 1, 2);
        checkSegment(copied, 1, parent, 2, 3);
        var slice = copied.sliceSegments(1, 2);
        checkSegment(slice, 0, parent, 2, 3);
        assertEquals(1, slice.codePointCount());
        assertEquals('\ude00', slice.charAt(0));
    }

    @Test void primitiveSideMetadataAndEmptyOccurrencesAreRetained() {
        var atom = MIndexString.literal("ab");
        var metadata = new MIndexStringMetadata(3, 5L, 7L, 9L, 11, 13);
        var original = MIndexCompositeString.builder().add(atom, metadata)
                .add(MIndexString.literal(""), metadata).build();
        var copied = MIndexCompositeString.builder().append(original).build();
        assertEquals(2, copied.segmentCount());
        for (int i = 0; i < 2; i++) {
            assertEquals(original.flags(i), copied.flags(i));
            assertEquals(original.lexicalBits(i), copied.lexicalBits(i));
            assertEquals(original.semanticBits(i), copied.semanticBits(i));
            assertEquals(original.relationBits(i), copied.relationBits(i));
            assertEquals(original.sourceStart(i), copied.sourceStart(i));
            assertEquals(original.sourceEnd(i), copied.sourceEnd(i));
        }
    }

    private static void checkSegment(MIndexCompositeString actual, int segment,
            MIndexString parent, int start, int end) {
        assertEquals(parent.contentIndex(), actual.parentSegment(segment).contentIndex());
        assertEquals(start, actual.segmentStart(segment));
        assertEquals(end, actual.segmentEnd(segment));
    }
}
