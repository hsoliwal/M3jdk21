// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
// Comparator body retained verbatim from Apache Lucene8.11.2; see donor notices.
import java.util.*;
public final class M3InstanceUtf8DonorOracle {
 static final class CharsRef {final char[] chars;final int offset,length;CharsRef(String s){chars=s.toCharArray();offset=0;length=chars.length;}}
 static final class FutureArrays {static int mismatch(char[]a,int af,int at,char[]b,int bf,int bt){return Arrays.mismatch(a,af,at,b,bf,bt);}}
  private static class UTF16SortedAsUTF8Comparator implements Comparator<CharsRef> {
    // Only singleton
    private UTF16SortedAsUTF8Comparator() {};

    @Override
    public int compare(CharsRef a, CharsRef b) {
      int aEnd = a.offset + a.length;
      int bEnd = b.offset + b.length;
      int i = FutureArrays.mismatch(a.chars, a.offset, aEnd, 
                                    b.chars, b.offset, bEnd);

      if (i >= 0 && i < Math.min(a.length, b.length)) {
        // http://icu-project.org/docs/papers/utf16_code_point_order.html

        char aChar = a.chars[a.offset + i];
        char bChar = b.chars[b.offset + i];        
        /* aChar != bChar, fix up each one if they're both in or above the surrogate range, then compare them */
        if (aChar >= 0xd800 && bChar >= 0xd800) {
          if (aChar >= 0xe000) {
            aChar -= 0x800;
          } else {
            aChar += 0x2000;
          }
          
          if (bChar >= 0xe000) {
            bChar -= 0x800;
          } else {
            bChar += 0x2000;
          }
        }

        /* now aChar and bChar are in code point order */
        return (int)aChar - (int)bChar; /* int must be 32 bits wide */
      }

      // One is a prefix of the other, or, they are equal:
      return a.length - b.length;
    }
  }

 public static int compare(String left,String right){return Integer.signum(new UTF16SortedAsUTF8Comparator().compare(new CharsRef(left),new CharsRef(right)));}
}
