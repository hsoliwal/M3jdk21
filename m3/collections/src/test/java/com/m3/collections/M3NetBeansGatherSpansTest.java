/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Observable Java/JNI parity over the original address owner, including partial failure. */
final class M3NetBeansGatherSpansTest {
    @Test void oneTwoAndManySpansPreserveOrderAndOffsets() {
        for (var backend : backends()) {
            for (int length : new int[] {0, 1, 2, 127, 128, 255, 256, 257, 511, 512, 513, 4097}) {
                for (int shape = 0; shape < 5; shape++) {
                    var fixture = new Fixture(length, shape);
                    long[] before = fixture.payload.clone();
                    int[] oldIds = fixture.ids.clone();
                    M3LongAddressKernel.gatherInto(fixture.payload, fixture.ids, 3, length,
                            fixture.output, 5, backend);
                    assertArrayEquals(fixture.expected(length), fixture.output);
                    assertArrayEquals(before, fixture.payload);
                    assertArrayEquals(oldIds, fixture.ids);
                    assertArrayEquals(Arrays.copyOfRange(fixture.output, 5, 5 + length),
                            M3LongAddressKernel.gather(fixture.payload, fixture.ids, 3, length, backend));
                }
            }
        }
    }

    @Test void invalidAddressesPublishOnlyTheValidPrefix() {
        for (var backend : backends()) {
            for (int length : new int[] {1, 128, 255, 256, 257, 511, 512, 513}) {
                for (int at : new int[] {0, 1, 126, 127, 128, 254, 255, 256, 257, length - 1}) {
                    if (at >= length) continue;
                    for (int shape = 0; shape < 5; shape++) {
                        for (int bad : new int[] {-1, 8192, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
                            var fixture = new Fixture(length, shape);
                            long[] expected = fixture.expected(at);
                            fixture.ids[3 + at] = bad;
                            Class<? extends IndexOutOfBoundsException> type =
                                    backend == M3IndexMaskCompactor.Backend.JAVA
                                            ? ArrayIndexOutOfBoundsException.class : IndexOutOfBoundsException.class;
                            assertThrowsExactly(type, () -> M3LongAddressKernel.gatherInto(
                                    fixture.payload, fixture.ids, 3, length, fixture.output, 5, backend));
                            assertArrayEquals(expected, fixture.output);
                        }
                    }
                }
            }
        }
    }

    private static M3IndexMaskCompactor.Backend[] backends() {
        if (Boolean.getBoolean("m3.collections.all.native.required")) {
            assertTrue(M3LongAddressKernel.nativeAvailable(), "required existing address JNI library");
        }
        return M3LongAddressKernel.nativeAvailable() ? M3IndexMaskCompactor.Backend.values()
                : new M3IndexMaskCompactor.Backend[] {M3IndexMaskCompactor.Backend.JAVA};
    }

    private static final class Fixture {
        final long[] payload = new long[8192];
        final int[] ids;
        final long[] output;

        Fixture(int length, int shape) {
            ids = new int[length + 6];
            output = new long[length + 10];
            Arrays.fill(ids, -1);
            Arrays.fill(output, 71L);
            for (int i = 0; i < payload.length; i++) payload[i] = Long.MIN_VALUE + i * 123457L;
            for (int i = 0; i < length; i++) {
                ids[3 + i] = switch (shape) {
                    case 0 -> i;
                    case 1 -> i % 256 < 128 ? i : 4096 + i % 256;
                    case 2 -> i % 256 < 128 ? 4096 + i % 256 : i;
                    case 3 -> i % 128; // Repeated physical IDs keep their encounter occurrences.
                    default -> (i * 31) % payload.length;
                };
            }
        }

        long[] expected(int prefix) {
            long[] expected = new long[output.length];
            Arrays.fill(expected, 71L);
            for (int i = 0; i < prefix; i++) expected[5 + i] = payload[ids[3 + i]];
            return expected;
        }
    }
}
