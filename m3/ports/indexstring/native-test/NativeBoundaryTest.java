// SPDX-License-Identifier: Apache-2.0
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/** Stock-JDK JNI boundary probe; it does not exercise a modified VM. */
public final class NativeBoundaryTest {
  private NativeBoundaryTest() { }
  private static native boolean utf16be(ByteBuffer data, char[] expected);
  private static native String modifiedUtf8RoundTrip(String text);
  private static native String criticalCopy(String text);
  public static void main(String[] args) {
    System.load(Objects.requireNonNull(args[0]));
    String[] samples={"", "\0", "A\0B", "\uD83D\uDE03", "\uD800", "\uDC00", "x\uD800\uDC00y", "\uFFFF\u0100"};
    int checks=0;
    for (int iteration=0;iteration<4096;iteration++) {
      String sample=samples[iteration%samples.length];
      ByteBuffer buffer=ByteBuffer.allocateDirect(2*sample.length()+1).order(ByteOrder.BIG_ENDIAN);
      buffer.position(1);
      for (int i=0;i<sample.length();i++)buffer.putChar(sample.charAt(i));
      ByteBuffer odd=buffer.flip().position(1).slice().asReadOnlyBuffer();
      if (!utf16be(odd,sample.toCharArray()))throw new AssertionError("direct units");checks++;
      if (!modifiedUtf8RoundTrip(sample).equals(sample))throw new AssertionError("modified UTF-8");checks++;
      if (!criticalCopy(sample).equals(sample))throw new AssertionError("critical ownership");checks++;
    }
    try { utf16be(ByteBuffer.wrap(new byte[2]),new char[]{'a'});throw new AssertionError("heap accepted"); }
    catch (IllegalArgumentException expected) { checks++; }
    try { utf16be(ByteBuffer.allocateDirect(1),new char[]{'a'});throw new AssertionError("odd accepted"); }
    catch (IllegalArgumentException expected) { checks++; }
    try { modifiedUtf8RoundTrip(null);throw new AssertionError("null accepted"); }
    catch (NullPointerException expected) { checks++; }
    try { criticalCopy(null);throw new AssertionError("null critical accepted"); }
    catch (NullPointerException expected) { checks++; }
    try { utf16be(null,new char[0]);throw new AssertionError("null direct accepted"); }
    catch (NullPointerException expected) { checks++; }
    try { utf16be(ByteBuffer.allocateDirect(4),new char[1]);throw new AssertionError("capacity accepted"); }
    catch (IllegalArgumentException expected) { checks++; }
    System.out.println("STOCK_JNI_BOUNDARY_PASS checks="+checks);
  }
}
