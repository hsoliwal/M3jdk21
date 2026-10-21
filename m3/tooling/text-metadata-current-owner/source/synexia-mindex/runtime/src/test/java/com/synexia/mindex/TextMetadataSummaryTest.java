// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/** Runtime/Jupiter adapter; the standalone proof separately runs in fresh JVMs. */
class TextMetadataSummaryTest {
    @Test void everyRangeAndJoinRetainsExactUtf16Semantics() {
        String text = "A\ud83d\ude00\0e\u0301\ud83d";
        for (int a = 0; a <= text.length(); a++) {
            for (int b = a; b <= text.length(); b++) {
                var left = TextMetadata.summarize(text, 0, a);
                var middle = TextMetadata.summarize(text, a, b);
                var right = TextMetadata.summarize(text, b, text.length());
                var actual = TextMetadata.concatAll(left, TextMetadata.EMPTY, middle, right);
                assertEquals(text.hashCode(), actual.javaHash());
                assertEquals(text.codePointCount(0, text.length()), actual.codePointLength());
                assertEquals(TextMetadata.concat(TextMetadata.concat(left, middle), right),
                        TextMetadata.concat(left, TextMetadata.concat(middle, right)));
                String range = text.substring(a, b);
                assertEquals(range.hashCode(), middle.javaHash());
                assertEquals(range.codePointCount(0, range.length()), middle.codePointLength());
            }
        }
    }

    @Test void metadataNeverGrantsEqualityAuthority() {
        assertNotEquals("xAaY", "xBBY");
        assertEquals(TextMetadata.summarize("xAaY", 0, 4),
                TextMetadata.summarize("xBBY", 0, 4));
    }

    @Test void checkedLengthOverflowAndBadRangeRemainVisible() {
        var huge = new TextMetadata.Summary(Integer.MAX_VALUE, 0, Integer.MAX_VALUE, 'a', 'a');
        assertThrows(ArithmeticException.class, () -> TextMetadata.concat(huge,
                TextMetadata.summarize("a", 0, 1)));
        assertThrows(IndexOutOfBoundsException.class, () -> TextMetadata.summarize("a", 1, 0));
        assertThrows(NullPointerException.class, () -> TextMetadata.concatAll(TextMetadata.EMPTY, null));
    }
}
