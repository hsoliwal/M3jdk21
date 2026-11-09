/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
import jdk.internal.mindex.M3TQ;
public final class TQSeamCounterexample {
    private TQSeamCounterexample() { }
    public static void main(String[] args) {
        var aa = M3TQ.precompute("aa", 2);
        if (!aa.concat(aa, 4).test(M3TQ.trigram('a', 'a', 'a'))) {
            throw new AssertionError("DUPLICATE_SET_IS_NOT_SEAM_COMPLETENESS");
        }
        System.out.println("TQ_SEAM_WITNESS_PASS");
    }
}
