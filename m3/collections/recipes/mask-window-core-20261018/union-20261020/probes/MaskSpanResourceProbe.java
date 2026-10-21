// SPDX-License-Identifier: Apache-2.0
import com.m3.collections.M3IndexMaskCompactor;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
/** Bounded prepared-workload observation, not JMH or an end-to-end claim. */
public final class MaskSpanResourceProbe {
 private static volatile long sink;
 private MaskSpanResourceProbe() { }
 public static void main(String[] args) {
  if(args.length!=1)throw new IllegalArgumentException("variant label");
  int n=262144;int[] rows=new int[n];for(int i=0;i<n;i++)rows[i]=i;
  var bean=ManagementFactory.getThreadMXBean();
  for(String density:new String[]{"singleton","sparse","half","all","zero"}) {
   byte[] states=new byte[n];int mask=density.equals("zero")?0:1;
   for(int i=0;i<n;i++)states[i]=(byte)(density.equals("singleton")?(i%256==127?1:0):density.equals("sparse")?(i%97==41?1:0):density.equals("half")?(i%2):1);
   for(int warm=0;warm<32;warm++)sink=call(rows,states,mask);
   for(int sample=0;sample<5;sample++){
    long cpu=bean.getCurrentThreadCpuTime(),wall=System.nanoTime(),sum=0;
    for(int op=0;op<20;op++)sum+=call(rows,states,mask);
    long elapsed=System.nanoTime()-wall;cpu=bean.getCurrentThreadCpuTime()-cpu;sink=sum;
    System.out.println(args[0]+"\t"+density+"\t"+sample+"\t"+cpu+"\t"+elapsed+"\t"+sum);
   }
  }
 }
 private static long call(int[] rows,byte[] states,int mask){int[] out=M3IndexMaskCompactor.select(rows,states,mask,M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);return out.length==0?0:(long)out.length+out[0]+out[out.length-1];}
}
