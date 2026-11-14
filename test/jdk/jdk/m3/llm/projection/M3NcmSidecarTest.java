// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.projection;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class M3NcmSidecarTest {
    private static M3SemanticIdentity identity(String text) {
        String root="a".repeat(64);
        return M3SemanticIdentity.builder("toy",new M3SemanticIdentity.Model(root,root,root,root),
                "fixture",root,1).segment(text).build();
    }
    private static M3NcmHierarchy hierarchy() {
        return new M3NcmHierarchy("toy-v1",1,new int[]{2,4},
                new float[][]{{0,2},{-.5f,0,.5f}});
    }

    @Test void exactSourceAndHierarchyBindImmutableCodesAndBinaryRoundTrip() {
        var h=hierarchy(); var identity=identity("source");
        int[] input={0,1,1,0,2,1};
        var sidecar=M3NcmSidecar.of(identity,h,input,1);
        input[0]=1;
        assertEquals(0,sidecar.codeAt(0,0,0));
        assertArrayEquals(new float[]{0,0,2,2},sidecar.decode(h));
        int[] copy=sidecar.codes(); copy[0]=1;
        assertEquals(0,sidecar.codeAt(0,0,0));
        var restored=M3NcmSidecar.fromBytes(identity,h,sidecar.toBytes());
        assertEquals(sidecar.rootHash(),restored.rootHash());
        assertNotEquals(sidecar.storageKey(),restored.storageKey());
        assertThrows(IllegalArgumentException.class,
                ()->M3NcmSidecar.fromBytes(identity("different"),h,sidecar.toBytes()));
        assertThrows(IllegalArgumentException.class,
                ()->sidecar.decode(new M3NcmHierarchy("different-version",1,new int[]{2,4},
                        new float[][]{{0,2},{-.5f,0,.5f}})));
    }

    @Test void corruptedTruncatedOversizedAndInvalidCodesRefuse() {
        var h=hierarchy(); var id=identity("x");
        var sidecar=M3NcmSidecar.of(id,h,new int[]{0,1,1,0,2,1},1);
        byte[] corrupt=sidecar.toBytes(); corrupt[corrupt.length-1]^=1;
        assertThrows(IllegalArgumentException.class,()->M3NcmSidecar.fromBytes(id,h,corrupt));
        assertThrows(IllegalArgumentException.class,()->M3NcmSidecar.fromBytes(id,h,new byte[0]));
        assertThrows(IllegalArgumentException.class,()->M3NcmSidecar.of(id,h,new int[]{0,2,1,0,2,1},1));
        assertThrows(IllegalArgumentException.class,()->M3NcmSidecar.of(id,h,new int[0],Integer.MAX_VALUE));
        assertThrows(IndexOutOfBoundsException.class,()->sidecar.codeAt(0,1,4));
        assertThrows(IllegalArgumentException.class,()->sidecar.toBytes(16));
    }
}
