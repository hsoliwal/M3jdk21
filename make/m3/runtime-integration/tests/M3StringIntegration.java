/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class M3StringIntegration {
    static final Field BODY=M3StringInvariant.STRING_M3;
    static final Field VALUE=M3StringInvariant.STRING_VALUE;
    static final Field OWNER=M3StringInvariant.M3_OWNER;
    static final Field COORDINATE=M3StringInvariant.M3_VALUE;
    static int checks;

    static String fresh(String value) { return M3StringInvariant.fresh(value); }
    static Object body(String value) throws Exception { return M3StringInvariant.body(value); }
    static Object owner(String value) throws Exception { return OWNER.get(body(value)); }
    static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    static void clean(String value)throws Exception{
        Object m3=body(value);
        M3StringInvariant.assertNoArrayInstanceFields(m3.getClass());
        M3StringInvariant.assertNoArrayInstanceFields(OWNER.get(m3).getClass());
    }

    static void constructors()throws Exception{
        String expected=fresh("alpha");Object canonical=owner(expected);
        String[] values={
            new String("alpha"),new String("alpha".toCharArray()),
            new String(new char[]{'!','a','l','p','h','a','!'},1,5),
            new String(new int[]{97,108,112,104,97},0,5),
            new String("alpha".getBytes(StandardCharsets.UTF_8),StandardCharsets.UTF_8),
            new String(new StringBuilder("alpha")),new String(new StringBuffer("alpha"))
        };
        for(String value:values){
            check(owner(value)==canonical,"constructor canonical owner");
            check(value.equals(expected),"constructor content");
        }
        char[] source={'a','l','p','h','a'};String immutable=new String(source);
        source[0]='x';check(immutable.equals("alpha"),"defensive construction");
    }

    public static void main(String[] args)throws Exception{
        boolean mapped=Boolean.getBoolean("m3.expect.lexicon");
        constructors();

        String a=fresh("alpha"), b=fresh("vm-local-miss");
        Object aa=owner(a), bb=owner(b);
        check(M3StringInvariant.atom(aa),"alpha scalar");
        check(M3StringInvariant.atom(bb),"miss scalar");
        check((M3StringInvariant.payloadOwner(aa)!=null)==mapped,"mapped alpha owner");
        check(M3StringInvariant.payloadOwner(bb)==null,"miss process-local native owner");
        check(owner(fresh("vm-local-miss"))==bb,"same local canonical owner");

        String joined=a.concat(b);
        String duplicate=fresh("alpha").concat(fresh("vm-local-miss"));
        Object joinedOwner=owner(joined);
        check(M3StringInvariant.tuple(joinedOwner),"concat tuple");
        check(joinedOwner==owner(duplicate),"canonical tuple owner");
        clean(joined);

        String prefix=joined.substring(0,a.length());
        check(owner(prefix)==joinedOwner,"substring is parent-owner range");
        check(prefix.equals(a),"prefix content");

        String partial=joined.substring(2,a.length()+3);
        check(owner(partial)==joinedOwner,"partial range retains parent owner");
        check(partial.equals("phavm"),"partial content");
        clean(partial);

        check(joined.equals("alphavm-local-miss"),"logical equality");
        check(joined.hashCode()=="alphavm-local-miss".hashCode(),"logical hash");
        check(joined.intern()==duplicate.intern(),"intern");

        String huge=fresh("x".repeat(9000));
        String tiny=huge.substring(4000,4002);
        check(owner(tiny)==owner(huge),"tiny slice retains canonical scalar owner");
        check(tiny.equals("xx"),"tiny slice content");

        for(int unit=0;unit<=Character.MAX_VALUE;unit++){
            String left=new String(new char[]{(char)unit});
            String right=new String(new char[]{(char)(Character.MAX_VALUE-unit)});
            String value=left.concat(right);
            check(value.length()==2&&value.charAt(0)==(char)unit
                    &&value.charAt(1)==(char)(Character.MAX_VALUE-unit),"UTF16 units");
            check(value.hashCode()==31*unit+Character.MAX_VALUE-unit,"UTF16 hash");
            String flat=new String(new char[]{(char)unit,(char)(Character.MAX_VALUE-unit)});
            check(value.equals(flat)&&flat.equals(value)&&value.compareTo(flat)==0,"Unicode API parity");
            for(var cs:new java.nio.charset.Charset[]{
                    StandardCharsets.UTF_8,StandardCharsets.UTF_16LE,StandardCharsets.ISO_8859_1}) {
                check(Arrays.equals(value.getBytes(cs),flat.getBytes(cs)),"encoding parity");
            }
            clean(value);
        }

        System.out.println("M3_STRING_INTEGRATION_PASS checks="+checks+" mapped="+mapped);
        if(args.length>0&&args[0].equals("hold")){
            check(mapped,"hold requires map");
            long id=M3StringInvariant.field(aa.getClass(),"canonicalId").getLong(aa);
            long address=M3StringInvariant.address(aa);
            String path=System.getProperty("jdk.mindex.lexicon");
            for(String line:Files.readAllLines(Path.of("/proc/self/maps"))) {
                if(line.contains(path)) System.out.println("MAP "+line);
            }
            System.out.println("READY id="+id+" address="+Long.toUnsignedString(address));
            System.out.flush();System.in.read();
            for(int i=0;i<5;i++){
                System.gc();
                check(a.equals("alpha")&&partial.charAt(0)=='p',"mapping retained after unlink/GC");
                clean(joined);
            }
            System.out.println("MAPPING_LIFETIME_PASS id="+id);
        }
    }
}
