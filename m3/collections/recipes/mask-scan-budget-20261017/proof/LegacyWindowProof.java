/* SPDX-License-Identifier: Apache-2.0 */
import com.m3.collections.*;
import java.util.*;
public final class LegacyWindowProof {
    public static void main(String[] args) {
        var backend=args.length>0?M3IndexMaskCompactor.Backend.NATIVE_REQUIRED:M3IndexMaskCompactor.Backend.JAVA;
        Random random=new Random(20261017);long digest=0, checks=0;
        for(int t=0;t<4096;t++) {
            int n=random.nextInt(1300);int[] rows=new int[n];byte[] states=new byte[n];random.nextBytes(states);
            Arrays.setAll(rows,i->i%17);int first=random.nextInt(n+1),limit=random.nextInt(130),req=random.nextInt(256),exc=random.nextInt(256);
            if(t%11==0){req=0;exc=0;}
            int[] actual=new int[limit+4],expected=actual.clone();int next=first,k=0;
            while(next<n&&k<limit){int b=states[next]&255;if((b&req)==req&&(b&exc)==0)expected[2+k++]=rows[next];next++;}
            long value=M3IndexMaskWindow.scan(rows,states,first,req,exc,actual,2,limit,backend);
            if(value!=((long)next<<32|k)||!Arrays.equals(actual,expected))throw new AssertionError("legacy "+t);
            digest=digest*31+value+Arrays.hashCode(actual);checks+=2;
        }
        System.out.println("LegacyWindowProof checks="+checks+" digest="+Long.toHexString(digest));
    }
}
