/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.swing.widgets;

import org.junit.jupiter.api.Test;

/** Existing m3-swing-viewport JUnit launcher discovers these native widget tests. */
class M3NativeWidgetParityTest {
    @Test void oscilloscopeEnvelopeAndRendering() throws Exception {
        M3OscilloscopeSmoke.main(new String[0]);
    }
    @Test void printPreviewPageableAndCaching() throws Exception {
        M3PrintPreviewSmoke.main(new String[0]);
    }
    @Test void maxValueEnvelopeDoesNotDisappear() throws Exception {
        M3OscilloscopeMaxEnvelopeSmoke.main(new String[0]);
    }
}
