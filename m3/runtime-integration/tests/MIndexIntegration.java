/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class MIndexIntegration {
    static final Field BODY=field(String.class,"mindex"), VALUE=field(String.class,"value");
    static final Class<?> TYPE=BODY.getType();
    static final Field KIND=field(TYPE,"storageKind"), LOCAL=field(TYPE,"localValue"),
        PARTS=field(TYPE,"segments"), CACHE=field(TYPE,"materialized"), OWNER=field(TYPE,"mappedOwner"),
        ADDRESS=field(TYPE,"mappedAddress"), ID=field(TYPE,"canonicalId");
    static int checks;
    static Field field(Class<?> c,String n) {try{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}catch(Exception e){throw new ExceptionInInitializerError(e);}}
    static Object body(String s)throws Exception{s.length();return BODY.get(s);}
    static void check(boolean b,String label){checks++;if(!b)throw new AssertionError(label);}
    static void clean(String s)throws Exception {Object b=body(s);check(b!=null,"admitted");check(CACHE.get(b)==null,"unmaterialized");}
    static String fresh(String s){return new String(s.toCharArray());}
    static String general(String a,String b,Object object,int i,char c,boolean bool,long l,float f,double d){return "["+a+":"+b+":"+object+":"+i+":"+c+":"+bool+":"+l+":"+f+":"+d+"]";}
    static void constructors()throws Exception{
        String expected=fresh("alpha"); Object canonical=body(expected);
        String[] constructors={new String("alpha"),new String("alpha".toCharArray()),new String(new char[]{'!','a','l','p','h','a','!'},1,5),new String(new int[]{97,108,112,104,97},0,5),new String("alpha".getBytes(StandardCharsets.UTF_8),StandardCharsets.UTF_8),new String(new StringBuilder("alpha")),new String(new StringBuffer("alpha"))};
        for(String s:constructors){check(body(s)==canonical,"constructor canonical atom");check(s.equals(expected),"constructor content");}
        char[] source={'a','l','p','h','a'};String immutable=new String(source);source[0]='x';check(immutable.equals("alpha"),"defensive construction");
    }
    public static void main(String[] args)throws Exception {
        boolean mapped=Boolean.getBoolean("m3.expect.lexicon");constructors();
        String a=fresh("alpha"),b=fresh("vm-local-miss");Object aa=body(a),bb=body(b);
        check(KIND.getByte(aa)==(mapped?(Boolean.getBoolean("m3.shared.owner")?4:2):1),"shared/local admission");check(KIND.getByte(bb)==1,"miss local");
        check(body(fresh("vm-local-miss"))==bb,"same local atom");check(VALUE.get(b)==LOCAL.get(bb),"canonical local array");
        String joined=a+b;String duplicate=fresh("alpha")+fresh("vm-local-miss");
        check(body(joined)==body(duplicate),"canonical tuple identity");Object[] parts=(Object[])PARTS.get(body(joined));
        check(parts[0]==aa&&parts[1]==bb,"original scalar references");clean(joined);
        check(body(joined.substring(0,a.length()))==aa,"whole shared slice");
        String partial=joined.substring(2,a.length()+3);clean(partial);check(((Object[])PARTS.get(body(partial)))[0]==aa,"partial shared owner");
        check(joined.equals("alphavm-local-miss"),"logical equality");check(joined.hashCode()=="alphavm-local-miss".hashCode(),"logical hash");check(joined.intern()==duplicate.intern(),"intern");clean(joined);
        String wide=fresh("\u0100alpha");String ascii=wide.substring(1);check(ascii.equals(a)&&ascii.compareTo(a)==0,"slice coder parity");clean(ascii);
        if(mapped){check(KIND.getByte(body(wide))==(Boolean.getBoolean("m3.shared.owner")?4:2),"wide mapped");check(OWNER.get(body(wide))==OWNER.get(aa),"single mapped owner");check(LOCAL.get(aa)==null&&((byte[])VALUE.get(a)).length==0,"no local mapped payload");}
        Object once=new Object(){int calls;public String toString(){if(++calls!=1)throw new AssertionError("toString twice");return "object";}};
        String g=general(a,b,once,42,'Q',true,99L,1.5f,2.25d);check(g.equals("[alpha:vm-local-miss:object:42:Q:true:99:1.5:2.25]"),"general concat semantics");
        check(KIND.getByte(body(g))==3,"general concat tuple");boolean foundA=false,foundB=false;for(Object p:(Object[])PARTS.get(body(g))){foundA|=p==aa;foundB|=p==bb;}check(foundA&&foundB,"general retains original atoms");clean(g);
        String special=general(a,b,null,Integer.MIN_VALUE,'\ud800',false,Long.MIN_VALUE,Float.NaN,Double.POSITIVE_INFINITY);
        check(special.equals("[alpha:vm-local-miss:null:-2147483648:\ud800:false:-9223372036854775808:NaN:Infinity]"),"general null/primitive extremes");clean(special);
        if(Boolean.getBoolean("m3.shared.owner"))check(KIND.getByte(body(fresh("language-only")))==1,"language namespace excluded from Java String plane");
        String huge=fresh("x".repeat(9000));String tiny=huge.substring(4000,4002);check(((Object[])PARTS.get(body(tiny)))[0]==body(huge),"small slice retains exact local owner");
        for(int u=0;u<=65535;u++){
            String left=new String(new char[]{(char)u}),right=new String(new char[]{(char)(65535-u)});String s=left+right;
            check(s.length()==2&&s.charAt(0)==(char)u&&s.charAt(1)==(char)(65535-u),"UTF16 unit preservation");
            check(s.hashCode()==31*u+65535-u,"UTF16 hash");clean(s);
            String flat=new String(new char[]{(char)u,(char)(65535-u)});
            check(s.equals(flat)&&flat.equals(s)&&s.compareTo(flat)==0,"Unicode API parity");
            for(var cs:new java.nio.charset.Charset[]{StandardCharsets.UTF_8,StandardCharsets.UTF_16LE,StandardCharsets.ISO_8859_1})check(Arrays.equals(s.getBytes(cs),flat.getBytes(cs)),"encoding parity");
        }
        System.out.println("MINDEX_INTEGRATION_PASS checks="+checks+" mapped="+mapped);
        if(args.length>0&&args[0].equals("hold")){
            check(mapped,"hold requires map");long id=ID.getLong(aa),address=ADDRESS.getLong(aa);String path=System.getProperty("jdk.mindex.lexicon");
            for(String line:Files.readAllLines(Path.of("/proc/self/maps")))if(line.contains(path))System.out.println("MAP "+line);
            System.out.println("READY id="+id+" address="+Long.toUnsignedString(address));System.out.flush();System.in.read();
            for(int i=0;i<5;i++){System.gc();check(a.equals("alpha")&&partial.charAt(0)=='p',"mapping retained after unlink/GC");clean(joined);}
            System.out.println("MAPPING_LIFETIME_PASS id="+id);
        }
    }
}
