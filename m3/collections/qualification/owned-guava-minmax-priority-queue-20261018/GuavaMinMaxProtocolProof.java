/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
 * Frozen public protocol: no heap algorithm, private field, or candidate source access.
 * Occurrences use identity scans of a plain list. Concrete rows are separate laws.
 */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
import java.util.Spliterator;
import java.util.function.IntFunction;

public final class GuavaMinMaxProtocolProof {
    private static Class<?> owner;
    private static Method createEmpty, createContents, orderedBy, expectedSize, maximumSize;
    private static Method firstPoll, firstPeek, firstRemove, lastPoll, lastPeek, lastRemove, comparator;
    private static final MessageDigest[] DIGESTS=new MessageDigest[4];
    private static final long[] ROWS=new long[4];
    private static long checks, operations;
    private static final String[] PARTITIONS={"MANDATORY_LAW","REQUIRED_CONCRETE","IDENTITY_OR_IMPLEMENTATION","UNDEFINED_REENTRY"};
    private GuavaMinMaxProtocolProof() { }
    @FunctionalInterface private interface Op { Object run() throws Throwable; }
    private record Outcome(Object result,String exception,String message) { }
    static final class Token implements Comparable<Token>, Serializable {
        private static final long serialVersionUID=1L;
        final String id;
        int rank;
        private Token(String id,int rank){this.id=id;this.rank=rank;}
        @Override public int compareTo(Token other){return Integer.compare(rank,other.rank);}
        @Override public String toString(){return id+"@"+rank;}
    }
    private static final Comparator<Token> RANK=(a,b)->Integer.compare(a.rank,b.rank);
    private enum Five { A,B,C,D,E }
    private static final class EqualToken implements Comparable<EqualToken> {
        final String id;int rank;
        EqualToken(String id,int rank){this.id=id;this.rank=rank;}
        @Override public int compareTo(EqualToken other){return Integer.compare(rank,other.rank);}
        @Override public boolean equals(Object other){return other instanceof EqualToken token&&token.rank==rank;}
        @Override public int hashCode(){return rank;}
        @Override public String toString(){return id+"@"+rank;}
    }
    private static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    private static void equal(Object actual,Object expected,String label){check(Objects.equals(actual,expected),label+": "+actual+" != "+expected);}
    static Object call(Method method,Object target,Object... arguments) throws Throwable {
        try{return method.invoke(target,arguments);}
        catch(InvocationTargetException wrapped){throw wrapped.getCause();}
    }
    private static Outcome capture(Op action){
        try{return new Outcome(action.run(),null,null);}
        catch(Throwable failure){if(failure instanceof AssertionError error)throw error;
            return new Outcome(null,failure.getClass().getName(),failure.getMessage());}
    }
    private static String format(Object value){
        if(value==null)return "null";
        if(value instanceof Outcome result)return result.exception==null?"VALUE/"+format(result.result):"EXCEPTION/"+result.exception+"/"+message(result.message);
        if(value instanceof Object[] array)return Arrays.toString(array);
        return String.valueOf(value);
    }
    private static String message(String value){return value==null?"null":value.replace(owner.getName(),"<OWNER>").replace('\n',' ').replace('\r',' ');}
    static void row(int partition,String label,Object value){
        String line=PARTITIONS[partition]+"\t"+label+"\t"+format(value);
        DIGESTS[partition].update((line+"\n").getBytes(StandardCharsets.UTF_8));ROWS[partition]++;System.out.println(line);
    }
    private static void concrete(String label,Object value,Queue<?> queue){row(1,label,format(value)+"|size="+queue.size()+"|array="+Arrays.toString(queue.toArray()));}
    @SuppressWarnings("unchecked") private static <T> Queue<T> create() throws Throwable{return (Queue<T>)call(createEmpty,null);}
    @SuppressWarnings("unchecked") private static <T> Queue<T> create(Iterable<T> contents) throws Throwable{return (Queue<T>)call(createContents,null,contents);}
    private static Object builder(Comparator<?> order) throws Throwable{return call(orderedBy,null,order);}
    private static Object option(Object builder,String method,int value) throws Throwable{return call(builder.getClass().getMethod(method,int.class),builder,value);}
    @SuppressWarnings("unchecked") private static <T> Queue<T> build(Object builder) throws Throwable{return (Queue<T>)call(builder.getClass().getMethod("create"),builder);}
    @SuppressWarnings("unchecked") private static <T> Queue<T> build(Object builder,Iterable<T> contents) throws Throwable{return (Queue<T>)call(builder.getClass().getMethod("create",Iterable.class),builder,contents);}
    private static Queue<Token> queue(Comparator<Token> order,int maximum,List<Token> contents) throws Throwable{
        Object builder=builder(order);option(builder,"maximumSize",maximum);return build(builder,contents);
    }
    private static Token token(Object value){return (Token)value;}
    private static Token min(Queue<Token> queue) throws Throwable{return token(call(firstPeek,queue));}
    private static Token max(Queue<Token> queue) throws Throwable{return token(call(lastPeek,queue));}
    private static int identityIndex(List<?> values,Object target){for(int i=0;i<values.size();i++)if(values.get(i)==target)return i;return -1;}
    private static void removeIdentity(List<?> values,Object target,String label){int index=identityIndex(values,target);check(index>=0,label);values.remove(index);}
    private static void occurrences(Object[] actual,List<?> model,String label){
        equal(actual.length,model.size(),label+" size");List<Object> remaining=new ArrayList<>(model);
        for(Object value:actual)removeIdentity(remaining,value,label+" occurrence "+value);
        check(remaining.isEmpty(),label+" exhausted");
    }
    private static List<Token> image(Queue<Token> queue){List<Token> values=new ArrayList<>();for(Object value:queue.toArray())values.add(token(value));return values;}
    private static String ranks(List<Token> values){List<Integer> ranks=new ArrayList<>();for(Token value:values)ranks.add(value.rank);Collections.sort(ranks);return ranks.toString();}
    private static void match(Queue<Token> queue,List<Token> model,Comparator<Token> order) throws Throwable{
        equal(queue.size(),model.size(),"logical size");equal(queue.isEmpty(),model.isEmpty(),"isEmpty");occurrences(queue.toArray(),model,"untyped model");
        List<Token> traversed=new ArrayList<>();queue.forEach(traversed::add);occurrences(traversed.toArray(),model,"forEach model");
        Token low=min(queue),high=max(queue);equal(queue.peek(),low,"peek alias");
        if(model.isEmpty()){equal(low,null,"empty min");equal(high,null,"empty max");}
        else{check(identityIndex(model,low)>=0&&identityIndex(model,high)>=0,"ends are existing occurrences");for(Token value:model){check(order.compare(low,value)<=0,"minimum law");check(order.compare(high,value)>=0,"maximum law");}}
        operations++;
    }
    private static void boundedOffer(Queue<Token> queue,List<Token> model,Token value,int cap,Comparator<Token> order,boolean useAdd) throws Throwable{
        Object result=useAdd?queue.add(value):queue.offer(value);model.add(value);
        if(model.size()>cap){
            List<Token> difference=new ArrayList<>(model);for(Object retained:queue.toArray())removeIdentity(difference,retained,"bounded retained");equal(difference.size(),1,"one evicted occurrence");
            Token lost=difference.getFirst();for(Token candidate:model)check(order.compare(candidate,lost)<=0,"smallest-k retention");removeIdentity(model,lost,"evict identity");
        }
        if(useAdd)equal(result,true,"add always true");match(queue,model,order);
        row(0,"offer-model/"+cap+"/"+useAdd,ranks(model));concrete("offer/"+cap+"/"+value.id+"/"+useAdd,result,queue);
    }
    private static List<Token> data(int size,String prefix){List<Token> values=new ArrayList<>();for(int i=0;i<size;i++)values.add(new Token(prefix+i,(i*37)%41-20));return values;}
    private static void factoriesAndBuilders() throws Throwable{
        Queue<Token> empty=create();equal(empty.size(),0,"create()");concrete("factory/create",null,empty);
        for(int option:new int[]{-1,0,1,2,3,11,63,64,65,127}){
            Outcome result=capture(()->call(expectedSize,null,option));
            if(option<0){equal(result.exception,IllegalArgumentException.class.getName(),"negative expectedSize");equal(result.message,null,"expectedSize null IAE message");row(1,"factory/expectedSize/"+option,result);}
            else{Object b=result.result;check(b!=null,"valid expected builder");Queue<Token> q=build(b);q.offer(new Token("e",1));concrete("factory/expectedSize/"+option,"BUILDER",q);}
        }
        for(int option:new int[]{-1,0,1,2,3,17,Integer.MAX_VALUE}){
            Outcome result=capture(()->call(maximumSize,null,option));
            if(option<=0){equal(result.exception,IllegalArgumentException.class.getName(),"invalid maximumSize");equal(result.message,null,"maximumSize null IAE message");row(1,"factory/maximumSize/"+option,result);}
            else{Queue<Token> q=build(result.result);concrete("factory/maximumSize/"+option,"BUILDER",q);}
        }
        row(1,"factory/orderedBy-null",capture(()->builder(null)));
        for(boolean reverse:List.of(false,true)){
            Object b=builder(RANK);Object first=option(b,reverse?"maximumSize":"expectedSize",reverse?1:Integer.MAX_VALUE);
            check(first==b,"setter returns builder");Object second=option(b,reverse?"expectedSize":"maximumSize",reverse?Integer.MAX_VALUE:1);check(second==b,"setter returns builder second");
            Queue<Token> q=build(b,data(8,"cap"));equal(q.size(),1,"huge size cap");concrete("builder/huge-capped/"+reverse,null,q);
        }
        Object b=builder(RANK);check(option(b,"expectedSize",2)==b,"expected setter identity");check(option(b,"maximumSize",2)==b,"maximum setter identity");
        Queue<Token> q1=build(b,data(7,"reuse"));row(1,"builder/expected-invalid",capture(()->option(b,"expectedSize",-1)));row(1,"builder/maximum-invalid",capture(()->option(b,"maximumSize",0)));
        Queue<Token> q2=build(b,data(7,"reuse2"));equal(q1.size(),2,"failed setter doesn't modify old queue");equal(q2.size(),2,"failed setter validation before assignment");option(b,"maximumSize",3);option(b,"expectedSize",0);Queue<Token> q3=build(b,data(7,"reuse3"));equal(q3.size(),3,"later builder policy");q2.clear();equal(q1.size(),2,"independent builder stores");equal(q3.size(),3,"independent builder stores third");
        concrete("builder/reuse-one",null,q1);concrete("builder/reuse-two",null,q2);concrete("builder/reuse-three",null,q3);
        for(int n:new int[]{0,1,2,3,17,65}){
            List<Token> values=data(n,"init");Queue<Token> natural=create(values);List<Token> model=new ArrayList<>(values);match(natural,model,RANK);concrete("factory/iterable/"+n,null,natural);
            List<Token> descending=new ArrayList<>(values);descending.sort(RANK.reversed());Queue<Token> sorted=create(descending);match(sorted,model,RANK);concrete("factory/descending/"+n,null,sorted);
            List<Token> ascending=new ArrayList<>(values);ascending.sort(RANK);Queue<Token> ordered=create(ascending);match(ordered,model,RANK);concrete("factory/ascending/"+n,null,ordered);
            Collections.shuffle(values,new Random(0x53485546464c45L+n));Queue<Token> shuffled=create(values);match(shuffled,model,RANK);concrete("factory/shuffled/"+n,null,shuffled);
        }
        for(boolean collection:List.of(false,true))for(int report:new int[]{0,2,10})for(int stop:new int[]{0,3}){
            List<String> calls=new ArrayList<>();List<Token> values=data(5,"observed");Iterable<Token> input=collection?new ObservedCollection<>(values,report,stop,calls):new ObservedIterable<>(values,stop,calls);
            Outcome result=capture(()->create(input));row(1,"constructor/iterable/"+collection+"/"+report+"/"+stop,format(result.exception==null?"CREATED/"+Arrays.toString(((Queue<?>)result.result).toArray()):result)+"|calls="+calls);
        }
        List<Token> nullPrefix=new ArrayList<>(data(2,"nullprefix"));nullPrefix.add(null);nullPrefix.add(new Token("after",0));row(1,"constructor/null-prefix",capture(()->create(nullPrefix)));
        row(1,"constructor/null-iterable",capture(()->create((Iterable<Token>)null)));
    }
    private static class ObservedIterable<T> implements Iterable<T> {
        final List<T> values;final int stop;final List<String> calls;
        ObservedIterable(List<T> values,int stop,List<String> calls){this.values=values;this.stop=stop;this.calls=calls;}
        @Override public Iterator<T> iterator(){calls.add("iterator");return new Iterator<>(){int index;
            @Override public boolean hasNext(){boolean more=index<values.size();calls.add("hasNext/"+more);return more;}
            @Override public T next(){calls.add("next/"+index);if(stop>0&&index+1==stop)throw new IllegalStateException("read-stop/"+stop);return values.get(index++);}};}
    }
    private static final class ObservedCollection<T> extends AbstractCollection<T> {
        final List<T> values;final int reported,stop;final List<String> calls;
        ObservedCollection(List<T> values,int reported,int stop,List<String> calls){this.values=values;this.reported=reported;this.stop=stop;this.calls=calls;}
        @Override public int size(){calls.add("size/"+reported);return reported;}
        @Override public Iterator<T> iterator(){return new ObservedIterable<>(values,stop,calls).iterator();}
    }
    private static void dualEndedAndRandom() throws Throwable{
        for(int n:new int[]{0,1,2,3,4,5,8,17,31,32,63,64,65,73,127,257})for(int mode=0;mode<4;mode++){
            Comparator<Token> order=switch(mode){case 1->RANK.reversed();case 2->Comparator.comparingInt((Token x)->Math.floorDiv(x.rank,5));case 3->RANK.thenComparing(x->x.id);default->RANK;};
            List<Token> model=data(n,"t");if(n>=3){model.set(0,new Token("low",Integer.MIN_VALUE));model.set(1,new Token("high",Integer.MAX_VALUE));model.set(2,model.getLast());}
            Queue<Token> q=queue(order,Integer.MAX_VALUE,model);match(q,model,order);row(0,"initial/"+n+"/"+mode,ranks(model));concrete("initial-layout/"+n+"/"+mode,null,q);
            Random random=new Random(0x4d494e4d4158L+31L*n+mode);
            for(int step=0;step<160;step++){
                int action=random.nextInt(9);Object result;
                switch(action){
                    case 0,1->{Token value=new Token("r"+step,random.nextInt(21)-10);result=action==0?q.offer(value):q.add(value);model.add(value);equal(result,true,"unbounded insert true");}
                    case 2,3->{Token value=token(action==2?call(firstPoll,q):call(lastPoll,q));result=value;if(value!=null){for(Token candidate:model)check(action==2?order.compare(value,candidate)<=0:order.compare(value,candidate)>=0,"removed end extremum");removeIdentity(model,value,"pop occurrence");}else check(model.isEmpty(),"null pop only empty");}
                    case 4->{Token value=model.isEmpty()?new Token("absent",0):model.get(random.nextInt(model.size()));result=q.remove(value);if((Boolean)result)removeIdentity(model,value,"equals-remove identity");else check(model.isEmpty(),"present remove success");}
                    case 5->{Iterator<Token> it=q.iterator();if(it.hasNext()){Token value=it.next();it.remove();removeIdentity(model,value,"iterator remove occurrence");result=value;}else result="EMPTY";}
                    case 6->{if(step%19==0){q.clear();model.clear();result="CLEAR";}else result=q.contains(model.isEmpty()?null:model.getFirst());}
                    case 7->{result=q.peek();if(!model.isEmpty())check(identityIndex(model,result)>=0,"peek exists");}
                    default->{result=q.containsAll(model);equal(result,true,"containsAll model");}
                }
                match(q,model,order);row(0,"random/"+n+"/"+mode+"/"+step+"/"+action,ranks(model));concrete("random-detail/"+n+"/"+mode+"/"+step,result,q);
            }
            for(boolean last:List.of(false,true)){
                Queue<Token> drain=queue(order,Integer.MAX_VALUE,model);List<Token> remaining=new ArrayList<>(model);List<Integer> removed=new ArrayList<>();
                while(!remaining.isEmpty()){Token value=token(last?call(lastPoll,drain):call(firstPoll,drain));for(Token candidate:remaining)check(last?order.compare(value,candidate)>=0:order.compare(value,candidate)<=0,"drain extremum");removed.add(value.rank);removeIdentity(remaining,value,"drain occurrence");match(drain,remaining,order);}
                row(0,"drain/"+n+"/"+mode+"/"+last,removed);
            }
        }
        for(String method:new String[]{"removeFirst","removeLast","remove","element"}){
            Queue<Token> empty=create();Outcome outcome=capture(()->switch(method){case "removeFirst"->call(firstRemove,empty);case "removeLast"->call(lastRemove,empty);case "remove"->empty.remove();default->empty.element();});equal(outcome.exception,NoSuchElementException.class.getName(),"empty "+method);equal(outcome.message,null,"empty message");concrete("empty/"+method,outcome,empty);
            Queue<Token> populated=create(data(4,"aliases"));Object value=switch(method){case "removeFirst"->call(firstRemove,populated);case "removeLast"->call(lastRemove,populated);case "remove"->populated.remove();default->populated.element();};concrete("populated/"+method,value,populated);
        }
        Queue<Token> empty=create();row(0,"empty/poll-peek",List.of(format(empty.poll()),format(empty.peek()),format(call(firstPoll,empty)),format(call(lastPoll,empty)),format(call(firstPeek,empty)),format(call(lastPeek,empty))));
    }
    private static void boundedAndIdentity() throws Throwable{
        for(int cap:new int[]{1,2,3,17}){
            Queue<Token> q=queue(RANK,cap,List.of());List<Token> model=new ArrayList<>();Token shared=new Token("shared",7);
            List<Token> incoming=new ArrayList<>(data(cap+8,"b"));incoming.add(shared);incoming.add(new Token("equal-distinct",7));incoming.add(shared);incoming.add(new Token("better",Integer.MIN_VALUE));incoming.add(new Token("worse",Integer.MAX_VALUE));
            for(Token value:incoming)boundedOffer(q,model,value,cap,RANK,false);
            boundedOffer(q,model,shared,cap,RANK,true);equal(q.addAll(List.of()),false,"empty addAll false");
            boolean changed=q.addAll(List.of(new Token("all-worse1",Integer.MAX_VALUE),new Token("all-worse2",Integer.MAX_VALUE)));equal(changed,true,"yielded addAll true");concrete("bounded/no-retained-change/"+cap,changed,q);
            match(q,image(q),RANK);concrete("bounded/null/"+cap,capture(()->q.offer(null)),q);concrete("bounded/null-add/"+cap,capture(()->q.add(null)),q);
        }
        for(int n:new int[]{0,1,2,3,17}){Queue<Token> q=create(data(n,"identity"));Queue<Token> peer=create(data(n,"identity"));check(q.equals(q),"self equals");check(!q.equals(peer),"identity equals peer");check(!q.equals(null),"identity equals null");equal(q.hashCode(),System.identityHashCode(q),"identity hash");int hash=q.hashCode();q.clear();equal(hash,q.hashCode(),"stable hash");equal(q.getClass(),owner,"getClass");synchronized(q){q.notify();q.notifyAll();row(1,"Object/wait-negative",capture(()->{q.wait(-1);return "WAITED";}));row(1,"Object/wait-zero-invalid-nanos",capture(()->{q.wait(0,1000000);return "WAITED";}));}row(0,"Object/identity/"+n,"equals/hash/getClass/monitor");}
        Queue<Token> monitor=create();row(1,"Object/notify-outside",capture(()->{monitor.notify();return null;}));row(1,"Object/notifyAll-outside",capture(()->{monitor.notifyAll();return null;}));row(1,"Object/wait-outside",capture(()->{monitor.wait();return null;}));
    }
    private static Outcome cursor(Iterator<?> it,int action){return capture(()->{if(action==0)return it.hasNext();if(action==1)return it.next();it.remove();return "REMOVED";});}
    private static void legalIteratorSequences() throws Throwable{
        for(int n=0;n<=4;n++)for(int sequence=0;sequence<729;sequence++){
            List<Token> remaining=data(n,"iter");Queue<Token> q=create(remaining);Iterator<Token> it=q.iterator();List<Token> unseen=new ArrayList<>(remaining);Token last=null;boolean canRemove=false,exhaustedSinceNext=false;int digits=sequence;
            for(int step=0;step<6;step++){
                int action=digits%3;digits/=3;Outcome result=cursor(it,action);
                if(action==0){equal(result.exception,null,"hasNext legal");equal(result.result,!unseen.isEmpty(),"hasNext remaining");}
                else if(action==1){if(unseen.isEmpty()){equal(result.exception,NoSuchElementException.class.getName(),"next exhaustion");exhaustedSinceNext=true;/* movement-backed last state after exhaustion is concrete */}
                    else{equal(result.exception,null,"next legal");last=token(result.result);removeIdentity(unseen,last,"iterator delivered each occurrence once");canRemove=true;exhaustedSinceNext=false;}}
                else if(!canRemove){equal(result.exception,IllegalStateException.class.getName(),"remove without next");}
                else if(result.exception==null){removeIdentity(remaining,last,"iterator remove last occurrence");canRemove=false;}
                else{check(exhaustedSinceNext,"legal Iterator.remove must succeed after next absent exhaustion");equal(result.exception,IllegalStateException.class.getName(),"exhausted movement remove state");canRemove=false;}
                match(q,remaining,RANK);row(0,"cursor-model/"+n+"/"+sequence+"/"+step,ranks(remaining));concrete("cursor-detail/"+n+"/"+sequence+"/"+step+"/"+action,result,q);
            }
        }
        for(int n:new int[]{0,1,3,17})for(int prefix:new int[]{0,1,2})for(int stop:new int[]{0,2}){
            Queue<Token> q=create(data(n,"bulkiter"));Iterator<Token> it=q.iterator();List<String> calls=new ArrayList<>();for(int i=0;i<prefix&&it.hasNext();i++)calls.add("prefix/"+it.next());
            Outcome outcome=capture(()->{it.forEachRemaining(value->{calls.add("consumer/"+value);if(stop>0&&calls.size()==prefix+stop)throw new IllegalStateException("consumer-stop");});return "DONE";});
            concrete("iterator/forEachRemaining/"+n+"/"+prefix+"/"+stop,format(outcome)+"|calls="+calls+"|remove="+format(cursor(it,2)),q);
        }
        Queue<Token> q=create(data(7,"removeconsumer"));Iterator<Token> it=q.iterator();List<Token> original=image(q);List<Token> seen=new ArrayList<>();it.forEachRemaining(value->{seen.add(value);it.remove();});occurrences(seen.toArray(),original,"consumer removal multiplicity");equal(q.size(),0,"consumer removal empties");concrete("iterator/consumer-removal",seen,q);
        Iterator<Token> empty=GuavaMinMaxProtocolProof.<Token>create().iterator();row(1,"iterator/null-consumer",capture(()->{empty.forEachRemaining(null);return "DONE";}));
    }
    private static void regression(List<Integer> ranks,int[] remove,String label) throws Throwable{
        List<Token> original=new ArrayList<>();for(int i=0;i<ranks.size();i++)original.add(new Token(label+"/"+i,ranks.get(i)));Queue<Token> q=create(original);List<Token> remaining=new ArrayList<>(original);List<Token> delivered=new ArrayList<>();List<String> cursorChecks=new ArrayList<>();Iterator<Token> it=q.iterator();int removal=0;
        while(it.hasNext()){boolean repeatedOne=it.hasNext(),repeatedTwo=it.hasNext();check(repeatedOne&&repeatedTwo,"nextCursor cache repeat hasNext");cursorChecks.add("true/"+repeatedOne+"/"+repeatedTwo);Token value=it.next();delivered.add(value);if(removal<remove.length&&value.rank==remove[removal]){it.remove();removeIdentity(remaining,value,"regression removal");removal++;}match(q,remaining,RANK);}
        occurrences(delivered.toArray(),original,"regression complete occurrence traversal");cursorChecks.add("false/"+it.hasNext()+"/"+it.hasNext());concrete("regression/"+label,"delivered="+delivered+"|cursorChecks="+cursorChecks+"|removals="+removal+"|exhaustedNext="+format(cursor(it,1))+"|removeAfter="+format(cursor(it,2)),q);
        for(boolean last:List.of(false,true)){Queue<Token> drain=create(remaining);List<Token> rest=new ArrayList<>(remaining);while(!rest.isEmpty()){Token v=token(last?call(lastPoll,drain):call(firstPoll,drain));removeIdentity(rest,v,"regression fresh drain");match(drain,rest,RANK);}row(0,"regression/drain/"+label+"/"+last,"EMPTY");}
    }
    private static void donorRegressions() throws Throwable{
        List<Integer> childless=List.of(1,15,13,8,9,10,11,14);Queue<Token> child=create();List<Token> remaining=new ArrayList<>();for(int i=0;i<childless.size();i++){Token v=new Token("uncle"+i,childless.get(i));child.offer(v);remaining.add(v);}
        for(int rank:new int[]{9,11,10}){Token selected=null;for(Token value:remaining)if(value.rank==rank)selected=value;check(child.remove(selected),"childless public remove");removeIdentity(remaining,selected,"childless removal");match(child,remaining,RANK);}Iterator<Token> cursor=child.iterator();List<Token> delivered=new ArrayList<>();while(cursor.hasNext()){Token value=cursor.next();delivered.add(value);if(value.rank==8){cursor.remove();removeIdentity(remaining,value,"childless iter remove");}match(child,remaining,RANK);}concrete("regression/childless-uncle",delivered,child);
        List<Integer> invalid=List.of(1,20,1000,2,3,30,40,10,11,12,13,300,400,500,600);regression(invalid,new int[]{12},"invalidating-remove");List<Integer> invalid2=new ArrayList<>(invalid);invalid2.addAll(List.of(4,5,6,7,8,9,4,5,200,250));regression(invalid2,new int[]{2},"invalidating-remove2");
        regression(List.of(1,20,100,2,3,30,40),new int[]{2},"forgetMeNot");regression(List.of(1,20,1000,2,3,30,40,10,11,12,13,200,300,500,400),new int[]{2,3},"skip-and-forget");regression(List.of(2,3,0,4,1),new int[]{4,1},"remove4then1");
        Random random=new Random(7522346378524621981L);List<Integer> seventyThree=new ArrayList<>();for(int i=0;i<73;i++)seventyThree.add(random.nextInt(1000));regression(seventyThree,new int[]{seventyThree.get(17)},"seed73");
        Token[] five={new Token("enumA",0),new Token("enumB",1),new Token("enumC",2),new Token("enumD",3),new Token("enumE",4)};Random repeats=new Random(0x454e554dL);
        for(int trial=0;trial<80;trial++){List<Token> initial=new ArrayList<>();for(int i=0;i<45;i++)initial.add(five[repeats.nextInt(5)]);Queue<Token> q=create(initial);List<Token> rest=new ArrayList<>(initial),seen=new ArrayList<>();Iterator<Token> it=q.iterator();while(it.hasNext()){Token value=it.next();seen.add(value);if(repeats.nextBoolean()){it.remove();removeIdentity(rest,value,"repeated identity remove");}match(q,rest,RANK);}occurrences(seen.toArray(),initial,"repeated same reference occurrence");concrete("regression/repeated-reference/"+trial,seen,q);}
        for(int trial=0;trial<40;trial++){List<Token> initial=data(31,"mutable"+trial+"/");for(Token value:initial)value.rank%=3;Queue<Token> q=create(initial);List<Token> rest=new ArrayList<>(initial),seen=new ArrayList<>();Iterator<Token> it=q.iterator();while(it.hasNext()){Token value=it.next();seen.add(value);if(value.id.hashCode()%3==0){it.remove();removeIdentity(rest,value,"equal distinct removal");}match(q,rest,RANK);}occurrences(seen.toArray(),initial,"equal distinct occurrence traversal");concrete("regression/equal-distinct/"+trial,seen,q);}
        for(int trial=0;trial<40;trial++){List<Five> original=new ArrayList<>();for(int i=0;i<65;i++)original.add(Five.values()[repeats.nextInt(5)]);Queue<Five> q=create(original);List<Five> rest=new ArrayList<>(original),seen=new ArrayList<>();Iterator<Five> it=q.iterator();while(it.hasNext()){Five value=it.next();seen.add(value);if(repeats.nextBoolean()){it.remove();removeIdentity(rest,value,"enum occurrence removal");}occurrences(q.toArray(),rest,"enum remaining");}occurrences(seen.toArray(),original,"enum traversal multiplicity");concrete("regression/five-enum/"+trial,seen,q);}
        for(int trial=0;trial<40;trial++){List<EqualToken> original=new ArrayList<>();for(int i=0;i<45;i++)original.add(new EqualToken("equal"+trial+"/"+i,i%3));Queue<EqualToken> q=create(original);List<EqualToken> rest=new ArrayList<>(original),seen=new ArrayList<>();Iterator<EqualToken> it=q.iterator();while(it.hasNext()){EqualToken value=it.next();seen.add(value);if(repeats.nextBoolean()){it.remove();removeIdentity(rest,value,"equal payload exact occurrence removal");}occurrences(q.toArray(),rest,"equal payload remaining");}occurrences(seen.toArray(),original,"equal payload traversal multiplicity");concrete("regression/equal-mutable-payload/"+trial,seen,q);}
    }
    private static void epochs() throws Throwable{
        for(String mutation:new String[]{"offer","poll","remove-present","other-iterator","peek","remove-absent","null","clear","clear-offer","empty-poll"})for(boolean prior:List.of(false,true))for(int action=0;action<3;action++){
            Queue<Token> q=create(mutation.equals("empty-poll")?List.of():data(4,"epoch"));Iterator<Token> it=q.iterator();if(prior&&it.hasNext())it.next();Outcome change=capture(()->switch(mutation){case "offer"->q.offer(new Token("new",0));case "poll","empty-poll"->q.poll();case "remove-present"->q.remove(q.peek());case "other-iterator"->{Iterator<Token> other=q.iterator();other.next();other.remove();yield "REMOVED";}case "peek"->q.peek();case "remove-absent"->q.remove(new Token("absent",0));case "null"->q.offer(null);case "clear"->{q.clear();yield "CLEAR";}default->{q.clear();q.offer(new Token("after-clear",1));yield "CLEAR/OFFER";}});
            Outcome next=cursor(it,action);concrete("epoch/"+mutation+"/"+prior+"/"+action,format(change)+"|cursor="+format(next),q);
            if((mutation.equals("offer")||mutation.equals("poll")||mutation.equals("remove-present")||mutation.equals("other-iterator"))&&(action!=2||prior))equal(next.exception,ConcurrentModificationException.class.getName(),"successful mutation invalidates");
            if(action==2&&!prior)equal(next.exception,IllegalStateException.class.getName(),"remove permission before epoch");
        }
        for(boolean mutation:List.of(false,true)){Queue<Token> q=create(data(7,"clearConsumer"));Iterator<Token> it=q.iterator();List<Token> seen=new ArrayList<>();Outcome out=capture(()->{it.forEachRemaining(v->{seen.add(v);if(seen.size()==2){q.clear();if(mutation)q.offer(new Token("new",2));}});return "DONE";});concrete("epoch/clear-forEachRemaining/"+mutation,format(out)+"|seen="+seen+"|remove="+format(cursor(it,2)),q);}
    }
    private static final class Query {
        final Object wanted;final List<String> calls;final int stop;int count;
        Query(Object wanted,List<String> calls,int stop){this.wanted=wanted;this.calls=calls;this.stop=stop;}
        @Override public boolean equals(Object value){calls.add("query.equals/"+value);if(++count==stop)throw new IllegalStateException("equals-stop");return value==wanted;}
        @Override public int hashCode(){return 1;}
    }
    private static final class ReverseEqualsToken implements Comparable<ReverseEqualsToken> {
        final String id;final List<String> calls;ReverseEqualsToken(String id,List<String> calls){this.id=id;this.calls=calls;}
        @Override public int compareTo(ReverseEqualsToken value){return id.compareTo(value.id);}
        @Override public boolean equals(Object value){calls.add("element.equals/"+id);throw new IllegalStateException("element-equals-forbidden");}
        @Override public int hashCode(){return 1;}
        @Override public String toString(){return id;}
    }
    private static void equalityAndBulk() throws Throwable{
        for(boolean remove:List.of(false,true))for(int stop:new int[]{0,2}){
            Queue<Token> q=create(data(5,"eq"));Object target=q.toArray()[3];List<String> calls=new ArrayList<>();Query query=new Query(target,calls,stop);Outcome out=capture(()->remove?q.remove(query):q.contains(query));concrete("equals/query-direction/"+remove+"/"+stop,format(out)+"|calls="+calls,q);
            if(stop==0)equal(out.result,true,"query equals element succeeds");
        }
        List<String> equalsCalls=new ArrayList<>();ReverseEqualsToken a=new ReverseEqualsToken("a",equalsCalls),b=new ReverseEqualsToken("b",equalsCalls);Queue<ReverseEqualsToken> rq=create(List.of(a,b));Query query=new Query(b,equalsCalls,0);equal(rq.contains(query),true,"element equals not called");equal(rq.remove(query),true,"element equals not called for removal");row(1,"equals/asymmetric-element",equalsCalls);
        Queue<Token> q=create(data(5,"nullsearch"));equal(q.contains(null),false,"contains null false");equal(q.remove(null),false,"remove null false");equal(q.containsAll(List.of()),true,"containsAll empty true");concrete("equals/null-empty",null,q);
        for(String operation:new String[]{"removeAll","retainAll"}){Queue<Token> self=create(data(7,"selfbulk"));Object result=operation.equals("removeAll")?self.removeAll(self):self.retainAll(self);concrete("bulk/self/"+operation,result,self);equal(self.size(),operation.equals("removeAll")?0:7,"self membership bulk");}
        EqualToken ea=new EqualToken("equal-a",1),eb=new EqualToken("equal-b",1),ec=new EqualToken("equal-c",1);Queue<EqualToken> equals=create(List.of(ea,eb,ea,ec));List<EqualToken> before=new ArrayList<>(Arrays.asList(ea,eb,ea,ec));check(equals.remove(new EqualToken("query-equal",1)),"remove equal query succeeds");List<EqualToken> lost=new ArrayList<>(before);for(Object kept:equals.toArray())removeIdentity(lost,kept,"equal query kept occurrence");equal(lost.size(),1,"remove one equals occurrence");concrete("equals/equal-distinct-one",lost,equals);equal(equals.containsAll(List.of(new EqualToken("duplicate",1),new EqualToken("duplicate2",1))),true,"duplicate containsAll");
        for(String operation:new String[]{"removeAll","retainAll","removeIf","forEach","containsAll"})for(int stop:new int[]{0,3}){
            Queue<Token> target=create(data(7,"bulk"));List<String> calls=new ArrayList<>();Membership membership=new Membership(target,stop,calls);int[] count={0};Outcome out=capture(()->switch(operation){case "removeAll"->target.removeAll(membership);case "retainAll"->target.retainAll(membership);case "containsAll"->target.containsAll(new ObservedCollection<>(image(target),target.size(),stop,calls));case "removeIf"->target.removeIf(value->{calls.add("predicate/"+value+"/"+Arrays.toString(target.toArray()));if(stop>0&&++count[0]==stop)throw new IllegalStateException("predicate-stop");return value.rank<0;});default->{target.forEach(value->{calls.add("consumer/"+value);if(stop>0&&++count[0]==stop)throw new IllegalStateException("consumer-stop");});yield "DONE";}});concrete("bulk/"+operation+"/"+stop,format(out)+"|calls="+calls,target);if(stop==0)match(target,image(target),RANK);
        }
        for(String operation:new String[]{"addAll","containsAll","removeAll","retainAll","removeIf","forEach"}){Queue<Token> target=create(data(3,"nullbulk"));Outcome out=capture(()->switch(operation){case "addAll"->target.addAll(null);case "containsAll"->target.containsAll(null);case "removeAll"->target.removeAll(null);case "retainAll"->target.retainAll(null);case "removeIf"->target.removeIf(null);default->{target.forEach(null);yield "DONE";}});equal(out.exception,NullPointerException.class.getName(),"null bulk argument "+operation);concrete("bulk/null/"+operation,out,target);}
        for(int stop:new int[]{0,3}){Queue<Token> target=create(data(2,"existing"));List<String> calls=new ArrayList<>();List<Token> values=data(5,"added");Outcome out=capture(()->target.addAll(new ObservedCollection<>(values,5,stop,calls)));concrete("addAll/throwing-read/"+stop,format(out)+"|calls="+calls,target);}
        Queue<Token> prefix=create(data(2,"before"));List<Token> values=new ArrayList<>(data(2,"prefix"));values.add(null);values.add(new Token("unread",0));concrete("addAll/null-prefix",capture(()->prefix.addAll(values)),prefix);
        for(int n:new int[]{0,1,3}){Queue<Token> self=create(data(n,"selfadd"));row(3,"addAll/self/"+n,format(capture(()->self.addAll(self)))+"|array="+Arrays.toString(self.toArray()));Queue<Token> alias=create(data(n,"alias"));row(3,"addAll/backed-alias/"+n,format(capture(()->alias.addAll(Collections.unmodifiableCollection(alias))))+"|array="+Arrays.toString(alias.toArray()));}
        for(String operation:new String[]{"removeIf","forEach","removeAll","retainAll"})for(boolean clear:List.of(false,true)){Queue<Token> target=create(data(5,"reentry"));int[] reentryCount={0};List<String> calls=new ArrayList<>();Outcome out=capture(()->{if(operation.equals("removeIf"))return target.removeIf(value->{calls.add("callback/"+value);if(reentryCount[0]++==0){if(clear)target.clear();else target.offer(new Token("reentrant",1));}return value.rank<0;});if(operation.equals("forEach")){target.forEach(value->{calls.add("callback/"+value);if(reentryCount[0]++==0){if(clear)target.clear();else target.offer(new Token("reentrant",1));}});return "DONE";}Membership member=new Membership(target,0,calls){@Override public boolean contains(Object value){boolean result=super.contains(value);if(reentryCount[0]++==0){if(clear)target.clear();else target.offer(new Token("reentrant",1));}return result;}};return operation.equals("removeAll")?target.removeAll(member):target.retainAll(member);});row(3,"bulk/reentry/"+operation+"/"+clear,format(out)+"|calls="+calls+"|array="+Arrays.toString(target.toArray()));}
    }
    private static class Membership extends AbstractCollection<Object> {
        final Queue<Token> target;final int stop;final List<String> calls;int count;
        Membership(Queue<Token> target,int stop,List<String> calls){this.target=target;this.stop=stop;this.calls=calls;}
        @Override public int size(){return 0;}
        @Override public Iterator<Object> iterator(){return Collections.emptyIterator();}
        @Override public boolean contains(Object value){calls.add("contains/"+value+"/"+Arrays.toString(target.toArray()));if(stop>0&&++count==stop)throw new IllegalStateException("membership-stop");return token(value).rank<0;}
    }
    private static void arraysStreamsText() throws Throwable{
        for(int n:new int[]{0,1,2,3,17,65}){
            Queue<Token> q=create(data(n,"array"));List<Token> model=image(q);Object[] detached=q.toArray();if(n>0)detached[0]=null;occurrences(q.toArray(),model,"array detached");
            for(int size:new int[]{0,n,n+3}){Token sentinel=new Token("sentinel",99);Token[] supplied=new Token[size];Arrays.fill(supplied,sentinel);Token[] result=q.toArray(supplied);occurrences(Arrays.copyOf(result,n),model,"typed array occurrence");equal(result.getClass(),Token[].class,"typed component");if(size>=n){check(result==supplied,"typed supplied identity");if(size>n){equal(result[n],null,"null sentinel");for(int i=n+1;i<size;i++)check(result[i]==sentinel,"tail untouched");}}concrete("arrays/typed/"+n+"/"+size,result,q);}
            List<Integer> args=new ArrayList<>();Token[] generated=q.toArray((IntFunction<Token[]>)size->{args.add(size);return new Token[size];});equal(args,List.of(0),"generator gets zero");occurrences(generated,model,"generator occurrences");concrete("arrays/generator/"+n,args+"/"+Arrays.toString(generated),q);
            for(String refusal:new String[]{"nullArray","nullGenerator","nullReturn","throwGenerator"}){Outcome out=capture(()->switch(refusal){case "nullArray"->q.toArray((Token[])null);case "nullGenerator"->q.toArray((IntFunction<Token[]>)null);case "nullReturn"->q.toArray((IntFunction<Token[]>)size->null);default->q.toArray((IntFunction<Token[]>)size->{throw new IllegalStateException("generator-stop/"+size);});});concrete("arrays/refusal/"+n+"/"+refusal,out,q);}
            Spliterator<Token> spliterator=q.spliterator();equal(spliterator.characteristics(),Spliterator.SIZED|Spliterator.SUBSIZED,"spliterator flags");equal(spliterator.estimateSize(),(long)n,"estimateSize");row(1,"spliterator/getComparator/"+n,capture(spliterator::getComparator));row(1,"spliterator/nullAction/"+n,capture(()->spliterator.tryAdvance(null)));
            Spliterator<Token> split=q.spliterator();List<Token> traversal=new ArrayList<>();Spliterator<Token> first=split.trySplit();if(first!=null)first.forEachRemaining(traversal::add);split.forEachRemaining(traversal::add);occurrences(traversal.toArray(),model,"split multiset");concrete("spliterator/split/"+n,traversal,q);
            equal(q.stream().isParallel(),false,"stream sequential");equal(q.parallelStream().isParallel(),true,"stream parallel");occurrences(q.stream().toArray(),model,"stream multiplicity");occurrences(q.parallelStream().toArray(),model,"parallel multiplicity");equal(q.stream().mapToLong(value->value.rank).sum(),q.parallelStream().mapToLong(value->value.rank).sum(),"parallel reduction");concrete("stream/"+n,q.stream().toList(),q);int[] closures={0};List<String> limited;try(java.util.stream.Stream<Token> stream=q.stream().onClose(()->closures[0]++)){limited=stream.limit(3).map(value->value.id).toList();}equal(closures[0],1,"stream close callback exactly once");row(1,"stream/limit-close/"+n,"prefix="+limited+"|closes="+closures[0]);
            equal(q.toString(),model.toString(),"inherited toString order");concrete("toString/"+n,q.toString(),q);
        }
        Queue<Object> mixed=build(builder((Comparator<Object>)(a,b)->0),List.of("one",2,"three"));String[] target={"KEEP","KEEP","KEEP","KEEP"};Outcome wrong=capture(()->mixed.toArray(target));equal(wrong.exception,ArrayStoreException.class.getName(),"wrong component throws");concrete("arrays/wrong-type-prefix",format(wrong)+"|target="+Arrays.toString(target),mixed);
        List<Integer> generatorCalls=new ArrayList<>();String[] malformed={"KEEP","KEEP","KEEP","KEEP"};Outcome generated=capture(()->mixed.toArray((IntFunction<String[]>)size->{generatorCalls.add(size);return malformed;}));concrete("arrays/malformed-generator-prefix",format(generated)+"|calls="+generatorCalls+"|target="+Arrays.toString(malformed),mixed);
        Queue<Token> late=create(data(2,"late"));Spliterator<Token> lazy=late.spliterator();late.offer(new Token("late-add",3));equal(lazy.estimateSize(),3L,"late binding");List<Token> seen=new ArrayList<>();lazy.forEachRemaining(seen::add);occurrences(seen.toArray(),image(late),"late traversal");concrete("spliterator/late-bind",seen,late);
        Queue<Token> bound=create(data(3,"bound"));Spliterator<Token> b=bound.spliterator();b.estimateSize();bound.offer(new Token("after-bind",0));concrete("spliterator/bound-mutation",capture(()->b.tryAdvance(value->{ })),bound);
        Queue<Object> self=build(builder((Comparator<Object>)(a,c)->0));self.offer(self);equal(self.toString(),"[(this Collection)]","outer self marker");row(1,"toString/outerself",self.toString());
        
        List<String> duplicateCalls=new ArrayList<>();Object duplicate=new Object(){@Override public String toString(){duplicateCalls.add("render");return "dup";}};Queue<Object> duplicateQueue=build(builder((Comparator<Object>)(a,c)->0));duplicateQueue.add(duplicate);duplicateQueue.add(duplicate);equal(duplicateQueue.toString(),"[dup, dup]","duplicate toString occurrences");equal(duplicateCalls,List.of("render","render"),"each duplicate rendered separately");row(1,"toString/duplicate-callback",duplicateCalls);
        List<String> calls=new ArrayList<>();Queue<Object> text=build(builder((Comparator<Object>)(a,c)->0));text.add(new Object(){@Override public String toString(){calls.add("one");return "one";}});text.add(new Object(){@Override public String toString(){calls.add("throw");throw new IllegalStateException("text-stop");}});row(1,"toString/throw-prefix",format(capture(text::toString))+"|calls="+calls);
    }
    private static final class LoggingComparator implements Comparator<Token>,Serializable {
        private static final long serialVersionUID=1L;
        final ArrayList<String> calls;int failAt,count,reversed;
        LoggingComparator(List<String> calls,int failAt){this.calls=(ArrayList<String>)calls;this.failAt=failAt;}
        @Override public int compare(Token a,Token b){calls.add(a+":"+b);if(failAt>0&&++count==failAt)throw new IllegalStateException("compare-stop/"+failAt);return a.rank==b.rank?0:a.rank<b.rank?Integer.MIN_VALUE:1;}
        @Override public Comparator<Token> reversed(){reversed++;throw new IllegalStateException("ordinary-reversed-forbidden");}
        @Override public String toString(){return "plain-rank-MIN";}
        @Override public boolean equals(Object value){return value instanceof LoggingComparator;}
        @Override public int hashCode(){return 773;}
    }
    private static final class NaturalProbe implements Comparable<NaturalProbe> {
        final String id;final int rank;final List<String> calls;final int[] count;final int stop;
        NaturalProbe(String id,int rank,List<String> calls,int[] count,int stop){this.id=id;this.rank=rank;this.calls=calls;this.count=count;this.stop=stop;}
        @Override public int compareTo(NaturalProbe b){calls.add("compareTo/"+id+":"+b.id);if(stop>0&&++count[0]==stop)throw new IllegalStateException("natural-stop/"+stop);return Integer.compare(rank,b.rank);}
        @Override public String toString(){return id;}
    }
    @SuppressWarnings("unchecked") private static Comparator<Token> comparatorOf(Queue<Token> q) throws Throwable{return (Comparator<Token>)call(comparator,q);}
    private static void ordinaryAndNaturalComparators() throws Throwable{
        List<String> calls=new ArrayList<>();LoggingComparator supplied=new LoggingComparator(calls,0);Object b=builder(supplied);Queue<Token> q=build(b);equal(supplied.reversed,0,"ordinary reversed not called on construction");Comparator<Token> wrapped=comparatorOf(q);check(wrapped==comparatorOf(q),"stable comparator wrapper");check(wrapped!=supplied,"ordinary wrapped");equal(wrapped.toString(),supplied.toString(),"wrapper toString delegation");equal(wrapped.hashCode(),773,"wrapper hash delegation");Queue<Token> other=build(b);Comparator<Token> second=comparatorOf(other);check(second!=wrapped,"per-construction wrapper");check(wrapped.equals(second),"wrapper comparator equals");row(1,"comparator/equals-directions","wrapper-wrapper="+wrapped.equals(second)+"|wrapper-caller="+wrapped.equals(supplied)+"|caller-wrapper="+supplied.equals(wrapped)+"|null="+wrapped.equals(null)+"|other="+wrapped.equals("different"));
        Token same=new Token("same",1),low=new Token("low",-1),high=new Token("high",2);q.addAll(List.of(same,low,high,same));Token maximum=max(q);equal(maximum.rank,2,"MIN_VALUE max compare swaps");equal(supplied.reversed,0,"ordinary reversed never called");concrete("comparator/plain",calls+"|reversed="+supplied.reversed,q);
        calls.clear();wrapped.reversed().compare(low,high);wrapped.thenComparing(value->value.id).compare(same,new Token("same-other",1));Comparator.nullsFirst(wrapped).compare(null,same);Comparator.nullsLast(wrapped).compare(same,null);row(1,"comparator/JDK-defaults",calls);
        Object copy=roundtrip(new Object[]{wrapped,second,supplied,wrapped});Object[] graph=(Object[])copy;check(graph[0]==graph[3],"serialized comparator alias");check(graph[0]!=graph[1],"distinct wrappers remain distinct");@SuppressWarnings("unchecked") Comparator<Token> restored=(Comparator<Token>)graph[0];@SuppressWarnings("unchecked") Comparator<Token> restoredSecond=(Comparator<Token>)graph[1];LoggingComparator restoredCaller=(LoggingComparator)graph[2];restoredCaller.calls.clear();equal(restored.compare(low,high),Integer.MIN_VALUE,"comparator roundtrip compare");equal(restoredSecond.compare(high,low),1,"second wrapper compare");restoredCaller.compare(same,low);equal(restoredCaller.calls.size(),3,"shared caller alias across wrappers and raw graph");equal(restored.toString(),"plain-rank-MIN","comparator roundtrip text");row(1,"comparator/roundtrip","alias=true|distinct=true|compare=MIN|calls="+restoredCaller.calls+"|text="+restored);row(2,"comparator/wire-class",graph[0].getClass().getName());
        Queue<Token> natural=create();Comparator<Token> defaultOrder=comparatorOf(natural);Queue<Token> natural2=create();row(2,"comparator/natural-identity",defaultOrder.getClass().getName()+"|singleton="+(defaultOrder==comparatorOf(natural2)));
        for(int stop:new int[]{0,1,3}){List<String> log=new ArrayList<>();int[] count={0};NaturalProbe repeated=new NaturalProbe("shared",1,log,count,stop);Queue<NaturalProbe> probe=create();@SuppressWarnings("unchecked") Comparator<NaturalProbe> naturalForward=(Comparator<NaturalProbe>)call(comparator,probe);Outcome forward=capture(()->naturalForward.compare(repeated,repeated));List<String> forwardCalls=new ArrayList<>(log);equal(forwardCalls,List.of("compareTo/shared:shared"),"forward natural invokes same-reference compareTo");count[0]=0;log.clear();Outcome insert=capture(()->probe.addAll(List.of(repeated,repeated,repeated)));Outcome max=capture(()->call(lastPeek,probe));check(log.isEmpty(),"queue reverse-natural same-reference shortcut avoids callback");row(1,"comparator/natural-same-reference/"+stop,"forward="+format(forward)+"|forwardCalls="+forwardCalls+"|insert="+format(insert)+"|max="+format(max)+"|reverseCalls="+log+"|size="+probe.size()+"|array="+Arrays.toString(probe.toArray()));}
    }
    private static void naturalDistinctFailures() throws Throwable{
        for(int stop:new int[]{0,1,2,3,5}){List<String> calls=new ArrayList<>();int[] count={0};Queue<NaturalProbe> q=create();List<NaturalProbe> values=List.of(new NaturalProbe("n3",3,calls,count,stop),new NaturalProbe("n1",1,calls,count,stop),new NaturalProbe("n2",2,calls,count,stop),new NaturalProbe("n0",0,calls,count,stop));Outcome insert=capture(()->q.addAll(values));Outcome max=capture(()->call(lastPeek,q));row(1,"comparator/natural-distinct-failure/"+stop,format(insert)+"|max="+format(max)+"|calls="+calls+"|size="+q.size()+"|array="+Arrays.toString(q.toArray()));}
    }
    private static void comparatorFailures() throws Throwable{
        for(String operation:new String[]{"offer","add","pollFirst","pollLast","peekLast"})for(int fail:new int[]{1,2,3,4,6,9}){
            List<String> calls=new ArrayList<>();LoggingComparator cmp=new LoggingComparator(calls,0);Queue<Token> q=queue(cmp,Integer.MAX_VALUE,data(7,"failure"));Iterator<Token> old=q.iterator();old.next();cmp.count=0;cmp.failAt=fail;calls.clear();Object[] before=q.toArray();Outcome result=capture(()->switch(operation){case "offer"->q.offer(new Token("offered",-100));case "add"->q.add(new Token("added",-100));case "pollFirst"->call(firstPoll,q);case "pollLast"->call(lastPoll,q);default->call(lastPeek,q);});List<String> prefix=new ArrayList<>(calls);cmp.failAt=0;
            Object[] after=q.toArray();Outcome typed=capture(()->Arrays.toString(q.toArray(Token[]::new))),it=capture(()->{Iterator<Token> scan=q.iterator();List<String> seen=new ArrayList<>();for(int i=0;i<q.size()+2&&scan.hasNext();i++)seen.add(format(scan.next()));return seen;});Outcome least=capture(q::peek),greatest=capture(()->call(lastPeek,q));
            row(1,"comparator/failure/"+operation+"/"+fail,format(result)+"|before="+Arrays.toString(before)+"|size="+q.size()+"|after="+Arrays.toString(after)+"|calls="+prefix+"|typed="+format(typed)+"|iterator="+format(it)+"|peek="+format(least)+"|max="+format(greatest)+"|old="+format(cursor(old,0)));
        }
        for(boolean clear:List.of(false,true)){List<String> calls=new ArrayList<>();Queue<Token>[] holder=queueHolder();boolean[] fired={false};Comparator<Token> cmp=(a,b)->{calls.add(a+":"+b);if(!fired[0]&&holder[0]!=null){fired[0]=true;if(clear)holder[0].clear();else holder[0].offer(new Token("inside",5));}return RANK.compare(a,b);};Queue<Token> q=queue(cmp,Integer.MAX_VALUE,List.of());holder[0]=q;q.offer(new Token("first",2));Outcome out=capture(()->q.offer(new Token("second",1)));row(3,"comparator/mutation/"+clear,format(out)+"|calls="+calls+"|size="+q.size()+"|array="+Arrays.toString(q.toArray()));}
    }
    @SuppressWarnings("unchecked") private static Queue<Token>[] queueHolder(){return (Queue<Token>[])new Queue<?>[1];}
    private static Object roundtrip(Object graph) throws Throwable{ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(ObjectOutputStream stream=new ObjectOutputStream(bytes)){stream.writeObject(graph);}try(ObjectInputStream stream=new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))){return stream.readObject();}}
    private static void serializationAndComplexity() throws Throwable{
        for(int n:new int[]{0,3}){Queue<Token> q=create(data(n,"serialization"));Outcome outcome=capture(()->roundtrip(q));equal(outcome.exception,"java.io.NotSerializableException","queue serialization unsupported");row(1,"queue/serialization/"+n,outcome);check(!(q instanceof Serializable),"queue not Serializable");check(!(q instanceof Cloneable),"queue not Cloneable");}
        for(int n:new int[]{1,2,3,8,17,32,63,64,65,127,257,1024}){int[] count={0};Comparator<Token> cmp=(a,b)->{count[0]++;return RANK.compare(a,b);};Queue<Token> q=queue(cmp,Integer.MAX_VALUE,List.of());for(int i=0;i<n;i++)q.offer(new Token("complex"+i,n-i));int total=count[0];int log=32-Integer.numberOfLeadingZeros(n);check(total<=12*n*(log+1),"insertion logarithmic discriminator");count[0]=0;for(int i=0;i<n;i++)if((i&1)==0)call(firstPoll,q);else call(lastPoll,q);check(count[0]<=20*n*(log+1),"pop logarithmic discriminator");row(0,"complexity/bound/"+n,"offer<="+(12*n*(log+1))+"|pop<="+(20*n*(log+1)));row(1,"complexity/callback-count/"+n,"insert="+total+"|pops="+count[0]);}
    }
    public static void main(String[] args) throws Throwable{
        if(args.length!=1)throw new IllegalArgumentException("owner FQN");owner=Class.forName(args[0]);createEmpty=owner.getMethod("create");createContents=owner.getMethod("create",Iterable.class);orderedBy=owner.getMethod("orderedBy",Comparator.class);expectedSize=owner.getMethod("expectedSize",int.class);maximumSize=owner.getMethod("maximumSize",int.class);
        firstPoll=owner.getMethod("pollFirst");firstPeek=owner.getMethod("peekFirst");firstRemove=owner.getMethod("removeFirst");lastPoll=owner.getMethod("pollLast");lastPeek=owner.getMethod("peekLast");lastRemove=owner.getMethod("removeLast");comparator=owner.getMethod("comparator");for(int i=0;i<4;i++)DIGESTS[i]=MessageDigest.getInstance("SHA-256");
        factoriesAndBuilders();dualEndedAndRandom();boundedAndIdentity();legalIteratorSequences();donorRegressions();epochs();equalityAndBulk();arraysStreamsText();ordinaryAndNaturalComparators();naturalDistinctFailures();comparatorFailures();serializationAndComplexity();
        System.out.println("{\"status\":\"PASS\",\"mandatoryChecks\":"+checks+",\"modeledOperations\":"+operations+",\"partitions\":["+partitionJSON(0)+","+partitionJSON(1)+","+partitionJSON(2)+","+partitionJSON(3)+"],\"contractGroups\":41,\"sourceBlind\":true,\"nativeQueueStoreClaim\":false}");
    }
    private static String partitionJSON(int index){return "{\"name\":\""+PARTITIONS[index]+"\",\"rows\":"+ROWS[index]+",\"sha256\":\""+HexFormat.of().formatHex(DIGESTS[index].digest())+"\"}";}
}
