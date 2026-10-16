// SPDX-License-Identifier: Apache-2.0
package com.m3.runtime.proof;

import com.m3.runtime.capability.ByteLineIndex;
import com.m3.runtime.capability.ByteEvidenceQuery;
import com.m3.runtime.capability.BytePatternSearch;
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
        ByteLineIndex lines = ByteLineIndex.javaIndex(source.whole());
        if (lines.lineCount() != 3 || lines.startOffset(0) != 0
                || lines.startOffset(1) != 1 || lines.startOffset(2) != 3
                || lines.lineAtOffset(2) != 1 || lines.content(0).length() != 0
                || lines.line(1).length() != 2) {
            throw new AssertionError("actual linked image immutable byte-line index contract");
        }
        int[] byteHits = BytePatternSearch.compile(new byte[]{10})
                .findAll(source.whole(), 2);
        if (byteHits.length != 2 || byteHits[0] != 0 || byteHits[1] != 2) {
            throw new AssertionError("actual linked image byte-pattern search contract");
        }
        ByteEvidenceQuery evidenceQuery = ByteEvidenceQuery.compile(source.whole(), new byte[]{10});
        ByteEvidenceQuery.Results receipt = evidenceQuery.execute(2);
        evidenceQuery.verify(receipt);
        if (receipt.count() != 2 || receipt.absoluteStart(1) != 2
                || receipt.firstLine(1) != 1 || receipt.firstColumnByte(1) != 1
                || !receipt.receipt().matches("[0-9a-f]{64}")) {
            throw new AssertionError("source-backed receipt/line semantics in actual linked JMOD");
        }
        System.out.println("M3_CAPABILITY_DEVELOPER_IMAGE_PASS");
    }
}
