// SPDX-License-Identifier: Apache-2.0
package com.m3.runtime.proof;

import com.m3.runtime.capability.LfCount;
import com.m3.runtime.capability.SourceArtifact;
import com.m3.runtime.capability.WorkingSet;
import java.nio.ByteBuffer;

/** Executed using a freshly jlinked JDK21 developer image's own javac/java. */
public final class CapabilityImageSmoke {
    private CapabilityImageSmoke() { }
    public static void main(String[] args) {
        SourceArtifact source = SourceArtifact.copyOf("jlinked:source", new byte[]{10, 0, 10, 42});
        WorkingSet working = new WorkingSet(4);
        try (WorkingSet.Lease lease = working.acquire(source.whole())) {
            if (lease.countLf() != 2 || lease.length() != 4) {
                throw new AssertionError("actual linked image working-set contract");
            }
        }
        if (working.reservedBytes() != 0
                || LfCount.javaCount(ByteBuffer.wrap(new byte[]{0, 10, 10}), 0, 3) != 2) {
            throw new AssertionError("actual linked image source-bytes contract");
        }
        System.out.println("M3_CAPABILITY_DEVELOPER_IMAGE_PASS");
    }
}
