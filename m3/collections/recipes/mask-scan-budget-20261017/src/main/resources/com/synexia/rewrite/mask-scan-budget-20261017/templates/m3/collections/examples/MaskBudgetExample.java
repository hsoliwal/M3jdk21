/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
import com.m3.collections.M3IndexMaskCompactor;
import com.m3.collections.M3IndexMaskWindow;
import java.util.Arrays;

/** Ordered physical IDs plus logical masks; a reusable caller buffer, no row wrappers. */
public final class MaskBudgetExample {
    public static void main(String[] args) {
        var backend=args.length==0?M3IndexMaskCompactor.Backend.JAVA:M3IndexMaskCompactor.Backend.NATIVE_REQUIRED;
        // Supplied by an existing sorted/filtered owner: equal IDs remain separate occurrences.
        int[] orderedRows={7,7,2,9,4,4,8};
        byte[] state={0,0,(byte)128,0,(byte)128,(byte)129,0};
        int[] output=new int[4]; int cursor=0,total=0,pages=0;
        while(cursor<orderedRows.length) {
            long page=M3IndexMaskWindow.scanBounded(orderedRows,state,cursor,2,128,1,
                    output,1,2,backend,null);
            int next=M3IndexMaskWindow.nextIndex(page),count=M3IndexMaskWindow.written(page);
            if(next<=cursor || next-cursor>2)throw new AssertionError("source budget");
            System.out.println("cursor="+next+" rows="+Arrays.toString(Arrays.copyOfRange(output,1,1+count)));
            total+=count;cursor=next;pages++;
            // A UI scheduler may yield here. Revalidate the parent revision before resuming.
        }
        if(total!=2||pages!=4)throw new AssertionError("resumption lost a selected occurrence");
    }
}
