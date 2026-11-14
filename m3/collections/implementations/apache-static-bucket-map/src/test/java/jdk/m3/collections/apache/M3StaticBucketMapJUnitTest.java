// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
package jdk.m3.collections.apache;

import org.junit.jupiter.api.Test;

/** Maven/Surefire owner of the real Java21 array-backed map differential. */
final class M3StaticBucketMapJUnitTest {
    @Test
    void arrayBackendMapContract() throws Exception {
        M3StaticBucketMapProof.main(new String[0]);
    }
}
