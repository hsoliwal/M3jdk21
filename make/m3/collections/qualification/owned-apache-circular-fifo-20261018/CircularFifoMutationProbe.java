/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Queue;
import java.util.Random;

public final class CircularFifoMutationProbe {
    private CircularFifoMutationProbe() { }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        Class<?> owner=Class.forName(args[0]);
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        Random random=new Random(0x4558544649464fL);
        int operations=0,exceptions=0;
        for(int capacity:new int[]{1,2,3,5,7})for(int trial=0;trial<100;trial++) {
            Queue<Integer> queue=(Queue<Integer>)owner.getConstructor(int.class).newInstance(capacity);
            for(int i=0;i<3*capacity;i++)queue.add(i);
            Iterator<Integer> iterator=queue.iterator();
            for(int step=0;step<50;step++) {
                int action=random.nextInt(8);
                Object result;
                try {
                    result=switch(action) {
                        case 0 -> iterator.hasNext();
                        case 1 -> iterator.next();
                        case 2 -> {iterator.remove();yield "REMOVED";}
                        case 3 -> queue.add(random.nextInt(20));
                        case 4 -> queue.poll();
                        case 5 -> {queue.clear();yield "CLEARED";}
                        case 6 -> queue.addAll(Arrays.asList(11,null,12));
                        default -> {iterator=queue.iterator();yield "NEW_ITERATOR";}
                    };
                }catch(RuntimeException failure) {
                    result=Arrays.asList(failure.getClass().getName(),failure.getMessage());exceptions++;
                }
                String row=capacity+"/"+trial+"/"+step+"/"+action+"/"+result+"/"+queue.size()+"/"+new ArrayList<>(queue)+"\n";
                digest.update(row.getBytes(StandardCharsets.UTF_8));operations++;
            }
        }
        Object collectionNull;
        try{owner.getConstructor(java.util.Collection.class).newInstance(new Object[]{null});collectionNull="NONE";}
        catch(InvocationTargetException failure){collectionNull=Arrays.asList(failure.getCause().getClass().getName(),failure.getCause().getMessage());}
        digest.update(String.valueOf(collectionNull).getBytes(StandardCharsets.UTF_8));
        System.out.println("{\"operations\":"+operations+",\"exceptionObservations\":"+exceptions
            +",\"protocolSHA256\":\""+HexFormat.of().formatHex(digest.digest())
            +"\",\"scope\":\"provider-specific external mutation/null-prefix observations only\"}");
    }
}
