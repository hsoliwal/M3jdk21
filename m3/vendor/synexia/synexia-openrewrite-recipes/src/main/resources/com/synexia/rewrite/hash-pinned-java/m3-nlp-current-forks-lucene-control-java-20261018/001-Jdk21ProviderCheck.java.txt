// SPDX-License-Identifier: Apache-2.0
import org.apache.lucene.util.VectorUtil;

/** The real core provider must retain its scalar implementation on Java 21. */
public final class Jdk21ProviderCheck {
  public static void main(String[] args) {
    float result = VectorUtil.dotProduct(new float[] {1f, 2f}, new float[] {3f, 4f});
    if (result != 11f) throw new AssertionError("dot product: " + result);
    System.out.println("Java 21 core vector fallback passed");
  }
}
