// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;
import jdk.m3.collections.text.IntPrefixRollingHash;

/** Direct-array oracle for the source-sealed rolling-range optimization. */
final class M3IntRollingRangeParityTest {
    @Test void randomizedRangesAndPrefixes() {
        var random = new Random(0x71C0AB2L);
        for (int round=0;round<120;round++) {
            int length=round<12? round:random.nextInt(2049);
            int[] data=new int[length];
            for(int i=0;i<length;i++)data[i]=switch(round%5){
                case 0 -> 0;
                case 1 -> i&7;
                case 2 -> random.nextInt(7);
                case 3 -> random.nextInt();
                default -> i%2==0?Integer.MIN_VALUE:Integer.MAX_VALUE;
            };
            var hash=new IntPrefixRollingHash(data);
            for(int j=0;j<350;j++) {
                int a=random.nextInt(length+1),b=random.nextInt(length+1);
                int c=random.nextInt(length+1),d=random.nextInt(length+1);
                int from=Math.min(a,b),to=Math.max(a,b),other=Math.min(c,d),end=Math.max(c,d);
                assertEquals(Arrays.equals(Arrays.copyOfRange(data,from,to),
                        Arrays.copyOfRange(data,other,end)),hash.equalRanges(from,to,other,end));
                assertEquals(hash.hashA(from,to),hash.hash(from,to).first());
                assertEquals(hash.hashB(from,to),hash.hash(from,to).second());
                int max=random.nextInt(length+5),limit=Math.min(max,Math.min(length-a,length-c));
                int expected=0;
                while(expected<limit&&data[a+expected]==data[c+expected])expected++;
                assertEquals(expected,hash.longestCommonPrefix(a,c,max));
            }
        }
    }
    @Test void errorMessagesAndInputIsolation() {
        var hash=new IntPrefixRollingHash(new int[]{1,2,1,2});
        assertEquals("-1..0",assertThrows(IndexOutOfBoundsException.class,
                ()->hash.equalRanges(-1,0,0,0)).getMessage());
        assertEquals("0,5",assertThrows(IndexOutOfBoundsException.class,
                ()->hash.longestCommonPrefix(0,5,0)).getMessage());
        assertEquals("maxLength < 0",assertThrows(IllegalArgumentException.class,
                ()->hash.longestCommonPrefix(0,0,-1)).getMessage());
        int[] data={Integer.MIN_VALUE,1,Integer.MAX_VALUE,1};
        var frozen=new IntPrefixRollingHash(data);
        var before=frozen.hash(0,data.length);
        data[0]^=1;
        assertEquals(before,frozen.hash(0,data.length));
        assertNotEquals(before,new IntPrefixRollingHash(data).hash(0,data.length));
    }
}
