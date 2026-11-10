// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.paradigm.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ControlWordTest {

    @Test
    void packsIdentityRevisionAndFlagsWithoutOverlap() {
        long word = ControlWord.initial(
                ControlWord.MAX_LOCAL_ID,
                0xffL);

        assertEquals(
                ControlWord.MAX_LOCAL_ID,
                ControlWord.localId(word));
        assertEquals(0L, ControlWord.revision(word));
        assertEquals(0xffL, ControlWord.flags(word));

        for (int bit = 0; bit < 8; bit++) {
            long mask = 1L << bit;
            long enabled = ControlWord.withPresence(
                    word, mask, true);
            assertEquals(mask, ControlWord.flags(enabled) & mask);
            assertEquals(
                    ControlWord.MAX_LOCAL_ID,
                    ControlWord.localId(enabled));
        }
    }

    @Test
    void revisionAdvancePreservesIdentityAndFlags() {
        long word = ControlWord.initial(73L, 0x5aL);
        for (int revision = 1; revision <= 10_000; revision++) {
            word = ControlWord.advance(word);
            assertEquals(revision, ControlWord.revision(word));
            assertEquals(73L, ControlWord.localId(word));
            assertEquals(0x5aL, ControlWord.flags(word));
        }

        long changed = ControlWord.advanceWithFlags(
                word, 0xa5L);
        assertEquals(10_001L, ControlWord.revision(changed));
        assertEquals(73L, ControlWord.localId(changed));
        assertEquals(0xa5L, ControlWord.flags(changed));
    }

    @Test
    void revisionAndIdentityBoundsFailClosed() {
        long spill =
                ControlWord.initial(0L, 0x3L);
        assertEquals(0L, ControlWord.localId(spill));
        assertEquals(0x3L, ControlWord.flags(spill));

        assertThrows(
                IllegalArgumentException.class,
                () -> ControlWord.initial(
                        ControlWord.MAX_LOCAL_ID + 1L, 0L));

        long exhausted =
                (1L << ControlWord.LOCAL_ID_SHIFT)
                | (ControlWord.MAX_REVISION
                        << ControlWord.REVISION_SHIFT);
        assertThrows(
                ArithmeticException.class,
                () -> ControlWord.advance(exhausted));
    }

    @Test
    void compatibilityProjectionDoesNotExposeCompactIdentityBits() {
        long word = ControlWord.initial(17L, 3L);
        word = ControlWord.advance(word);
        long compatibility =
                ControlWord.compatibilityWord(word);

        assertEquals(
                0L,
                compatibility >>> ControlWord.LOCAL_ID_SHIFT);
        assertEquals(1L, compatibility >>> ControlWord.REVISION_SHIFT);
        assertEquals(3L, compatibility & ControlWord.FLAGS_MASK);
    }
}
