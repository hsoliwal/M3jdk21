// SPDX-License-Identifier: Apache-2.0
package jdk.m3.structured;
import org.junit.jupiter.api.Test;
final class DelimitedIndexJUnitTest {
    @Test void productJavaAndRequiredNative() throws Exception {
        String library = System.getProperty("m3.structured.native", "");
        DelimitedIndexContract.main(library.isEmpty() ? new String[]{"java"}
                : new String[]{"native", library});
    }
}
