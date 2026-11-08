/*
 * @test
 * @summary JNI/JVMTI reports shallow object sizes, not reference counts
 * @modules java.base/jdk.internal.m3
 * @run main/othervm M3ObjectMetricsTest
 */

import jdk.internal.m3.ObjectMetrics;

public class ObjectMetricsTest {
    public static void main(String[] args) {
        byte[] small = new byte[0];
        byte[] large = new byte[8192];
        long smallSize = ObjectMetrics.shallowSize(small);
        long largeSize = ObjectMetrics.shallowSize(large);
        long objectSize = ObjectMetrics.shallowSize(new Object());
        if (smallSize <= 0 || largeSize <= smallSize || objectSize <= 0) {
            throw new AssertionError("Unexpected shallow sizes: " + smallSize
                    + " / " + largeSize + " / " + objectSize);
        }
        if (smallSize != ObjectMetrics.shallowSize(small)) {
            throw new AssertionError("Size of an unchanged array must remain stable");
        }
        try {
            ObjectMetrics.shallowSize(null);
            throw new AssertionError("Expected NullPointerException");
        } catch (NullPointerException expected) {
            // expected
        }
        System.out.println("M3ObjectMetricsTest PASS small=" + smallSize
                + " large=" + largeSize + " object=" + objectSize);
    }
}
