// SPDX-License-Identifier: Apache-2.0
package jdk.m3.structured;

import jdk.m3.text.Limits;
import jdk.m3.text.TextFrame;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import jdk.m3.structured.StructuredTextTools.DelimitedBackend;
import jdk.m3.structured.StructuredTextTools.DelimitedIndex;

/** Actual original TextFrame/limits/UTF-8 helpers plus the candidate and native kernel. */
public final class DelimitedIndexContract {
    private static long checks, models;
    private static DelimitedBackend backend=DelimitedBackend.JAVA;
    private static final Limits LIMITS=new Limits(100,0,10000,8L<<20);
    private DelimitedIndexContract() { }
    private static void check(boolean yes,String message){checks++;if(!yes)throw new AssertionError(message);}
    private static void expect(Class<? extends Throwable> type,Runnable body){
        try{body.run();}catch(Throwable failure){check(type.isInstance(failure),"wrong exception "+failure);return;}
        throw new AssertionError("expected "+type.getName());
    }
    private static String encode(List<List<String>> rows,char separator,String newline) {
        StringBuilder out=new StringBuilder();
        for(var row:rows){
            for(int field=0;field<row.size();field++){
                if(field!=0)out.append(separator);
                // Always quoted, including empty: distinguishes a sole empty row from empty input.
                out.append('"').append(row.get(field).replace("\"","\"\"")).append('"');
            }
            out.append(newline);
        }
        return out.toString();
    }
    private static void generated() {
        Random random=new Random(0x435356505245434fL);
        String[] atoms={"","a","b","01","1","x,y","x\ty","x;y","\"","\"\"","a\r\nb","\n","漢","😀","\0"," x ","\\","[x]","\ufeff"};
        for(int trial=0;trial<2000;trial++){
            char separator=new char[]{',','\t',';','|'}[trial%4];
            int columns=1+random.nextInt(7), records=random.nextInt(16);
            List<List<String>> expected=new ArrayList<>();
            for(int r=0;r<records;r++){
                var row=new ArrayList<String>();
                for(int c=0;c<columns;c++)row.add(atoms[random.nextInt(atoms.length)]);
                expected.add(row);
            }
            String source=encode(expected,separator,new String[]{"\n","\r","\r\n"}[trial%3]);
            var frame=TextFrame.fromString(source);
            DelimitedIndex index=StructuredTextTools.prepareDelimited(frame,separator,LIMITS,8L<<20);
            check(index.records()==records,"record count");
            check(index.fieldCount()==records*columns,"field count");
            check(index.primitivePayloadBytes()==4L*(3L*records*columns+records+1),"primitive bytes");
            check(index.materialize().equals(expected),"generated original field values");
            check(StructuredTextTools.delimited(frame,separator,LIMITS).equals(expected),"legacy materialization");
            for(int r=0;r<records;r++)for(int c=0;c<columns;c++)check(index.value(r,c).equals(expected.get(r).get(c)),"field view");
            for(int c=0;c<columns;c++)for(String value:atoms){
                int wanted=0;for(var row:expected)if(row.get(c).equals(value))wanted++;
                check(index.countEquals(c,value)==wanted,"uncapped predicate");
                for(int cap:new int[]{0,1,2,records,Integer.MAX_VALUE})
                    check(index.countEqualsUpTo(c,value,cap,backend)==Math.min(cap,wanted),"capped predicate");
            }
            int[] selection={columns-1,0,columns-1};
            int[] oldSelection=selection.clone();
            check(index.project(selection,LIMITS).equals(StructuredTextTools.selectDelimited(frame,separator,selection,LIMITS)),"projection parity");
            check(Arrays.equals(selection,oldSelection),"projection input ownership");
            String filter=atoms[random.nextInt(atoms.length)];
            List<List<String>> retained=new ArrayList<>();
            for(var row:expected)if(row.get(0).equals(filter))retained.add(row);
            String oracle=StructuredTextTools.selectDelimited(TextFrame.fromString(encode(retained,separator,"\n")),separator,selection,LIMITS).text();
            check(index.projectWhereEquals(0,filter,selection,LIMITS).text().equals(oracle),"filtered projection");
            models++;
        }
    }
    private static void guards() {
        var empty=StructuredTextTools.prepareDelimited(TextFrame.fromString(""),',',LIMITS,4);
        check(empty.countEqualsUpTo(Integer.MAX_VALUE,"",0,backend)==0,"empty zero");
        check(empty.countEqualsUpTo(Integer.MAX_VALUE,"",9,backend)==0,"empty nonzero");
        expect(IllegalArgumentException.class,()->StructuredTextTools.prepareDelimited(TextFrame.fromString(""),',',LIMITS,3));
        expect(IllegalArgumentException.class,()->StructuredTextTools.prepareDelimited(TextFrame.fromString("a"),',',LIMITS,19));
        check(StructuredTextTools.prepareDelimited(TextFrame.fromString("a"),',',LIMITS,20).primitivePayloadBytes()==20,"exact small budget");
        var ragged=StructuredTextTools.prepareDelimited(TextFrame.fromString("a,b\na\n"),',',LIMITS,1000);
        for(int cap:new int[]{0,1,Integer.MAX_VALUE})expect(IllegalArgumentException.class,()->ragged.countEqualsUpTo(1,"b",cap,backend));
        expect(IllegalArgumentException.class,()->empty.countEqualsUpTo(-1,"",0,backend));
        expect(IllegalArgumentException.class,()->empty.countEqualsUpTo(0,"",-1,backend));
        expect(NullPointerException.class,()->empty.countEqualsUpTo(0,null,0,backend));
        expect(NullPointerException.class,()->empty.countEqualsUpTo(0,"",0,null));
        expect(IndexOutOfBoundsException.class,()->empty.value(0,0));
        expect(IllegalArgumentException.class,()->ragged.project(new int[]{2},LIMITS));
        expect(IllegalArgumentException.class,()->ragged.projectWhereEquals(9,"a",new int[]{0},LIMITS));
        expect(IllegalArgumentException.class,()->ragged.project(new int[0],LIMITS));
        expect(IllegalArgumentException.class,()->ragged.project(new int[]{-1},LIMITS));
        expect(IllegalArgumentException.class,()->ragged.project(new int[]{0},new Limits(10,0,100,0)));
        var immutable=ragged.materialize();
        expect(UnsupportedOperationException.class,()->immutable.clear());
        expect(UnsupportedOperationException.class,()->immutable.getFirst().clear());
        if(backend==DelimitedBackend.JAVA)expect(UnsatisfiedLinkError.class,()->ragged.countEqualsUpTo(0,"a",1,DelimitedBackend.NATIVE_REQUIRED));
        String[] malformed={"\"","a\"","\"a\" ","\"a\"x", "\"a\n"};
        for(String s:malformed)expect(IllegalArgumentException.class,()->StructuredTextTools.prepareDelimited(TextFrame.fromString(s),',',LIMITS,1000));
    }
    private static Method rawMethod() throws Exception {
        var m=StructuredTextTools.class.getDeclaredMethod("nativeCountDelimitedEquals",String.class,int[].class,int[].class,int.class,String.class,int.class);
        m.setAccessible(true);return m;
    }
    private static int raw(Method method,String source,int[] fields,int[] rows,int column,String expected,int cap) {
        try{return (Integer)method.invoke(null,source,fields,rows,column,expected,cap);}
        catch(InvocationTargetException failure){
            if(failure.getCause() instanceof RuntimeException e)throw e;
            if(failure.getCause() instanceof Error e)throw e;
            throw new AssertionError(failure);
        }catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    private static void rawGuards() throws Exception {
        var raw=rawMethod();
        for(int cap:new int[]{0,1,Integer.MAX_VALUE}){
            check(raw(raw,"a\n\"b\"",new int[]{0,1,1,3,4,1},new int[]{0,1,2},0,"a",cap)==Math.min(cap,1),"raw valid");
            for(int[] fields:new int[][]{{0,1,1,3,4,2},{0,1,1,-1,4,1},{0,1,1,3,99,1},{0,1,1,2,3,1}, {0,1,1,0,1,1}})
                expect(IllegalArgumentException.class,()->raw(raw,"a\n\"b\"",fields,new int[]{0,1,2},0,"a",cap));
            expect(IllegalArgumentException.class,()->raw(raw,"a",new int[]{0,1,1},new int[]{0,1,1},0,"a",cap));
            expect(IllegalArgumentException.class,()->raw(raw,"a",new int[]{0,1,1},new int[]{0,2},0,"a",cap));
            expect(IllegalArgumentException.class,()->raw(raw,"a",new int[]{0,1,1},new int[]{0,1},1,"a",cap));
        }
        expect(IllegalArgumentException.class,()->raw(raw,null,new int[0],new int[]{0},0,"",0));
        expect(IllegalArgumentException.class,()->raw(raw,"",null,new int[]{0},0,"",0));
        expect(IllegalArgumentException.class,()->raw(raw,"",new int[0],null,0,"",0));
        expect(IllegalArgumentException.class,()->raw(raw,"",new int[0],new int[]{0},0,null,0));
        expect(IllegalArgumentException.class,()->raw(raw,"",new int[]{0},new int[]{0},0,"",0));
        expect(IllegalArgumentException.class,()->raw(raw,"",new int[0],new int[0],0,"",0));
        expect(IllegalArgumentException.class,()->raw(raw,"",new int[0],new int[]{0},-1,"",0));
        expect(IllegalArgumentException.class,()->raw(raw,"",new int[0],new int[]{0},0,"",-1));
    }
    private static void concurrent() throws Exception {
        var shared=StructuredTextTools.prepareDelimited(TextFrame.fromString("\"a\"\"b\",漢\r\n\"a\"\"b\",😀\n"),',',LIMITS,1000);
        try(var executor=Executors.newFixedThreadPool(8)){
            List<Future<Boolean>> jobs=new ArrayList<>();
            for(int worker=0;worker<16;worker++)jobs.add(executor.submit(()->{
                for(int i=0;i<100;i++)if(shared.countEqualsUpTo(0,"a\"b",2,backend)!=2)return false;
                return true;
            }));
            for(var job:jobs)check(job.get(),"concurrent read");
        }
    }
    public static void main(String[] args) throws Exception {
        String mode=args.length==0?"java":args[0];
        if(!mode.equals("java")){
            if(mode.equals("bad-abi")){
                expect(UnsatisfiedLinkError.class,()->StructuredTextTools.loadDelimitedNative(Path.of(args[1])));
                check(StructuredTextTools.delimited(TextFrame.fromString("a"),',',LIMITS).equals(List.of(List.of("a"))),"old parser survives ABI failure");
                System.out.println("DELIMITED_BAD_ABI_PASS checks="+checks);return;
            }
            StructuredTextTools.loadDelimitedNative(Path.of(args[1]));backend=DelimitedBackend.NATIVE_REQUIRED;
        }
        if(mode.equals("corrupt")||mode.equals("missing")){
            var index=StructuredTextTools.prepareDelimited(TextFrame.fromString("a\na\n"),',',LIMITS,1000);
            expect(mode.equals("missing")?UnsatisfiedLinkError.class:IllegalStateException.class,
                ()->index.countEqualsUpTo(0,"a",2,backend));
            check(index.countEquals(0,"a")==2,"Java unaffected by native failure");
            System.out.println("DELIMITED_FAULT_PASS mode="+mode+" checks="+checks);return;
        }
        generated();guards();concurrent();if(backend==DelimitedBackend.NATIVE_REQUIRED)rawGuards();
        System.out.println("DELIMITED_INDEX_PASS backend="+backend+" models="+models+" checks="+checks+" workers=16x100");
    }
}
