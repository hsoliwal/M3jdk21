/*
 * Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
 * Independent collection reference and frozen Apache/M3 differential protocol.
 */
import com.m3.collections.M3IndexMaskCompactor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
import org.apache.commons.collections4.BoundedCollection;

public final class CircularFifoProtocolProof {
    private static Class<?> owner;
    private static Method get;
    private static Method atCapacity;
    private static long checks;
    private static long operations;
    private static MessageDigest digest;
    private CircularFifoProtocolProof() { }
    private static void check(boolean condition,String label) {
        checks++; if(!condition)throw new AssertionError(label);
    }
    private static void equal(Object actual,Object expected,String label) {
        check(Objects.equals(actual,expected),label+": "+actual+" != "+expected);
    }
    private static Object unwrap(ReflectiveOperationException failure) {
        Throwable cause=failure instanceof InvocationTargetException wrapped?wrapped.getCause():failure;
        return Arrays.asList("EXCEPTION",cause.getClass().getName(),cause.getMessage());
    }
    @SuppressWarnings("unchecked")
    private static Queue<Integer> create(int capacity) throws Exception {
        return (Queue<Integer>)owner.getConstructor(int.class).newInstance(capacity);
    }
    private static Object index(Queue<Integer> queue,int position) {
        try{return get.invoke(queue,position);}
        catch(ReflectiveOperationException failure){return unwrap(failure);}
    }
    private static Object failure(Runnable operation) {
        try {operation.run();return "NONE";}
        catch(RuntimeException failure){return Arrays.asList("EXCEPTION",failure.getClass().getName(),failure.getMessage());}
    }
    private static void trace(String label,Object result,Queue<Integer> queue) {
        String value=label+"|"+result+"|"+new ArrayList<>(queue)+"|"+queue.size()+"|"+queue.peek()+"\n";
        digest.update(value.getBytes(StandardCharsets.UTF_8));operations++;
    }
    private static void match(Queue<Integer> queue,List<Integer> model,int capacity) throws Exception {
        equal(new ArrayList<>(queue),model,"encounter order");
        equal(queue.size(),model.size(),"size");
        equal(queue.isEmpty(),model.isEmpty(),"empty");
        equal(queue.peek(),model.isEmpty()?null:model.getFirst(),"peek");
        var bounded=(BoundedCollection<?>)queue;
        equal(bounded.maxSize(),capacity,"capacity");
        check(!bounded.isFull(),"evicting queue accepts at capacity");
        equal(atCapacity.invoke(queue),model.size()==capacity,"at capacity");
        check(Arrays.equals(queue.toArray(),model.toArray()),"untyped toArray");
        Integer[] oversized=new Integer[capacity+3];Arrays.fill(oversized,-999);
        Integer[] actual=queue.toArray(oversized);
        check(actual==oversized,"large destination identity");
        for(int i=0;i<model.size();i++)equal(actual[i],model.get(i),"typed prefix");
        equal(actual[model.size()],null,"typed terminator");
        for(int i=model.size()+1;i<actual.length;i++)equal(actual[i],-999,"typed tail retained");
        check(Arrays.equals(queue.toArray(Integer[]::new),model.toArray(Integer[]::new)),"array factory");
        for(int i=0;i<model.size();i++)equal(index(queue,i),model.get(i),"get");
        equal(queue.contains(null),false,"null absent");
    }
    private static void boundary() throws Exception {
        for(int invalid:new int[]{-1,0}) {
            Object observed;
            try{owner.getConstructor(int.class).newInstance(invalid);observed="NONE";}
            catch(ReflectiveOperationException failure){observed=unwrap(failure);}
            check(observed instanceof List<?> list&&list.get(1).equals(IllegalArgumentException.class.getName()),"invalid capacity");
        }
        Queue<?> defaultQueue=(Queue<?>)owner.getConstructor().newInstance();
        equal(((BoundedCollection<?>)defaultQueue).maxSize(),32,"default32");
        Object emptyCollection;
        try{owner.getConstructor(Collection.class).newInstance(List.of());emptyCollection="NONE";}
        catch(ReflectiveOperationException failure){emptyCollection=unwrap(failure);}
        check(emptyCollection instanceof List<?> list&&list.get(1).equals(IllegalArgumentException.class.getName()),"empty collection capacity");
        for(int capacity:new int[]{1,2,3,5,8,17}) {
            Queue<Integer> queue=create(capacity);List<Integer> model=new ArrayList<>();
            trace("empty-remove",failure(()->queue.remove()),queue);
            trace("empty-element",failure(()->queue.element()),queue);
            for(int i=0;i<capacity;i++){queue.add(i);model.add(i);}
            equal(failure(()->queue.add(null)),Arrays.asList("EXCEPTION",NullPointerException.class.getName(),"element"),"null before eviction");
            match(queue,model,capacity);
            for(int i=0;i<3*capacity+2;i++){queue.add(i+100);model.removeFirst();model.add(i+100);}
            match(queue,model,capacity);
            trace("get-negative",index(queue,-1),queue);trace("get-end",index(queue,queue.size()),queue);
            Iterator<Integer> iterator=queue.iterator();
            equal(failure(iterator::remove),Arrays.asList("EXCEPTION",IllegalStateException.class.getName(),null),"remove before next");
            while(iterator.hasNext()) {
                Integer delivered=iterator.next();
                iterator.hasNext();iterator.hasNext();
                iterator.remove();model.remove(delivered);
                match(queue,model,capacity);
                equal(failure(iterator::remove),Arrays.asList("EXCEPTION",IllegalStateException.class.getName(),null),"double removal");
            }
            equal(failure(iterator::next),Arrays.asList("EXCEPTION",NoSuchElementException.class.getName(),null),"exhausted next");
            trace("after drain",null,queue);
        }
    }
    private static void randomized() throws Exception {
        Random random=new Random(0x4d334649464fL);
        for(int capacity:new int[]{1,2,3,5,8,17,32}) {
            Queue<Integer> queue=create(capacity);List<Integer> model=new ArrayList<>();
            for(int step=0;step<5000;step++) {
                int action=random.nextInt(10),value=random.nextInt(31)-15;
                Object result;
                switch(action) {
                    case 0,1 -> {result=action==0?queue.add(value):queue.offer(value);if(model.size()==capacity)model.removeFirst();model.add(value);equal(result,true,"add always true");}
                    case 2 -> {result=queue.poll();equal(result,model.isEmpty()?null:model.removeFirst(),"poll");}
                    case 3 -> {result=queue.remove(value);equal(result,model.remove(Integer.valueOf(value)),"remove object");}
                    case 4 -> {result=queue.contains(value);equal(result,model.contains(value),"contains");}
                    case 5 -> {queue.clear();model.clear();result=null;}
                    case 6 -> {result=queue.removeIf(element->element<0);boolean changed=model.removeIf(element->element<0);equal(result,changed,"removeIf");}
                    case 7 -> {List<Integer> input=Arrays.asList(value,value+1,value+2);result=queue.addAll(input);for(int element:input){if(model.size()==capacity)model.removeFirst();model.add(element);}equal(result,true,"bulk add");}
                    case 8 -> {List<Integer> keep=Arrays.asList(value,value+1);result=queue.retainAll(keep);equal(result,model.retainAll(keep),"retainAll");}
                    default -> {List<Integer> remove=Arrays.asList(value,value+1);result=queue.removeAll(remove);equal(result,model.removeAll(remove),"removeAll");}
                }
                match(queue,model,capacity);trace("random"+capacity+"/"+step+"/"+action,result,queue);
            }
        }
    }
    private static void cursorProtocols() throws Exception {
        Random random=new Random(0x435552534f52L);
        for(int trial=0;trial<1000;trial++) {
            int capacity=1+random.nextInt(17);
            Queue<Integer> queue=create(capacity);List<Integer> reference=new ArrayList<>();
            int supplied=random.nextInt(3*capacity+1);
            for(int i=0;i<supplied;i++){queue.add(i);if(reference.size()==capacity)reference.removeFirst();reference.add(i);}
            Iterator<Integer> actual=queue.iterator(),expected=reference.iterator();
            for(int step=0;step<60;step++) {
                int action=random.nextInt(3);
                Object left=cursor(actual,action),right=cursor(expected,action);
                // JDK's exception messages are unspecified; compare exception classes and values.
                Object normalizedLeft=left instanceof List<?> l&&l.getFirst().equals("EXCEPTION")?l.subList(0,2):left;
                Object normalizedRight=right instanceof List<?> l&&l.getFirst().equals("EXCEPTION")?l.subList(0,2):right;
                equal(normalizedLeft,normalizedRight,"legal cursor protocol");
                equal(new ArrayList<>(queue),reference,"cursor owner");
                trace("cursor"+trial+"/"+step+"/"+action,left,queue);
            }
        }
    }
    private static Object cursor(Iterator<Integer> iterator,int action) {
        try {
            if(action==0)return iterator.hasNext();
            if(action==1)return iterator.next();
            iterator.remove();return "REMOVED";
        }catch(RuntimeException failure){return Arrays.asList("EXCEPTION",failure.getClass().getName(),failure.getMessage());}
    }
    @SuppressWarnings("unchecked")
    private static Queue<Integer> roundtrip(Queue<Integer> queue) throws Exception {
        var bytes=new ByteArrayOutputStream();
        try(var output=new ObjectOutputStream(bytes)){output.writeObject(queue);}
        try(var input=new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))){
            return (Queue<Integer>)input.readObject();
        }
    }
    private static void serialization() throws Exception {
        var serial=ObjectStreamClass.lookup(owner);
        equal(serial.getSerialVersionUID(),-8423413834657610406L,"UID");
        equal(serial.getFields().length,1,"default serialized fields");
        equal(serial.getFields()[0].getName(),"maxElements","field name");
        equal(serial.getFields()[0].getType(),int.class,"field type");
        for(int capacity:new int[]{1,2,3,5,8,17})for(int supplied:new int[]{0,1,3,17,51}) {
            Queue<Integer> queue=create(capacity);
            for(int i=0;i<supplied;i++)queue.add(i);
            List<Integer> before=new ArrayList<>(queue);
            Queue<Integer> copy=roundtrip(queue);match(copy,before,capacity);
            copy.add(999);List<Integer> after=new ArrayList<>(before);if(after.size()==capacity)after.removeFirst();after.add(999);
            match(copy,after,capacity);equal(new ArrayList<>(queue),before,"roundtrip detached");
            trace("serde"+capacity+"/"+supplied,null,copy);
        }
    }
    private static void masks(boolean nativeRequired) throws Exception {
        var backend=nativeRequired?M3IndexMaskCompactor.Backend.NATIVE_REQUIRED:M3IndexMaskCompactor.Backend.JAVA;
        for(int capacity:new int[]{1,3,5,17}) {
            Queue<Integer> queue=create(capacity);for(int i=0;i<3*capacity+2;i++)queue.add(i);
            int[] rows=new int[queue.size()];byte[] states=new byte[rows.length];
            for(int i=0;i<rows.length;i++){rows[i]=i;states[i]=(byte)(i*37);}
            for(int mask=0;mask<256;mask++) {
                int[] selected=M3IndexMaskCompactor.select(rows,states,mask,backend);
                List<Integer> expected=new ArrayList<>(),actual=new ArrayList<>();
                for(int i=0;i<rows.length;i++)if(((states[i]&255)&mask)==mask)expected.add((Integer)index(queue,i));
                for(int index:selected)actual.add((Integer)index(queue,index));
                equal(actual,expected,"aligned mask projection");
                check(Arrays.equals(rows,java.util.stream.IntStream.range(0,rows.length).toArray()),"index lane unchanged");
            }
        }
    }
    public static void main(String[] args) throws Exception {
        if(args.length<1||args.length>2)throw new IllegalArgumentException("implementation class [native]");
        owner=Class.forName(args[0]);get=owner.getMethod("get",int.class);atCapacity=owner.getMethod("isAtFullCapacity");
        digest=MessageDigest.getInstance("SHA-256");
        boundary();randomized();cursorProtocols();serialization();
        masks(args.length==2&&args[1].equals("native"));
        System.out.println("{\"status\":\"PASS\",\"checks\":"+checks+",\"operations\":"+operations
            +",\"protocolSHA256\":\""+HexFormat.of().formatHex(digest.digest())+"\"}");
    }
}
