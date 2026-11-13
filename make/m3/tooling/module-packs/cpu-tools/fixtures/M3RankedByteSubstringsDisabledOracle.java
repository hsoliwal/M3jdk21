// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import jdk.m3.tools.cpu.M3RankedByteSubstrings;
import jdk.m3.tools.cpu.M3RankedByteSubstringsNative;

public final class M3RankedByteSubstringsDisabledOracle {
    public static void main(String[] args) {
        if(M3RankedByteSubstringsNative.isAvailable()) throw new AssertionError("native implicitly enabled");
        var owner=M3RankedByteSubstrings.copyOf(new byte[]{1,2,1});
        if(owner.select(1).length==0) throw new AssertionError("Java fallback broken");
        try {
            M3RankedByteSubstringsNative.selectSpans(owner,new long[]{1});
            throw new AssertionError("disabled native admitted");
        } catch (IllegalStateException expected) {
            if(!"ranked native backend not configured".equals(expected.getMessage()))
                throw new AssertionError(expected);
        }
        System.out.println("M3_RANKED_NATIVE_DISABLED_PASS checks=3");
    }
}
