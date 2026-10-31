/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
import com.m3.collections.M3IndexMaskCompactor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

/** Separate primitive-lane integration proof; never reads private collection state. */
public final class GuavaEvictingQueueMaskProjectionProof {
    private static Method factory;
    private static long checks,cases;
    private static MessageDigest digest;
    private static final boolean[] unsignedStates=new boolean[256];
    private GuavaEvictingQueueMaskProjectionProof() { }
    private static void check(boolean condition,String label) {
        checks++;if(!condition)throw new AssertionError(label);
    }
    @SuppressWarnings("unchecked")
    private static Queue<Integer> create(int capacity) throws Exception {
        return (Queue<Integer>)factory.invoke(null,capacity);
    }
    @SuppressWarnings("unchecked")
    private static Queue<Integer> roundtrip(Queue<Integer> queue) throws Exception {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(ObjectOutputStream output=new ObjectOutputStream(bytes)){output.writeObject(queue);}
        try(ObjectInputStream input=new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))){
            return (Queue<Integer>)input.readObject();
        }
    }
    private static void append(List<Integer> model,int capacity,int value) {
        if(capacity==0)return;
        if(model.size()==capacity)model.removeFirst();
        model.add(value);
    }
    private static Queue<Integer> frame(int capacity,int frame,List<Integer> model) throws Exception {
        Queue<Integer> queue=create(capacity);
        if(frame==0)return queue;
        for(int i=0;i<3*capacity+5;i++){
            int value=(i%7)-3;queue.add(value);append(model,capacity,value);
        }
        if(frame==2&&!model.isEmpty()) {
            Iterator<Integer> cursor=queue.iterator();int count=0;
            while(cursor.hasNext()){
                cursor.next();
                if((count++&1)==0)cursor.remove();
            }
            List<Integer> retained=new ArrayList<>();
            for(int i=0;i<model.size();i++)if((i&1)!=0)retained.add(model.get(i));
            model.clear();model.addAll(retained);
        }
        if(frame==3) {
            queue.clear();model.clear();
            for(int i=0;i<Math.min(2,capacity);i++){queue.add(i);append(model,capacity,i);}
            queue=roundtrip(queue);
            for(int i=0;i<capacity+2;i++){queue.add(i%5);append(model,capacity,i%5);}
        }
        return queue;
    }
    private static int[] selected(int[] rows,int offset,byte[] states,int stateOffset,int length,int mask) {
        List<Integer> values=new ArrayList<>();
        for(int i=0;i<length;i++)if((Byte.toUnsignedInt(states[stateOffset+i])&mask)==mask)values.add(rows[offset+i]);
        return values.stream().mapToInt(Integer::intValue).toArray();
    }
    private static long[] selected(long[] rows,int offset,byte[] states,int stateOffset,int length,int mask) {
        List<Long> values=new ArrayList<>();
        for(int i=0;i<length;i++)if((Byte.toUnsignedInt(states[stateOffset+i])&mask)==mask)values.add(rows[offset+i]);
        return values.stream().mapToLong(Long::longValue).toArray();
    }
    private static void exercise(Queue<Integer> queue,List<Integer> model,int capacity,int frame,
            M3IndexMaskCompactor.Backend backend) throws Exception {
        Integer[] payload=queue.toArray(Integer[]::new);
        List<Integer> cursorPayload=new ArrayList<>();queue.iterator().forEachRemaining(cursorPayload::add);
        check(Arrays.asList(payload).equals(model)&&cursorPayload.equals(model),"public array and cursor model");
        int count=payload.length;
        int[] ordinals=new int[count*2];
        for(int i=0;i<ordinals.length;i++)ordinals[i]=i%count;
        byte[] aligned=new byte[ordinals.length];
        for(int seed=0;seed<256;seed++) {
            int rowOffset=1+(seed%3),stateOffset=2+(seed%2);
            int length=count==0?0:count*2+3;
            int[] signedRows=new int[length+7];
            long[] longRows=new long[length+7];
            byte[] states=new byte[length+7];
            Arrays.fill(signedRows,-777);Arrays.fill(longRows,-777L);Arrays.fill(states,(byte)0xff);
            for(int i=0;i<aligned.length;i++) {
                aligned[i]=(byte)(seed*29+i*57);unsignedStates[Byte.toUnsignedInt(aligned[i])]=true;
            }
            for(int i=0;i<length;i++) {
                signedRows[rowOffset+i]=switch(i%5){case 0,4->Integer.MIN_VALUE;case 1->Integer.MAX_VALUE;case 2->-1;default->0;};
                longRows[rowOffset+i]=switch(i%5){case 0,4->Long.MIN_VALUE;case 1->Long.MAX_VALUE;case 2->-1L;default->0L;};
                states[stateOffset+i]=(byte)(seed*29+i*57);unsignedStates[Byte.toUnsignedInt(states[stateOffset+i])]=true;
            }
            int[] ordinalBefore=ordinals.clone(),signedBefore=signedRows.clone();
            long[] longBefore=longRows.clone();byte[] alignedBefore=aligned.clone(),statesBefore=states.clone();
            for(int mask=0;mask<256;mask++) {
                int[] projected=M3IndexMaskCompactor.select(ordinals,aligned,mask,backend);
                int[] expected=selected(ordinals,0,aligned,0,aligned.length,mask);
                check(Arrays.equals(projected,expected),"ordinal projection values/order/duplicates");
                check(projected!=ordinals,"ordinal result detached");
                List<Integer> actualPayload=new ArrayList<>(),expectedPayload=new ArrayList<>();
                for(int ordinal:projected)actualPayload.add(payload[ordinal]);
                for(int ordinal:expected)expectedPayload.add(model.get(ordinal));
                check(actualPayload.equals(expectedPayload),"selected public payload");
                int[] ints=M3IndexMaskCompactor.selectWindow(signedRows,rowOffset,states,stateOffset,length,mask,backend);
                long[] longs=M3IndexMaskCompactor.selectLongWindow(longRows,rowOffset,states,stateOffset,length,mask,backend);
                check(Arrays.equals(ints,selected(signedRows,rowOffset,states,stateOffset,length,mask)),"int offset window");
                check(Arrays.equals(longs,selected(longRows,rowOffset,states,stateOffset,length,mask)),"long offset window");
                check(ints!=signedRows&&longs!=longRows,"window results detached");
                check(Arrays.equals(ordinals,ordinalBefore)&&Arrays.equals(signedRows,signedBefore)
                        &&Arrays.equals(longRows,longBefore),"primitive index inputs unchanged");
                check(Arrays.equals(aligned,alignedBefore)&&Arrays.equals(states,statesBefore),"signed state inputs unchanged");
                String row=capacity+"/"+frame+"/"+seed+"/"+mask+"/"+Arrays.toString(projected)
                        +"/"+actualPayload+"/"+Arrays.toString(ints)+"/"+Arrays.toString(longs)+"\n";
                digest.update(row.getBytes(StandardCharsets.UTF_8));cases++;
                if(projected.length>0)projected[0]=-999;
                if(ints.length>0)ints[0]=777;
                if(longs.length>0)longs[0]=777L;
                check(Arrays.equals(ordinals,ordinalBefore)&&Arrays.equals(signedRows,signedBefore)
                        &&Arrays.equals(longRows,longBefore),"output mutation isolated");
            }
        }
        check(Arrays.asList(queue.toArray(Integer[]::new)).equals(model),"queue untouched by projection");
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=2)throw new IllegalArgumentException("implementation class java|native");
        factory=Class.forName(args[0]).getMethod("create",int.class);
        boolean nativeRequired=args[1].equals("native");
        if(!nativeRequired&&!args[1].equals("java"))throw new IllegalArgumentException("java|native");
        M3IndexMaskCompactor.Backend backend=nativeRequired?M3IndexMaskCompactor.Backend.NATIVE_REQUIRED:M3IndexMaskCompactor.Backend.JAVA;
        if(nativeRequired)check(M3IndexMaskCompactor.nativeAvailable(),"real native backend loaded");
        // Keep cross-backend result counters equal while still requiring native presence.
        if(!nativeRequired)check(Objects.nonNull(backend),"explicit Java backend");
        digest=MessageDigest.getInstance("SHA-256");
        for(int capacity:new int[]{0,1,3,17})for(int frame=0;frame<4;frame++) {
            List<Integer> model=new ArrayList<>();
            exercise(frame(capacity,frame,model),model,capacity,frame,backend);
        }
        for(boolean present:unsignedStates)check(present,"all signed byte values exercised as unsigned");
        System.out.println("{\"status\":\"PASS\",\"checks\":"+checks+",\"cases\":"+cases
                +",\"projectionSHA256\":\""+HexFormat.of().formatHex(digest.digest())
                +"\",\"scope\":\"existing aligned primitive kernel over public queue snapshots/cursors; no native queue store or ABI addition\"}");
    }
}
