/*
 * Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
 * Prewritten Guava33.4.8-jre protocol fixture. No candidate algorithm is embedded.
 * Same reflection factory runs the donor and a later complete mapped owner.
 * Mandatory ordered-content/cursor models are independent ArrayList state.
 * Selected provider policy observations are separately counted and hashed.
 */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
import java.util.Spliterator;
import java.util.function.IntFunction;
import java.util.function.Predicate;

public final class GuavaEvictingQueueProtocolProof {
    private static Class<?> owner;
    private static Method factory;
    private static Method remaining;
    private static long checks, operations, policyChecks, policyOperations, observations;
    private static MessageDigest protocol, policy, observed;
    private GuavaEvictingQueueProtocolProof() { }
    @FunctionalInterface private interface Operation { Object run() throws Exception; }
    private record Outcome(Object value,String failure,String message) { }
    private static void check(boolean condition,String label) {
        checks++; if(!condition)throw new AssertionError(label);
    }
    private static void equal(Object actual,Object expected,String label) {
        check(Objects.equals(actual,expected),label+": "+actual+" != "+expected);
    }
    private static void selected(boolean condition,String label) {
        policyChecks++; if(!condition)throw new AssertionError("selected policy: "+label);
    }
    private static Outcome capture(Operation operation) {
        try{return new Outcome(operation.run(),null,null);}
        catch(InvocationTargetException wrapped) {
            Throwable cause=wrapped.getCause();
            return new Outcome(null,cause.getClass().getName(),cause.getMessage());
        }catch(Exception | StackOverflowError failure) {
            return new Outcome(null,failure.getClass().getName(),failure.getMessage());
        }
    }
    private static Outcome cursor(Iterator<Integer> iterator,int action) {
        return capture(()->{
            if(action==0)return iterator.hasNext();
            if(action==1)return iterator.next();
            iterator.remove();return "REMOVED";
        });
    }
    private static String render(Outcome outcome) {
        return outcome.failure==null?"VALUE/"+outcome.value:"EXCEPTION/"+outcome.failure+"/"+outcome.message;
    }
    private static void update(MessageDigest digest,String value) {
        digest.update((value+"\n").getBytes(StandardCharsets.UTF_8));
    }
    private static List<Integer> snapshot(Queue<Integer> queue) {
        return Arrays.asList(queue.toArray(Integer[]::new));
    }
    private static void trace(String label,Object value,Queue<Integer> queue) {
        update(protocol,label+"|"+value+"|"+snapshot(queue)+"|"+queue.size()+"|"+queue.peek());
        operations++;
    }
    private static void policyTrace(String label,Object value,Queue<Integer> queue) {
        update(policy,label+"|"+value+"|"+snapshot(queue)+"|"+queue.size()+"|"+queue.peek());
        policyOperations++;
    }
    private static void observation(String label,Object value) {
        update(observed,label+"|"+value);observations++;
    }
    @SuppressWarnings("unchecked")
    private static <E> Queue<E> create(int capacity) throws Exception {
        return (Queue<E>)factory.invoke(null,capacity);
    }
    private static void append(List<Integer> model,int capacity,Integer value) {
        Objects.requireNonNull(value);
        if(capacity==0)return;
        if(model.size()==capacity)model.removeFirst();
        model.add(value);
    }
    private static boolean bulkModel(List<Integer> model,int capacity,List<Integer> input) {
        int first=0;
        if(input.size()>=capacity){model.clear();first=input.size()-capacity;}
        boolean changed=false;
        for(int i=first;i<input.size();i++){append(model,capacity,input.get(i));changed=true;}
        return changed;
    }
    private static void match(Queue<Integer> queue,List<Integer> model,int capacity) throws Exception {
        equal(snapshot(queue),model,"FIFO contents");
        equal(new ArrayList<>(queue),model,"iterator contents");
        equal(queue.size(),model.size(),"size");
        equal(queue.isEmpty(),model.isEmpty(),"isEmpty");
        equal(queue.peek(),model.isEmpty()?null:model.getFirst(),"peek");
        equal(remaining.invoke(queue),capacity-model.size(),"remainingCapacity");
        check(Arrays.equals(queue.toArray(),model.toArray()),"Object[] contents");
        Integer[] target=new Integer[capacity+3];Arrays.fill(target,-999);
        Integer[] result=queue.toArray(target);
        check(result==target,"oversized typed target identity");
        for(int i=0;i<model.size();i++)equal(result[i],model.get(i),"typed prefix");
        equal(result[model.size()],null,"typed null terminator");
        for(int i=model.size()+1;i<result.length;i++)equal(result[i],-999,"typed tail untouched");
        check(Arrays.equals(queue.toArray(Integer[]::new),model.toArray(Integer[]::new)),"generator contents");
        equal(queue.contains(null),false,"null absent");
        equal(queue.remove(null),false,"null not removed");
        equal(queue.toString(),model.toString(),"FIFO toString");
        equal(queue.hashCode(),System.identityHashCode(queue),"identity hash");
        check(queue.equals(queue),"self identity");
    }
    private static void boundaries() throws Exception {
        for(int invalid:new int[]{-1,Integer.MIN_VALUE}) {
            Outcome failure=capture(()->create(invalid));
            equal(failure.failure,IllegalArgumentException.class.getName(),"negative capacity type");
            selected(Objects.equals(failure.message,"maxSize ("+invalid+") must >= 0"),"negative capacity message");
            observation("negative/"+invalid,render(failure));
        }
        for(int capacity:new int[]{0,1,2,3,5,17,32}) {
            Queue<Integer> queue=create(capacity);List<Integer> model=new ArrayList<>();
            Outcome remove=capture(queue::remove),element=capture(queue::element);
            equal(remove.failure,NoSuchElementException.class.getName(),"empty remove type");
            equal(element.failure,NoSuchElementException.class.getName(),"empty element type");
            selected(remove.message==null&&element.message==null,"empty queue messages");
            policyTrace("empty/remove",render(remove),queue);policyTrace("empty/element",render(element),queue);
            equal(queue.poll(),null,"empty poll");equal(queue.peek(),null,"empty peek");
            for(int i=0;i<3*capacity+3;i++){equal(queue.add(i),true,"add true");append(model,capacity,i);}
            match(queue,model,capacity);
            Outcome nullAdd=capture(()->queue.add(null));
            Outcome nullOffer=capture(()->queue.offer(null));
            equal(nullAdd.failure,NullPointerException.class.getName(),"null add type");
            equal(nullOffer.failure,NullPointerException.class.getName(),"null offer type");
            selected(nullAdd.message==null&&nullOffer.message==null,"eager null messages");
            match(queue,model,capacity);
            policyTrace("full-null-add/"+capacity,render(nullAdd),queue);
            policyTrace("full-null-offer/"+capacity,render(nullOffer),queue);
            check(!queue.equals(create(capacity)),"queue identity equality");
            int identity=queue.hashCode();queue.clear();model.clear();
            equal(queue.hashCode(),identity,"hash stable through clear");
            match(queue,model,capacity);trace("boundary/"+capacity,null,queue);
        }
    }
    private static void randomized() throws Exception {
        Random random=new Random(0x47334556494354L);
        for(int capacity:new int[]{0,1,2,3,5,8,17,32}) {
            Queue<Integer> queue=create(capacity);List<Integer> model=new ArrayList<>();
            for(int step=0;step<4000;step++) {
                int action=random.nextInt(12),value=random.nextInt(31)-15;Object result;
                switch(action) {
                    case 0,1->{result=action==0?queue.add(value):queue.offer(value);append(model,capacity,value);equal(result,true,"append result");}
                    case 2->{result=queue.poll();equal(result,model.isEmpty()?null:model.removeFirst(),"poll");}
                    case 3->{result=queue.remove(value);equal(result,model.remove(Integer.valueOf(value)),"remove first object");}
                    case 4->{result=queue.contains(value);equal(result,model.contains(value),"contains");}
                    case 5->{queue.clear();model.clear();result=null;}
                    case 6->{result=queue.removeIf(element->element<0);equal(result,model.removeIf(element->element<0),"default removeIf");}
                    case 7->{
                        List<Integer> input=new ArrayList<>();
                        for(int i=0,n=random.nextInt(capacity+5);i<n;i++)input.add(value+i);
                        result=queue.addAll(input);equal(result,bulkModel(model,capacity,input),"addAll");
                    }
                    case 8->{List<Integer> keep=Arrays.asList(value,value+1);result=queue.retainAll(keep);equal(result,model.retainAll(keep),"retainAll");}
                    case 9->{List<Integer> erase=Arrays.asList(value,value+1);result=queue.removeAll(erase);equal(result,model.removeAll(erase),"removeAll");}
                    case 10->{List<Integer> input=Arrays.asList(value,value+1);result=queue.containsAll(input);equal(result,model.containsAll(input),"containsAll");}
                    default->{
                        if(model.isEmpty()){
                            Outcome failure=capture(queue::remove);equal(failure.failure,NoSuchElementException.class.getName(),"random empty remove");result=render(failure);
                        }else{result=queue.remove();equal(result,model.removeFirst(),"remove head");}
                    }
                }
                match(queue,model,capacity);trace("random/"+capacity+"/"+step+"/"+action,result,queue);
            }
        }
    }
    private static void legalCursors() throws Exception {
        Random random=new Random(0x435552534f5247L);
        for(int trial=0;trial<1000;trial++) {
            int capacity=random.nextInt(18);Queue<Integer> queue=create(capacity);
            List<Integer> model=new ArrayList<>();
            for(int i=0,n=random.nextInt(3*capacity+5);i<n;i++){queue.add(i);append(model,capacity,i);}
            Iterator<Integer> actual=queue.iterator(),expected=model.iterator();
            for(int step=0;step<60;step++) {
                int action=random.nextInt(3);Outcome left=cursor(actual,action),right=cursor(expected,action);
                equal(left.value,right.value,"legal cursor value");
                equal(left.failure,right.failure,"legal cursor exception type");
                equal(snapshot(queue),model,"legal cursor owner");
                trace("cursor/"+trial+"/"+step+"/"+action,render(left),queue);
            }
        }
        for(int capacity:new int[]{0,1,3,17}) {
            Queue<Integer> queue=create(capacity);List<Integer> model=new ArrayList<>();
            for(int i=0;i<3*capacity+2;i++){queue.add(i);append(model,capacity,i);}
            Iterator<Integer> actual=queue.iterator(),expected=model.iterator();
            List<Integer> delivered=new ArrayList<>(),reference=new ArrayList<>();
            actual.forEachRemaining(delivered::add);expected.forEachRemaining(reference::add);
            equal(delivered,reference,"forEachRemaining values");
            if(!model.isEmpty()){
                actual.remove();expected.remove();equal(snapshot(queue),model,"remove after successful forEachRemaining");
            }
            trace("legal/forEachRemaining/"+capacity,delivered,queue);
        }
    }
    private static final class SuffixOnlyList extends AbstractList<Integer> {
        private final List<Integer> values;
        private final int firstReadable;
        private final List<String> calls;
        private SuffixOnlyList(List<Integer> values,int firstReadable,List<String> calls) {
            this.values=values;this.firstReadable=firstReadable;this.calls=calls;
        }
        @Override public int size(){calls.add("size");return values.size();}
        @Override public Integer get(int index){
            calls.add("get/"+index);
            if(index<firstReadable)throw new AssertionError("discarded List prefix was read");
            return values.get(index);
        }
    }
    private static final class ObservedCollection extends AbstractCollection<Integer> {
        private final List<Integer> values;
        private final int reportedSize;
        private final List<String> calls;
        private ObservedCollection(List<Integer> values,int reportedSize,List<String> calls) {
            this.values=values;this.reportedSize=reportedSize;this.calls=calls;
        }
        @Override public int size(){calls.add("size/"+reportedSize);return reportedSize;}
        @Override public Iterator<Integer> iterator(){
            calls.add("iterator");Iterator<Integer> source=values.iterator();
            return new Iterator<>() {
                @Override public boolean hasNext(){boolean value=source.hasNext();calls.add("hasNext/"+value);return value;}
                @Override public Integer next(){Integer value=source.next();calls.add("next/"+value);return value;}
                @Override public void remove(){source.remove();}
            };
        }
    }
    private static void bulkPolicy() throws Exception {
        Queue<Integer> queue=create(3);queue.add(99);
        List<String> calls=new ArrayList<>();
        List<Integer> values=Arrays.asList(null,1,2,3,4);
        boolean added=queue.addAll(new SuffixOnlyList(values,2,calls));
        selected(added&&snapshot(queue).equals(List.of(2,3,4)),"large List skips inaccessible/null prefix");
        selected(calls.stream().noneMatch(x->x.equals("get/0")||x.equals("get/1")),"no prefix get");
        policyTrace("List/suffix",calls,queue);
        queue.clear();calls.clear();queue.add(99);
        added=queue.addAll(new ObservedCollection(values,5,calls));
        selected(added&&snapshot(queue).equals(List.of(2,3,4)),"nonList suffix content");
        selected(calls.contains("next/null")&&calls.contains("next/1"),"nonList skipped prefix consumed");
        policyTrace("nonList/suffix",calls,queue);
        queue.clear();queue.add(99);
        Queue<Integer> poisonQueue=queue;
        Outcome poisoned=capture(()->poisonQueue.addAll(Arrays.asList(null,1,2,null,4)));
        selected(Objects.equals(poisoned.failure,NullPointerException.class.getName())&&poisoned.message==null,"null suffix rejection");
        selected(snapshot(queue).equals(List.of(2)),"clear and committed suffix prefix before null");
        policyTrace("List/null-suffix",render(poisoned),queue);
        for(int capacity:new int[]{0,1,3,5}) {
            queue=create(capacity);
            for(int i=0;i<capacity;i++)queue.add(i);
            Queue<Integer> full=queue;
            equal(full.addAll(full),false,"full self addAll result");
            equal(snapshot(full),List.of(),"full self addAll clears first");
            policyTrace("self/full/"+capacity,null,full);
            full.addAll(List.of(1,2,3,4,5));
            Collection<Integer> alias=Collections.unmodifiableCollection(full);
            equal(full.addAll(alias),false,"full backed alias result");
            equal(snapshot(full),List.of(),"full backed alias cleared");
            policyTrace("self/alias/"+capacity,null,full);
        }
        Queue<Integer> partial=create(5);partial.addAll(List.of(1,2));
        equal(partial.addAll(partial),true,"partial self result");
        equal(snapshot(partial),List.of(1,2,1,2),"partial self contents");
        policyTrace("self/partial",null,partial);
        Queue<Integer> zero=create(0);
        equal(zero.addAll(List.of()),false,"zero empty bulk false");
        equal(zero.addAll(Arrays.asList(null,1,null)),false,"zero skips all List members");
        equal(snapshot(zero),List.of(),"zero empty");
        policyTrace("zero/skipped-nulls",null,zero);
        Queue<Integer> absent=create(3);
        policyTrace("null/addAll",render(capture(()->absent.addAll(null))),absent);
        for(int reported:new int[]{0,2,10}) {
            Queue<Integer> weird=create(3);weird.add(99);
            List<String> effects=new ArrayList<>();
            ObservedCollection source=new ObservedCollection(Arrays.asList(1,2,3,4,5),reported,effects);
            Outcome result=capture(()->weird.addAll(source));
            observation("size-mismatch/"+reported,render(result)+"/"+effects+"/"+snapshot(weird));
        }
    }
    private static final class Membership extends AbstractCollection<Object> {
        private final Queue<Integer> ownerQueue;
        private final int throwAt;
        private final List<String> calls;
        private int count;
        private Membership(Queue<Integer> queue,int throwAt,List<String> calls) {
            this.ownerQueue=queue;this.throwAt=throwAt;this.calls=calls;
        }
        @Override public int size(){return 0;}
        @Override public Iterator<Object> iterator(){return Collections.emptyIterator();}
        @Override public boolean contains(Object value){
            calls.add(value+"/"+snapshot(ownerQueue));
            if(++count==throwAt)throw new IllegalStateException("membership-stop");
            return ((Integer)value)%2==0;
        }
    }
    private static void callbackPolicies() throws Exception {
        for(boolean retain:List.of(false,true))for(int stop:new int[]{0,4}) {
            Queue<Integer> queue=create(7);queue.addAll(List.of(1,2,3,4,5));
            List<Integer> original=snapshot(queue);
            List<String> calls=new ArrayList<>();
            Membership membership=new Membership(queue,stop,calls);
            Outcome result=capture(()->retain?queue.retainAll(membership):queue.removeAll(membership));
            List<Integer> expected=new ArrayList<>(original);
            if(stop==0)expected.removeIf(value->retain?value%2!=0:value%2==0);
            else selected(Objects.equals(result.failure,IllegalStateException.class.getName())&&Objects.equals(result.message,"membership-stop"),"membership stop");
            selected(snapshot(queue).equals(expected),"deferred bulk result/failure");
            for(String call:calls)selected(call.endsWith("/"+original),"membership reads original before expunge");
            policyTrace("membership/"+retain+"/"+stop,render(result)+"/"+calls,queue);
        }
        for(int stop:new int[]{0,4}) {
            Queue<Integer> queue=create(7);queue.addAll(List.of(1,2,3,4,5));
            List<String> calls=new ArrayList<>();int[] counter={0};
            Predicate<Integer> predicate=value->{
                calls.add(value+"/"+snapshot(queue));
                if(++counter[0]==stop)throw new IllegalStateException("predicate-stop");
                return value%2==0;
            };
            Outcome result=capture(()->queue.removeIf(predicate));
            List<Integer> expected=stop==0?List.of(1,3,5):List.of(1,3,4,5);
            selected(snapshot(queue).equals(expected),"immediate default removeIf effects");
            selected(calls.get(2).equals("3/[1, 3, 4, 5]"),"predicate sees removed prefix");
            policyTrace("predicate/"+stop,render(result)+"/"+calls,queue);
        }
        Queue<Integer> empty=create(0);
        for(String method:List.of("removeAll","retainAll","containsAll","removeIf","forEach")) {
            Outcome value=capture(()->{
                switch(method){
                    case "removeAll"->{return empty.removeAll(null);}
                    case "retainAll"->{return empty.retainAll(null);}
                    case "containsAll"->{return empty.containsAll(null);}
                    case "removeIf"->{return empty.removeIf(null);}
                    default->{empty.forEach(null);return "DONE";}
                }
            });
            equal(value.failure,NullPointerException.class.getName(),"null "+method);
            policyTrace("null/"+method,render(value),empty);
        }
        Queue<Integer> queue=create(8);queue.addAll(List.of(1,2,3));
        List<Integer> delivered=new ArrayList<>();
        queue.forEach(value->{delivered.add(value);if(value==1)queue.add(4);});
        selected(delivered.equals(List.of(1,2,3))&&snapshot(queue).equals(List.of(1,2,3,4)),"Iterable default not deque bulk forEach");
        policyTrace("default/forEach-mutation",delivered,queue);
    }
    private static void arraysStreamsAndIdentity() throws Exception {
        for(int capacity:new int[]{0,1,3,17}) {
            Queue<Integer> queue=create(capacity);List<Integer> model=new ArrayList<>();
            for(int i=0;i<3*capacity+1;i++){queue.add(i);append(model,capacity,i);}
            List<Integer> args=new ArrayList<>();
            IntFunction<Integer[]> generator=size->{args.add(size);return new Integer[size];};
            equal(Arrays.asList(queue.toArray(generator)),model,"generator array");
            equal(args,List.of(0),"generator argument zero");
            Integer[] undersized=queue.toArray(new Integer[0]);
            check(Arrays.equals(undersized,model.toArray(Integer[]::new)),"undersized typed result");
            Object[] detached=queue.toArray();if(detached.length>0)detached[0]=-1000;
            match(queue,model,capacity);
            List<Integer> delivered=new ArrayList<>();queue.forEach(delivered::add);
            equal(delivered,model,"forEach ordered delivery");
            Spliterator<Integer> spliterator=queue.spliterator();
            equal(spliterator.characteristics(),Spliterator.SIZED|Spliterator.SUBSIZED,"Collection default characteristics");
            equal(spliterator.getExactSizeIfKnown(),(long)model.size(),"spliterator exact size");
            List<Integer> splitResult=new ArrayList<>();Spliterator<Integer> split=spliterator.trySplit();
            if(split!=null)split.forEachRemaining(splitResult::add);
            spliterator.forEachRemaining(splitResult::add);equal(splitResult,model,"split traversal");
            equal(queue.stream().isParallel(),false,"sequential stream");
            equal(queue.parallelStream().isParallel(),true,"parallel stream");
            equal(queue.stream().toList(),model,"sequential stream contents");
            List<Integer> parallel=new ArrayList<>(queue.parallelStream().toList());
            List<Integer> sorted=new ArrayList<>(model);Collections.sort(parallel);Collections.sort(sorted);
            equal(parallel,sorted,"parallel stream content multiset");
            trace("array-streams/"+capacity,args,queue);
            Outcome nullArray=capture(()->queue.toArray((Integer[])null));
            Outcome nullGenerator=capture(()->queue.toArray((IntFunction<Integer[]>)null));
            equal(nullArray.failure,NullPointerException.class.getName(),"null typed array");
            equal(nullGenerator.failure,NullPointerException.class.getName(),"null array generator");
            policyTrace("null/typed-array/"+capacity,render(nullArray),queue);
            policyTrace("null/generator/"+capacity,render(nullGenerator),queue);
        }
        Queue<Object> mixed=create(5);mixed.add("one");mixed.add(2);mixed.add("three");
        String[] target={"KEEP","KEEP","KEEP","KEEP"};
        Outcome wrong=capture(()->Arrays.asList(mixed.toArray(target)));
        equal(wrong.failure,ArrayStoreException.class.getName(),"typed ArrayStoreException");
        observation("wrong-array/partial",render(wrong)+"/"+Arrays.asList(target)+"/"+Arrays.asList(mixed.toArray()));
        Queue<Integer> late=create(5);late.addAll(List.of(1,2));
        Spliterator<Integer> lazy=late.spliterator();late.add(3);
        equal(lazy.estimateSize(),3L,"late binding size");
        List<Integer> values=new ArrayList<>();lazy.forEachRemaining(values::add);
        equal(values,List.of(1,2,3),"late bound values");trace("late-spliterator",values,late);
    }
    @SuppressWarnings("unchecked")
    private static <E> Queue<E> roundtrip(Queue<E> queue) throws Exception {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(ObjectOutputStream output=new ObjectOutputStream(bytes)){output.writeObject(queue);}
        try(ObjectInputStream input=new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))){
            return (Queue<E>)input.readObject();
        }
    }
    private static void serialization() throws Exception {
        equal(ObjectStreamClass.lookup(owner).getSerialVersionUID(),0L,"Guava owner UID0");
        for(int capacity:new int[]{0,1,2,3,5,17,32})for(int supplied:new int[]{0,1,3,17,51}) {
            Queue<Integer> queue=create(capacity);List<Integer> model=new ArrayList<>();
            for(int i=0;i<supplied;i++){queue.add(i);append(model,capacity,i);}
            Queue<Integer> copy=roundtrip(queue);match(copy,model,capacity);
            List<Integer> detached=new ArrayList<>(model);
            for(int i=0;i<2*capacity+3;i++){copy.add(100+i);append(model,capacity,100+i);match(copy,model,capacity);}
            equal(snapshot(queue),detached,"serialized owner detached");trace("serde/"+capacity+"/"+supplied,null,copy);
        }
        Queue<Object> graph=create(4);List<String> shared=new ArrayList<>(List.of("shared"));
        graph.add(shared);graph.add(shared);Queue<Object> graphCopy=roundtrip(graph);
        Object[] values=graphCopy.toArray();
        check(values[0]==values[1],"shared reference graph alias retained");
        check(values[0]!=shared,"graph detached from original element");
        observation("serde/shared-alias",values[0]==values[1]);
        Queue<Object> self=create(2);self.add(self);Queue<Object> selfCopy=roundtrip(self);
        check(selfCopy.peek()==selfCopy,"serialized owner self-cycle retained");
        check(selfCopy!=self,"self owner detached");
        observation("serde/self-alias",selfCopy.peek()==selfCopy);
    }
    private static void selectedCursorObservers() throws Exception {
        for(boolean firstNext:List.of(false,true))for(boolean throwing:List.of(false,true)) {
            Queue<Integer> queue=create(8);queue.addAll(List.of(1,2,3));
            Iterator<Integer> iterator=queue.iterator();
            Object initial=firstNext?render(cursor(iterator,1)):"NO_NEXT";
            List<Integer> calls=new ArrayList<>();
            Outcome bulk=capture(()->{
                iterator.forEachRemaining(value->{calls.add(value);if(throwing&&value==2)throw new IllegalStateException("consumer-stop");});
                return "DONE";
            });
            Outcome has=cursor(iterator,0),next=cursor(iterator,1),remove=cursor(iterator,2);
            observation("cursor/bulk/"+firstNext+"/"+throwing,initial+"/"+calls+"/"+render(bulk)+"/"+render(has)+"/"+render(next)+"/"+render(remove)+"/"+snapshot(queue));
        }
        for(boolean prior:List.of(false,true)) {
            Queue<Integer> queue=create(8);queue.addAll(List.of(1,2,3));
            Iterator<Integer> iterator=queue.iterator();if(prior)iterator.next();
            List<Integer> calls=new ArrayList<>();
            Outcome bulk=capture(()->{iterator.forEachRemaining(value->{calls.add(value);if(value==2)queue.add(4);});return "DONE";});
            observation("cursor/bulk-tail-change/"+prior,calls+"/"+render(bulk)+"/"+render(cursor(iterator,0))+"/"+render(cursor(iterator,2))+"/"+snapshot(queue));
        }
        Queue<Integer> zero=create(0);Iterator<Integer> empty=zero.iterator();
        observation("cursor/null-consumer",render(capture(()->{empty.forEachRemaining(null);return "DONE";})));
        for(int capacity:new int[]{3,5,17,32})for(int supplied:new int[]{0,1,2}) {
            Queue<Integer> queue=create(capacity);for(int i=0;i<supplied;i++)queue.add(i);
            Queue<Integer> copy=roundtrip(queue);Iterator<Integer> iterator=copy.iterator();
            List<String> result=new ArrayList<>();
            for(int i=0;i<capacity+2;i++){
                copy.add(100+i);result.add("add/"+i+"/"+snapshot(copy));
                result.add("next/"+render(cursor(iterator,1)));
            }
            observation("cursor/sparse-serde-growth/"+capacity+"/"+supplied,result);
        }
        Queue<Object> self=create(2);self.add(self);
        Outcome text=capture(self::toString);
        observation("toString/owner-self",render(text));
        // Reentrant toString reads are separately observable without relying on object identity text.
        Queue<Integer> queue=create(3);queue.addAll(List.of(1,2));
        Queue<Integer> peer=create(3);peer.addAll(List.of(1,2));
        observation("identity/equal-content",queue.equals(peer)+"/"+(queue.hashCode()==System.identityHashCode(queue)));
    }
    private static void externalMutationObservers() throws Exception {
        Random random=new Random(0x4755415641455854L);
        for(int capacity:new int[]{0,1,2,3,5,7})for(int trial=0;trial<100;trial++) {
            Queue<Integer> queue=create(capacity);for(int i=0;i<3*capacity;i++)queue.add(i);
            Iterator<Integer> iterator=queue.iterator();
            for(int step=0;step<50;step++) {
                int action=random.nextInt(8);Outcome result;
                switch(action) {
                    case 0,1,2->result=cursor(iterator,action);
                    case 3->{int value=random.nextInt(20);result=capture(()->queue.add(value));}
                    case 4->result=capture(queue::poll);
                    case 5->result=capture(()->{queue.clear();return "CLEARED";});
                    case 6->result=capture(()->queue.addAll(Arrays.asList(11,null,12)));
                    default->{iterator=queue.iterator();result=new Outcome("NEW_ITERATOR",null,null);}
                }
                observation("mutation/"+capacity+"/"+trial+"/"+step+"/"+action,render(result)+"/"+snapshot(queue)+"/"+queue.size());
            }
        }
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=1)throw new IllegalArgumentException("implementation class with static create(int)");
        owner=Class.forName(args[0]);factory=owner.getMethod("create",int.class);remaining=owner.getMethod("remainingCapacity");
        protocol=MessageDigest.getInstance("SHA-256");policy=MessageDigest.getInstance("SHA-256");observed=MessageDigest.getInstance("SHA-256");
        boundaries();randomized();legalCursors();bulkPolicy();callbackPolicies();arraysStreamsAndIdentity();serialization();selectedCursorObservers();externalMutationObservers();
        System.out.println("{\"status\":\"PASS\",\"mandatoryChecks\":"+checks+",\"mandatoryOperations\":"+operations
            +",\"mandatorySHA256\":\""+HexFormat.of().formatHex(protocol.digest())+"\",\"selectedPolicyChecks\":"+policyChecks
            +",\"selectedPolicyOperations\":"+policyOperations+",\"selectedPolicySHA256\":\""+HexFormat.of().formatHex(policy.digest())
            +"\",\"providerSpecificObservations\":"+observations+",\"providerSpecificSHA256\":\""+HexFormat.of().formatHex(observed.digest())
            +"\",\"scope\":\"one full queue public protocol; concrete observers are separate from common Collection laws; owned namespace/serialization identity declared; no native queue store claim\"}");
    }
}
