// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import org.junit.jupiter.api.Test;

/** Maven/JUnit entrypoint for the explicit collection-algebra contract oracle. */
final class LongCompositeOperatorDefaultsTest {
    @Test void checksOperatorCapabilitiesWithoutChangingCollectionInterfaces() {
        LongCompositeOperatorDefaultsSelfTest.main(new String[0]);
    }
}
