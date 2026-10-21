/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

final class M3KeyedParallelismAdmissionTest {
    @Test
    void failedFullLaneStillDeliversItsFailureAndCancelsQueuedWork() throws Exception {
        M3KeyedParallelismAdmissionContract.main(new String[0]);
    }
}
