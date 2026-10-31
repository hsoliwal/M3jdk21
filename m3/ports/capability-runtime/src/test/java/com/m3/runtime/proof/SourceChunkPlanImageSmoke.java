// SPDX-License-Identifier: Apache-2.0
package com.m3.runtime.proof;

import com.m3.runtime.capability.SourceArtifact;
import com.m3.runtime.capability.SourceChunkPlan;

/** Compile with the jlinked image's own javac; run with its own java. */
public final class SourceChunkPlanImageSmoke {
    private SourceChunkPlanImageSmoke() { }
    public static void main(String[] unused) {
        byte[] original = new byte[]{65, 13, 10, 66, 13, 10, 67, 68, 69, 70, 71};
        var source = SourceArtifact.copyOf("developer-image-source", original);
        var plan = SourceChunkPlan.javaPlan(source.whole(), 2, 4);
        if (plan.chunkCount() != 4 || plan.endOffset(0) != 3
                || plan.endOffset(1) != 6 || plan.endOffset(2) != 10
                || plan.endOffset(3) != 11
                || plan.chunkAtOffset(3) != 1
                || plan.chunkAtOffset(11) != 4
                || !plan.rangeRoot().equals(source.whole().sha256())) {
            throw new AssertionError("source-byte developer-image chunk contract");
        }
        System.out.println("M3_SOURCE_CHUNK_PLAN_IMAGE_PASS");
    }
}
